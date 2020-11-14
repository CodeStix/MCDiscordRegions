package nl.codestix.mcdiscordregions;

import java.util.ArrayList;
import java.util.List;

public class Region {

    public String name;
    public int limit;
    public List<String> playerUuids;

    public Region(String name) {
        this.name = name;
        this.limit = 0;
        this.playerUuids = new ArrayList<>();
    }

    public Region(String name, int limit, List<String> playerUuids) {
        this.name = name;
        this.limit = limit;
        this.playerUuids = playerUuids;
    }

}
