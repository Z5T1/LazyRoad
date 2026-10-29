package com.creadri.lazyroad;

import com.creadri.lazyroad.commands.LazyMinerCommand;
import com.creadri.lazyroad.commands.roadCommand;
import com.creadri.util.Messages;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gson.Gson;
import java.nio.file.Files;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author creadri some updates buy VeraLapsa
 */
public class LazyRoad extends JavaPlugin {

    private final LazyRoadPlayerListener playerListener = new LazyRoadPlayerListener(this);
    private boolean eventRegistered = false;
    // player properties
    private HashSet<String> playerPropStraight = new HashSet<String>();
    private HashMap<String, LazyMiner> lazyMiners = new HashMap<String, LazyMiner>();
    // roads and pillars
    private HashMap<String, Road> roads = new HashMap<String, Road>();
    private HashMap<String, Pillar> pillars = new HashMap<String, Pillar>();
    // files related
    private File roadsDirectory;
    private File pillarsDirectory;
    private File undoSave;
    private Map<String, CommandHandler> commands = new HashMap<String, CommandHandler>();
    private FilenameFilter filenameFilter = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".json");
        }
    };
    // LazyMiner
    private int checkIds[] = null;
    // config and message related
    public static Messages messages;
    public static final Logger log = Logger.getLogger("Minecraft");

    /**
     * method called by bukkit when the plugin is enabled.
     */
    @Override
    public void onEnable() {
        // configuration files
        try {

            roadsDirectory = new File(getDataFolder(), "roads");
            pillarsDirectory = new File(getDataFolder(), "pillars");

            if (!roadsDirectory.exists()) roadsDirectory.mkdirs();
            if (!pillarsDirectory.exists()) pillarsDirectory.mkdirs();

            PluginUtils.extractResourceZip(this, "defaultRoads.zip", getDataFolder());
            PluginUtils.extractResourceZip(this, "defaultPillars.zip", getDataFolder());

            // load roads and pillars
            loadRoads();
            loadPillars();

            undoSave = new File(getDataFolder(), "undo.dat");

            // load undo
            playerListener.deserializeRoadsUndos(undoSave);

            if (!getConfig().contains("version")) {
                this.saveDefaultConfig();
                this.getConfig().set("version", "" + getDescription().getVersion());
            } else if (!getConfig().getString("version").equalsIgnoreCase(getDescription().getVersion())) {
                this.saveDefaultConfig();
                this.getConfig().set("version", "" + getDescription().getVersion());
            }
            this.getConfig().options().copyDefaults(true);
            this.saveConfig();

        } catch (IOException ex) {

            log.log(Level.SEVERE, "[LazyRoad] : Errors on files, stopping");
            return;
        }

        setupLazyMinerIds();

        // Register events
        if (!eventRegistered) {
            PluginManager pm = getServer().getPluginManager();
            pm.registerEvents(this.playerListener, this);

            eventRegistered = true;

            log.log(Level.INFO, "[LazyRoad] : Version " + getDescription().getVersion() + " is enabled!");
        }

        commands.put("road", new roadCommand(this));
        commands.put("tunnel", new roadCommand(this));
        commands.put("bridge", new roadCommand(this));
        commands.put("lazyminer", new LazyMinerCommand(this));
    }

    /**
     * Bukkit method that is called when the plugin is disabled
     */
    @Override
    public void onDisable() {
        playerListener.serializeRoadsUndos(undoSave);
        for (Map.Entry<String, LazyMiner> entry : lazyMiners.entrySet()) {
            entry.getValue().saveMinerData();
        }
        log.info("[LazyRoad] : Plugin disabled");
    }
    
    /**
     * Handles the commands for the plugin.
     * @param sender
     * @param command
     * @param label
     * @param args
     * @return
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        CommandHandler handler = commands.get(command.getName().toLowerCase());

        if (handler != null) {
            return handler.perform(sender, command.getName().toLowerCase(), args);
        } else {
            return false;
        }
    }
    
    /** 
     * Handles tab completion 
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
    	List<String> completions = new ArrayList<String>();
    	String cmdName = cmd.getName().toLowerCase();
    	String cmdAlias = alias.toLowerCase();
    	
    	if (
			cmdName.equals("road") || cmdAlias.equals("road") || cmdAlias.equals("lr") || 
			cmdName.equals("tunnel") || cmdAlias.equals("tunnel") || cmdAlias.equals("lt") || 
			cmdName.equals("bridge") || cmdAlias.equals("bridge") || cmdAlias.equals("lb")
    	) {
    		if (args.length == 1) {
	    		if ("reload".startsWith(args[0].toLowerCase()))
	    			completions.add("reload");
	    		if ("stop".startsWith(args[0].toLowerCase()))
	    			completions.add("stop");
	    		if ("undo".startsWith(args[0].toLowerCase()))
	    			completions.add("undo");
	    		if ("straight".startsWith(args[0].toLowerCase()))
	    			completions.add("straight");
	    		if ("up".startsWith(args[0].toLowerCase()))
	    			completions.add("up");
	    		if ("down".startsWith(args[0].toLowerCase()))
	    			completions.add("down");
	    		if ("normal".startsWith(args[0].toLowerCase()))
	    			completions.add("normal");
	    		for (String string : roads.keySet()) {
	    			if (string.toLowerCase().startsWith(args[0].toLowerCase()))
	        			completions.add(string);
	    		}
    		}
    		else if (args.length == 2 && (cmdName.equals("bridge") || cmdAlias.equals("bridge") || cmdAlias.equals("lb"))) {
    			for (String string : pillars.keySet()) {
	    			if (string.toLowerCase().startsWith(args[1].toLowerCase()))
	        			completions.add(string);
	    		}
    		}
    	}
    	
    	else if (cmdName.equals("lazyminer") || cmdAlias.equals("lazyminer") || cmdAlias.equals("lm")) {
    		if (args.length == 1) {
    			if ("ids".startsWith(args[0].toLowerCase()))
	    			completions.add("ids");
    			if ("addid".startsWith(args[0].toLowerCase()))
	    			completions.add("addid");
    			if ("removeid".startsWith(args[0].toLowerCase()))
	    			completions.add("removeid");
    			if ("store".startsWith(args[0].toLowerCase()))
	    			completions.add("store");
    		}
    	}
    	
    	return completions;
    }
    
    /**
     * Loads the roads in the roads folder into the plugin.
     * @throws IOException
     */
    public void loadRoads() throws IOException {
        roads.clear();

        File[] files = roadsDirectory.listFiles(filenameFilter);

        int imax = files.length;
        for (int i = 0; i < imax; i++) {
            File f = files[i];

            String name = "noRoad";

            try {
                name = f.getName();
                name = name.substring(0, name.length() - 5);
                name = name.toLowerCase();

                Gson gson = new Gson();
                String json = Files.readString(f.toPath());
                Road openRoad = gson.fromJson(json, Road.class);
                roads.put(name, openRoad);

            } catch (Exception ex) {
                log.warning("[LazyRoad] An error occured while parsing the road " + name + " !");
            }
        }
    }
    /**
     * Loads the pillars in the pillars folder into the plugin.
     * @throws IOException
     */
    public void loadPillars() throws IOException {
        pillars.clear();

        File[] files = pillarsDirectory.listFiles(filenameFilter);

        int imax = files.length;
        for (int i = 0; i < imax; i++) {
            File f = files[i];

            String name = "noPillar";

            try {
                name = f.getName();
                name = name.substring(0, name.length() - 5);
                name = name.toLowerCase();

                Gson gson = new Gson();
                String json = Files.readString(f.toPath());
                Pillar openPillar = gson.fromJson(json, Pillar.class);
                pillars.put(name, openPillar);

            } catch (Exception ex) {
                log.warning("[LazyRoad] An error occured while parsing the Pillar " + name + " !");
            }
        }
    }

    /**
     * Takes a string and replaces &# color codes with ChatColors
     *
     * @param message
     * @return
     */
    protected String replaceColors(String message) {
        return message.replaceAll("(?i)&([a-f0-9])", "\u00A7$1");
    }
    /**
     * Get a message from the config.
     * @param node The node in the config.
     * @param values 0 or more values that are to be inserted into the message.
     * @return The parsed message.
     */
    public String getMessage(String node, Object... values) {
        String msg = getConfig().getString(node);
        if (msg == null) {
            return node;
        }
        msg = replaceColors(msg);
        if (values != null) {
            for (int j = 0; j < values.length; j++) {
                String fieldName = "%" + j;
                msg = msg.replaceFirst(fieldName, values[j].toString());
            }
        }
        return msg;
    }
    /**
     * Generates the int[] checkIds from the config
     */
    private void setupLazyMinerIds() {
        String ids = getConfig().getString("lazyminer.ids", "");
        if (ids.equalsIgnoreCase("")) {
            return;
        }
        ids = ids.replace('[', ' ').replace(']', ' ');
        String[] parsedIds = ids.split(",");

        if (parsedIds.length > 0) {
            checkIds = new int[parsedIds.length];
            for (int i = 0; i < parsedIds.length; i++) {
                parsedIds[i] = parsedIds[i].trim();
                try {
                    checkIds[i] = Integer.parseInt(parsedIds[i]);
                } catch (NumberFormatException numberFormatException) {
                    log.warning("Error Parsing " + parsedIds[i] + " as a number.");
                }
            }
        } else {
            log.warning("No Id's set for the LazyMiner Feature.");
        }
    }
    /**
     *
     * @param name
     * @return If the Miner if the plugin has the miner else it returns null
     */
    public LazyMiner getLazyMiner(String name) {
        if (lazyMiners.containsKey(name)) {
            return lazyMiners.get(name);
        } else {
            return null;
        }

    }
    /**
     * Removes a miner from the plugin.
     * @param name
     */
    public void removeMiner(String name) {
        if (lazyMiners.containsKey(name)) {
            lazyMiners.remove(name);
        }
    }
    /**
     * Adds a new Miner to the plugin.
     * @param name Name of the miner
     * @param lm The new Miner
     */
    public void putLazyMiner(String name, LazyMiner lm) {
        lazyMiners.put(name, lm);
    }
    /**
     *
     * @return Returns the array of checkids.
     */
    public int[] getCheckIds() {
        return checkIds;
    }
    /**
     * Gets the plugins loaded Pillars
     * @return
     */
    public HashMap<String, Pillar> getPillars() {
        return pillars;
    }
    /**
     * Set the plugins Pillars
     * @param pillars
     */
    public void setPillars(HashMap<String, Pillar> pillars) {
        this.pillars = pillars;
    }
    /**
     * Gets the plugins loaded Roads
     * @return
     */
    public HashMap<String, Road> getRoads() {
        return roads;
    }
    /**
     * Set the plugins Roads
     * @param roads
     */
    public void setRoads(HashMap<String, Road> roads) {
        this.roads = roads;
    }

    /**
     * Gets if the player should lay the road straight
     * @param name
     * @return
     */
    public boolean getPlayerPropStraight(String name) {
        return playerPropStraight.contains(name);
    }
    /**
     * Sets if the player should lay the road straight
     * @param name
     * @param state
     */
    public void setPlayerPropStraight(String name, boolean state) {
        if (state) {
            playerPropStraight.add(name);
        } else {
            playerPropStraight.remove(name);
        }
    }
    /**
     * Returns the plugins playerListiner
     * @return
     */
    public LazyRoadPlayerListener getPlayerListener() {
        return playerListener;
    }
    
}
