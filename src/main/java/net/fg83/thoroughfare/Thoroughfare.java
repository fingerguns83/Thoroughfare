package net.fg83.thoroughfare;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class Thoroughfare extends JavaPlugin implements Listener {

    FileConfiguration config;
    @Override
    public void onEnable() {
        int pluginId = 20641;
        Metrics metrics = new Metrics(this, pluginId);

        saveDefaultConfig();
        config = getConfig();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event){
        if (event.getTo() == null){
            return;
        }
        if (event.getFrom().getBlock().equals(event.getTo().getBlock())){
            return;
        }
        if (event.getPlayer().isFlying() || event.getPlayer().isSneaking()){
            return;
        }
        getLogger().info("Player Move From: " + event.getFrom().getX() + ", " + event.getFrom().getY() + ", " + event.getFrom().getZ());
        getLogger().info("Player From Material: " + event.getFrom().getBlock().getType().toString());

        int fromX = event.getFrom().getBlockX();
        int fromY = (int) Math.ceil(event.getFrom().getY());
        int fromZ = event.getFrom().getBlockZ();



        Block steppedBlock = Objects.requireNonNull(event.getFrom().getWorld()).getBlockAt(fromX, fromY, fromZ).getRelative(BlockFace.DOWN);

        getLogger().info("Stepped Block: " + steppedBlock.getX() + ", " + steppedBlock.getY() + ", " + steppedBlock.getZ());
        getLogger().info("Stepped Block Material: " + steppedBlock.getType().toString());

        int value = 1;

        if (event.getPlayer().getVehicle() != null){
            Vehicle vehicle = (Vehicle) event.getPlayer().getVehicle();
            if (!vehicle.isOnGround()){
                return;
            }
            if (vehicle.getPassengers().isEmpty()){
                return;
            }
            switch (vehicle.getType()){
                case CAMEL:
                        value = config.getInt("camel.value");
                    break;
                case DONKEY:
                    value = config.getInt("donkey.value");
                    break;
                case HORSE:
                case SKELETON_HORSE:
                case ZOMBIE_HORSE:
                        value = config.getInt("horse.value");
                    break;
                case MULE:
                        value = config.getInt("mule.value");
                    break;
                case LLAMA:
                case TRADER_LLAMA:
                        value = config.getInt("llama.value");
                    break;
                case PIG:
                        value = config.getInt("pig.value");
                    break;
                case STRIDER:
                        value = config.getInt("strider.value");
                    break;
                default:
                    return;
            }
        }

        Material fromMat = event.getFrom().getBlock().getType();
        List<Material> allowedMats = Arrays.asList(
                Material.AIR,
                Material.GRASS,
                Material.TALL_GRASS,
                Material.ORANGE_TULIP,
                Material.PINK_TULIP,
                Material.CORNFLOWER,
                Material.RED_TULIP,
                Material.WHITE_TULIP,
                Material.LILY_OF_THE_VALLEY,
                Material.AZURE_BLUET,
                Material.DANDELION,
                Material.POPPY,
                Material.BLUE_ORCHID,
                Material.ALLIUM,
                Material.OXEYE_DAISY,
                Material.FERN,
                Material.LARGE_FERN,
                Material.ACACIA_FENCE_GATE,
                Material.BAMBOO_FENCE_GATE,
                Material.BIRCH_FENCE_GATE,
                Material.CHERRY_FENCE_GATE,
                Material.CRIMSON_FENCE_GATE,
                Material.DARK_OAK_FENCE_GATE,
                Material.JUNGLE_FENCE_GATE,
                Material.MANGROVE_FENCE_GATE,
                Material.OAK_FENCE_GATE,
                Material.SPRUCE_FENCE_GATE,
                Material.WARPED_FENCE_GATE,
                Material.TORCH
        );
        if (!allowedMats.contains(fromMat)){
            return;
        }

        if (!testBlockType(steppedBlock)) {
            setBlockSteps(steppedBlock, 0);
            return;
        }
        incrementBlock(steppedBlock, value);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        setBlockSteps(event.getBlock(), 0);
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        setBlockSteps(event.getBlock(), 0);
    }

    public boolean testBlockType(Block block){
        Material blockType = block.getType();
        return blockType.equals(Material.GRASS_BLOCK)
                || blockType.equals(Material.DIRT)
                || blockType.equals(Material.PODZOL)
                || blockType.equals(Material.MYCELIUM)
                || blockType.equals(Material.COARSE_DIRT)
                || blockType.equals(Material.DIRT_PATH);
    }

    public void incrementBlock(Block block, int amount){
        setBlockSteps(block, getBlockSteps(block) + amount);
        testBlockChange(block);
    }
    public void setBlockSteps(Block block, int value) {
        if (value >= 0 && value <= 4095) {
            int lower = value & 0xFF; // Extract the lower 8 bits
            int upper = (value >> 8) & 0xF; // Shift right by 8 bits and extract the upper 4 bits

            block.setMetadata("stepCountLower", new FixedMetadataValue(this, lower));
            block.setMetadata("stepCountUpper", new FixedMetadataValue(this, upper));
        } else {
            setBlockSteps(block, 0);
        }
    }
    public int getBlockSteps(Block block) {
        if (block.hasMetadata("stepCountLower") && block.hasMetadata("stepCountUpper")) {
            int lower = block.getMetadata("stepCountLower").get(0).asInt();
            int upper = block.getMetadata("stepCountUpper").get(0).asInt();

            return (upper << 8) | lower;
        }
        return 0;
    }

    public void testBlockChange(Block block){
        switch (block.getType()){
            case GRASS_BLOCK:
            case DIRT:
            case PODZOL:
            case MYCELIUM:
                if (getBlockSteps(block) >= config.getInt("grass-reps")){
                    block.setType(Material.COARSE_DIRT);
                    setBlockSteps(block, 0);
                }
                break;
            case COARSE_DIRT:
                if (getBlockSteps(block) >= config.getInt("coarse-dirt-reps")){
                    block.setType(Material.DIRT_PATH);
                    setBlockSteps(block, 0);
                }
                break;
            case DIRT_PATH:
                if (config.getBoolean("paths-wear")){
                    if (getBlockSteps(block) >= config.getInt("path-reps")){
                        block.setType(Material.COBBLESTONE_SLAB);
                        setBlockSteps(block, 0);
                    }
                }
                break;
        }
    }

}
