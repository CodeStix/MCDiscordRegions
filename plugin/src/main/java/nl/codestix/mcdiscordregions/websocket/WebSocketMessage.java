package nl.codestix.mcdiscordregions.websocket;

import com.google.gson.Gson;

public class WebSocketMessage {
    public String serverId;
    public WebSocketMessageType action;

    public WebSocketMessage(String serverId, WebSocketMessageType action) {
        this.serverId = serverId;
        this.action = action;
    }

    public static WebSocketMessage fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, WebSocketMessage.class);
    }

    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
