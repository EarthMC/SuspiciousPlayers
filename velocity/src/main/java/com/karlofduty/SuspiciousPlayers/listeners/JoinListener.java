package com.karlofduty.SuspiciousPlayers.listeners;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;

import com.karlofduty.SuspiciousPlayers.TPHandler;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;

import com.karlofduty.SuspiciousPlayers.models.PlayerEntry;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class JoinListener {
    private final SuspiciousPlayers plugin;

    public JoinListener(SuspiciousPlayers plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        plugin.proxy().getScheduler().buildTask(plugin, () -> {
            try (Connection connection = plugin.getConnection()) {
                PreparedStatement statement = connection.prepareStatement("select * from player_history where uuid = ? limit 1");
                statement.setString(1, event.getPlayer().getUniqueId().toString());
                ResultSet resultSet = statement.executeQuery();

                if (!resultSet.next()) {
                    PreparedStatement statement2 = connection.prepareStatement("insert into player_history(uuid, name) values (?, ?)");
                    statement2.setString(1, event.getPlayer().getUniqueId().toString());
                    statement2.setString(2, event.getPlayer().getUsername());

                    statement2.execute();
                } else if (!event.getPlayer().getUsername().equals(resultSet.getString("name"))) {
                    // The player has changed their name, update their name in the db
                    PreparedStatement statement2 = connection.prepareStatement("replace into player_history (uuid, name, name_history) values (?, ?, ?)");
                    statement2.setString(1, event.getPlayer().getUniqueId().toString());
                    statement2.setString(2, event.getPlayer().getUsername());
                    // Clear username history
                    statement2.setString(3, null);

                    statement2.execute();
                }
            } catch (SQLException e) {
                plugin.logger().warn("An exception occurred when updating player history", e);
            }
        }).schedule();
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        plugin.proxy().getScheduler().buildTask(plugin, () -> {
            try (Connection c = plugin.getConnection()) {
                PreparedStatement statement = c.prepareStatement(ActiveEntry.SELECT_PLAYER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
                statement.setString(1, event.getPlayer().getUniqueId().toString());
                statement.setInt(2, 1000);
                ResultSet results = statement.executeQuery();

                int count = 0;
                while (results.next())
                    count++;

                if (count == 1) {
                    results.first();
                    plugin.notify(event.getServer(), Component.empty().append(PlayerEntry.getNameComponent(event.getPlayer().getUniqueId(), NamedTextColor.RED)).append(Component.text(" has been marked as suspicious:\n", NamedTextColor.RED)).append(new ActiveEntry(results).getInteractiveMessage()));
                } else if (count > 1) {
                    plugin.notify(event.getServer(), Component.empty()
                            .append(Component.text(event.getPlayer().getUsername() + " has been marked as suspicious " + count + " times!\nDo ", NamedTextColor.RED))
                            .append(Component.text("/susplist " + event.getPlayer().getUsername(), NamedTextColor.YELLOW)
                                    .clickEvent(ClickEvent.runCommand("/susplist " + event.getPlayer().getUsername()))
                                    .hoverEvent(HoverEvent.showText(Component.text("Show susplist", NamedTextColor.YELLOW))))
                            .append(Component.text(" to see why.", NamedTextColor.RED)));
                }
            } catch (SQLException e) {
                plugin.logger().error("An exception occurred when contacting the database", e);
            }
        }).schedule();
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        for (Map<UUID, Integer> serverIndices : TPHandler.indices.values())
            serverIndices.remove(event.getPlayer().getUniqueId());
    }
}
