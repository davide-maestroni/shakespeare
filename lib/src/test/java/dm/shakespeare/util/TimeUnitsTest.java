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

package dm.shakespeare.util;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.TimeUnits.Condition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TimeUnits} unit tests.
 */
public class TimeUnitsTest {

  @Test
  public void currentTimeMillis() {
    assertThat(TimeUnits.currentTimeIn(TimeUnit.MILLISECONDS)).isEqualTo(
        System.currentTimeMillis());
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void currentTimeNPE() {
    TimeUnits.currentTimeIn(null);
  }

  @Test
  public void currentTimeNanos() {
    final long nanoTime = System.nanoTime();
    assertThat(TimeUnits.currentTimeIn(TimeUnit.NANOSECONDS)).isBetween(nanoTime,
        System.nanoTime());
  }

  @Test
  public void currentTimeSeconds() {
    assertThat(TimeUnits.currentTimeIn(TimeUnit.SECONDS)).isEqualTo(
        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
  }

  @Test
  public void sleepAtLeastMillis() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    TimeUnits.sleepAtLeast(200, TimeUnit.MILLISECONDS);
    assertThat(System.currentTimeMillis()).isGreaterThanOrEqualTo(startTime + 200);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void sleepAtLeastNPE() throws InterruptedException {
    TimeUnits.sleepAtLeast(200, null);
  }

  @Test
  public void sleepAtLeastNanos() throws InterruptedException {
    final long startTime = System.nanoTime();
    TimeUnits.sleepAtLeast(200, TimeUnit.NANOSECONDS);
    assertThat(System.nanoTime()).isGreaterThanOrEqualTo(startTime + 200);
  }

  @Test
  public void sleepAtLeastOneMilliNanos() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    TimeUnits.sleepAtLeast(TimeUnit.MILLISECONDS.toNanos(1), TimeUnit.NANOSECONDS);
    assertThat(System.currentTimeMillis()).isGreaterThanOrEqualTo(startTime + 1);
  }

  @Test
  public void sleepAtLeastZero() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    TimeUnits.sleepAtLeast(0, TimeUnit.HOURS);
    assertThat(System.currentTimeMillis()).isBetween(startTime, startTime + 100);
  }

  @Test
  public void sleepSinceMillis() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    assertThat(TimeUnits.sleepSinceMillis(200, TimeUnit.MILLISECONDS, startTime - 100)).isTrue();
    assertThat(System.currentTimeMillis()).isGreaterThanOrEqualTo(startTime + 100);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void sleepSinceMillisNPE() throws InterruptedException {
    TimeUnits.sleepSinceMillis(200, null, 200);
  }

