package cn.popcraft.volunteerpunish.listener;

import cn.popcraft.volunteerpunish.VolunteerPunish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final VolunteerPunish plugin;
    
    public ChatListener(VolunteerPunish plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // 检查玩家是否被禁言
        if (plugin.isMuted(event.getPlayer().getUniqueId())) {
            // 取消聊天事件
            event.setCancelled(true);
            
            // 发送禁言提示消息
            event.getPlayer().sendMessage("§c你已被禁言，无法发送消息");
        }
    }
}