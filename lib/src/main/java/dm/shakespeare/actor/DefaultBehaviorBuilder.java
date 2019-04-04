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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.function.Observer;
import dm.shakespeare.function.Tester;
import dm.shakespeare.util.ConstantConditions;

/**
 * Default implementation of a {@code BehaviorBuilder}.
 */
class DefaultBehaviorBuilder implements BehaviorBuilder {

  private static final Observer<Agent> DEFAULT_AGENT_OBSERVER = new Observer<Agent>() {

    public void accept(@NotNull final Agent agent) {
    }
  };
  private static final Handler<?> DEFAULT_MESSAGE_HANDLER = new Handler<Object>() {

    public void handle(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
    }
  };

  private final ArrayList<MatchingHandler> mMessageHandlers = new ArrayList<MatchingHandler>();
  private final ArrayList<Handler<?>> mNoMatchHandlers = new ArrayList<Handler<?>>();
  private final ArrayList<Observer<? super Agent>> mStartObservers =
      new ArrayList<Observer<? super Agent>>();
  private final ArrayList<Observer<? super Agent>> mStopObservers =
      new ArrayList<Observer<? super Agent>>();

  @NotNull
  @SuppressWarnings("unchecked")
  public Behavior build() {
    final ArrayList<MatchingHandler> messageHandlers = mMessageHandlers;
    final ArrayList<Handler<?>> noMatchHandlers = mNoMatchHandlers;
    final Handler<?> messageHandler;
    if (messageHandlers.isEmpty()) {
      if (noMatchHandlers.isEmpty()) {
        messageHandler = DEFAULT_MESSAGE_HANDLER;

      } else if (noMatchHandlers.size() == 1) {
        messageHandler = noMatchHandlers.get(0);

      } else {
        messageHandler = new MultipleMessageHandler(messageHandlers, noMatchHandlers);
      }

    } else if ((messageHandlers.size() == 1) && noMatchHandlers.isEmpty()) {
      messageHandler = messageHandlers.get(0);

    } else {
      messageHandler = new MultipleMessageHandler(messageHandlers, noMatchHandlers);
    }
    final ArrayList<Observer<? super Agent>> startObservers = mStartObservers;
    final Observer<? super Agent> startObserver;
    if (startObservers.isEmpty()) {
      startObserver = DEFAULT_AGENT_OBSERVER;

    } else if (startObservers.size() == 1) {
      startObserver = startObservers.get(0);

    } else {
      startObserver = new MultipleAgentObserver(startObservers);
    }
    final ArrayList<Observer<? super Agent>> stopObservers = mStopObservers;
    final Observer<? super Agent> stopObserver;
    if (stopObservers.isEmpty()) {
      stopObserver = DEFAULT_AGENT_OBSERVER;

    } else if (stopObservers.size() == 1) {
      stopObserver = stopObservers.get(0);

    } else {
      stopObserver = new MultipleAgentObserver(stopObservers);
    }
    return new DefaultBehavior(messageHandler, startObserver, stopObserver);
  }

