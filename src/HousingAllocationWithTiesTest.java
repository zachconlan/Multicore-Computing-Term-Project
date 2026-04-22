import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

public class HousingAllocationWithTiesTest {

    // 2-cycle
    final List<List<Integer>>[] PREF1 = tiedPrefs(new int[][][] {
            {{1}, {0}},
            {{0}, {1}}
    });
    final int[] SOL1 = {1, 0};

    // Self Loops
    final List<List<Integer>>[] PREF2 = tiedPrefs(new int[][][] {
            {{0}, {1}, {2}},
            {{1}, {0}, {2}},
            {{2}, {0}, {1}}
    });
    final int[] SOL2 = {0, 1, 2};

    // 3-cycle
    final List<List<Integer>>[] PREF3 = tiedPrefs(new int[][][] {
            {{1}, {2}, {0}},
            {{2}, {0}, {1}},
            {{0}, {1}, {2}}
    });
    final int[] SOL3 = {1, 2, 0};

    // Cycle with advance
    final List<List<Integer>>[] PREF4 = tiedPrefs(new int[][][] {
            {{1}, {0}, {2}},
            {{0}, {1}, {2}},
            {{1}, {2}, {0}}
    });
    final int[] SOL4 = {1, 0, 2};

    // Tie resolution: agent 0 is indifferent between houses 2 and 1.
    // The tie is intentionally given as {2, 1} so the test verifies that
    // normalization sorts the tier in ascending order, yielding 1 before 2.
    final List<List<Integer>>[] PREF5 = tiedPrefs(new int[][][] {
            {{2, 1}, {0}},
            {{0}, {1}, {2}},
            {{1}, {2}, {0}}
    });
    final int[] SOL5 = {1, 0, 2};

    private static List<List<Integer>>[] tiedPrefs(int[][][] rawPrefs) {
        @SuppressWarnings("unchecked")
        List<List<Integer>>[] tiedPrefs = new List[rawPrefs.length];

        for (int agent = 0; agent < rawPrefs.length; agent++) {
            List<List<Integer>> tiers = new ArrayList<>();
            for (int[] rawTier : rawPrefs[agent]) {
                List<Integer> tier = new ArrayList<>();
                for (int house : rawTier) {
                    tier.add(house);
                }
                tiers.add(tier);
            }
            tiedPrefs[agent] = tiers;
        }

        return tiedPrefs;
    }

    @Test
    public void testTwoCycle() {
        HousingAllocationWithTies h = new HousingAllocationWithTies(PREF1);
        h.solve();
        int[] sol = h.getSolution();
        assertArrayEquals(SOL1, sol);
    }

    @Test
    public void testSelfLoops() {
        HousingAllocationWithTies h = new HousingAllocationWithTies(PREF2);
        h.solve();
        int[] sol = h.getSolution();
        assertArrayEquals(SOL2, sol);
    }

    @Test
    public void testThreeCycle() {
        HousingAllocationWithTies h = new HousingAllocationWithTies(PREF3);
        h.solve();
        int[] sol = h.getSolution();
        assertArrayEquals(SOL3, sol);
    }

    @Test
    public void testCycleWithAdvance() {
        HousingAllocationWithTies h = new HousingAllocationWithTies(PREF4);
        h.solve();
        int[] sol = h.getSolution();
        assertArrayEquals(SOL4, sol);
    }

    @Test
    public void testTieResolutionUsesAscendingOrderWithinTier() {
        HousingAllocationWithTies h = new HousingAllocationWithTies(PREF5);
        h.solve();
        int[] sol = h.getSolution();
        assertArrayEquals(SOL5, sol);
    }
}
