package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class AuthMessage extends WebSocketMessage {

    public String serverId;

    public AuthMessage(String serverId) {
        super(WebSocketMessageType.Auth);
        this.serverId = serverId;
    }
}
