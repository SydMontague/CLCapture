package de.craftlancer.clcapture.commands;

import de.craftlancer.clcapture.CLCapture;
import de.craftlancer.clcapture.CapturePointType;
import de.craftlancer.core.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TypeSetDisplayNameCommand extends CaptureSubCommand {
    public TypeSetDisplayNameCommand(CLCapture plugin) {
        super(CLCapture.ADMIN_PERMISSION, plugin, false);
    }
    
    @Override
    protected String execute(CommandSender sender, Command command, String s, String[] args) {
        if (!checkSender(sender))
            return CLCapture.PREFIX + "You must be a player to use this.";
        if (args.length < 3)
            return CLCapture.PREFIX + "You must specify a type id!";
        
        if (args.length < 4)
            return CLCapture.PREFIX + "You must specify a displayname you want to use!";
        
        Optional<CapturePointType> type = getPlugin().getTypes().values().stream().filter(a -> a.getName().equals(args[2])).findFirst();
        
        if (!type.isPresent())
            return CLCapture.PREFIX + "This point does not exist.";
        
        type.get().setDisplayName(args[3]);
        return CLCapture.PREFIX + "§aYou have set the display name to: §2" + args[3] + "§a!";
    }
    
    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 3)
            return Utils.getMatches(args[2], getPlugin().getTypes().values().stream().map(CapturePointType::getName).collect(Collectors.toList()));
        if (args.length == 4)
            return Utils.getMatches(args[3], Collections.singletonList("displayname"));
        
        return Collections.emptyList();
    }
    
    @Override
    public void help(CommandSender commandSender) {
    
    }
}
