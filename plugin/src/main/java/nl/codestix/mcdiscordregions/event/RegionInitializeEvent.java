package nl.codestix.mcdiscordregions.event;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RegionInitializeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private ApplicableRegionSet regionSet;

    public RegionInitializeEvent(Player player, ApplicableRegionSet regionSet) {
        this.player = player;
        this.regionSet = regionSet;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }

    public ApplicableRegionSet getRegionSet() {
        return regionSet;
    }
}
