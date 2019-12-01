package de.craftlancer.clcapture.commands;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import de.craftlancer.clcapture.CLCapture;
import de.craftlancer.clcapture.CapturePointType;

public class TypeItemAddCommand extends CaptureSubCommand {
    
    public TypeItemAddCommand(CLCapture plugin) {
        super("", plugin, false);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkSender(sender))
            return "You must be a player to use this.";
        if (args.length < 3)
            return "You must specify a type id.";
        
        Optional<CapturePointType> type = getPlugin().getTypes().values().stream().filter(a -> a.getName().equals(args[2])).findFirst();
        
        if (!type.isPresent())
            return "This type does not exist.";

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if(item.getType().isAir())
            return "Please hold an Item in your hand.";
        
        type.get().addItem(item);
        return "Item added.";
    }
    
    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2)
            return getPlugin().getTypes().values().stream().map(CapturePointType::getName).collect(Collectors.toList());
        if (args.length == 3)
            return getPlugin().getTypes().values().stream().map(CapturePointType::getName).filter(a -> a.startsWith(args[2])).collect(Collectors.toList());
        
        return Collections.emptyList();        
    }
    
    @Override
    public void help(CommandSender sender) {
        // TODO Auto-generated method stub
        
    }
    
}
