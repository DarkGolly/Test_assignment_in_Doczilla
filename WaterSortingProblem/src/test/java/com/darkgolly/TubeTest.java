package com.darkgolly;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TubeTest {

    @Test
    void testEmptyTube() {
        Tube tube = new Tube(new ArrayList<>(), 4, 0);
        assertTrue(tube.isEmpty());
        assertTrue(tube.isSame());
        assertFalse(tube.isClosed());
    }

    @Test
    void testTubeWithSameLiquids() {
        List<Integer> liquids = new ArrayList<>(Arrays.asList(1, 1, 1, 1));
        Tube tube = new Tube(liquids, 4, 0);
        
        assertFalse(tube.isEmpty());
        assertTrue(tube.isSame());
        assertTrue(tube.isClosed());
        assertEquals(1, tube.getLastLiquid());
    }

    @Test
    void testTubeWithMixedLiquids() {
        List<Integer> liquids = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        Tube tube = new Tube(liquids, 4, 0);
        
        assertFalse(tube.isEmpty());
        assertFalse(tube.isSame());
        assertFalse(tube.isClosed());
        assertEquals(4, tube.getLastLiquid());
    }

    @Test
    void testPollLiquid() {
        List<Integer> liquids = new ArrayList<>(Arrays.asList(1, 2, 2, 2));
        Tube tube = new Tube(liquids, 4, 0);
        
        List<Integer> poured = tube.pollLiquid();
        
        assertEquals(3, poured.size());
        assertEquals(1, tube.getLiquids().size());
        assertEquals(1, tube.getLiquids().get(0));
    }

    @Test
    void testGetIndex() {
        Tube tube = new Tube(new ArrayList<>(), 4, 5);
        assertEquals(5, tube.getIndex());
    }
}
