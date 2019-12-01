package de.craftlancer.clcapture.commands;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import de.craftlancer.clcapture.CLCapture;
import de.craftlancer.clcapture.CapturePointType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_14_R1.NBTTagCompound;

public class TypeItemListCommand extends CaptureSubCommand {
    
    public TypeItemListCommand(CLCapture plugin) {
        super("", plugin, true);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 3)
            return "You must specify a type id.";
        
        Optional<CapturePointType> type = getPlugin().getTypes().values().stream().filter(a -> a.getName().equals(args[2])).findFirst();
        
        if (!type.isPresent())
            return "This type does not exist.";
        
        sender.sendMessage("ID - Item - Amount - Action");
        
        int id = 0;
        for(ItemStack a : type.get().getItems()) {
            BaseComponent item = new TextComponent(a.getType().name());
            item.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new BaseComponent[] {
                    new TextComponent(org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack.asNMSCopy(a).save(new NBTTagCompound()).toString()) }));
            
            BaseComponent delAction = new TextComponent("[Delete]");
            delAction.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/capture type itemremove " + type.get().getName() + " " + id));
            
            BaseComponent base = new TextComponent(Integer.toString(id));
            base.addExtra(" - ");
            base.addExtra(item);
            base.addExtra(" - ");
            base.addExtra(Integer.toString(a.getAmount()));
            base.addExtra(" - ");
            base.addExtra(delAction);
            
            sender.spigot().sendMessage(base);
            id++;
        }
        
        return null;
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
