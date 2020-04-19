package de.craftlancer.clcapture.util;

import org.bukkit.Material;

public class MaterialUtil {
	public static boolean isBanner(Material material) {
		switch (material) {
			case BLACK_BANNER:
			case BLACK_WALL_BANNER:
			case BLUE_BANNER:
			case BLUE_WALL_BANNER:
			case BROWN_BANNER:
			case BROWN_WALL_BANNER:
			case CYAN_BANNER:
			case CYAN_WALL_BANNER:
			case GRAY_BANNER:
			case GRAY_WALL_BANNER:
			case GREEN_BANNER:
			case GREEN_WALL_BANNER:
			case LIGHT_BLUE_BANNER:
			case LIGHT_BLUE_WALL_BANNER:
			case LIGHT_GRAY_BANNER:
			case LIGHT_GRAY_WALL_BANNER:
			case LIME_BANNER:
			case LIME_WALL_BANNER:
			case MAGENTA_BANNER:
			case MAGENTA_WALL_BANNER:
			case ORANGE_BANNER:
			case ORANGE_WALL_BANNER:
			case PINK_BANNER:
			case PINK_WALL_BANNER:
			case PURPLE_BANNER:
			case PURPLE_WALL_BANNER:
			case RED_BANNER:
			case RED_WALL_BANNER:
			case WHITE_BANNER:
			case WHITE_WALL_BANNER:
			case YELLOW_BANNER:
			case YELLOW_WALL_BANNER:
				return true;
			default:
				return false;
		}
	}
}
