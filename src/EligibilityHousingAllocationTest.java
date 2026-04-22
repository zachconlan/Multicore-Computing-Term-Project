import org.junit.Test;
import static org.junit.Assert.*;

public class EligibilityHousingAllocationTest {

    @Test
    public void testNoRestrictionsBehavesLikeOriginalTwoCycle() {
        int[][] pref = {
                {1, 0},
                {0, 1}
        };

        boolean[][] eligible = allEligible(2);

        EligibilityHousingAllocation h = new EligibilityHousingAllocation(pref, eligible);
        h.solve();

        assertArrayEquals(new int[] {1, 0}, h.getSolution());
        assertSolutionRespectsEligibility(h, h.getSolution());
    }

    @Test
    public void testSkipsIneligibleFirstChoiceDuringInitialization() {
        int[][] pref = {
                {2, 1, 0},  // house 2 is not eligible, so agent 0 starts at house 1
                {0, 1, 2},
                {2, 0, 1}
        };

        boolean[][] eligible = {
                {true,  true,  false},
                {true,  true,  true },
                {true,  true,  true }
        };

        EligibilityHousingAllocation h = new EligibilityHousingAllocation(pref, eligible);
        h.solve();

        assertArrayEquals(new int[] {1, 0, 2}, h.getSolution());
        assertSolutionRespectsEligibility(h, h.getSolution());
    }

    @Test
    public void testAdvanceSkipsIneligibleMiddleChoice() {
        int[][] pref = {
                {0, 1, 2},
                {0, 2, 1},  // agent 1 first points to 0; house 2 is ineligible; then skips to 1
                {2, 0, 1}
        };

        boolean[][] eligible = {
                {true,  true,  true },
                {true,  true,  false},
                {true,  true,  true }
        };

        EligibilityHousingAllocation h = new EligibilityHousingAllocation(pref, eligible);
        h.solve();

        assertArrayEquals(new int[] {0, 1, 2}, h.getSolution());
        assertSolutionRespectsEligibility(h, h.getSolution());
    }

    @Test
    public void testEligibilityCanForceLowerRankedButValidAssignment() {
        int[][] pref = {
                {1, 0, 2},
                {0, 1, 2},
                {1, 2, 0}
        };

        boolean[][] eligible = {
                {true,  false, true }, // agent 0 cannot receive top choice 1
                {true,  true,  true },
                {true,  true,  true }
        };

        EligibilityHousingAllocation h = new EligibilityHousingAllocation(pref, eligible);
        h.solve();

        assertArrayEquals(new int[] {0, 1, 2}, h.getSolution());
        assertSolutionRespectsEligibility(h, h.getSolution());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsAgentWithNoEligibleHouses() {
        int[][] pref = {
                {0, 1},
                {1, 0}
        };

        boolean[][] eligible = {
                {true,  true },
                {false, false}
        };

        new EligibilityHousingAllocation(pref, eligible);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsDuplicateHouseInPreferences() {
        int[][] pref = {
                {0, 0},
                {1, 0}
        };

        boolean[][] eligible = allEligible(2);

        new EligibilityHousingAllocation(pref, eligible);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsInvalidHouseIdInPreferences() {
        int[][] pref = {
                {0, 2},
                {1, 0}
        };

        boolean[][] eligible = allEligible(2);

        new EligibilityHousingAllocation(pref, eligible);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsWrongEligibilityRowLength() {
        int[][] pref = {
                {0, 1},
                {1, 0}
        };

        boolean[][] eligible = {
                {true, true},
                {true}
        };

        new EligibilityHousingAllocation(pref, eligible);
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowsWhenEligibilityMakesInstanceInfeasibleDuringSolve() {
        int[][] pref = {
                {0, 1},
                {0, 1}
        };

        boolean[][] eligible = {
                {true,  false},
                {true,  false}
        };

        EligibilityHousingAllocation h = new EligibilityHousingAllocation(pref, eligible);
        h.solve();
    }

    private static boolean[][] allEligible(int n) {
        boolean[][] eligible = new boolean[n][n];

        for (int agent = 0; agent < n; agent++) {
            for (int house = 0; house < n; house++) {
                eligible[agent][house] = true;
            }
        }

        return eligible;
    }

    private static void assertSolutionRespectsEligibility(
            EligibilityHousingAllocation h,
            int[] solution
    ) {
        boolean[] used = new boolean[solution.length];

        for (int agent = 0; agent < solution.length; agent++) {
            int house = solution[agent];

            assertTrue(
                    "Agent " + agent + " is not eligible for house " + house,
                    h.isEligible(agent, house)
            );

            assertFalse(
                    "House " + house + " was assigned more than once.",
                    used[house]
            );

            used[house] = true;
        }
    }
}
