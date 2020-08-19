package nl.codestix.mcdiscordregions.listener;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StringFlag;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import nl.codestix.mcdiscordregions.DiscordBot;
import nl.codestix.mcdiscordregions.event.DiscordPlayerEvents;
import nl.codestix.mcdiscordregions.WorldGuardHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class DiscordPlayerListener implements DiscordPlayerEvents {

    private StringFlag discordChannelFlag;
    private DiscordBot bot;
    private JavaPlugin plugin;
    private boolean useWhitelist = false;

    public boolean kickOnDiscordLeave = true;
    public String kickOnDiscordLeaveMessage = "Not registered.";

    public DiscordPlayerListener(JavaPlugin plugin, DiscordBot bot, StringFlag discordChannelFlag) {
        this.plugin = plugin;
        this.bot = bot;
        this.discordChannelFlag = discordChannelFlag;
    }

    public boolean getUseWhitelist() {
        return useWhitelist;
    }

    public void setUseWhitelist(boolean useWhitelist) {
        if (this.useWhitelist && !useWhitelist)
            for(UUID pl : bot.getCurrentDiscordPlayers().keySet())
                Bukkit.getOfflinePlayer(pl).setWhitelisted(false);

        this.useWhitelist = useWhitelist;
    }

    @Override
    public void onDiscordPlayerJoin(UUID playerId, Member channelMember) {
        if (useWhitelist)
            Bukkit.getOfflinePlayer(playerId).setWhitelisted(true);

        Player pl = Bukkit.getPlayer(playerId);
        if (pl != null)
            forceMoveDelayedAppropriateChannel(pl, channelMember);
    }

    @Override
    public void onDiscordPlayerLeave(UUID playerId, Member channelMember) {
        if (playerId == null)
            return;

        if (useWhitelist)
            Bukkit.getOfflinePlayer(playerId).setWhitelisted(false);

        if (kickOnDiscordLeave) {
            Player pl = Bukkit.getPlayer(playerId);
            if (pl != null)
            {
                if (Bukkit.isPrimaryThread())
                    pl.kickPlayer(kickOnDiscordLeaveMessage);
                else
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> pl.kickPlayer(kickOnDiscordLeaveMessage));
            }
        }
    }

    public void forceMoveDelayedAppropriateChannel(Player pl, Member member) {
        ApplicableRegionSet set = WorldGuardHandler.getPlayerRegions(pl);
        String channelName = set.queryValue(null, discordChannelFlag);
        if (channelName == null) {
            bot.forceMoveDelayed(plugin, member, bot.getEntryChannel());
            plugin.getLogger().warning("No global Discord channel defined, use '/region flag __global__ discord-channel Global' to set the global Discord channel to 'Global'.");
            return;
        }

        VoiceChannel vc = bot.getChannelByName(channelName);
        if (vc == null)
            bot.createNormalChannel(channelName, nvc -> bot.forceMoveDelayed(plugin, member, nvc));
        else
            bot.forceMoveDelayed(plugin, member, vc);
    }
}
