package nl.codestix.mcdiscordregions;

import net.dv8tion.jda.api.entities.Member;

import java.util.UUID;

public interface IDiscordPlayerLoader {
    boolean unregisterPlayer(Member channelMember, boolean async);
    void registerPlayer(UUID playerId, Member channelMember);
}
