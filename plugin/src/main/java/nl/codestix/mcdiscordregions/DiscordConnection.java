package nl.codestix.mcdiscordregions;

import java.util.Collection;
import java.util.UUID;

public interface DiscordConnection {
    void playerJoin(UUID uuid, String regionName);
    void playerLeave(UUID uuid);
    void playerDeath(UUID uuid);
    void playerRespawn(UUID uuid);
    void playerRegionMove(UUID uuid, String regionName);
    void limitRegion(String regionName, int limit);
    void unbind(UUID uuid);
    void close();
    Region getOrCreateRegion(String regionName);
    Collection<Region> getRegions();
}
