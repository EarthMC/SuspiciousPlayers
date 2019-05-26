package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
        if(args.length < 2 || !SuspiciousPlayers.isInt(args[1]))
        {
            sender.sendMessage(RED + "Invalid arguments.");
            return false;
        }

        if(!sender.hasPermission("susp.archive"))
        {
            sender.sendMessage(RED + "You do not have permission to use this command.");
            return true;
        }

        OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
        if (!op.hasPlayedBefore())
        {
            sender.sendMessage(RED + "Can not find a player by that name, make sure you are using their current username.");
            return true;
        }

        String suspiciousUUID = op.getUniqueId().toString();
        final String archiverUUID = sender instanceof Player ? ((Player)sender).getUniqueId().toString() : "console";
        int listIndex = Integer.parseInt(args[1]);
        BukkitRunnable r = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                try(Connection c = plugin.getConnection())
                {
                    PreparedStatement selectStatement = c.prepareStatement(ActiveEntry.SELECT);
                    selectStatement.setString(1,suspiciousUUID);
                    selectStatement.setInt(2, 10000);
                    ResultSet results = selectStatement.executeQuery();

                    for (int i = 1; results.next(); i++)
                    {
                        if(i == listIndex)
                        {
                            ActiveEntry entry = new ActiveEntry(results);
                            sender.sendMessage(entry.archive(c, archiverUUID));
                            return;
                        }
                    }
                    sender.sendMessage(RED + "Can not find that entry, please use the susplist command to verify the list number.");
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
