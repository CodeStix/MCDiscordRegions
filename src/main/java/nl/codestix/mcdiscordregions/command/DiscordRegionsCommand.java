package nl.codestix.mcdiscordregions.command;

import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import nl.codestix.mcdiscordregions.MCDiscordRegionsPlugin;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Set;

public class DiscordRegionsCommand implements CommandExecutor
{
    private MCDiscordRegionsPlugin plugin;

    public DiscordRegionsCommand(MCDiscordRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    public String join(String delimiter, int start, String... strings) {
        StringBuilder builder = new StringBuilder();
        for(int i = start; i < strings.length; i++) {
            if (i > start)
                builder.append(delimiter);
            builder.append(strings[i]);
        }
        return builder.toString();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if (strings.length == 1 && strings[0].equalsIgnoreCase("save")) {
            plugin.saveConfig();
            commandSender.sendMessage("§dSettings were written to config file.");
            return true;
        }
        else if (strings.length == 1 && strings[0].equalsIgnoreCase("info")) {
            Guild guild = plugin.bot.getGuild();
            Category category = plugin.bot.getCategory();
            VoiceChannel entryChannel = plugin.bot.getEntryChannel();

            if (category != null) {
                Set<Map.Entry<String, VoiceChannel>> channels = plugin.bot.getChannels().entrySet();
                commandSender.sendMessage(String.format("§dList of §f%d§d channels in category §f%s§d:§r", channels.size(), category.getName()));
                int i = 0;
                for(Map.Entry<String, VoiceChannel> entry : channels) {
                    String format;
                    if (entry.getValue() == entryChannel)
                        format = "§6#%d:§r %s §a(entry channel)§r";
                    else
                        format = "§6#%d:§r %s";
                    commandSender.sendMessage(String.format(format, ++i, entry.getKey()));
                }

                commandSender.sendMessage("§dAll members in category:");
                for(Member mem : plugin.bot.getCategoryMembers())
                    commandSender.sendMessage( mem.getEffectiveName());
            }

            commandSender.sendMessage("§dDiscord information:");
            commandSender.sendMessage("§6Server:§f " + (guild == null ? "<not set>" : guild.getName()));
            commandSender.sendMessage("§6Category:§f " + (category == null ? "<not set>" : category.getName()));
            commandSender.sendMessage("§6Entry channel:§f " + (entryChannel == null ? "<not set>" : entryChannel.getName()));
            commandSender.sendMessage("§dWhitelist: " + (plugin.discordPlayerListener.getUseWhitelist() ? "§aon" : "§coff"));
            commandSender.sendMessage("§dKick on Discord leave: " + (plugin.discordPlayerListener.kickOnDiscordLeave ? "§aon" : "§coff"));
            if (plugin.discordPlayerListener.kickOnDiscordLeave)
                commandSender.sendMessage("§dKick on Discord leave message: §f" + plugin.discordPlayerListener.kickOnDiscordLeaveMessage);
            return true;
        }
        else if (strings.length >= 1 && strings[0].equalsIgnoreCase("deletecategory")) {
            if (strings.length == 1 || !strings[1].equalsIgnoreCase("confirm")) {
                commandSender.sendMessage(String.format("§dAre you sure you want to delete category §f%s§d and §f%d§d channels in it? " +
                        "Use '/%s %s confirm' to confirm.", plugin.bot.getCategory().getName(), plugin.bot.getChannelCount(), s, strings[0]));
            }
            else {
                commandSender.sendMessage("§dRemoving category and channels...");
                plugin.bot.deleteCategory();
            }
            return true;
        }
        else if (strings.length >= 1 && strings[0].equalsIgnoreCase("whitelist")) {
            if (strings.length >= 2) {
                boolean useWhitelist = strings[1].equalsIgnoreCase("on");
                plugin.discordPlayerListener.setUseWhitelist(useWhitelist);
            }
            commandSender.sendMessage("§dDiscord Regions' whitelist is " + (plugin.discordPlayerListener.getUseWhitelist() ? "§aon" : "§coff"));
            return true;
        }
        else if (strings.length >= 1 && strings[0].equalsIgnoreCase("kickOnDiscordLeave")) {
            if (strings.length >= 2) {
                plugin.discordPlayerListener.kickOnDiscordLeave = strings[1].equalsIgnoreCase("on");
            }
            commandSender.sendMessage("§dKick on discord leave is " + (plugin.discordPlayerListener.kickOnDiscordLeave ? "§aon" : "§coff"));

            if (strings.length >= 3) {
                plugin.discordPlayerListener.kickOnDiscordLeaveMessage = join(" ", 2, strings);
            }
            commandSender.sendMessage("§dKick on discord leave message is: §f" + plugin.discordPlayerListener.kickOnDiscordLeaveMessage);
            return true;
        }
        else if (strings.length >= 2 && strings[0].equalsIgnoreCase("category")) {
            String categoryName = join(" ", 1, strings);
            try {
                plugin.bot.getCategoryByNameOrCreate(categoryName, category -> {
                    plugin.bot.setCategory(category);
                    commandSender.sendMessage(String.format("§dCategory §f%s§d is now the active category.", categoryName));
                });
            } catch(PermissionException ex) {
                commandSender.sendMessage("§cCould not set category due to permissions: " + ex.getMessage());
            } catch (NullArgumentException ex) {
                commandSender.sendMessage(String.format("§cThe specified category §f%s§d was not found.", categoryName));
            }
            return true;
        }
        else if (strings.length >= 2 && strings[0].equalsIgnoreCase("entry")) {
            String entryChannelName = join(" ", 1, strings);
            if (plugin.bot.getCategory() == null) {
                commandSender.sendMessage("§cCannot set entry channel, the Discord category must be configured first with §f/dregion category your_category_name§c.");
                return true;
            }

            try {
                plugin.bot.setEntryChannel(entryChannelName, true);
                commandSender.sendMessage(String.format("§dEntry channel name is now set to §f%s§d.", entryChannelName));
            } catch(PermissionException ex) {
                commandSender.sendMessage("§cCould not set entry channel name due to permissions: " + ex.getMessage());
            } catch (NullArgumentException ex) {
                commandSender.sendMessage("§c" + ex.getMessage());
            }
            return true;
        }
        else if (strings.length >= 2 && strings[0].equalsIgnoreCase("limit")) {
            String userLimitString = strings[1];
            String channelName = join(" ", 2, strings);

            int userLimit;
            try {
                userLimit = Integer.parseInt(userLimitString);
            }
            catch(NumberFormatException ex) {
                commandSender.sendMessage("§cInvalid user limit number '" + userLimitString + "'");
                return true;
            }

            if (userLimit < 0 || userLimit > 100) {
                commandSender.sendMessage("§cUser limit is too low or too high!");
                return true;
            }

            VoiceChannel channel = plugin.bot.getChannelByName(channelName);
            if (channel == null)
            {
                commandSender.sendMessage(String.format("§cChannel with name '%s' not found.", channelName));
                return true;
            }

            channel.getManager().setUserLimit(userLimit).queue((a) -> {
                commandSender.sendMessage(String.format("§dUser limit was set to %d for channel '%s'.", userLimit, channelName));
            });
        }
        else if (strings.length >= 1 && strings[0].equalsIgnoreCase("db")) {
            commandSender.sendMessage(String.format("§dThere are §f%d/%d§d registered Discord players in the database. ", plugin.playerDatabase.getPlayerCount(), plugin.playerDatabase.maxPlayers));
            commandSender.sendMessage("§dAccepting new players? " + (plugin.playerDatabase.acceptNewPlayers ? "§ayes" : "§cno"));
            return true;
        }

        commandSender.sendMessage("§cUnknown arguments. Usage: " + command.getUsage());
        return true;
    }
}
