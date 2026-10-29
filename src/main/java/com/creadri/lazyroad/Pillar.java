package com.creadri.lazyroad;

import java.util.ArrayList;

public class Pillar {

    private ArrayList<PillarPart> parts;
    private int maxSequence;

    public Pillar() {
        this.parts = new ArrayList<>();
    }

    public Pillar(int partsSize) {
        this.parts = new ArrayList<>(partsSize);
    }

    public int size() {
        return parts.size();
    }

    public PillarPart getPillarPart(int index) {
        return parts.get(index);
    }

    public void setPillarPart(int index, PillarPart part) {
        parts.set(index, part);
    }

    public boolean addPillarPart(PillarPart part) {
        parts.add(part);
        return true;
    }

    public void removePillarPart(PillarPart part) {
        parts.remove(part);
    }

    public int getMaxSequence() {
        return maxSequence;
    }

    public void setMaxSequence(int maxSequence) {
        this.maxSequence = maxSequence;
    }

    public ArrayList<PillarPart> getParts() {
        return parts;
    }

    public void setParts(ArrayList<PillarPart> parts) {
        this.parts = parts;
    }

    public int getStartIndex() {
        if (parts.isEmpty()) return 0;

        PillarPart targetPart = null;
        for (PillarPart p : parts) {
            if (p.isStartHere()) {
                targetPart = p;
                break;
            }
        }

        if (targetPart == null) {
            return 0;
        }

        int seq = maxSequence > 0 ? maxSequence : 1;

        // If targetPart defines trigger positions, solve for offset where targetPart triggers at count 0
        if (targetPart.getTriggerPositions() != null && !targetPart.getTriggerPositions().isEmpty()) {
            int targetStep = targetPart.getTriggerPositions().get(0); // 1-indexed
            return (targetStep - 1 + seq) % seq;
        }

        // If 1:1 part-per-step sequence (parts.size() == maxSequence)
        if (parts.size() == maxSequence && maxSequence > 0) {
            return parts.indexOf(targetPart);
        }

        // Modulo solver
        for (int offset = 0; offset < seq; offset++) {
            for (PillarPart p : parts) {
                int re = p.getRepeatEvery();
                if (re > 0 && (offset % re) == 0) {
                    if (p == targetPart) {
                        return offset;
                    }
                    break;
                }
            }
        }

        return parts.indexOf(targetPart);
    }

    public PillarPart getRoadPartToBuild(int count) {
        if (parts.isEmpty()) return null;
        int seq = maxSequence > 0 ? maxSequence : 1;
        int startOffset = getStartIndex();

        // Check if any part uses triggerPositions
        boolean hasTriggerPositions = false;
        for (PillarPart p : parts) {
            if (p.getTriggerPositions() != null && !p.getTriggerPositions().isEmpty()) {
                hasTriggerPositions = true;
                break;
            }
        }

        // 1. If template uses triggerPositions, evaluate steps in [1, seq]
        // Count 0 is step 1, count 1 is step 2, etc. (unless shifted by startHere).
        // Only steps with matching triggerPositions build; all other steps return null.
        if (hasTriggerPositions) {
            int step = ((count + startOffset) % seq) + 1; // 1-indexed step in [1, seq]
            for (PillarPart part : parts) {
                java.util.List<Integer> triggers = part.getTriggerPositions();
                if (triggers != null && triggers.contains(step)) {
                    return part;
                }
            }
            return null;
        }

        // 2. If template defines a 1:1 part-per-step sequence (like BigBridge with 22 parts and maxSequence 22)
        if (parts.size() == maxSequence && maxSequence > 0) {
            int index = (count + startOffset) % maxSequence;
            return parts.get(index);
        }

        // 3. Interval frequency repeatEvery evaluation:
        // Do not force build at count=0 unless count > 0 or a part explicitly repeats every 1 or has startHere
        int effectiveCount = count + startOffset;
        for (PillarPart part : parts) {
            int re = part.getRepeatEvery();
            if (re > 0) {
                if (count == 0 && startOffset == 0 && re > 1) {
                    // Without startHere, an interval > 1 waits until that interval completes
                    continue;
                }
                if ((effectiveCount % re) == 0) {
                    return part;
                }
            }
        }
        return null;
    }
}