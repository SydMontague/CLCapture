package de.craftlancer.clcapture.commands;

import de.craftlancer.clcapture.CLCapture;
import de.craftlancer.clcapture.CapturePointType;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PointAddCommand extends CaptureSubCommand {
    private CLCapture plugin;
    PointAddCommand(CLCapture plugin) {
        super(CLCapture.ADMIN_PERMISSION, plugin, true);
        this.plugin = plugin;
    }
    
    @Override
    protected String execute(CommandSender sender, Command cmd, String s, String[] args) {
        if (!checkSender(sender))
            return "You're not allowed to use this command.";
        if (!(sender instanceof Player))
            return "You must be a player to use this command!";
        if (args.length < 3)
            return "Please enter a type!";
        if (args.length < 4)
            return "Please enter a name!";
        if (args.length < 5)
            return "Please enter an ID!";
        
        Player player = ((Player) sender).getPlayer();
        Block targettedBlock = player.getTargetBlock(null, 5);
        plugin.pointAdd(player,targettedBlock.getLocation(),args[2], args[3],args[4]);
        return null;
    }
    
    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 3)
            return getPlugin().getTypes().values().stream().map(CapturePointType::getName).collect(Collectors.toList());
        if (args.length == 4)
            return Arrays.asList("NAME");
        if (args.length == 5)
            return Arrays.asList("ID");
        return Collections.emptyList();
    }
    
    @Override
    public void help(CommandSender commandSender) {
    
    }
}
