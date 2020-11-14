package nl.codestix.mcdiscordregions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Region {

    public String name;
    public int limit;
    public Set<String> playerUuids;

    public Region(String name) {
        this.name = name;
        this.limit = 0;
        this.playerUuids = new HashSet<>();
    }

    public Region(String name, int limit, Set<String> playerUuids) {
        this.name = name;
        this.limit = limit;
        this.playerUuids = playerUuids;
    }

}
