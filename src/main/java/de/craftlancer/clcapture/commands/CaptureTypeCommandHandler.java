package de.craftlancer.clcapture.commands;

import org.bukkit.command.CommandSender;

import de.craftlancer.clcapture.CLCapture;
import de.craftlancer.core.command.SubCommandHandler;

public class CaptureTypeCommandHandler extends SubCommandHandler {
    
    public CaptureTypeCommandHandler(CLCapture plugin, int depth) {
        super(CLCapture.ADMIN_PERMISSION, plugin, true, depth);

        registerSubCommand("list", new TypeListCommand(plugin));
        registerSubCommand("create", new TypeCreateCommand(plugin));
        registerSubCommand("delete", new TypeDeleteCommand(plugin));
        registerSubCommand("capturetime", new TypeCapturetimeCommand(plugin));
        registerSubCommand("broadcast", new TypeBroadcastCommand(plugin));
        registerSubCommand("distance", new TypeBossbarDistanceCommand(plugin));
        registerSubCommand("times", new TypeTimeListCommand(plugin));
        registerSubCommand("timeadd", new TypeTimeAddCommand(plugin));
        registerSubCommand("timeremove", new TypeTimeRemoveCommand(plugin));
        registerSubCommand("items", new TypeItemListCommand(plugin));
        registerSubCommand("itemadd", new TypeItemAddCommand(plugin));
        registerSubCommand("itemremove", new TypeItemRemoveCommand(plugin));
        registerSubCommand("pmods", new TypePModListCommand(plugin));
        registerSubCommand("pmodadd", new TypePModAddCommand(plugin));
        registerSubCommand("pmodremove", new TypePModRemoveCommand(plugin));
    }

    @Override
    public void help(CommandSender sender) {
        // TODO Auto-generated method stub
        
    }

}
