package nl.codestix.mcdiscordregions.listener;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.codestix.mcdiscordregions.MCDiscordRegionsPlugin;
import nl.codestix.mcdiscordregions.Region;
import nl.codestix.mcdiscordregions.event.RegionChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RegionListener implements Listener {

    private MCDiscordRegionsPlugin plugin;

    public RegionListener(MCDiscordRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRegionChange(RegionChangeEvent event) {
        String left = plugin.getRegionName(event.getLeftRegion());
        String entered = plugin.getRegionName(event.getEnteredRegion());
        if (entered == null || (left != null && left.equals(entered)))
            return;

        Region enteredRegion = plugin.connection.getOrCreateRegion(entered);
        if (enteredRegion.limit != 0 && enteredRegion.playerUuids.size() >= enteredRegion.limit) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThis room is full!");
            return;
        }

        plugin.connection.playerRegionMove(event.getPlayer().getUniqueId(), entered);
    }
}
