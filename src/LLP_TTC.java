package org.example;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntConsumer;

public class LLP_TTC {
    private final int n;
    private final int[][] pref;

    private final PrefNode[] prefListHead;
    private final PrefNode[][] prefPointer;

    private final boolean[] fixed;
    private final boolean[] inCycle;
    private final Set<Integer>[] children;

    private static final int UNVISITED = 0;
    private static final int IN_PATH = 1;
    private static final int DONE = 2;

    private static class PrefNode {
        final int house;
        PrefNode prev;
        PrefNode next;

        PrefNode(int house) {
            this.house = house;
        }
    }

    public LLP_TTC(int[][] pref) {
        validateAndGetSize(pref);
        this.n = pref.length;
        this.pref = copyPreferences(pref);

        this.prefListHead = new PrefNode[n];
        this.prefPointer = new PrefNode[n][n];

        this.fixed = new boolean[n];
        this.inCycle = new boolean[n];

        @SuppressWarnings("unchecked")
        Set<Integer>[] tree = new Set[n];
        this.children = tree;

        for (int i = 0; i < n; i++) {
            children[i] = ConcurrentHashMap.newKeySet();
        }

        buildPreferenceLists();
    }

    public void solve() {
        int nThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        int rounds = 0;

        try {
            while (existsUnfixedAgent()) {
                rounds++;
                if (rounds > n) {
                    throw new IllegalStateException("Unexpected non-termination in LLP_TTC.");
                }

                initializeRoundState(pool);
                markRoots(pool);
                informTree();
                deleteFixedHousesFromOthers(pool);
                finalizeFixedAgents(pool);
            }
        } finally {
            pool.shutdown();
        }
    }

    public int[] getSolution() {
        int[] solution = new int[n];

        for (int i = 0; i < n; i++) {
            PrefNode head = prefListHead[i];
            if (head == null) {
                throw new IllegalStateException("Agent " + i + " has no assigned house.");
            }
            solution[i] = head.house;
        }

        return solution;
    }

    void markRoots(ExecutorService pool) {
        int[] succ = new int[n];
        parallelFor(pool, 0, n, i -> {
            succ[i] = fixed[i] ? -1 : headHouse(i);
        });

        int[] state = new int[n];
        int[] position = new int[n];
        Arrays.fill(position, -1);

        for (int start = 0; start < n; start++) {
            if (fixed[start] || state[start] != UNVISITED) {
                continue;
            }

            List<Integer> path = new ArrayList<>();
            int current = start;

            while (true) {
                if (fixed[current]) {
                    markDone(path, state, position);
                    break;
                }

                if (state[current] == UNVISITED) {
                    state[current] = IN_PATH;
                    position[current] = path.size();
                    path.add(current);
                    current = succ[current];
                    continue;
                }

                if (state[current] == IN_PATH) {
                    int cycleStart = position[current];
                    for (int k = cycleStart; k < path.size(); k++) {
                        inCycle[path.get(k)] = true;
                    }
                    markDone(path, state, position);
                    break;
                }

                markDone(path, state, position);
                break;
            }
        }

        parallelFor(pool, 0, n, child -> {
            if (fixed[child] || inCycle[child]) {
                return;
            }
            int parent = succ[child];
            if (!fixed[parent]) {
                children[parent].add(child);
            }
        });
    }

    void informTree() {
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        boolean[] informed = new boolean[n];

        for (int i = 0; i < n; i++) {
            if (!fixed[i] && inCycle[i]) {
                queue.add(i);
                informed[i] = true;
            }
        }

        while (!queue.isEmpty()) {
            int node = queue.removeFirst();
            for (int child : children[node]) {
                if (!informed[child]) {
                    informed[child] = true;
                    queue.addLast(child);
                }
            }
        }
    }

    private void initializeRoundState(ExecutorService pool) {
        parallelFor(pool, 0, n, i -> {
            if (!fixed[i]) {
                inCycle[i] = false;
            }
            children[i].clear();
        });
    }

    private void deleteFixedHousesFromOthers(ExecutorService pool) {
        boolean[] cycleHouses = new boolean[n];

        for (int house = 0; house < n; house++) {
            cycleHouses[house] = !fixed[house] && inCycle[house];
        }

        parallelFor(pool, 0, n, agent -> {
            if (fixed[agent] || inCycle[agent]) {
                return;
            }

            for (int house = 0; house < n; house++) {
                if (cycleHouses[house]) {
                    PrefNode node = prefPointer[agent][house];
                    if (node != null) {
                        deleteNode(agent, node);
                    }
                }
            }

            if (prefListHead[agent] == null) {
                throw new IllegalStateException(
                        "Agent " + agent + " has no feasible house remaining."
                );
            }
        });
    }

    private void finalizeFixedAgents(ExecutorService pool) {
        parallelFor(pool, 0, n, i -> {
            if (!fixed[i] && inCycle[i]) {
                fixed[i] = true;
            }
        });
    }

    private void parallelFor(ExecutorService pool, int startInclusive, int endExclusive, IntConsumer action) {
        CountDownLatch done = new CountDownLatch(endExclusive - startInclusive);

        for (int i = startInclusive; i < endExclusive; i++) {
            final int idx = i;
            pool.execute(() -> {
                try {
                    action.accept(idx);
                } finally {
                    done.countDown();
                }
            });
        }

        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Parallel phase interrupted.", e);
        }
    }

    private void buildPreferenceLists() {
        for (int agent = 0; agent < n; agent++) {
            PrefNode head = null;
            PrefNode tail = null;

            for (int rank = 0; rank < n; rank++) {
                int house = pref[agent][rank];
                PrefNode node = new PrefNode(house);
                prefPointer[agent][house] = node;

                if (head == null) {
                    head = node;
                    tail = node;
                } else {
                    tail.next = node;
                    node.prev = tail;
                    tail = node;
                }
            }

            prefListHead[agent] = head;
        }
    }

    private int headHouse(int agent) {
        PrefNode head = prefListHead[agent];
        if (head == null) {
            throw new IllegalStateException("Agent " + agent + " has empty preference list.");
        }
        return head.house;
    }

    private void deleteNode(int agent, PrefNode node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            prefListHead[agent] = node.next;
        }

        if (node.next != null) {
            node.next.prev = node.prev;
        }

        prefPointer[agent][node.house] = null;
        node.prev = null;
        node.next = null;
    }

    private boolean existsUnfixedAgent() {
        for (boolean b : fixed) {
            if (!b) {
                return true;
            }
        }
        return false;
    }

    private static void markDone(List<Integer> path, int[] state, int[] pos) {
        for (int node : path) {
            state[node] = DONE;
            pos[node] = -1;
        }
    }

    private static int validateAndGetSize(int[][] pref) {
        if (pref == null) {
            throw new IllegalArgumentException("Preference matrix must be non-null.");
        }

        int n = pref.length;
        if (n == 0) {
            throw new IllegalArgumentException("There must be at least one agent.");
        }

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

        return n;
    }

    private static int[][] copyPreferences(int[][] pref) {
        int n = pref.length;
        int[][] copy = new int[n][n];

        for (int i = 0; i < n; i++) {
            System.arraycopy(pref[i], 0, copy[i], 0, n);
        }

        return copy;
    }
}
