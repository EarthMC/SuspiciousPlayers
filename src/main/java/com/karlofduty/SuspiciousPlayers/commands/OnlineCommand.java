package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import com.karlofduty.SuspiciousPlayers.models.PlayerEntry;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class OnlineCommand implements CommandExecutor {
  private final SuspiciousPlayers plugin;

  public OnlineCommand(SuspiciousPlayers pl) {
    plugin = pl;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      String @NotNull [] args) {
    if (!sender.hasPermission("susp.online")) {
      sender.sendMessage(
          Component.text("You are not allowed to use that command.", NamedTextColor.RED));
      return true;
    }

    plugin
        .server
        .getAsyncScheduler()
        .runNow(
            plugin,
            t -> {
              try (Connection c = plugin.getConnection()) {
                final Map<String, LinkedList<ActiveEntry>> entries = ActiveEntry.selectOnline(c);

                if (entries.isEmpty()) {
                  sender.sendMessage(
                      Component.text(
                          "No online users have any active entries.", NamedTextColor.RED));
                  return;
                }

                TextComponent.Builder message = Component.text();
                for (Map.Entry<String, LinkedList<ActiveEntry>> playerEntries :
                    entries.entrySet()) {
                  try {
                    message.append(
                        Component.text("----- Displaying entries for ", NamedTextColor.GOLD));
                    message.append(
                        PlayerEntry.getNameComponent(
                            UUID.fromString(playerEntries.getKey()), NamedTextColor.YELLOW));
                    message.append(Component.text(" -----\n", NamedTextColor.GOLD));
                    for (PlayerEntry entry : playerEntries.getValue()) {
                      message.append(entry.getInteractiveMessage());
                    }
                  } catch (Exception e) {
                    plugin.logger().warn("Exception in susponline:", e);
                  }
                }
                sender.sendMessage(message);
              } catch (SQLException e) {
                sender.sendMessage(
                    Component.text(
                        "Error occurred while interacting with the database. " + e.getMessage(),
                        NamedTextColor.RED));
                plugin.logger().warn("Exception in susponline:", e);
              }
            });
    return true;
  }
}
