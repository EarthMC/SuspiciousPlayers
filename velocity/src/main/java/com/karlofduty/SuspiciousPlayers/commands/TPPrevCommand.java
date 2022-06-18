package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.TPHandler;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TPPrevCommand implements SimpleCommand {
	private final SuspiciousPlayers plugin;

	public TPPrevCommand(SuspiciousPlayers plugin) {
		this.plugin = plugin;
	}

	@Override
	public void execute(Invocation invocation) {
		if (!invocation.source().hasPermission("susp.tp"))
		{
			invocation.source().sendMessage(Component.text("You are not allowed to use that command.", NamedTextColor.RED));
			return;
		}

		if (!(invocation.source() instanceof Player player)) {
			invocation.source().sendMessage(Component.text("You cannot run this as console!", NamedTextColor.RED));
			return;
		}

		Player tpTarget = TPHandler.prev(player);

		if (tpTarget == null) {
			invocation.source().sendMessage(Component.text("No one online to teleport to.", NamedTextColor.RED));
			return;
		}

		plugin.proxy().getCommandManager().executeAsync(plugin.proxy().getConsoleCommandSource(), "/cp ttp " + player.getUsername() + " " + tpTarget.getUsername());
		player.sendMessage(TPHandler.getTPStatus(tpTarget, false));
	}

	@Override
	public boolean hasPermission(Invocation invocation) {
		return invocation.source().hasPermission("susp.tp");
	}
}