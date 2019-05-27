package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static org.bukkit.ChatColor.RED;

public class ReloadCommand implements CommandExecutor
{
	private SuspiciousPlayers plugin;

	public ReloadCommand(SuspiciousPlayers pl)
	{
		plugin = pl;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (!sender.hasPermission("susp.reload"))
		{
			sender.sendMessage(RED + "You do not have permission to use this command.");
			return true;
		}

		sender.sendMessage(plugin.reload());
		return true;
	}
}
