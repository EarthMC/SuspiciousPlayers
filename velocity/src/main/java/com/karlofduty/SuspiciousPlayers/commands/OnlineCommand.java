package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import com.karlofduty.SuspiciousPlayers.models.PlayerEntry;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class OnlineCommand implements SimpleCommand {
	private final SuspiciousPlayers plugin;

	public OnlineCommand(SuspiciousPlayers plugin) {
		this.plugin = plugin;
	}

	@Override
	public void execute(Invocation invocation) {
		if (!invocation.source().hasPermission("susp.online")) {
			invocation.source().sendMessage(Component.text("You are not allowed to use that command.", NamedTextColor.RED));
			return;
		}

		if (!(invocation.source() instanceof Player player)) {
			invocation.source().sendMessage(Component.text("You cannot run this as console!", NamedTextColor.RED));
			return;
		}

		plugin.proxy().getScheduler().buildTask(plugin, () -> {
			RegisteredServer server = player.getCurrentServer().map(ServerConnection::getServer).orElse(null);
			if (server == null)
				return;

			try (Connection c = plugin.getConnection()) {
				final Map<String, LinkedList<ActiveEntry>> entries = ActiveEntry.selectOnline(c, server);

				// Send feedback message if there are no entries
				if (entries.isEmpty()) {
					invocation.source().sendMessage(Component.text("No online users have any active entries.", NamedTextColor.RED));
					return;
				}

				// Builds the return message
				Component message = Component.empty();
				for (Map.Entry<String, LinkedList<ActiveEntry>> playerEntries : entries.entrySet()) {
					try {
						message = message.append(Component.text("----- Displaying entries for ", NamedTextColor.GOLD)
								.append(PlayerEntry.getNameComponent(UUID.fromString(playerEntries.getKey()), NamedTextColor.YELLOW))
								.append(Component.text(" -----\n", NamedTextColor.GOLD)));

						for (PlayerEntry entry : playerEntries.getValue())
							message = message.append(entry.getInteractiveMessage());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				invocation.source().sendMessage(message);
			} catch (SQLException e) {
				invocation.source().sendMessage(Component.text("Error occurred while interacting with the database. " + e.getMessage(), NamedTextColor.RED));
				e.printStackTrace();
			}
		}).schedule();
	}

	@Override
	public boolean hasPermission(Invocation invocation) {
		return invocation.source().hasPermission("susp.online");
	}
}
