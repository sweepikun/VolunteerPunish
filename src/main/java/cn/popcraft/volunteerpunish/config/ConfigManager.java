package cn.popcraft.volunteerpunish.config;

import cn.popcraft.volunteerpunish.VolunteerPunish;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final VolunteerPunish plugin;
    private FileConfiguration config;
    
    // 数据库配置
    private String databaseType;
    private String sqlitePath;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    
    // 身份组配置
    private Map<String, GroupConfig> groups;
    
    // 其他配置
    private boolean enableDailyReset;
    private String resetTimezone;
    private boolean enableLoginNotification;
    private Map<String, String> notificationMessages;
    private Map<String, TitleConfig> titleMessages;
    private Map<String, String> actionbarMessages;
    
    public ConfigManager(VolunteerPunish plugin) {
        this.plugin = plugin;
        this.groups = new HashMap<>();
        this.notificationMessages = new HashMap<>();
        this.titleMessages = new HashMap<>();
        this.actionbarMessages = new HashMap<>();
        loadConfig();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // 加载数据库配置
        databaseType = config.getString("database.type", "sqlite");
        sqlitePath = config.getString("database.sqlite.path", "database.db");
        mysqlHost = config.getString("database.mysql.host", "localhost");
        mysqlPort = config.getInt("database.mysql.port", 3306);
        mysqlDatabase = config.getString("database.mysql.database", "volunteerpunish");
        mysqlUsername = config.getString("database.mysql.username", "root");
        mysqlPassword = config.getString("database.mysql.password", "");
        
        // 加载身份组配置
        loadGroups();
        
        // 加载其他配置
        enableDailyReset = config.getBoolean("daily-reset.enabled", true);
        resetTimezone = config.getString("daily-reset.timezone", "UTC");
        enableLoginNotification = config.getBoolean("notification.login.enabled", true);
        
        // 加载通知消息
        notificationMessages.put("ban", config.getString("notification.messages.ban", 
            "&c你已被志愿者 #{volunteer_id} 封禁 {duration}，原因：{reason}。解封时间：{unban_time}"));
        notificationMessages.put("mute", config.getString("notification.messages.mute", 
            "&c你已被志愿者 #{volunteer_id} 禁言 {duration}，原因：{reason}"));
            
        // 加载Title消息
        titleMessages.put("ban", new TitleConfig(
            config.getString("notification.title.ban.title", "&c你已被封禁"),
            config.getString("notification.title.ban.subtitle", "&7原因: {reason}")
        ));
        titleMessages.put("mute", new TitleConfig(
            config.getString("notification.title.mute.title", "&c你已被禁言"),
            config.getString("notification.title.mute.subtitle", "&7原因: {reason}")
        ));
        
        // 加载ActionBar消息
        actionbarMessages.put("ban", config.getString("notification.actionbar.ban", "&c你当前处于封禁状态"));
        actionbarMessages.put("mute", config.getString("notification.actionbar.mute", "&c你当前处于禁言状态"));
    }
    
    private void loadGroups() {
        groups.clear();
        if (config.contains("groups")) {
            for (String groupName : config.getConfigurationSection("groups").getKeys(false)) {
                int banQuota = config.getInt("groups." + groupName + ".quotas.ban", 0);
                int muteQuota = config.getInt("groups." + groupName + ".quotas.mute", 0);
                List<Integer> banDurations = config.getIntegerList("groups." + groupName + ".durations.ban");
                List<Integer> muteDurations = config.getIntegerList("groups." + groupName + ".durations.mute");
                boolean allowCustomBanReason = config.getBoolean("groups." + groupName + ".reasons.ban", true);
                boolean allowCustomMuteReason = config.getBoolean("groups." + groupName + ".reasons.mute", true);
                
                GroupConfig groupConfig = new GroupConfig(groupName, banQuota, muteQuota, 
                    banDurations, muteDurations, allowCustomBanReason, allowCustomMuteReason);
                groups.put(groupName, groupConfig);
            }
        }
    }
    
    // Getters
    public String getDatabaseType() {
        return databaseType;
    }
    
    public String getSqlitePath() {
        return sqlitePath;
    }
    
    public String getMysqlHost() {
        return mysqlHost;
    }
    
    public int getMysqlPort() {
        return mysqlPort;
    }
    
    public String getMysqlDatabase() {
        return mysqlDatabase;
    }
    
    public String getMysqlUsername() {
        return mysqlUsername;
    }
    
    public String getMysqlPassword() {
        return mysqlPassword;
    }
    
    public Map<String, GroupConfig> getGroups() {
        return groups;
    }
    
    public boolean isEnableDailyReset() {
        return enableDailyReset;
    }
    
    public String getResetTimezone() {
        return resetTimezone;
    }
    
    public boolean isEnableLoginNotification() {
        return enableLoginNotification;
    }
    
    public Map<String, String> getNotificationMessages() {
        return notificationMessages;
    }
    
    public Map<String, TitleConfig> getTitleMessages() {
        return titleMessages;
    }
    
    public Map<String, String> getActionbarMessages() {
        return actionbarMessages;
    }
    
    public static class GroupConfig {
        private final String name;
        private final int banQuota;
        private final int muteQuota;
        private final List<Integer> banDurations;
        private final List<Integer> muteDurations;
        private final boolean allowCustomBanReason;
        private final boolean allowCustomMuteReason;
        
        public GroupConfig(String name, int banQuota, int muteQuota, 
                          List<Integer> banDurations, List<Integer> muteDurations,
                          boolean allowCustomBanReason, boolean allowCustomMuteReason) {
            this.name = name;
            this.banQuota = banQuota;
            this.muteQuota = muteQuota;
            this.banDurations = banDurations;
            this.muteDurations = muteDurations;
            this.allowCustomBanReason = allowCustomBanReason;
            this.allowCustomMuteReason = allowCustomMuteReason;
        }
        
        public String getName() {
            return name;
        }
        
        public int getBanQuota() {
            return banQuota;
        }
        
        public int getMuteQuota() {
            return muteQuota;
        }
        
        public List<Integer> getBanDurations() {
            return banDurations;
        }
        
        public List<Integer> getMuteDurations() {
            return muteDurations;
        }
        
        public boolean isAllowCustomBanReason() {
            return allowCustomBanReason;
        }
        
        public boolean isAllowCustomMuteReason() {
            return allowCustomMuteReason;
        }
    }
    
    public static class TitleConfig {
        private final String title;
        private final String subtitle;
        
        public TitleConfig(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getSubtitle() {
            return subtitle;
        }
    }
}