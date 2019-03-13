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
    assertThat(System.currentTimeMillis()).isBetween(startTime, startTime + 10);
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
    assertThat(System.currentTimeMillis()).isBetween(startTime, startTime + 10);
  }

  @Test
  public void sleepSinceMillisZero() throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    assertThat(TimeUnits.sleepSinceMillis(0, TimeUnit.MILLISECONDS, startTime - 100)).isFalse();
    assertThat(System.currentTimeMillis()).isBetween(startTime, startTime + 10);
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
        startTime + TimeUnit.MILLISECONDS.toNanos(10));
  }

  @Test
  public void sleepSinceNanosZero() throws InterruptedException {
    final long startTime = System.nanoTime();
    assertThat(TimeUnits.sleepSinceNanos(0, TimeUnit.NANOSECONDS, startTime - 100)).isFalse();
    assertThat(System.nanoTime()).isBetween(startTime,
        startTime + TimeUnit.MILLISECONDS.toNanos(10));
  }
}
