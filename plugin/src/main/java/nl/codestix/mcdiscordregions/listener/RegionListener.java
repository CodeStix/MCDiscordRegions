package nl.codestix.mcdiscordregions.listener;

import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.codestix.mcdiscordregions.DiscordConnection;
import nl.codestix.mcdiscordregions.event.RegionChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RegionListener implements Listener {

    private DiscordConnection connection;
    private StringFlag discordChannelFlag;
    private String globalRegionName;

    public RegionListener(StringFlag discordChannelFlag, DiscordConnection connection, String globalRegionName) {
        this.discordChannelFlag = discordChannelFlag;
        this.connection = connection;
        this.globalRegionName = globalRegionName;
    }

    @EventHandler
    public void onRegionChange(RegionChangeEvent event) {
        ProtectedRegion region = event.getEnteredRegion();
        if (region == null)
        {
            // Move to global region
            connection.regionMove(event.getPlayer().getUniqueId(), globalRegionName);
            return;
        }
        String regionName = region.getFlag(discordChannelFlag);
        if (regionName == null)
            return;
        connection.regionMove(event.getPlayer().getUniqueId(), regionName);
    }
}
