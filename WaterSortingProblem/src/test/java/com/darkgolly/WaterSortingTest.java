package com.darkgolly;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WaterSortingTest {

    @Test
    void testSimpleSorting() {
        int[][] initial = {
                {1, 1, 2, 2},
                {2, 2, 1, 1},
                {},
                {}
        };
        int size = 4;

        List<Tube> tubes = TubeConverter.toTubes(initial, size);
        List<Move> moves = WaterSorting.sort(tubes, size);

        assertFalse(moves.isEmpty());

        for (Tube tube : tubes) {
            assertTrue(tube.isEmpty() || tube.isSame(),
                    "Все пробирки должны быть пустыми или содержать жидкость одного цвета");
        }
    }

    @Test
    void testAlreadySorted() {
        int[][] initial = {
                {1, 1, 1, 1},
                {2, 2, 2, 2},
                {},
                {}
        };
        int size = 4;

        List<Tube> tubes = TubeConverter.toTubes(initial, size);
        List<Move> moves = WaterSorting.sort(tubes, size);

        assertTrue(moves.isEmpty());
    }

    @Test
    void testMoveRecording() {
        int[][] initial = {
                {1, 2},
                {2, 1},
                {},
                {}
        };
        int size = 2;

        List<Tube> tubes = TubeConverter.toTubes(initial, size);
        List<Move> moves = WaterSorting.sort(tubes, size);

        for (Move move : moves) {
            assertTrue(move.from() >= 0 && move.from() < 4);
            assertTrue(move.to() >= 0 && move.to() < 4);
            assertNotEquals(move.from(), move.to());
        }
    }
}
