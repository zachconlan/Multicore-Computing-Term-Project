package org.example;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class LLP_TTCTest {

    // 2-cycle
    final int[][] PREF1 = {
            {1, 0},
            {0, 1}
    };
    final int[] SOL1 = {1, 0};

    // Self loops
    final int[][] PREF2 = {
            {0, 1, 2},
            {1, 0, 2},
            {2, 0, 1}
    };
    final int[] SOL2 = {0, 1, 2};

    // 3-cycle
    final int[][] PREF3 = {
            {1, 2, 0},
            {2, 0, 1},
            {0, 1, 2}
    };
    final int[] SOL3 = {1, 2, 0};

    // Cycle with advance
    final int[][] PREF4 = {
            {1, 0, 2},
            {0, 1, 2},
            {1, 2, 0}
    };
    final int[] SOL4 = {1, 0, 2};

    @Test
    public void testTwoCycle() {
        LLP_TTC h = new LLP_TTC(PREF1);
        h.solve();
        int[] sol = h.getSolution();
        assertArrayEquals(SOL1, sol);
    }

    @Test
    public void testSelfLoops() {
        LLP_TTC h = new LLP_TTC(PREF2);
        h.solve();
        int[] sol = h.getSolution();
        assertArrayEquals(SOL2, sol);
    }

    @Test
    public void testThreeCycle() {
        LLP_TTC h = new LLP_TTC(PREF3);
        h.solve();
        int[] sol = h.getSolution();
        assertArrayEquals(SOL3, sol);
    }

    @Test
    public void testCycleWithAdvance() {
        LLP_TTC h = new LLP_TTC(PREF4);
        h.solve();
        int[] sol = h.getSolution();
        assertArrayEquals(SOL4, sol);
    }
}
