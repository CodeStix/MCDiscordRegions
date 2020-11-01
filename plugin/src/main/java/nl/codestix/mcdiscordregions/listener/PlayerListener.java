package nl.codestix.mcdiscordregions.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {

    public PlayerListener() {
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

    }
}
