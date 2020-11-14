package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class JoinRequireUserResponseMessage extends PlayerBasedMessage {

    public String key;

    public JoinRequireUserResponseMessage(String playerUuid, String key) {
        super(WebSocketMessageType.JoinRequireUserResponse, playerUuid);
        this.key = key;
    }
}
