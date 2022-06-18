package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ArchivedEntry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;

import static org.bukkit.ChatColor.*;

public class DeleteCommand implements CommandExecutor
{
    private SuspiciousPlayers plugin;

    public DeleteCommand(SuspiciousPlayers pl)
    {
        plugin = pl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length < 1 || !SuspiciousPlayers.isInt(args[0]))
        {
            sender.sendMessage(RED + "Invalid arguments. You are not supposed to use this command, it is automatically called from /susplist.");
            return false;
        }

        if (!sender.hasPermission("susp.delete"))
        {
            sender.sendMessage(RED + "You do not have permission to use this command.");
            return true;
        }

        final String deleterUUID = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "console";
        final int id = Integer.parseInt(args[0]);
        BukkitRunnable r = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                try (Connection c = plugin.getConnection())
                {
                    ArchivedEntry entry = ArchivedEntry.select(c, id);

                    if(entry == null)
                    {
                        sender.sendMessage(RED + "Invalid ID, does that entry still exist?");
                        return;
                    }

                    sender.sendMessage(entry.delete(c, deleterUUID));
                }
                catch (SQLException e)
                {
                    sender.sendMessage(RED + "Error occurred while loading entries from database: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        r.runTaskAsynchronously(plugin);
        return true;
    }
}