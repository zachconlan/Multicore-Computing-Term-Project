import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class LLP {

    final protected int numProcesses;

    public LLP(int numProcesses) {
        this.numProcesses = numProcesses;
    }

    // Checks whether process j is forbidden in the state vector G
    public abstract boolean forbidden(int j);

    // Advances on process j
    public abstract void advance(int j);


    public void solve() {
        int nThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);

        boolean[] isForbidden = new boolean[numProcesses];
        AtomicBoolean hasForbidden = new AtomicBoolean();

        try {
            while (true) {
                hasForbidden.set(false);

                // Phase 1: check forbidden states in parallel
                CountDownLatch forbiddenDone = new CountDownLatch(numProcesses);

                for (int j = 0; j < numProcesses; j++) {
                    final int idx = j;
                    pool.execute(() -> {
                        isForbidden[idx] = forbidden(idx);
                        if (isForbidden[idx]) {
                            hasForbidden.set(true);
                        }
                        forbiddenDone.countDown();
                    });
                }
                forbiddenDone.await();

                if (!hasForbidden.get()) {
                    break;
                }


                // Phase 2: advance on forbidden states in parallel
                CountDownLatch advanceDone = new CountDownLatch(numProcesses);

                for (int j = 0; j < numProcesses; j++) {
                    final int idx = j;
                    pool.execute(() -> {
                        if (isForbidden[idx]) {
                            advance(idx);
                        }
                        advanceDone.countDown();
                    });
                }
                advanceDone.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdown();
        }
    }
}
