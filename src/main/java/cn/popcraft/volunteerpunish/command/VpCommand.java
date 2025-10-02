package cn.popcraft.volunteerpunish.command;

import cn.popcraft.volunteerpunish.VolunteerPunish;
import cn.popcraft.volunteerpunish.model.Punishment;
import cn.popcraft.volunteerpunish.model.Volunteer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class VpCommand extends BaseCommand {
    private final MuteCommand muteCommand;
    private final UnbanCommand unbanCommand;
    private final UnmuteCommand unmuteCommand;
    private final HistoryCommand historyCommand;
    private final SetIdCommand setIdCommand;
    private final RemoveIdCommand removeIdCommand;
    private final GroupCommand groupCommand; // 新增GroupCommand

    public VpCommand(VolunteerPunish plugin) {
        super(plugin);
        this.muteCommand = new MuteCommand(plugin);
        this.unbanCommand = new UnbanCommand(plugin);
        this.unmuteCommand = new UnmuteCommand(plugin);
        this.historyCommand = new HistoryCommand(plugin);
        this.setIdCommand = new SetIdCommand(plugin);
        this.removeIdCommand = new RemoveIdCommand(plugin);
        this.groupCommand = new GroupCommand(plugin); // 初始化GroupCommand
    }

    @Override
    protected void execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "ban":
                handleBanCommand(sender, args);
                break;
            case "mute":
                handleMuteCommand(sender, args);
                break;
            case "unban":
                handleUnbanCommand(sender, args);
                break;
            case "unmute":
                handleUnmuteCommand(sender, args);
                break;
            case "history":
                historyCommand.execute(sender, command, label, args);
                break;
            case "setid":
                setIdCommand.execute(sender, command, label, args);
                break;
            case "removeid":
                removeIdCommand.execute(sender, command, label, args);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "group": // 添加对group命令的处理
                groupCommand.execute(sender, command, label, args);
                break;
            case "help":
                sendHelpMessage(sender);
                break;
            default:
                sender.sendMessage("§c未知的子命令: " + subCommand);
                sendHelpMessage(sender);
                break;
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // 补全子命令
            List<String> subCommands = Arrays.asList("ban", "mute", "unban", "unmute", "history", "setid", "removeid", "group", "reload", "help");
            return subCommands.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "ban":
                case "mute":
                case "unban":
                case "unmute":
                case "history":
                case "setid":
                case "removeid":
                    if (args.length == 2) {
                        // 补全玩家名
                        return null; // 返回null让Bukkit自动补全在线玩家名
                    } else if (args.length == 3) {
                        if (subCommand.equals("ban") || subCommand.equals("mute")) {
                            // 补全时长参数
                            List<String> durations = getDurationOptions(sender, subCommand);
                            return durations.stream()
                                    .filter(duration -> duration.startsWith(args[2].toLowerCase()))
                                    .collect(Collectors.toList());
                        }
                    }
                    break;
                case "group": // 添加group命令的tab补全
                    return groupCommand.tabComplete(sender, command, alias, args);
                case "reload":
                case "help":
                    // 这些命令没有更多参数
                    break;
            }
        }
        return Collections.emptyList();
    }

    /**
     * 获取配置中定义的时长选项
     */
    private List<String> getDurationOptions(CommandSender sender, String commandType) {
        // 默认时长选项
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String volunteerId = plugin.getVolunteerId(player.getUniqueId());
            if (volunteerId != null) {
                // 获取志愿者所在组
                CompletableFuture<Volunteer> volunteerFuture = plugin.getDatabase().getVolunteerByVolunteerId(volunteerId);
                try {
                    Volunteer volunteer = volunteerFuture.join();
                    if (volunteer != null) {
                        // 获取组配置
                        String groupName = volunteer.getGroupName();
                        cn.popcraft.volunteerpunish.config.ConfigManager.GroupConfig groupConfig = 
                            plugin.getConfigManager().getGroups().get(groupName);
                        
                        if (groupConfig != null) {
                            // 根据命令类型获取时长列表
                            if ("ban".equals(commandType) && !groupConfig.getBanDurations().isEmpty()) {
                                return groupConfig.getBanDurations().stream()
                                        .map(String::valueOf)
                                        .collect(Collectors.toList());
                            } else if ("mute".equals(commandType) && !groupConfig.getMuteDurations().isEmpty()) {
                                return groupConfig.getMuteDurations().stream()
                                        .map(String::valueOf)
                                        .collect(Collectors.toList());
                            }
                        }
                    }
                } catch (Exception e) {
                    // 忽略异常，使用默认选项
                }
            }
        }
        
        // 默认选项
        return Arrays.asList("60", "300", "600", "3600", "86400", "604800", "permanent");
    }
    
    /**
     * 获取命令执行者的ID
     */
    private String getExecutorId(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String volunteerId = plugin.getVolunteerId(player.getUniqueId());
            return volunteerId != null ? volunteerId : "ADMIN";
        }
        return "CONSOLE";
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§e--------- §6VolunteerPunish 帮助§e ---------");
        sender.sendMessage("§a/vp ban <玩家> <时长(秒)|permanent> [原因] §7- 封禁玩家");
        sender.sendMessage("§a/vp mute <玩家> <时长(秒)|permanent> [原因] §7- 禁言玩家");
        sender.sendMessage("§a/vp unban <玩家> §7- 解封玩家");
        sender.sendMessage("§a/vp unmute <玩家> §7- 解除玩家禁言");
        sender.sendMessage("§a/vp history [玩家] §7- 查看处罚历史记录");
        sender.sendMessage("§a/vp setid <玩家> <ID> §7- 设置志愿者ID");
        sender.sendMessage("§a/vp removeid <玩家> §7- 移除志愿者身份");
        sender.sendMessage("§a/vp group §7- 查看自己的身份组");
        sender.sendMessage("§a/vp group <志愿者ID> <组名> §7- 修改志愿者身份组");
        sender.sendMessage("§a/vp reload §7- 重新加载配置文件");
        sender.sendMessage("§a/vp help §7- 显示此帮助信息");
        sender.sendMessage("§e----------------------------------------");
    }

    private void handleBanCommand(CommandSender sender, String[] args) {
        // 检查参数
        if (args.length < 3) {
            sender.sendMessage("§c用法: /vp ban <玩家> <时长(秒)|permanent> [原因]");
            sender.sendMessage("§c示例: /vp ban Notch 3600 重复违反服务器规定");
            sender.sendMessage("§c提示: 时长单位为秒，使用 permanent 表示永久封禁");
            return;
        }

        String targetName = args[1];
        String durationStr = args[2];
        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;

        // 检查执行者是否为玩家
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以执行此命令");
            return;
        }

        Player player = (Player) sender;
        String volunteerId = plugin.getVolunteerId(player.getUniqueId());

        if (volunteerId == null) {
            sender.sendMessage("§c你不是志愿者，无法执行此命令");
            return;
        }

        // 解析时长
        long duration;
        if (durationStr.equalsIgnoreCase("permanent")) {
            duration = 0; // 永久封禁
        } else {
            try {
                duration = Long.parseLong(durationStr);
                if (duration <= 0) {
                    sender.sendMessage("§c时长必须为正数或使用 permanent 表示永久封禁");
                    sender.sendMessage("§c示例: /vp ban Notch 3600 重复违反服务器规定");
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c无效的时长格式: " + durationStr);
                sender.sendMessage("§c示例: /vp ban Notch 3600 重复违反服务器规定");
                return;
            }
        }

        // 获取目标玩家
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage("§c玩家不存在: " + targetName);
            return;
        }

        UUID targetUuid = target.getUniqueId();

        // 检查目标是否已被封禁
        if (plugin.isBanned(targetUuid)) {
            sender.sendMessage("§c玩家 " + targetName + " 已被封禁");
            return;
        }

        // 检查配额
        CompletableFuture<Boolean> quotaFuture = plugin.checkBanQuota(volunteerId);
        quotaFuture.thenAccept(hasQuota -> {
            if (!hasQuota) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage("§c你今天的封禁配额已用完"));
                return;
            }

            // 创建处罚记录
            Punishment punishment = new Punishment();
            punishment.setTargetUuid(targetUuid);
            punishment.setVolunteerId(volunteerId);
            punishment.setType(Punishment.Type.BAN);
            punishment.setDuration(duration);
            punishment.setReason(reason);
            punishment.setIssuedAt(new Date());
            
            if (duration > 0) {
                punishment.setExpiresAt(new Date(System.currentTimeMillis() + duration * 1000));
            }

            // 保存处罚记录
            CompletableFuture<Void> saveFuture = plugin.getDatabase().savePunishment(punishment);
            saveFuture.thenRun(() -> {
                // 增加志愿者的使用计数
                CompletableFuture<Void> incrementFuture = plugin.incrementBanCount(volunteerId);
                incrementFuture.thenRun(() -> {
                    // 执行封禁
                    plugin.banPlayer(targetUuid);
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§a成功封禁玩家 " + targetName + 
                            (duration > 0 ? (" (" + duration + "秒)") : " (永久)"));
                        
                        // 如果玩家在线，通知他们
                        Player onlineTarget = Bukkit.getPlayer(targetUuid);
                        if (onlineTarget != null && onlineTarget.isOnline()) {
                            String banReason = reason != null ? reason : "违反服务器规定";
                            onlineTarget.kickPlayer("§c你已被封禁\n§7原因: " + banReason + 
                                (duration > 0 ? ("\n§7时长: " + duration + "秒") : "\n§7类型: 永久封禁"));
                        }
                    });
                }).exceptionally(throwable -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§c封禁玩家时发生错误");
                        plugin.getLogger().log(Level.SEVERE, "增加志愿者封禁计数时发生错误", throwable);
                    });
                    return null;
                });
            }).exceptionally(throwable -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§c封禁玩家时发生错误");
                    plugin.getLogger().log(Level.SEVERE, "保存处罚记录时发生错误", throwable);
                });
                return null;
            });
        }).exceptionally(throwable -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§c封禁玩家时发生错误");
                plugin.getLogger().log(Level.SEVERE, "检查配额时发生错误", throwable);
            });
            return null;
        });
    }

    private void handleMuteCommand(CommandSender sender, String[] args) {
        // 检查参数
        if (args.length < 3) {
            sender.sendMessage("§c用法: /vp mute <玩家> <时长(秒)|permanent> [原因]");
            sender.sendMessage("§c示例: /vp mute Notch 3600 重复违反服务器规定");
            sender.sendMessage("§c提示: 时长单位为秒，使用 permanent 表示永久禁言");
            return;
        }

        String targetName = args[1];
        String durationStr = args[2];
        String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;

        // 检查执行者是否为玩家
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以执行此命令");
            return;
        }

        Player player = (Player) sender;
        String volunteerId = plugin.getVolunteerId(player.getUniqueId());

        if (volunteerId == null) {
            sender.sendMessage("§c你不是志愿者，无法执行此命令");
            return;
        }

        // 解析时长
        long duration;
        if (durationStr.equalsIgnoreCase("permanent")) {
            duration = 0; // 永久禁言
        } else {
            try {
                duration = Long.parseLong(durationStr);
                if (duration <= 0) {
                    sender.sendMessage("§c时长必须为正数或使用 permanent 表示永久禁言");
                    sender.sendMessage("§c示例: /vp mute Notch 3600 重复违反服务器规定");
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c无效的时长格式: " + durationStr);
                sender.sendMessage("§c示例: /vp mute Notch 3600 重复违反服务器规定");
                return;
            }
        }

        // 获取目标玩家
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage("§c玩家不存在: " + targetName);
            return;
        }

        UUID targetUuid = target.getUniqueId();

        // 检查目标是否已被禁言
        if (plugin.isMuted(targetUuid)) {
            sender.sendMessage("§c玩家 " + targetName + " 已被禁言");
            return;
        }

        // 检查配额
        CompletableFuture<Boolean> quotaFuture = plugin.checkMuteQuota(volunteerId);
        quotaFuture.thenAccept(hasQuota -> {
            if (!hasQuota) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage("§c你今天的禁言配额已用完"));
                return;
            }

            // 创建处罚记录
            Punishment punishment = new Punishment();
            punishment.setTargetUuid(targetUuid);
            punishment.setVolunteerId(volunteerId);
            punishment.setType(Punishment.Type.MUTE);
            punishment.setDuration(duration);
            punishment.setReason(reason);
            punishment.setIssuedAt(new Date());
            
            if (duration > 0) {
                punishment.setExpiresAt(new Date(System.currentTimeMillis() + duration * 1000));
            }

            // 保存处罚记录
            CompletableFuture<Void> saveFuture = plugin.getDatabase().savePunishment(punishment);
            saveFuture.thenRun(() -> {
                // 增加志愿者的使用计数
                CompletableFuture<Void> incrementFuture = plugin.incrementMuteCount(volunteerId);
                incrementFuture.thenRun(() -> {
                    // 执行禁言
                    plugin.mutePlayer(targetUuid);
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§a成功禁言玩家 " + targetName + 
                            (duration > 0 ? (" (" + duration + "秒)") : " (永久)"));
                        
                        // 如果玩家在线，通知他们
                        Player onlineTarget = Bukkit.getPlayer(targetUuid);
                        if (onlineTarget != null && onlineTarget.isOnline()) {
                            String muteReason = reason != null ? reason : "违反服务器规定";
                            onlineTarget.sendMessage("§c你已被禁言\n§7原因: " + muteReason + 
                                (duration > 0 ? ("\n§7时长: " + duration + "秒") : "\n§7类型: 永久禁言"));
                        }
                    });
                }).exceptionally(throwable -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§c禁言玩家时发生错误");
                        plugin.getLogger().log(Level.SEVERE, "增加志愿者禁言计数时发生错误", throwable);
                    });
                    return null;
                });
            }).exceptionally(throwable -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§c禁言玩家时发生错误");
                    plugin.getLogger().log(Level.SEVERE, "保存处罚记录时发生错误", throwable);
                });
                return null;
            });
        }).exceptionally(throwable -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§c禁言玩家时发生错误");
                plugin.getLogger().log(Level.SEVERE, "检查配额时发生错误", throwable);
            });
            return null;
        });
    }

    private void handleUnbanCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /vp unban <玩家>");
            sender.sendMessage("§c示例: /vp unban Notch");
            return;
        }

        String targetName = args[1];

        // 检查执行者是否为玩家
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以执行此命令");
            return;
        }

        Player player = (Player) sender;
        String volunteerId = plugin.getVolunteerId(player.getUniqueId());

        if (volunteerId == null) {
            sender.sendMessage("§c你不是志愿者，无法执行此命令");
            return;
        }

        // 获取目标玩家
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage("§c玩家不存在: " + targetName);
            return;
        }

        UUID targetUuid = target.getUniqueId();

        // 检查目标是否被封禁
        if (!plugin.isBanned(targetUuid)) {
            sender.sendMessage("§c玩家 " + targetName + " 未被封禁");
            return;
        }

        // 创建解除处罚记录
        Punishment punishment = new Punishment();
        punishment.setTargetUuid(targetUuid);
        punishment.setVolunteerId(volunteerId);
        punishment.setType(Punishment.Type.BAN);
        punishment.setDuration(0);
        punishment.setReason("手动解除封禁");
        punishment.setIssuedAt(new Date());
        punishment.setExpiresAt(new Date()); // 立即过期

        // 保存处罚记录
        CompletableFuture<Void> saveFuture = plugin.getDatabase().savePunishment(punishment);
        saveFuture.thenRun(() -> {
            // 执行解封
            plugin.unbanPlayer(targetUuid);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§a成功解封玩家 " + targetName);
                
                // 如果玩家在线，通知他们
                Player onlineTarget = Bukkit.getPlayer(targetUuid);
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    onlineTarget.sendMessage("§a你已被解除封禁");
                }
            });
        }).exceptionally(throwable -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§c解封玩家时发生错误");
                plugin.getLogger().log(Level.SEVERE, "保存处罚记录时发生错误", throwable);
            });
            return null;
        });
    }

    private void handleUnmuteCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /vp unmute <玩家>");
            sender.sendMessage("§c示例: /vp unmute Notch");
            return;
        }

        String targetName = args[1];

        // 检查执行者是否为玩家
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以执行此命令");
            return;
        }

        Player player = (Player) sender;
        String volunteerId = plugin.getVolunteerId(player.getUniqueId());

        if (volunteerId == null) {
            sender.sendMessage("§c你不是志愿者，无法执行此命令");
            return;
        }

        // 获取目标玩家
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage("§c玩家不存在: " + targetName);
            return;
        }

        UUID targetUuid = target.getUniqueId();

        // 检查目标是否被禁言
        if (!plugin.isMuted(targetUuid)) {
            sender.sendMessage("§c玩家 " + targetName + " 未被禁言");
            return;
        }

        // 创建解除处罚记录
        Punishment punishment = new Punishment();
        punishment.setTargetUuid(targetUuid);
        punishment.setVolunteerId(volunteerId);
        punishment.setType(Punishment.Type.MUTE);
        punishment.setDuration(0);
        punishment.setReason("手动解除禁言");
        punishment.setIssuedAt(new Date());
        punishment.setExpiresAt(new Date()); // 立即过期

        // 保存处罚记录
        CompletableFuture<Void> saveFuture = plugin.getDatabase().savePunishment(punishment);
        saveFuture.thenRun(() -> {
            // 执行解除禁言
            plugin.unmutePlayer(targetUuid);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§a成功解除玩家 " + targetName + " 的禁言");
                
                // 如果玩家在线，通知他们
                Player onlineTarget = Bukkit.getPlayer(targetUuid);
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    onlineTarget.sendMessage("§a你已被解除禁言");
                }
            });
        }).exceptionally(throwable -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§c解除禁言时发生错误");
                plugin.getLogger().log(Level.SEVERE, "保存处罚记录时发生错误", throwable);
            });
            return null;
        });
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("volunteerpunish.admin.reload")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return;
        }

        plugin.reloadConfig();
        sender.sendMessage("§a配置文件已重新加载");
    }
}