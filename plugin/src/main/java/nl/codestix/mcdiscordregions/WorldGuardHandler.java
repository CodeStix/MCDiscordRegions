package nl.codestix.mcdiscordregions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
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

    private static HashMap<UUID, ApplicableRegionSet> regionsPerPlayer = new HashMap<>();

    public WorldGuardHandler(Session session) {
        super(session);
    }

    @Override
    public void initialize(LocalPlayer player, Location current, ApplicableRegionSet set) {
        regionsPerPlayer.put(player.getUniqueId(), set);
        RegionInitializeEvent ev = new RegionInitializeEvent(Bukkit.getPlayer(player.getUniqueId()), set);
        Bukkit.getPluginManager().callEvent(ev);
        super.initialize(player, current, set);
    }

    public static <T> T queryFlag(Player player, ApplicableRegionSet set, Flag<T> flag) {
        return set.queryValue(WorldGuardPlugin.inst().wrapPlayer(player), flag);
    }

    public static <T> T queryFlag(Player player, Flag<T> flag) {
        return queryFlag(player, getPlayerRegions(player), flag);
    }

    @Override
    public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType) {
        RegionChangeEvent ev = new RegionChangeEvent(player.getUniqueId(), Bukkit.getPlayer(player.getUniqueId()), toSet);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled())
            return false;

        regionsPerPlayer.put(player.getUniqueId(), toSet);
        return super.onCrossBoundary(player, from, to, toSet, entered, exited, moveType);
    }

    public static ApplicableRegionSet getPlayerRegions(Player player) {
        return regionsPerPlayer.get(player.getUniqueId());
        //RegionQuery q = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        //return q.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
    }


    public static class Factory extends Handler.Factory<WorldGuardHandler> {

        @Override
        public WorldGuardHandler create(Session session) {
            return new WorldGuardHandler(session);
        }
    }
}
