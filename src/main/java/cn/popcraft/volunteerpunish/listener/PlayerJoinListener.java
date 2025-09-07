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
        if (!plugin.getConfigManager().isEnableLoginNotification()) {
            return;
        }
        
        Player player = event.getPlayer();
        CompletableFuture<List<Punishment>> future = plugin.getDatabase().getPunishmentsByTargetUuid(player.getUniqueId());
        
        future.thenAccept(punishments -> {
            // 查找活跃的处罚
            for (Punishment punishment : punishments) {
                if (!punishment.isActive()) {
                    continue;
                }
                
                // 检查是否已过期
                if (punishment.getExpiresAt() != null && punishment.getExpiresAt().before(new Date())) {
                    continue;
                }
                
                // 显示处罚通知
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    showPunishmentNotification(player, punishment);
                });
                
                // 目前只显示最新的一个处罚
                break;
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to check punishments for player " + player.getName() + ": " + throwable.getMessage());
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