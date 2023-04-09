package com.karlofduty.SuspiciousPlayers.commands;

import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;
import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.SQLException;

public class ArchiveCommand implements SimpleCommand {
    private final SuspiciousPlayers plugin;

    public ArchiveCommand(SuspiciousPlayers plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (invocation.arguments().length < 1 || !SuspiciousPlayers.isInt(invocation.arguments()[0])) {
            invocation.source().sendMessage(Component.text("Invalid arguments. You are not supposed to use this command, it is automatically called from /susplist.", NamedTextColor.RED));
            return;
        }

        if (!invocation.source().hasPermission("susp.archive"))
        {
            invocation.source().sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        final String archiverUUID = invocation.source() instanceof Player player ? player.getUniqueId().toString() : "Console";
        final int id;
        try {
            id = Integer.parseInt(invocation.arguments()[0]);
        } catch (NumberFormatException e) {
            invocation.source().sendMessage(Component.text("Invalid id: " + invocation.arguments()[0]));
            return;
        }

        plugin.proxy().getScheduler().buildTask(plugin, () -> {
            try (Connection c = plugin.getConnection()) {
                ActiveEntry entry = ActiveEntry.select(c, id);

                if (entry == null) {
                    invocation.source().sendMessage(Component.text("Invalid ID, does that entry still exist?", NamedTextColor.RED));
                    return;
                }

                invocation.source().sendMessage(entry.archive(c, archiverUUID));
            } catch (SQLException e) {
                invocation.source().sendMessage(Component.text("Archive command sql error: " + e.getMessage(), NamedTextColor.RED));
                e.printStackTrace();
            }
        }).schedule();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("susp.archive");
    }
}
