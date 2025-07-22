package com.yourname;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

public class WorldGuardExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private WorldGuardPlugin worldGuard;

    public WorldGuardExpansion(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canRegister() {
        return (worldGuard = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard")) != null;
    }

    @Override
    public String getIdentifier() {
        return "worldguard";
    }

    @Override
    public String getAuthor() {
        return "YourName";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getRequiredPlugin() {
        return "WorldGuard";
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (worldGuard == null) return "WG not loaded";

        // Разбиваем параметры на части: worldguard_region_{region}_{type}_{extra}
        String[] parts = params.split("_");
        if (parts.length < 3) return null;

        if (!parts[0].equalsIgnoreCase("region")) return null;

        String regionName = parts[1];
        String type = parts[2].toLowerCase();
        World world = player != null ? player.getWorld() : Bukkit.getWorlds().get(0);

        ProtectedRegion region = getRegion(regionName, world);
        if (region == null) return "Invalid region";

        switch (type) {
            case "player":
                return String.valueOf(countPlayersInRegion(region));

            case "owner":
                if (parts.length < 4) return "No index";
                try {
                    int index = Integer.parseInt(parts[3]);
                    return getOwner(region, index);
                } catch (NumberFormatException e) {
                    return "Invalid index";
                }

            case "member":
                if (parts.length < 4) return "No index";
                try {
                    int index = Integer.parseInt(parts[3]);
                    return getMember(region, index);
                } catch (NumberFormatException e) {
                    return "Invalid index";
                }

            default: // Обработка типов существ
                try {
                    EntityType entityType = EntityType.valueOf(type.toUpperCase());
                    return String.valueOf(countEntitiesInRegion(region, entityType));
                } catch (IllegalArgumentException e) {
                    return "Invalid type";
                }
        }
    }

    private ProtectedRegion getRegion(String name, World world) {
        RegionManager regionManager = worldGuard.getRegionManager(world);
        return regionManager != null ? regionManager.getRegion(name) : null;
    }

    private int countPlayersInRegion(ProtectedRegion region) {
        return (int) Bukkit.getOnlinePlayers().stream()
                .filter(p -> region.contains(
                    p.getLocation().getBlockX(),
                    p.getLocation().getBlockY(),
                    p.getLocation().getBlockZ()))
                .count();
    }

    private int countEntitiesInRegion(ProtectedRegion region, EntityType type) {
        World world = Bukkit.getWorld(region.getWorld().getName());
        if (world == null) return 0;

        return (int) world.getEntities().stream()
                .filter(e -> e.getType() == type)
                .filter(e -> region.contains(
                    e.getLocation().getBlockX(),
                    e.getLocation().getBlockY(),
                    e.getLocation().getBlockZ()))
                .count();
    }

    private String getOwner(ProtectedRegion region, int index) {
        List<String> owners = region.getOwners().getPlayers().stream()
                .map(p -> Bukkit.getOfflinePlayer(p.getUniqueId()).getName())
                .filter(name -> name != null)
                .collect(Collectors.toList());

        return index > 0 && index <= owners.size() ? owners.get(index - 1) : "None";
    }

    private String getMember(ProtectedRegion region, int index) {
        List<String> members = region.getMembers().getPlayers().stream()
                .map(p -> Bukkit.getOfflinePlayer(p.getUniqueId()).getName())
                .filter(name -> name != null)
                .collect(Collectors.toList());

        return index > 0 && index <= members.size() ? members.get(index - 1) : "None";
    }
}
