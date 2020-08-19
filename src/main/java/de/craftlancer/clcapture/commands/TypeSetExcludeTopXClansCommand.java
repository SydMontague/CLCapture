package de.craftlancer.clcapture.commands;

import de.craftlancer.clcapture.CLCapture;
import de.craftlancer.clcapture.CapturePointType;
import de.craftlancer.core.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TypeSetExcludeTopXClansCommand extends CaptureSubCommand {
    public TypeSetExcludeTopXClansCommand(CLCapture plugin) {
        super(CLCapture.ADMIN_PERMISSION, plugin, false);
    }
    
    @Override
    protected String execute(CommandSender sender, Command command, String s, String[] args) {
        if (!checkSender(sender))
            return CLCapture.PREFIX + "You must be a player to use this.";
        if (args.length < 3)
            return CLCapture.PREFIX + "You must specify a type id!";
    
        if (args.length < 4)
            return CLCapture.PREFIX + "You must specify a number!";
    
        Optional<CapturePointType> type = getPlugin().getTypes().values().stream().filter(a -> a.getName().equals(args[2])).findFirst();
    
        if (!type.isPresent())
            return CLCapture.PREFIX + "This point does not exist.";
    
        int excludeXClans;
        try {
            excludeXClans = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            return CLCapture.PREFIX + "You must specify an integer!";
        }
        
        type.get().setExcludeTopXClans(excludeXClans);
        return CLCapture.PREFIX + "§aThis point will now exclude the top §b" + excludeXClans + " §aclans.";
    }
    
    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 3)
            return Utils.getMatches(args[2], getPlugin().getTypes().values().stream().map(CapturePointType::getName).collect(Collectors.toList()));
        if (args.length == 4)
            return Utils.getMatches(args[3], Collections.singletonList("#"));
    
        return Collections.emptyList();
    }
    
    @Override
    public void help(CommandSender commandSender) {
    
    }
}
