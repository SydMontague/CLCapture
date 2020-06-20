package de.craftlancer.clcapture;

import de.craftlancer.clclans.Clan;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerPastClanStorage implements ConfigurationSerializable {
    
    private CLCapture plugin;
    private UUID playerUUID;
    private Map<UUID, Long> clanLeaveMap;
    
    public PlayerPastClanStorage(CLCapture plugin, UUID playerUUID, UUID clan, long currentTime) {
        this.plugin = plugin;
        this.playerUUID = playerUUID;
        
        clanLeaveMap = new HashMap<>();
        clanLeaveMap.put(clan,currentTime+1728000);
    }
    
    public PlayerPastClanStorage(Map<String, Object> map) {
        this.plugin = (CLCapture) Bukkit.getPluginManager().getPlugin("CLCapture");
        clanLeaveMap = new HashMap<>();
        
        playerUUID = UUID.fromString((String) map.get("playerUUID"));
        map.remove("==");
        map.remove("playerUUID");
        
        map.forEach((key, value) -> {
                UUID uuid = UUID.fromString(key);
                long time = (long) value;

                if (time < System.currentTimeMillis())
                    return;
                
                clanLeaveMap.put(uuid, time);
        });
    }
    
    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("playerUUID", playerUUID.toString());
        clanLeaveMap.entrySet().stream().filter(entry -> plugin.getClanPlugin().getClanByUUID(entry.getKey()) != null)
                .forEach(entry -> map.put(entry.getKey().toString(),entry.getValue()));

        return map;
    }
    
    /**
     * @return All clans that the player has been in in the last 24 hours
     */
    public List<Clan> getLast24HourClans() {
        return clanLeaveMap.entrySet().stream()
                .filter(entry -> System.currentTimeMillis() < entry.getValue() && plugin.getClanPlugin().getClanByUUID(entry.getKey()) != null)
                .map(entry -> plugin.getClanPlugin().getClanByUUID(entry.getKey())).collect(Collectors.toList());
    }
    
    public void add(UUID clanUUID, long currentTime) {
        clanLeaveMap.put(clanUUID, currentTime+1728000);
    }
    
    public void remove(UUID clanUUID) {
        clanLeaveMap.remove(clanUUID);
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
}
