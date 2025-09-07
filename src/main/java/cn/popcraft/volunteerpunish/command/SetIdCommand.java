package cn.popcraft.volunteerpunish.command;

import cn.popcraft.volunteerpunish.VolunteerPunish;
import cn.popcraft.volunteerpunish.model.Volunteer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SetIdCommand extends BaseCommand {
    public SetIdCommand(VolunteerPunish plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, Command command, String label, String[] args) {
        // 检查权限
        if (!sender.hasPermission("volunteerpunish.admin.setid")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§c用法: /vp setid <玩家> <新ID>");
            sender.sendMessage("§c示例: /vp setid Notch 01");
            sender.sendMessage("§c说明: 为指定玩家设置志愿者ID");
            return;
        }

        String targetName = args[1];
        String newVolunteerId = args[2];

        // 获取目标玩家
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage("§c玩家不存在: " + targetName);
            return;
        }

        UUID targetUuid = target.getUniqueId();

        // 异步处理设置志愿者ID
        CompletableFuture.runAsync(() -> {
            try {
                // 获取志愿者信息
                Volunteer volunteer = plugin.getDatabase().getVolunteerByUuid(targetUuid).join();
                
                // 如果志愿者不存在，则创建新的志愿者记录
                if (volunteer == null) {
                    volunteer = new Volunteer();
                    volunteer.setUuid(targetUuid);
                    volunteer.setVolunteerId(newVolunteerId);
                    volunteer.setGroupName("default"); // 默认组
                    volunteer.setDailyBanUsed(0);
                    volunteer.setDailyMuteUsed(0);
                    
                    // 保存新的志愿者记录
                    plugin.getDatabase().saveVolunteer(volunteer).join();
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§a成功为玩家 " + targetName + " 创建志愿者记录，ID设置为: " + newVolunteerId);
                        
                        // 如果玩家在线，给予权限
                        Player onlineTarget = target.getPlayer();
                        if (onlineTarget != null && onlineTarget.isOnline()) {
                            // 使用LuckPermsHook添加权限
                            if (plugin.getLuckPermsHook() != null) {
                                plugin.getLuckPermsHook().addVolunteerPermissions(onlineTarget);
                            } else {
                                // 如果LuckPermsHook不可用，发送普通消息
                                onlineTarget.sendMessage("§a你已成为志愿者！ID: " + newVolunteerId);
                            }
                        }
                    });
                } else {
                    // 更新现有的志愿者ID
                    String oldVolunteerId = volunteer.getVolunteerId();
                    volunteer.setVolunteerId(newVolunteerId);
                    
                    // 保存更新后的志愿者记录
                    plugin.getDatabase().saveVolunteer(volunteer).join();
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§a成功将玩家 " + targetName + " 的志愿者ID从 " + oldVolunteerId + " 更改为 " + newVolunteerId);
                        
                        // 如果玩家在线，通知更新
                        Player onlineTarget = target.getPlayer();
                        if (onlineTarget != null && onlineTarget.isOnline()) {
                            // 使用LuckPermsHook更新权限（实际上权限不会改变，只是通知）
                            if (plugin.getLuckPermsHook() != null && plugin.getLuckPermsHook().isEnabled()) {
                                onlineTarget.sendMessage("§a你的志愿者ID已更新为: " + newVolunteerId);
                            } else {
                                // 如果LuckPerms不可用，发送普通消息
                                onlineTarget.sendMessage("§a你的志愿者ID已更新为: " + newVolunteerId);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§c设置志愿者ID时发生错误");
                    plugin.getLogger().severe("设置玩家 " + targetName + " 的志愿者ID时发生错误: " + e.getMessage());
                });
            }
        });
    }
    
    @Override
    protected List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) {
            // 补全玩家名
            return null; // 返回null让Bukkit自动补全在线玩家名
        } else if (args.length == 3) {
            // 提供一些示例ID
            List<String> exampleIds = Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10");
            return exampleIds.stream()
                    .filter(id -> id.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, command, alias, args);
    }
}