package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ArchivedEntry;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.SQLException;

public class UnarchiveCommand implements SimpleCommand {
	private final SuspiciousPlayers plugin;

	public UnarchiveCommand(SuspiciousPlayers plugin) {
		this.plugin = plugin;
	}

	@Override
	public void execute(Invocation invocation) {
		if (invocation.arguments().length < 1 || !SuspiciousPlayers.isInt(invocation.arguments()[0])) {
			invocation.source().sendMessage(Component.text("Invalid arguments. You are not supposed to use this command, it is automatically called from /susplist.", NamedTextColor.RED));
			return;
		}

		if (!invocation.source().hasPermission("susp.unarchive")) {
			invocation.source().sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
			return;
		}

		int id = Integer.parseInt(invocation.arguments()[0]);

		plugin.proxy().getScheduler().buildTask(plugin, () -> {
			try (Connection c = plugin.getConnection()) {
				ArchivedEntry entry = ArchivedEntry.select(c, id);

				if (entry == null) {
					invocation.source().sendMessage(Component.text("Invalid ID, does that entry still exist?", NamedTextColor.RED));
					return;
				}

				invocation.source().sendMessage(entry.unarchive(c));
			} catch (SQLException e) {
				invocation.source().sendMessage(Component.text("Error occurred while loading entries from database: " + e.getMessage(), NamedTextColor.RED));
				e.printStackTrace();
			}
		}).schedule();
	}

	@Override
	public boolean hasPermission(Invocation invocation) {
		return invocation.source().hasPermission("susp.unarchive");
	}
}