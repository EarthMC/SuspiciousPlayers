package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import com.karlofduty.SuspiciousPlayers.models.ArchivedEntry;
import com.karlofduty.SuspiciousPlayers.models.PlayerEntry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.*;

import static org.bukkit.ChatColor.*;

public class ListCommand implements CommandExecutor
{
    SuspiciousPlayers plugin;
    public ListCommand(SuspiciousPlayers pl)
    {
        plugin = pl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(RED + "Invalid arguments.");
            return false;
        }

        if(!sender.hasPermission("susp.list"))
        {
            sender.sendMessage(RED + "You do not have permission to use this command.");
            return true;
        }

        OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
        if (!op.hasPlayedBefore())
        {
            sender.sendMessage(RED + "Can not find a player by that name, make sure you are using their current username.");
            return false;
        }

        String suspiciousUUID = op.getUniqueId().toString();
        int maxEntries = args.length >= 2 && SuspiciousPlayers.isInt(args[1]) ? Integer.parseInt(args[1]) : 10;
        BukkitRunnable r = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                try(Connection c = SuspiciousPlayers.instance.getConnection())
                {
                    ArrayList<PlayerEntry> entries = new ArrayList<>();

                    // Reads all active entries about the player
                    PreparedStatement statement = c.prepareStatement(ActiveEntry.SELECT);
                    statement.setString(1, suspiciousUUID);
                    statement.setInt(2, maxEntries);
                    ResultSet activeResults = statement.executeQuery();

                    while(activeResults.next())
                    {
                        entries.add(new ActiveEntry(activeResults));
                    }

                    // Reads archived entries about the player if there are still spots open in the list
                    if(entries.size() < maxEntries)
                    {

                        statement = c.prepareStatement(ArchivedEntry.SELECT);
                        statement.setString(1, suspiciousUUID);
                        statement.setInt(2, maxEntries - entries.size());
                        ResultSet archiveResults = statement.executeQuery();

                        while(archiveResults.next())
                        {
                            entries.add(new ArchivedEntry(archiveResults));
                        }
                    }

                    // Send feedback message if there are no entries
                    if(entries.isEmpty())
                    {
                        sender.sendMessage(RED + "User does not have any entries.");
                        return;
                    }

                    // Builds the message and sends it
                    StringBuilder sb = new StringBuilder();
                    for (PlayerEntry entry : entries)
                    {
                        sb.append(entry.getFormattedString());
                    }
                    String message = sb.toString();
                    if(message.endsWith(" \n"))
                    {
                        message = message.substring(0,message.length() - 2);
                    }
                    sender.sendMessage(GOLD + "----- Displaying (Max: " + YELLOW + maxEntries + GOLD + ") entries for " + YELLOW + op.getName() + GOLD + " -----\n" + message);

                }
                catch (SQLException e)
                {
                    sender.sendMessage(RED + "Error occurred while listing entries. " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        r.runTaskAsynchronously(plugin);
        return true;
    }

    //private String buildMessage(ArrayList<String> active, ArrayList<String> archived, int max)
    //{
    //    StringBuilder message = new StringBuilder();
//
    //    int i = 1;
    //    for(;i <= max && i <= active.size(); i++)
    //    {
    //        message.append(active.get(active.size()));
    //    }
//
    //    for(; i <= max && i <= archived.size(); i++)
    //    {
    //        message.append(archived.get(i - 1));
    //    }
//
    //    return message.toString();
    //}

    //private ArrayList<String> formatActiveEntries(ResultSet results) throws SQLException
    //{
    //    ArrayList<String> entries = new ArrayList<>();
    //    for(int i = 1; results.next(); i++)
    //    {
    //        String username = getUsername(results.getString("creator_uuid"));
//
    //        entries.add(buildEntryString(results.getTimestamp("created_time"), username, results.getString("entry"), i));
    //    }
    //    return entries;
    //}
//
    //private ArrayList<String> formatArchivedEntries(ResultSet results) throws SQLException
    //{
    //    ArrayList<String> entries = new ArrayList<>();
    //    for(int i = 1; results.next(); i++)
    //    {
    //        String creatorUsername = getUsername(results.getString("creator_uuid"));
    //        String archiverUsername = getUsername(results.getString("archiver_uuid"));
//
    //        entries.add(buildArchivedEntryString(results.getTimestamp("archived_time"), archiverUsername, results.getTimestamp("created_time"), creatorUsername, results.getString("entry"), i));
    //    }
    //    return entries;
    //}
}
