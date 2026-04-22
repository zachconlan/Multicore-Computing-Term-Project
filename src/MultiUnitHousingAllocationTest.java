import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

public class MultiUnitHousingAllocationTest {

    private static List<List<Integer>>[] tiedPrefs(int[][][] rawPrefs) {
        @SuppressWarnings("unchecked")
        List<List<Integer>>[] tiedPrefs = new List[rawPrefs.length];

        for (int agent = 0; agent < rawPrefs.length; agent++) {
            List<List<Integer>> tiers = new ArrayList<>();
            for (int[] rawTier : rawPrefs[agent]) {
                List<Integer> tier = new ArrayList<>();
                for (int houseType : rawTier) {
                    tier.add(houseType);
                }
                tiers.add(tier);
            }
            tiedPrefs[agent] = tiers;
        }

        return tiedPrefs;
    }

    @Test
    public void testMultiUnitThreeAgentsTwoOfSameType() {
        int[] capacity = {2, 1};
        List<List<Integer>>[] pref = tiedPrefs(new int[][][] {
                {{1}, {0}},
                {{0}, {1}},
                {{0}, {1}}
        });

        MultiUnitHousingAllocation h = new MultiUnitHousingAllocation(capacity, 3, pref);
        h.solve();

        assertArrayEquals(new int[] {1, 0, 0}, h.getSolution());
    }

    @Test
    public void testMultiUnitFourAgentsBalancedSwap() {
        int[] capacity = {2, 2};
        List<List<Integer>>[] pref = tiedPrefs(new int[][][] {
                {{1}, {0}},
                {{1}, {0}},
                {{0}, {1}},
                {{0}, {1}}
        });

        MultiUnitHousingAllocation h = new MultiUnitHousingAllocation(capacity, 4, pref);
        h.solve();

        assertArrayEquals(new int[] {1, 1, 0, 0}, h.getSolution());
    }

    @Test
    public void testExpandPreferencesCopiesAllUnitsIntoSameTier() {
        int[] capacity = {2, 1};
        List<List<Integer>>[] pref = tiedPrefs(new int[][][] {
                {{0, 1}},
                {{0}, {1}},
                {{1}, {0}}
        });

        MultiUnitHousingAllocation h = new MultiUnitHousingAllocation(capacity, 3, pref);
        List<List<Integer>>[] expanded = h.expandPreferences(pref);

        assertEquals(1, expanded[0].size());
        assertEquals(listOf(0, 1, 2), expanded[0].get(0));
    }

    @Test
    public void testMultiUnitTieOverHouseTypesResolvesDeterministically() {
        int[] capacity = {2, 1};
        List<List<Integer>>[] pref = tiedPrefs(new int[][][] {
                {{1, 0}},
                {{0}, {1}},
                {{1}, {0}}
        });

        MultiUnitHousingAllocation h = new MultiUnitHousingAllocation(capacity, 3, pref);
        h.solve();

        assertArrayEquals(new int[] {0, 0, 1}, h.getSolution());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsCapacityMismatch() {
        int[] capacity = {2, 1};
        List<List<Integer>>[] pref = tiedPrefs(new int[][][] {
                {{1}, {0}},
                {{0}, {1}}
        });

        new MultiUnitHousingAllocation(capacity, 2, pref);
    }

    private static List<Integer> listOf(int... values) {
        List<Integer> list = new ArrayList<>();
        for (int value : values) {
            list.add(value);
        }
        return list;
    }
}
