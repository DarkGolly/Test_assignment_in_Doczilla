package com.darkgolly;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        int[][] initial = {
                {4, 3, 1},
                {2, 1, 4},
                {1, 2, 3},
                {4, 3, 2},
                {},
                {}
        };
        int size = 3;

        System.out.println("Исходное состояние:");
        printState(initial);
        System.out.println();

        List<Tube> tubes = TubeConverter.toTubes(initial, size);
        List<Move> solution = WaterSorting.sort(tubes, size);

        System.out.println("Результат сортировки:");
        printTubes(tubes);
        System.out.println();

        System.out.println("Количество ходов: " + solution.size());
        System.out.println("\nПоследовательность ходов:");
        for (int i = 0; i < solution.size(); i++) {
            System.out.println((i + 1) + ". " + solution.get(i));
        }
    }

    static void printTubes(List<Tube> tubes) {
        for (Tube tube : tubes) {
            System.out.print("Пробирка " + tube.getIndex() + ": [");
            List<Integer> liquids = tube.getLiquids();
            for (int j = 0; j < liquids.size(); j++) {
                System.out.print(liquids.get(j));
                if (j < liquids.size() - 1) System.out.print(", ");
            }
            System.out.println("]");
        }
    }

    static void printState(int[][] state) {
        for (int i = 0; i < state.length; i++) {
            System.out.print("Пробирка " + i + ": [");
            for (int j = 0; j < state[i].length; j++) {
                System.out.print(state[i][j]);
                if (j < state[i].length - 1) System.out.print(", ");
            }
            System.out.println("]");
        }
    }
}
