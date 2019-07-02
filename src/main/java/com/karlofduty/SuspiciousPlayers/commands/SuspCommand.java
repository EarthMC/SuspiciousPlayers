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

public class SuspCommand implements CommandExecutor
{
	SuspiciousPlayers plugin;
	public SuspCommand(SuspiciousPlayers pl)
	{
		plugin = pl;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if(args.length > 1)
		{
			return plugin.getCommand("suspadd").execute(sender,  label, args);
		}
		return plugin.getCommand("susplist").execute(sender, label, args);
	}
}
