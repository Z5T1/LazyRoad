package com.creadri.lazyroad;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/**
 *
 * @author creadri
 */
public class RoadEnabled {

    private Road road;
    private Player player;
    private Pillar pillar = null;
    private int count;
    private int lastBuiltStairs = -1;
    private boolean hasBuilt = false;
    private boolean tunnel = false;
    private boolean bridge = false;
    private boolean straight = false;
    private boolean forceUp = false;
    private boolean forceDown = false;
    private int oldX;
    private int oldY;
    private int oldZ;
    private Direction oldDir;
    private Undo undo;
    private World world;
    protected final LazyRoad plugin;
    private LazyMiner lm = null;

    public RoadEnabled(Player player, Road road, LazyRoad plugin) {
        this.player = player;
        Location loc = player.getLocation();
        this.world = loc.getWorld();
        this.oldX = loc.getBlockX();
        this.oldZ = loc.getBlockZ();
        this.oldY = getYFirstBlock(oldX, loc.getBlockY(), oldZ);
        this.oldDir = getDirection(player.getLocation());

        this.road = road;
        this.count = 0;
        this.undo = new Undo(world);
        this.plugin = plugin;
        this.lm = plugin.getLazyMiner(player.getName());
    }

    public void setPillar(Pillar pillar) {
        this.pillar = pillar;
    }

    public boolean isStraight() {
        return straight;
    }

    public void setStraight(boolean straight) {
        this.straight = straight;
    }

    public void undo() {
        undo.undo();
    }

    public Undo getUndo() {
        return undo;
    }

    public void drawRoad(Player player) {

        Location playerLocation = player.getLocation();

        // player current coordinates
        int x = playerLocation.getBlockX();
        int z = playerLocation.getBlockZ();
        
        if (bridge && hasBuilt) {
            float yaw = playerLocation.getYaw();
            if (yaw < 0) {
                yaw += 360;
            }
            if (yaw >= 315 || yaw < 45) { // south
                z += 1;
            } else if (yaw < 135) { // west
                x -= 1;
            } else if (yaw < 225) { // north
                z -= 1;
            } else { // east
                x += 1;
            }
        }

        // do not bother check the height
        if (x == oldX && z == oldZ) {
            return;
        }

        // get y coordinate
        int y = getYFirstBlock(x, playerLocation.getBlockY(), z);

        // bridge mode forces straight and just preserves y
        if (bridge) {
            straight = true;
            if (hasBuilt) {
                y = oldY;
            }
        }


        // constraint the y by the tunnel mode or to make stairs
        if (hasBuilt && tunnel && !forceUp && !forceDown) {
            // for tunnel mode, always keep old Y
            y = oldY;
        } else if (hasBuilt && forceUp) {
            if ((count - lastBuiltStairs) < road.getMaxGradient()) {
                y = oldY;
            } else {
                y = oldY + 1;
            }
        } else if (hasBuilt && forceDown) {
            if ((count - lastBuiltStairs) < road.getMaxGradient()) {
                y = oldY;
            } else {
                y = oldY - 1;
            }
        } else if (hasBuilt && (oldY - y) != 0) {
            // limit the y value for stairs to apply correctly
            if ((count - lastBuiltStairs) < road.getMaxGradient()) {
                y = oldY;
            } else if (oldY - y > 1) {
                y = oldY - 1;
            } else if (y - oldY > 1) {
                y = oldY + 1;
            }
        }

        Direction dir = getDirection(playerLocation);

        switch (dir) {
            case EAST:
                if ((hasBuilt && (oldZ - z) <= 0) || oldDir == Direction.WEST) {
                    return;
                }
                if (straight) {
                    x = oldX;
                }
                if (drawCorner(x, y, z, dir)) {
                    return;
                }
                drawEast(x, y, z, tunnel);
                break;
            case NORTH:
                if ((hasBuilt && (oldX - x) <= 0) || oldDir == Direction.SOUTH) {
                    return;
                }
                if (straight) {
                    z = oldZ;
                }
                if (drawCorner(x, y, z, dir)) {
                    return;
                }
                drawNorth(x, y, z, tunnel);
                break;
            case SOUTH:
                if ((hasBuilt && (x - oldX) <= 0) || oldDir == Direction.NORTH) {
                    return;
                }
                if (straight) {
                    z = oldZ;
                }
                if (drawCorner(x, y, z, dir)) {
                    return;
                }
                drawSouth(x, y, z, tunnel);
                break;
            case WEST:
                if ((hasBuilt && (z - oldZ) <= 0) || oldDir == Direction.EAST) {
                    return;
                }
                if (straight) {
                    x = oldX;
                }
                if (drawCorner(x, y, z, dir)) {
                    return;
                }
                drawWest(x, y, z, tunnel);
                break;
        }

        // saving current data to old ones
        oldDir = dir;
        oldX = x;
        oldZ = z;
        oldY = y;

        hasBuilt = true;

        count++;

        if (count % 20 == 0) {
            //LazyRoad.messages.sendPlayerMessage(player, "messages.roadCount", count);
            player.sendMessage(plugin.getMessage("messages.roadCount", count));
        }
    }

