package nl.codestix.mcdiscordregions;

import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

public class Commands implements CommandExecutor
{
    private JavaPlugin plugin;
    private DiscordBot bot;

    public Commands(JavaPlugin plugin, DiscordBot bot) {
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

        if (command.getName().equalsIgnoreCase("drg")) {

            if (strings.length >= 2 && strings[0].equalsIgnoreCase("message")) {
                String msg = join(" ", 1, strings);
                commandSender.sendMessage("Sending message: " + msg);
                bot.sendMessage(msg);
                return true;
            }
            else if (strings.length == 1 && strings[0].equalsIgnoreCase("info")) {
                Guild guild = bot.getGuild();
                Category category = bot.getCategory();
                VoiceChannel entry = bot.getEntryChannel();

                commandSender.sendMessage("Discord information:");
                commandSender.sendMessage(String.format("Server:        %-20s (%s)", guild == null ? "<not set>" : guild.getName(), guild == null ? "0" : guild.getId()));
                commandSender.sendMessage(String.format("Category:      %-20s (%s)", category == null ? "<not set>" : category.getName(), category == null ? "0" : category.getId()));
                commandSender.sendMessage(String.format("Entry Channel: %-20s (%s)", entry == null ? "<not set>" : entry.getName(), entry == null ? "0" : entry.getId()));
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
            else if(strings.length == 1 && strings[0].equalsIgnoreCase("list")) {
                Set<String> channelNames = bot.getChannels().keySet();
                commandSender.sendMessage(String.format("List of %d channels in category %s:", channelNames.size(), bot.getCategory().getName()));
                int i = 0;
                for(String name : channelNames) {
                    commandSender.sendMessage(String.format("#%d: %s", ++i, name));
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
            //commandSender.sendMessage("You entered the command: " + String.join(", ", strings));
            return true;
        }
        return false;
    }
}
