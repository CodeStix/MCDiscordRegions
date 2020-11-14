package nl.codestix.mcdiscordregions.listener;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.codestix.mcdiscordregions.DiscordConnection;
import nl.codestix.mcdiscordregions.WorldGuardHandler;
import nl.codestix.mcdiscordregions.event.RegionChangeEvent;
import nl.codestix.mcdiscordregions.event.RegionInitializeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class RegionListener implements Listener {

    private DiscordConnection connection;
    private StringFlag discordChannelFlag;
    private String globalRegionName;

    private HashSet<String> fullRegions = new HashSet<>();

    public RegionListener(StringFlag discordChannelFlag, DiscordConnection connection, String globalRegionName) {
        this.discordChannelFlag = discordChannelFlag;
        this.connection = connection;
        this.globalRegionName = globalRegionName;
    }

    public String getRegionName(ProtectedRegion region) {
        if (region == null) {
            return globalRegionName;
        }
        else {
            String flag = region.getFlag(discordChannelFlag);
            return flag == null ? globalRegionName : flag;
        }
    }

    public void fullRegion(String regionName) {
        fullRegions.add(regionName);
    }

    @EventHandler
    public void onRegionInit(RegionInitializeEvent event) {
        connection.regionMove(event.getPlayer().getUniqueId(), getRegionName(event.getRegion()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        fullRegions.remove(getRegionName(WorldGuardHandler.getPlayerRegion(event.getPlayer())));
    }

    @EventHandler
    public void onRegionChange(RegionChangeEvent event) {
        String left = getRegionName(event.getLeftRegion());
        String entered = getRegionName(event.getEnteredRegion());
        if (left.equals(entered))
            return;

        if (fullRegions.contains(entered)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThis room is full!");
            return;
        }
        fullRegions.remove(left);

        connection.regionMove(event.getPlayer().getUniqueId(), entered);
    }
}