    private Direction getDirection(Location loc) {
        float rot = loc.getYaw() % 360;
        if (rot < 0) {
            rot += 360;
        }
        if ((rot >= 0 && rot < 45) || (rot >= 315 && rot <= 360)) {
            return Direction.WEST;
        } else if (rot >= 45 && rot < 135) {
            return Direction.NORTH;
        } else if (rot >= 135 && rot < 225) {
            return Direction.EAST;
        } else if (rot >= 225 && rot < 315) {
            return Direction.SOUTH;
        }
        return null;
    }

    private void putBlock(int x, int y, int z, String stringData, Direction dir) {
        if (stringData == null) {
            return;
        }

        Block b = world.getBlockAt(x, y, z);
        try {
            BlockData targetData = Bukkit.getServer().createBlockData(stringData);
            if (targetData instanceof org.bukkit.block.data.Directional) {
                org.bukkit.block.data.Directional dirData = (org.bukkit.block.data.Directional) targetData;
                org.bukkit.block.BlockFace newFace = dirData.getFacing();
                int rotations = 0;
                switch(dir) {
                      case WEST: rotations = 0; break;
                      case NORTH: rotations = 1; break;
                      case EAST: rotations = 2; break;
                      case SOUTH: rotations = 3; break;
                  }
                for (int i=0; i<rotations; i++) {
                    switch(newFace) {
                        case NORTH: newFace = org.bukkit.block.BlockFace.EAST; break;
                        case EAST: newFace = org.bukkit.block.BlockFace.SOUTH; break;
                        case SOUTH: newFace = org.bukkit.block.BlockFace.WEST; break;
                        case WEST: newFace = org.bukkit.block.BlockFace.NORTH; break;
                        case NORTH_EAST: newFace = org.bukkit.block.BlockFace.SOUTH_EAST; break;
                        case SOUTH_EAST: newFace = org.bukkit.block.BlockFace.SOUTH_WEST; break;
                        case SOUTH_WEST: newFace = org.bukkit.block.BlockFace.NORTH_WEST; break;
                        case NORTH_WEST: newFace = org.bukkit.block.BlockFace.NORTH_EAST; break;
                        default: break;
                    }
                }
                try { dirData.setFacing(newFace); targetData = dirData; } catch (Exception e) {}
            }
            if (b.getBlockData().matches(targetData)) return;
            undo.put(b);
            if (!b.getType().isAir()) {
                LazyMiner miner = plugin.getLazyMiner(player.getName());
                if (miner != null && miner.enabled() && player.hasPermission("lazyroad.lazyminer")) {
                    miner.SaveBlock(b);
                }
            }
            if (!bridge && !b.getType().isAir() && (targetData.getMaterial().isAir() || targetData.getMaterial().isTransparent())) {
                try {
                    org.bukkit.event.block.BlockDamageEvent event = new org.bukkit.event.block.BlockDamageEvent(
                        player, b, player.getInventory().getItemInMainHand(), true
                    );
                    org.bukkit.Bukkit.getPluginManager().callEvent(event);
                } catch (Exception ignored) {}
            }
            b.setBlockData(targetData, true);
        } catch (IllegalArgumentException ex) {
            // Silently ignore invalid block data strings like "minecraft:lantern_slab" produced by custom templates
        }
    }
    private void drawPillar(PillarPart pillarPart, int x, int z, int topY, Direction dir, boolean tunnel, boolean bridge) {
        if (pillarPart == null) return;
        int width = pillarPart.getWidth();
        int coreHeight = pillarPart.getHeight();
        int baseHeight = pillarPart.getBaseHeight();
        String[][] coreBlocks = pillarPart.getBlockDatas();
        String[][] baseBlocks = pillarPart.getBaseBlockDatas();
        int buildUntil = pillarPart.getBuildUntil();
        if (buildUntil == 0) {
            buildUntil = Integer.MAX_VALUE;
        }

        int pillarX = x;
        int pillarZ = z;
        if (dir == Direction.NORTH || dir == Direction.SOUTH) {
            pillarX = (tunnel || bridge) ? (dir == Direction.NORTH ? x - 1 : x + 1) : x;
        } else {
            pillarZ = (tunnel || bridge) ? (dir == Direction.WEST ? z + 1 : z - 1) : z;
        }

        int offset = pillarPart.getPlayerOffset();

        // Probe downward from topY to locate ground
        int groundY = -1;
        for (int probeY = topY; probeY > 0; probeY--) {
            boolean hitSolid = false;
            for (int j = 0; j < width; j++) {
                int px = pillarX;
                int pz = pillarZ;
                if (dir == Direction.NORTH) pz = z + offset - j;
                else if (dir == Direction.SOUTH) pz = z - offset + j;
                else if (dir == Direction.WEST) px = x + offset - j;
                else if (dir == Direction.EAST) px = x - offset + j;

                if (!isToIgnoreForPillar(world.getBlockAt(px, probeY, pz))) {
                    hitSolid = true;
                    break;
                }
            }
            if (hitSolid) {
                groundY = probeY;
                break;
            }
        }

        int availableGap = groundY >= 0 ? (topY - groundY) : (topY);

        if (baseHeight > 0 && baseBlocks != null) {
            // 1. Build the base from the ground up
            int baseBottomY = groundY + 1;
            if (availableGap <= baseHeight) {
                // Gap is smaller than or equal to base: build from bottom up towards road and stop
                for (int curY = baseBottomY; curY <= topY; curY++) {
                    int baseLayer = curY - baseBottomY; // 0 = bottom-most layer touching ground
                    if (baseLayer >= baseHeight) break;
                    int row = baseHeight - 1 - baseLayer;
                    drawPillarLayer(baseBlocks[row], width, offset, pillarX, curY, pillarZ, z, x, dir);
                }
                return;
            }

            // Full base fits on the ground
            for (int mh = 0; mh < baseHeight; mh++) {
                int curY = baseBottomY + (baseHeight - 1 - mh);
                drawPillarLayer(baseBlocks[mh], width, offset, pillarX, curY, pillarZ, z, x, dir);
            }

            // 2. Build the top pillar core downwards from topY
            // If buildUntil is specified (> 0), core only extends down by at most buildUntil blocks.
            // Any space between the bottom of the core and the top of the base remains air (intentional opening/gap).
            int coreBottomLimitY = baseBottomY + baseHeight;
            if (buildUntil < Integer.MAX_VALUE) {
                int maxCoreY = topY - buildUntil + 1;
                if (maxCoreY > coreBottomLimitY) {
                    coreBottomLimitY = maxCoreY;
                }
            }

            int coreLayer = 0;
            for (int curY = topY; curY >= coreBottomLimitY; curY--) {
                int row = coreLayer >= coreHeight ? (coreHeight - 1) : coreLayer;
                drawPillarLayer(coreBlocks[row], width, offset, pillarX, curY, pillarZ, z, x, dir);
                coreLayer++;
            }
        } else {
            // No base defined: repeating core builds down from topY until ground or buildUntil
            int totalLimit = Math.min(availableGap, buildUntil);
            int coreLayer = 0;
            for (int curY = topY; curY > 0 && curY > (topY - totalLimit); curY--) {
                int row = coreLayer >= coreHeight ? (coreHeight - 1) : coreLayer;
                drawPillarLayer(coreBlocks[row], width, offset, pillarX, curY, pillarZ, z, x, dir);
                coreLayer++;
            }
        }
    }

