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

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for handling time units.
 */
public class TimeUnits {

  private static final long ONE_MILLI_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

  /**
   * Avoid explicit instantiation.
   */
  private TimeUnits() {
    ConstantConditions.avoid();
  }

  /**
   * Returns the current time in the specified unit.
   *
   * @param unit the time unit.
   * @return the current time.
   */
  public static long currentTimeIn(@NotNull final TimeUnit unit) {
    if (unit == TimeUnit.NANOSECONDS) {
      ConstantConditions.notNull("unit", unit);
      return System.nanoTime();
    }
    return unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
  }

  /**
   * Performs a {@link Thread#sleep(long, int)} using the specified duration as timeout,
   * ensuring that the sleep time is respected even if spurious wake-ups happen in the while.
   *
   * @param time the time value.
   * @param unit the time unit.
   * @throws InterruptedException if the current thread is interrupted.
   */
  public static void sleepAtLeast(final long time, @NotNull final TimeUnit unit) throws
      InterruptedException {
    if (time == 0) {
      ConstantConditions.notNull("unit", unit);
      return;
    }

    if ((unit.compareTo(TimeUnit.MILLISECONDS) >= 0) || ((unit.toNanos(time) % ONE_MILLI_NANOS)
        == 0)) {
      final long startMillis = System.currentTimeMillis();
      while (true) {
        if (!sleepSinceMillis(time, unit, startMillis)) {
          return;
        }
      }
    }
    final long startNanos = System.nanoTime();
    while (true) {
      if (!sleepSinceNanos(time, unit, startNanos)) {
        return;
      }
    }
  }

  /**
   * Performs a {@link Thread#sleep(long, int)} as if started from the specified system time in
   * milliseconds, by using the specified time as timeout.
   *
   * @param time      the time value.
   * @param unit      the time unit.
   * @param milliTime the starting system time in milliseconds.
   * @return whether the sleep happened at all.
   * @throws IllegalStateException if this duration overflows the maximum sleep time.
   * @throws InterruptedException  if the current thread is interrupted.
   * @see System#currentTimeMillis()
   */
  public static boolean sleepSinceMillis(final long time, @NotNull final TimeUnit unit,
      final long milliTime) throws InterruptedException {
    if (time == 0) {
      ConstantConditions.notNull("unit", unit);
      return false;
    }
    final long millisToSleep = milliTime - System.currentTimeMillis() + unit.toMillis(time);
    if (millisToSleep <= 0) {
      ConstantConditions.notNull("unit", unit);
      return false;
    }
    TimeUnit.MILLISECONDS.sleep(millisToSleep);
    return true;
  }

  /**
   * Performs a {@link Thread#sleep(long, int)} as if started from the specified high precision
   * system time in nanoseconds, by using the specified time as timeout.
   *
   * @param time     the time value.
   * @param unit     the time unit.
   * @param nanoTime the starting system time in nanoseconds.
   * @return whether the sleep happened at all.
   * @throws IllegalStateException if this duration overflows the maximum sleep time.
   * @throws InterruptedException  if the current thread is interrupted.
   * @see System#nanoTime()
   */
  public static boolean sleepSinceNanos(final long time, @NotNull final TimeUnit unit,
      final long nanoTime) throws InterruptedException {
    if (time == 0) {
      ConstantConditions.notNull("unit", unit);
      return false;
    }
    final long nanosToSleep = nanoTime - System.nanoTime() + unit.toNanos(time);
    if (nanosToSleep <= 0) {
      ConstantConditions.notNull("unit", unit);
      return false;
    }
    TimeUnit.NANOSECONDS.sleep(nanosToSleep);
    return true;
  }

  /**
   * Converts the specified time in floating point to number of days.
   *
   * @param value the time value.
   * @param unit  the time unit.
   * @return the number of days.
   */
  public static long toDays(final float value, @NotNull final TimeUnit unit) {
    if (unit.compareTo(TimeUnit.DAYS) < 0) {
      return (long) Math.floor(value / unit.convert(1, TimeUnit.DAYS));
    }
    return Math.round(value * TimeUnit.DAYS.convert(1, unit));
  }

  /**
   * Converts the specified time in floating point to number of hours.
   *
   * @param value the time value.
   * @param unit  the time unit.
   * @return the number of hours.
   */
  public static long toHours(final float value, @NotNull final TimeUnit unit) {
    if (unit.compareTo(TimeUnit.HOURS) < 0) {
      return (long) Math.floor(value / unit.convert(1, TimeUnit.HOURS));
    }
    return Math.round(value * TimeUnit.HOURS.convert(1, unit));
  }

  /**
   * Converts the specified time in floating point to number of microseconds.
   *
   * @param value the time value.
   * @param unit  the time unit.
   * @return the number of microseconds.
   */
  public static long toMicros(final float value, @NotNull final TimeUnit unit) {
    if (unit.compareTo(TimeUnit.MICROSECONDS) < 0) {
      return (long) Math.floor(value / unit.convert(1, TimeUnit.MICROSECONDS));
    }
    return Math.round(value * TimeUnit.MICROSECONDS.convert(1, unit));
  }

  /**
   * Converts the specified time in floating point to number of milliseconds.
   *
   * @param value the time value.
   * @param unit  the time unit.
   * @return the number of milliseconds.
   */
  public static long toMillis(final float value, @NotNull final TimeUnit unit) {
    if (unit.compareTo(TimeUnit.MILLISECONDS) < 0) {
      return (long) Math.floor(value / unit.convert(1, TimeUnit.MILLISECONDS));
    }
    return Math.round(value * TimeUnit.MILLISECONDS.convert(1, unit));
  }

