package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.Date;

import static org.bukkit.ChatColor.*;

public class UnarchiveCommand implements CommandExecutor
{
	private SuspiciousPlayers plugin;
	public UnarchiveCommand(SuspiciousPlayers pl)
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

		if(!sender.hasPermission("susp.unarchive"))
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
		int listIndex = Integer.parseInt(args[1]);
		BukkitRunnable r = new BukkitRunnable()
		{
			@Override
			public void run()
			{
				try(Connection c = plugin.getConnection())
				{
					ResultSet selectResults = c.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
							.executeQuery("SELECT * FROM archived_entries WHERE suspicious_uuid = '" + suspiciousUUID + "' ORDER BY created_time;");

					for (int i = 1; selectResults.next(); i++)
					{
						if(i == listIndex)
						{
							try
							{
								PreparedStatement insertStatement = c.prepareStatement("INSERT INTO active_entries(created_time, creator_uuid, suspicious_uuid, entry) VALUES (?,?,?,?)");
								insertStatement.setString(1, selectResults.getString("created_time"));
								insertStatement.setString(2, selectResults.getString("creator_uuid"));
								insertStatement.setString(3, selectResults.getString("suspicious_uuid"));
								insertStatement.setString(4, selectResults.getString("entry"));
								insertStatement.executeUpdate();
							}
							catch (SQLException e)
							{
								sender.sendMessage(RED + "Error occurred while inserting entry into active table: " + e.getMessage());
								e.printStackTrace();
								return;
							}

							try
							{
								selectResults.deleteRow();
							}
							catch (SQLException e)
							{
								sender.sendMessage(RED + "Error occurred while deleting archived entry from database: " + e.getMessage());
								e.printStackTrace();
								return;
							}
							sender.sendMessage(GREEN + "Entry archived.");
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