package com.postofficer;

import org.bukkit.Location;
import org.bukkit.block.Barrel;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;

public class POBoxManager {

    private final Map<UUID, Location> poBoxes = new HashMap<>();
    private final File dataFile = new File("path_to_data_file.yml");  // Set your file path
    private final YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);

    // Method to save POBox data
    public void saveData() {
        for (Map.Entry<UUID, Location> entry : poBoxes.entrySet()) {
            UUID playerUUID = entry.getKey();
            Location location = entry.getValue();
            dataConfig.set("poboxes." + playerUUID.toString(), location.serialize());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Other methods...

    public boolean isBarrelPOBox(Barrel barrel) {
        String name = barrel.getCustomName();
        return name != null && name.endsWith("'s Mailbox");
    }

    // Method to add, remove, and get PO Boxes using UUIDs

    public void addPOBox(UUID playerUUID, Location location) {
        poBoxes.put(playerUUID, location);
        // Code to save data to file (omitted for brevity)
    }

    public Location getPOBox(UUID playerUUID) {
        return poBoxes.get(playerUUID);
    }

    public boolean hasPOBox(UUID playerUUID) {
        return poBoxes.containsKey(playerUUID);
    }

    public void removePOBox(UUID playerUUID) {
        poBoxes.remove(playerUUID);
        // Code to save data to file (omitted for brevity)
    }
    public Location getRandomPOBoxInArea(PostOfficeArea.Area area) {
        // Filter PO Boxes to only those within the specified Post Office area
        List<Location> poBoxesInArea = poBoxes.values().stream()
                .filter(area::isWithin)
                .collect(Collectors.toList());

        if (poBoxesInArea.isEmpty()) {
            return null;  // No PO Boxes in the area
        }

        // Randomly select a PO Box from the list
        Random random = new Random();
        return poBoxesInArea.get(random.nextInt(poBoxesInArea.size()));
    }
}
