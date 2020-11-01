package nl.codestix.mcdiscordregions.listener;

import net.dv8tion.jda.api.entities.Member;
import nl.codestix.mcdiscordregions.DiscordBot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {

    private DiscordPlayerListener discordPlayerListener;
    private DiscordBot bot;

    public PlayerListener(DiscordBot bot, DiscordPlayerListener listener) {
        this.bot = bot;
        this.discordPlayerListener = listener;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        bot.muteMember(event.getEntity().getUniqueId(), true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        bot.movePlayerToEntry(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        bot.muteMember(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player pl = event.getPlayer();
        Member member = bot.getMember(pl.getUniqueId());
        if (member == null) {
            if (bot.getEntryChannel() != null) {
                pl.sendMessage(String.format("§eThis server supports Discord Regions, to use this feature, " +
                        "go to the Discord server of this Minecraft server and join the §f%s§e channel. " +
                        "Then, send your Minecraft in-game name to a bot named §f%s§e in private.", bot.getEntryChannel().getName(), bot.getName()));
            }
        }
        else {
            discordPlayerListener.forceMoveDelayedAppropriateChannel(pl, member);
        }
    }
}
