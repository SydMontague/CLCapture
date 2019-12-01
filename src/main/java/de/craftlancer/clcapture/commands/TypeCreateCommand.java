package de.craftlancer.clcapture.commands;

import java.util.Optional;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import de.craftlancer.clcapture.CLCapture;
import de.craftlancer.clcapture.CapturePointType;

public class TypeCreateCommand extends CaptureSubCommand {
    
    public TypeCreateCommand(CLCapture plugin) {
        super("", plugin, true);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 3)
            return "You must specify a type id.";
        
        Optional<CapturePointType> type = getPlugin().getTypes().values().stream().filter(a -> a.getName().equals(args[2])).findFirst();
        
        if (type.isPresent())
            return "A type with this name exists already.";
        
        getPlugin().addType(new CapturePointType(args[2]));
        return null;
    }
    
    @Override
    public void help(CommandSender sender) {
        // TODO Auto-generated method stub
        
    }
    
}
