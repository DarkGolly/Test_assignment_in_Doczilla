package com.darkgolly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TubeConverter {

    public static List<Tube> toTubes(int[][] initial, int size) {
        if (initial == null) {
            return Collections.emptyList();
        }

        List<Tube> result = new ArrayList<>(initial.length);

        for (int i = 0; i < initial.length; i++) {
            int[] row = initial[i];

            if (row == null || row.length == 0) {
                result.add(new Tube(new ArrayList<>(), size, i));
                continue;
            }

            List<Integer> liquids = new ArrayList<>(row.length);
            for (int value : row) {
                liquids.add(value);
            }

            result.add(new Tube(liquids, size, i));
        }

        return result;
    }
}
