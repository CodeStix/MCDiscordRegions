package nl.codestix.mcdiscordregions;

import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.apache.commons.lang.NullArgumentException;
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
            commandSender.sendMessage("§dWhitelist: " + (plugin.regionEvents.getUseWhitelist() ? "§aon" : "§coff"));
            commandSender.sendMessage("§dKick on Discord leave: " + (plugin.regionEvents.kickOnDiscordLeave ? "§aon" : "§coff"));
            if (plugin.regionEvents.kickOnDiscordLeave)
                commandSender.sendMessage("§dKick on Discord leave message: §f" + plugin.regionEvents.kickOnDiscordLeaveMessage);
            return true;
        }
        else if (strings.length >= 1 && strings[0].equalsIgnoreCase("whitelist")) {
            if (strings.length >= 2) {
                boolean useWhitelist = strings[1].equalsIgnoreCase("on");
                plugin.regionEvents.setUseWhitelist(useWhitelist);
            }
            commandSender.sendMessage("§dDiscord Regions' whitelist is " + (plugin.regionEvents.getUseWhitelist() ? "§aon" : "§coff"));
            return true;
        }
        else if (strings.length >= 1 && strings[0].equalsIgnoreCase("kickOnDiscordLeave")) {
            if (strings.length >= 2) {
                plugin.regionEvents.kickOnDiscordLeave = strings[1].equalsIgnoreCase("on");
            }
            commandSender.sendMessage("§dKick on discord leave is " + (plugin.regionEvents.kickOnDiscordLeave ? "§aon" : "§coff"));

            if (strings.length >= 3) {
                plugin.regionEvents.kickOnDiscordLeaveMessage = join(" ", 2, strings);
            }
            commandSender.sendMessage("§dKick on discord leave message is: §f" + plugin.regionEvents.kickOnDiscordLeaveMessage);
            return true;
        }
        else if (strings.length >= 2 && strings[0].equalsIgnoreCase("category")) {
            String categoryName = join(" ", 1, strings);
            try {
                plugin.bot.getCategoryByNameOrCreate(categoryName, category -> {
                    plugin.bot.setCategory(category);
                    commandSender.sendMessage(String.format("§dCategory '%s' is now the active category.", categoryName));
                });
            } catch(PermissionException ex) {
                commandSender.sendMessage("§cCould not set category due to permissions: " + ex.getMessage());
            } catch (NullArgumentException ex) {
                commandSender.sendMessage(String.format("§cThe specified category '%s' was not found.", categoryName));
            }
            return true;
        }
        else if (strings.length >= 2 && strings[0].equalsIgnoreCase("entry")) {
            String entryChannelName = join(" ", 1, strings);
            try {
                plugin.bot.setEntryChannel(entryChannelName, true);
                commandSender.sendMessage(String.format("§dEntry channel name is now set to '%s'.", entryChannelName));
            } catch(PermissionException ex) {
                commandSender.sendMessage("§cCould not set entry channel name due to permissions: " + ex.getMessage());
            } catch (NullArgumentException ex) {
                commandSender.sendMessage("§c" + ex.getMessage());
            }
            return true;
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
