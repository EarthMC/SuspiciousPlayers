package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class AddCommand implements CommandExecutor
{
    SuspiciousPlayers plugin;
    public AddCommand(SuspiciousPlayers pl)
    {
        plugin = pl;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args)
    {
        //TODO: Make async
        if (commandSender.hasPermission("susp.add"))
        {
            if (args.length > 1)
            {
                String creatorUUID = "console";
                if(commandSender instanceof Player)
                {
                    creatorUUID = ((Player)commandSender).getUniqueId().toString();
                }
                String username = args[0];
                OfflinePlayer op = Bukkit.getOfflinePlayer(username);
                if (op.hasPlayedBefore())
                {
                    String suspiciousUUID = op.getUniqueId().toString();

                    ArrayList<String> argumentList = new ArrayList<>(Arrays.asList(args));
                    argumentList.remove(0);
                    String playerEntry = String.join(" ", argumentList);

                    try
                    {
                        Statement statement = plugin.connection.createStatement();
                        statement.executeUpdate("INSERT INTO active_entries(creator_uuid, suspicious_uuid, entry) VALUES ('" + creatorUUID + "', '" + suspiciousUUID + "', '" + playerEntry + "' )");
                    }
                    catch (SQLException e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    commandSender.sendMessage(ChatColor.RED + "Can not find a player by that name, make sure you are using their current username.");
                }
            }
            else
            {
                commandSender.sendMessage(ChatColor.RED + "Invalid arguments.");
            }
        }
        else
        {
            commandSender.sendMessage(ChatColor.RED + "You are not allowed to use that command.");
        }
        return true;
    }
}
