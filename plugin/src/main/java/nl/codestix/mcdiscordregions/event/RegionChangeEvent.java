package nl.codestix.mcdiscordregions.event;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class RegionChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private UUID id;
    private Player player;
    private ProtectedRegion leftRegion, enteredRegion;
    private ApplicableRegionSet regionSet;

    public RegionChangeEvent(UUID id, Player player, ProtectedRegion leftRegion, ProtectedRegion enteredRegion, ApplicableRegionSet regionSet) {
        this.id = id;
        this.player = player;
        this.leftRegion = leftRegion;
        this.enteredRegion = enteredRegion;
        this.regionSet = regionSet;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }

    public UUID getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public ProtectedRegion getLeftRegion() {
        return leftRegion;
    }

    public ProtectedRegion getEnteredRegion() {
        return enteredRegion;
    }

    public ApplicableRegionSet getRegionSet() {
        return regionSet;
    }
}
