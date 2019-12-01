package de.craftlancer.clcapture.commands;

import de.craftlancer.clcapture.CLCapture;
import de.craftlancer.core.command.CommandHandler;

public class CaptureCommandHandler extends CommandHandler {

    public CaptureCommandHandler(CLCapture plugin) {
        super(plugin);
        
        registerSubCommand("type", new CaptureTypeCommandHandler(plugin, 1));
        registerSubCommand("point", new CapturePointCommandHandler(plugin, 1));
    }
    
}
