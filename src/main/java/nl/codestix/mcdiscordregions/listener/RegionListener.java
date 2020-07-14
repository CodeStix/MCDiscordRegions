package nl.codestix.mcdiscordregions.listener;

import com.sk89q.worldguard.protection.flags.StringFlag;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import nl.codestix.mcdiscordregions.DiscordBot;
import nl.codestix.mcdiscordregions.event.RegionChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class RegionListener implements Listener {

    private JavaPlugin plugin;
    private DiscordBot bot;
    private StringFlag discordChannelFlag;

    public RegionListener(JavaPlugin plugin, DiscordBot bot, StringFlag discordChannelFlag) {
        this.plugin = plugin;
        this.bot = bot;
        this.discordChannelFlag = discordChannelFlag;
    }

    @EventHandler
    public void onRegionChange(RegionChangeEvent event) {
        UUID id = event.getId();
        Member member = bot.getMember(id);
        if (member == null || !bot.isInVoiceChannel(member))
            return;

//        Bukkit.getLogger().info(event.getPlayer().getName() + " entered new region: " + (event.getEnteredRegion() == null ? "(null)" : event.getEnteredRegion().getId()));

        String channelName = event.getRegionSet().queryValue(null, discordChannelFlag);
        if (channelName == null) {
            bot.forceMoveDelayed(plugin, member, bot.getEntryChannel());
            plugin.getLogger().warning("No global Discord channel defined, use '/region flag __global__ discord-channel Global' to set the global Discord channel to 'Global'.");
            return;
        }

        VoiceChannel vc = bot.getChannelByName(channelName);
        if (vc == null) {
            bot.createChannel(channelName, c -> {
                plugin.getLogger().info("Created new voice channel");
                bot.forceMoveDelayed(plugin, member, c);
            });
        }
        else {
            if (!bot.tryMoveDelayed(plugin, member, vc)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(String.format("§cYou can not enter %s, it has a limit of %d players.", channelName, vc.getUserLimit()));
            }
        }
    }
}
