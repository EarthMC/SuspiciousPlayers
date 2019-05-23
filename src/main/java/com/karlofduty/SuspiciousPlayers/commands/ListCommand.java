package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
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
        BukkitRunnable r = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                int max = 10;
                if(args.length >= 2 && SuspiciousPlayers.isInt(args[1]))
                {
                    max = Integer.parseInt(args[1]);
                }

                try(Connection c = SuspiciousPlayers.instance.getConnection())
                {
                    // Reads all active entries about the player
                    ResultSet activeListResults = c.createStatement().executeQuery("SELECT * FROM active_entries WHERE suspicious_uuid = '" + suspiciousUUID + "' ORDER BY created_time;");
                    ArrayList<String> activeEntries = formatActiveEntries(activeListResults);

                    // Read archived entries if the active ones didnt meet the maximum to be displayed
                    ArrayList<String> archivedEntries = new ArrayList<>();
                    if(activeEntries.size() < max)
                    {
                        ResultSet archivedListResults = c.createStatement().executeQuery("SELECT * FROM archived_entries WHERE suspicious_uuid = '" + suspiciousUUID + "' ORDER BY created_time;");
                        archivedEntries = formatArchivedEntries(archivedListResults);
                    }

                    // Send feedback message if there are no entries
                    if(activeEntries.isEmpty() && archivedEntries.isEmpty())
                    {
                        sender.sendMessage(RED + "User does not have any entries.");
                        return;
                    }

                    // Builds the message and sends it
                    String list = buildMessage(activeEntries, archivedEntries, max);
                    sender.sendMessage(GOLD + "Displaying (Max: " + YELLOW + max + GOLD + ") entries for " + RED + op.getName() + GOLD + ":\n" + list);

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

    private String buildMessage(ArrayList<String> active, ArrayList<String> archived, int max)
    {
        StringBuilder message = new StringBuilder();

        int i = 1;
        for(;i <= max && i <= active.size(); i++)
        {
            message.append(active.get(active.size() - i));
        }

        for(; i <= max && i <= archived.size(); i++)
        {
            message.append(archived.get(archived.size() - i));
        }

        return message.toString();
    }

    private ArrayList<String> formatActiveEntries(ResultSet results) throws SQLException
    {
        ArrayList<String> entries = new ArrayList<>();
        while(results.next())
        {
            String username = getUsername(results.getString("creator_uuid"));

            entries.add(buildEntryString(results.getTimestamp("created_time"), username, results.getString("entry")));
        }
        return entries;
    }

    private ArrayList<String> formatArchivedEntries(ResultSet results) throws SQLException
    {
        ArrayList<String> entries = new ArrayList<>();
        while(results.next())
        {
            String creatorUsername = getUsername(results.getString("creator_uuid"));
            String archiverUsername = getUsername(results.getString("archiver_uuid"));

            entries.add(buildArchivedEntryString(results.getTimestamp("archived_time"), archiverUsername, results.getTimestamp("created_time"), creatorUsername, results.getString("entry")));
        }
        return entries;
    }

    public static String buildEntryString(Timestamp createdTime, String creatorName, String entry)
    {
        return GREEN + "Reported: [" + SuspiciousPlayers.displayDateFormat.format(createdTime) + "] UTC by " + GOLD + creatorName + ":\n" +
                YELLOW + entry + '\n';
    }
    private static String buildArchivedEntryString(Timestamp archivedTime, String archiverName, Timestamp createdTime, String creatorName, String entry)
    {
        return DARK_GRAY + "Archived: [" + SuspiciousPlayers.displayDateFormat.format(archivedTime) + "] UTC by " + archiverName + ":\n" +
                GRAY + "Reported: " + SuspiciousPlayers.displayDateFormat.format(createdTime) + " UTC by " + creatorName + ":\n" +
                GRAY + entry + '\n';
    }
    private static String getUsername(String uuid)
    {
        try
        {
            return Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
        }
        catch (Exception e)
        {
            return uuid;
        }
    }
}
