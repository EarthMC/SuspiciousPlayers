package com.karlofduty.SuspiciousPlayers;

import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import com.karlofduty.SuspiciousPlayers.models.PlayerEntry;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TPHandler {
	public static final Map<String, Map<UUID, Integer>> indices = new ConcurrentHashMap<>();

	/**
	 * Gets the current player list index of the player which the command caller has last teleported to,
	 * or set it to the default if it does not exist
	 * @param player Player to be teleported
	 * @return Index of the player that they last teleported to
	 */
	private static int getIndexOrInitialize(Player player, String serverName) {
		Map<UUID, Integer> serverIndices = indices.putIfAbsent(serverName, new ConcurrentHashMap<>());

		if (!serverIndices.containsKey(player.getUniqueId()))
			serverIndices.put(player.getUniqueId(), 0);

		return serverIndices.get(player.getUniqueId());
	}
	
	/**
	 * Gets the next player to teleport to, susp only.
	 * @param player Player who is to be teleported
	 * @return Player who will be teleported to
	 */
	public static Player next(Player player) {
		RegisteredServer server = player.getCurrentServer().map(ServerConnection::getServer).orElse(null);

		// If the player is the only one online, return null
		if (server == null || server.getPlayersConnected().size() <= 1)
			return null;

		int currentPos = getIndexOrInitialize(player, server.getServerInfo().getName().toLowerCase(Locale.ROOT)) + 1;
		if (currentPos >= server.getPlayersConnected().size())
			currentPos = 0;

		indices.putIfAbsent(server.getServerInfo().getName().toLowerCase(Locale.ROOT), new ConcurrentHashMap<>()).put(player.getUniqueId(), currentPos);

		Player tpTarget = (Player) server.getPlayersConnected().toArray()[currentPos];

		// If the target is themselves, skip it
		return tpTarget == player ? next(player) : tpTarget;
	}

	/**
	 * Gets the previous player to teleport to, susp only.
	 * @param player Player who is to be teleported
	 * @return Player who will be teleported to
	 */
	public static Player prev(Player player) {
		RegisteredServer server = player.getCurrentServer().map(ServerConnection::getServer).orElse(null);

		// If the player is the only one online, return null
		if (server == null || server.getPlayersConnected().size() <= 1)
			return null;

		int currentPos = getIndexOrInitialize(player, server.getServerInfo().getName().toLowerCase(Locale.ROOT)) - 1;
		if (currentPos < 0)
			currentPos = server.getPlayersConnected().size() - 1;

		indices.putIfAbsent(server.getServerInfo().getName().toLowerCase(Locale.ROOT), new ConcurrentHashMap<>()).put(player.getUniqueId(), currentPos);

		Player tpTarget = (Player) server.getPlayersConnected().toArray()[currentPos];

		// If the target is themselves, skip it
		return tpTarget == player ? prev(player) : tpTarget;
	}

	/**
	 * Gets the next player to teleport to, susp only.
	 * @param player Player who is to be teleported
	 * @return Player who will be teleported to
	 */
	public static Player nextSusp(Player player) {
		RegisteredServer server = player.getCurrentServer().map(ServerConnection::getServer).orElse(null);

		if (server == null || server.getPlayersConnected().size() <= 1)
			return null;

		int currentPos = getIndexOrInitialize(player, server.getServerInfo().getName().toLowerCase(Locale.ROOT)) + 1;

		try (Connection c = SuspiciousPlayers.plugin().getConnection()) {
			final Map<String, LinkedList<ActiveEntry>> entries = ActiveEntry.selectOnline(c, server);

			// Check if there are no suspicious players online
			if (entries.isEmpty())
				return null;

			// Check if the only suspicious player online is the command caller themselves
			if (entries.size() == 1 && entries.containsKey(player.getUniqueId().toString()))
				return null;

			if (currentPos >= entries.size())
				currentPos = 0;

			indices.putIfAbsent(server.getServerInfo().getName().toLowerCase(Locale.ROOT), new ConcurrentHashMap<>()).put(player.getUniqueId(), currentPos);

			Player tpTarget = SuspiciousPlayers.plugin().proxy().getPlayer(UUID.fromString((String)entries.keySet().toArray()[currentPos])).orElse(null);

			// If the target is themselves, skip it
			return tpTarget == player ? nextSusp(player) : tpTarget;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Gets the previous player to teleport to, susp only.
	 * @param player Player who is to be teleported
	 * @return Player who will be teleported to
	 */
	public static Player prevSusp(Player player) {
		RegisteredServer server = player.getCurrentServer().map(ServerConnection::getServer).orElse(null);

		if (server == null || server.getPlayersConnected().size() <= 1)
			return null;

		int currentPos = getIndexOrInitialize(player, server.getServerInfo().getName().toLowerCase(Locale.ROOT)) - 1;

		try (Connection c = SuspiciousPlayers.plugin().getConnection()) {
			final Map<String, LinkedList<ActiveEntry>> entries = ActiveEntry.selectOnline(c, server);

			// Check if there are no suspicious players online
			if (entries.isEmpty())
				return null;

			// Check if the only suspicious player online is the command caller themselves
			if (entries.size() == 1 && entries.containsKey(player.getUniqueId().toString()))
				return null;

			if (currentPos < 0)
				currentPos = entries.size() - 1;

			indices.putIfAbsent(server.getServerInfo().getName().toLowerCase(Locale.ROOT), new ConcurrentHashMap<>()).put(player.getUniqueId(), currentPos);

			Player tpTarget = SuspiciousPlayers.plugin().proxy().getPlayer(UUID.fromString((String) entries.keySet().toArray()[currentPos])).orElse(null);

			// If the target is themselves, skip it
			return tpTarget == player ? prevSusp(player) : tpTarget;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Creates the left arrow button for the status messages
	 * @param suspOnly If only teleporting between suspicious players or all players
	 * @return The message button
	 */
	private static Component getLeftArrow(boolean suspOnly) {
		return Component.empty()
				.append(Component.text("[", NamedTextColor.DARK_GRAY))
				.append(Component.text("<", NamedTextColor.GOLD)
						.clickEvent(ClickEvent.runCommand(suspOnly ? "/suspprev" : "/tpprev"))
						.hoverEvent(HoverEvent.showText(Component.text("Previous", NamedTextColor.YELLOW))))
				.append(Component.text("] ", NamedTextColor.DARK_GRAY));
	}

	/**
	 * Creates the right arrow button for the status messages
	 * @param suspOnly If only teleporting between suspicious players or all players
	 * @return The message button
	 */
	private static Component getRightArrow(boolean suspOnly) {
		return Component.empty()
				.append(Component.text(" [", NamedTextColor.DARK_GRAY))
				.append(Component.text(">", NamedTextColor.GOLD)
						.clickEvent(ClickEvent.runCommand(suspOnly ? "/suspnext" : "/tpnext"))
						.hoverEvent(HoverEvent.showText(Component.text("Next", NamedTextColor.YELLOW))))
				.append(Component.text("]", NamedTextColor.DARK_GRAY));
	}

	/**
	 * Creates a status message for the teleport commands
	 * @param player The player currently teleported to
	 * @param suspOnly If only teleporting between suspicious players or all players
	 * @return The message
	 */
	public static Component getTPStatus(Player player, boolean suspOnly) {
		return Component.empty()
				.append(getLeftArrow(suspOnly))
				.append(PlayerEntry.getNameComponent(player.getUniqueId(), NamedTextColor.YELLOW))
				.append(getRightArrow(suspOnly));
	}
}
