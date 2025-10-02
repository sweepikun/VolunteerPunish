package cn.popcraft.volunteerpunish.command;

import cn.popcraft.volunteerpunish.VolunteerPunish;
import cn.popcraft.volunteerpunish.config.ConfigManager;
import cn.popcraft.volunteerpunish.model.Volunteer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GroupCommand extends BaseCommand {
    public GroupCommand(VolunteerPunish plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, Command command, String label, String[] args) {
        // 检查是否是玩家执行命令
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以执行此命令");
            return;
        }

        Player player = (Player) sender;
        
        // 如果是查询自己的组
        if (args.length == 1) {
            // 异步获取志愿者信息
            CompletableFuture.runAsync(() -> {
                try {
                    Volunteer volunteer = plugin.getDatabase().getVolunteerByUuid(player.getUniqueId()).join();
                    
                    // 在主线程中发送消息
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (volunteer == null) {
                            player.sendMessage("§c你不是志愿者");
                        } else {
                            player.sendMessage("§a你的身份组: §e" + volunteer.getGroupName());
                            
                            // 显示组的详细信息
                            ConfigManager.GroupConfig groupConfig = plugin.getConfigManager().getGroups()
                                    .get(volunteer.getGroupName());
                            
                            if (groupConfig != null) {
                                player.sendMessage("§a封禁配额: §e" + volunteer.getDailyBanUsed() + "/" + groupConfig.getBanQuota());
                                player.sendMessage("§a禁言配额: §e" + volunteer.getDailyMuteUsed() + "/" + groupConfig.getMuteQuota());
                            }
                        }
                    });
                } catch (Exception e) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§c查询身份组信息时发生错误");
                        plugin.getLogger().severe("查询玩家 " + player.getName() + " 的身份组信息时发生错误: " + e.getMessage());
                    });
                }
            });
            return;
        }
        
        // 如果是管理员操作修改他人组
        if (args.length == 3) {
            // 检查权限
            if (!sender.hasPermission("volunteerpunish.admin.setgroup")) {
                sender.sendMessage("§c你没有权限执行此命令");
                return;
            }
            
            String targetName = args[1];
            String newGroupName = args[2];
            
            // 检查组是否存在
            if (!plugin.getConfigManager().getGroups().containsKey(newGroupName)) {
                sender.sendMessage("§c身份组不存在: " + newGroupName);
                sender.sendMessage("§c可用的身份组: " + String.join(", ", plugin.getConfigManager().getGroups().keySet()));
                return;
            }
            
            // 异步处理修改组
            CompletableFuture.runAsync(() -> {
                try {
                    // 获取目标玩家的志愿者信息
                    Volunteer volunteer = plugin.getDatabase().getVolunteerByVolunteerId(targetName).join();
                    
                    if (volunteer == null) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage("§c未找到志愿者: " + targetName);
                        });
                        return;
                    }
                    
                    // 更新组
                    String oldGroup = volunteer.getGroupName();
                    volunteer.setGroupName(newGroupName);
                    plugin.getDatabase().saveVolunteer(volunteer).join();
                    
                    // 在主线程中发送消息
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§a成功将志愿者 " + targetName + " 的身份组从 " + oldGroup + " 更改为 " + newGroupName);
                    });
                } catch (Exception e) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§c修改志愿者身份组时发生错误");
                        plugin.getLogger().severe("修改志愿者 " + targetName + " 的身份组时发生错误: " + e.getMessage());
                    });
                }
            });
            return;
        }
        
        // 显示帮助信息
        sender.sendMessage("§c用法:");
        sender.sendMessage("§c/vp group - 查看自己的身份组");
        sender.sendMessage("§c/vp group <志愿者ID> <组名> - 修改志愿者的身份组(需要管理员权限)");
    }
    
    @Override
    protected List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2 && sender.hasPermission("volunteerpunish.admin.setgroup")) {
            // 提供志愿者ID补全
            return Collections.emptyList(); // 简化处理，实际可查询数据库获取所有志愿者ID
        } else if (args.length == 3 && sender.hasPermission("volunteerpunish.admin.setgroup")) {
            // 提供组名补全
            return plugin.getConfigManager().getGroups().keySet().stream()
                    .filter(group -> group.startsWith(args[2].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return Collections.emptyList();
    }
}