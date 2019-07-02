package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import com.karlofduty.SuspiciousPlayers.models.PlayerEntry;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import static org.bukkit.ChatColor.*;

public class OnlineCommand implements CommandExecutor
{
	SuspiciousPlayers plugin;
	public OnlineCommand(SuspiciousPlayers pl)
	{
		plugin = pl;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (!sender.hasPermission("susp.online"))
		{
			sender.sendMessage(RED + "You are not allowed to use that command.");
			return true;
		}

		BukkitRunnable r = new BukkitRunnable()
		{
			@Override
			public void run()
			{
				try (Connection c = plugin.getConnection())
				{
					final LinkedHashMap<String, LinkedList<ActiveEntry>> entries = ActiveEntry.selectOnline(c);

					// Send feedback message if there are no entries
					if(entries.isEmpty())
					{
						sender.sendMessage(RED + "No online users have any active entries.");
						return;
					}

					// Builds the return message
					TextComponent message = new TextComponent();
					for (Map.Entry<String, LinkedList<ActiveEntry>> playerEntries : entries.entrySet())
					{
						try
						{
							message.addExtra(new TextComponent(TextComponent.fromLegacyText(GOLD + "----- Displaying entries for ")));
							message.addExtra(PlayerEntry.getUsernameComponent(playerEntries.getKey(), ChatColor.YELLOW));
							message.addExtra(new TextComponent(TextComponent.fromLegacyText(GOLD + " -----\n")));
							for (PlayerEntry entry : playerEntries.getValue())
							{
								message.addExtra(entry.getInteractiveMessage());
							}
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
					sender.spigot().sendMessage(message);
				}
				catch (SQLException e)
				{
					sender.sendMessage(RED + "Error occurred while interacting with the database. " + e.getMessage());
					e.printStackTrace();
				}
			}
		};
		r.runTaskAsynchronously(plugin);
		return true;

	}

}
