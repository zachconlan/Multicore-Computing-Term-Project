import org.junit.Test;
import static org.junit.Assert.*;

public class HousingAllocationTest {

    // 2-cycle
    final int[][] PREF1 = {
            {1, 0},
            {0, 1}
    };
    final int[] SOL1 = {1, 0};

    // Self Loops
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
        HousingAllocation h = new HousingAllocation(PREF1);
        h.solve();
        int[] sol = h.getSolution();
        assertArrayEquals(SOL1, sol);
    }

    @Test
    public void testSelfLoops() {
        HousingAllocation h = new HousingAllocation(PREF2);
        h.solve();
        int[] sol = h.getSolution();
        assertArrayEquals(SOL2, sol);
    }

    @Test
    public void testThreeCycle() {
        HousingAllocation h = new HousingAllocation(PREF3);
        h.solve();
        int[] sol = h.getSolution();
        assertArrayEquals(SOL3, sol);
    }

    @Test
    public void testCycleWithAdvance() {
        HousingAllocation h = new HousingAllocation(PREF4);
        h.solve();
        int[] sol = h.getSolution();
        assertArrayEquals(SOL4, sol);
    }
}