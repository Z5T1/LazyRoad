package com.creadri.lazyroad;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author VeraLapsa
 */
public class LazyMiner {

    private LazyRoad plugin;
    private Player player;
    private MinerData data;

    public LazyMiner(LazyRoad plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.data = new MinerData(plugin.getCheckIds());
    }

    public LazyMiner(LazyRoad plugin, Player player, MinerData data) {
        this.plugin = plugin;
        this.player = player;
        this.data = data;
    }

    public boolean SaveBlock(Block b) {
        if (!checkIfOne(b)) {
            return false;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        Collection<ItemStack> drops = null;
        if (tool != null && !tool.getType().isAir()) {
            try {
                drops = b.getDrops(tool, player);
            } catch (Exception ignored) {}
        }
        if (drops == null || drops.isEmpty()) {
            try {
                drops = b.getDrops(new ItemStack(Material.DIAMOND_PICKAXE));
            } catch (Exception ignored) {}
        }
        if (drops == null || drops.isEmpty()) {
            return false;
        }

        for (ItemStack drop : drops) {
            if (drop != null && !drop.getType().isAir() && drop.getAmount() > 0) {
                addDrop(drop.clone());
            }
        }
        return true;
    }

    private void addDrop(ItemStack drop) {
        int toAdd = drop.getAmount();
        int maxStack = drop.getMaxStackSize();

        for (ItemStack existing : data.getDrops().values()) {
            if (existing != null && existing.isSimilar(drop) && existing.getAmount() < maxStack) {
                int space = maxStack - existing.getAmount();
                if (toAdd <= space) {
                    existing.setAmount(existing.getAmount() + toAdd);
                    toAdd = 0;
                    break;
                } else {
                    existing.setAmount(maxStack);
                    toAdd -= space;
                }
            }
        }

        while (toAdd > 0) {
            int stackAmount = Math.min(toAdd, maxStack);
            ItemStack newStack = drop.clone();
            newStack.setAmount(stackAmount);
            data.put(data.size(), newStack);
            toAdd -= stackAmount;
        }
    }

    private boolean checkIfOne(Block b) {
        if (b == null) return false;
        Material mat = b.getType();
        if (!mat.isSolid()) return false;
        if (mat == Material.BEDROCK || mat == Material.BARRIER) return false;
        return true;
    }

    public void putBlocks() {
        Block t = player.getTargetBlock((java.util.Set<Material>) null, 10);
        if (t == null) {
            player.sendMessage(plugin.getMessage("messages.lazyminer.notChest"));
            return;
        }
        BlockState b = t.getState();
        if (!(b instanceof org.bukkit.block.Container)) {
            player.sendMessage(plugin.getMessage("messages.lazyminer.notChest"));
            return;
        }

        if (data.size() == 0) {
            player.sendMessage(plugin.getMessage("messages.lazyminer.emptyStore"));
            return;
        }

        org.bukkit.block.Container container = (org.bukkit.block.Container) b;
        Inventory chestInv = container.getInventory();

        List<ItemStack> toDeposit = new ArrayList<>();
        int initialTotalItems = 0;
        for (ItemStack is : data.getDrops().values()) {
            if (is != null && !is.getType().isAir() && is.getAmount() > 0) {
                toDeposit.add(is);
                initialTotalItems += is.getAmount();
            }
        }

        if (toDeposit.isEmpty()) {
            data.getDrops().clear();
            player.sendMessage(plugin.getMessage("messages.lazyminer.emptyStore"));
            saveMinerData();
            return;
        }

        HashMap<Integer, ItemStack> remaining = chestInv.addItem(toDeposit.toArray(new ItemStack[0]));
        data.getDrops().clear();
        int remainingTotalItems = 0;
        int idx = 0;
        for (ItemStack is : remaining.values()) {
            if (is != null && !is.getType().isAir() && is.getAmount() > 0) {
                remainingTotalItems += is.getAmount();
                data.put(idx++, is);
            }
        }
        saveMinerData();

        if (remaining.isEmpty()) {
            player.sendMessage(plugin.getMessage("messages.lazyminer.empty"));
        } else if (remainingTotalItems == initialTotalItems) {
            player.sendMessage(plugin.getMessage("messages.lazyminer.chestFull"));
        } else {
            player.sendMessage(plugin.getMessage("messages.lazyminer.chestPartialFull", data.size()));
        }
    }

    public int size() {
        return data.getDrops().size();
    }

    public boolean enabled() {
        return data.isEnabled();
    }

    public void disable() {
        data.setEnabled(false);
    }

    public void enable() {
        data.setEnabled(true);
    }

    public void addCheckID(int id) {
        data.addACheckID(id);
    }

    public boolean removeCheckId(int id) {
        return data.removeACheckId(id);
    }

    public void saveMinerData() {
        File folder = new File(plugin.getDataFolder(), "miners");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File saveFile = new File(folder, player.getName().concat(".yml"));
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("enabled", data.isEnabled());
            List<ItemStack> dropList = new ArrayList<>();
            for (ItemStack is : data.getDrops().values()) {
                if (is != null && !is.getType().isAir() && is.getAmount() > 0) {
                    dropList.add(is);
                }
            }
            yaml.set("drops", dropList);
            yaml.save(saveFile);
        } catch (Exception ex) {
            LazyRoad.log.severe("[LazyRoad] An error occurred when trying to save " + saveFile.getName() + ": " + ex.getMessage());
        }
    }

    public String checkIdsToString() {
        return data.checkIdsToString();
    }
}