  /**
   * Converts the specified time in floating point to number of minutes.
   *
   * @param value the time value.
   * @param unit  the time unit.
   * @return the number of minutes.
   */
  public static long toMinutes(final float value, @NotNull final TimeUnit unit) {
    if (unit.compareTo(TimeUnit.MINUTES) < 0) {
      return (long) Math.floor(value / unit.convert(1, TimeUnit.MINUTES));
    }
    return Math.round(value * TimeUnit.MINUTES.convert(1, unit));
  }

  /**
   * Converts the specified time in floating point to number of nanoseconds.
   *
   * @param value the time value.
   * @param unit  the time unit.
   * @return the number of nanoseconds.
   */
  public static long toNanos(final float value, @NotNull final TimeUnit unit) {
    return Math.round(value * unit.toNanos(1));
  }

  /**
   * Converts the specified time in floating point to number of seconds.
   *
   * @param value the time value.
   * @param unit  the time unit.
   * @return the number of seconds.
   */
  public static long toSeconds(final float value, @NotNull final TimeUnit unit) {
    if (unit.compareTo(TimeUnit.SECONDS) < 0) {
      return (long) Math.floor(value / unit.convert(1, TimeUnit.SECONDS));
    }
    return Math.round(value * TimeUnit.SECONDS.convert(1, unit));
  }

  /**
   * Computes a timestamp in nanoseconds by adding the specified delay to the current time.
   *
   * @param delay the delay value.
   * @param unit  the delay unit.
   * @return the timestamp in number of nanoseconds.
   */
  public static long toTimestampNanos(final long delay, @NotNull final TimeUnit unit) {
    final long delayNano = TimeUnit.NANOSECONDS.convert(Math.max(0, delay), unit);
    final long nanoTime = System.nanoTime();
    return (Long.MAX_VALUE - delayNano < nanoTime) ? Long.MAX_VALUE : nanoTime + delayNano;
  }

  /**
   * Performs an {@link Object#wait()} as if started from the specified system time in
   * milliseconds, by using the specified time.<br>
   * If the specified time is negative, the method will wait indefinitely.
   *
   * @param target    the target object.
   * @param time      the time value.
   * @param unit      the time unit.
   * @param milliTime the starting system time in milliseconds.
   * @return whether the wait happened at all.
   * @throws InterruptedException if the current thread is interrupted.
   * @see System#currentTimeMillis()
   */
  public static boolean waitSinceMillis(@NotNull final Object target, final long time,
      @NotNull final TimeUnit unit, final long milliTime) throws InterruptedException {
    if (time == 0) {
      ConstantConditions.notNull("unit", unit);
      return false;
    }

    if (time < 0) {
      ConstantConditions.notNull("unit", unit);
      target.wait();
      return true;
    }
    final long millisToWait = milliTime - System.currentTimeMillis() + unit.toMillis(time);
    if (millisToWait <= 0) {
      ConstantConditions.notNull("unit", unit);
      return false;
    }
    TimeUnit.MILLISECONDS.timedWait(target, millisToWait);
    return true;
  }

  /**
   * Performs an {@link Object#wait()} as if started from the specified high precision system time
   * in nanoseconds, by using the specified time.<br>
   * If the specified time is negative, the method will wait indefinitely.
   *
   * @param target   the target object.
   * @param time     the time value.
   * @param unit     the time unit.
   * @param nanoTime the starting system time in nanoseconds.
   * @return whether the wait happened at all.
   * @throws InterruptedException if the current thread is interrupted.
   * @see System#nanoTime()
   */
  public static boolean waitSinceNanos(@NotNull final Object target, final long time,
      @NotNull final TimeUnit unit, final long nanoTime) throws InterruptedException {
    if (time == 0) {
      ConstantConditions.notNull("unit", unit);
      return false;
    }

    if (time < 0) {
      ConstantConditions.notNull("unit", unit);
      target.wait();
      return true;
    }
    final long nanosToWait = nanoTime - System.nanoTime() + unit.toNanos(time);
    if (nanosToWait <= 0) {
      ConstantConditions.notNull("unit", unit);
      return false;
    }
    TimeUnit.NANOSECONDS.timedWait(target, nanosToWait);
    return true;
  }

  /**
   * Waits for the specified condition to be true by performing an {@link Object#wait()} and using
   * the specified time.<br>
   * If the specified time is negative, the method will wait indefinitely.
   *
   * @param target    the target object.
   * @param time      the time value.
   * @param unit      the time unit.
   * @param condition the condition to verify.
   * @return whether the check became true before the timeout elapsed.
   * @throws InterruptedException if the current thread is interrupted.
   */
  public static boolean waitUntil(@NotNull final Object target, final long time,
      @NotNull final TimeUnit unit, @NotNull final Condition condition) throws
      InterruptedException {
    if (time == 0) {
      ConstantConditions.notNull("unit", unit);
      return condition.isTrue();
    }

    if (time < 0) {
      ConstantConditions.notNull("unit", unit);
      while (!condition.isTrue()) {
        target.wait();
      }
      return true;
    }

    if ((unit.toNanos(time) % ONE_MILLI_NANOS) == 0) {
      final long startMillis = System.currentTimeMillis();
      while (!condition.isTrue()) {
        if (!waitSinceMillis(target, time, unit, startMillis)) {
          return false;
        }
      }

    } else {
      final long startNanos = System.nanoTime();
      while (!condition.isTrue()) {
        if (!waitSinceNanos(target, time, unit, startNanos)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Interface defining a condition to check.
   */
  public interface Condition {

    /**
     * Checks if true.
     *
     * @return whether the condition is verified.
     */
    boolean isTrue();
  }
}
