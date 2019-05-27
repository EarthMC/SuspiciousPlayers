package com.karlofduty.SuspiciousPlayers;

import com.karlofduty.SuspiciousPlayers.commands.*;
import com.karlofduty.SuspiciousPlayers.listeners.JoinListener;
import com.zaxxer.hikari.HikariDataSource;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public class SuspiciousPlayers extends JavaPlugin
{
    public static SuspiciousPlayers instance;
    public FileConfiguration config;

    private HikariDataSource datasource;

    @Override
    public void onEnable()
    {
        instance = this;
        this.saveDefaultConfig();
        config = this.getConfig();

        connect();
        createTables();

        Objects.requireNonNull(this.getCommand("suspadd")).setExecutor(new AddCommand(this));
        Objects.requireNonNull(this.getCommand("susplist")).setExecutor(new ListCommand(this));
        Objects.requireNonNull(this.getCommand("susparchive")).setExecutor(new ArchiveCommand(this));
        Objects.requireNonNull(this.getCommand("suspunarchive")).setExecutor(new UnarchiveCommand(this));
        Objects.requireNonNull(this.getCommand("suspdelete")).setExecutor(new DeleteCommand(this));
        Objects.requireNonNull(this.getCommand("suspreload")).setExecutor(new ReloadCommand(this));
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        log("Suspicious Players Loaded.");
    }

    @Override
    public void onDisable()
    {
        datasource.close();
    }

    public void notify(BaseComponent[] message)
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if(player.hasPermission("susp.notify"))
            {
                player.spigot().sendMessage(message);
            }
        }
    }

    public void notify(String message)
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if(player.hasPermission("susp.notify"))
            {
                player.sendMessage(message);
            }
        }
    }

    public Connection getConnection() throws SQLException
    {
        return datasource.getConnection();
    }

    public String reload()
    {
        boolean error = false;
        try
        {
            datasource.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            error = true;
        }

        try
        {
            config = this.getConfig();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            error = true;
        }


        try
        {
            connect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            error = true;
        }

        return error ? "Plugin reloaded with errors." : "Plugin reloaded successfully";
    }

    private void connect()
    {
        datasource = new HikariDataSource();
        datasource.setDataSourceClassName("mariadb".equalsIgnoreCase(config.getString("database.type")) ? "org.mariadb.jdbc.MariaDbDataSource" : "com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        datasource.addDataSourceProperty("serverName", config.getString("database.hostname"));
        datasource.addDataSourceProperty("port", config.getInt("database.port"));
        datasource.addDataSourceProperty("databaseName", config.getString("database.name"));
        datasource.addDataSourceProperty("user", config.getString("database.user"));
        datasource.addDataSourceProperty("password", config.getString("database.password"));
    }

    private void createTables()
    {
        try (Connection connection = datasource.getConnection(); Statement statement = connection.createStatement())
        {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS active_entries(" +
                "id INT UNSIGNED NOT NULL UNIQUE PRIMARY KEY AUTO_INCREMENT," +
                "created_time TIMESTAMP NOT NULL," +
                "creator_uuid VARCHAR(36) NOT NULL," +
                "suspicious_uuid VARCHAR(36) NOT NULL," +
                "entry VARCHAR(2000) NOT NULL," +
                "INDEX(suspicious_uuid))");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS archived_entries(" +
                "id INT UNSIGNED NOT NULL UNIQUE PRIMARY KEY AUTO_INCREMENT," +
                "archived_time TIMESTAMP NOT NULL," +
                "archiver_uuid VARCHAR(36) NOT NULL," +
                "created_time TIMESTAMP NOT NULL," +
                "creator_uuid VARCHAR(36) NOT NULL," +
                "suspicious_uuid VARCHAR(36) NOT NULL," +
                "entry VARCHAR(2000) NOT NULL," +
                "INDEX(suspicious_uuid))");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS deleted_entries(" +
                "id INT UNSIGNED NOT NULL UNIQUE PRIMARY KEY AUTO_INCREMENT," +
                "deleted_time TIMESTAMP NOT NULL," +
                "deleter_uuid VARCHAR(36) NOT NULL," +
                "archived_time TIMESTAMP NOT NULL," +
                "archiver_uuid VARCHAR(36) NOT NULL," +
                "created_time TIMESTAMP NOT NULL," +
                "creator_uuid VARCHAR(36) NOT NULL," +
                "suspicious_uuid VARCHAR(36) NOT NULL," +
                "entry VARCHAR(2000) NOT NULL," +
                "INDEX(suspicious_uuid))");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static boolean executeCommand(String command)
    {
        return instance.getServer().dispatchCommand(instance.getServer().getConsoleSender(), command);
    }

    public static boolean isInt(String s)
    {
        try
        {
            Integer.parseInt(s);
        }
        catch(NumberFormatException e)
        {
            return false;
        }
        return true;
    }

    public static void log(String message)
    {
        instance.getServer().getLogger().info(message);
    }
}