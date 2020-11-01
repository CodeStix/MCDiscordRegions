package nl.codestix.mcdiscordregions;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.UUID;

public class WebSocketConnection extends WebSocketClient implements DiscordConnection  {

    private String serverId;

    public WebSocketConnection(URI serverUri, String serverId) {
        super(serverUri);
    }

    @Override
    public String join(UUID uuid) {
        // Send websocket message to server
        return null;
    }

    @Override
    public void regionMove(UUID uuid, String regionName) {
        // Send websocket message to server
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
        // Process close
    }

    @Override
    public void onError(Exception e) {
        // Process error
    }
}
