/*
 * Copyright 2019 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dm.shakespeare.concurrent;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by davide-maestroni on 03/05/2019.
 */
public class ExecutorServicesTest {

  @Test
  public void actorExecutorCached() throws Exception {
    testExecutor(ExecutorServices.asActorExecutor(Executors.newCachedThreadPool()));
  }

  @Test
  public void actorExecutorFuture() {
    testFuture(ExecutorServices.asActorExecutor(Executors.newSingleThreadExecutor()));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void actorExecutorNPE() {
    ExecutorServices.asActorExecutor((ExecutorService) null);
  }

  @Test
  public void actorExecutorShutdown() throws Exception {
    testShutdown(ExecutorServices.asActorExecutor(Executors.newSingleThreadExecutor()));
  }

  @Test
  public void actorExecutorSingle() throws Exception {
    testExecutor(ExecutorServices.asActorExecutor(Executors.newSingleThreadExecutor()));
  }

  @Test
  public void actorExecutorThread() throws Exception {
    testExecutor(ExecutorServices.asActorExecutor(Executors.newScheduledThreadPool(5)));
  }

  @Test
  public void actorScheduledExecutorFuture() {
    testFuture(ExecutorServices.asActorExecutor(Executors.newSingleThreadScheduledExecutor()));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void actorScheduledExecutorNPE() {
    ExecutorServices.asActorExecutor(null);
  }

  @Test
  public void actorScheduledExecutorShutdown() throws Exception {
    testShutdown(ExecutorServices.asActorExecutor(Executors.newSingleThreadScheduledExecutor()));
  }

  @Test
  public void dynamicScheduledThreadPool() throws Exception {
    testExecutor(ExecutorServices.newDynamicScheduledThreadPool(3, 3, 0, TimeUnit.SECONDS));
  }

  @Test
  public void dynamicScheduledThreadPoolFactory() throws Exception {
    final DefaultThreadFactory threadFactory = new DefaultThreadFactory();
    testExecutor(
        ExecutorServices.newDynamicScheduledThreadPool(0, 5, 10, TimeUnit.SECONDS, threadFactory));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void dynamicScheduledThreadPoolFactoryNPE() {
    ExecutorServices.newDynamicScheduledThreadPool(null);
  }

  @Test
  public void dynamicScheduledThreadPoolFactoryOnly() throws Exception {
    final DefaultThreadFactory threadFactory = new DefaultThreadFactory();
    testExecutor(ExecutorServices.newDynamicScheduledThreadPool(threadFactory));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void dynamicScheduledThreadPoolFullFactoryNPE() {
    ExecutorServices.newDynamicScheduledThreadPool(0, 5, 10, TimeUnit.SECONDS, null);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void dynamicScheduledThreadPoolFullUnitNPE() {
    final DefaultThreadFactory threadFactory = new DefaultThreadFactory();
    ExecutorServices.newDynamicScheduledThreadPool(0, 5, 10, null, threadFactory);
  }

  @Test
  public void dynamicScheduledThreadPoolFuture() {
    testFuture(ExecutorServices.newDynamicScheduledThreadPool(0, 3, 0, TimeUnit.SECONDS));
  }

  @Test
  public void dynamicScheduledThreadPoolShutdown() throws Exception {
    testShutdown(ExecutorServices.newDynamicScheduledThreadPool(0, 3, 0, TimeUnit.SECONDS));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void dynamicScheduledThreadPoolUnitNPE() {
    ExecutorServices.newDynamicScheduledThreadPool(0, 5, 10, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void localExecutor() throws Exception {
    testExecutor(ExecutorServices.localExecutor());
  }

  @Test
  public void localExecutorAwait() throws Exception {
    final ExecutorService executor = ExecutorServices.localExecutor();
    assertThat(executor.isShutdown()).isFalse();
    assertThat(executor.isTerminated()).isFalse();
    assertThat(executor.awaitTermination(100, TimeUnit.MILLISECONDS)).isFalse();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void localExecutorShutdown() {
    final ExecutorService executor = ExecutorServices.localExecutor();
    assertThat(executor.isShutdown()).isFalse();
    assertThat(executor.isTerminated()).isFalse();
    executor.shutdownNow();
  }

  @Test
  public void priorityExecutor() throws Exception {
    testExecutor(ExecutorServices.withPriority(1, ExecutorServices.newTrampolineExecutor()));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void priorityExecutorNPE() {
    ExecutorServices.withPriority(0, (ExecutorService) null);
  }

  @Test
  public void priorityScheduledExecutor() throws Exception {
    testExecutor(ExecutorServices.withPriority(13, Executors.newScheduledThreadPool(3)));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void priorityScheduledExecutorNPE() {
    ExecutorServices.withPriority(0, null);
  }

  @Test
  public void throttlingExecutor() throws Exception {
    testExecutor(ExecutorServices.withThrottling(3, ExecutorServices.newTrampolineExecutor()));
  }

  @Test
  public void throttlingExecutorFuture() {
    testFuture(ExecutorServices.withThrottling(3, Executors.newSingleThreadExecutor()));
  }

  @Test
  public void throttlingExecutorShutdown() throws Exception {
    testShutdown(ExecutorServices.withThrottling(3, Executors.newSingleThreadExecutor()));
  }

  @Test
  public void throttlingExecutorSingle() throws Exception {
    testExecutor(ExecutorServices.withThrottling(1, ExecutorServices.newTrampolineExecutor()));
  }

  @Test
  public void throttlingExecutorThreads() throws Exception {
    final ScheduledExecutorService executor =
        ExecutorServices.withThrottling(1, Executors.newScheduledThreadPool(13));
    final AtomicBoolean isFailed = new AtomicBoolean();
    final AtomicBoolean isRunning = new AtomicBoolean();
    final CountDownLatch latch = new CountDownLatch(13);
    for (int i = 0; i < 13; ++i) {
      executor.execute(new ThrottlingRunnable(500, isRunning, isFailed, latch));
    }
    final boolean isDone = latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();
    assertThat(isDone).isTrue();
    assertThat(isFailed.get()).isFalse();
  }

  @Test
  public void throttlingScheduledExecutor() throws Exception {
    testExecutor(ExecutorServices.withThrottling(5, Executors.newScheduledThreadPool(13)));
  }

  @Test
  public void throttlingScheduledExecutorFuture() {
    testFuture(ExecutorServices.withThrottling(5, Executors.newScheduledThreadPool(13)));
  }

  @Test
  public void throttlingScheduledExecutorShutdown() throws Exception {
    testShutdown(ExecutorServices.withThrottling(5, Executors.newScheduledThreadPool(13)));
  }

  @Test
  public void throttlingScheduledExecutorSingle() throws Exception {
    testExecutor(ExecutorServices.withThrottling(1, Executors.newScheduledThreadPool(13)));
  }

  @Test
  public void timeoutExecutor() throws Exception {
    testExecutor(ExecutorServices.withTimeout(3, TimeUnit.SECONDS, false,
        ExecutorServices.newTrampolineExecutor()));
  }

  @Test
  public void timeoutExecutorFuture() {
    testFuture(ExecutorServices.withTimeout(3, TimeUnit.SECONDS, false,
        Executors.newSingleThreadExecutor()));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void timeoutExecutorNPE() {
    ExecutorServices.withTimeout(1, TimeUnit.SECONDS, true, (ExecutorService) null);
  }

  @Test(expected = IllegalArgumentException.class)
  @SuppressWarnings("ConstantConditions")
  public void timeoutExecutorPositive() {
    ExecutorServices.withTimeout(0, TimeUnit.SECONDS, true, ExecutorServices.localExecutor());
  }

  @Test(expected = IllegalArgumentException.class)
  @SuppressWarnings("ConstantConditions")
  public void timeoutExecutorScheduledPositive() {
    ExecutorServices.withTimeout(0, TimeUnit.SECONDS, true, Executors.newSingleThreadExecutor());
  }

  @Test
  public void timeoutExecutorShutdown() throws Exception {
    testShutdown(ExecutorServices.withTimeout(3, TimeUnit.SECONDS, false,
        Executors.newSingleThreadExecutor()));
  }

  @Test
  public void timeoutExecutorTimeout() throws Exception {
    final ScheduledExecutorService executor =
        ExecutorServices.withTimeout(100, TimeUnit.MILLISECONDS, true,
            Executors.newSingleThreadScheduledExecutor());
    final AtomicBoolean isFailed = new AtomicBoolean();
    final CountDownLatch latch = new CountDownLatch(1);
    executor.execute(new TimeoutRunnable(500, isFailed, latch));
    final boolean isDone = latch.await(2, TimeUnit.SECONDS);
    executor.shutdown();
    assertThat(isDone).isTrue();
    assertThat(isFailed.get()).isFalse();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void timeoutExecutorUnitNPE() {
    ExecutorServices.withTimeout(1, null, true, ExecutorServices.localExecutor());
  }

  @Test
  public void timeoutScheduledExecutor() throws Exception {
    testExecutor(ExecutorServices.withTimeout(3, TimeUnit.SECONDS, true,
        Executors.newScheduledThreadPool(13)));
  }

  @Test
  public void timeoutScheduledExecutorFuture() {
    testFuture(ExecutorServices.withTimeout(3, TimeUnit.SECONDS, true,
        Executors.newScheduledThreadPool(13)));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void timeoutScheduledExecutorNPE() {
    ExecutorServices.withTimeout(1, TimeUnit.SECONDS, true, null);
  }

  @Test
  public void timeoutScheduledExecutorShutdown() throws Exception {
    testShutdown(ExecutorServices.withTimeout(3, TimeUnit.SECONDS, true,
        Executors.newScheduledThreadPool(13)));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void timeoutScheduledExecutorUnitNPE() {
    ExecutorServices.withTimeout(1, null, true, Executors.newSingleThreadExecutor());
  }

  @Test
  public void trampolineExecutor() throws Exception {
    testExecutor(ExecutorServices.newTrampolineExecutor());
  }

  @Test
  public void trampolineExecutorFuture() {
    final ExecutorService executor = ExecutorServices.newTrampolineExecutor();
    final Future<Object> future = executor.submit(new SleepCallable<Object>(1000));
    assertThat(future.cancel(true)).isFalse();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isTrue();
    executor.shutdown();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void trampolineExecutorNPE() {
    ExecutorServices.newTrampolineExecutor(null);
  }

  @Test
  public void trampolineExecutorQueue() throws Exception {
    testExecutor(ExecutorServices.newTrampolineExecutor(new LinkedBlockingQueue<Runnable>()));
  }

  @Test
  public void trampolineExecutorShutdown() throws Exception {
    testShutdown(ExecutorServices.newTrampolineExecutor());
  }

  private void testExecutor(@NotNull final ExecutorService executor) throws Exception {
    testExecutor(ExecutorServices.asScheduled(executor));
  }

  private void testExecutor(@NotNull final ScheduledExecutorService executor) throws Exception {
    try {
      final Random random = new Random(System.currentTimeMillis());
      final ArrayList<TestRunExecution> executions = new ArrayList<TestRunExecution>();
      for (int i = 0; i < 13; ++i) {
        final long delay;
        final TimeUnit timeUnit;
        final int unit = random.nextInt(4);
        switch (unit) {
          case 0:
            delay = (long) Math.floor(random.nextFloat() * 500);
            timeUnit = TimeUnit.MILLISECONDS;
            break;
          case 1:
            delay = (long) Math.floor(random.nextFloat() * TimeUnit.MILLISECONDS.toMicros(500));
            timeUnit = TimeUnit.MICROSECONDS;
            break;
          case 2:
            delay = (long) Math.floor(random.nextFloat() * TimeUnit.MILLISECONDS.toNanos(500));
            timeUnit = TimeUnit.NANOSECONDS;
            break;
          default:
            delay = 0;
            timeUnit = TimeUnit.MILLISECONDS;
            break;
        }
        final TestRunExecution execution = new TestRunExecution(delay, timeUnit);
        executions.add(execution);
        executor.schedule(execution, delay, timeUnit);
      }

      for (final TestRunExecution execution : executions) {
        execution.await();
        assertThat(execution.isPassed()).isTrue();
      }
      executions.clear();
      final ArrayList<Delay> delays = new ArrayList<Delay>();
      for (int i = 0; i < 13; ++i) {
        final long delay;
        final TimeUnit timeUnit;
        final int unit = random.nextInt(4);
        switch (unit) {
          case 0:
            delay = (long) Math.floor(random.nextFloat() * 500);
            timeUnit = TimeUnit.MILLISECONDS;
            break;
          case 1:
            delay = (long) Math.floor(random.nextFloat() * TimeUnit.MILLISECONDS.toMicros(500));
            timeUnit = TimeUnit.MICROSECONDS;
            break;
          case 2:
            delay = (long) Math.floor(random.nextFloat() * TimeUnit.MILLISECONDS.toNanos(500));
            timeUnit = TimeUnit.NANOSECONDS;
            break;
          default:
            delay = 0;
            timeUnit = TimeUnit.MILLISECONDS;
            break;
        }
        delays.add(new Delay(delay, timeUnit));
        final TestRunExecution execution = new TestRunExecution(delay, timeUnit);
        executions.add(execution);
      }
      final TestRecursiveExecution recursiveExecution =
          new TestRecursiveExecution(executor, executions, delays, 0, TimeUnit.MILLISECONDS);
      executor.execute(recursiveExecution);
      for (final TestRunExecution execution : executions) {
        execution.await();
        assertThat(execution.isPassed()).isTrue();
      }

      final Object result = new Object();
      final ScheduledFuture<Object> future =
          executor.schedule(new TestCallable<Object>(result), 0, TimeUnit.MILLISECONDS);
      assertThat(future.get(10, TimeUnit.SECONDS)).isSameAs(result);
      List<Future<Object>> futures =
          executor.invokeAll(Collections.singleton(new TestCallable<Object>(result)));
      assertThat(futures.get(0).get(10, TimeUnit.SECONDS)).isSameAs(result);
      futures = executor.invokeAll(Collections.singleton(new TestCallable<Object>(result)), 10,
          TimeUnit.MILLISECONDS);
      assertThat(futures.get(0).get(10, TimeUnit.SECONDS)).isSameAs(result);
      assertThat(
          executor.invokeAny(Collections.singleton(new TestCallable<Object>(result)))).isSameAs(
          result);
      assertThat(executor.invokeAny(Collections.singleton(new TestCallable<Object>(result)), 10,
          TimeUnit.MILLISECONDS)).isSameAs(result);

    } finally {
      executor.shutdown();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  private void testFuture(@NotNull final ExecutorService executor) {
    final Future<Object> future = executor.submit(new SleepCallable<Object>(1000));
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isFalse();
    assertThat(future.cancel(true)).isTrue();
    assertThat(future.isCancelled()).isTrue();
    assertThat(future.isDone()).isTrue();
    executor.shutdown();
  }

  private void testShutdown(@NotNull final ExecutorService executor) throws InterruptedException {
    assertThat(executor.isShutdown()).isFalse();
    assertThat(executor.isTerminated()).isFalse();
    assertThat(executor.shutdownNow()).isEmpty();
    assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    assertThat(executor.isShutdown()).isTrue();
    assertThat(executor.isTerminated()).isTrue();
  }

  private static class DefaultThreadFactory implements ThreadFactory {

    public Thread newThread(@NotNull final Runnable runnable) {
      return new Thread(runnable);
    }
  }

  private static class Delay {

    private final long mDelay;
    private final TimeUnit mTimeUnit;

    private Delay(final long delay, @NotNull final TimeUnit timeUnit) {
      mDelay = delay;
      mTimeUnit = timeUnit;
    }

    long getDelay() {
      return mDelay;
    }

    @NotNull
    TimeUnit getTimeUnit() {
      return mTimeUnit;
    }
  }

  private static class SleepCallable<V> implements Callable<V> {

    private final long mSleepMillis;

    SleepCallable(final long sleepMillis) {
      mSleepMillis = sleepMillis;
    }

    public V call() throws InterruptedException {
      Thread.sleep(mSleepMillis);
      return null;
    }
  }

  private static class TestCallable<V> implements Callable<V> {

    private final V mValue;

    TestCallable(final V value) {
      mValue = value;
    }

    public V call() {
      return mValue;
    }
  }

  private static class TestRecursiveExecution extends TestRunExecution {

    private final ArrayList<Delay> mDelays;
    private final ArrayList<TestRunExecution> mExecutions;
    private final ScheduledExecutorService mExecutor;

    TestRecursiveExecution(@NotNull final ScheduledExecutorService executor,
        @NotNull final ArrayList<TestRunExecution> executions,
        @NotNull final ArrayList<Delay> delays, final long delay,
        @NotNull final TimeUnit timeUnit) {
      super(delay, timeUnit);
      mExecutor = executor;
      mExecutions = executions;
      mDelays = delays;
    }

    @Override
    @SuppressWarnings("UnnecessaryLocalVariable")
    public void run() {
      final ArrayList<TestRunExecution> executions = mExecutions;
      final ArrayList<Delay> delays = mDelays;
      final ScheduledExecutorService executor = mExecutor;
      final int size = executions.size();
      for (int i = 0; i < size; ++i) {
        final Delay delay = delays.get(i);
        final TestRunExecution execution = executions.get(i);
        executor.schedule(execution, delay.getDelay(), delay.getTimeUnit());
      }
      super.run();
    }
  }

  private static class TestRunExecution implements Runnable {

    private final long mDelay;
    private final Semaphore mSemaphore = new Semaphore(0);
    private final long mStartTime;
    private final TimeUnit mTimeUnit;
    private boolean mIsPassed;

    TestRunExecution(final long delay, @NotNull final TimeUnit timeUnit) {
      mStartTime = System.currentTimeMillis();
      mDelay = delay;
      mTimeUnit = timeUnit;
    }

    public void run() {
      // The JVM might not have nanosecond precision...
      mIsPassed = (System.currentTimeMillis() - mStartTime >= mTimeUnit.toMillis(mDelay));
      mSemaphore.release();
    }

    void await() throws InterruptedException {
      mSemaphore.acquire();
    }

    boolean isPassed() {
      return mIsPassed;
    }
  }

  private static class ThrottlingRunnable implements Runnable {

    private final AtomicBoolean mIsFailed;
    private final AtomicBoolean mIsRunning;
    private final CountDownLatch mLatch;
    private final long mSleepMillis;

    ThrottlingRunnable(final long sleepMillis, @NotNull final AtomicBoolean isRunning,
        @NotNull final AtomicBoolean isFailed, @NotNull final CountDownLatch latch) {
      mSleepMillis = sleepMillis;
      mIsRunning = isRunning;
      mIsFailed = isFailed;
      mLatch = latch;
    }

    public void run() {
      try {
        if (mIsRunning.getAndSet(true)) {
          mIsFailed.set(true);

        } else {
          Thread.sleep(mSleepMillis);
        }

      } catch (final InterruptedException ignored) {

      } finally {
        mIsRunning.set(false);
        mLatch.countDown();
      }
    }
  }

  private static class TimeoutRunnable implements Runnable {

    private final AtomicBoolean mIsFailed;
    private final CountDownLatch mLatch;
    private final long mSleepMillis;

    TimeoutRunnable(final long sleepMillis, @NotNull final AtomicBoolean isFailed,
        @NotNull final CountDownLatch latch) {
      mSleepMillis = sleepMillis;
      mIsFailed = isFailed;
      mLatch = latch;
    }

    public void run() {
      try {
        Thread.sleep(mSleepMillis);
        mIsFailed.set(true);

      } catch (final InterruptedException ignored) {

      } finally {
        mLatch.countDown();
      }
    }
  }
}
