package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ArchivedEntry;
import java.sql.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DeleteCommand implements CommandExecutor {
  private final SuspiciousPlayers plugin;

  public DeleteCommand(SuspiciousPlayers pl) {
    plugin = pl;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      String @NotNull [] args) {
    if (args.length < 1 || !SuspiciousPlayers.isInt(args[0])) {
      sender.sendMessage(
          Component.text(
              "Invalid arguments. You are not supposed to use this command, it is automatically called from /susplist.",
              NamedTextColor.RED));
      return false;
    }

    if (!sender.hasPermission("susp.delete")) {
      sender.sendMessage(
          Component.text("You do not have permission to use this command.", NamedTextColor.RED));
      return true;
    }

    final String deleterUUID =
        sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "console";
    final int id = Integer.parseInt(args[0]);
    plugin
        .server
        .getAsyncScheduler()
        .runNow(
            plugin,
            t -> {
              try (Connection c = plugin.getConnection()) {
                ArchivedEntry entry = ArchivedEntry.select(c, id);

                if (entry == null) {
                  sender.sendMessage(
                      Component.text(
                          "Invalid ID, does that entry still exist?", NamedTextColor.RED));
                  return;
                }

                sender.sendMessage(entry.delete(c, deleterUUID));
              } catch (SQLException e) {
                sender.sendMessage(
                    Component.text(
                        "Error occurred while loading entries from database: " + e.getMessage(),
                        NamedTextColor.RED));
                plugin.logger().warn("Exception while deleting entry:", e);
              }
            });
    return true;
  }
}
