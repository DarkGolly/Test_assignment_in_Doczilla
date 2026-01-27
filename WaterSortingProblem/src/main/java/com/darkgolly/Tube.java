package com.darkgolly;

import java.util.ArrayList;
import java.util.List;

public class Tube {
    public int getSize() {
        return size;
    }

    final private int size;
    private final int index;

    public boolean isClosed() {
        return isClosedTube();
    }

    private final List<Integer> liquids;

    public Tube(List<Integer> liquids, int size, int index) {
        this.liquids = liquids;
        this.size = size;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public List<Integer> getLiquids() {
        return liquids;
    }

    public boolean isEmpty() {
        return liquids.isEmpty();
    }

    public int getLastLiquid(){
        return liquids.get(liquids.size()-1);
    }

    public boolean isSame() {
        return isLiquidsSame();
    }
    private boolean isLiquidsSame(){
        if (liquids.size() < 2) return true;
        for (int i = 0; i < liquids.size() - 1; i++) {
            if (!liquids.get(i).equals(liquids.get(i+1))) {
                return false;
            }
        }
        return true;
    }
    public List<Integer> pollLiquid() {
        List<Integer> result = new ArrayList<>();

        if (liquids.isEmpty()) {
            return result;
        }

        int topColor = liquids.get(liquids.size() - 1);

        while (!liquids.isEmpty() &&
                liquids.get(liquids.size() - 1) == topColor) {
            result.add(liquids.remove(liquids.size() - 1));
        }

        return result;
    }

    private boolean isClosedTube(){
        if (liquids.size() < size) return false;
        for (int i = 0; i < liquids.size() - 1; i++) {
            if (!liquids.get(i).equals(liquids.get(i+1))) {
                return false;
            }
        }
        return true;
    }
}

