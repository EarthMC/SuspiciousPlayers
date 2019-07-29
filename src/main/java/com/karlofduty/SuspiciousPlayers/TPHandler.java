package com.karlofduty.SuspiciousPlayers;

import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import com.karlofduty.SuspiciousPlayers.models.PlayerEntry;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class TPHandler
{
	private static HashMap<Player, Integer> indicies = new HashMap<>();

	/**
	 * Gets the current player list index of the player which the command caller has last teleported to,
	 * or set it to the default if it does not exist
	 * @param player Player to be teleported
	 * @return Index of the player that they last teleported to
	 */
	private static int getIndexOrInitialize(Player player)
	{
		if(!indicies.containsKey(player))
		{
			indicies.put(player, 0);
		}
		return indicies.get(player);
	}
	
	/**
	 * Gets the next player to teleport to, susp only.
	 * @param player Player who is to be teleported
	 * @return Player who will be teleported to
	 */
	public static Player next(Player player)
	{
		// If the player is the only one online, return null
		if(Bukkit.getOnlinePlayers().size() <= 1)
		{
			return null;
		}

		int currentPos = getIndexOrInitialize(player) + 1;
		if(currentPos >= Bukkit.getOnlinePlayers().size())
		{
			currentPos = 0;
		}

		indicies.put(player, currentPos);

		Player tpTarget = (Player)Bukkit.getOnlinePlayers().toArray()[currentPos];

		// If the target is themselves, skip it
		return tpTarget == player ? next(player) : tpTarget;
	}

	/**
	 * Gets the previous player to teleport to, susp only.
	 * @param player Player who is to be teleported
	 * @return Player who will be teleported to
	 */
	public static Player prev(Player player)
	{
		// If the player is the only one online, return null
		if(Bukkit.getOnlinePlayers().size() <= 1)
		{
			return null;
		}

		int currentPos = getIndexOrInitialize(player) - 1;
		if(currentPos < 0)
		{
			currentPos = Bukkit.getOnlinePlayers().size() - 1;
		}

		indicies.put(player, currentPos);

		Player tpTarget = (Player)Bukkit.getOnlinePlayers().toArray()[currentPos];

		// If the target is themselves, skip it
		return tpTarget == player ? prev(player) : tpTarget;
	}

	/**
	 * Gets the next player to teleport to, susp only.
	 * @param player Player who is to be teleported
	 * @return Player who will be teleported to
	 */
	public static Player nextSusp(Player player)
	{
		int currentPos = getIndexOrInitialize(player) + 1;

		try (Connection c = SuspiciousPlayers.instance.getConnection())
		{
			final LinkedHashMap<String, LinkedList<ActiveEntry>> entries = ActiveEntry.selectOnline(c);

			// Check if there are no suspicious players online
			if(entries.isEmpty())
			{
				return null;
			}

			SuspiciousPlayers.instance.getLogger().info("Thing: " + entries.size());
			// Check if the only suspicious player online is the command caller themselves
			if(entries.size() == 1 && entries.keySet().contains(player.getUniqueId().toString()))
			{
				SuspiciousPlayers.instance.getLogger().info("Test");
				return null;
			}

			if(currentPos >= entries.size())
			{
				currentPos = 0;
			}

			indicies.put(player, currentPos);

			Player tpTarget = Bukkit.getPlayer(UUID.fromString((String)entries.keySet().toArray()[currentPos]));

			if(tpTarget == player)
			{
				SuspiciousPlayers.instance.getLogger().info("Equal");
			}
			else if(tpTarget == null)
			{
				SuspiciousPlayers.instance.getLogger().info("Null");
			}

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
	public static Player prevSusp(Player player)
	{
		int currentPos = getIndexOrInitialize(player) - 1;

		try (Connection c = SuspiciousPlayers.instance.getConnection())
		{
			final LinkedHashMap<String, LinkedList<ActiveEntry>> entries = ActiveEntry.selectOnline(c);

			// Check if there are no suspicious players online
			if(entries.isEmpty())
			{
				return null;
			}

			// Check if the only suspicious player online is the command caller themselves
			if(entries.size() == 1 && entries.keySet().contains(player.getUniqueId().toString()))
			{
				return null;
			}

			if(currentPos < 0)
			{
				currentPos = entries.size() - 1;
			}

			indicies.put(player, currentPos);

			Player tpTarget = Bukkit.getPlayer(UUID.fromString((String)entries.keySet().toArray()[currentPos]));

			// If the target is themselves, skip it
			return tpTarget == player ? prevSusp(player) : tpTarget;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Creates the left arrow button for the status messages
	 * @param suspOnly If only teleporting between suspicious players or all players
	 * @return The message button
	 */
	private static TextComponent getLeftArrow(boolean suspOnly)
	{
		return new TextComponent(
				new ComponentBuilder("[")
						.color(ChatColor.DARK_GRAY)
						.append("<")
						.color(ChatColor.GOLD)
						.event(suspOnly ? new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/suspprev") : new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpprev"))
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.YELLOW + "Previous")))
						.append("] ")
						.color(ChatColor.DARK_GRAY)
						.create());
	}

	/**
	 * Creates the right arrow button for the status messages
	 * @param suspOnly If only teleporting between suspicious players or all players
	 * @return The message button
	 */
	private static TextComponent getRightArrow(boolean suspOnly)
	{
		return new TextComponent(
				new ComponentBuilder(" [")
						.color(ChatColor.DARK_GRAY)
						.append(">")
						.color(ChatColor.GOLD)
						.event(suspOnly ? new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/suspnext") : new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpnext"))
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.YELLOW + "Next")))
						.append("]")
						.color(ChatColor.DARK_GRAY)
						.create());
	}

	/**
	 * Creates a status message for the teleport commands
	 * @param player The player currently teleported to
	 * @param suspOnly If only teleporting between suspicious players or all players
	 * @return The message
	 */
	public static TextComponent getTPStatus(Player player, boolean suspOnly)
	{
		TextComponent message = getLeftArrow(suspOnly);
		message.addExtra(PlayerEntry.getNameComponent(player.getUniqueId(), net.md_5.bungee.api.ChatColor.YELLOW));
		message.addExtra(getRightArrow(suspOnly));
		return message;
	}
}
