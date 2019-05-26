package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import org.bukkit.Bukkit;
import static org.bukkit.ChatColor.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.*;

public class AddCommand implements CommandExecutor
{
    SuspiciousPlayers plugin;
    public AddCommand(SuspiciousPlayers pl)
    {
        plugin = pl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!sender.hasPermission("susp.add"))
        {
            sender.sendMessage(RED + "You are not allowed to use that command.");
            return true;
        }

        if (args.length < 2)
        {
            sender.sendMessage(RED + "Invalid arguments.");
            sender.sendMessage(RED + "Usage: " + command.getUsage());
            return false;
        }

        final String creatorUUID =  sender instanceof Player ? ((Player)sender).getUniqueId().toString() : "console";

        final OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
        if (!op.hasPlayedBefore())
        {
            sender.sendMessage(RED + "Can not find a player by that name, make sure you are using their current username.");
            return true;
        }

        BukkitRunnable r = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                try
                {
                    String suspiciousUUID = op.getUniqueId().toString();

                    ArrayList<String> argumentList = new ArrayList<>(Arrays.asList(args));
                    argumentList.remove(0);
                    String playerEntry = String.join(" ", argumentList);

                    try(Connection c = plugin.getConnection())
                    {
                        PreparedStatement statement = c.prepareStatement(ActiveEntry.INSERT);
                        statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                        statement.setString(2, creatorUUID);
                        statement.setString(3, suspiciousUUID);
                        statement.setString(4, playerEntry);
                        statement.executeUpdate();

                        sender.sendMessage(YELLOW + "Entry added.");
                    }
                    catch (SQLException e)
                    {
                        sender.sendMessage(RED + "Error occurred while adding entry to database. " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                catch (Exception e)
                {
                    sender.sendMessage(RED + "Error occurred while preparing to add entry. " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        r.runTaskAsynchronously(plugin);
        return true;

    }

}