    private void drawPillarLayer(String[] rowData, int width, int offset, int pillarX, int curY, int pillarZ, int z, int x, Direction dir) {
        if (rowData == null || curY <= 0) return;
        for (int j = 0; j < width; j++) {
            String blockData = rowData[j];
            if (blockData != null && !blockData.equals("minecraft:air")) {
                int px = pillarX;
                int pz = pillarZ;
                if (dir == Direction.NORTH) pz = z + offset - j;
                else if (dir == Direction.SOUTH) pz = z - offset + j;
                else if (dir == Direction.WEST) px = x + offset - j;
                else if (dir == Direction.EAST) px = x - offset + j;

                Block block = world.getBlockAt(px, curY, pz);
                if (isToIgnoreForPillar(block)) {
                    undo.put(block);
                    putBlock(px, curY, pz, blockData, dir);
                }
            }
        }
    }

    private int getYFirstBlock(int x, int y, int z) {
        int ymin = y - 3;
        if (ymin <= 0) {
            ymin = 1;
        }
        while (y >= ymin && isToIgnore(world.getBlockAt(x, y, z))) {
            y--;
        }
        return y;
    }

    private boolean drawCorner(int x, int y, int z, Direction dir) {

        // same direction, no corner
        if (oldDir == dir) {
            return false;
        }

        RoadPart part = road.getRoadPartToBuild(1);

        if (part == null) {
            return false;
        }

        // check though all the 8 cases
        if (oldDir == Direction.NORTH && dir == Direction.EAST) {

            int newY = y - part.getGroundLayer();
            int height = part.getHeight();
            int jmax = part.getWidth() / 2;

            // browse all heights layers
            for (int i = 0; i < height; i++) {

                String[] blockDataRow = part.getBlockDatas()[i];
                

                for (int j = 0; j <= jmax; j++) {
                    // forward shift
                    for (int a = 0; a <= j; a++) {
                        putBlock(oldX - a, newY, oldZ + j, blockDataRow[jmax - j], oldDir);
                    }
                    // east shift
                    for (int a = 0; a <= jmax + j; a++) {
                        putBlock(oldX - j, newY, oldZ - a + j, blockDataRow[jmax - j], dir);
                    }
                    // backward shift
                    for (int a = j; a < jmax; a++) {
                        putBlock(oldX + j + 1, newY, oldZ - a - 1, blockDataRow[jmax + j + 1], dir);
                    }
                }
                newY++;
            }

            oldZ = oldZ - ((tunnel || bridge) ? jmax - 1 : jmax);
            oldY = y; hasBuilt = true; count++; oldDir = dir;
            return true;

        } else if (oldDir == Direction.NORTH && dir == Direction.WEST) {

            int newY = y - part.getGroundLayer();
            int height = part.getHeight();
            int jmax = part.getWidth() / 2;

            // browse all heights layers
            for (int i = 0; i < height; i++) {

                String[] blockDataRow = part.getBlockDatas()[i];
                

                for (int j = 0; j <= jmax; j++) {
                    // forward shift
                    for (int a = 0; a <= j; a++) {
                        putBlock(oldX - a, newY, oldZ - j, blockDataRow[jmax - j], oldDir);
                    }
                    // west shift
                    for (int a = 0; a <= jmax + j; a++) {
                        putBlock(oldX - j, newY, oldZ + a - j, blockDataRow[jmax - j], dir);
                    }
                    // backward shift
                    for (int a = j; a < jmax; a++) {
                        putBlock(oldX + j + 1, newY, oldZ + a + 1, blockDataRow[jmax + j + 1], dir);
                    }
                }
                newY++;
            }

            oldZ = oldZ + ((tunnel || bridge) ? jmax - 1 : jmax);
            oldY = y; hasBuilt = true; count++; oldDir = dir;
            return true;

        } else if (oldDir == Direction.SOUTH && dir == Direction.EAST) {

            int newY = y - part.getGroundLayer();
            int height = part.getHeight();
            int jmax = part.getWidth() / 2;

            // browse all heights layers
            for (int i = 0; i < height; i++) {

                String[] blockDataRow = part.getBlockDatas()[i];
                

                for (int j = 0; j <= jmax; j++) {
                    // forward shift
                    for (int a = 0; a <= j; a++) {
                        putBlock(oldX + a, newY, oldZ + j, blockDataRow[jmax - j], oldDir);
                    }
                    // east shift
                    for (int a = 0; a <= jmax + j; a++) {
                        putBlock(oldX + j, newY, oldZ - a + j, blockDataRow[jmax - j], dir);
                    }
                    // backward shift
                    for (int a = j; a < jmax; a++) {
                        putBlock(oldX - j - 1, newY, oldZ - a - 1, blockDataRow[jmax + j + 1], dir);
                    }
                }
                newY++;
            }

            oldZ = oldZ - ((tunnel || bridge) ? jmax - 1 : jmax);
            oldY = y; hasBuilt = true; count++; oldDir = dir;
            return true;

        } else if (oldDir == Direction.SOUTH && dir == Direction.WEST) {

            int newY = y - part.getGroundLayer();
            int height = part.getHeight();
            int jmax = part.getWidth() / 2;

            // browse all heights layers
            for (int i = 0; i < height; i++) {

                String[] blockDataRow = part.getBlockDatas()[i];
                

                for (int j = 0; j <= jmax; j++) {
                    // forward shift
                    for (int a = 0; a <= j; a++) {
                        putBlock(oldX + a, newY, oldZ - j, blockDataRow[jmax - j], oldDir);
                    }
                    // west shift
                    for (int a = 0; a <= jmax + j; a++) {
                        putBlock(oldX + j, newY, oldZ + a - j, blockDataRow[jmax - j], dir);
                    }
                    // backward shift
                    for (int a = j; a < jmax; a++) {
                        putBlock(oldX - j - 1, newY, oldZ + a + 1, blockDataRow[jmax + j + 1], dir);
                    }
                }
                newY++;
            }

            oldZ = oldZ + ((tunnel || bridge) ? jmax - 1 : jmax);
            oldY = y; hasBuilt = true; count++; oldDir = dir;
            return true;

        } else if (oldDir == Direction.EAST && dir == Direction.NORTH) {

            int newY = y - part.getGroundLayer();
            int height = part.getHeight();
            int jmax = part.getWidth() / 2;

            // browse all heights layers
            for (int i = 0; i < height; i++) {

                String[] blockDataRow = part.getBlockDatas()[i];
                

                for (int j = 0; j <= jmax; j++) {
                    // forward shift
                    for (int a = 0; a <= j; a++) {
                        putBlock(oldX + j, newY, oldZ - a, blockDataRow[jmax - j], oldDir);
                    }
                    // east shift
                    for (int a = 0; a <= jmax + j; a++) {
                        putBlock(oldX - a + j, newY, oldZ - j, blockDataRow[jmax - j], dir);
                    }
                    // backward shift
                    for (int a = j; a < jmax; a++) {
                        putBlock(oldX - a - 1, newY, oldZ + j + 1, blockDataRow[jmax + j + 1], dir);
                    }
                }
                newY++;
            }

            oldX = oldX - ((tunnel || bridge) ? jmax - 1 : jmax);
            oldY = y; hasBuilt = true; count++; oldDir = dir;
            return true;

        } else if (oldDir == Direction.EAST && dir == Direction.SOUTH) {

            int newY = y - part.getGroundLayer();
            int height = part.getHeight();
            int jmax = part.getWidth() / 2;

            // browse all heights layers
            for (int i = 0; i < height; i++) {

                String[] blockDataRow = part.getBlockDatas()[i];
                

                for (int j = 0; j <= jmax; j++) {
                    // forward shift
                    for (int a = 0; a <= j; a++) {
                        putBlock(oldX - j, newY, oldZ - a, blockDataRow[jmax - j], oldDir);
                    }
                    // east shift
                    for (int a = 0; a <= jmax + j; a++) {
                        putBlock(oldX + a - j, newY, oldZ - j, blockDataRow[jmax - j], dir);
                    }
                    // backward shift
                    for (int a = j; a < jmax; a++) {
                        putBlock(oldX + a + 1, newY, oldZ + j + 1, blockDataRow[jmax + j + 1], dir);
                    }
                }
                newY++;
            }

            oldX = oldX + ((tunnel || bridge) ? jmax - 1 : jmax);
            oldY = y; hasBuilt = true; count++; oldDir = dir;
            return true;

        } else if (oldDir == Direction.WEST && dir == Direction.NORTH) {

            int newY = y - part.getGroundLayer();
            int height = part.getHeight();
            int jmax = part.getWidth() / 2;

            // browse all heights layers
            for (int i = 0; i < height; i++) {

                String[] blockDataRow = part.getBlockDatas()[i];
                

                for (int j = 0; j <= jmax; j++) {
                    // forward shift
                    for (int a = 0; a <= j; a++) {
                        putBlock(oldX + j, newY, oldZ + a, blockDataRow[jmax - j], oldDir);
                    }
                    // east shift
                    for (int a = 0; a <= jmax + j; a++) {
                        putBlock(oldX - a + j, newY, oldZ + j, blockDataRow[jmax - j], dir);
                    }
                    // backward shift
                    for (int a = j; a < jmax; a++) {
                        putBlock(oldX - a - 1, newY, oldZ - j - 1, blockDataRow[jmax + j + 1], dir);
                    }
                }
                newY++;
            }

            oldX = oldX - ((tunnel || bridge) ? jmax - 1 : jmax);
            oldY = y; hasBuilt = true; count++; oldDir = dir;
            return true;

        } else if (oldDir == Direction.WEST && dir == Direction.SOUTH) {

            int newY = y - part.getGroundLayer();
            int height = part.getHeight();
            int jmax = part.getWidth() / 2;

            // browse all heights layers
            for (int i = 0; i < height; i++) {

                String[] blockDataRow = part.getBlockDatas()[i];
                

                for (int j = 0; j <= jmax; j++) {
                    // forward shift
                    for (int a = 0; a <= j; a++) {
                        putBlock(oldX - j, newY, oldZ + a, blockDataRow[jmax - j], oldDir);
                    }
                    // east shift
                    for (int a = 0; a <= jmax + j; a++) {
                        putBlock(oldX + a - j, newY, oldZ + j, blockDataRow[jmax - j], dir);
                    }
                    // backward shift
                    for (int a = j; a < jmax; a++) {
                        putBlock(oldX + a + 1, newY, oldZ - j - 1, blockDataRow[jmax + j + 1], dir);
                    }
                }
                newY++;
            }

            oldX = oldX + ((tunnel || bridge) ? jmax - 1 : jmax);
            oldY = y; hasBuilt = true; count++; oldDir = dir;
            return true;

        }

        return false;
    }


