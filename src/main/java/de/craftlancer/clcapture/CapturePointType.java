package de.craftlancer.clcapture;

import de.craftlancer.clapi.clcapture.AbstractCapturePointType;
import de.craftlancer.clapi.clcapture.ArtifactModifier;
import de.craftlancer.clapi.clclans.AbstractClan;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class CapturePointType implements AbstractCapturePointType {
    private final String name;
    private String displayName;
    private List<ItemStack> items = new ArrayList<>();
    private List<TimeOfDay> times = new ArrayList<>();
    private int captureTime;
    private int bossbarDistance;
    private boolean broadcastStart;
    private NavigableMap<Integer, Float> playerModifier = new TreeMap<>();
    private ArtifactModifier artifactModifer;
    private String days;
    private boolean excludeTopClans;
    private int excludeTopXClans;
    private boolean pingDiscord = true;
    private CLCapture plugin;
    private List<AbstractClan> topXClans;
    
    public CapturePointType(CLCapture plugin, ConfigurationSection config) {
        this.plugin = plugin;
        name = config.getName();
        
        displayName = config.contains("displayName") ? config.getString("displayName") : name;
        items = (List<ItemStack>) config.getList("drops", new ArrayList<>());
        times = config.getStringList("times").stream().map(TimeOfDay::new).filter(TimeOfDay::isValid).collect(Collectors.toList());
        captureTime = config.getInt("captureTime", 18000); // 15 minutes
        bossbarDistance = config.getInt("bossbarDistance", 200);
        broadcastStart = config.getBoolean("broadcastStart", true);
        artifactModifer = ArtifactModifier.fromString(config.getString("modifier"));
        days = config.contains("days") ? config.getString("days") : "1234567";
        excludeTopClans = config.contains("excludeTopClans") ? config.getBoolean("excludeTopClans") : false;
        excludeTopXClans = config.contains("excludeTopXClans") ? config.getInt("excludeTopXClans") : 3;
        
        playerModifier.put(0, 1.0f); // default value
        config.getStringList("playerMod").forEach(a -> {
            String[] bla = a.split(" ");
            int pNum = Integer.parseInt(bla[0]);
            float mod = Float.parseFloat(bla[1]);
            playerModifier.put(pNum, mod);
        });
    }
    
    public void save(Configuration config) {
        ConfigurationSection section = config.createSection(getName());
        section.set("displayName", displayName);
        section.set("drops", items);
        section.set("captureTime", captureTime);
        section.set("bossbarDistance", bossbarDistance);
        section.set("broadcastStart", broadcastStart);
        section.set("playerMod", playerModifier.entrySet().stream().map(a -> a.getKey() + " " + a.getValue()).collect(Collectors.toList()));
        section.set("times", times.stream().map(TimeOfDay::toString).collect(Collectors.toList()));
        section.set("modifier", artifactModifer.toString());
        section.set("days", days);
        section.set("excludeTopClans", excludeTopClans);
        section.set("excludeTopXClans", excludeTopXClans);
    }
    
    public CapturePointType(String name) {
        this.name = name;
        this.displayName = name;
    }
    
    public CapturePointType(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public List<ItemStack> getItems() {
        return Collections.unmodifiableList(items);
    }
    
    public List<TimeOfDay> getTimes() {
        return Collections.unmodifiableList(times);
    }
    
    @Override
    public ArtifactModifier getArtifactModifier() {
        return artifactModifer;
    }
    
    public void setArtifactModifier(ArtifactModifier artifactModifer) {
        this.artifactModifer = artifactModifer;
    }
    
    public int getCaptureTime() {
        return captureTime;
    }
    
    public void setCaptureTime(int captureTime) {
        this.captureTime = captureTime;
    }
    
    @Override
    public NavigableMap<Integer, Float> getPlayerModifier() {
        return Collections.unmodifiableNavigableMap(playerModifier);
    }
    
    @Override
    public int getBossbarDistance() {
        return bossbarDistance;
    }
    
    @Override
    public boolean isBroadcastStart() {
        return broadcastStart;
    }
    
    public void setBossbarDistance(int bossbarDistance) {
        this.bossbarDistance = bossbarDistance;
    }
    
    public void setBroadcastStart(boolean broadcastStart) {
        this.broadcastStart = broadcastStart;
    }
    
    public void addTime(TimeOfDay time) {
        times.add(time);
    }
    
    public void removeTime(int index) {
        times.remove(index);
    }
    
    public void addPMod(int numPlayers, float mod) {
        playerModifier.put(numPlayers, mod);
    }
    
    public void addItem(ItemStack item) {
        items.add(item);
    }
    
    public void removeItem(int index) {
        items.remove(index);
    }
    
    public void removePMod(int playerCount) {
        playerModifier.remove(playerCount);
    }
    
    public String getDays() {
        return days;
    }
    
    public void setDays(String days) {
        this.days = days;
    }
    
    @Override
    public boolean isPing() {
        return pingDiscord;
    }
    
    public void setPing(boolean pingDiscord) {
        this.pingDiscord = pingDiscord;
    }
    
    @Override
    public boolean isExcludeTopClans() {
        return excludeTopClans;
    }
    
    public void setExcludeTopClans(boolean excludeTopClans) {
        this.excludeTopClans = excludeTopClans;
    }
    
    public int getExcludeTopXClans() {
        return excludeTopXClans;
    }
    
    public void setExcludeTopXClans(int excludeTopXClans) {
        this.excludeTopXClans = excludeTopXClans;
    }
    
    public void setTopXClans(List<AbstractClan> topXClans) {
        this.topXClans = topXClans;
    }
    
    /**
     *
     * @param uuid - the player
     * @return - true if the point is excluding top x clans, and that player is in a top x clan
     */
    @Override
    public boolean isPlayerExcluded(UUID uuid) {
        if (!excludeTopClans)
            return false;
        Optional<PlayerPastClanStorage> optional = plugin.getPastClans().stream().filter(storage -> storage.getPlayerUUID().equals(uuid)).findFirst();
        if (optional.isPresent())
            if (optional.get().getLast24HourClans().stream().anyMatch(clan -> topXClans.contains(clan)))
                return true;
        return topXClans.stream().anyMatch(clan -> clan.isMember(uuid));
    }
    
    public static class TimeOfDay implements Comparable<TimeOfDay> {
        int hour;
        int minute;
        
        public TimeOfDay(String string) {
            String[] arr = string.split(":");
            
            if (arr.length != 2)
                throw new IllegalArgumentException("Given string does not comply to hh:mm format.");
            
            hour = Integer.parseInt(arr[0]);
            minute = Integer.parseInt(arr[1]);
            
            if (hour >= 24 || hour < 0 || minute >= 60 || minute < 0)
                hour = minute = -1;
        }
        
        public TimeOfDay(int hour, int minute) {
            if (hour >= 24 || hour < 0)
                throw new IllegalArgumentException("Given hour must be between 0 and 23");
            if (minute >= 60 || minute < 0)
                throw new IllegalArgumentException("Given minute must be between 0 and 59");
            
            this.hour = hour;
            this.minute = minute;
        }
        
        public boolean isValid() {
            return hour != -1 && minute != -1;
        }
        
        public int toSecondsOfDay() {
            return hour * 3600 + minute * 60;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + hour;
            result = prime * result + minute;
            return result;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof TimeOfDay))
                return false;
            TimeOfDay other = (TimeOfDay) obj;
            if (hour != other.hour)
                return false;
            if (minute != other.minute)
                return false;
            return true;
        }
        
        @Override
        public int compareTo(TimeOfDay o) {
            return Integer.compare(this.toSecondsOfDay(), o.toSecondsOfDay());
        }
        
        @Override
        public String toString() {
            return String.format("%02d:%02d", hour, minute);
        }
    }
}
