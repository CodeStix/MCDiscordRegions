package nl.codestix.mcdiscordregions.command;

import nl.codestix.mcdiscordregions.MCDiscordRegionsPlugin;
import nl.codestix.mcdiscordregions.Region;
import nl.codestix.mcdiscordregions.WorldGuardHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

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

        if (!commandSender.hasPermission("discordregions.modify")) {
            commandSender.sendMessage("§cYou don't have the permission to use this command.");
            return true;
        }

        switch (strings.length > 0 ? strings[0] : "help")
        {
            case "require": {
                if (strings.length > 2)
                    break;
                if (strings.length == 2) {
                    plugin.getConfig().set(MCDiscordRegionsPlugin.CONFIG_REQUIRE_DISCORD, strings[1].equalsIgnoreCase("on"));
                    plugin.saveConfig();
                }
                commandSender.sendMessage("§dRequired Discord voice connection is " + (plugin.getConfig().getBoolean(MCDiscordRegionsPlugin.CONFIG_REQUIRE_DISCORD, false) ? "§aenabled" : "§cdisabled"));
                commandSender.sendMessage("§dEdit the kick/chat messages in the config.yml file of this plugin, then use /drg reload to load them.");
                return true;
            }

            case "limit": {
                if (strings.length < 2)
                    break;

                int limit = Integer.parseInt(strings[1]);
                String regionName;
                if (strings.length >= 3) {
                    regionName =  join(" ", 2, strings);
                }
                else if (commandSender instanceof Player) {
                    regionName = WorldGuardHandler.queryFlag((Player)commandSender, plugin.discordChannelFlag);
                    if (regionName == null) {
                        commandSender.sendMessage("§cCannot infer region to limit from your location, stand in a Discord region or use /drg limit <maxUsers> [regionName...]");
                        return true;
                    }
                }
                else {
                    commandSender.sendMessage("§cUse /drg limit <maxUsers> [regionName...]");
                    return true;
                }

                if (limit < 0 || limit > 100) {
                    commandSender.sendMessage("§cUser limit should be in range 0-100, 0 meaning no limit.");
                }
                else if (plugin.discordConnection.limitRegion(regionName, limit)) {
                    commandSender.sendMessage("§dSet the limit for " + regionName + " to " + limit);
                }
                else {
                    commandSender.sendMessage("§cCould not set limit for region " + regionName);
                }
                return true;
            }

            case "prune": {
                if (strings.length != 1)
                    break;
                commandSender.sendMessage("§cRemoved " + plugin.discordConnection.getRegions().size() + " Discord channels");
                plugin.discordConnection.pruneRegions();
                return true;
            }

            case "debug": {
                if (strings.length != 1)
                    break;
                Collection<Region> regions = plugin.discordConnection.getRegions();
                for(Region region : regions) {
                    commandSender.sendMessage(String.format("§dRegion %s (limit=%d)", region.name, region.limit));
                    for(String uuid : region.playerUuids) {
                        Player pl = plugin.getServer().getPlayer(UUID.fromString(uuid));
                        commandSender.sendMessage(String.format(" - Player %s (uuid=%s)", pl == null ? "<null>" : pl.getName(), uuid));
                    }
                }
                return true;
            }

            case "reload": {
                if (strings.length != 1)
                    break;
                plugin.reloadConfig();
                commandSender.sendMessage("§dReloaded config.");
                return true;
            }
        }

        commandSender.sendMessage("§cUnknown arguments. Usage: " + command.getUsage());
        return true;
    }
}
