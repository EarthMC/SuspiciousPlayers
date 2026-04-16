package com.karlofduty.SuspiciousPlayers;

import com.karlofduty.SuspiciousPlayers.commands.*;
import com.karlofduty.SuspiciousPlayers.listeners.JoinListener;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class SuspiciousPlayers extends JavaPlugin {
    private static SuspiciousPlayers instance;
    private final Logger logger = getSLF4JLogger();
    private FileConfiguration config;
    private HikariDataSource datasource;
    public final Server server = getServer();

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        config = this.getConfig();

        initializeDatasource();
        createTables();

        Objects.requireNonNull(this.getCommand("suspadd")).setExecutor(new AddCommand(this));
        Objects.requireNonNull(this.getCommand("susplist")).setExecutor(new ListCommand(this));

        Objects.requireNonNull(this.getCommand("susparchive")).setExecutor(new ArchiveCommand(this));
        Objects.requireNonNull(this.getCommand("suspunarchive")).setExecutor(new UnarchiveCommand(this));
        Objects.requireNonNull(this.getCommand("suspdelete")).setExecutor(new DeleteCommand(this));
        Objects.requireNonNull(this.getCommand("suspreload")).setExecutor(new ReloadCommand(this));
        Objects.requireNonNull(this.getCommand("susponline")).setExecutor(new OnlineCommand(this));

        Objects.requireNonNull(this.getCommand("tpnext")).setExecutor(new TPNextCommand());
        Objects.requireNonNull(this.getCommand("tpprev")).setExecutor(new TPPrevCommand());
        Objects.requireNonNull(this.getCommand("suspnext")).setExecutor(new SuspNextCommand());
        Objects.requireNonNull(this.getCommand("suspprev")).setExecutor(new SuspPrevCommand());

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getLogger().info("Suspicious Players Loaded.");
    }

    @Override
    public void onDisable() {
        datasource.close();
    }

    public void notify(Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("susp.notify")) {
                player.sendMessage(message);
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return datasource.getConnection();
    }

    public Component reload() {
        boolean error = false;
        try {
            datasource.close();
        } catch (Exception e) {
            logger.warn("Exception while closing datasource in reload:", e);
            error = true;
        }
        config = this.getConfig();

        try {
            initializeDatasource();
        } catch (Exception e) {
            logger.warn("Exception while initializing datasource in reload:", e);
            error = true;
        }

        return error ? Component.text("Plugin reloaded with errors.", NamedTextColor.RED) : Component.text("Plugin reloaded successfully", NamedTextColor.GREEN);
    }

    /**
     * Sets up the mysql datasource
     */
    private void initializeDatasource() {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getString("database.hostname") + ":" + config.getString("database.port") + "/" + config.getString("database.name"));

        hikariConfig.setUsername(config.getString("database.user"));
        hikariConfig.setPassword(config.getString("database.password"));

        this.datasource = new HikariDataSource(hikariConfig);
    }

    /**
     * Creates all tables that do not already exist in the database
     */
    private void createTables() {
        try (Connection connection = datasource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS active_entries(" + "id INT UNSIGNED NOT NULL UNIQUE PRIMARY KEY AUTO_INCREMENT," + "created_time TIMESTAMP NOT NULL," + "creator_uuid VARCHAR(36) NOT NULL," + "suspicious_uuid VARCHAR(36) NOT NULL," + "entry VARCHAR(2000) NOT NULL," + "INDEX(suspicious_uuid))");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS archived_entries(" + "id INT UNSIGNED NOT NULL UNIQUE PRIMARY KEY AUTO_INCREMENT," + "archived_time TIMESTAMP NOT NULL," + "archiver_uuid VARCHAR(36) NOT NULL," + "created_time TIMESTAMP NOT NULL," + "creator_uuid VARCHAR(36) NOT NULL," + "suspicious_uuid VARCHAR(36) NOT NULL," + "entry VARCHAR(2000) NOT NULL," + "INDEX(suspicious_uuid))");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS deleted_entries(" + "id INT UNSIGNED NOT NULL UNIQUE PRIMARY KEY AUTO_INCREMENT," + "deleted_time TIMESTAMP NOT NULL," + "deleter_uuid VARCHAR(36) NOT NULL," + "archived_time TIMESTAMP NOT NULL," + "archiver_uuid VARCHAR(36) NOT NULL," + "created_time TIMESTAMP NOT NULL," + "creator_uuid VARCHAR(36) NOT NULL," + "suspicious_uuid VARCHAR(36) NOT NULL," + "entry VARCHAR(2000) NOT NULL," + "INDEX(suspicious_uuid))");
        } catch (SQLException e) {
            logger.warn("SQLException while creating tables:", e);
        }
    }

    /**
     * Small utility function to check if the contents of a string is an int
     *
     * @param s The string
     * @return True if yes, false if no.
     */
    public static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public Logger logger() {
        return logger;
    }

    public static SuspiciousPlayers plugin() {
        return instance;
    }
}
