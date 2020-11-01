package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class MoveMessage extends PlayerBasedMessage {

    public String regionName;

    public MoveMessage(String playerUuid, String regionName) {
        super(WebSocketMessageType.Move, playerUuid);
        this.regionName = regionName;
    }
}
