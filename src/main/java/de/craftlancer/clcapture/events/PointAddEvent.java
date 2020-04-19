package de.craftlancer.clcapture.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PointAddEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();

	private Location chestLocation;
	private Player player;
	private String type;
	private String id;
	private String name;
	private boolean cancelled = false;

	public PointAddEvent(Player player, Location chestLocation, String type, String name, String id) {
		this.chestLocation = chestLocation;
		this.player = player;
		this.type = type;
		this.id = id;
		this.name = name;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Location getChestLocation() {
		return chestLocation;
	}

	public Player getPlayer() {
		return player;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean b) {
		this.cancelled = b;
	}
}
