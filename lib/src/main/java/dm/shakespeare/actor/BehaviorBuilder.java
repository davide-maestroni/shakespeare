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

package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.function.Observer;
import dm.shakespeare.function.Tester;

/**
 * Created by davide-maestroni on 09/10/2018.
 */
public interface BehaviorBuilder {

  @NotNull
  Behavior build();

  @NotNull
  <T> BehaviorBuilder onAny(@NotNull Handler<? super T> handler);

  @NotNull
  <T> BehaviorBuilder onMatch(@NotNull Matcher<? super T> matcher,
      @NotNull Handler<? super T> handler);

  @NotNull
  <T> BehaviorBuilder onMessage(@NotNull Class<T> messageClass,
      @NotNull Handler<? super T> handler);

  @NotNull
  <T> BehaviorBuilder onMessage(@NotNull Collection<? extends Class<? extends T>> messageClasses,
      @NotNull Handler<? super T> handler);

  @NotNull
  <T> BehaviorBuilder onMessage(@NotNull Tester<? super T> tester,
      @NotNull Handler<? super T> handler);

  @NotNull
  <T> BehaviorBuilder onMessageEqualTo(T message, @NotNull Handler<? super T> handler);

  @NotNull
  BehaviorBuilder onNoMatch(@NotNull Handler<? super Object> handler);

  @NotNull
  <T> BehaviorBuilder onSender(@NotNull Tester<? super Envelop> tester,
      @NotNull Handler<? super T> handler);

  @NotNull
  BehaviorBuilder onStart(@NotNull Observer<? super Context> observer);

  @NotNull
  BehaviorBuilder onStop(@NotNull Observer<? super Context> observer);

  interface Handler<T> {

    void handle(T message, @NotNull Envelop envelop, @NotNull Context context) throws Exception;
  }

  interface Matcher<T> {

    boolean match(T message, @NotNull Envelop envelop, @NotNull Context context) throws Exception;
  }
}
