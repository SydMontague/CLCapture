package de.craftlancer.clcapture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class CapturePointType {
    private final String name;
    private List<ItemStack> items = new ArrayList<>();
    private List<TimeOfDay> times = new ArrayList<>();
    private int captureTime;
    private int bossbarDistance;
    private boolean broadcastStart;
    private NavigableMap<Integer, Float> playerModifier = new TreeMap<>();
    
    @SuppressWarnings("unchecked")
    public CapturePointType(ConfigurationSection config) {
        name = config.getName();
        
        items = (List<ItemStack>) config.getList("drops", new ArrayList<>());
        times = config.getStringList("times").stream().map(TimeOfDay::new).filter(TimeOfDay::isValid).collect(Collectors.toList());
        captureTime = config.getInt("captureTime", 18000); // 15 minutes
        bossbarDistance = config.getInt("bossbarDistance", 200);
        broadcastStart = config.getBoolean("broadcastStart", true);
        
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
        section.set("drops", items);
        section.set("captureTime", captureTime);
        section.set("bossbarDistance", bossbarDistance);
        section.set("broadcastStart", broadcastStart);
        section.set("playerMod", playerModifier.entrySet().stream().map(a -> a.getKey() + " " + a.getValue()).collect(Collectors.toList()));
        section.set("times", times.stream().map(TimeOfDay::toString).collect(Collectors.toList()));
    }
    
    public CapturePointType(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public List<ItemStack> getItems() {
        return Collections.unmodifiableList(items);
    }
    
    public List<TimeOfDay> getTimes() {
        return Collections.unmodifiableList(times);
    }
    
    public int getCaptureTime() {
        return captureTime;
    }
    
    public void setCaptureTime(int captureTime) {
        this.captureTime = captureTime;
    }
    
    public NavigableMap<Integer, Float> getPlayerModifier() {
        return Collections.unmodifiableNavigableMap(playerModifier);
    }
    
    public int getBossbarDistance() {
        return bossbarDistance;
    }
    
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
