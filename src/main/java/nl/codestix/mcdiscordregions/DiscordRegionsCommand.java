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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;

public class DiscordRegionsCommand implements CommandExecutor
{
    private JavaPlugin plugin;
    private DiscordBot bot;

    public DiscordRegionsCommand(JavaPlugin plugin, DiscordBot bot) {
        this.plugin = plugin;
        this.bot = bot;
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
            commandSender.sendMessage("Settings were written to config file.");
            return true;
        }
        else if (strings.length == 1 && strings[0].equalsIgnoreCase("info")) {
            Guild guild = bot.getGuild();
            Category category = bot.getCategory();
            VoiceChannel entryChannel = bot.getEntryChannel();
            VoiceChannel globalChannel = bot.getGlobalChannel();

            if (category != null) {
                Set<Map.Entry<String, VoiceChannel>> channels = bot.getChannels().entrySet();
                commandSender.sendMessage(String.format("§dList of §f%d§d channels in category §f%s§d:§r", channels.size(), category.getName()));
                int i = 0;
                for(Map.Entry<String, VoiceChannel> entry : channels) {
                    String format;
                    if (entry.getValue() == entryChannel)
                        format = "§6#%d:§r %s §a(entry channel)§r";
                    else if (entry.getValue() == globalChannel)
                        format = "§6#%d:§r %s §e(global channel)§r";
                    else
                        format = "§6#%d:§r %s";
                    commandSender.sendMessage(String.format(format, ++i, entry.getKey()));
                }

                commandSender.sendMessage("§dAll members in category:");
                for(Member mem : bot.getChannelMembers())
                    commandSender.sendMessage( mem.getEffectiveName());
            }

            commandSender.sendMessage("§dDiscord information:");
            commandSender.sendMessage("§6Server:§f " + (guild == null ? "<not set>" : (guild.getName() + "(" + guild.getIdLong() + ")")));
            commandSender.sendMessage("§6Category:§f " + (category == null ? "<not set>" : (category.getName() + "(" + category.getIdLong() + ")")));
            commandSender.sendMessage("§6Global channel:§f " + (globalChannel == null ? "<not set>" : (globalChannel.getName() + "(" + globalChannel.getIdLong() + ")")));
            commandSender.sendMessage("§6Entry channel:§f " + (entryChannel == null ? "<not set>" : (entryChannel.getName() + "(" + entryChannel.getIdLong() + ")")));
            return true;
        }
        else if (strings.length == 2 && strings[0].equalsIgnoreCase("server")) {
            String serverId = strings[1];
            try {
                bot.setGuild(serverId);
                commandSender.sendMessage(String.format("The used server is now '%s' (%s).", bot.getGuild().getName(), serverId));
            } catch (NullArgumentException ex) {
                commandSender.sendMessage(String.format("The server with id '%s' was not found.", serverId));
            }
            return true;
        }
        else if (strings.length >= 2 && strings[0].equalsIgnoreCase("category")) {
            String categoryName = join(" ", 1, strings);
            try {
                bot.setCategory(categoryName);
                commandSender.sendMessage(String.format("Category '%s' is now the active category.", categoryName));
            } catch(PermissionException ex) {
                commandSender.sendMessage("Could not set category due to permissions: " + ex.getMessage());
            } catch (NullArgumentException ex) {
                commandSender.sendMessage(String.format("The specified category '%s' was not found.", categoryName));
            }
            return true;
        }
        else if (strings.length >= 2 && strings[0].equalsIgnoreCase("global")) {
            String globalChannelName = join(" ", 1, strings);
            try {
                bot.setGlobalChannel(globalChannelName, true);
                commandSender.sendMessage(String.format("Global channel name is now set to '%s'.", globalChannelName));
            } catch(PermissionException ex) {
                commandSender.sendMessage("Could not set global channel name due to permissions: " + ex.getMessage());
            } catch (NullArgumentException ex) {
                commandSender.sendMessage(ex.getMessage());
            }
            return true;
        }
        else if (strings.length >= 2 && strings[0].equalsIgnoreCase("entry")) {
            String entryChannelName = join(" ", 1, strings);
            try {
                bot.setEntryChannel(entryChannelName, true);
                commandSender.sendMessage(String.format("Entry channel name is now set to '%s'.", entryChannelName));
            } catch(PermissionException ex) {
                commandSender.sendMessage("Could not set entry channel name due to permissions: " + ex.getMessage());
            } catch (NullArgumentException ex) {
                commandSender.sendMessage(ex.getMessage());
            }
            return true;
        }

        commandSender.sendMessage("Unknown arguments. Usage: " + command.getUsage());
        return true;
    }
}
