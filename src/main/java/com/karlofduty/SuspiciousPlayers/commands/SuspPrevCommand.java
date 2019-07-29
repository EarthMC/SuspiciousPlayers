package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.TPHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static org.bukkit.ChatColor.RED;

public class SuspPrevCommand implements CommandExecutor
{
	SuspiciousPlayers plugin;
	public SuspPrevCommand(SuspiciousPlayers pl)
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

		BukkitRunnable r = new BukkitRunnable()
		{
			@Override
			public void run()
			{
				Player tpTarget = TPHandler.prevSusp(player);
				if(tpTarget == null)
				{
					sender.sendMessage(RED + "No suspicious player online to teleport to.");
					return;
				}

				player.teleport(tpTarget);
				sender.spigot().sendMessage(TPHandler.getTPStatus(tpTarget, true));
			}
		};
		r.runTaskAsynchronously(plugin);
		return true;
	}
}