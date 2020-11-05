package nl.codestix.mcdiscordregions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;
import nl.codestix.mcdiscordregions.event.RegionChangeEvent;
import nl.codestix.mcdiscordregions.event.RegionInitializeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class WorldGuardHandler extends Handler {

    private static HashMap<UUID, ProtectedRegion> regionsPerPlayer = new HashMap<>();

    public WorldGuardHandler(Session session) {
        super(session);
    }

    @Override
    public void initialize(LocalPlayer player, Location current, ApplicableRegionSet set) {
        ProtectedRegion region = set.size() == 0 ? null : set.iterator().next();
        regionsPerPlayer.put(player.getUniqueId(), region);
        RegionInitializeEvent ev = new RegionInitializeEvent(Bukkit.getPlayer(player.getUniqueId()), region);
        Bukkit.getPluginManager().callEvent(ev);
        super.initialize(player, current, set);
    }

    @Override
    public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType) {
        ProtectedRegion region = toSet.size() == 0 ? null : toSet.iterator().next();
        ProtectedRegion currentRegion = regionsPerPlayer.get(player.getUniqueId());
        if (region != currentRegion) {
            RegionChangeEvent ev = new RegionChangeEvent(player.getUniqueId(), Bukkit.getPlayer(player.getUniqueId()), currentRegion, region, toSet);
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled())
                return false;

            regionsPerPlayer.put(player.getUniqueId(), region);
        }
        return super.onCrossBoundary(player, from, to, toSet, entered, exited, moveType);
    }

    public static ApplicableRegionSet getPlayerRegions(Player player) {
        RegionQuery q = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        return q.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
    }

    public static ProtectedRegion getPlayerRegion(Player player) {
        return regionsPerPlayer.get(player.getUniqueId());
    }

    public static class Factory extends Handler.Factory<WorldGuardHandler> {

        @Override
        public WorldGuardHandler create(Session session) {
            return new WorldGuardHandler(session);
        }
    }
}
