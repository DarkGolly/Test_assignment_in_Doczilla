package com.darkgolly;

import java.util.*;

public class WaterSorting {
    public static List<Move> sort(List<Tube> tubes, int size) {
        List<Move> moves = new ArrayList<>();
        boolean finished = false;
        Set<Tube> wrongMin = new HashSet<>();

        Tube emptyTube = null;
        Tube firstMixedTube = null;
        Tube min = null;
        while (!finished) {
            finished = true;
            for (Tube tube : tubes) {
                if ((!tube.isEmpty() && tube.getLiquids().size() < size) || (tube.getLiquids().size() == size && !tube.isClosed())) {
                    finished = false;
                } else if (tube.isClosed()) {
                    continue;
                }
                if (firstMixedTube == null && !tube.isSame()) {
                    firstMixedTube = tube;
                }
                if (emptyTube == null && tube.isEmpty()) {
                    emptyTube = tube;
                }
                if (!tube.isEmpty() && !wrongMin.contains(tube) && min != null && !tube.isEmpty() && tube.getLiquids().size() <= min.getLiquids().size()) {
                    min = tube;
                } else if (!tube.isEmpty() && !wrongMin.contains(tube) && min == null) {
                    min = tube;
                }
            }
            if (emptyTube != null && firstMixedTube != null) {
                transfusion(firstMixedTube, emptyTube, moves);
            }else if (min != null) {
                for (Tube tube : tubes) {
                    if (!tube.isEmpty() && !min.equals(tube) && min.getLastLiquid() == tube.getLastLiquid()) {
                        transfusion(tube, min, moves);
                        wrongMin.clear();
                        min = null;
                        break;
                    }
                }
                if (min != null) {
                    wrongMin.add(min);
                }
            }

            emptyTube = null;
            firstMixedTube = null;
            min = null;
        }

        return moves;
    }
    private static void transfusion(Tube from, Tube to, List<Move> moves) {
        List<Integer> poured = from.pollLiquid();

        int freeSpace = to.getSize() - to.getLiquids().size();
        int canPour = Math.min(freeSpace, poured.size());

        for (int i = 0; i < canPour; i++) {
            to.getLiquids().add(poured.get(i));
        }

        for (int i = poured.size() - 1; i >= canPour; i--) {
            from.getLiquids().add(poured.get(i));
        }

        moves.add(new Move(from.getIndex(), to.getIndex()));
    }

}
