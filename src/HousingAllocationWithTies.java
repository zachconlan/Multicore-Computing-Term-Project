import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HousingAllocationWithTies extends LLP {
    int n;                      // Num of agents/houses

    // Strict preference list after tie-breaking within each tier
    int[][] pref;

    int[] G;                    // Current agent proposal number
    boolean[] inSubmatching;    // True if agent i is in the largest submatching

    /**
     * tiedPref[i] is a list of tiers for agent i.
     * Each tier is a list of equally preferred houses.
     *
     * Example for one agent:
     * [
     *   [1, 2],   // top tie
     *   [0],      // next
     *   [3, 4]    // next tie
     * ]
     */
    public HousingAllocationWithTies(List<List<Integer>>[] tiedPref) {
        super(tiedPref.length);
        this.n = tiedPref.length;
        this.pref = normalizePreferences(tiedPref);   // flatten ties -> strict list
        this.G = new int[n];
        this.inSubmatching = new boolean[n];
    }

    /**
     * Convert weak preferences (tiers) into strict preferences
     * by deterministically tie-breaking within each tier.
     *
     * Here we use ascending order of house id.
     */
    private int[][] normalizePreferences(List<List<Integer>>[] tiedPref) {
        int[][] strictPref = new int[n][n];

        for (int agent = 0; agent < n; agent++) {
            List<Integer> flattened = new ArrayList<>();

            boolean[] seen = new boolean[n];

            for (List<Integer> tier : tiedPref[agent]) {
                List<Integer> sortedTier = new ArrayList<>(tier);
                Collections.sort(sortedTier); // deterministic tie-break

                for (int house : sortedTier) {
                    if (house < 0 || house >= n) {
                        throw new IllegalArgumentException(
                            "Invalid house id " + house + " for agent " + agent
                        );
                    }
                    if (seen[house]) {
                        throw new IllegalArgumentException(
                            "Duplicate house " + house + " in preferences of agent " + agent
                        );
                    }
                    seen[house] = true;
                    flattened.add(house);
                }
            }

            if (flattened.size() != n) {
                throw new IllegalArgumentException(
                    "Agent " + agent + " must rank all houses exactly once. Got " + flattened.size()
                );
            }

            for (int k = 0; k < n; k++) {
                strictPref[agent][k] = flattened.get(k);
            }
        }

        return strictPref;
    }

    // wish(G, i) = pref[i][G[i]]
    int wish(int i) {
        return pref[i][G[i]];
    }

    // forbidden(j): (j ∉ S(G)) ∧ (wish(G, j) ∈ S(G))
    @Override
    public boolean forbidden(int j) {
        getSubmatching();
        return !inSubmatching[j] && inSubmatching[wish(j)];
    }

    // advance(j): G[j] := G[j] + 1
    @Override
    public void advance(int j) {
        G[j]++;
    }

    // This method will be called after solve()
    public int[] getSolution() {
        int[] solution = new int[n];
        for (int i = 0; i < n; i++) {
            solution[i] = wish(i);
        }
        return solution;
    }

    final int UNVISITED = 0;
    final int IN_PATH = 1;
    final int DONE = 2;

    // Computes S(G) by following each agent’s wish and marking all
    // agents that are part of cycles in the graph
    void getSubmatching() {
        boolean[] updatedSubmatching = new boolean[n];

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

                    for (int i = cycleStart; i < pathLength; i++) {
                        updatedSubmatching[path[i]] = true;
                    }

                    for (int i = 0; i < pathLength; i++) {
                        state[path[i]] = DONE;
                        position[path[i]] = -1;
                    }
                    break;
                } else {
                    for (int i = 0; i < pathLength; i++) {
                        state[path[i]] = DONE;
                        position[path[i]] = -1;
                    }
                    break;
                }
            }
        }

        inSubmatching = updatedSubmatching;
    }
}