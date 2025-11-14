package cn.popcraft.volunteerpunish.listener;

import cn.popcraft.volunteerpunish.VolunteerPunish;
import cn.popcraft.volunteerpunish.config.ConfigManager;
import cn.popcraft.volunteerpunish.model.Punishment;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayerJoinListener implements Listener {
    private final VolunteerPunish plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public PlayerJoinListener(VolunteerPunish plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否被封禁
        CompletableFuture<List<Punishment>> future = plugin.getDatabase().getPunishmentsByTargetUuid(player.getUniqueId());
        
        future.thenAccept(punishments -> {
            // 查找活跃的封禁处罚
            Punishment activeBan = null;
            for (Punishment punishment : punishments) {
                if (punishment.getType() == Punishment.Type.BAN && punishment.isActive()) {
                    // 检查是否已过期
                    if (punishment.getExpiresAt() == null || punishment.getExpiresAt().after(new Date())) {
                        activeBan = punishment;
                        break;
                    }
                }
            }
            
            if (activeBan != null) {
                // 玩家被封禁，阻止加入并显示封禁信息
                // 创建final变量用于lambda表达式
                final Punishment finalBan = activeBan;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    String banReason = finalBan.getReason() != null ? finalBan.getReason() : "违反服务器规定";
                    String duration = "永久";
                    if (finalBan.getDuration() > 0) {
                        duration = formatDuration(finalBan.getDuration());
                    }
                    
                    String kickMessage = "§c你已被封禁\n" +
                                       "§7原因: " + banReason + "\n" +
                                       "§7时长: " + duration + "\n" +
                                       "§7封禁者: " + finalBan.getVolunteerId();
                    
                    player.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMessage));
                    event.setJoinMessage(null); // 清除加入消息
                });
                return;
            }
            
            // 如果没有封禁但有其他处罚（如禁言），则显示通知
            if (plugin.getConfigManager().isEnableLoginNotification()) {
                Punishment activeMute = null;
                for (Punishment punishment : punishments) {
                    if (punishment.getType() == Punishment.Type.MUTE && punishment.isActive()) {
                        // 检查是否已过期
                        if (punishment.getExpiresAt() == null || punishment.getExpiresAt().after(new Date())) {
                            activeMute = punishment;
                            break;
                        }
                    }
                }
                
                if (activeMute != null) {
                    // 创建final变量用于lambda表达式
                    final Punishment finalMute = activeMute;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        showPunishmentNotification(player, finalMute);
                    });
                }
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("检查玩家处罚状态时发生错误: " + throwable.getMessage());
            return null;
        });
    }
    
    private void showPunishmentNotification(Player player, Punishment punishment) {
        String message = formatMessage(punishment);
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        
        // 发送聊天消息
        player.sendMessage(coloredMessage);
        
        // 发送Title
        ConfigManager.TitleConfig titleConfig = plugin.getConfigManager().getTitleMessages()
                .getOrDefault(punishment.getType().name().toLowerCase(), 
                        new ConfigManager.TitleConfig("&c你已被" + (punishment.getType() == Punishment.Type.BAN ? "封禁" : "禁言"), 
                                "&7原因: " + (punishment.getReason() != null ? punishment.getReason() : "未指定")));
        
        String title = ChatColor.translateAlternateColorCodes('&', 
                titleConfig.getTitle().replace("{reason}", punishment.getReason() != null ? punishment.getReason() : "未指定"));
        String subtitle = ChatColor.translateAlternateColorCodes('&', 
                titleConfig.getSubtitle().replace("{reason}", punishment.getReason() != null ? punishment.getReason() : "未指定"));
        
        player.sendTitle(title, subtitle, 10, 70, 20);
        
        // 发送ActionBar消息
        String actionbarMessage = plugin.getConfigManager().getActionbarMessages()
                .getOrDefault(punishment.getType().name().toLowerCase(), 
                        "&c你当前处于" + (punishment.getType() == Punishment.Type.BAN ? "封禁" : "禁言") + "状态");
        actionbarMessage = ChatColor.translateAlternateColorCodes('&', 
                actionbarMessage.replace("{reason}", punishment.getReason() != null ? punishment.getReason() : "未指定"));
        
        // 使用BungeeCord API发送ActionBar消息
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionbarMessage));
    }
    
    private String formatMessage(Punishment punishment) {
        String template = plugin.getConfigManager().getNotificationMessages()
                .getOrDefault(punishment.getType().name().toLowerCase(), 
                        "&c你已被志愿者 #{volunteer_id} {type} {duration}，原因：{reason}");
        
        String duration = "永久";
        if (punishment.getDuration() > 0) {
            duration = formatDuration(punishment.getDuration());
        }
        
        String unbanTime = "永久";
        if (punishment.getExpiresAt() != null) {
            unbanTime = dateFormat.format(punishment.getExpiresAt());
        }
        
        return template
                .replace("{volunteer_id}", punishment.getVolunteerId())
                .replace("{type}", punishment.getType() == Punishment.Type.BAN ? "封禁" : "禁言")
                .replace("{duration}", duration)
                .replace("{reason}", punishment.getReason() != null ? punishment.getReason() : "未指定")
                .replace("{unban_time}", unbanTime);
    }
    
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "小时";
        } else {
            return (seconds / 86400) + "天";
        }
    }
}