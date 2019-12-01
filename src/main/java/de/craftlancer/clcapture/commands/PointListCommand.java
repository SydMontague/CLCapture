package de.craftlancer.clcapture.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import de.craftlancer.clcapture.CLCapture;
import de.craftlancer.clcapture.CapturePoint.CapturePointState;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;

public class PointListCommand extends CaptureSubCommand {
    
    public PointListCommand(CLCapture plugin) {
        super("", plugin, true);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage("ID Name Type Location - Next Action");
        
        getPlugin().getPoints().forEach(a -> {
            BaseComponent tpAction = new TextComponent("[TP]");
            tpAction.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/capture point tp " + a.getId()));
            BaseComponent startAction = new TextComponent("[Start]");
            startAction.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/capture point start " + a.getId()));
            
            BaseComponent base = new TextComponent(a.getId());
            base.addExtra(" ");
            base.addExtra(a.getName());
            base.addExtra(" ");
            base.addExtra(a.getType().getName());
            base.addExtra(" ");
            Location loc = a.getChestLoction();
            base.addExtra(Integer.toString(loc.getBlockX()));
            base.addExtra(",");
            base.addExtra(Integer.toString(loc.getBlockY()));
            base.addExtra(",");
            base.addExtra(Integer.toString(loc.getBlockZ()));
            base.addExtra(" - ");
            base.addExtra(a.getState() == CapturePointState.ACTIVE ? "ACTIVE" : a.getNextTime().toString());
            base.addExtra(" ");
            
            base.addExtra(tpAction);
            base.addExtra(startAction);
            
            sender.spigot().sendMessage(base);
        });
        return null;
    }
    
    @Override
    public void help(CommandSender sender) {
        // TODO Auto-generated method stub
        
    }
    
}
