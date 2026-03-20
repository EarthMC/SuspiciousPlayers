package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import java.sql.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AddCommand implements CommandExecutor {
  private final SuspiciousPlayers plugin;

  public AddCommand(SuspiciousPlayers pl) {
    plugin = pl;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      String @NotNull [] args) {
    if (!sender.hasPermission("susp.add")) {
      sender.sendMessage(
          Component.text("You are not allowed to use that command.", NamedTextColor.RED));
      return true;
    }

    if (args.length < 2) {
      sender.sendMessage(
          Component.text(
              "Invalid arguments, usage: /suspadd [player] [message]", NamedTextColor.RED));
      return true;
    }

    final String creatorUUID =
        sender instanceof Player player ? player.getUniqueId().toString() : "Console";
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
                  plugin.logger().error("An exception occurred when getting player name", e);
                }

                if (suspiciousUUID == null) {
                  sender.sendMessage(
                      Component.text(
                          "Can not find a player by that name, make sure you are using their current username.",
                          NamedTextColor.RED));
                  return;
                }

                List<String> argumentList = new ArrayList<>(Arrays.asList(args));
                argumentList.removeFirst();
                String playerEntry = String.join(" ", argumentList);

                try (PreparedStatement statement =
                    connection.prepareStatement(ActiveEntry.INSERT)) {
                  statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                  statement.setString(2, creatorUUID);
                  statement.setString(3, suspiciousUUID.toString());
                  statement.setString(4, playerEntry);
                  statement.executeUpdate();

                  sender.sendMessage(Component.text("Entry added.", NamedTextColor.GREEN));
                } catch (SQLException e) {
                  sender.sendMessage(
                      Component.text(
                          "Error occurred while adding entry to database. " + e.getMessage(),
                          NamedTextColor.RED));
                  plugin.logger().warn("SQLException while adding entry:", e);
                }
              } catch (Exception e) {
                sender.sendMessage(
                    Component.text(
                        "Error occurred while preparing to add entry. " + e.getMessage(),
                        NamedTextColor.RED));
                plugin.logger().warn("Exception while adding entry:", e);
              }
            });
    return true;
  }
}
