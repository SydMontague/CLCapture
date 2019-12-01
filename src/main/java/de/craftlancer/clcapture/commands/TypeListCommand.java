package de.craftlancer.clcapture.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import de.craftlancer.clcapture.CLCapture;

public class TypeListCommand extends CaptureSubCommand {
    
    public TypeListCommand(CLCapture plugin) {
        super("", plugin, true);
    }
    
    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage("Name - CapTime - #Items - #Times - #PMods");
        
        getPlugin().getTypes().values()
                   .forEach(a -> sender.sendMessage(String.format("%s - %d - %d - %d - %d",
                                                                  a.getName(),
                                                                  a.getCaptureTime(),
                                                                  a.getItems().size(),
                                                                  a.getTimes().size(),
                                                                  a.getPlayerModifier().size())));

        return null;
    }
    
    @Override
    public void help(CommandSender sender) {
        // TODO Auto-generated method stub
        
    }
    
}