    private void clearTunnel(int startX, int startY, int startZ, int width, int height, boolean isXAxis) {
        int clearHeight = Math.max(4, height);
        for (int h = 0; h <= clearHeight; h++) {
            for (int w = -width/2; w <= width/2; w++) {
                int px = startX;
                int pz = startZ;
                if (isXAxis) {
                    pz += w;
                } else {
                    px += w;
                }
                Block b = world.getBlockAt(px, startY + h, pz);
                if (!b.getType().isAir()) {
                    undo.put(b);
                    
                    // Mimic block damage for anti-xray / Orebfuscator updates
                    org.bukkit.event.block.BlockDamageEvent event = new org.bukkit.event.block.BlockDamageEvent(
                        player, b, player.getInventory().getItemInMainHand(), true
                    );
                    org.bukkit.Bukkit.getPluginManager().callEvent(event);
                    
                    LazyMiner miner = plugin.getLazyMiner(player.getName());
                    if (miner != null && miner.enabled() && player.hasPermission("lazyroad.lazyminer")) {
                        miner.SaveBlock(b);
                    }
                    b.setType(org.bukkit.Material.AIR);
                }
            }
        }
    }
    
    private void drawNorth(int x, int y, int z, boolean tunnel) {
        /**
         * DRAWING ROAD MAIN PART
         */
        RoadPart part = road.getRoadPartToBuild(count);

        if (part == null) {
            return;
        }
        int newY;
        int groundLayer = part.getGroundLayer();
        // new coords
        int newX = (tunnel || bridge) ? x - 1 : x;
        if (y - oldY > 0) {
            newY = y - part.getGroundLayer() - 1;
        } else {
            newY = y - part.getGroundLayer();
        }
        int newZ = z;
        // information about the array of informations
        int height = part.getHeight();
        int width = part.getWidth();
        if (tunnel) {
            clearTunnel(newX, newY, newZ, width, height, true);
        }


        String[][] blockDatas = part.getBlockDatas();

        int offset = part.getPlayerOffset();
        for (int i = 0; i < height; i++) {

            // go to the left
            newZ = z + offset;

            for (int j = 0; j < width; j++) {

                // the block to place
                String blockData = blockDatas[i][j];

                putBlock(newX, newY, newZ, blockData, Direction.NORTH);

                newZ--;
            }
            newY++;
        }


        /**
         * DRAWING STAIRS
         */
        if (hasBuilt && y - oldY != 0) {

            RoadPart stairs = road.getStairs();

            newX = (tunnel || bridge) ? x - 1 : x;
            newY = ((y - oldY) > 0 ? y : y + 1) - stairs.getGroundLayer();
            newZ = z;

            height = stairs.getHeight();
            width = stairs.getWidth();

            blockDatas = stairs.getBlockDatas();
            

            int stairOffset = stairs.getPlayerOffset();
            for (int i = 0; i < height; i++) {

                // go to the left
                newZ = z + stairOffset;

                for (int j = 0; j < width; j++) {

                    // the block to place
                    String blockData = blockDatas[i][j];
                    if (y - oldY > 0) {
                        putBlock(newX, newY, newZ, blockData, Direction.SOUTH);
                    } else {
                        putBlock(newX, newY, newZ, blockData, Direction.NORTH);
                    }

                    newZ--;
                }
                newY++;
            }

            lastBuiltStairs = count;
        }


        /**
         * DRAWING PILLARS
         */
        if (pillar != null) {
            PillarPart pillarPart = pillar.getRoadPartToBuild(count);
            if (pillarPart != null) {
                drawPillar(pillarPart, x, z, y - groundLayer - 1, Direction.NORTH, tunnel, bridge);
            }
        }
    }

