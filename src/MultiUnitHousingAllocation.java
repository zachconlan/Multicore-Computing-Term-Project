import java.util.ArrayList;
import java.util.List;


/**
 * This class has the following limitations:
 * 1. It assumes that the total capacity across all house types exactly equals the number of agents. No more, no less.
 * 2. It assumes initially each agent owns a room of a expanded copy.
 *    This is not very realistic, but it allows us to reuse the same logic of HousingAllocationWithTies without modification.
 *    To lift this assumption we would need to rework the entire codebase to allow arbitrary initial endowments, which is a non-trivial amount of work.
 */
public class MultiUnitHousingAllocation extends LLP {
    int numAgents;
    List<List<Integer>>[] expandedPref; // Expanded preferences over copies, tiered but with copies instead of house types
    
    // capacity[h] = number of rooms of house type h
    int numHouseTypes;
    int[] capacity;

    // Total number of expanded copies = sum of rooms across all house types
    int totalCopies;

    // Map an expanded copy id to its original house type. Used to compress the solution back to original house types.
    int[] copyToHouseType;
    // List of copy ids for each house type
    List<Integer>[] houseTypeToCopies;

    // This class uses HousingAllocationWithTies internally to solve the expanded problem.
    // After solving, it compresses the assignment back to original house types.
    HousingAllocationWithTies solver;

    /**
     * capacity is an array of size numHouseTypes, where capacity[h] is the number of rooms of house type h.
     * Example:
     * capacity = [2, 1] means there are 2 types of houses: A and B. There are 2 rooms of type A, and 1 room of type B. 
     * Total 3 rooms, so this implementation requires exactly 3 agents.
     */
    public MultiUnitHousingAllocation(int[] capacity, int numAgents, List<List<Integer>>[] originalPref) {
        super(numAgents);

        if (capacity == null || originalPref == null) {
            throw new IllegalArgumentException("capacity and originalPref must be non-null");
        }
        if (originalPref.length != numAgents) {
            throw new IllegalArgumentException(
                "Expected preferences for exactly " + numAgents + " agents. Got " + originalPref.length
            );
        }

        this.numAgents = numAgents;
        this.capacity = capacity.clone();
        this.numHouseTypes = capacity.length;

        buildCopies();
        this.expandedPref = expandPreferences(originalPref);
        this.solver = new HousingAllocationWithTies(expandedPref);
    }

    @SuppressWarnings("unchecked")
    private void buildCopies() {
        totalCopies = 0;
        for (int c : capacity) {
            if (c < 0) {
                throw new IllegalArgumentException("Capacities must be non-negative");
            }
            totalCopies += c;
        }

        if (totalCopies != numAgents) {
            // We want a room for each agent, and no extra rooms. Exact.
            throw new IllegalArgumentException(
                "Total capacity must equal number of agents. " +
                "capacity sum = " + totalCopies + ", agents = " + numAgents
            );
        }

        copyToHouseType = new int[totalCopies];
        houseTypeToCopies = new List[numHouseTypes];
        for (int h = 0; h < numHouseTypes; h++) {
            houseTypeToCopies[h] = new ArrayList<>();
        }

        int nextCopyId = 0;
        for (int h = 0; h < numHouseTypes; h++) {
            for (int k = 0; k < capacity[h]; k++) {
                copyToHouseType[nextCopyId] = h;
                houseTypeToCopies[h].add(nextCopyId);
                nextCopyId++;
            }
        }
    }

    /**
     * Expand preferences over house types into tied preferences over copies.
     *
     * Example:
     *   if original tier of an agent is [A, C]
     *   and A has copies [0,1], C has copy [3]
     *   then expanded tier for this agent is [0,1,3]
     */
    public List<List<Integer>>[] expandPreferences(List<List<Integer>>[] originalPref) {
        List<List<Integer>>[] expanded = new List[numAgents];

        for (int agent = 0; agent < numAgents; agent++) {
            List<List<Integer>> expandedTiers = new ArrayList<>();
            for (List<Integer> tier : originalPref[agent]) {
                List<Integer> expandedTier = new ArrayList<>();
                for (int houseType : tier) {
                    expandedTier.addAll(houseTypeToCopies[houseType]);
                }
                expandedTiers.add(expandedTier);
            }
            expanded[agent] = expandedTiers;
        }

        return expanded;
    }

    @Override
    public void solve() {
        solver.solve();
    }

    @Override
    public boolean forbidden(int j) {
        return solver.forbidden(j);
    }

    @Override
    public void advance(int j) {
        solver.advance(j);
    }

    public int[] getSolution() {
        int[] copyAssignment = solver.getSolution();
        return compressSolution(copyAssignment);
    }

    /**
     * Convert assignment over copy ids back to original house types.
     */
    public int[] compressSolution(int[] copyAssignment) {
        int[] houseTypeAssignment = new int[numAgents];
        for (int i = 0; i < numAgents; i++) {
            houseTypeAssignment[i] = copyToHouseType[copyAssignment[i]];
        }
        return houseTypeAssignment;
    }

    public int[] getCopyToHouseType() {
        return copyToHouseType;
    }

    public List<Integer>[] getHouseTypeToCopies() {
        return houseTypeToCopies;
    }
}
