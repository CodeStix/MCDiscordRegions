package nl.codestix.mcdiscordregions;

import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
            return true;
        }
        else if (strings.length >= 2 && strings[0].equalsIgnoreCase("resolve")) {
            String name = join(" ", 1, strings);
            UUID id = MojangAPI.playerNameToUUID(name);
            if (id != null)
                commandSender.sendMessage("UUID: " + id.toString());
            else
                commandSender.sendMessage("§cCould not resolve");
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
            Set<Map.Entry<String, VoiceChannel>> channels = bot.getChannels().entrySet();
            VoiceChannel entryChannel = bot.getEntryChannel();
            commandSender.sendMessage(String.format("§dList of §f%d§d channels in category §f%s§d:§r", channels.size(), bot.getCategory().getName()));

            int i = 0;
            for(Map.Entry<String, VoiceChannel> entry : channels)
                commandSender.sendMessage(String.format("§6#%d:§r %s" + (entry.getValue() == entryChannel ? "§a(entry channel)§r" : ""), ++i, entry.getKey()));
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
