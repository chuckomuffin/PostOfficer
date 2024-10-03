package com.postofficer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.milkbowl.vault.economy.Economy;

public class EventListener implements Listener {
    private final POBoxManager poBoxManager;
    private final PostOfficeArea postOfficeArea;
    private final JavaPlugin plugin;
    private final Map<Player, Player> wandergramRecipients = new HashMap<>();  // Store sender -> recipient for Wandergram

    // Constructor
    public EventListener(POBoxManager poBoxManager, PostOfficeArea postOfficeArea, JavaPlugin plugin) {
        this.poBoxManager = poBoxManager;
        this.postOfficeArea = postOfficeArea;
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (event.getBlock().getType() == Material.BARREL) {
            Barrel barrel = (Barrel) event.getBlock().getState();
            if (barrel.getCustomName() != null && barrel.getCustomName().equals(player.getName() + "'s Mailbox")) {
                if (!poBoxManager.hasPOBox(playerUUID)) {
                    if (postOfficeArea.isWithinPostOffice(event.getBlock().getLocation())) {
                        poBoxManager.addPOBox(playerUUID, barrel.getLocation());
                        player.sendMessage(ChatColor.BLUE + "Your PO Box has been created!");
                    } else {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.BLUE + "You can only place a PO Box inside a Post Office area!");
                    }
                } else {
                    player.sendMessage(ChatColor.BLUE + "You already have a PO Box!");
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.BARREL) {
            Barrel barrel = (Barrel) event.getBlock().getState();
            if (poBoxManager.isBarrelPOBox(barrel)) {
                String ownerName = barrel.getCustomName().replace("'s Mailbox", "");
                Player owner = Bukkit.getPlayer(ownerName);
                UUID ownerUUID = owner != null ? owner.getUniqueId() : Bukkit.getOfflinePlayer(ownerName).getUniqueId();

                poBoxManager.removePOBox(ownerUUID);  // Remove the PO Box
                if (owner != null && owner.isOnline()) {
                    owner.sendMessage(ChatColor.BLUE + "Your PO Box has been destroyed!");
                }

                event.getPlayer().sendMessage(ChatColor.BLUE + "You have destroyed " + ownerName + "'s PO Box.");
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Barrel barrel) {
            if (poBoxManager.isBarrelPOBox(barrel)) {
                Inventory inventory = barrel.getInventory();
                if (inventory.isEmpty()) {
                    stopParticleEffect(barrel);
                } else {
                    startParticleEffect(barrel);
                    Player player = Bukkit.getPlayer(barrel.getCustomName().replace("'s Mailbox", ""));
                    if (player != null) {
                        player.sendMessage(ChatColor.BLUE + "You have new mail!");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (poBoxManager.hasPOBox(playerUUID)) {
            Location poBoxLocation = poBoxManager.getPOBox(playerUUID);
            if (poBoxLocation != null) {
                Barrel barrel = (Barrel) poBoxLocation.getBlock().getState();
                Inventory inventory = barrel.getInventory();
                if (!inventory.isEmpty()) {
                    player.sendMessage(ChatColor.BLUE + "You have mail waiting in your PO Box!");
                }
            }
        }
    }

    // Method to start the Wax On particle effect around the barrel (mimicking copper block wax_on behavior)
    private void startParticleEffect(Barrel barrel) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (barrel.getInventory().isEmpty()) {
                    cancel();  // Stop spawning particles if the barrel is empty
                    return;
                }

                Location barrelLocation = barrel.getLocation().add(0.5, 0.5, 0.5);

                for (int i = 0; i < 10; i++) {
                    int face = i % 6;
                    Location particleLoc = barrelLocation.clone();

                    switch (face) {
                        case 0: particleLoc.add(0, 0.5, 0); break;  // Top face
                        case 1: particleLoc.add(0, -0.5, 0); break;
                        case 2: particleLoc.add(0.5, 0, 0); break;
                        case 3: particleLoc.add(-0.5, 0, 0); break;
                        case 4: particleLoc.add(0, 0, 0.5); break;
                        case 5: particleLoc.add(0, 0, -0.5); break;
                    }

                    barrel.getWorld().spawnParticle(
                            Particle.WAX_ON,
                            particleLoc,
                            1, 0, 0, 0, 0.02
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 60L);
    }

    private void stopParticleEffect(Barrel barrel) {
        // No action needed; particles disappear naturally when the barrel is empty
    }

    // Handle interaction with the Post Officer
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        if (entity instanceof WanderingTrader && entity.hasMetadata("PostOfficer")) {
            event.setCancelled(true);  // Prevent the default trade window from opening
            openPostmastersAlmanac(player);
        }
    }

    // Custom GUI: Postmaster's Almanac
    private void openPostmastersAlmanac(Player player) {
        Inventory almanac = Bukkit.createInventory(null, 9, "Postmaster's Almanac");

        long ticks = Bukkit.getWorlds().get(0).getFullTime();
        TimeData timeData = calculateTimeSinceStart(ticks);

        // Create items for the Almanac GUI
        ItemStack yearsItem = createGuiItem(Material.BOOKSHELF, "Years Passed", timeData.years + " years");
        ItemStack monthsItem = createGuiItem(Material.BOOK, "Months Passed", timeData.months + " months");
        ItemStack weeksItem = createGuiItem(Material.PAPER, "Weeks Passed", timeData.weeks + " weeks");
        ItemStack daysItem = createGuiItem(Material.CLOCK, "Days Passed", timeData.days + " days");

        // "Send Allaygram" option
        ItemStack allaygramItem = createGuiItem(Material.FILLED_MAP, "Send Allaygram", "Send an item to a player!");

        // Weather Forecast option
        ItemStack weatherForecastItem = createGuiItem(Material.COMPASS, "Weather Forecast", "View the weather forecast");

        // Add items to the custom GUI
        almanac.setItem(0, yearsItem);
        almanac.setItem(1, monthsItem);
        almanac.setItem(2, weeksItem);
        almanac.setItem(3, daysItem);
        almanac.setItem(6, weatherForecastItem);  // Weather Forecast in slot 6
        almanac.setItem(8, allaygramItem);  // Allaygram in the last slot

        player.openInventory(almanac);
    }

    // Weather Forecast GUI
    private void openWeatherForecast(Player player) {
        Inventory weatherInventory = Bukkit.createInventory(null, 9, "Weather Forecast");

        World world = player.getWorld();
        boolean isCurrentlyRaining = world.hasStorm();
        boolean isThundering = world.isThundering();

        String currentWeather = isThundering ? "Thunderstorm" : (isCurrentlyRaining ? "Rain" : "Clear");

        // Prediction logic: Randomly predict rain or clear weather for the next day
        Random random = new Random();
        boolean willRainTomorrow = random.nextBoolean();
        String predictedWeather = willRainTomorrow ? "Rain" : "Clear";

        // Create items for weather forecast
        ItemStack currentWeatherItem = createGuiItem(Material.WATER_BUCKET, "Current Weather", currentWeather);
        ItemStack predictedWeatherItem = createGuiItem(Material.SUNFLOWER, "Tomorrow's Forecast", predictedWeather);

        // Add weather items to the forecast GUI
        weatherInventory.setItem(3, currentWeatherItem);
        weatherInventory.setItem(5, predictedWeatherItem);

        player.openInventory(weatherInventory);
    }

    // Helper method to create items for the GUI
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    // Handle clicks in the Postmaster's Almanac GUI
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String inventoryTitle = event.getView().getTitle();

        if (inventoryTitle.equals("Postmaster's Almanac")) {
            event.setCancelled(true);

            // Handle "Send Allaygram" option
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.FILLED_MAP) {
                if (plugin.getConfig().getBoolean("allaygram.enabled")) {
                    openPlayerSelectionGUI(player);
                } else {
                    player.sendMessage(ChatColor.RED + "Allaygram is currently disabled.");
                }
            }

            // Handle "Weather Forecast" option
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.COMPASS) {
                openWeatherForecast(player);
            }
        }
        // Handle player selection in the "Select a Player" GUI
        else if (inventoryTitle.equals("Select a Player")) {
            event.setCancelled(true);  // Prevent normal interaction with player heads

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD) {
                String selectedPlayerName = clickedItem.getItemMeta().getDisplayName();
                Player selectedPlayer = Bukkit.getPlayer(selectedPlayerName);

                if (selectedPlayer != null) {
                    // Open the sender's inventory for item selection and track recipient
                    openItemSelectionGUI(player, selectedPlayer);
                } else {
                    player.sendMessage(ChatColor.BLUE + "The player you selected is not online.");
                }
            }
        }
        // Handle item selection for the Allaygram in the sender's inventory
        else if (wandergramRecipients.containsKey(player)) {
            // The player is selecting an item for the Allaygram
            event.setCancelled(true);  // Prevent normal behavior

            Player recipient = wandergramRecipients.get(player);  // Get the recipient
            ItemStack selectedItem = event.getCurrentItem();  // The item being sent

            if (selectedItem != null && !selectedItem.getType().isAir()) {
                double cost = plugin.getConfig().getDouble("allaygram.cost");
                Economy economy = ((PostOfficer) plugin).getEconomy();
                String postOfficeAccount = ((PostOfficer) plugin).getPostOfficeAccount();

                if (economy.has(player, cost)) {
                    economy.withdrawPlayer(player, cost);
                    economy.depositPlayer(postOfficeAccount, cost);

                    // Send the selected item to the recipient using an Allay
                    sendAllaygram(player, recipient, selectedItem);

                    // Remove the item from the sender's inventory
                    player.getInventory().removeItem(selectedItem);

                    // Clear the tracking for this Allaygram
                    wandergramRecipients.remove(player);

                    // Close the inventory after sending
                    player.closeInventory();
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have enough money to send an Allaygram.");
                }
            } else {
                player.sendMessage(ChatColor.BLUE + "Please select a valid item to send.");
            }
        }
    }


    // Player selection GUI
    private void openPlayerSelectionGUI(Player sender) {
        Inventory playerSelection = Bukkit.createInventory(null, 27, "Select a Player");

        // Add player heads for all online players
        int index = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = playerHead.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(player.getName());
                // Set the head to represent the player
                ((SkullMeta) meta).setOwningPlayer(player);
                playerHead.setItemMeta(meta);
            }
            playerSelection.setItem(index, playerHead);
            index++;
        }

