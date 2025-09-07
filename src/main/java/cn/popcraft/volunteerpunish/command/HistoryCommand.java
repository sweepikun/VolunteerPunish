package cn.popcraft.volunteerpunish.command;

import cn.popcraft.volunteerpunish.VolunteerPunish;
import cn.popcraft.volunteerpunish.model.Punishment;
import cn.popcraft.volunteerpunish.model.Volunteer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HistoryCommand extends BaseCommand {
    public HistoryCommand(VolunteerPunish plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /vp history <玩家ID或名称>");
            sender.sendMessage("§c示例: /vp history Notch");
            sender.sendMessage("§c说明: 查看指定玩家的所有处罚记录");
            return;
        }

        String target = args[1];
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(target);
        UUID targetUuid = offlinePlayer.getUniqueId();
        
        // 检查玩家是否存在
        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage("§c玩家不存在: " + target);
            return;
        }

        // 异步查询历史记录
        CompletableFuture<List<Punishment>> future = plugin.getDatabase().getPunishmentsByTargetUuid(targetUuid);
        future.thenAccept(punishments -> {
            // 回到主线程发送消息
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (punishments.isEmpty()) {
                    sender.sendMessage("§a玩家 " + target + " 没有处罚记录");
                    return;
                }

                sender.sendMessage("§a玩家 §e" + target + " §a的处罚历史记录:");
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                for (Punishment punishment : punishments) {
                    String volunteerId = punishment.getVolunteerId();
                    String type = punishment.getType() == Punishment.Type.BAN ? "封禁" : "禁言";
                    String duration = punishment.getDuration() > 0 ? punishment.getDuration() + "秒" : "永久";
                    String reason = punishment.getReason() != null ? punishment.getReason() : "无";
                    String issuedAt = dateFormat.format(punishment.getIssuedAt());
                    
                    sender.sendMessage("§7[ID: " + punishment.getId() + "] §b" + type + " §f时长: §e" + duration + 
                            " §f原因: §e" + reason + " §f执行者: §e" + volunteerId + " §f时间: §e" + issuedAt);
                }
            });
        }).exceptionally(throwable -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§c查询历史记录时发生错误");
                plugin.getLogger().severe("查询玩家 " + target + " 的处罚历史记录时发生错误: " + throwable.getMessage());
            });
            return null;
        });
    }
    
    @Override
    protected List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) {
            // 获取所有曾经玩过服务器的玩家
            List<String> playerNames = Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .filter(name -> name != null && name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());

            return playerNames;
        }
        return super.tabComplete(sender, command, alias, args);
    }
}