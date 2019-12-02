package de.craftlancer.clcapture;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import de.craftlancer.clcapture.CapturePointType.TimeOfDay;
import de.craftlancer.clclans.Clan;

public class CapturePoint implements Listener {
    private static final int DISTANCE = 200;
    private static final long EXCLUSIVE_TIMEOUT = 6000;
    
    private static final String MSG_PREFIX = ChatColor.GRAY + "[CapPoints] ";
    
    private static final String CAPTURE_MESSAGE = MSG_PREFIX + "%s took the capture point %s";
    private static final String EVENT_START_MSG = MSG_PREFIX + "The battle for %s begun!";
    private static final String EVENT_END_MSG = MSG_PREFIX + "%s won the battle for %s!";
    private static final String CANT_OPEN_MSG = MSG_PREFIX + "You can't open this chest!";
    
    private CLCapture plugin;
    
    // configuration parameter
    private final String id;
    private final String name;
    private final CapturePointType type;
    
    private final Location chestLocation;
    private final Location signLocation;
    
    // runtime parameter
    private int tickId = 0;
    
    private CapturePointState state = CapturePointState.INACTIVE;
    private float lootModifier = 1.0f;
    private int lastTime = LocalTime.now().toSecondOfDay();
    private long winTime = -1;
    
    private UUID currentOwner = null;
    private Map<UUID, Integer> timeMap = new HashMap<>();
    private KeyedBossBar bar;
    
    public CapturePoint(CLCapture plugin, String name, String id, CapturePointType type, Block chestLocation, Block signLocation) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.name = name;
        this.id = id;
        
        this.type = type;
        
        this.chestLocation = chestLocation.getLocation();
        this.signLocation = signLocation.getLocation();
        
