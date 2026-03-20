package com.karlofduty.SuspiciousPlayers;

import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import com.karlofduty.SuspiciousPlayers.models.PlayerEntry;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TPHandler {
  private static final HashMap<Player, Integer> indicies = new HashMap<>();

  /**
   * Gets the current player list index of the player which the command caller has last teleported
   * to, or set it to the default if it does not exist
   *
   * @param player Player to be teleported
   * @return Index of the player that they last teleported to
   */
  private static int getIndexOrInitialize(Player player) {
    if (!indicies.containsKey(player)) {
      indicies.put(player, 0);
    }
    return indicies.get(player);
  }

  /**
   * Gets the next player to teleport to, susp only.
   *
   * @param player Player who is to be teleported
   * @return Player who will be teleported to
   */
  public static Player next(Player player) {
    // If the player is the only one online, return null
    if (Bukkit.getOnlinePlayers().size() <= 1) {
      return null;
    }

    int currentPos = getIndexOrInitialize(player) + 1;
    if (currentPos >= Bukkit.getOnlinePlayers().size()) {
      currentPos = 0;
    }

    indicies.put(player, currentPos);

    Player tpTarget = (Player) Bukkit.getOnlinePlayers().toArray()[currentPos];

    // If the target is themselves, skip it
    return tpTarget == player ? next(player) : tpTarget;
  }

  /**
   * Gets the previous player to teleport to, susp only.
   *
   * @param player Player who is to be teleported
   * @return Player who will be teleported to
   */
  public static Player prev(Player player) {
    // If the player is the only one online, return null
    if (Bukkit.getOnlinePlayers().size() <= 1) {
      return null;
    }

    int currentPos = getIndexOrInitialize(player) - 1;
    if (currentPos < 0) {
      currentPos = Bukkit.getOnlinePlayers().size() - 1;
    }

    indicies.put(player, currentPos);

    Player tpTarget = (Player) Bukkit.getOnlinePlayers().toArray()[currentPos];

    // If the target is themselves, skip it
    return tpTarget == player ? prev(player) : tpTarget;
  }

  /**
   * Gets the next player to teleport to, susp only.
   *
   * @param player Player who is to be teleported
   * @return Player who will be teleported to
   */
  public static Player nextSusp(Player player) {
    int currentPos = getIndexOrInitialize(player) + 1;

    try (Connection c = SuspiciousPlayers.plugin().getConnection()) {
      final Map<String, LinkedList<ActiveEntry>> entries = ActiveEntry.selectOnline(c);

      // Check if there are no suspicious players online
      if (entries.isEmpty()) {
        return null;
      }

      SuspiciousPlayers.plugin().getLogger().info("Thing: " + entries.size());
      // Check if the only suspicious player online is the command caller themselves
      if (entries.size() == 1 && entries.containsKey(player.getUniqueId().toString())) {
        SuspiciousPlayers.plugin().getLogger().info("Test");
        return null;
      }

      if (currentPos >= entries.size()) {
        currentPos = 0;
      }

      indicies.put(player, currentPos);

      Player tpTarget =
          Bukkit.getPlayer(UUID.fromString((String) entries.keySet().toArray()[currentPos]));

      if (tpTarget == player) {
        SuspiciousPlayers.plugin().getLogger().info("Equal");
      } else if (tpTarget == null) {
        SuspiciousPlayers.plugin().getLogger().info("Null");
      }

      // If the target is themselves, skip it
      return tpTarget == player ? nextSusp(player) : tpTarget;
    } catch (SQLException e) {
      SuspiciousPlayers.plugin()
          .logger()
          .warn("SQLException while loading entries for nextSusp:", e);
      return null;
    }
  }

  /**
   * Gets the previous player to teleport to, susp only.
   *
   * @param player Player who is to be teleported
   * @return Player who will be teleported to
   */
  public static Player prevSusp(Player player) {
    int currentPos = getIndexOrInitialize(player) - 1;

    try (Connection c = SuspiciousPlayers.plugin().getConnection()) {
      final Map<String, LinkedList<ActiveEntry>> entries = ActiveEntry.selectOnline(c);

      // Check if there are no suspicious players online
      if (entries.isEmpty()) {
        return null;
      }

      // Check if the only suspicious player online is the command caller themselves
      if (entries.size() == 1 && entries.containsKey(player.getUniqueId().toString())) {
        return null;
      }

      if (currentPos < 0) {
        currentPos = entries.size() - 1;
      }

      indicies.put(player, currentPos);

      Player tpTarget =
          Bukkit.getPlayer(UUID.fromString((String) entries.keySet().toArray()[currentPos]));

      // If the target is themselves, skip it
      return tpTarget == player ? prevSusp(player) : tpTarget;
    } catch (SQLException e) {
      SuspiciousPlayers.plugin()
          .logger()
          .warn("SQLException while loading entries for prevSusp:", e);
      return null;
    }
  }

  /**
   * Creates the left arrow button for the status messages
   *
   * @param suspOnly If only teleporting between suspicious players or all players
   * @return The message button
   */
  private static Component getLeftArrow(boolean suspOnly) {
    return Component.text()
        .append(Component.text("[", NamedTextColor.DARK_GRAY))
        .append(
            Component.text("<", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand(suspOnly ? "/suspprev" : "/tpprev"))
                .hoverEvent(HoverEvent.showText(Component.text("Previous", NamedTextColor.YELLOW))))
        .append(Component.text("] ", NamedTextColor.DARK_GRAY))
        .build();
  }

  /**
   * Creates the right arrow button for the status messages
   *
   * @param suspOnly If only teleporting between suspicious players or all players
   * @return The message button
   */
  private static Component getRightArrow(boolean suspOnly) {
    return Component.text()
        .append(Component.text(" [", NamedTextColor.DARK_GRAY))
        .append(
            Component.text(">", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand(suspOnly ? "/suspnext" : "/tpnext"))
                .hoverEvent(HoverEvent.showText(Component.text("Next", NamedTextColor.YELLOW))))
        .append(Component.text("]", NamedTextColor.DARK_GRAY))
        .build();
  }

  /**
   * Creates a status message for the teleport commands
   *
   * @param player The player currently teleported to
   * @param suspOnly If only teleporting between suspicious players or all players
   * @return The message
   */
  public static Component getTPStatus(Player player, boolean suspOnly) {
    return getLeftArrow(suspOnly)
        .append(PlayerEntry.getNameComponent(player.getUniqueId(), NamedTextColor.YELLOW))
        .append(getRightArrow(suspOnly));
  }
}
