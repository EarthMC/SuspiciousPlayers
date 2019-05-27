package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;

import static org.bukkit.ChatColor.*;

public class ArchiveCommand implements CommandExecutor
{
    private SuspiciousPlayers plugin;
    public ArchiveCommand(SuspiciousPlayers pl)
    {
        plugin = pl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length < 1 || !SuspiciousPlayers.isInt(args[0]))
        {
            sender.sendMessage(RED + "Invalid arguments. You are not supposed to use this command, it is automatically called from /susplist.");
            return false;
        }

        if(!sender.hasPermission("susp.archive"))
        {
            sender.sendMessage(RED + "You do not have permission to use this command.");
            return true;
        }

        final String archiverUUID = sender instanceof Player ? ((Player)sender).getUniqueId().toString() : "console";
        int id = Integer.parseInt(args[0]);
        BukkitRunnable r = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                try(Connection c = plugin.getConnection())
                {
                    ActiveEntry entry = ActiveEntry.select(c, id);

                    if(entry == null)
                    {
                        sender.sendMessage("Invalid ID, does that entry still exist?");
                        return;
                    }

                    sender.sendMessage(entry.archive(c, archiverUUID));
                }
                catch (SQLException e)
                {
                    sender.sendMessage(RED + "Archive command sql error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        r.runTaskAsynchronously(plugin);
        return true;
    }
}
