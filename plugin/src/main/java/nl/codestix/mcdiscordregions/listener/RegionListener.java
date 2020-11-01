package nl.codestix.mcdiscordregions.listener;

import com.sk89q.worldguard.protection.flags.StringFlag;
import nl.codestix.mcdiscordregions.event.RegionChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class RegionListener implements Listener {

    private JavaPlugin plugin;
    private StringFlag discordChannelFlag;

    public RegionListener(JavaPlugin plugin, StringFlag discordChannelFlag) {
        this.plugin = plugin;
        this.discordChannelFlag = discordChannelFlag;
    }

    @EventHandler
    public void onRegionChange(RegionChangeEvent event) {

    }
}
