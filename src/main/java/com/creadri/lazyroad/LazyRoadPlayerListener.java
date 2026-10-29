package com.creadri.lazyroad;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Handle events for all Player related events
 *
 * @author creadri
 */
public class LazyRoadPlayerListener implements Listener {

    private final LazyRoad plugin;
    private HashMap<String, RoadEnabled> builders;
    private HashMap<String, Stack<Undo>> undoers;

    /**
     *
     * @param instance
     */
    public LazyRoadPlayerListener(LazyRoad instance) {
        plugin = instance;
        builders = new HashMap<String, RoadEnabled>();
        undoers = new HashMap<String, Stack<Undo>>();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {

        if (event.isCancelled()) {
            return;
        }

        RoadEnabled road = builders.get(event.getPlayer().getName());

        if (road == null) {
            return;
        }

        road.drawRoad(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        String player = event.getPlayer().getName();

        RoadEnabled road = builders.get(player);

        if (road == null) {
            return;
        }

        if (road.getWorld().equals(event.getPlayer().getWorld())) {
            return;
        }

        removeBuilder(player);

        event.getPlayer().sendMessage(plugin.getMessage("messages.teleported"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        File minerFolder = new File(plugin.getDataFolder(), "miners");
        if (!minerFolder.exists()) {
            minerFolder.mkdirs();
        }
        File saveFile = new File(minerFolder, player.getName() + ".yml");
        if (saveFile.exists()) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(saveFile);
                boolean enabled = yaml.getBoolean("enabled", false);
                List<?> list = yaml.getList("drops");
                Map<Integer, ItemStack> drops = new HashMap<Integer, ItemStack>();
                if (list != null) {
                    int idx = 0;
                    for (Object obj : list) {
                        if (obj instanceof ItemStack) {
                            drops.put(idx++, (ItemStack) obj);
                        }
                    }
                }
                int[] checkIds = plugin.getCheckIds();
                MinerData minerData = new MinerData(checkIds != null ? checkIds : new int[0], drops, enabled);
                if (!player.hasPermission("lazyroad.lazyminer")) {
                    minerData.setEnabled(false);
                }
                plugin.putLazyMiner(player.getName(), new LazyMiner(plugin, player, minerData));
            } catch (Exception ex) {
                plugin.log.warning("[LazyRoad] Could not load miner data for " + player.getName() + ": " + ex.getMessage());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        LazyMiner lm = plugin.getLazyMiner(event.getPlayer().getName());
        if (lm != null) {
            lm.saveMinerData();
            plugin.removeMiner(event.getPlayer().getName());
        }
    }

    public boolean addBuilder(String player, RoadEnabled road) {
        if (builders.containsKey(player)) {
            return false;
        }

        builders.put(player, road);
        return true;
    }

    public RoadEnabled getBuilder(String player) {
        return builders.get(player);
    }

    public RoadEnabled setForceUp(String player) {
        RoadEnabled re;
        if (builders.containsKey(player)) {
            re = builders.get(player);
            re.setForceUp(true);
            re.setForceDown(false);
        } else {
            re = null;
        }
        return re;
    }

    public RoadEnabled setForceDown(String player) {
        RoadEnabled re;
        if (builders.containsKey(player)) {
            re = builders.get(player);
            re.setForceUp(false);
            re.setForceDown(true);
        } else {
            re = null;
        }
        return re;
    }

    public RoadEnabled setNormal(String player) {
        RoadEnabled re;
        if (builders.containsKey(player)) {
            re = builders.get(player);
            re.setForceUp(false);
            re.setForceDown(false);
        } else {
            re = null;
        }
        return re;
    }

    public RoadEnabled removeBuilder(String player) {
        RoadEnabled re = builders.remove(player);
        if (re != null) {
            if (!undoers.containsKey(player)) {
                undoers.put(player, new Stack<Undo>());
            }
            undoers.get(player).push(re.getUndo());
        }
        return re;
    }

    public boolean undo(String player) {
        RoadEnabled re = builders.get(player);
        if (re != null) {
            re.undo();
            return true;

        } else {

            //get undo stack
            Stack<Undo> stack = undoers.get(player);
            if (stack == null || stack.empty()) {
                return false;
            }

            stack.pop().undo();
            return true;
        }
    }

    public void serializeRoadsUndos(File file) {
        Iterator<String> itPlayers = ((HashMap<String, RoadEnabled>) builders.clone()).keySet().iterator();
        while (itPlayers.hasNext()) {
            removeBuilder(itPlayers.next());
        }
        try {
            Gson gson = new Gson();
            String json = gson.toJson(undoers);
            Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            LazyRoad.log.warning("[LazyRoad] Unable to save undo file.");
        }
    }

    public void deserializeRoadsUndos(File file) {
        if (!file.exists()) {
            return;
        }
        try {
            Gson gson = new Gson();
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            Type mapType = new TypeToken<HashMap<String, Stack<Undo>>>(){}.getType();
            undoers = gson.fromJson(json, mapType);
            if (undoers == null) {
                undoers = new HashMap<String, Stack<Undo>>();
            }
        } catch (Exception ex) {
            LazyRoad.log.warning("[LazyRoad] Unable to read undo file.");
        }
    }
}
