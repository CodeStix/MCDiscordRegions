package nl.codestix.mcdiscordregions.event;

import net.dv8tion.jda.api.entities.Member;

import java.util.UUID;

/**
 * Events that fire when a registered (= bound with minecraft ign) Discord member joins or leaves the Minecraft regions category.
 */
public interface DiscordPlayerEvents {
    void onDiscordPlayerLeave(UUID playerId, Member channelMember);
    void onDiscordPlayerJoin(UUID playerId, Member channelMember);
}
