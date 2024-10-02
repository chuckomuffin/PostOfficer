package com.postofficer.commands;

import com.postofficer.POBoxManager;
import com.postofficer.PostOfficeArea;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class POCommand implements CommandExecutor {

    private final POBoxManager poBoxManager;
    private final PostOfficeArea postOfficeArea;

    // Temporary storage to handle area creation
    private final Map<Player, Block[]> cornerSelections = new HashMap<>();  // Changed to store Blocks

    public POCommand(POBoxManager poBoxManager, PostOfficeArea postOfficeArea) {
        this.poBoxManager = poBoxManager;
        this.postOfficeArea = postOfficeArea;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("Usage: /po <create|destroy|postal|demolish>");
            return true;
        }

        // Handle "/po create" command
        if (args[0].equalsIgnoreCase("create")) {
            if (!player.hasPermission("postofficer.create")) {
                player.sendMessage("You don't have permission to use this command.");
                return true;
            }

            // Store the selected corner for area creation using the block the player is targeting
            Block targetBlock = player.getTargetBlockExact(100);  // Get block the player is looking at (up to 100 blocks away)
            if (targetBlock == null || targetBlock.getType() == Material.AIR) {
                player.sendMessage("You must target a valid block.");
                return true;
            }

            if (args.length == 2 && args[1].equalsIgnoreCase("corner1")) {
                cornerSelections.putIfAbsent(player, new Block[2]);
                cornerSelections.get(player)[0] = targetBlock;
                player.sendMessage("Corner 1 has been set at: " + targetBlock.getLocation());
                return true;
            }

            if (args.length == 2 && args[1].equalsIgnoreCase("corner2")) {
                cornerSelections.putIfAbsent(player, new Block[2]);
                cornerSelections.get(player)[1] = targetBlock;
                player.sendMessage("Corner 2 has been set at: " + targetBlock.getLocation());

                // Check if both corners are set
                Block[] corners = cornerSelections.get(player);
                if (corners[0] != null && corners[1] != null) {
                    // Create the Post Office area using the two corners
                    Location loc1 = corners[0].getLocation();
                    Location loc2 = corners[1].getLocation();
                    postOfficeArea.createPostOffice(loc1, loc2);  // Create and save the area
                    player.sendMessage("Post Office area created!");
                    cornerSelections.remove(player);  // Clear selection
                } else {
                    player.sendMessage("You need to set both Corner 1 and Corner 2 to create a Post Office area.");
                }
                return true;
            }
        }

        // Handle "/po destroy [playername]" command
        if (args[0].equalsIgnoreCase("destroy")) {
            if (!player.hasPermission("postofficer.destroy")) {
                player.sendMessage("You don't have permission to use this command.");
                return true;
            }

            if (args.length == 2) {
                String playerName = args[1];
                Player target = Bukkit.getPlayer(playerName);
                if (target != null) {
                    // Remove the PO Box of the target player
                    if (poBoxManager.hasPOBox(target.getUniqueId())) {
                        poBoxManager.removePOBox(target.getUniqueId());
                        player.sendMessage("PO Box for " + playerName + " has been destroyed.");
                    } else {
                        player.sendMessage(playerName + " does not have a PO Box.");
                    }
                } else {
                    player.sendMessage("Player " + playerName + " not found.");
                }
            } else {
                player.sendMessage("Usage: /po destroy [playername]");
            }
            return true;
        }

        // Handle "/po demolish" command
        if (args[0].equalsIgnoreCase("demolish")) {
            if (!player.hasPermission("postofficer.demolish")) {
                player.sendMessage("You don't have permission to use this command.");
                return true;
            }

            // Check if the player is standing inside a Post Office area
            Location playerLocation = player.getLocation();
            PostOfficeArea.Area areaToRemove = postOfficeArea.getAreaContainingLocation(playerLocation);

            if (areaToRemove != null) {
                // Remove the Post Office area
                postOfficeArea.removePostOffice(areaToRemove);
                player.sendMessage("Post Office area has been demolished.");
            } else {
                player.sendMessage("You are not standing in a Post Office area.");
            }
            return true;
        }

        // Handle "/po postal" command
        if (args[0].equalsIgnoreCase("postal")) {
            if (!player.hasPermission("postofficer.postal")) {
                player.sendMessage("You don't have permission to use this command.");
                return true;
            }

            // Kill all Post Officers (Wandering Traders)
            killAllPostOfficers();
            // Respawn new Post Officers in every Post Office area
            respawnPostOfficers();

            player.sendMessage("All Post Officers have been reset.");
            return true;
        }

        player.sendMessage("Usage: /po <create|destroy|postal|demolish>");
        return true;
    }

    // Method to kill all existing Post Officers (Wandering Traders)
    private void killAllPostOfficers() {
        List<Entity> entities = Bukkit.getWorlds().get(0).getEntities();  // Get all entities in the main world
        for (Entity entity : entities) {
            if (entity instanceof WanderingTrader trader && trader.getCustomName() != null &&
                    trader.getCustomName().equals("Post Officer") && trader.isInvulnerable()) {
                trader.remove();  // Remove the Post Officer
            }
        }
    }

    // Method to respawn Post Officers in every Post Office area
    private void respawnPostOfficers() {
        List<PostOfficeArea.Area> postOfficeAreas = postOfficeArea.getPostOffices();
        for (PostOfficeArea.Area area : postOfficeAreas) {
            postOfficeArea.spawnPostOfficer(area);  // Spawn a new Post Officer at the center of each Post Office area
        }
    }
}