        // Open the player selection GUI for the sender
        sender.openInventory(playerSelection);
    }

    // Open inventory for item selection
    private void openItemSelectionGUI(Player sender, Player recipient) {
        // Inform the player to select an item to send
        sender.sendMessage(ChatColor.BLUE + "Select an item to send to " + recipient.getName());

        // Track the recipient for the Wandergram
        wandergramRecipients.put(sender, recipient);

        // Open the sender's inventory for them to select the item to send
        sender.openInventory(sender.getInventory());
    }


    // Send the item using an Allay that follows the recipient and delivers the item
    private void sendAllaygram(Player sender, Player recipient, ItemStack item) {
        Location recipientLocation = recipient.getLocation();

        // Calculate a random position 32 blocks away from the recipient
        Location allaySpawnLocation = getRandomLocationNear(recipientLocation, 32);

        // Summon an Allay at the calculated location
        Allay allay = (Allay) recipient.getWorld().spawnEntity(allaySpawnLocation, EntityType.ALLAY);
        allay.setCustomName("Allaygram Delivery");
        allay.setCustomNameVisible(true);
        allay.setInvulnerable(true);

        // Make the Allay hold the item
        allay.getEquipment().setItemInMainHand(item);

        // Task to make the Allay follow the recipient and deliver the item
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!allay.isValid()) {
                    cancel();
                    return;
                }

                // Follow the recipient's current location
                Location recipientHeadLocation = recipient.getLocation().add(0, 1.62, 0);  // Head height

                allay.getPathfinder().moveTo(recipientHeadLocation);

                // Check if the Allay is within 1 block of the recipient's head
                if (allay.getLocation().distance(recipientHeadLocation) <= 1) {
                    // Deliver the item to the recipient's inventory
                    recipient.getInventory().addItem(item);
                    sender.sendMessage(ChatColor.BLUE + "Your item has been sent to " + recipient.getName() + " via Allaygram!");

                    // Remove the item from the Allay's hand after delivery
                    allay.getEquipment().setItemInMainHand(null);

                    // Start the Allay flying away toward the nearest Post Office area
                    flyAwayToNearestPostOffice(allay, recipient);
                    cancel();  // Stop the delivery task
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);  // Run every tick (20 ticks per second)
    }

    // Make the Allay fly toward the nearest Post Office area after delivery without carrying any item
    private void flyAwayToNearestPostOffice(Allay allay, Player recipient) {
        Location nearestPostOffice = findNearestPostOffice(recipient.getLocation());

        // Check if we found a Post Office nearby
        if (nearestPostOffice != null) {
            // Fly the Allay toward the nearest Post Office
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!allay.isValid()) {
                        cancel();
                        return;
                    }

                    allay.getPathfinder().moveTo(nearestPostOffice);

                    // If the Allay has arrived at the Post Office (within 2 blocks)
                    if (allay.getLocation().distanceSquared(nearestPostOffice) <= 4) {
                        // Despawn the Allay after reaching the destination
                        allay.remove();
                        cancel();  // Stop the flight task
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);  // Run every tick (20 ticks per second)
        } else {
            // If no Post Office is found, fly 32 blocks away from the recipient and then despawn
            Location direction = recipient.getLocation().add(allay.getLocation().getDirection().multiply(32));

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!allay.isValid()) {
                        cancel();
                        return;
                    }

                    allay.getPathfinder().moveTo(direction);

                    // If the Allay has moved 32 blocks away from the recipient
                    if (allay.getLocation().distanceSquared(recipient.getLocation()) >= 1024) {  // 32 blocks = 32^2 = 1024
                        allay.remove();  // Despawn the Allay
                        cancel();  // Stop the flight task
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }
    }


    // Find the nearest Post Office area to the recipient's location
    private Location findNearestPostOffice(Location recipientLocation) {
        List<PostOfficeArea.Area> postOfficeAreas = postOfficeArea.getPostOffices();
        Location nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (PostOfficeArea.Area area : postOfficeAreas) {
            Location center = area.getCenter();
            double distance = recipientLocation.distance(center);

            if (distance < minDistance) {
                minDistance = distance;
                nearest = center;
            }
        }

        return nearest;  // Return the nearest Post Office area, or null if none
    }


    // Method to calculate a random location 32 blocks away from the recipient
    private Location getRandomLocationNear(Location center, int distance) {
        Random random = new Random();
        double angle = random.nextDouble() * 2 * Math.PI;  // Random angle
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        double y = center.getWorld().getHighestBlockYAt((int) x, (int) z);  // Get the highest Y coordinate at this X/Z
        return new Location(center.getWorld(), x, y, z);
    }



    // Time calculation method to convert ticks into Minecraft years, months, weeks, and days
    private TimeData calculateTimeSinceStart(long ticks) {
        long days = ticks / 24000;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

        days = days % 7;
        weeks = weeks % 4;
        months = months % 12;

        return new TimeData(years, months, weeks, days);
    }

    // Inner class to store time data
    private static class TimeData {
        long years;
        long months;
        long weeks;
        long days;

        public TimeData(long years, long months, long weeks, long days) {
            this.years = years;
            this.months = months;
            this.weeks = weeks;
            this.days = days;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals("Postmaster's Almanac")) {
            event.setCancelled(true);
        }
    }
}
