package nl.codestix.mcdiscordregions.listener;

import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.codestix.mcdiscordregions.DiscordConnection;
import nl.codestix.mcdiscordregions.event.RegionChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class RegionListener implements Listener {

    private DiscordConnection connection;
    private StringFlag discordChannelFlag;

    public RegionListener(StringFlag discordChannelFlag, DiscordConnection connection) {
        this.discordChannelFlag = discordChannelFlag;
        this.connection = connection;
    }

    @EventHandler
    public void onRegionChange(RegionChangeEvent event) {
        ProtectedRegion region = event.getEnteredRegion();
        if (region == null)
        {
            // Move to global region
            connection.regionMove(event.getPlayer().getUniqueId(), null);
            return;
        }
        String regionName = region.getFlag(discordChannelFlag);
        if (regionName == null)
            return;
        connection.regionMove(event.getPlayer().getUniqueId(), regionName);
    }
}
