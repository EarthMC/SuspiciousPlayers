package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ReloadCommand implements SimpleCommand {
	private final SuspiciousPlayers plugin;

	public ReloadCommand(SuspiciousPlayers plugin) {
		this.plugin = plugin;
	}

	@Override
	public void execute(Invocation invocation) {
		if (!invocation.source().hasPermission("susp.reload")) {
			invocation.source().sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
			return;
		}

		invocation.source().sendMessage(plugin.reload());
	}

	@Override
	public boolean hasPermission(Invocation invocation) {
		return invocation.source().hasPermission("susp.reload");
	}
}
