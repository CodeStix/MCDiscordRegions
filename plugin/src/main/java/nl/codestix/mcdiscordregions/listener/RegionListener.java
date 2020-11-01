package nl.codestix.mcdiscordregions.listener;

import com.sk89q.worldguard.protection.flags.StringFlag;
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
        String regionName = event.getEnteredRegion().getFlag(discordChannelFlag);
        if (regionName == null) {
            Bukkit.getLogger().info("regionName is null, not sending message");
            return;
        }

        connection.regionMove(event.getPlayer().getUniqueId(), regionName);
    }
}
