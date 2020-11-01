package nl.codestix.mcdiscordregions;

import com.google.gson.Gson;

public class RegionMessage {
    public String serverId;
    public RegionMessageType action;

    public RegionMessage(String serverId, RegionMessageType action) {
        this.serverId = serverId;
        this.action = action;
    }

    public static RegionMessage fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, RegionMessage.class);
    }

    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
