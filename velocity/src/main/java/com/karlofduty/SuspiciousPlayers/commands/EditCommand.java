package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

public class EditCommand implements SimpleCommand {
    private final SuspiciousPlayers plugin;

    public EditCommand(SuspiciousPlayers plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (invocation.arguments().length < 2) {
            invocation.source().sendMessage(Component.text("Invalid arguments. Usage: /suspedit [id] [message]", NamedTextColor.RED));
            return;
        }

        if (!invocation.source().hasPermission("susp.edit")) {
            invocation.source().sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        final int id;
        try {
            id = Integer.parseInt(invocation.arguments()[0]);
        } catch (NumberFormatException e) {
            invocation.source().sendMessage(Component.text("Invalid id: " + invocation.arguments()[0]));
            return;
        }

        plugin.proxy().getScheduler().buildTask(plugin, () -> {
            try (Connection connection = plugin.getConnection()) {
                final ActiveEntry entry = ActiveEntry.select(connection, id);

                if (entry == null) {
                    invocation.source().sendMessage(Component.text("Invalid ID, does that entry still exist?", NamedTextColor.RED));
                    return;
                }

                final String message = String.join(" ", Arrays.copyOfRange(invocation.arguments(), 1, invocation.arguments().length));

                invocation.source().sendMessage(entry.edit(connection, message));
            } catch (SQLException e) {
                plugin.logger().error("while looking up active entry for edit", e);
            }
        }).schedule();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("susp.edit");
    }
}
