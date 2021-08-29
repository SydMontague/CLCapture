package de.craftlancer.clcapture;

import de.craftlancer.clapi.clcapture.PluginCLCapture;
import de.craftlancer.clapi.clclans.AbstractClan;
import de.craftlancer.clapi.clclans.PluginClans;
import de.craftlancer.clapi.clclans.events.ClanLeaveEvent;
import de.craftlancer.clcapture.commands.CaptureCommandHandler;
import de.craftlancer.core.IntRingBuffer;
import de.craftlancer.core.LambdaRunnable;
import org.bukkit.Bukkit;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.FileUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

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

public class CLCapture extends JavaPlugin implements Listener, PluginCLCapture {
    public static final String PREFIX = "§f[§4Craft§fCitizen]§e ";
    
    public static final String ADMIN_PERMISSION = "clcapture.admin";
    
    private static final int PLAYER_BUFFER_SIZE = 60; // number of updates kept
    private static final int PLAYER_BUFFER_FREQUENCY = 60 * 20; // update frequency
    
    private final File pointsFile = new File(getDataFolder(), "points.yml");
    private final File typesFile = new File(getDataFolder(), "types.yml");
    private final File pastClansFile = new File(getDataFolder(), "pastClans.yml");
    
    private PluginClans clanPlugin;
    
    private IntRingBuffer playerCountBuffer = new IntRingBuffer(PLAYER_BUFFER_SIZE);
    private Map<String, CapturePointType> types;
    private List<CapturePoint> points;
    private List<PlayerPastClanStorage> pastClans;
    
    private boolean useDiscord;
    
    @Override
    public void onEnable() {
        useDiscord = Bukkit.getPluginManager().getPlugin("DiscordSRV") != null;
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getServicesManager().register(PluginCLCapture.class,this,this, ServicePriority.Highest);
        
        if (clanPlugin == null)
            getLogger().severe("Couldn't find CLClans!");
        
        saveDefaultConfig();
        loadConfig();
        registerPastClans();
        
        getCommand("capture").setExecutor(new CaptureCommandHandler(this));
        
        new LambdaRunnable(() -> saveTypes(true)).runTaskTimer(this, 18000L, 18000L);
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
        
        savePoints(false);
        saveTypes(false);
        savePastClans(false);
    }
    
    @EventHandler (ignoreCancelled = true)
    public void onClanLeave(ClanLeaveEvent event) {
        AbstractClan clan = event.getClan();
        UUID playerUUID = event.getPlayer().getUniqueId();
        
        Optional<PlayerPastClanStorage> optional = pastClans.stream().filter(pastClan -> pastClan.getPlayerUUID().equals(playerUUID)).findFirst();
        
        if (optional.isPresent())
            optional.get().add(clan.getUniqueId(), System.currentTimeMillis());
        else
            pastClans.add(new PlayerPastClanStorage(this, playerUUID, clan.getUniqueId(), System.currentTimeMillis()));
    }
    
    private void registerPastClans() {
        if (!pastClansFile.exists())
            try {
                InputStream stream = getResource("pastClans.yml");
                FileUtils.copyInputStreamToFile(stream, pastClansFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        YamlConfiguration pastClansData = YamlConfiguration.loadConfiguration(pastClansFile);
            
            pastClans = (List<PlayerPastClanStorage>) pastClansData.get("pastClans");
    }
    
    private void loadConfig() {
        FileConfiguration pointsData = YamlConfiguration.loadConfiguration(pointsFile);
        FileConfiguration typesData = YamlConfiguration.loadConfiguration(typesFile);
        
        types = typesData.getKeys(false).stream().map(key -> new CapturePointType(this, typesData.getConfigurationSection(key)))
                .collect(Collectors.toMap(CapturePointType::getName, a -> a));
        
        points = pointsData.getKeys(false).stream().map(key -> new CapturePoint(this, key, pointsData.getConfigurationSection(key)))
                .collect(Collectors.toList());
    }
    
    private void savePoints(boolean async) {
        YamlConfiguration pointsData = new YamlConfiguration();
        points.forEach(a -> a.save(pointsData));
        
        BukkitRunnable run = new LambdaRunnable(() -> {
            try {
                pointsData.save(pointsFile);
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error while saving points.yml", e);
            }
        });
        
        if (async)
            run.runTaskAsynchronously(this);
        else run.run();
    }
    
    private void saveTypes(boolean async) {
        YamlConfiguration typesData = new YamlConfiguration();
        types.forEach((b, a) -> a.save(typesData));
        
        BukkitRunnable run = new LambdaRunnable(() -> {
            try {
                typesData.save(typesFile);
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error while saving types.yml", e);
            }
        });
        
        if (async)
            run.runTaskAsynchronously(this);
        else
            run.run();
    }
    
    private void savePastClans(boolean async) {
        if (!pastClansFile.exists())
            try {
                InputStream stream = getResource("pastClans.yml");
                FileUtils.copyInputStreamToFile(stream, pastClansFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        YamlConfiguration pastClansData = YamlConfiguration.loadConfiguration(pastClansFile);
        
        BukkitRunnable run = new LambdaRunnable(() -> {
            pastClansData.set("pastClans", pastClans.stream().filter(storage -> storage.getLast24HourClans().size() > 0).collect(Collectors.toList()));
            try {
                pastClansData.save(pastClansFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    
        if (async)
            run.runTaskAsynchronously(this);
        else
            run.run();
    }
    
    @Override
    public void saveDefaultConfig() {
        super.saveDefaultConfig();
        
        if (!pointsFile.exists())
            try {
                Files.createFile(pointsFile.toPath());
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.WARNING, "Error while creating points.yml", e);
            }
    }
    
    public boolean addPoint(CapturePoint point) {
        boolean success = points.add(point);
        savePoints(true);
        return success;
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
        savePoints(true);
    }
    
    protected PluginClans getClanPlugin() {
        if (clanPlugin == null)
            clanPlugin = Bukkit.getServicesManager().load(PluginClans.class);
        
        return clanPlugin;
    }
    
    @Override
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
    
    public boolean isUsingDiscord() {
        return useDiscord;
    }
    
    public List<PlayerPastClanStorage> getPastClans() {
        return pastClans;
    }
}
