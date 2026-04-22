public class HousingAllocation extends LLP{
    int n;                      // Num of agents/houses
    int[][] pref;               // Agent preference list
    int[] G;                    // Current agent proposal number
    boolean[] inSubmatching;    // True if agent i is in the largest submatching

    public HousingAllocation(int[][] pref) {
        super(pref.length);
        this.pref = pref;
        this.n = pref.length;
        this.G = new int[n];
        this.inSubmatching = new boolean[n];

    }

    // wish(G, i) = pref[i][G[i]]
    int wish(int i){
        return pref[i][G[i]];
    }


    // forbidden(j): (j ∉ S(G)) ∧ (wish(G, j) ∈ S(G))
    @Override
    public boolean forbidden(int j) {
        getSubmatching();
        if(!inSubmatching[j] && inSubmatching[wish(j)]){
            return true;
        }
        return false;
    }

    // advance(j): G[j] := G[j] + 1
    @Override
    public void advance(int j) {
        G[j]++;
    }

    // This method will be called after solve()
    public int[] getSolution() {
        int[] solution = new int[n];
        for(int i=0;i<n;i++){
            solution[i] = wish(i);
        }
        return solution;
    }


    final int UNVISITED = 0;
    final int IN_PATH = 1;
    final int DONE = 2;

    // Find S(G) = largest J such that submatching(G, J)

    // Computes S(G) by following each agent’s wish and marking all
    // agents that are part of cycles in the graph
    void getSubmatching() {
        boolean[] updatedSubmatching = new boolean[n];

        int[] state =  new int[n];      // tracks DFS state of each node
        int[] position =  new int[n];   // index of node in current path
        for(int i=0;i<n;i++){
            position[i] = -1;
        }
        // Start a path from every unvisited agent
        for (int start = 0; start < n; start++) {
            if (state[start] != UNVISITED) {
                continue;
            }

            int[] path = new int[n];
            int pathLength = 0;
            int current = start;

            // Follow wish until you reach a cycle or processed node
            while (true) {
                if (state[current] == UNVISITED) {
                    // Add node to path
                    state[current] = IN_PATH;
                    position[current] = pathLength;
                    path[pathLength] = current;
                    pathLength++;
                    // Continue along path
                    current = wish(current);
                }
                else if (state[current] == IN_PATH) {
                    // Cycle found
                    int cycleStart = position[current];
                    // Add agents to submatching
                    for (int i = cycleStart; i < pathLength; i++) {
                        updatedSubmatching[path[i]] = true;
                    }
                    // Mark path as done
                    for (int i = 0; i < pathLength; i++) {
                        state[path[i]] = DONE;
                        position[path[i]] = -1;
                    }
                    break;
                }
                else {
                    // state[current] == DONE
                    // Reached a processed node, mark path as done
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
