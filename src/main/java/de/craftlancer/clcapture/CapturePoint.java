package de.craftlancer.clcapture;

import de.craftlancer.clcapture.CapturePointType.TimeOfDay;
import de.craftlancer.clcapture.util.ClanColorUtil;
import de.craftlancer.clclans.Clan;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CapturePoint implements Listener {
    private static final long EXCLUSIVE_TIMEOUT = 6000;
    
    private static final String MSG_PREFIX = ChatColor.GRAY + "§f[§4§lPvP §c§lEvent§f] ";
    
    private static final String CAPTURE_MESSAGE = MSG_PREFIX + "%s §etook the capture point %s!";
    private static final String CAPTURE_MESSAGE_DISCORD = ":bannerred:%s took the capture point %s!";
    private static final String EVENT_START_MSG = MSG_PREFIX + "§eThe battle for §6%s §ehas begun! Grab your sword and armor and go to portal: '§6PVP§e' to capture a point!";
    private static final String EVENT_START_MSG_DISCORD = ":bannerwhite:The battle for %s has begun! <@&661388575039946752>";
    private static final String EVENT_END_MSG = MSG_PREFIX + "%s §ewon the battle for %s!";
    private static final String EVENT_END_MSG_DISCORD = ":bannergreen:%s won the battle for %s!";
    private static final String CANT_OPEN_MSG = MSG_PREFIX + "§eYou can't open this chest!";
    
    private CLCapture plugin;
    
    // configuration parameter
    private final String id;
    private final String name;
    private final CapturePointType type;
    private final Location chestLocation;
    //private Location minRegionLocation;
    //private Location maxRegionLocation;
    private BoundingBox region;
    
    // runtime parameter
    private int tickId = 0;
    
    private CapturePointState state = CapturePointState.INACTIVE;
    private float lootModifier = 1.0f;
    private int lastTime = LocalTime.now().toSecondOfDay();
    private long winTime = -1;
    
    private UUID currentOwner = null;
    private UUID previousOwner = null;
    private UUID previousMessageOwner = null;
    private Map<UUID, Integer> timeMap = new HashMap<>();
    private KeyedBossBar bar;
    
    private int lastRadius = 0;
    //A list of all clans/players that are in the region
    
    public CapturePoint(CLCapture plugin, String name, String id, CapturePointType type, Block chestLocation) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.name = name;
        this.id = id;
        this.type = type;
        this.chestLocation = chestLocation.getLocation();
        updateRegion();
    }
    
    public CapturePoint(CLCapture plugin, String id, ConfigurationSection config) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.id = id;
        this.name = config.getString("name", id);
        this.type = plugin.getPointType(config.getString("type"));
        this.chestLocation = config.getObject("chest", Location.class);
        updateRegion();
    }
    
    private void updateRegion() {
        region = new BoundingBox(
                chestLocation.getX()-2,
                chestLocation.getY()-2,
                chestLocation.getZ()-2,
                chestLocation.getX()+3,
                chestLocation.getY()+5,
                chestLocation.getZ()+3);
        //minRegionLocation = new Location(chestLocation.getWorld(), chestLocation.getX() - 2, chestLocation.getY() - 2, chestLocation.getZ() - 2);
        //maxRegionLocation = new Location(chestLocation.getWorld(), chestLocation.getX() + 3, chestLocation.getY() + 5, chestLocation.getZ() + 3);
    }
    
    public Inventory getInventory() {
        return ((Chest) chestLocation.getBlock().getState()).getBlockInventory();
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onChestInteract(PlayerInteractEvent event) {
        if (state == CapturePointState.INACTIVE && isCurrentOwner(event.getPlayer()))
            return;
        
        if (winTime >= EXCLUSIVE_TIMEOUT)
            return;
        
        if (event.getPlayer().hasPermission(CLCapture.ADMIN_PERMISSION))
            return;
        
        if (getChestLoction().equals(event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CANT_OPEN_MSG);
        }
    }
    
    private boolean isUnstackableItem(ItemStack item) {
        return item != null && item.getMaxStackSize() == 1;
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onItemPick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        Inventory inventory = event.getClickedInventory();
        
        if (inventory == null || inventory.getType() != InventoryType.CHEST)
            return;
        
        if (currentItem == null || !isUnstackableItem(currentItem))
            return;
        
        InventoryHolder holder = inventory.getHolder();
        
        if (!(holder instanceof Container && ((Container) holder).getLocation().equals(getChestLoction())))
            return;
        
        event.setCancelled(true);
        ItemStack clone = currentItem.clone();
        clone.setAmount(1);
        
        if (event.getWhoClicked().getInventory().addItem(clone).isEmpty())
            currentItem.setAmount(currentItem.getAmount() - 1);
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getLocation().equals(chestLocation)) {
            if (event.getPlayer().hasPermission(CLCapture.ADMIN_PERMISSION)) {
                plugin.removePoint(this);
                chestLocation.getBlock().breakNaturally();
                event.getPlayer().sendMessage("CapturePoint removed.");
            } else
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
        winTime++;
        
        if (currentOwner != null && winTime >= EXCLUSIVE_TIMEOUT)
            currentOwner = null;
        
        int now = LocalTime.now().toSecondOfDay();
        if (now == lastTime)
            return;
        
        if (type.getTimes().stream().map(TimeOfDay::toSecondsOfDay).anyMatch(time -> (time > lastTime || now < lastTime) && time <= now))
            startEvent();
        
        lastTime = now;
    }
    
    private void runActive() {
        HashMap<UUID, Integer> inRegionMap = new HashMap<>();
        lastTime = LocalTime.now().toSecondOfDay();
        
        //If a player is within the capturepoint region, add them to the list.
        Bukkit.getOnlinePlayers().forEach(a -> {
            if (isInRegion(a))
                inRegionMap.compute(convertToOwner(a), (b, c) -> inRegionMap.containsKey(b) ? c + 1 : 1);
        });
        
        //See if there are multiple clans by comparing clans
        
        //If there are multiple clans present in the region, don't add to bossbar/map
        int scoreMultiplier;
        int tempInt = setOwner(inRegionMap);
        if (tempInt == -1)
            scoreMultiplier = 1;
        else
            scoreMultiplier = tempInt;
        timeMap.replaceAll((a, b) -> a.equals(currentOwner) ? b + scoreMultiplier : Math.max(b - scoreMultiplier, 0));
        bar.setProgress(Math.min(timeMap.getOrDefault(currentOwner, 0) / (double) type.getCaptureTime(), 1D));
        bar.setTitle(name + " - " + getOwnerName(tempInt) + " - (" + scoreMultiplier + ")");
        createParticleEffects();
        bar.setColor(ClanColorUtil.getBarColor(plugin.getClanPlugin().getClanByUUID(currentOwner)));
    
        //Check if players are within distance to add to the boss bar
        if (tickId % 20 == 0) {
            bar.getPlayers().stream().filter(a -> a.getWorld().equals(chestLocation.getWorld()))
                    .filter(a -> a.getLocation().distance(chestLocation) >= type.getBossbarDistance()).forEach(bar::removePlayer);
            Bukkit.getOnlinePlayers().stream().filter(a -> a.getWorld().equals(chestLocation.getWorld()))
                    .filter(a -> a.getLocation().distance(chestLocation) < type.getBossbarDistance()).forEach(bar::addPlayer);
        }
        
        if (currentOwner == null)
            setClanColors(null);
        
        //If there is a winner, handle the win and return
        if (timeMap.getOrDefault(currentOwner, 0) >= type.getCaptureTime()) {
            handleWin();
            return;
        }
        
        //Set variables back to default values
        previousOwner = currentOwner;
        currentOwner = null;
    }
    
    private void createParticleEffects() {
        if (tickId % 8 != 0 || currentOwner == null)
            return;
        int a;
        if (lastRadius == 3)
            a = 1;
        else
            a = lastRadius + 1;
        
        World world = chestLocation.getWorld();
        Particle.DustOptions particle;
        if (plugin.getClanPlugin().getClanByUUID(currentOwner) == null)
            particle = new Particle.DustOptions(Color.WHITE,1F);
        else
            particle = new Particle.DustOptions(ClanColorUtil.getBukkitColor(plugin.getClanPlugin().getClanByUUID(currentOwner)), 1);
        
        double increment = (2 * Math.PI) / 150;
        for (int i = 0; i < 150; i++) {
            double angle = i * increment;
            double x = (chestLocation.getX() + 0.5) + (a * Math.cos(angle));
            double z = (chestLocation.getZ() + 0.5) + (a * Math.sin(angle));
            world.spawnParticle(Particle.REDSTONE, new Location(world, x, chestLocation.getY(), z), 1, particle);
        }
        lastRadius = a;
    }
    
    //If there are people in the region, set the appropriate variables
    private int setOwner(Map<UUID, Integer> inRegionMap) {
        if (inRegionMap.size() == 0) {
            currentOwner = null;
            return 1;
        } else {
            Map.Entry<UUID, Integer> maxEntry = inRegionMap.entrySet().stream().max(Comparator.comparing(Map.Entry::getValue)).get();
            for (Map.Entry<UUID, Integer> entry : inRegionMap.entrySet())
                if (entry.getKey() != maxEntry.getKey() && maxEntry.getValue().equals(entry.getValue())) {
                    inRegionMap.clear();
                    break;
                }
            if (inRegionMap.size() == 0)
                currentOwner = null;
            else
                currentOwner = maxEntry.getKey();
            if (previousMessageOwner != currentOwner && currentOwner != null) {
                previousMessageOwner = currentOwner;
                announce();
            }
            if (currentOwner != previousOwner)
                setClanColors(plugin.getClanPlugin().getClanByUUID(currentOwner));
            if (currentOwner != null)
                timeMap.putIfAbsent(currentOwner, 0);
            if (currentOwner != null && inRegionMap.size() == 1)
                return maxEntry.getValue();
            else if (currentOwner != null && inRegionMap.size() > 1) {
                int secondGreatestValue = 0;
                for (Map.Entry<UUID, Integer> entry : inRegionMap.entrySet())
                    if (entry.getKey() != maxEntry.getKey() && entry.getValue() > secondGreatestValue)
                        secondGreatestValue = entry.getValue();
                    return maxEntry.getValue()-secondGreatestValue;
            }
            previousOwner = currentOwner;
        }
        return -1;
    }
    
    private void handleWin() {
        bar.removeAll();
        Bukkit.removeBossBar(new NamespacedKey(plugin, id));
        
        if (plugin.isUsingDiscord() && type.isBroadcastStart())
            DiscordUtil.queueMessage(DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("event"),
                    String.format(EVENT_END_MSG_DISCORD, getOwnerName(1), this.name));
        
        Bukkit.broadcastMessage(String.format(EVENT_END_MSG, getOwnerName(1), this.name));
        state = CapturePointState.INACTIVE;
        
        type.getItems().forEach(a -> {
            ItemStack stack = a.clone();
            stack.setAmount((int) (a.getAmount() * lootModifier));
            if (!getInventory().addItem(stack).isEmpty())
                plugin.getLogger().warning("Couldn't add item to chest, is it full?");
        });
        
        winTime = 0;
    }
    
    public void startEvent() {
        previousMessageOwner = null;
        previousOwner = null;
        currentOwner = null;
        setClanColors(null);
        timeMap.clear();
        lootModifier = type.getPlayerModifier().floorEntry(plugin.getMaxPlayerCountLastHour()).getValue();
        
        // clear inventory at the start of the event
        getInventory().clear();
        
        bar = Bukkit.createBossBar(new NamespacedKey(plugin, id), "", BarColor.WHITE, BarStyle.SOLID);
        
        state = CapturePointState.ACTIVE;
        
        if (type.isBroadcastStart()) {
            Bukkit.broadcastMessage(String.format(EVENT_START_MSG, this.name));
            if (plugin.isUsingDiscord())
                DiscordUtil.queueMessage(DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("event"),
                        String.format(EVENT_START_MSG_DISCORD, this.name));
        }
        
        winTime = 0;
    }
    
    private void announce() {
        if (getType().isBroadcastStart()) {
            Bukkit.broadcastMessage(String.format(CAPTURE_MESSAGE, getOwnerName(1), this.name));
            if (plugin.isUsingDiscord())
                DiscordUtil.queueMessage(DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("event"),
                        String.format(CAPTURE_MESSAGE_DISCORD, getOwnerName(1), this.name));
        }
    }
    
    //Size is only for the boss bar calling this method, use int 1 normally.
    private String getOwnerName(int size) {
        if (currentOwner == null)
            if (size == -1)
                return "Contended";
            else
                return "Uncaptured";
        
        Clan c = plugin.getClanPlugin().getClanByUUID(currentOwner);
        if (c != null)
            return c.getName();
        
        OfflinePlayer p = Bukkit.getOfflinePlayer(currentOwner);
        if (p.isOnline())
            return p.getName();
        
        return "Uncaptured";
    }
    
    private boolean isCurrentOwner(HumanEntity player) {
        if (currentOwner == null || player == null)
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
    
    public CapturePointState getState() {
        return state;
    }
    
    private boolean isInRegion(Player player) {
        Location location = player.getLocation();
        return region.contains(location.getX(), location.getY(),location.getZ());
    }
    
    private void setClanColors(Clan clan) {
        double minX = region.getMinX();
        double minY = region.getMinY();
        double minZ = region.getMinZ();
        
        double maxX = region.getMaxX();
        double maxY = region.getMaxY();
        double maxZ = region.getMaxZ();
        
        for (double x = minX; x <= maxX; x++)
            for (double y = minY; y <= maxY; y++)
                for (double z = minZ; z <= maxZ; z++) {
                    Location location = new Location(chestLocation.getWorld(), x, y, z);
                    if (ClanColorUtil.isConcrete(location.getBlock().getType()))
                        location.getBlock().setType(ClanColorUtil.getConcreteColor(clan));
                    if (ClanColorUtil.isGlass(location.getBlock().getType()))
                        location.getBlock().setType(ClanColorUtil.getGlassColor(clan));
                    if (Tag.BANNERS.isTagged(location.getBlock().getType()))
                        ClanColorUtil.setClanBanner(clan, location);
                }
    }
    
    public enum CapturePointState {
        INACTIVE,
        ACTIVE
    }
}
