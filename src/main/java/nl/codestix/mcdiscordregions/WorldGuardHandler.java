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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class WorldGuardHandler extends Handler {

    private MCDiscordRegionsPlugin plugin;
    private HashMap<UUID, ProtectedRegion> regionsPerPlayer = new HashMap<>();

    public WorldGuardHandler(MCDiscordRegionsPlugin plugin, Session session) {
        super(session);
        this.plugin = plugin;
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
        RegionQuery q =  WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        return q.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
    }

    public static class Factory extends Handler.Factory<WorldGuardHandler> {

        private MCDiscordRegionsPlugin plugin;

        public Factory(MCDiscordRegionsPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public WorldGuardHandler create(Session session) {
            return new WorldGuardHandler(plugin, session);
        }
    }
}
