package de.craftlancer.clcapture.util;

import de.craftlancer.clclans.Clan;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.boss.BarColor;
import org.bukkit.inventory.meta.BannerMeta;

public class ClanColorUtil {
    
    public static boolean isConcrete(Material material) {
        switch (material) {
            case BLACK_CONCRETE:
            case CYAN_CONCRETE:
            case BLUE_CONCRETE:
            case RED_CONCRETE:
            case PINK_CONCRETE:
            case BROWN_CONCRETE:
            case GRAY_CONCRETE:
            case GREEN_CONCRETE:
            case LIGHT_BLUE_CONCRETE:
            case LIGHT_GRAY_CONCRETE:
            case LIME_CONCRETE:
            case MAGENTA_CONCRETE:
            case ORANGE_CONCRETE:
            case PURPLE_CONCRETE:
            case WHITE_CONCRETE:
            case YELLOW_CONCRETE:
                return true;
            default:
                return false;
        }
    }
    
    public static boolean isGlass(Material material) {
        switch (material) {
            case BLACK_STAINED_GLASS:
            case CYAN_STAINED_GLASS:
            case BLUE_STAINED_GLASS:
            case RED_STAINED_GLASS:
            case PINK_STAINED_GLASS:
            case BROWN_STAINED_GLASS:
            case GRAY_STAINED_GLASS:
            case GREEN_STAINED_GLASS:
            case LIGHT_BLUE_STAINED_GLASS:
            case LIGHT_GRAY_STAINED_GLASS:
            case LIME_STAINED_GLASS:
            case MAGENTA_STAINED_GLASS:
            case ORANGE_STAINED_GLASS:
            case PURPLE_STAINED_GLASS:
            case WHITE_STAINED_GLASS:
            case YELLOW_STAINED_GLASS:
                return true;
            default:
                return false;
        }
    }
    
    public static Material getConcreteColor(Clan clan) {
        if (clan == null)
            return Material.WHITE_CONCRETE;
        ChatColor color = clan.getColor();
        switch (color) {
            case AQUA:
                return Material.LIGHT_BLUE_CONCRETE;
            case RED:
            case DARK_RED:
                return Material.RED_CONCRETE;
            case BLUE:
            case DARK_BLUE:
                return Material.BLUE_CONCRETE;
            case GOLD:
                return Material.ORANGE_CONCRETE;
            case GRAY:
                return Material.LIGHT_GRAY_CONCRETE;
            case BLACK:
                return Material.BLACK_CONCRETE;
            case GREEN:
                return Material.LIME_CONCRETE;
            case YELLOW:
                return Material.YELLOW_CONCRETE;
            case DARK_AQUA:
                return Material.CYAN_CONCRETE;
            case DARK_GRAY:
                return Material.GRAY_CONCRETE;
            case DARK_GREEN:
                return Material.GREEN_CONCRETE;
            case DARK_PURPLE:
                return Material.PURPLE_CONCRETE;
            case LIGHT_PURPLE:
                return Material.MAGENTA_CONCRETE;
            default:
                return Material.WHITE_CONCRETE;
        }
    }
    
    public static Material getGlassColor(Clan clan) {
        if (clan == null)
            return Material.WHITE_STAINED_GLASS;
        ChatColor color = clan.getColor();
        switch (color) {
            case AQUA:
                return Material.LIGHT_BLUE_STAINED_GLASS;
            case RED:
            case DARK_RED:
                return Material.RED_STAINED_GLASS;
            case BLUE:
            case DARK_BLUE:
                return Material.BLUE_STAINED_GLASS;
            case GOLD:
                return Material.ORANGE_STAINED_GLASS;
            case GRAY:
                return Material.LIGHT_GRAY_STAINED_GLASS;
            case BLACK:
                return Material.BLACK_STAINED_GLASS;
            case GREEN:
                return Material.LIME_STAINED_GLASS;
            case YELLOW:
                return Material.YELLOW_STAINED_GLASS;
            case DARK_AQUA:
                return Material.CYAN_STAINED_GLASS;
            case DARK_GRAY:
                return Material.GRAY_STAINED_GLASS;
            case DARK_GREEN:
                return Material.GREEN_STAINED_GLASS;
            case DARK_PURPLE:
                return Material.PURPLE_STAINED_GLASS;
            case LIGHT_PURPLE:
                return Material.MAGENTA_STAINED_GLASS;
            default:
                return Material.WHITE_STAINED_GLASS;
        }
    }
    
    public static BarColor getBarColor(Clan clan) {
        if (clan == null)
            return BarColor.WHITE;
        ChatColor color = clan.getColor();
        switch (color) {
            case RED:
            case DARK_RED:
                return BarColor.RED;
            case DARK_PURPLE:
            case LIGHT_PURPLE:
                return BarColor.PURPLE;
            case GOLD:
            case YELLOW:
                return BarColor.YELLOW;
            case BLUE:
            case DARK_BLUE:
            case AQUA:
            case DARK_AQUA:
                return BarColor.BLUE;
            case GREEN:
            case DARK_GREEN:
                return BarColor.GREEN;
            default:
                return BarColor.WHITE;
        }
    }
    
    public static void setClanBanner(Clan clan, Location bannerLocation) {
        if (bannerLocation.getBlock().getType().toString().contains("_WALL_BANNER")) {
            Directional directional = (Directional) bannerLocation.getBlock().getBlockData();
            BlockFace face = directional.getFacing();
            
            if (clan == null || clan.getBanner() == null)
                bannerLocation.getBlock().setType(Material.WHITE_WALL_BANNER);
            else
                bannerLocation.getBlock().setType(Material.getMaterial(clan.getBanner().getType().toString().replace("_BANNER", "_WALL_BANNER")));
            //To apply direction
            
            //Set face to what it was before.
            Directional newFace = (Directional) bannerLocation.getBlock().getBlockData();
            newFace.setFacing(face);
            bannerLocation.getBlock().setBlockData(newFace);
        } else {
            Rotatable rotatable = (Rotatable) bannerLocation.getBlock().getBlockData();
            BlockFace face = rotatable.getRotation();
            
            if (clan == null || clan.getBanner() == null)
                bannerLocation.getBlock().setType(Material.WHITE_BANNER);
            else
                bannerLocation.getBlock().setType(clan.getBanner().getType());
            
            Rotatable newRotatable = (Rotatable) bannerLocation.getBlock().getBlockData();
            newRotatable.setRotation(face);
            bannerLocation.getBlock().setBlockData(newRotatable);
        }
        
        //If the banner meta of the clan banner is null, don't set the patterns.
        
        //Set directional to previous face
        
        if (clan == null || clan.getBanner() == null)
            return;
        
        BannerMeta clanBanner = (BannerMeta) clan.getBanner().getItemMeta();
        Banner banner = (Banner) bannerLocation.getBlock().getState();
        
        if (clanBanner != null) {
            banner.setPatterns(clanBanner.getPatterns());
            banner.update();
        }
    }
}
