package cn.popcraft.volunteerpunish.database;

import cn.popcraft.volunteerpunish.VolunteerPunish;
import cn.popcraft.volunteerpunish.config.ConfigManager;
import cn.popcraft.volunteerpunish.model.Punishment;
import cn.popcraft.volunteerpunish.model.Volunteer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {
    private final VolunteerPunish plugin;
    private HikariDataSource dataSource;
    private final String databaseType;
    
    public DatabaseManager(VolunteerPunish plugin) {
        this.plugin = plugin;
        ConfigManager config = plugin.getConfigManager();
        this.databaseType = config.getDatabaseType();
        setupDatabase();
    }
    
    private void setupDatabase() {
        try {
            if ("sqlite".equalsIgnoreCase(databaseType)) {
                setupSQLite();
            } else if ("mysql".equalsIgnoreCase(databaseType)) {
                setupMySQL();
            } else {
                plugin.getLogger().severe("Unsupported database type: " + databaseType);
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
            
            createTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to setup database", e);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }
    
    private void setupSQLite() {
        ConfigManager config = plugin.getConfigManager();
        String path = new File(plugin.getDataFolder(), config.getSqlitePath()).getAbsolutePath();
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + path);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setMaximumPoolSize(10);
        
        dataSource = new HikariDataSource(hikariConfig);
    }
    
    private void setupMySQL() {
        ConfigManager config = plugin.getConfigManager();
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getMysqlHost() + ":" + config.getMysqlPort() + "/" + config.getMysqlDatabase());
        hikariConfig.setUsername(config.getMysqlUsername());
        hikariConfig.setPassword(config.getMysqlPassword());
        hikariConfig.setMaximumPoolSize(10);
        
        dataSource = new HikariDataSource(hikariConfig);
    }
    
    private void createTables() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // 创建 volunteers 表
            try (Statement statement = connection.createStatement()) {
                String createVolunteersTable = "CREATE TABLE IF NOT EXISTS volunteers (" +
                        "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                        "uuid VARCHAR(36) NOT NULL UNIQUE, " +
                        "group_name VARCHAR(32) NOT NULL, " +
                        "volunteer_id VARCHAR(8) NOT NULL, " +
                        "daily_ban_used INTEGER NOT NULL DEFAULT 0, " +
                        "daily_mute_used INTEGER NOT NULL DEFAULT 0, " +
                        "last_reset DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ")";
                
                // SQLite 使用不同的自增语法
                if ("sqlite".equalsIgnoreCase(databaseType)) {
                    createVolunteersTable = createVolunteersTable.replace("AUTO_INCREMENT", "");
                }
                
                statement.executeUpdate(createVolunteersTable);
            }
            
            // 创建 punishments 表
            try (Statement statement = connection.createStatement()) {
                String createPunishmentsTable = "CREATE TABLE IF NOT EXISTS punishments (" +
                        "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                        "target_uuid VARCHAR(36) NOT NULL, " +
                        "volunteer_id VARCHAR(8) NOT NULL, " +
                        "type VARCHAR(10) NOT NULL, " +
                        "duration INTEGER NOT NULL, " +
                        "reason TEXT, " +
                        "issued_at DATETIME NOT NULL, " +
                        "expires_at DATETIME, " +
                        "is_active BOOLEAN NOT NULL DEFAULT TRUE" +
                        ")";
                
                // SQLite 使用不同的自增语法
                if ("sqlite".equalsIgnoreCase(databaseType)) {
                    createPunishmentsTable = createPunishmentsTable.replace("AUTO_INCREMENT", "");
                }
                
                statement.executeUpdate(createPunishmentsTable);
            }
        }
    }
    
    public CompletableFuture<Volunteer> getVolunteerByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM volunteers WHERE uuid = ?")) {
                
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        Volunteer volunteer = new Volunteer();
                        volunteer.setId(rs.getInt("id"));
                        volunteer.setUuid(UUID.fromString(rs.getString("uuid")));
                        volunteer.setGroupName(rs.getString("group_name"));
                        volunteer.setVolunteerId(rs.getString("volunteer_id"));
                        volunteer.setDailyBanUsed(rs.getInt("daily_ban_used"));
                        volunteer.setDailyMuteUsed(rs.getInt("daily_mute_used"));
                        volunteer.setLastReset(rs.getTimestamp("last_reset"));
                        return volunteer;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get volunteer by UUID: " + uuid, e);
            }
            return null;
        });
    }
    
    public CompletableFuture<Volunteer> getVolunteerByVolunteerId(String volunteerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM volunteers WHERE volunteer_id = ?")) {
                
                statement.setString(1, volunteerId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        Volunteer volunteer = new Volunteer();
                        volunteer.setId(rs.getInt("id"));
                        volunteer.setUuid(UUID.fromString(rs.getString("uuid")));
                        volunteer.setGroupName(rs.getString("group_name"));
                        volunteer.setVolunteerId(rs.getString("volunteer_id"));
                        volunteer.setDailyBanUsed(rs.getInt("daily_ban_used"));
                        volunteer.setDailyMuteUsed(rs.getInt("daily_mute_used"));
                        volunteer.setLastReset(rs.getTimestamp("last_reset"));
                        return volunteer;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get volunteer by ID: " + volunteerId, e);
            }
            return null;
        });
    }
    
    public CompletableFuture<Void> saveVolunteer(Volunteer volunteer) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                String sql = "INSERT OR REPLACE INTO volunteers (uuid, group_name, volunteer_id, daily_ban_used, daily_mute_used, last_reset) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
                
                // MySQL 使用不同的语法
                if ("mysql".equalsIgnoreCase(databaseType)) {
                    sql = "INSERT INTO volunteers (uuid, group_name, volunteer_id, daily_ban_used, daily_mute_used, last_reset) " +
                            "VALUES (?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "group_name = VALUES(group_name), " +
                            "volunteer_id = VALUES(volunteer_id), " +
                            "daily_ban_used = VALUES(daily_ban_used), " +
                            "daily_mute_used = VALUES(daily_mute_used), " +
                            "last_reset = VALUES(last_reset)";
                }
                
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, volunteer.getUuid().toString());
                    statement.setString(2, volunteer.getGroupName());
                    statement.setString(3, volunteer.getVolunteerId());
                    statement.setInt(4, volunteer.getDailyBanUsed());
                    statement.setInt(5, volunteer.getDailyMuteUsed());
                    statement.setTimestamp(6, volunteer.getLastReset());
                    
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save volunteer: " + volunteer.getUuid(), e);
            }
        });
    }
    
    public CompletableFuture<Void> removeVolunteer(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                String sql = "DELETE FROM volunteers WHERE uuid = ?";
                
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, uuid.toString());
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove volunteer: " + uuid, e);
            }
        });
    }
    
    public CompletableFuture<List<Punishment>> getPunishmentsByTargetUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM punishments WHERE target_uuid = ? ORDER BY issued_at DESC")) {
                
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Punishment punishment = new Punishment();
                        punishment.setId(rs.getInt("id"));
                        punishment.setTargetUuid(UUID.fromString(rs.getString("target_uuid")));
                        punishment.setVolunteerId(rs.getString("volunteer_id"));
                        punishment.setType(Punishment.Type.valueOf(rs.getString("type")));
                        punishment.setDuration(rs.getLong("duration"));
                        punishment.setReason(rs.getString("reason"));
                        punishment.setIssuedAt(rs.getTimestamp("issued_at"));
                        punishment.setExpiresAt(rs.getTimestamp("expires_at"));
                        punishment.setActive(rs.getBoolean("is_active"));
                        punishments.add(punishment);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get punishments by target UUID: " + uuid, e);
            }
            return punishments;
        });
    }
    
    public CompletableFuture<Boolean> isBanned(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT 1 FROM punishments WHERE target_uuid = ? AND type = 'BAN' AND is_active = TRUE AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)")) {
                
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next(); // 如果有结果，说明玩家被封禁
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check if player is banned: " + uuid, e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> isMuted(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT 1 FROM punishments WHERE target_uuid = ? AND type = 'MUTE' AND is_active = TRUE AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)")) {
                
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next(); // 如果有结果，说明玩家被禁言
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check if player is muted: " + uuid, e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Void> savePunishment(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                String sql = "INSERT INTO punishments (target_uuid, volunteer_id, type, duration, reason, issued_at, expires_at, is_active) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, punishment.getTargetUuid().toString());
                    statement.setString(2, punishment.getVolunteerId());
                    statement.setString(3, punishment.getType().name());
                    statement.setLong(4, punishment.getDuration());
                    statement.setString(5, punishment.getReason());
                    statement.setTimestamp(6, new Timestamp(punishment.getIssuedAt().getTime()));
                    
                    if (punishment.getExpiresAt() != null) {
                        statement.setTimestamp(7, new Timestamp(punishment.getExpiresAt().getTime()));
                    } else {
                        statement.setNull(7, Types.TIMESTAMP);
                    }
                    
                    statement.setBoolean(8, punishment.isActive());
                    
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save punishment for target: " + punishment.getTargetUuid(), e);
            }
        });
    }
    
    public CompletableFuture<Void> deactivatePunishments(UUID targetUuid, Punishment.Type type) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                String sql = "UPDATE punishments SET is_active = FALSE WHERE target_uuid = ? AND type = ? AND is_active = TRUE";
                
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, targetUuid.toString());
                    statement.setString(2, type.name());
                    
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to deactivate punishments for target: " + targetUuid + ", type: " + type, e);
            }
        });
    }
    
    /**
     * 重置所有志愿者的每日配额计数
     * @return CompletableFuture表示操作完成
     */
    public CompletableFuture<Void> resetDailyQuotas() {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                String sql = "UPDATE volunteers SET daily_ban_used = 0, daily_mute_used = 0, last_reset = CURRENT_TIMESTAMP";
                
                // SQLite使用不同的时间戳函数
                if ("sqlite".equalsIgnoreCase(databaseType)) {
                    sql = "UPDATE volunteers SET daily_ban_used = 0, daily_mute_used = 0, last_reset = datetime('now')";
                }
                
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    int updatedRows = statement.executeUpdate();
                    plugin.getLogger().info("已重置 " + updatedRows + " 名志愿者的每日配额");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to reset daily quotas", e);
            }
        });
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}