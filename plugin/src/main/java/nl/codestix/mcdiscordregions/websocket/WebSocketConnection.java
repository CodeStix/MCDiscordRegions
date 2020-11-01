package nl.codestix.mcdiscordregions.websocket;

import nl.codestix.mcdiscordregions.DiscordConnection;
import nl.codestix.mcdiscordregions.websocket.messages.*;
import org.bukkit.Bukkit;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.UUID;

public class WebSocketConnection extends WebSocketClient implements DiscordConnection {

    public WebSocketConnection(URI serverUri) throws InterruptedException {
        super(serverUri);
        setTcpNoDelay(true);
        connectBlocking();
    }

    public WebSocketConnection(URI serverUri, String serverId) throws InterruptedException {
        this(serverUri);
        auth(serverId);
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
    public void onOpen(ServerHandshake handshake) {
        // Setup
    }

    @Override
    public void onMessage(String s) {
        // Process message
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
