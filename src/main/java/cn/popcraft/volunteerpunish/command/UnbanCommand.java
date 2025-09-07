package cn.popcraft.volunteerpunish.command;

import cn.popcraft.volunteerpunish.VolunteerPunish;
import org.bukkit.command.CommandSender;

public class UnbanCommand extends BaseCommand {
    public UnbanCommand(VolunteerPunish plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        // 这个类的方法将通过VpCommand调用，不需要在这里实现
    }
}