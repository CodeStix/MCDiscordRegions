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
//        else if (strings.length >= 1 && strings[0].equalsIgnoreCase("whitelist")) {
//            if (strings.length == 1) {
//
//            }
//            else {
//                plugin.getConfig().set(MCDiscordRegionsPlugin.CONFIG_USE_WHITELIST,strings[1].equalsIgnoreCase("on"));
//            }
//        }

        commandSender.sendMessage("§cUnknown arguments. Usage: " + command.getUsage());
        return true;
    }
}
