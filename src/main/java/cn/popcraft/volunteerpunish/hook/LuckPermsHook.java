package cn.popcraft.volunteerpunish.hook;

import cn.popcraft.volunteerpunish.VolunteerPunish;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class LuckPermsHook {
    private final VolunteerPunish plugin;
    private LuckPerms luckPerms;
    private boolean enabled = false;
    
    public LuckPermsHook(VolunteerPunish plugin) {
        this.plugin = plugin;
        setupLuckPerms();
    }
    
    private void setupLuckPerms() {
        try {
            // 检查LuckPerms类是否存在
            Class.forName("net.luckperms.api.LuckPerms");

            // 尝试通过服务注册获取LuckPerms API
            RegisteredServiceProvider<LuckPerms> provider = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                luckPerms = provider.getProvider();
                enabled = true;
                plugin.getLogger().info("成功连接到LuckPerms API");
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("未检测到LuckPerms插件，将使用基础权限系统");
            enabled = false;
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "连接LuckPerms API时出错: " + e.getMessage());
            enabled = false;
        }
    }
    
    /**
     * 为玩家添加志愿者权限
     * @param player 玩家对象
     * @return 操作是否成功
     */
    public CompletableFuture<Boolean> addVolunteerPermissions(Player player) {
        if (!enabled || luckPerms == null) {
            // 如果没有LuckPerms，发送普通消息
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage("§a你已成为志愿者！ID: " + plugin.getVolunteerId(player.getUniqueId()));
            });
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join();
                if (user == null) {
                    return false;
                }
                
                // 添加志愿者权限
                user.data().add(PermissionNode.builder("volunteerpunish.volunteer.ban").build());
                user.data().add(PermissionNode.builder("volunteerpunish.volunteer.mute").build());
                
                // 保存用户数据
                luckPerms.getUserManager().saveUser(user).join();
                
                // 更新用户权限（推送更改）
                luckPerms.getMessagingService().ifPresent(service -> {
                    service.pushUserUpdate(user);
                });
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§a你已成为志愿者！相关权限已授予。");
                });
                
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "为玩家添加志愿者权限时出错: " + e.getMessage(), e);
                return false;
            }
        });
    }
    
    /**
     * 移除玩家的志愿者权限
     * @param player 玩家对象
     * @return 操作是否成功
     */
    public CompletableFuture<Boolean> removeVolunteerPermissions(Player player) {
        if (!enabled || luckPerms == null) {
            // 如果没有LuckPerms，发送普通消息
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage("§a你已不再是志愿者！");
            });
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join();
                if (user == null) {
                    return false;
                }
                
                // 移除志愿者权限
                user.data().remove(PermissionNode.builder("volunteerpunish.volunteer.ban").build());
                user.data().remove(PermissionNode.builder("volunteerpunish.volunteer.mute").build());
                
                // 保存用户数据
                luckPerms.getUserManager().saveUser(user).join();
                
                // 更新用户权限（推送更改）
                luckPerms.getMessagingService().ifPresent(service -> {
                    service.pushUserUpdate(user);
                });
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§a你已不再是志愿者！相关权限已移除。");
                });
                
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "移除玩家志愿者权限时出错: " + e.getMessage(), e);
                return false;
            }
        });
    }
    
    /**
     * 检查玩家是否拥有指定权限
     * @param player 玩家对象
     * @param permission 权限节点
     * @return 是否拥有权限
     */
    public boolean hasPermission(Player player, String permission) {
        if (!enabled || luckPerms == null) {
            // 如果没有LuckPerms，回落到Bukkit的权限检查
            return player.hasPermission(permission);
        }
        
        try {
            User user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join();
            if (user == null) {
                return false;
            }
            
            return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "检查玩家权限时出错: " + e.getMessage(), e);
            // 出错时回落到Bukkit的权限检查
            return player.hasPermission(permission);
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}