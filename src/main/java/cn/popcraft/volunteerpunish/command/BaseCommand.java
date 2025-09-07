package cn.popcraft.volunteerpunish.command;

import cn.popcraft.volunteerpunish.VolunteerPunish;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {
    protected final VolunteerPunish plugin;
    
    public BaseCommand(VolunteerPunish plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        execute(sender, command, label, args);
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return tabComplete(sender, command, alias, args);
    }
    
    protected abstract void execute(CommandSender sender, Command command, String label, String[] args);
    
    protected List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}