  @Test
  public void sleepSinceMillisNegative() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    assertThat(TimeUnits.sleepSinceMillis(100, TimeUnit.MILLISECONDS, startTime - 200)).isFalse();
    assertThat(System.currentTimeMillis()).isBetween(startTime, startTime + 100);
  }

  @Test
  public void sleepSinceMillisZero() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    assertThat(TimeUnits.sleepSinceMillis(0, TimeUnit.MILLISECONDS, startTime - 100)).isFalse();
    assertThat(System.currentTimeMillis()).isBetween(startTime, startTime + 100);
  }

  @Test
  public void sleepSinceNanos() throws InterruptedException {
    final long startTime = System.nanoTime();
    assertThat(
        TimeUnits.sleepSinceNanos(2000000, TimeUnit.NANOSECONDS, startTime - 1000000)).isTrue();
    assertThat(System.nanoTime()).isGreaterThanOrEqualTo(startTime + 1000000);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void sleepSinceNanosNPE() throws InterruptedException {
    TimeUnits.sleepSinceNanos(200, null, 200);
  }

  @Test
  public void sleepSinceNanosNegative() throws InterruptedException {
    final long startTime = System.nanoTime();
    assertThat(TimeUnits.sleepSinceNanos(100, TimeUnit.NANOSECONDS, startTime - 200)).isFalse();
    assertThat(System.nanoTime()).isBetween(startTime,
        startTime + TimeUnit.MILLISECONDS.toNanos(100));
  }

  @Test
  public void sleepSinceNanosZero() throws InterruptedException {
    final long startTime = System.nanoTime();
    assertThat(TimeUnits.sleepSinceNanos(0, TimeUnit.NANOSECONDS, startTime - 100)).isFalse();
    assertThat(System.nanoTime()).isBetween(startTime,
        startTime + TimeUnit.MILLISECONDS.toNanos(100));
  }

  @Test
  public void toDays() {
    assertThat(TimeUnits.toDays(24.5f, TimeUnit.HOURS)).isEqualTo(1);
  }

  @Test
  public void toDaysZero() {
    assertThat(TimeUnits.toDays(0, TimeUnit.MILLISECONDS)).isZero();
  }

  @Test
  public void toHoursLarger() {
    assertThat(TimeUnits.toHours(2.5f, TimeUnit.DAYS)).isEqualTo(60);
  }

  @Test
  public void toHoursSmaller() {
    assertThat(TimeUnits.toHours(99.99f, TimeUnit.MINUTES)).isEqualTo(1);
  }

  @Test
  public void toHoursZero() {
    assertThat(TimeUnits.toHours(0, TimeUnit.MILLISECONDS)).isZero();
  }

  @Test
  public void toMicrosLarger() {
    assertThat(TimeUnits.toMicros(2.5f, TimeUnit.MILLISECONDS)).isEqualTo(2500);
  }

  @Test
  public void toMicrosSmaller() {
    assertThat(TimeUnits.toMicros(9999.99f, TimeUnit.NANOSECONDS)).isEqualTo(9);
  }

  @Test
  public void toMicrosZero() {
    assertThat(TimeUnits.toMicros(0, TimeUnit.MILLISECONDS)).isZero();
  }

  @Test
  public void toMillisLarger() {
    assertThat(TimeUnits.toMillis(2.5f, TimeUnit.SECONDS)).isEqualTo(2500);
  }

  @Test
  public void toMillisSmaller() {
    assertThat(TimeUnits.toMillis(9999.99f, TimeUnit.MICROSECONDS)).isEqualTo(9);
  }

  @Test
  public void toMillisZero() {
    assertThat(TimeUnits.toMillis(0, TimeUnit.MILLISECONDS)).isZero();
  }

  @Test
  public void toMinutesLarger() {
    assertThat(TimeUnits.toMinutes(2.5f, TimeUnit.HOURS)).isEqualTo(150);
  }

  @Test
  public void toMinutesSmaller() {
    assertThat(TimeUnits.toMinutes(99.99f, TimeUnit.SECONDS)).isEqualTo(1);
  }

  @Test
  public void toMinutesZero() {
    assertThat(TimeUnits.toHours(0, TimeUnit.MILLISECONDS)).isZero();
  }

  @Test
  public void toNanos() {
    assertThat(TimeUnits.toNanos(2.5f, TimeUnit.MICROSECONDS)).isEqualTo(2500);
  }

  @Test
  public void toNanosZero() {
    assertThat(TimeUnits.toNanos(0, TimeUnit.MILLISECONDS)).isZero();
  }

  @Test
  public void toSecondsLarger() {
    assertThat(TimeUnits.toSeconds(2.5f, TimeUnit.MINUTES)).isEqualTo(150);
  }

  @Test
  public void toSecondsSmaller() {
    assertThat(TimeUnits.toSeconds(9999.99f, TimeUnit.MILLISECONDS)).isEqualTo(9);
  }

  @Test
  public void toSecondsZero() {
    assertThat(TimeUnits.toSeconds(0, TimeUnit.MILLISECONDS)).isZero();
  }

  @Test
  public void waitSinceMillis() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    synchronized (this) {
      assertThat(
          TimeUnits.waitSinceMillis(this, 200, TimeUnit.MILLISECONDS, startTime - 100)).isTrue();
    }
    assertThat(System.currentTimeMillis()).isGreaterThanOrEqualTo(startTime + 100);
  }

  @Test
  public void waitSinceMillisIndefinite() throws InterruptedException {
    final WaitSinceThread thread = new WaitSinceThread() {

      boolean await() throws InterruptedException {
        return TimeUnits.waitSinceMillis(this, -1, TimeUnit.MILLISECONDS, 0);
      }
    };
    thread.start();
    Thread.sleep(200);
    thread.unlock();
    Thread.sleep(200);
    assertThat(thread.isCalled()).isTrue();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void waitSinceMillisNPE() throws InterruptedException {
    TimeUnits.waitSinceMillis(this, 200, null, 200);
  }

  @Test
  public void waitSinceMillisNegative() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    assertThat(
        TimeUnits.waitSinceMillis(this, 100, TimeUnit.MILLISECONDS, startTime - 200)).isFalse();
    assertThat(System.currentTimeMillis()).isBetween(startTime, startTime + 100);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void waitSinceMillisTargetNPE() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    TimeUnits.waitSinceMillis(null, 200, TimeUnit.MILLISECONDS, startTime);
  }

  @Test
  public void waitSinceMillisZero() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    assertThat(
        TimeUnits.waitSinceMillis(this, 0, TimeUnit.MILLISECONDS, startTime - 100)).isFalse();
    assertThat(System.currentTimeMillis()).isBetween(startTime, startTime + 100);
  }

  @Test
  public void waitSinceNanos() throws InterruptedException {
    final long startTime = System.nanoTime();
    synchronized (this) {
      assertThat(
          TimeUnits.waitSinceNanos(this, 200, TimeUnit.MILLISECONDS, startTime - 100)).isTrue();
    }
    assertThat(System.nanoTime()).isGreaterThanOrEqualTo(
        startTime + TimeUnit.MILLISECONDS.toNanos(100));
  }

  @Test
  public void waitSinceNanosIndefinite() throws InterruptedException {
    final WaitSinceThread thread = new WaitSinceThread() {

      boolean await() throws InterruptedException {
        return TimeUnits.waitSinceNanos(this, -1, TimeUnit.MILLISECONDS, 0);
      }
    };
    thread.start();
    Thread.sleep(200);
    thread.unlock();
    Thread.sleep(200);
    assertThat(thread.isCalled()).isTrue();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void waitSinceNanosNPE() throws InterruptedException {
    TimeUnits.waitSinceNanos(this, 200, null, 200);
  }

  @Test
  public void waitSinceNanosNegative() throws InterruptedException {
    final long startTime = System.nanoTime();
    assertThat(
        TimeUnits.waitSinceNanos(this, 100, TimeUnit.NANOSECONDS, startTime - 200)).isFalse();
    assertThat(System.nanoTime()).isBetween(startTime,
        startTime + TimeUnit.MILLISECONDS.toNanos(100));
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void waitSinceNanosTargetNPE() throws InterruptedException {
    final long startTime = System.nanoTime();
    TimeUnits.waitSinceNanos(null, 200, TimeUnit.MILLISECONDS, startTime);
  }

  @Test
  public void waitSinceNanosZero() throws InterruptedException {
    final long startTime = System.nanoTime();
    assertThat(TimeUnits.waitSinceNanos(this, 0, TimeUnit.MILLISECONDS, startTime - 100)).isFalse();
    assertThat(System.nanoTime()).isBetween(startTime,
        startTime + TimeUnit.MILLISECONDS.toNanos(100));
  }

  @Test
  public void waitUntil() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    synchronized (this) {
      assertThat(
          TimeUnits.waitUntil(this, 200, TimeUnit.MILLISECONDS, new TrueCondition())).isTrue();
    }
    assertThat(System.currentTimeMillis()).isBetween(startTime, startTime + 100);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void waitUntilConditionNPE() throws InterruptedException {
    TimeUnits.waitUntil(this, 200, TimeUnit.MILLISECONDS, null);
  }

  @Test
  public void waitUntilIndefinite() throws InterruptedException {
    final WaitSinceThread thread = new WaitSinceThread() {

      boolean await() throws InterruptedException {
        return TimeUnits.waitUntil(this, -1, TimeUnit.MILLISECONDS, new TrueCondition());
      }
    };
    thread.start();
    Thread.sleep(200);
    thread.unlock();
    Thread.sleep(200);
    assertThat(thread.isCalled()).isTrue();
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void waitUntilNPE() throws InterruptedException {
    TimeUnits.waitUntil(this, 200, null, new FalseCondition());
  }

  @Test
  public void waitUntilNegative() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    assertThat(TimeUnits.waitUntil(this, 100, TimeUnit.MILLISECONDS, new TrueCondition())).isTrue();
    assertThat(System.currentTimeMillis()).isBetween(startTime, startTime + 100);
  }

  @Test(expected = NullPointerException.class)
  @SuppressWarnings("ConstantConditions")
  public void waitUntilTargetNPE() throws InterruptedException {
    TimeUnits.waitUntil(null, 200, TimeUnit.MILLISECONDS, new FalseCondition());
  }

  @Test
  public void waitUntilTimeout() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    synchronized (this) {
      assertThat(
          TimeUnits.waitUntil(this, 200, TimeUnit.NANOSECONDS, new FalseCondition())).isFalse();
    }
    assertThat(System.currentTimeMillis()).isBetween(startTime, startTime + 100);
  }

  @Test
  public void waitUntilZero() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    assertThat(TimeUnits.waitUntil(this, 0, TimeUnit.MILLISECONDS, new TrueCondition())).isTrue();
    assertThat(System.currentTimeMillis()).isBetween(startTime, startTime + 100);
  }

  private static class FalseCondition implements Condition {

    public boolean isTrue() {
      return false;
    }
  }

  private static class TrueCondition implements Condition {

    public boolean isTrue() {
      return true;
    }
  }

  private abstract static class WaitSinceThread extends Thread {

    private boolean isCalled;

    @Override
    public void run() {
      synchronized (this) {
        try {
          assertThat(await()).isTrue();
          isCalled = true;

        } catch (InterruptedException ignored) {
        }
      }
    }

    abstract boolean await() throws InterruptedException;

    boolean isCalled() {
      synchronized (this) {
        return isCalled;
      }
    }

    boolean unlock() {
      final boolean isCalled;
      synchronized (this) {
        isCalled = this.isCalled;
        notifyAll();
      }
      return isCalled;
    }
  }
}
