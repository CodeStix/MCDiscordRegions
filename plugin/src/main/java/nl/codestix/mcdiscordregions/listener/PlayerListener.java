package nl.codestix.mcdiscordregions.listener;

import nl.codestix.mcdiscordregions.MCDiscordRegionsPlugin;
import nl.codestix.mcdiscordregions.WorldGuardHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    private MCDiscordRegionsPlugin plugin;

    public PlayerListener(MCDiscordRegionsPlugin plugin) {
        this.plugin = plugin;
    }

//    @EventHandler
//    public void onWhitelistDenied(PlayerLoginEvent event) {
//        if (event.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {
//        }
//    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.connection.playerDeath(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.connection.playerLeave(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.connection.playerRespawn(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String regionName = plugin.getRegionName(WorldGuardHandler.getPlayerRegion(event.getPlayer()));
        plugin.connection.playerJoin(event.getPlayer().getUniqueId(), regionName);
    }
}
