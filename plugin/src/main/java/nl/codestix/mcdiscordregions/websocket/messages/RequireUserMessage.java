package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class RequireUserMessage extends PlayerBasedMessage {

    public String key;

    public RequireUserMessage(String playerUuid, String key) {
        super(WebSocketMessageType.RequireUser, playerUuid);
        this.key = key;
    }
}
