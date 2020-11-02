package nl.codestix.mcdiscordregions.listener;

import nl.codestix.mcdiscordregions.DiscordConnection;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    private DiscordConnection connection;

    public PlayerListener(DiscordConnection connection) {
        this.connection = connection;
    }

//    @EventHandler
//    public void onWhitelistDenied(PlayerLoginEvent event) {
//        if (event.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {
//        }
//    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        connection.death(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        connection.left(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        connection.respawn(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        connection.join(event.getPlayer().getUniqueId());
    }
}
