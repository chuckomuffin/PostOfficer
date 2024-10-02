package com.postofficer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PostOfficeArea {

    private final List<Area> postOffices = new ArrayList<>();
    private final JavaPlugin plugin;
    private File postOfficeFile;
    private FileConfiguration postOfficeConfig;

    public PostOfficeArea(JavaPlugin plugin) {
        this.plugin = plugin;
        loadPostOffices();  // Load areas from file at startup
    }

    // Method to get all Post Office areas
    public List<Area> getPostOffices() {
        return postOffices;
    }

    // Method to check if a specific location is within any Post Office area
    public boolean isWithinPostOffice(Location loc) {
        for (Area area : postOffices) {
            if (area.isWithin(loc)) {
                return true;  // Location is within one of the Post Office areas
            }
        }
        return false;  // Location is not inside any Post Office area
    }

    // Method to get the Post Office area containing a specific location
    public Area getAreaContainingLocation(Location loc) {
        for (Area area : postOffices) {
            if (area.isWithin(loc)) {
                return area;  // Return the area that contains the location
            }
        }
        return null;  // No area contains the location
    }

    // Create Post Office area and save it to the file
    public void createPostOffice(Location corner1, Location corner2) {
        Area newArea = new Area(corner1, corner2);
        postOffices.add(newArea);
        savePostOfficeToFile(newArea);  // Save the new area to file
        spawnPostOfficer(newArea);  // Spawn the Post Officer
    }

    // Remove Post Office area and save changes to the file
    public void removePostOffice(Area area) {
        postOffices.remove(area);  // Remove from memory
        saveAllPostOffices();      // Save updated list to file
    }

    // Spawn a Post Officer in a given area
    public void spawnPostOfficer(Area area) {
        Location spawnLocation = area.getCenter();

        WanderingTrader postOfficer = (WanderingTrader) spawnLocation.getWorld().spawn(spawnLocation, WanderingTrader.class);
        postOfficer.setCustomName("Post Officer");
        postOfficer.setCustomNameVisible(true);
        postOfficer.setAI(true);
        postOfficer.setCollidable(false);
        postOfficer.setInvulnerable(true);  // Make the Post Officer immortal
        postOfficer.setMetadata("PostOfficer", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

        startPostOfficerBehavior(postOfficer, area);
    }

    // Behavior for Post Officer (moving to PO Boxes, etc.)
    private void startPostOfficerBehavior(WanderingTrader postOfficer, Area area) {
        // Implementation omitted for brevity
    }

    // Save a single Post Office area to the file
    private void savePostOfficeToFile(Area area) {
        String key = "post_offices." + area.getId();
        postOfficeConfig.set(key + ".corner1", area.getCorner1().serialize());
        postOfficeConfig.set(key + ".corner2", area.getCorner2().serialize());
        saveConfig();  // Save the file
    }

    // Save all Post Office areas to the file
    private void saveAllPostOffices() {
        postOfficeConfig.set("post_offices", null);  // Clear existing data
        for (Area area : postOffices) {
            savePostOfficeToFile(area);
        }
    }

    // Load Post Office areas from the file at startup
    private void loadPostOffices() {
        postOfficeFile = new File(plugin.getDataFolder(), "postoffices.yml");

        // Check if the file exists; if not, create a new empty one
        if (!postOfficeFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();  // Ensure the directory exists
                postOfficeFile.createNewFile();   // Create the file
                plugin.getLogger().info("postoffices.yml created successfully.");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create postoffices.yml!");
                e.printStackTrace();
            }
        }

        postOfficeConfig = YamlConfiguration.loadConfiguration(postOfficeFile);

        if (postOfficeConfig.contains("post_offices")) {
            for (String key : postOfficeConfig.getConfigurationSection("post_offices").getKeys(false)) {
                Location corner1 = Location.deserialize(postOfficeConfig.getConfigurationSection("post_offices." + key + ".corner1").getValues(true));
                Location corner2 = Location.deserialize(postOfficeConfig.getConfigurationSection("post_offices." + key + ".corner2").getValues(true));
                Area area = new Area(corner1, corner2);
                postOffices.add(area);
            }
        }
    }

    // Save the config to file
    private void saveConfig() {
        try {
            postOfficeConfig.save(postOfficeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Class representing a Post Office area
    public static class Area {
        private final Location corner1;
        private final Location corner2;
        private final String id;

        public Area(Location corner1, Location corner2) {
            this.corner1 = corner1;
            this.corner2 = corner2;
            this.id = corner1.getWorld().getName() + "_" + corner1.getBlockX() + "_" + corner1.getBlockY() + "_" + corner1.getBlockZ();
        }

        public boolean isWithin(Location loc) {
            double minX = Math.min(corner1.getX(), corner2.getX());
            double maxX = Math.max(corner1.getX(), corner2.getX());
            double minY = Math.min(corner1.getY(), corner2.getY());
            double maxY = Math.max(corner1.getY(), corner2.getY());
            double minZ = Math.min(corner1.getZ(), corner2.getZ());
            double maxZ = Math.max(corner1.getZ(), corner2.getZ());

            return loc.getX() >= minX && loc.getX() <= maxX &&
                    loc.getY() >= minY && loc.getY() <= maxY &&
                    loc.getZ() >= minZ && loc.getZ() <= maxZ;
        }

        public Location getCorner1() {
            return corner1;
        }

        public Location getCorner2() {
            return corner2;
        }

        public Location getCenter() {
            double centerX = (corner1.getX() + corner2.getX()) / 2;
            double centerY = (corner1.getY() + corner2.getY()) / 2;
            double centerZ = (corner1.getZ() + corner2.getZ()) / 2;
            return new Location(corner1.getWorld(), centerX, centerY, centerZ);
        }

        public String getId() {
            return id;
        }
    }
}