    private void drawSouth(int x, int y, int z, boolean tunnel) {
        /**
         * DRAWING ROAD MAIN PART
         */
        RoadPart part = road.getRoadPartToBuild(count);

        if (part == null) {
            return;
        }

        int groundLayer = part.getGroundLayer();
        // new coords
        int newX = (tunnel || bridge) ? x + 1 : x;
        int newY;
        if (y - oldY > 0) {
            newY = y - part.getGroundLayer() - 1;
        } else {
            newY = y - part.getGroundLayer();
        }
        int newZ = z;
        // information about the array of informations
        int height = part.getHeight();
        int width = part.getWidth();
        if (tunnel) {
            clearTunnel(newX, newY, newZ, width, height, true);
        }


        String[][] blockDatas = part.getBlockDatas();

        int offset = part.getPlayerOffset();
        for (int i = 0; i < height; i++) {

            // go to the left
            newZ = z - offset;

            for (int j = 0; j < width; j++) {

                // the block to place
                String blockData = blockDatas[i][j];

                putBlock(newX, newY, newZ, blockData, Direction.SOUTH);

                newZ++;
            }
            newY++;
        }


        /**
         * DRAWING STAIRS
         */
        if (hasBuilt && y - oldY != 0) {

            RoadPart stairs = road.getStairs();

            newX = (tunnel || bridge) ? x + 1 : x;
            newY = ((y - oldY) > 0 ? y : y + 1) - stairs.getGroundLayer();
            newZ = z;

            height = stairs.getHeight();
            width = stairs.getWidth();

            blockDatas = stairs.getBlockDatas();
            

            int stairOffset = stairs.getPlayerOffset();
            for (int i = 0; i < height; i++) {

                // go to the left
                newZ = z - stairOffset;

                for (int j = 0; j < width; j++) {

                    // the block to place
                    String blockData = blockDatas[i][j];

                    if (y - oldY > 0) {
                        putBlock(newX, newY, newZ, blockData, Direction.NORTH);
                    } else {
                        putBlock(newX, newY, newZ, blockData, Direction.SOUTH);
                    }

                    newZ++;
                }
                newY++;
            }

            lastBuiltStairs = count;
        }


        /**
         * DRAWING PILLARS
         */
        if (pillar != null) {
            PillarPart pillarPart = pillar.getRoadPartToBuild(count);
            if (pillarPart != null) {
                drawPillar(pillarPart, x, z, y - groundLayer - 1, Direction.SOUTH, tunnel, bridge);
            }
        }
    }

