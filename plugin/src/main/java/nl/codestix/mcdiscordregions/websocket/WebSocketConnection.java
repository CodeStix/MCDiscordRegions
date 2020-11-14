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
            case SyncRequest:
                return gson.fromJson(json, SyncRequestMessage.class);
            case Move:
                return gson.fromJson(json, MoveMessage.class);
            case SyncResponse:
                return gson.fromJson(json, SyncResponseMessage.class);
            case JoinEvent:
                return gson.fromJson(json, JoinEventMessage.class);
            case Left:
            case Respawn:
            case Death:
            case Bound:
                return gson.fromJson(json, PlayerBasedMessage.class);
            case RequireUser:
                return gson.fromJson(json, RequireUserMessage.class);
            case Limit:
                return gson.fromJson(json, LimitMessage.class);
            default:
                return null;
        }
    }

    private void send(WebSocketMessage message) {
        send(message.toJSON());
    }

    @Override
    public void playerJoin(UUID uuid, String regionName) {
        trackedPlayers.add(uuid);
        getOrCreateRegion(regionName).playerUuids.add(uuid.toString());
        send(new JoinEventMessage(uuid.toString(), regionName));
    }

    @Override
    public void playerLeave(UUID uuid) {
        trackedPlayers.remove(uuid);
        getPlayerRegion(uuid.toString()).playerUuids.remove(uuid.toString());
        send(new PlayerBasedMessage(WebSocketMessageType.Left, uuid.toString()));
    }

    @Override
    public void playerDeath(UUID uuid) {
        if (!trackedPlayers.contains(uuid))
            return;
        send(new PlayerBasedMessage(WebSocketMessageType.Death, uuid.toString()));
    }

    @Override
    public void playerRespawn(UUID uuid) {
        if (!trackedPlayers.contains(uuid))
            return;
        send(new PlayerBasedMessage(WebSocketMessageType.Respawn, uuid.toString()));
    }

    @Override
    public void playerRegionMove(UUID uuid, String regionName) {
        if (!trackedPlayers.contains(uuid))
            return;
        String id = uuid.toString();
        getPlayerRegion(id).playerUuids.remove(id);
        getOrCreateRegion(regionName).playerUuids.add(id);
        send(new MoveMessage(id, regionName));
    }

    @Override
    public void limitRegion(String regionName, int limit) {
        send(new LimitMessage(regionName, limit));
    }

    @Override
    public void unbind(UUID uuid) {
        send(new UnBindMessage(uuid.toString()));
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
        if (message instanceof RequireUserMessage) {
            RequireUserMessage requireUserMessage = (RequireUserMessage)message;
            listener.userRequired(UUID.fromString(requireUserMessage.playerUuid), requireUserMessage.key);
        }
        else if (message instanceof LimitMessage) {
            LimitMessage limitMessage = (LimitMessage)message;
            if (limitMessage.limit < 0)
                listener.regionLimitFailed(limitMessage.regionName);
            else
                listener.regionGotLimited(limitMessage.regionName, limitMessage.limit);
        }
        else if (message instanceof JoinEventMessage) {
            JoinEventMessage joinMessage = (JoinEventMessage)message;
            listener.userJoined(UUID.fromString(joinMessage.playerUuid), joinMessage.regionName);
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
                case Left:
                    listener.userLeft(id);
                    break;
                case Bound:
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
