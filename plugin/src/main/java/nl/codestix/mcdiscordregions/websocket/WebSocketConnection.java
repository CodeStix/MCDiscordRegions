package nl.codestix.mcdiscordregions.websocket;

import com.google.gson.Gson;
import nl.codestix.mcdiscordregions.DiscordConnection;
import nl.codestix.mcdiscordregions.DiscordEvents;
import nl.codestix.mcdiscordregions.websocket.messages.*;
import org.bukkit.Bukkit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.UUID;

public class WebSocketConnection extends WebSocketClient implements DiscordConnection {

    private DiscordEvents listener;

    public WebSocketConnection(URI serverUri, DiscordEvents listener) throws InterruptedException {
        super(serverUri);
        setTcpNoDelay(true);
        connectBlocking();
        this.listener = listener;
    }

    public WebSocketConnection(URI serverUri, DiscordEvents listener, String serverId) throws InterruptedException {
        this(serverUri, listener);
        auth(serverId);
    }

    public static WebSocketMessage fromJSON(String json) {
        Gson gson = new Gson();
        WebSocketMessage base = gson.fromJson(json, WebSocketMessage.class);
        switch (base.action) {
            case Auth:
                return gson.fromJson(json, AuthMessage.class);
            case Move:
                return gson.fromJson(json, MoveMessage.class);
            case Join:
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
    public void auth(String serverId) {
        send(new AuthMessage(serverId));
    }

    @Override
    public void join(UUID uuid) {
        send(new PlayerBasedMessage(WebSocketMessageType.Join, uuid.toString()));
    }

    @Override
    public void left(UUID uuid) {
        send(new PlayerBasedMessage(WebSocketMessageType.Left, uuid.toString()));
    }

    @Override
    public void death(UUID uuid) {
        send(new PlayerBasedMessage(WebSocketMessageType.Death, uuid.toString()));
    }

    @Override
    public void respawn(UUID uuid) {
        send(new PlayerBasedMessage(WebSocketMessageType.Respawn, uuid.toString()));
    }

    @Override
    public void regionMove(UUID uuid, String regionName) {
        send(new MoveMessage(uuid.toString(), regionName));
    }

    @Override
    public void limitRegion(String regionName, int limit) {
        send(new LimitMessage(regionName, limit));
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        // Setup
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
        else if (message instanceof MoveMessage) {
            MoveMessage moveMessage = (MoveMessage)message;
            listener.regionLimitReached(UUID.fromString(moveMessage.playerUuid), moveMessage.regionName);
        }
        else if (message instanceof PlayerBasedMessage) {
            PlayerBasedMessage playerMessage = (PlayerBasedMessage)message;
            UUID id = UUID.fromString(playerMessage.playerUuid);
            switch (playerMessage.action) {
                case Join:
                    listener.userJoined(id);
                    break;
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
