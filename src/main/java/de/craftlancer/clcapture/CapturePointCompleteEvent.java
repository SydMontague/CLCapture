package de.craftlancer.clcapture;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class CapturePointCompleteEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    
    private UUID currentOwner;
    private List<Player> players;
    private float lootModifier;
    
    public CapturePointCompleteEvent(UUID currentOwner, List<Player> capturers, float lootModifier) {
        this.currentOwner = currentOwner;
        this.players = capturers;
        this.lootModifier = lootModifier;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    public UUID getOwner() {
        return currentOwner;
    }
    
    public List<Player> getPlayers() {
        return players;
    }
    
    public float getLootModifier() {
        return lootModifier;
    }
}