        updateSign();
    }
    
    public CapturePoint(CLCapture plugin, String id, ConfigurationSection config) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.id = id;
        this.name = config.getString("name", id);
        
        this.type = plugin.getPointType(config.getString("type"));
        this.chestLocation = config.getObject("chest", Location.class);
        this.signLocation = config.getObject("sign", Location.class);
        
        updateSign();
    }
    
    public Inventory getInventory() {
        return ((Chest) chestLocation.getBlock().getState()).getBlockInventory();
    }
    
    public Sign getSign() {
        return (Sign) signLocation.getBlock().getState();
    }
    
    @EventHandler
    public void onChestInteract(PlayerInteractEvent event) {
        InventoryHolder holder = getInventory().getHolder();
        
        if (state == CapturePointState.INACTIVE && isCurrentOwner(event.getPlayer()))
            return;
        
        if (winTime >= EXCLUSIVE_TIMEOUT)
            return;
        
        if (event.getPlayer().hasPermission(CLCapture.ADMIN_PERMISSION))
            return;
        
        boolean cancel = false;
        
        if (holder instanceof Chest && ((Chest) holder).getBlock().equals(event.getClickedBlock()))
            cancel = true;
        
        if (cancel) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CANT_OPEN_MSG);
        }
    }
    
    private boolean isUnstackableItem(ItemStack item) {
        return item.getMaxStackSize() == 1;
    }
    
    @EventHandler
    public void onItemPick(InventoryClickEvent event) {
        if (!getInventory().equals(event.getClickedInventory()))
            return;
        
        ItemStack currentItem = event.getCurrentItem();
        
        if (!isUnstackableItem(currentItem))
            return;
        
        event.setCancelled(true);
        ItemStack clone = currentItem.clone();
        clone.setAmount(1);
        
        if (event.getWhoClicked().getInventory().addItem(clone).isEmpty())
            currentItem.setAmount(currentItem.getAmount() - 1);
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (state == CapturePointState.INACTIVE)
            return;
        
        if (!event.hasBlock() || !event.getClickedBlock().equals(signLocation.getBlock()))
            return;
        
        if (isCurrentOwner(event.getPlayer()))
            return;
        
        currentOwner = convertToOwner(event.getPlayer());
        timeMap.putIfAbsent(currentOwner, 0);
        
        Bukkit.broadcastMessage(String.format(CAPTURE_MESSAGE, getOwnerName(), this.name));
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        InventoryHolder holder = getInventory().getHolder();
        
        if (event.getBlock().getLocation().equals(signLocation) || (holder instanceof Chest && ((Chest) holder).getBlock().equals(event.getBlock()))) {
            if (event.getPlayer().hasPermission(CLCapture.ADMIN_PERMISSION)) {
                plugin.removePoint(this);
                signLocation.getBlock().breakNaturally();
                ((Chest) holder).getBlock().breakNaturally();
                event.getPlayer().sendMessage("CapturePoint removed.");
            }
            else
                event.setCancelled(true);
        }
    }
    
    // called once per tick
    public void run() {
        switch (state) {
            case INACTIVE:
                runInactive();
                break;
            case ACTIVE:
                runActive();
                break;
            default:
                break;
        }
        
        tickId++;
    }
    
    private void runInactive() {
        int now = LocalTime.now().toSecondOfDay();
        if (now == lastTime)
            return;
        
        if (type.getTimes().stream().map(TimeOfDay::toSecondsOfDay).anyMatch(time -> (time > lastTime || now < lastTime) && time <= now))
            startEvent();
        
        lastTime = now;
        winTime++;
    }
    
    private void runActive() {
        lastTime = LocalTime.now().toSecondOfDay();
        
        timeMap.replaceAll((a, b) -> a.equals(currentOwner) ? b + 1 : Math.max(b - 1, 0));
        
        bar.setProgress(timeMap.getOrDefault(currentOwner, 0) / (double) type.getCaptureTime());
        bar.setTitle(name + " - " + getOwnerName());
        
        if (tickId % 20 == 0) {
            bar.getPlayers().stream().filter(a -> a.getWorld().equals(signLocation.getWorld())).filter(a -> a.getLocation().distance(signLocation) >= DISTANCE)
               .forEach(bar::removePlayer);
            Bukkit.getOnlinePlayers().stream().filter(a -> a.getWorld().equals(signLocation.getWorld()))
                  .filter(a -> a.getLocation().distance(signLocation) < DISTANCE).forEach(bar::addPlayer);
        }
        
        if (timeMap.getOrDefault(currentOwner, 0) >= type.getCaptureTime())
            handleWin();
    }
    
    private void handleWin() {
        bar.removeAll();
        Bukkit.removeBossBar(new NamespacedKey(plugin, id));
        
        Bukkit.broadcastMessage(String.format(EVENT_END_MSG, getOwnerName(), this.name));
        state = CapturePointState.INACTIVE;
        
        type.getItems().forEach(a -> {
            ItemStack stack = a.clone();
            stack.setAmount((int) (a.getAmount() * lootModifier));
            if (!getInventory().addItem(stack).isEmpty())
                plugin.getLogger().warning("Couldn't add item to chest, is it full?");
        });
        
        updateSign();
        winTime = 0;
    }
    
    public void startEvent() {
        currentOwner = null;
        timeMap.clear();
        lootModifier = type.getPlayerModifier().floorEntry(plugin.getMaxPlayerCountLastHour()).getValue();
        
        // clear inventory at the start of the event
        getInventory().clear();
        
        bar = Bukkit.createBossBar(new NamespacedKey(plugin, id), "", BarColor.WHITE, BarStyle.SOLID);
        Bukkit.getOnlinePlayers().forEach(bar::addPlayer);
        
        state = CapturePointState.ACTIVE;
        
        Bukkit.broadcastMessage(String.format(EVENT_START_MSG, this.name));
        
        updateSign();
        winTime = 0;
    }
    
    private void updateSign() {
        Sign sign = getSign();
        sign.setLine(0, "[CapPoints]");
        sign.setLine(1, getName());
        sign.setLine(2, getOwnerName());
        if (state == CapturePointState.ACTIVE)
            sign.setLine(3, "Event Running");
        else {
            TimeOfDay time = getNextTime();
            sign.setLine(3, String.format("Next: %02d:%02d", time.hour, time.minute));
        }
        sign.update();
    }
    
    private String getOwnerName() {
        Clan c = plugin.getClanPlugin().getClanByUUID(currentOwner);
        if (c != null)
            return c.getName();
        
        Player p = Bukkit.getPlayer(currentOwner);
        if (p != null)
            return p.getName();
        
        return "Uncaptured";
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (state == CapturePointState.ACTIVE)
            bar.addPlayer(event.getPlayer());
    }
    
    private boolean isCurrentOwner(HumanEntity player) {
        if (player == null)
            return false;
        
        Clan c = plugin.getClanPlugin().getClan(Bukkit.getOfflinePlayer(player.getUniqueId()));
        
        if (c != null && c.getUniqueId().equals(currentOwner))
            return true;
        
        return player.getUniqueId().equals(currentOwner);
    }
    
    private UUID convertToOwner(HumanEntity player) {
        Clan c = plugin.getClanPlugin().getClan(Bukkit.getOfflinePlayer(player.getUniqueId()));
        
        if (c != null)
            return c.getUniqueId();
        
        return player.getUniqueId();
    }
    
    public String getName() {
        return name;
    }
    
    public String getId() {
        return id;
    }
    
    public TimeOfDay getNextTime() {
        return type.getTimes().stream().filter(a -> a.toSecondsOfDay() - LocalTime.now().toSecondOfDay() >= 0).min(TimeOfDay::compareTo)
                   .orElseGet(() -> type.getTimes().stream().min(TimeOfDay::compareTo).get());
    }
    
    protected void save(FileConfiguration pointsData) {
        ConfigurationSection section = pointsData.createSection(getId());
        section.set("name", name);
        section.set("type", type.getName());
        section.set("sign", signLocation);
        section.set("chest", chestLocation);
    }
    
    public void destroy() {
        if (bar != null) {
            bar.removeAll();
            Bukkit.removeBossBar(bar.getKey());
        }
    }
    
    public CapturePointType getType() {
        return type;
    }
    
    public Location getChestLoction() {
        return chestLocation;
    }
    
    public Location getSignLocation() {
        return signLocation;
    }
    
    public CapturePointState getState() {
        return state;
    }
    
    public enum CapturePointState {
        INACTIVE,
        ACTIVE;
    }
}
