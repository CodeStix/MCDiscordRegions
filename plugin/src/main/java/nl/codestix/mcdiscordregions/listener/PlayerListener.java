package nl.codestix.mcdiscordregions.listener;

import nl.codestix.mcdiscordregions.DiscordConnection;
import nl.codestix.mcdiscordregions.WorldGuardHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    private DiscordConnection connection;
    private RegionListener regionListener;

    public PlayerListener(DiscordConnection connection, RegionListener regionListener) {
        this.connection = connection;
        this.regionListener = regionListener;
    }

//    @EventHandler
//    public void onWhitelistDenied(PlayerLoginEvent event) {
//        if (event.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {
//        }
//    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        connection.playerDeath(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        connection.playerLeave(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        connection.playerRespawn(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String regionName = regionListener.getRegionName(WorldGuardHandler.getPlayerRegion(event.getPlayer()));
        connection.playerJoin(event.getPlayer().getUniqueId(), regionName);
    }
}
