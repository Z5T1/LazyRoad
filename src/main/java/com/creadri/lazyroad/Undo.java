package com.creadri.lazyroad;

import org.bukkit.block.Block;
import java.io.Serializable;
import org.bukkit.Bukkit;
import org.bukkit.World;
import java.util.ArrayList;

public class Undo implements Serializable {
    private ArrayList<String> blockDatas;
    private ArrayList<Integer> xs;
    private ArrayList<Integer> ys;
    private ArrayList<Integer> zs;
    private String worldString;
    private transient World world;

    public Undo(World world) {
        this.world = world;
        worldString = world.getName();
        blockDatas = new ArrayList<>();
        xs = new ArrayList<>();
        ys = new ArrayList<>();
        zs = new ArrayList<>();
    }

    public void put(Block b) {
        blockDatas.add(b.getBlockData().getAsString());
        xs.add(b.getX());
        ys.add(b.getY());
        zs.add(b.getZ());
    }

    public void undo() {
        for (int i = blockDatas.size() - 1; i >= 0; i--) {
            Block b = world.getBlockAt(xs.get(i), ys.get(i), zs.get(i));
            if (blockDatas.get(i) != null) {
                b.setBlockData(Bukkit.getServer().createBlockData(blockDatas.get(i)), false);
            }
        }
        blockDatas.clear();
        xs.clear();
        ys.clear();
        zs.clear();
    }

    public String getWorldString() {
        return worldString;
    }

    public void setWorld(World world) {
        this.world = world;
    }
}