package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ArchivedEntry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;

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
		if(args.length < 1 || !SuspiciousPlayers.isInt(args[0]))
		{
			sender.sendMessage(RED + "Invalid arguments. You are not supposed to use this command, it is automatically called from /susplist.");
			return false;
		}

		if(!sender.hasPermission("susp.unarchive"))
		{
			sender.sendMessage(RED + "You do not have permission to use this command.");
			return true;
		}

		int id = Integer.parseInt(args[0]);
		BukkitRunnable r = new BukkitRunnable()
		{
			@Override
			public void run()
			{
				try(Connection c = plugin.getConnection())
				{
					ArchivedEntry entry = ArchivedEntry.select(c, id);

					if(entry == null)
					{
						sender.sendMessage("Invalid ID, does that entry still exist?");
						return;
					}

					sender.sendMessage(entry.unarchive(c));
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