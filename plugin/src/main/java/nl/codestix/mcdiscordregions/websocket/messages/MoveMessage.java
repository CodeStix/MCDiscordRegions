package nl.codestix.mcdiscordregions.websocket.messages;

import nl.codestix.mcdiscordregions.websocket.WebSocketMessage;
import nl.codestix.mcdiscordregions.websocket.WebSocketMessageType;

public class MoveMessage extends WebSocketMessage {

    public String playerUuid;
    public String regionName;

    public MoveMessage(String serverId, String playerUuid, String regionName) {
        super(serverId, WebSocketMessageType.Move);
        this.regionName = regionName;
        this.playerUuid = playerUuid;
    }
}
