package de.craftlancer.clcapture.commands;

import de.craftlancer.clcapture.CLCapture;
import de.craftlancer.core.command.SubCommand;

public abstract class CaptureSubCommand extends SubCommand {
    
    public CaptureSubCommand(String permission, CLCapture plugin, boolean console) {
        super(permission, plugin, console);
    }
    
    @Override
    public CLCapture getPlugin() {
        return (CLCapture) super.getPlugin();
    }

}
