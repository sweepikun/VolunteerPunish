package cn.popcraft.volunteerpunish.command;

import cn.popcraft.volunteerpunish.VolunteerPunish;
import cn.popcraft.volunteerpunish.model.Volunteer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RemoveIdCommand extends BaseCommand {
    public RemoveIdCommand(VolunteerPunish plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, Command command, String label, String[] args) {
        // 检查权限
        if (!sender.hasPermission("volunteerpunish.admin.removeid")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /vp removeid <玩家>");
            sender.sendMessage("§c示例: /vp removeid Notch");
            sender.sendMessage("§c说明: 移除指定玩家的志愿者身份");
            return;
        }

        String targetName = args[1];

        // 获取目标玩家
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage("§c玩家不存在: " + targetName);
            return;
        }

        UUID targetUuid = target.getUniqueId();

        // 异步处理移除志愿者ID
        CompletableFuture.runAsync(() -> {
            try {
                // 获取志愿者信息
                Volunteer volunteer = plugin.getDatabase().getVolunteerByUuid(targetUuid).join();
                
                // 如果志愿者不存在
                if (volunteer == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage("§c玩家 " + targetName + " 不是志愿者"));
                    return;
                }
                
                // 删除志愿者记录
                plugin.getDatabase().removeVolunteer(targetUuid).join();
                
                // 如果玩家在线，移除其权限
                Player onlineTarget = target.getPlayer();
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    // 使用LuckPermsHook移除权限
                    if (plugin.getLuckPermsHook() != null) {
                        plugin.getLuckPermsHook().removeVolunteerPermissions(onlineTarget);
                    } else {
                        // 如果LuckPermsHook不可用，发送普通消息
                        onlineTarget.sendMessage("§a你已不再是志愿者！");
                    }
                }
                
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage("§a成功移除玩家 " + targetName + " 的志愿者身份"));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§c移除志愿者身份时发生错误");
                    plugin.getLogger().severe("移除玩家 " + targetName + " 的志愿者身份时发生错误: " + e.getMessage());
                });
            }
        });
    }
    
    @Override
    protected java.util.List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) {
            // 补全玩家名
            return null; // 返回null让Bukkit自动补全在线玩家名
        }
        return super.tabComplete(sender, command, alias, args);
    }
}