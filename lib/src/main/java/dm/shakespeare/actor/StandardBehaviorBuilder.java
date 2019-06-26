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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.function.Observer;
import dm.shakespeare.function.Tester;
import dm.shakespeare.util.ConstantConditions;

/**
 * Standard implementation of a {@code BehaviorBuilder}.
 */
class StandardBehaviorBuilder implements BehaviorBuilder {

  private static final DefaultAgentObserver DEFAULT_AGENT_OBSERVER = new DefaultAgentObserver();
  private static final Matcher<?> DEFAULT_MATCHER = new Matcher<Object>() {

    public boolean match(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      return false;
    }
  };
  private static final DefaultMessageHandler DEFAULT_MESSAGE_HANDLER = new DefaultMessageHandler();
  private static final Tester<?> DEFAULT_TESTER = new Tester<Object>() {

    public boolean test(final Object value) {
      return false;
    }
  };

  private final ArrayList<MatchingHandler> messageHandlers = new ArrayList<MatchingHandler>();
  private final ArrayList<Handler<?>> noMatchHandlers = new ArrayList<Handler<?>>();
  private final ArrayList<Observer<? super Agent>> startObservers =
      new ArrayList<Observer<? super Agent>>();
  private final ArrayList<Observer<? super Agent>> stopObservers =
      new ArrayList<Observer<? super Agent>>();

  @NotNull
  @SuppressWarnings("unchecked")
  public Behavior build() {
    final ArrayList<MatchingHandler> messageHandlers = this.messageHandlers;
    final ArrayList<Handler<?>> noMatchHandlers = this.noMatchHandlers;
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
    final ArrayList<Observer<? super Agent>> startObservers = this.startObservers;
    final Observer<? super Agent> startObserver;
    if (startObservers.isEmpty()) {
      startObserver = DEFAULT_AGENT_OBSERVER;

    } else if (startObservers.size() == 1) {
      startObserver = startObservers.get(0);

    } else {
      startObserver = new MultipleAgentObserver(startObservers);
    }
    final ArrayList<Observer<? super Agent>> stopObservers = this.stopObservers;
    final Observer<? super Agent> stopObserver;
    if (stopObservers.isEmpty()) {
      stopObserver = DEFAULT_AGENT_OBSERVER;

    } else if (stopObservers.size() == 1) {
      stopObserver = stopObservers.get(0);

    } else {
      stopObserver = new MultipleAgentObserver(stopObservers);
    }
    return new StandardBehavior(messageHandler, startObserver, stopObserver);
  }

