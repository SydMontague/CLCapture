package de.craftlancer.clcapture.commands;

import org.bukkit.command.CommandSender;

import de.craftlancer.clcapture.CLCapture;
import de.craftlancer.core.command.SubCommandHandler;

public class CapturePointCommandHandler extends SubCommandHandler {

    public CapturePointCommandHandler(CLCapture plugin, int depth) {
        super(CLCapture.ADMIN_PERMISSION, plugin, true, depth);
        
        registerSubCommand("list", new PointListCommand(plugin));
        registerSubCommand("tp", new PointTPCommand(plugin));
        registerSubCommand("start", new PointStartCommand(plugin));
    }

    @Override
    public void help(CommandSender sender) {
        // TODO Auto-generated method stub
        
    }
    
}
