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

package dm.shakespeare.log;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Log set unit tests.
 * <p>
 * Created by davide-maestroni on 12/29/2015.
 */
public class LogSetTest {

  private static final String[] ARGS = new String[]{"test1", "test2", "test3", "test4", "test5"};
  private static final String FORMAT0 = "0: %s";
  private static final String FORMAT1 = "0: %s - 1: %s";
  private static final String FORMAT2 = "0: %s - 1: %s - 2: %s";
  private static final String FORMAT3 = "0: %s - 1: %s - 2: %s - 3: %s";
  private static final String FORMAT4 = "0: %s - 1: %s - 2: %s - 3: %s - 4: %s";

//  @Test
//  public void testAdd() {
//    final NullLog log1 = new NullLog();
//    final NullLog log2 = new NullLog();
//    final NullLog log3 = new NullLog();
//    final LogSet logs = new LogSet();
//    logs.addAll(log1, log2, log3);
//    assertThat(logs).containsAll(Arrays.asList(log1, log2, log3));
//    assertThat(new LogSet().appendAll(log1, log2, log3)).containsAll(
//        Arrays.asList(log1, log2, log3));
//    assertThat(new LogSet().appendAll(Arrays.asList(log1, log2, log3))).containsAll(
//        Arrays.asList(log1, log2, log3));
//    assertThat(new LogSet().append(log1).append(log2).append(log3)).containsAll(
//        Arrays.asList(log1, log2, log3));
//    logs.clear();
//    logs.add(LogSet.of(log1, log2, log3));
//    assertThat(logs).containsAll(Arrays.asList(log1, log2, log3));
//  }
//
//  @Test
//  public void testContains() {
//    final NullLog log1 = new NullLog();
//    final NullLog log2 = new NullLog();
//    final NullLog log3 = new NullLog();
//    final LogSet logs = LogSet.of(Arrays.asList(log1, log2, log3));
//    assertThat(logs.contains(log1)).isTrue();
//    assertThat(logs.containsAll(Arrays.asList(log1, log2, log3))).isTrue();
//    assertThat(logs.contains(LogSet.of(log1, log2, log3))).isTrue();
//    assertThat(logs.contains(LogSet.of(new NullLog()))).isFalse();
//    assertThat(logs.containsAll(log1, log2, new NullLog())).isFalse();
//  }
//
//  @Test
//  public void testLogDbg() {
//    final NullPointerException ex = new NullPointerException();
//    final LogSet logSet = LogSet.of(new SystemLog(), new NullLog());
//    final Logger logger = Logger.newLogger(logSet, Level.DEBUG, this);
//
//    logger.dbg(ARGS[0]);
//    logger.dbg(FORMAT0, ARGS[0]);
//    logger.dbg(FORMAT1, ARGS[0], ARGS[1]);
//    logger.dbg(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
//    logger.dbg(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
//    logger.dbg(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
//    logger.dbg(ex);
//    logger.dbg(ex, ARGS[0]);
//    logger.dbg(ex, FORMAT0, ARGS[0]);
//    logger.dbg(ex, FORMAT1, ARGS[0], ARGS[1]);
//    logger.dbg(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
//    logger.dbg(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
//    logger.dbg(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
//  }
//
//  @Test
//  public void testLogErr() {
//    final NullPointerException ex = new NullPointerException();
//    final LogSet logSet = LogSet.of(Arrays.asList(new SystemLog(), new NullLog()));
//    final Logger logger = Logger.newLogger(logSet, Level.DEBUG, this);
//
//    logger.err(ARGS[0]);
//    logger.err(FORMAT0, ARGS[0]);
//    logger.err(FORMAT1, ARGS[0], ARGS[1]);
//    logger.err(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
//    logger.err(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
//    logger.err(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
//    logger.err(ex);
//    logger.err(ex, ARGS[0]);
//    logger.err(ex, FORMAT0, ARGS[0]);
//    logger.err(ex, FORMAT1, ARGS[0], ARGS[1]);
//    logger.err(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
//    logger.err(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
//    logger.err(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
//  }
//
//  @Test
//  public void testLogWrn() {
//    final NullPointerException ex = new NullPointerException();
//    final LogSet logSet = new LogSet().append(new SystemLog()).append(new NullLog());
//    final Logger logger = Logger.newLogger(logSet, Level.DEBUG, this);
//
//    logger.wrn(ARGS[0]);
//    logger.wrn(FORMAT0, ARGS[0]);
//    logger.wrn(FORMAT1, ARGS[0], ARGS[1]);
//    logger.wrn(FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
//    logger.wrn(FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
//    logger.wrn(FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
//    logger.wrn(ex);
//    logger.wrn(ex, ARGS[0]);
//    logger.wrn(ex, FORMAT0, ARGS[0]);
//    logger.wrn(ex, FORMAT1, ARGS[0], ARGS[1]);
//    logger.wrn(ex, FORMAT2, ARGS[0], ARGS[1], ARGS[2]);
//    logger.wrn(ex, FORMAT3, ARGS[0], ARGS[1], ARGS[2], ARGS[3]);
//    logger.wrn(ex, FORMAT4, ARGS[0], ARGS[1], ARGS[2], ARGS[3], ARGS[4]);
//  }
//
//  @Test
//  public void testRemove() {
//    final NullLog log1 = new NullLog();
//    final NullLog log2 = new NullLog();
//    final NullLog log3 = new NullLog();
//    assertThat(LogSet.of(log1, log2, log3).remove(log1)).isTrue();
//    assertThat(LogSet.of(log1, log2).remove(log3)).isFalse();
//    assertThat(LogSet.of(log1, log2).removeAll(log1, log2)).isTrue();
//    assertThat(LogSet.of(log1, log2).removeAll(Arrays.asList(log3, log3))).isFalse();
//    assertThat(LogSet.of(log1, log2, log3).remove(LogSet.of(log2, log3))).isTrue();
//    assertThat(LogSet.of(log1, log2).remove(LogSet.of(log3))).isFalse();
//  }
//
//  @Test
//  public void testRetain() {
//    final NullLog log1 = new NullLog();
//    final NullLog log2 = new NullLog();
//    final NullLog log3 = new NullLog();
//    assertThat(LogSet.of(log1, log2, log3).retainAll(log1)).isTrue();
//    assertThat(LogSet.of(log1, log2).retainAll(log1, log2)).isFalse();
//    assertThat(LogSet.of(log1, log2, log3).retainAll(Arrays.asList(log1, log3))).isTrue();
//    assertThat(LogSet.of(log1, log2).retainAll(Arrays.asList(log1, log2))).isFalse();
//    assertThat(LogSet.of(log1, log2, log3).retainAll(LogSet.of(log1))).isTrue();
//    assertThat(LogSet.of(log1, log2).retainAll(LogSet.of(log1, log2))).isFalse();
//  }
}