  @NotNull
  public <T> BehaviorBuilder onAny(@NotNull final Handler<? super T> handler) {
    messageHandlers.add(new AnyMessageHandler(ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public <T> BehaviorBuilder onEnvelop(@NotNull final Tester<? super Envelop> tester,
      @NotNull final Handler<? super T> handler) {
    messageHandlers.add(new SenderTesterMessageHandler(ConstantConditions.notNull("tester", tester),
        ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public <T> BehaviorBuilder onEqualTo(final T message, @NotNull final Handler<? super T> handler) {
    messageHandlers.add(
        new EqualToMessageHandler(message, ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public <T> BehaviorBuilder onMatch(@NotNull final Matcher<? super T> matcher,
      @NotNull final Handler<? super T> handler) {
    messageHandlers.add(new MatcherMessageHandler(ConstantConditions.notNull("matcher", matcher),
        ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public <T> BehaviorBuilder onMessage(@NotNull final Class<T> messageClass,
      @NotNull final Handler<? super T> handler) {
    messageHandlers.add(
        new ClassMessageHandler(ConstantConditions.notNull("messageClass", messageClass),
            ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public <T> BehaviorBuilder onMessage(
      @NotNull final Collection<? extends Class<? extends T>> messageClasses,
      @NotNull final Handler<? super T> handler) {
    messageHandlers.add(new ClassesMessageHandler(
        ConstantConditions.notNullElements("messageClasses", messageClasses),
        ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public <T> BehaviorBuilder onMessage(@NotNull final Tester<? super T> tester,
      @NotNull final Handler<? super T> handler) {
    messageHandlers.add(new TesterMessageHandler(ConstantConditions.notNull("tester", tester),
        ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public BehaviorBuilder onNoMatch(@NotNull final Handler<? super Object> handler) {
    noMatchHandlers.add(ConstantConditions.notNull("handler", handler));
    return this;
  }

  @NotNull
  public BehaviorBuilder onStart(@NotNull final Observer<? super Agent> observer) {
    startObservers.add(ConstantConditions.notNull("observer", observer));
    return this;
  }

  @NotNull
  public BehaviorBuilder onStop(@NotNull final Observer<? super Agent> observer) {
    stopObservers.add(ConstantConditions.notNull("observer", observer));
    return this;
  }

  private static class AnyMessageHandler extends MatchingHandler {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private AnyMessageHandler() {
    }

    private AnyMessageHandler(@NotNull final Handler<?> handler) {
      super(handler);
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      return true;
    }
  }

  private static class ClassMessageHandler extends MatchingHandler {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Class<?> messageClass;

    private ClassMessageHandler() {
      messageClass = getClass();
    }

    private ClassMessageHandler(@NotNull final Class<?> messageClass,
        @NotNull final Handler<?> handler) {
      super(handler);
      this.messageClass = messageClass;
    }

    @NotNull
    public Class<?> getMessageClass() {
      return messageClass;
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      return messageClass.isInstance(message);
    }
  }

  private static class ClassesMessageHandler extends MatchingHandler {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Collection<? extends Class<?>> messageClasses;

    private ClassesMessageHandler() {
      messageClasses = Collections.emptyList();
    }

    private ClassesMessageHandler(@NotNull final Collection<? extends Class<?>> messageClasses,
        @NotNull final Handler<?> handler) {
      super(handler);
      this.messageClasses = messageClasses;
    }

    @NotNull
    public Collection<? extends Class<?>> getMessageClasses() {
      return messageClasses;
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      for (final Class<?> messageClass : messageClasses) {
        if (messageClass.isInstance(message)) {
          return true;
        }
      }
      return false;
    }
  }

  private static class DefaultAgentObserver implements Observer<Agent>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    public void accept(@NotNull final Agent agent) {
    }

    private Object readResolve() throws ObjectStreamException {
      return DEFAULT_AGENT_OBSERVER;
    }
  }

  private static class DefaultMessageHandler implements Handler<Object>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    public void handle(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
    }

    private Object readResolve() throws ObjectStreamException {
      return DEFAULT_MESSAGE_HANDLER;
    }
  }

  private static class EqualToMessageHandler extends MatchingHandler {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Object message;

    private EqualToMessageHandler() {
      message = null;
    }

    private EqualToMessageHandler(final Object message, @NotNull final Handler<?> handler) {
      super(handler);
      this.message = message;
    }

    public Object getMessage() {
      return message;
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      final Object other = this.message;
      return (other == message) || (other != null) && other.equals(message);
    }
  }

  private static class MatcherMessageHandler extends MatchingHandler {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Matcher<Object> matcher;

    @SuppressWarnings("unchecked")
    private MatcherMessageHandler() {
      matcher = (Matcher<Object>) DEFAULT_MATCHER;
    }

    @SuppressWarnings("unchecked")
    private MatcherMessageHandler(@NotNull final Matcher<?> matcher,
        @NotNull final Handler<?> handler) {
      super(handler);
      this.matcher = (Matcher<Object>) matcher;
    }

    @NotNull
    public Matcher<Object> getMatcher() {
      return matcher;
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      return matcher.match(message, envelop, agent);
    }
  }

  private static abstract class MatchingHandler implements Handler, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Handler<Object> handler;

    private MatchingHandler() {
      this(DEFAULT_MESSAGE_HANDLER);
    }

    @SuppressWarnings("unchecked")
    private MatchingHandler(@NotNull final Handler<?> handler) {
      this.handler = (Handler<Object>) handler;
    }

    @NotNull
    public Handler<Object> getHandler() {
      return handler;
    }

    public void handle(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      if (matches(message, envelop, agent)) {
        handler.handle(message, envelop, agent);
      }
    }

    abstract boolean matches(Object message, @NotNull Envelop envelop, @NotNull Agent agent) throws
        Exception;
  }

  private static class MultipleAgentObserver implements Observer<Agent>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final List<Observer<? super Agent>> observers;

    private MultipleAgentObserver() {
      observers = Collections.emptyList();
    }

    private MultipleAgentObserver(@NotNull final List<Observer<? super Agent>> observers) {
      this.observers = observers;
    }

    public void accept(@NotNull final Agent agent) throws Exception {
      for (final Observer<? super Agent> handler : observers) {
        handler.accept(agent);
      }
    }

    @NotNull
    public List<Observer<? super Agent>> getObservers() {
      return observers;
    }
  }

  private static class MultipleMessageHandler implements Handler, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final List<Handler<?>> fallbacks;
    private final List<MatchingHandler> handlers;

    private MultipleMessageHandler() {
      handlers = Collections.emptyList();
      fallbacks = Collections.emptyList();
    }

    private MultipleMessageHandler(@NotNull final List<MatchingHandler> handlers,
        @NotNull final List<Handler<?>> fallbacks) {
      this.handlers = handlers;
      this.fallbacks = fallbacks;
    }

    @NotNull
    public List<Handler<?>> getFallbacks() {
      return fallbacks;
    }

    @NotNull
    public List<MatchingHandler> getHandlers() {
      return handlers;
    }

    @SuppressWarnings("unchecked")
    public void handle(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      boolean hasMatch = false;
      for (final MatchingHandler handler : handlers) {
        if (handler.matches(message, envelop, agent)) {
          handler.handle(message, envelop, agent);
          hasMatch = true;
        }
      }

      if (!hasMatch) {
        for (final Handler<?> handler : fallbacks) {
          ((Handler<Object>) handler).handle(message, envelop, agent);
        }
      }
    }
  }

  private static class SenderTesterMessageHandler extends MatchingHandler {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Tester<? super Envelop> tester;

    @SuppressWarnings("unchecked")
    private SenderTesterMessageHandler() {
      tester = (Tester<? super Envelop>) DEFAULT_TESTER;
    }

    private SenderTesterMessageHandler(@NotNull final Tester<? super Envelop> tester,
        @NotNull final Handler<?> handler) {
      super(handler);
      this.tester = tester;
    }

    @NotNull
    public Tester<? super Envelop> getTester() {
      return tester;
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      return tester.test(envelop);
    }
  }

  private static class StandardBehavior implements Behavior, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Handler<Object> messageHandler;
    private final Observer<? super Agent> startObserver;
    private final Observer<? super Agent> stopObserver;

    private StandardBehavior() {
      this(DEFAULT_MESSAGE_HANDLER, DEFAULT_AGENT_OBSERVER, DEFAULT_AGENT_OBSERVER);
    }

    @SuppressWarnings("unchecked")
    private StandardBehavior(@NotNull final Handler<?> messageHandler,
        @NotNull final Observer<? super Agent> startObserver,
        @NotNull final Observer<? super Agent> stopObserver) {
      this.messageHandler = (Handler<Object>) messageHandler;
      this.startObserver = startObserver;
      this.stopObserver = stopObserver;
    }

    @NotNull
    public Handler<Object> getMessageHandler() {
      return messageHandler;
    }

    @NotNull
    public Observer<? super Agent> getStartObserver() {
      return startObserver;
    }

    @NotNull
    public Observer<? super Agent> getStopObserver() {
      return stopObserver;
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      messageHandler.handle(message, envelop, agent);
    }

    public void onStart(@NotNull final Agent agent) throws Exception {
      startObserver.accept(agent);
    }

    public void onStop(@NotNull final Agent agent) throws Exception {
      stopObserver.accept(agent);
    }
  }

  private static class TesterMessageHandler extends MatchingHandler {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Tester<Object> tester;

    @SuppressWarnings("unchecked")
    private TesterMessageHandler() {
      tester = (Tester<Object>) DEFAULT_TESTER;
    }

    @SuppressWarnings("unchecked")
    private TesterMessageHandler(@NotNull final Tester<?> tester,
        @NotNull final Handler<?> handler) {
      super(handler);
      this.tester = (Tester<Object>) tester;
    }

    @NotNull
    public Tester<Object> getTester() {
      return tester;
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      return tester.test(message);
    }
  }
}
