package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import com.karlofduty.SuspiciousPlayers.models.ArchivedEntry;
import com.karlofduty.SuspiciousPlayers.models.PlayerEntry;
import java.sql.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ListCommand implements CommandExecutor {
  private final SuspiciousPlayers plugin;

  public ListCommand(SuspiciousPlayers pl) {
    plugin = pl;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      String @NotNull [] args) {
    if (args.length < 1) {
      sender.sendMessage(Component.text("Invalid arguments.", NamedTextColor.RED));
      return false;
    }

    if (!sender.hasPermission("susp.list")) {
      sender.sendMessage(
          Component.text("You do not have permission to use this command.", NamedTextColor.RED));
      return true;
    }

    plugin
        .server
        .getAsyncScheduler()
        .runNow(
            plugin,
            t -> {
              try (Connection connection = plugin.getConnection()) {
                UUID suspiciousUUID = null;

                // Get the uuid of this player using the history table
                try (PreparedStatement statement =
                    connection.prepareStatement(
                        "select * from player_history where upper(name) = ?")) {
                  statement.setString(1, args[0].toUpperCase(Locale.ROOT));

                  ResultSet resultSet = statement.executeQuery();
                  if (resultSet.next())
                    suspiciousUUID = UUID.fromString(resultSet.getString("uuid"));
                } catch (SQLException e) {
                  SuspiciousPlayers.plugin()
                      .logger()
                      .error("An exception occurred when getting player name", e);
                }

                if (suspiciousUUID == null) {
                  sender.sendMessage(
                      Component.text(
                          "Can not find a player by that name, make sure you are using their current username.",
                          NamedTextColor.RED));
                  return;
                }

                int maxEntries =
                    args.length >= 2 && SuspiciousPlayers.isInt(args[1])
                        ? Integer.parseInt(args[1])
                        : 10;
                List<PlayerEntry> entries = new ArrayList<>();

                // Reads all active entries about the player
                PreparedStatement statement =
                    connection.prepareStatement(ActiveEntry.SELECT_PLAYER);
                statement.setString(1, suspiciousUUID.toString());
                statement.setInt(2, maxEntries);
                ResultSet activeResults = statement.executeQuery();

                while (activeResults.next()) entries.add(new ActiveEntry(activeResults));

                // Reads archived entries about the player if there are still spots open in the list
                if (entries.size() < maxEntries) {

                  statement = connection.prepareStatement(ArchivedEntry.SELECT_PLAYER);
                  statement.setString(1, suspiciousUUID.toString());
                  statement.setInt(2, maxEntries - entries.size());
                  ResultSet archiveResults = statement.executeQuery();

                  while (archiveResults.next()) entries.add(new ArchivedEntry(archiveResults));
                }

                // Send feedback message if there are no entries
                if (entries.isEmpty()) {
                  sender.sendMessage(
                      Component.text("User does not have any entries.", NamedTextColor.RED));
                  return;
                }

                Component message =
                    Component.text("----- Displaying (Max: ", NamedTextColor.GOLD)
                        .append(Component.text(maxEntries, NamedTextColor.YELLOW))
                        .append(Component.text(") entries for ", NamedTextColor.GOLD))
                        .append(PlayerEntry.getNameComponent(suspiciousUUID, NamedTextColor.YELLOW))
                        .append(Component.text(" -----\n", NamedTextColor.GOLD));

                for (PlayerEntry entry : entries)
                  message = message.append(entry.getInteractiveMessage());

                sender.sendMessage(message);

              } catch (SQLException e) {
                sender.sendMessage(
                    Component.text(
                        "Error occurred while listing entries. " + e.getMessage(),
                        NamedTextColor.RED));
                plugin.logger().error("Error occurred while listing entries", e);
              }
            });
    return true;
  }
}
