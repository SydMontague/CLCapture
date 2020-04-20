package de.craftlancer.clcapture.commands;

import de.craftlancer.clcapture.CLCapture;
import de.craftlancer.clcapture.CapturePoint;
import de.craftlancer.clcapture.CapturePointType;
import org.bukkit.Location;
import org.bukkit.Material;
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
        Location chestLocation = (Location) player.getTargetBlock(null, 5).getLocation();
        String type = args[2];
        String name = args[3];
        String id = args[4];
        if (!plugin.getTypes().containsKey(type))
            sender.sendMessage("This capture point type does not exist!");
        else if (name.isEmpty() || id.isEmpty())
            sender.sendMessage("You must specify a name and an ID!");
        else if (plugin.getPoints().stream().map(CapturePoint::getName).anyMatch(a -> a.equals(name)))
            sender.sendMessage("A capture point with this name already exists!");
        else if (plugin.getPoints().stream().map(CapturePoint::getId).anyMatch(a -> a.equals(name)))
            sender.sendMessage("A capture point with this id already exists!");
        else if (chestLocation.getBlock().getType() != Material.CHEST && chestLocation.getBlock().getType() != Material.TRAPPED_CHEST)
            sender.sendMessage("You must be looking at a chest!");
        else {
            CapturePoint point = new CapturePoint(this.plugin, name, id, plugin.getTypes().get(type), chestLocation.getBlock());
            plugin.addPoint(point);
        }
        return "You have successfully added a point!";
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
