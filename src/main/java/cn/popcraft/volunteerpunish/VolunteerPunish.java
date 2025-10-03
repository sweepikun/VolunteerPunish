package cn.popcraft.volunteerpunish;

import cn.popcraft.volunteerpunish.command.VpCommand;
import cn.popcraft.volunteerpunish.config.ConfigManager;
import cn.popcraft.volunteerpunish.database.DatabaseManager;
import cn.popcraft.volunteerpunish.hook.LuckPermsHook;
import cn.popcraft.volunteerpunish.listener.ChatListener;
import cn.popcraft.volunteerpunish.listener.PlayerJoinListener;
import cn.popcraft.volunteerpunish.model.Punishment;
import cn.popcraft.volunteerpunish.model.Volunteer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class VolunteerPunish extends JavaPlugin {
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private LuckPermsHook luckPermsHook;
    private boolean isPluginEnabled = true;
    private VpCommand vpCommand;
    private BukkitTask quotaResetTask;
    
    @Override
    public void onEnable() {
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        
        // 初始化数据库管理器
        databaseManager = new DatabaseManager(this);
        
        // 初始化LuckPermsHook
        try {
            luckPermsHook = new LuckPermsHook(this);
        } catch (Throwable e) {
            getLogger().log(Level.WARNING, "初始化LuckPermsHook时出错: " + e.getMessage());
            luckPermsHook = null;
        }
        
        // 初始化命令
        vpCommand = new VpCommand(this);
        
        // 注册命令
        org.bukkit.command.PluginCommand command = this.getCommand("vp");
        if (command != null) {
            command.setExecutor(vpCommand);
            command.setTabCompleter(vpCommand);
        }
        
        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        
        // 启动配额重置任务
        startQuotaResetTask();
        
        getLogger().info("VolunteerPunish 插件已启用");
    }
    
    @Override
    public void onDisable() {
        // 取消配额重置任务
        if (quotaResetTask != null) {
            quotaResetTask.cancel();
        }
        
        // 关闭数据库连接
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("VolunteerPunish 插件已禁用");
    }
    
    /**
     * 启动每日配额重置任务
     */
    private void startQuotaResetTask() {
        // 检查是否启用每日重置功能
        if (!configManager.isEnableDailyReset()) {
            getLogger().info("每日配额重置功能已禁用");
            return;
        }
        
        // 计算到下次重置的时间
        long delay = calculateNextResetDelay();
        getLogger().info("配额重置任务将在 " + delay + " 秒后首次执行");
        
        // 安排定时任务（每天执行一次）
        quotaResetTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            getLogger().info("开始执行每日配额重置任务");
            databaseManager.resetDailyQuotas().thenRun(() -> {
                getLogger().info("每日配额重置任务执行完成");
            }).exceptionally(throwable -> {
                getLogger().severe("每日配额重置任务执行失败: " + throwable.getMessage());
                return null;
            });
        }, delay * 20L, 24 * 60 * 60 * 20L); // 延迟指定秒数后开始，然后每24小时执行一次
    }
    
    /**
     * 计算到下次重置的延迟时间（秒）
     * @return 延迟秒数
     */
    private long calculateNextResetDelay() {
        // 获取配置的时区
        String timezoneStr = configManager.getResetTimezone();
        ZoneId timezone;
        try {
            timezone = ZoneId.of(timezoneStr);
        } catch (Exception e) {
            getLogger().warning("无效的时区配置: " + timezoneStr + "，使用默认时区 UTC");
            timezone = ZoneId.of("UTC");
        }
        
        // 设置重置时间为每天的 UTC 00:00（或配置的时区对应时间）
        LocalTime resetTime = LocalTime.MIDNIGHT;
        LocalTime now = LocalTime.now(timezone);
        LocalDate today = LocalDate.now(timezone);
        
        // 计算下次重置日期
        LocalDate nextResetDate = today;
        if (!now.isBefore(resetTime)) {
            // 如果当前时间已经过了今天的重置时间，则下次重置是明天
            nextResetDate = today.plusDays(1);
        }
        
        // 计算延迟秒数
        long secondsUntilReset = java.time.Duration.between(now, resetTime.atDate(nextResetDate)).getSeconds();
        return secondsUntilReset;
    }
    
    // 实现获取志愿者ID的逻辑
    public String getVolunteerId(UUID uuid) {
        if (!isPluginEnabled) {
            return null;
        }
        
        // 同步等待获取志愿者信息
        try {
            Volunteer volunteer = databaseManager.getVolunteerByUuid(uuid).join();
            return volunteer != null ? volunteer.getVolunteerId() : null;
        } catch (Exception e) {
            getLogger().severe("获取志愿者ID时发生错误: " + e.getMessage());
            return null;
        }
    }
    
    public boolean isBanned(UUID uuid) {
        if (!isPluginEnabled) {
            return false;
        }
        
        // 同步检查玩家是否被封禁
        try {
            return databaseManager.isBanned(uuid).join();
        } catch (Exception e) {
            getLogger().severe("检查玩家是否被封禁时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isMuted(UUID uuid) {
        if (!isPluginEnabled) {
            return false;
        }
        
        // 同步检查玩家是否被禁言
        try {
            return databaseManager.isMuted(uuid).join();
        } catch (Exception e) {
            getLogger().severe("检查玩家是否被禁言时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    public void banPlayer(UUID uuid) {
        if (!isPluginEnabled) {
            return;
        }
        
        // 如果玩家在线，则将其踢出服务器
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.kickPlayer("§c你已被封禁\n§7请遵守服务器规定");
        }
    }
    
    public void mutePlayer(UUID uuid) {
        // 禁言逻辑将在事件监听器中处理
        // 这里可以添加其他需要的逻辑
    }
    
    public void unbanPlayer(UUID uuid) {
        if (!isPluginEnabled) {
            return;
        }
        
        // 异步停用该玩家的所有封禁记录
        CompletableFuture.runAsync(() -> {
            try {
                databaseManager.deactivatePunishments(uuid, Punishment.Type.BAN).join();
                
                // 如果玩家在线，发送解封通知
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    getServer().getScheduler().runTask(this, () -> 
                        player.sendMessage("§a你已被解封"));
                }
            } catch (Exception e) {
                getLogger().severe("解封玩家时发生错误: " + e.getMessage());
            }
        });
    }
    
    public void unmutePlayer(UUID uuid) {
        if (!isPluginEnabled) {
            return;
        }
        
        // 异步停用该玩家的所有禁言记录
        CompletableFuture.runAsync(() -> {
            try {
                databaseManager.deactivatePunishments(uuid, Punishment.Type.MUTE).join();
                
                // 如果玩家在线，发送解除禁言通知
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    getServer().getScheduler().runTask(this, () -> 
                        player.sendMessage("§a你已被解除禁言"));
                }
            } catch (Exception e) {
                getLogger().severe("解除玩家禁言时发生错误: " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<Boolean> checkBanQuota(String volunteerId) {
        if (!isPluginEnabled) {
            return CompletableFuture.completedFuture(false);
        }
        
        // 异步检查封禁配额
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 查找志愿者
                Volunteer volunteer = databaseManager.getVolunteerByVolunteerId(volunteerId).join();
                if (volunteer == null) {
                    return false;
                }
                
                // 获取该志愿者所在组的配额
                ConfigManager.GroupConfig groupConfig = configManager.getGroups().get(volunteer.getGroupName());
                if (groupConfig == null) {
                    return false;
                }
                
                // 检查配额
                return volunteer.getDailyBanUsed() < groupConfig.getBanQuota();
            } catch (Exception e) {
                getLogger().severe("检查封禁配额时发生错误: " + e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> checkMuteQuota(String volunteerId) {
        if (!isPluginEnabled) {
            return CompletableFuture.completedFuture(false);
        }
        
        // 异步检查禁言配额
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 查找志愿者
                Volunteer volunteer = databaseManager.getVolunteerByVolunteerId(volunteerId).join();
                if (volunteer == null) {
                    return false;
                }
                
                // 获取该志愿者所在组的配额
                ConfigManager.GroupConfig groupConfig = configManager.getGroups().get(volunteer.getGroupName());
                if (groupConfig == null) {
                    return false;
                }
                
                // 检查配额
                return volunteer.getDailyMuteUsed() < groupConfig.getMuteQuota();
            } catch (Exception e) {
                getLogger().severe("检查禁言配额时发生错误: " + e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<Void> incrementBanCount(String volunteerId) {
        if (!isPluginEnabled) {
            return CompletableFuture.completedFuture(null);
        }
        
        // 异步增加封禁计数
        return CompletableFuture.runAsync(() -> {
            try {
                // 查找志愿者
                Volunteer volunteer = databaseManager.getVolunteerByVolunteerId(volunteerId).join();
                if (volunteer == null) {
                    return;
                }
                
                // 增加计数
                volunteer.setDailyBanUsed(volunteer.getDailyBanUsed() + 1);
                
                // 保存更新
                databaseManager.saveVolunteer(volunteer).join();
            } catch (Exception e) {
                getLogger().severe("增加封禁计数时发生错误: " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<Void> incrementMuteCount(String volunteerId) {
        if (!isPluginEnabled) {
            return CompletableFuture.completedFuture(null);
        }
        
        // 异步增加禁言计数
        return CompletableFuture.runAsync(() -> {
            try {
                // 查找志愿者
                Volunteer volunteer = databaseManager.getVolunteerByVolunteerId(volunteerId).join();
                if (volunteer == null) {
                    return;
                }
                
                // 增加计数
                volunteer.setDailyMuteUsed(volunteer.getDailyMuteUsed() + 1);
                
                // 保存更新
                databaseManager.saveVolunteer(volunteer).join();
            } catch (Exception e) {
                getLogger().severe("增加禁言计数时发生错误: " + e.getMessage());
            }
        });
    }
    
    public DatabaseManager getDatabase() {
        return databaseManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }
}