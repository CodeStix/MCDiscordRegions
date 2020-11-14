package nl.codestix.mcdiscordregions;

import java.util.List;

public class Region {

    public String name;
    public int limit;
    public List<String> playerUuids;

    public Region(String name, int limit, List<String> playerUuids) {
        this.name = name;
        this.limit = limit;
        this.playerUuids = playerUuids;
    }

}