package nl.codestix.mcdiscordregions.listener;

import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.codestix.mcdiscordregions.DiscordConnection;
import nl.codestix.mcdiscordregions.Region;
import nl.codestix.mcdiscordregions.event.RegionChangeEvent;
import nl.codestix.mcdiscordregions.event.RegionInitializeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class RegionListener implements Listener {

    private DiscordConnection connection;
    private StringFlag discordChannelFlag;
    private String globalRegionName;

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

    @EventHandler
    public void onRegionInit(RegionInitializeEvent event) {
        //connection.regionMove(event.getPlayer().getUniqueId(), getRegionName(event.getRegion()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
    }

    @EventHandler
    public void onRegionChange(RegionChangeEvent event) {
        String left = getRegionName(event.getLeftRegion());
        String entered = getRegionName(event.getEnteredRegion());
        if (left.equals(entered))
            return;

        Region enteredRegion = connection.getOrCreateRegion(entered);
        if (enteredRegion.limit != 0 && enteredRegion.playerUuids.size() >= enteredRegion.limit) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThis room is full!");
            return;
        }

        connection.regionMove(event.getPlayer().getUniqueId(), entered);
    }
}