    private void drawWest(int x, int y, int z, boolean tunnel) {
        /**
         * DRAWING ROAD MAIN PART
         */
        RoadPart part = road.getRoadPartToBuild(count);

        if (part == null) {
            return;
        }

        int groundLayer = part.getGroundLayer();
        // new coords
        int newX = x;
        int newY;
        if (y - oldY > 0) {
            newY = y - part.getGroundLayer() - 1;
        } else {
            newY = y - part.getGroundLayer();
        }
        int newZ = (tunnel || bridge) ? z + 1 : z;
        // information about the array of informations
        int height = part.getHeight();
        int width = part.getWidth();
        if (tunnel) {
            clearTunnel(newX, newY, newZ, width, height, false);
        }


        String[][] blockDatas = part.getBlockDatas();

        int offset = part.getPlayerOffset();
        for (int i = 0; i < height; i++) {

            // go to the left
            newX = x + offset;

            for (int j = 0; j < width; j++) {

                // the block to place
                String blockData = blockDatas[i][j];

                putBlock(newX, newY, newZ, blockData, Direction.WEST);

                newX--;
            }
            newY++;
        }


        /**
         * DRAWING STAIRS
         */
        if (hasBuilt && y - oldY != 0) {

            RoadPart stairs = road.getStairs();

            newX = x;
            newY = ((y - oldY) > 0 ? y : y + 1) - stairs.getGroundLayer();
            newZ = (tunnel || bridge) ? z + 1 : z;

            height = stairs.getHeight();
            width = stairs.getWidth();

            blockDatas = stairs.getBlockDatas();
            

            int stairOffset = stairs.getPlayerOffset();
            for (int i = 0; i < height; i++) {

                // go to the left
                newX = x + stairOffset;

                for (int j = 0; j < width; j++) {

                    // the block to place
                    String blockData = blockDatas[i][j];

                    if (y - oldY > 0) {
                        putBlock(newX, newY, newZ, blockData, Direction.EAST);
                    } else {
                        putBlock(newX, newY, newZ, blockData, Direction.WEST);
                    }

                    newX--;
                }
                newY++;
            }

            lastBuiltStairs = count;
        }


        /**
         * DRAWING PILLARS
         */
        if (pillar != null) {
            PillarPart pillarPart = pillar.getRoadPartToBuild(count);
            if (pillarPart != null) {
                drawPillar(pillarPart, x, z, y - groundLayer - 1, Direction.WEST, tunnel, bridge);
            }
        }
    }

