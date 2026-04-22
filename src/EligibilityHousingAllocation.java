/**
 * Housing allocation with an eligibility constraint.
 *
 * Model:
 * - There are n agents and n houses.
 * - Agent i initially owns house i, matching the original HousingAllocation model.
 * - pref[i] is a strict ranking of all houses.
 * - eligible[i][h] is true only if agent i is allowed to receive house h.
 *
 * The algorithm follows the same LLP / top-trading-cycle style logic as HousingAllocation,
 * except an agent's current wish is always the best remaining eligible house in that
 * agent's preference list.
 */
public class EligibilityHousingAllocation extends LLP {
    private final int n;
    private final int[][] pref;
    private final boolean[][] eligible;

    // G[i] is the current index into pref[i].
    private final int[] G;

    // Cached final/current cycle membership, useful for debugging or inspection.
    private boolean[] inSubmatching;

    private static final int UNVISITED = 0;
    private static final int IN_PATH = 1;
    private static final int DONE = 2;

    public EligibilityHousingAllocation(int[][] pref, boolean[][] eligible) {
        super(validateAndGetSize(pref, eligible));

        this.n = pref.length;
        this.pref = copyPreferences(pref);
        this.eligible = copyEligibility(eligible);
        this.G = new int[n];
        this.inSubmatching = new boolean[n];

        initializeToFirstEligibleChoices();
    }

    /**
     * Returns the house currently wished for by agent i.
     */
    int wish(int i) {
        return pref[i][G[i]];
    }

    /**
     * forbidden(j): j is not in S(G), but j currently points into S(G).
     *
     * This method is provided to satisfy the LLP interface. The solve() method below
     * overrides LLP.solve() and computes the submatching only once per round.
     */
    @Override
    public boolean forbidden(int j) {
        boolean[] submatching = computeSubmatching();
        return !submatching[j] && submatching[wish(j)];
    }

    /**
     * Advances agent j to the next eligible house in its preference list.
     */
    @Override
    public void advance(int j) {
        G[j]++;
        skipIneligible(j);

        if (G[j] >= n) {
            throw new IllegalStateException(
                "No eligible house remains for agent " + j +
                ". The eligibility constraints may make the instance infeasible."
            );
        }
    }

    /**
     * Sequential LLP solver.
     *
     * This avoids recomputing the submatching once per agent and avoids the shared-state
     * concurrency issue that can occur if the inherited parallel solve() calls forbidden()
     * for several agents at the same time.
     */
    @Override
    public void solve() {
        int totalAdvances = 0;
        int maxAdvances = n * n;

        while (true) {
            boolean[] submatching = computeSubmatching();
            boolean[] shouldAdvance = new boolean[n];
            boolean foundForbidden = false;

            for (int j = 0; j < n; j++) {
                if (!submatching[j] && submatching[wish(j)]) {
                    shouldAdvance[j] = true;
                    foundForbidden = true;
                }
            }

            if (!foundForbidden) {
                inSubmatching = submatching;
                return;
            }

            for (int j = 0; j < n; j++) {
                if (shouldAdvance[j]) {
                    advance(j);
                    totalAdvances++;

                    if (totalAdvances > maxAdvances) {
                        throw new IllegalStateException(
                            "Solver exceeded the maximum number of advances. " +
                            "The eligibility constraints may make the instance infeasible."
                        );
                    }
                }
            }
        }
    }

    /**
     * Returns the final assignment. Call solve() before this method.
     */
    public int[] getSolution() {
        int[] solution = new int[n];

        for (int i = 0; i < n; i++) {
            solution[i] = wish(i);
        }

        return solution;
    }

    /**
     * Returns true if agent i is eligible for house h.
     */
    public boolean isEligible(int i, int h) {
        return eligible[i][h];
    }