  @NotNull
  public <T> BehaviorBuilder onAny(@NotNull final Handler<? super T> handler) {
    mMessageHandlers.add(new AnyMessageHandler(ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public <T> BehaviorBuilder onEnvelop(@NotNull final Tester<? super Envelop> tester,
      @NotNull final Handler<? super T> handler) {
    mMessageHandlers.add(
        new SenderTesterMessageHandler(ConstantConditions.notNull("tester", tester),
            ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public <T> BehaviorBuilder onMatch(@NotNull final Matcher<? super T> matcher,
      @NotNull final Handler<? super T> handler) {
    mMessageHandlers.add(new MatcherMessageHandler(ConstantConditions.notNull("matcher", matcher),
        ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public <T> BehaviorBuilder onMessage(@NotNull final Class<T> messageClass,
      @NotNull final Handler<? super T> handler) {
    mMessageHandlers.add(
        new ClassMessageHandler(ConstantConditions.notNull("messageClass", messageClass),
            ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public <T> BehaviorBuilder onMessage(
      @NotNull final Collection<? extends Class<? extends T>> messageClasses,
      @NotNull final Handler<? super T> handler) {
    mMessageHandlers.add(new ClassesMessageHandler(
        ConstantConditions.notNullElements("messageClasses", messageClasses),
        ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public <T> BehaviorBuilder onMessage(@NotNull final Tester<? super T> tester,
      @NotNull final Handler<? super T> handler) {
    mMessageHandlers.add(new TesterMessageHandler(ConstantConditions.notNull("tester", tester),
        ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public <T> BehaviorBuilder onMessageEqualTo(final T message,
      @NotNull final Handler<? super T> handler) {
    mMessageHandlers.add(
        new EqualToMessageHandler(message, ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public BehaviorBuilder onNoMatch(@NotNull final Handler<? super Object> handler) {
    mNoMatchHandlers.add(ConstantConditions.notNull("handler", handler));
    return this;
  }

  @NotNull
  public BehaviorBuilder onStart(@NotNull final Observer<? super Agent> observer) {
    mStartObservers.add(ConstantConditions.notNull("observer", observer));
    return this;
  }

  @NotNull
  public BehaviorBuilder onStop(@NotNull final Observer<? super Agent> observer) {
    mStopObservers.add(ConstantConditions.notNull("observer", observer));
    return this;
  }

  private static class AnyMessageHandler extends MatchingHandler {

    private AnyMessageHandler(@NotNull final Handler<?> handler) {
      super(handler);
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      return true;
    }
  }

  private static class ClassMessageHandler extends MatchingHandler {

    private final Class<?> mClass;

    private ClassMessageHandler(@NotNull final Class<?> messageClass,
        @NotNull final Handler<?> handler) {
      super(handler);
      mClass = messageClass;
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      return mClass.isInstance(message);
    }
  }

  private static class ClassesMessageHandler extends MatchingHandler {

    private final Collection<? extends Class<?>> mClasses;

    private ClassesMessageHandler(@NotNull final Collection<? extends Class<?>> messageClasses,
        @NotNull final Handler<?> handler) {
      super(handler);
      mClasses = messageClasses;
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      for (final Class<?> messageClass : mClasses) {
        if (messageClass.isInstance(message)) {
          return true;
        }
      }
      return false;
    }
  }

  private static class DefaultBehavior implements Behavior {

    private final Handler<Object> mMessageHandler;
    private final Observer<? super Agent> mStartObserver;
    private final Observer<? super Agent> mStopObserver;

    @SuppressWarnings("unchecked")
    private DefaultBehavior(@NotNull final Handler<?> messageHandler,
        @NotNull final Observer<? super Agent> startObserver,
        @NotNull final Observer<? super Agent> stopObserver) {
      mMessageHandler = (Handler<Object>) messageHandler;
      mStartObserver = startObserver;
      mStopObserver = stopObserver;
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      mMessageHandler.handle(message, envelop, agent);
    }

    public void onStart(@NotNull final Agent agent) throws Exception {
      mStartObserver.accept(agent);
    }

    public void onStop(@NotNull final Agent agent) throws Exception {
      mStopObserver.accept(agent);
    }
  }

  private static class EqualToMessageHandler extends MatchingHandler {

    private final Object mMessage;

    private EqualToMessageHandler(final Object message, @NotNull final Handler<?> handler) {
      super(handler);
      mMessage = message;
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      final Object other = mMessage;
      return (other == message) || (other != null) && other.equals(message);
    }
  }

  private static class MatcherMessageHandler extends MatchingHandler {

    private final Matcher<Object> mMatcher;

    @SuppressWarnings("unchecked")
    private MatcherMessageHandler(@NotNull final Matcher<?> matcher,
        @NotNull final Handler<?> handler) {
      super(handler);
      mMatcher = (Matcher<Object>) matcher;
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      return mMatcher.match(message, envelop, agent);
    }
  }

  private static abstract class MatchingHandler implements Handler {

    private final Handler<Object> mHandler;

    @SuppressWarnings("unchecked")
    private MatchingHandler(@NotNull final Handler<?> handler) {
      mHandler = (Handler<Object>) handler;
    }

    abstract boolean matches(Object message, @NotNull Envelop envelop, @NotNull Agent agent) throws
        Exception;

    public void handle(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      if (matches(message, envelop, agent)) {
        mHandler.handle(message, envelop, agent);
      }
    }
  }

  private static class MultipleAgentObserver implements Observer<Agent> {

    private final List<Observer<? super Agent>> mObservers;

    private MultipleAgentObserver(@NotNull final List<Observer<? super Agent>> observers) {
      mObservers = observers;
    }

    public void accept(@NotNull final Agent agent) throws Exception {
      for (final Observer<? super Agent> handler : mObservers) {
        handler.accept(agent);
      }
    }
  }

  private static class MultipleMessageHandler implements Handler {

    private final List<Handler<?>> mFallbacks;
    private final List<MatchingHandler> mHandlers;

    private MultipleMessageHandler(@NotNull final List<MatchingHandler> handlers,
        @NotNull final List<Handler<?>> fallbacks) {
      mHandlers = handlers;
      mFallbacks = fallbacks;
    }

    @SuppressWarnings("unchecked")
    public void handle(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      boolean hasMatch = false;
      for (final MatchingHandler handler : mHandlers) {
        if (handler.matches(message, envelop, agent)) {
          handler.handle(message, envelop, agent);
          hasMatch = true;
        }
      }

      if (!hasMatch) {
        for (final Handler<?> handler : mFallbacks) {
          ((Handler<Object>) handler).handle(message, envelop, agent);
        }
      }
    }
  }

  private static class SenderTesterMessageHandler extends MatchingHandler {

    private final Tester<? super Envelop> mTester;

    @SuppressWarnings("unchecked")
    private SenderTesterMessageHandler(@NotNull final Tester<? super Envelop> tester,
        @NotNull final Handler<?> handler) {
      super(handler);
      mTester = tester;
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      return mTester.test(envelop);
    }
  }

  private static class TesterMessageHandler extends MatchingHandler {

    private final Tester<Object> mTester;

    @SuppressWarnings("unchecked")
    private TesterMessageHandler(@NotNull final Tester<?> tester,
        @NotNull final Handler<?> handler) {
      super(handler);
      mTester = (Tester<Object>) tester;
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      return mTester.test(message);
    }
  }
}