    private void drawEast(int x, int y, int z, boolean tunnel) {
        /**
         * DRAWING ROAD MAIN PART
         */
        RoadPart part = road.getRoadPartToBuild(count);

        if (part == null) {
            return;
        }

        int groundLayer = part.getGroundLayer();
        // new coords
        int newX = x;
        int newY;
        if (y - oldY > 0) {
            newY = y - part.getGroundLayer() - 1;
        } else {
            newY = y - part.getGroundLayer();
        }
        int newZ = (tunnel || bridge) ? z - 1 : z;
        // information about the array of informations
        int height = part.getHeight();
        int width = part.getWidth();
        if (tunnel) {
            clearTunnel(newX, newY, newZ, width, height, false);
        }


        String[][] blockDatas = part.getBlockDatas();

        int offset = part.getPlayerOffset();
        for (int i = 0; i < height; i++) {

            // go to the left
            newX = x - offset;

            for (int j = 0; j < width; j++) {

                // the block to place
                String blockData = blockDatas[i][j];

                putBlock(newX, newY, newZ, blockData, Direction.EAST);

                newX++;
            }
            newY++;
        }


        /**
         * DRAWING STAIRS
         */
        if (hasBuilt && y - oldY != 0) {

            RoadPart stairs = road.getStairs();

            newX = x;
            newY = ((y - oldY) > 0 ? y : y + 1) - stairs.getGroundLayer();
            newZ = (tunnel || bridge) ? z - 1 : z;

            height = stairs.getHeight();
            width = stairs.getWidth();

            blockDatas = stairs.getBlockDatas();
            

            int stairOffset = stairs.getPlayerOffset();
            for (int i = 0; i < height; i++) {

                // go to the left
                newX = x - stairOffset;

                for (int j = 0; j < width; j++) {

                    // the block to place
                    String blockData = blockDatas[i][j];

                    if (y - oldY > 0) {
                        putBlock(newX, newY, newZ, blockData, Direction.WEST);
                    } else {
                        putBlock(newX, newY, newZ, blockData, Direction.EAST);
                    }

                    newX++;
                }
                newY++;
            }

            lastBuiltStairs = count;
        }


        /**
         * DRAWING PILLARS
         */
        if (pillar != null) {
            PillarPart pillarPart = pillar.getRoadPartToBuild(count);
            if (pillarPart != null) {
                drawPillar(pillarPart, x, z, y - groundLayer - 1, Direction.EAST, tunnel, bridge);
            }
        }
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Road getRoad() {
        return road;
    }

    public boolean isBridge() {
        return bridge;
    }

    public void setBridge(boolean bridge) {
        this.bridge = bridge;
    }

    public boolean isTunnel() {
        return tunnel;
    }

    public void setForceDown(boolean forceDown) {
        this.forceDown = forceDown;
    }

    public void setForceUp(boolean forceUp) {
        this.forceUp = forceUp;
    }

    public void setTunnel(boolean tunnel) {
        this.tunnel = tunnel;
    }

    private boolean isToIgnore(Block b) {
        Material type = b.getType();
        return !type.isSolid() && type != Material.WATER && type != Material.LAVA;
    }

    private boolean isToIgnoreForPillar(Block b) {
        Material type = b.getType();
        return !type.isSolid() || type.name().contains("LOG") || type.name().contains("LEAVES");
    }

    public boolean isHasBuilt() {
        return hasBuilt;
    }

    public int getLastBuiltStairs() {
        return lastBuiltStairs;
    }

    public int getOldX() {
        return oldX;
    }

    public int getOldY() {
        return oldY;
    }

    public int getOldZ() {
        return oldZ;
    }

    public Pillar getPillar() {
        return pillar;
    }

    public World getWorld() {
        return world;
    }
}









