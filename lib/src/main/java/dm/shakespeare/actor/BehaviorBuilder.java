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

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.function.Observer;
import dm.shakespeare.function.Tester;

/**
 * Interface defining a builder of {@code Behavior} instances.<br>
 * The created behavior instances will call in definition order, upon reception of a new message,
 * all the handlers matching the specified conditions. In case the message or sender match none of
 * them, all the handlers registered by calling {@link #onNoMatch(Handler)} will be invoked.<br>
 * In the same way, all the handlers registered by calling {@link #onStart(Observer)} and
 * {@link #onStop(Observer)}, will be invoked when the actor is started and stopped respectively.
 *
 * @see Behavior
 */
public interface BehaviorBuilder {

  /**
   * Builds the behavior instance.
   *
   * @return the new behavior instance.
   */
  @NotNull
  Behavior build();

  /**
   * Registers the specified handler so that it will be called upon reception of any message.
   *
   * @param handler the handler instance.
   * @param <T>     the message type.
   * @return this builder.
   */
  @NotNull
  <T> BehaviorBuilder onAny(@NotNull Handler<? super T> handler);

  /**
   * Registers the specified handler so that it will be called upon reception of messages whose
   * envelop matches the conditions implemented by the specified tester.
   *
   * @param tester  the tester filtering the message envelop.
   * @param handler the handler instance.
   * @param <T>     the message type.
   * @return this builder.
   */
  @NotNull
  <T> BehaviorBuilder onEnvelop(@NotNull Tester<? super Envelop> tester,
      @NotNull Handler<? super T> handler);

  /**
   * Registers the specified handler so that it will be called upon reception of messages matching
   * the conditions implemented by the specified matcher.
   *
   * @param matcher the matcher filtering the message and its envelop.
   * @param handler the handler instance.
   * @param <T>     the message type.
   * @return this builder.
   */
  @NotNull
  <T> BehaviorBuilder onMatch(@NotNull Matcher<? super T> matcher,
      @NotNull Handler<? super T> handler);

  /**
   * Registers the specified handler so that it will be called upon reception of messages which
   * are instances of the specified class.
   *
   * @param messageClass the message class to filter.
   * @param handler      the handler instance.
   * @param <T>          the message type.
   * @return this builder.
   */
  @NotNull
  <T> BehaviorBuilder onMessage(@NotNull Class<T> messageClass,
      @NotNull Handler<? super T> handler);

  /**
   * Registers the specified handler so that it will be called upon reception of messages which
   * are instances of at least one of the specified classes.
   *
   * @param messageClasses the message classes to filter.
   * @param handler        the handler instance.
   * @param <T>            the message type.
   * @return this builder.
   */
  @NotNull
  <T> BehaviorBuilder onMessage(@NotNull Collection<? extends Class<? extends T>> messageClasses,
      @NotNull Handler<? super T> handler);

  /**
   * Registers the specified handler so that it will be called upon reception of messages matching
   * the conditions implemented by the specified tester.
   *
   * @param tester  the tester filtering the message object.
   * @param handler the handler instance.
   * @param <T>     the message type.
   * @return this builder.
   */
  @NotNull
  <T> BehaviorBuilder onMessage(@NotNull Tester<? super T> tester,
      @NotNull Handler<? super T> handler);

  /**
   * Registers the specified handler so that it will be called upon reception of messages which
   * are {@link Object#equals(Object)} to the specified one.
   *
   * @param message the message object to compare.
   * @param handler the handler instance.
   * @param <T>     the message type.
   * @return this builder.
   */
  @NotNull
  <T> BehaviorBuilder onMessageEqualTo(T message, @NotNull Handler<? super T> handler);

  /**
   * Registers the specified handler so that it will be called if no other registered handler
   * matches the incoming message.
   *
   * @param handler the handler instance.
   * @return this builder.
   */
  @NotNull
  BehaviorBuilder onNoMatch(@NotNull Handler<? super Object> handler);

  /**
   * Registers the specified handler so that it will be called when the actor is started.
   *
   * @param observer the observer instance.
   * @return this builder.
   */
  @NotNull
  BehaviorBuilder onStart(@NotNull Observer<? super Agent> observer);

  /**
   * Registers the specified handler so that it will be called when the actor is stopped.
   *
   * @param observer the observer instance.
   * @return this builder.
   */
  @NotNull
  BehaviorBuilder onStop(@NotNull Observer<? super Agent> observer);

  /**
   * Interface defining an handler of messages.
   *
   * @param <T> the message runtime type.
   */
  interface Handler<T> {

    /**
     * Handles a new message.
     *
     * @param message the message object.
     * @param envelop the message envelop.
     * @param agent   the behavior agent.
     * @throws Exception when an unexpected error occurs.
     */
    void handle(T message, @NotNull Envelop envelop, @NotNull Agent agent) throws Exception;
  }

  /**
   * Interface defining a matcher of messages.
   *
   * @param <T> the message runtime type.
   */
  interface Matcher<T> {

    /**
     * Verifies a new message.
     *
     * @param message the message object.
     * @param envelop the message envelop.
     * @param agent   the behavior agent.
     * @return {@code true} if the specified parameters match the implemented conditions.
     * @throws Exception when an unexpected error occurs.
     */
    boolean match(T message, @NotNull Envelop envelop, @NotNull Agent agent) throws Exception;
  }
}
