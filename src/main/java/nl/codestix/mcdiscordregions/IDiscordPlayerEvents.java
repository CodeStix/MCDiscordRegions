package nl.codestix.mcdiscordregions;

import net.dv8tion.jda.api.entities.Member;

import java.util.UUID;

public interface IDiscordPlayerEvents {
    boolean onDiscordPlayerLeave(Member channelMember);
    void onDiscordPlayerJoin(UUID playerId, Member channelMember);
}
