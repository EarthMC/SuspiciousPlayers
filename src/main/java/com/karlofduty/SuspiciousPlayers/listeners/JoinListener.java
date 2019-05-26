package com.karlofduty.SuspiciousPlayers.listeners;

import com.karlofduty.SuspiciousPlayers.JSONChatBuilder;
import com.karlofduty.SuspiciousPlayers.SuspiciousPlayers;

import com.karlofduty.SuspiciousPlayers.models.ActiveEntry;
import static net.md_5.bungee.api.ChatColor.*;

import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JoinListener implements Listener
{
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        BukkitRunnable r = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                try(Connection c = SuspiciousPlayers.instance.getConnection())
                {
                    ResultSet results = c.createStatement().executeQuery("SELECT * FROM active_entries WHERE suspicious_uuid = '" + event.getPlayer().getUniqueId() + "' ORDER BY created_time;");
                    int count = 0;
                    while(results.next())
                    {
                        count++;
                    }
                    if(count == 1)
                    {
                        results.first();
                        SuspiciousPlayers.instance.notify(RED + event.getPlayer().getName() + " has been marked as suspicious:\n" +
                                new ActiveEntry(results).getFormattedString());
                    }
                    else if(count > 1)
                    {
                        SuspiciousPlayers.instance.notify(ComponentSerializer.parse( "[" +
                                JSONChatBuilder.plainText(event.getPlayer().getName() + " has been marked as suspicious " + count + " times!\nDo ", RED) + ',' +
                                JSONChatBuilder.runCommand(YELLOW + "/susplist " + event.getPlayer().getName(), "/susplist " + event.getPlayer().getName()) + ',' +
                                JSONChatBuilder.plainText(" to see why.", RED) + "]"));
                    }
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }
        };
        r.runTaskAsynchronously(SuspiciousPlayers.instance);
    }
}
