package nl.codestix.mcdiscordregions.websocket;

import com.google.gson.Gson;
import nl.codestix.mcdiscordregions.DiscordConnection;
import nl.codestix.mcdiscordregions.DiscordEvents;
import nl.codestix.mcdiscordregions.Region;
import nl.codestix.mcdiscordregions.websocket.messages.*;
import org.bukkit.Bukkit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.*;

public class WebSocketConnection extends WebSocketClient implements DiscordConnection {

    private DiscordEvents listener;
    private List<Region> regions = new ArrayList<>();
    private Set<UUID> trackedPlayers = new HashSet<>();
    private String serverId;

    public WebSocketConnection(URI serverUri, DiscordEvents listener, String serverId) throws InterruptedException {
        super(serverUri);
        this.listener = listener;
        this.serverId = serverId;
        setTcpNoDelay(true);
        connectBlocking();
    }

    public static WebSocketMessage fromJSON(String json) {
        Gson gson = new Gson();
        WebSocketMessage base = gson.fromJson(json, WebSocketMessage.class);
        switch (base.action) {
            case RegionMoveEvent:
                return gson.fromJson(json, RegionMoveMessage.class);
            case SyncResponse:
                return gson.fromJson(json, SyncResponseMessage.class);
            case JoinEvent:
                return gson.fromJson(json, JoinEventMessage.class);
            case LeaveEvent:
            case RespawnEvent:
            case DeathEvent:
            case BoundEvent:
                return gson.fromJson(json, PlayerBasedMessage.class);
            case JoinRequireUserResponse:
                return gson.fromJson(json, JoinRequireUserResponseMessage.class);
            default:
                return null;
        }
    }

    private void send(WebSocketMessage message) {
        send(message.toJSON());
    }

    @Override
    public void playerJoin(UUID uuid, String regionName) {
        send(new JoinEventMessage(uuid.toString(), regionName));
    }

    @Override
    public void playerLeave(UUID uuid) {
        if (!trackedPlayers.contains(uuid))
            return;
        send(new PlayerBasedMessage(WebSocketMessageType.LeaveEvent, uuid.toString()));
    }

    @Override
    public void playerDeath(UUID uuid) {
        if (!trackedPlayers.contains(uuid))
            return;
        send(new PlayerBasedMessage(WebSocketMessageType.DeathEvent, uuid.toString()));
    }

    @Override
    public void playerRespawn(UUID uuid) {
        if (!trackedPlayers.contains(uuid))
            return;
        send(new PlayerBasedMessage(WebSocketMessageType.RespawnEvent, uuid.toString()));
    }

    @Override
    public void playerRegionMove(UUID uuid, String regionName) {
        if (!trackedPlayers.contains(uuid))
            return;
        String id = uuid.toString();
        getPlayerRegion(id).playerUuids.remove(id);
        getOrCreateRegion(regionName).playerUuids.add(id);
        send(new RegionMoveMessage(id, regionName));
    }

    @Override
    public void pruneRegions() {
        send(new WebSocketMessage(WebSocketMessageType.PruneRequest));
        this.regions.removeIf((region) -> region.playerUuids.size() == 0);
    }

    @Override
    public boolean limitRegion(String regionName, int limit) {
        Region region = getRegion(regionName);
        if (region == null)
            return false;
        region.limit = limit;
        send(new LimitRequestMessage(regionName, limit));
        return true;
    }

    @Override
    public void unbind(UUID uuid) {
        send(new UnBindRequestMessage(uuid.toString()));
    }

    private Region getRegion(String regionName) {
        for(Region region : regions)
            if (region.name.equalsIgnoreCase(regionName))
                return region;
        return null;
    }

    @Override
    public Region getOrCreateRegion(String regionName) {
        for(Region region : regions)
            if (region.name.equalsIgnoreCase(regionName))
                return region;
        Region newRegion = new Region(regionName);
        regions.add(newRegion);
        return newRegion;
    }

    @Override
    public Collection<Region> getRegions() {
        return regions;
    }

    public Region getPlayerRegion(String playerUuid) {
        for(Region region : regions)
            if (region.playerUuids.contains(playerUuid))
                return region;
        return null;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        send(new SyncRequestMessage(serverId));
    }

    @Override
    public void onMessage(String s) {
        WebSocketMessage message = fromJSON(s);
        if (message instanceof JoinRequireUserResponseMessage) {
            JoinRequireUserResponseMessage requireUserMessage = (JoinRequireUserResponseMessage)message;
            listener.userRequired(UUID.fromString(requireUserMessage.playerUuid), requireUserMessage.key);
        }
        else if (message instanceof JoinEventMessage) {
            JoinEventMessage joinMessage = (JoinEventMessage)message;
            UUID id = UUID.fromString(joinMessage.playerUuid);
            trackedPlayers.add(id);
            getOrCreateRegion(joinMessage.regionName).playerUuids.add(id.toString());
            listener.userJoined(id, joinMessage.regionName);
        }
        else if (message instanceof SyncResponseMessage) {
            SyncResponseMessage syncMessage = (SyncResponseMessage)message;
            this.regions = syncMessage.regions;
            this.trackedPlayers.clear();
            for(Region region : syncMessage.regions) {
                for(String uuid : region.playerUuids)
                    this.trackedPlayers.add(UUID.fromString(uuid));
            }
        }
        else if (message instanceof PlayerBasedMessage) {
            PlayerBasedMessage playerMessage = (PlayerBasedMessage)message;
            UUID id = UUID.fromString(playerMessage.playerUuid);
            switch (playerMessage.action) {
                case LeaveEvent:
                    trackedPlayers.remove(id);
                    getPlayerRegion(id.toString()).playerUuids.remove(id.toString());
                    listener.userLeft(id);
                    break;
                case BoundEvent:
                    listener.userBound(id);
                    break;
            }
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        Bukkit.getLogger().warning("Connection lost with Discord bot.");
    }

    @Override
    public void onError(Exception e) {
        Bukkit.getLogger().severe("Discord bot connection exception: " + e.getMessage());
    }
}
