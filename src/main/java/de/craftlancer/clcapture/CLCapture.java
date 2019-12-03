package de.craftlancer.clcapture;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import de.craftlancer.clcapture.commands.CaptureCommandHandler;
import de.craftlancer.clclans.CLClans;
import de.craftlancer.core.IntRingBuffer;
import de.craftlancer.core.LambdaRunnable;

/*
 * /capture listpoints
 * ID - Name - Location - Next - Owner - Action (Delete, TP, Set Type, Start)
 * 
 * X - CapPointMain1 - ACTIVE - Uncaptured - [TP][Del][TP]
 * 
 * /capture point
 * /capture point list
 * /capture point tp <id>
 * /capture point delete <id>
 * /capture point start <id>
 * /capture type times
 * /capture type addtime <time>
 * /capture type removetime <id>
 * /capture type items
 * /capture type additem
 * /capture type removeitem <id>
 * /capture type pmods
 * /capture type addpmod <amount> <mod>
 * /capture type removepmod <id>
 * /capture type list
 * /capture type create <name>
 * /capture type delete
 * /capture type capturetime <time>
 * 
 * 
 * 
 */

public class CLCapture extends JavaPlugin implements Listener {
    private static final String SIGN_HEADER = "[CapPoints]";
    public static final String ADMIN_PERMISSION = "clcapture.admin";
    
    private static final int PLAYER_BUFFER_SIZE = 60; // number of updates kept
    private static final int PLAYER_BUFFER_FREQUENCY = 60 * 20; // update frequency
    
    private final File pointsFile = new File(getDataFolder(), "points.yml");
    private final File typesFile = new File(getDataFolder(), "types.yml");
    
    private CLClans clanPlugin;
    
    private IntRingBuffer playerCountBuffer = new IntRingBuffer(PLAYER_BUFFER_SIZE);
    private Map<String, CapturePointType> types;
    private List<CapturePoint> points;
    
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        
        clanPlugin = (CLClans) getServer().getPluginManager().getPlugin("CLClans");
        if (clanPlugin == null)
            getLogger().severe("Couldn't find CLClans!");
        
        saveDefaultConfig();
        loadConfig();
        
        getCommand("capture").setExecutor(new CaptureCommandHandler(this));
        
        new LambdaRunnable(this::saveTypes).runTaskTimer(this, 18000L, 18000L);
        new LambdaRunnable(() -> points.forEach(CapturePoint::run)).runTaskTimer(this, 1L, 1L);
        new LambdaRunnable(() -> playerCountBuffer.push(Bukkit.getOnlinePlayers().size())).runTaskTimer(this, 0L, PLAYER_BUFFER_FREQUENCY);
    }
    
    @Override
    public void onDisable() {
        List<KeyedBossBar> toRemove = new ArrayList<>();
        
        Bukkit.getBossBars().forEachRemaining(a -> {
            if (a.getKey().getNamespace().equalsIgnoreCase(this.getName())) {
                toRemove.add(a);
                a.removeAll();
            }
        });
        
        toRemove.forEach(a -> Bukkit.removeBossBar(a.getKey()));
        
        savePoints();
        saveTypes();
    }
    
    private void loadConfig() {
        FileConfiguration pointsData = YamlConfiguration.loadConfiguration(pointsFile);
        FileConfiguration typesData = YamlConfiguration.loadConfiguration(typesFile);
        
        types = typesData.getKeys(false).stream().map(key -> new CapturePointType(typesData.getConfigurationSection(key)))
                         .collect(Collectors.toMap(CapturePointType::getName, a -> a));
        
        points = pointsData.getKeys(false).stream().map(key -> new CapturePoint(this, key, pointsData.getConfigurationSection(key)))
                           .collect(Collectors.toList());
    }
    
    private void savePoints() {
        YamlConfiguration pointsData = new YamlConfiguration();
        points.forEach(a -> a.save(pointsData));
        
        new LambdaRunnable(() -> {
            try {
                pointsData.save(pointsFile);
            }
            catch (IOException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error while saving points.yml", e);
            }
        }).runTaskAsynchronously(this);
    }
    
    private void saveTypes() {
        try {
            YamlConfiguration typesData = new YamlConfiguration();
            types.forEach((b, a) -> a.save(typesData));
            typesData.save(typesFile);
        }
        catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error while saving types.yml", e);
        }
    }
    
    @Override
    public void saveDefaultConfig() {
        super.saveDefaultConfig();
        
        if (!pointsFile.exists())
            try {
                Files.createFile(pointsFile.toPath());
            }
            catch (IOException e) {
                Bukkit.getLogger().log(Level.WARNING, "Error while creating points.yml", e);
            }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onSignCreation(SignChangeEvent event) {
        if (!event.getLine(0).equalsIgnoreCase(SIGN_HEADER))
            return;
        
        if (!event.getPlayer().hasPermission(ADMIN_PERMISSION)) {
            event.setCancelled(true);
            return;
        }
        
        String type = event.getLine(1);
        String name = event.getLine(2);
        String id = event.getLine(3);
        
        Block chestLocation = event.getBlock().getRelative(BlockFace.DOWN);
        
        if (!types.containsKey(type))
            event.getPlayer().sendMessage("This capture point type does not exist!");
        else if (name.isEmpty() || id.isEmpty())
            event.getPlayer().sendMessage("You must specify a name and an ID!");
        else if (points.stream().map(CapturePoint::getName).anyMatch(a -> a.equals(name)))
            event.getPlayer().sendMessage("A capture point with this name already exists!");
        else if (points.stream().map(CapturePoint::getId).anyMatch(a -> a.equals(name)))
            event.getPlayer().sendMessage("A capture point with this id already exists!");
        else if (chestLocation.getType() != Material.CHEST && chestLocation.getType() != Material.TRAPPED_CHEST)
            event.getPlayer().sendMessage("The sign must be placed directly above a Chest!");
        else {
            CapturePoint point = new CapturePoint(this, name, id, types.get(type), chestLocation, event.getBlock());
            points.add(point);
            savePoints();
            event.getPlayer().sendMessage("CapPoint successfully created!");
        }
    }
    
    public Integer getMaxPlayerCountLastHour() {
        return playerCountBuffer.stream().max().orElseGet(() -> 0);
    }
    
    public CapturePointType getPointType(String string) {
        return types.get(string);
    }
    
    public void removePoint(CapturePoint capturePoint) {
        points.remove(capturePoint);
        HandlerList.unregisterAll(capturePoint);
        capturePoint.destroy();
        savePoints();
    }
    
    public CLClans getClanPlugin() {
        return clanPlugin;
    }
    
    public List<CapturePoint> getPoints() {
        return points;
    }
    
    public Map<String, CapturePointType> getTypes() {
        return types;
    }
    
    public void removeType(CapturePointType type) {
        types.remove(type.getName());
    }
    
    public void addType(CapturePointType type) {
        types.put(type.getName(), type);
    }
}
