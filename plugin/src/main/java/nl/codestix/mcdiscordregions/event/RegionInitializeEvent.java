package nl.codestix.mcdiscordregions.event;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RegionInitializeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private ProtectedRegion initRegion;

    public RegionInitializeEvent(Player player, ProtectedRegion initRegion) {
        this.player = player;
        this.initRegion = initRegion;
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

    public ProtectedRegion getRegion() {
        return initRegion;
    }
}
