package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class RegionMoveMessage extends PlayerBasedMessage {

    public String regionName;

    public RegionMoveMessage(String playerUuid, String regionName) {
        super(WebSocketMessageType.RegionMoveEvent, playerUuid);
        this.regionName = regionName;
    }
}
