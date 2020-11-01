package nl.codestix.mcdiscordregions.command;

import nl.codestix.mcdiscordregions.MCDiscordRegionsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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
        else if (strings.length >= 1 && strings[0].equalsIgnoreCase("whitelist")) {
            if (strings.length >= 2) {
                plugin.getConfig().set(MCDiscordRegionsPlugin.CONFIG_USE_WHITELIST, strings[1].equalsIgnoreCase("on"));
            }
            commandSender.sendMessage("§dThe Discord-bound whitelist is " + (plugin.getConfig().getBoolean(MCDiscordRegionsPlugin.CONFIG_USE_WHITELIST, false) ? "on" : "off"));
            return true;
        }
        else if (strings.length >= 1 && strings[0].equalsIgnoreCase("kick")) {
            if (strings.length >= 2) {
                plugin.getConfig().set(MCDiscordRegionsPlugin.CONFIG_KICK_DISCORD_LEAVE, strings[1].equalsIgnoreCase("on"));
                if (strings.length >= 3) {
                    plugin.getConfig().set(MCDiscordRegionsPlugin.CONFIG_KICK_DISCORD_LEAVE_MESSAGE, join(" ", 2, strings));
                }
            }
            commandSender.sendMessage("§dDiscord leave bound kicking is " + (plugin.getConfig().getBoolean(MCDiscordRegionsPlugin.CONFIG_KICK_DISCORD_LEAVE, false) ? "on" : "off"));
            commandSender.sendMessage("§dKick message: " + plugin.getConfig().getString(MCDiscordRegionsPlugin.CONFIG_KICK_DISCORD_LEAVE_MESSAGE));
            return true;
        }
        else if (strings.length >= 1 && strings[0].equalsIgnoreCase("limit")) {
            commandSender.sendMessage("§cNot implemented yet.");
            return true;
        }

        commandSender.sendMessage("§cUnknown arguments. Usage: " + command.getUsage());
        return true;
    }
}
