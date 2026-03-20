package com.karlofduty.SuspiciousPlayers.listeners;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import com.karlofduty.SuspiciousPlayers.models.PlayerEntry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
  private final SuspiciousPlayers plugin;

  public JoinListener(SuspiciousPlayers pl) {
    plugin = pl;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    plugin
        .server
        .getAsyncScheduler()
        .runNow(
            plugin,
            t -> {
              try (Connection c = plugin.getConnection()) {
                PreparedStatement statement = c.prepareStatement(ActiveEntry.SELECT_PLAYER);
                statement.setString(1, event.getPlayer().getUniqueId().toString());
                statement.setInt(2, 1000);
                ResultSet results = statement.executeQuery();

                int count = 0;
                while (results.next()) {
                  count++;
                }
                if (count == 1) {
                  results.first();
                  plugin.notify(
                      PlayerEntry.getNameComponent(
                              event.getPlayer().getUniqueId(), NamedTextColor.RED)
                          .append(
                              Component.text(
                                  " has been marked as suspicious:\n", NamedTextColor.RED))
                          .append(new ActiveEntry(results).getInteractiveMessage()));
                } else if (count > 1) {
                  plugin.notify(
                      Component.text(
                              event.getPlayer().getName()
                                  + " has been marked as suspicious "
                                  + count
                                  + " times!\nDo ",
                              NamedTextColor.RED)
                          .append(
                              Component.text(
                                  "/susplist " + event.getPlayer().getName(),
                                  NamedTextColor.YELLOW))
                          .clickEvent(
                              ClickEvent.runCommand("/susplist " + event.getPlayer().getName()))
                          .hoverEvent(
                              HoverEvent.showText(
                                  Component.text("Show susplist.", NamedTextColor.YELLOW)))
                          .append(Component.text(" to see why.", NamedTextColor.RED)));
                }
              } catch (SQLException e) {
                plugin.logger().warn("SQLException while loading player susplist on join:", e);
              }
            });
  }
}