    /**
     * Computes S(G): the set of agents that are part of cycles in the graph
     * agent i -> wish(i).
     */
    private boolean[] computeSubmatching() {
        boolean[] submatching = new boolean[n];

        int[] state = new int[n];
        int[] position = new int[n];

        for (int i = 0; i < n; i++) {
            position[i] = -1;
        }

        for (int start = 0; start < n; start++) {
            if (state[start] != UNVISITED) {
                continue;
            }

            int[] path = new int[n];
            int pathLength = 0;
            int current = start;

            while (true) {
                if (state[current] == UNVISITED) {
                    state[current] = IN_PATH;
                    position[current] = pathLength;
                    path[pathLength] = current;
                    pathLength++;

                    current = wish(current);
                } else if (state[current] == IN_PATH) {
                    int cycleStart = position[current];

                    for (int k = cycleStart; k < pathLength; k++) {
                        submatching[path[k]] = true;
                    }

                    markPathDone(path, pathLength, state, position);
                    break;
                } else {
                    markPathDone(path, pathLength, state, position);
                    break;
                }
            }
        }

        return submatching;
    }

    private void markPathDone(int[] path, int pathLength, int[] state, int[] position) {
        for (int k = 0; k < pathLength; k++) {
            state[path[k]] = DONE;
            position[path[k]] = -1;
        }
    }

    private void initializeToFirstEligibleChoices() {
        for (int agent = 0; agent < n; agent++) {
            G[agent] = 0;
            skipIneligible(agent);

            if (G[agent] >= n) {
                throw new IllegalArgumentException(
                    "Agent " + agent + " has no eligible houses."
                );
            }
        }
    }

    private void skipIneligible(int agent) {
        while (G[agent] < n && !eligible[agent][pref[agent][G[agent]]]) {
            G[agent]++;
        }
    }

    private static int validateAndGetSize(int[][] pref, boolean[][] eligible) {
        if (pref == null) {
            throw new IllegalArgumentException("Preference matrix must be non-null.");
        }

        int n = pref.length;
        if (n == 0) {
            throw new IllegalArgumentException("There must be at least one agent.");
        }

        if (eligible == null) {
            throw new IllegalArgumentException("Eligibility matrix must be non-null.");
        }

        if (eligible.length != n) {
            throw new IllegalArgumentException(
                "Eligibility matrix must have one row per agent."
            );
        }

        validatePreferences(pref, n);
        validateEligibility(eligible, n);

        return n;
    }

    private static void validatePreferences(int[][] pref, int n) {
        for (int agent = 0; agent < n; agent++) {
            if (pref[agent] == null || pref[agent].length != n) {
                throw new IllegalArgumentException(
                    "Agent " + agent + " must rank exactly " + n + " houses."
                );
            }

            boolean[] seen = new boolean[n];

            for (int rank = 0; rank < n; rank++) {
                int house = pref[agent][rank];

                if (house < 0 || house >= n) {
                    throw new IllegalArgumentException(
                        "Invalid house id " + house + " in preferences of agent " + agent + "."
                    );
                }

                if (seen[house]) {
                    throw new IllegalArgumentException(
                        "Duplicate house " + house + " in preferences of agent " + agent + "."
                    );
                }

                seen[house] = true;
            }
        }
    }

    private static void validateEligibility(boolean[][] eligible, int n) {
        for (int agent = 0; agent < n; agent++) {
            if (eligible[agent] == null || eligible[agent].length != n) {
                throw new IllegalArgumentException(
                    "Eligibility row for agent " + agent + " must have exactly " + n + " entries."
                );
            }

            boolean hasEligibleHouse = false;
            for (int house = 0; house < n; house++) {
                if (eligible[agent][house]) {
                    hasEligibleHouse = true;
                }
            }

            if (!hasEligibleHouse) {
                throw new IllegalArgumentException(
                    "Agent " + agent + " must be eligible for at least one house."
                );
            }
        }
    }

    private static int[][] copyPreferences(int[][] pref) {
        int[][] copy = new int[pref.length][];

        for (int i = 0; i < pref.length; i++) {
            copy[i] = new int[pref[i].length];
            for (int j = 0; j < pref[i].length; j++) {
                copy[i][j] = pref[i][j];
            }
        }

        return copy;
    }

    private static boolean[][] copyEligibility(boolean[][] eligible) {
        boolean[][] copy = new boolean[eligible.length][];

        for (int i = 0; i < eligible.length; i++) {
            copy[i] = new boolean[eligible[i].length];
            for (int j = 0; j < eligible[i].length; j++) {
                copy[i][j] = eligible[i][j];
            }
        }

        return copy;
    }
}
