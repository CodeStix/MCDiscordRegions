package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class JoinEventMessage extends PlayerBasedMessage {

    public String regionName;

    public JoinEventMessage(String playerUuid, String regionName) {
        super(WebSocketMessageType.JoinEvent, playerUuid);
        this.regionName = regionName;
    }
}
