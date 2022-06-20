package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.TPHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static org.bukkit.ChatColor.RED;

public class TPPrevCommand implements CommandExecutor
{
	SuspiciousPlayers plugin;
	public TPPrevCommand(SuspiciousPlayers pl)
	{
		plugin = pl;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (!sender.hasPermission("susp.tp"))
		{
			sender.sendMessage(RED + "You are not allowed to use that command.");
			return true;
		}

		if (!(sender instanceof Player))
		{
			sender.sendMessage(RED + "You cannot run this as console!");
			return true;
		}

		Player player = (Player) sender;
		Player tpTarget = TPHandler.prev(player);

		if(tpTarget == null)
		{
			sender.sendMessage(RED + "No one online to teleport to.");
			return true;
		}

		player.teleportAsync(tpTarget.getLocation());
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> sender.spigot().sendMessage(TPHandler.getTPStatus(tpTarget, false)));

		return true;
	}
}