package nl.codestix.mcdiscordregions.command;

import nl.codestix.mcdiscordregions.DiscordConnection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NoDiscordCommand implements CommandExecutor {

    private DiscordConnection connection;

    public NoDiscordCommand(DiscordConnection connection) {
        this.connection = connection;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if (!commandSender.hasPermission("discordregions.unbound"))
            return false;
        if (!(commandSender instanceof Player))
            return false;

        Player pl = (Player)commandSender;
        pl.sendMessage("§dDisconnected your Minecraft account from Discord.");
        connection.unbind(pl.getUniqueId());
        return true;
    }
}
