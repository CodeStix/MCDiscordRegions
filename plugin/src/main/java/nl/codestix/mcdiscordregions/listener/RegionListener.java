package nl.codestix.mcdiscordregions.listener;

import nl.codestix.mcdiscordregions.MCDiscordRegionsPlugin;
import nl.codestix.mcdiscordregions.Region;
import nl.codestix.mcdiscordregions.WorldGuardHandler;
import nl.codestix.mcdiscordregions.event.RegionChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RegionListener implements Listener {

    private MCDiscordRegionsPlugin plugin;

    public RegionListener(MCDiscordRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRegionChange(RegionChangeEvent event) {
        Player pl = event.getPlayer();
        String left = WorldGuardHandler.queryFlag(pl, WorldGuardHandler.getPlayerRegions(pl), plugin.discordChannelFlag);
        String entered = WorldGuardHandler.queryFlag(pl, event.getRegionSet(), plugin.discordChannelFlag);
        if (entered == null || (left != null && left.equals(entered)))
            return;

        Region enteredRegion = plugin.discordConnection.getOrCreateRegion(entered);
        if (enteredRegion.limit != 0 && enteredRegion.playerUuids.size() >= enteredRegion.limit) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThis room is full!");
            return;
        }

        plugin.discordConnection.playerRegionMove(event.getPlayer().getUniqueId(), entered);
    }
}
