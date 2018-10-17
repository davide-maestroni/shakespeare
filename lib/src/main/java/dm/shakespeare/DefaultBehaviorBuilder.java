package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dm.shakespeare.actor.Actor.Envelop;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.function.Observer;
import dm.shakespeare.function.Tester;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/10/2018.
 */
class DefaultBehaviorBuilder implements BehaviorBuilder {

  private static final Observer<Context> DEFAULT_CONTEXT_OBSERVER = new Observer<Context>() {

    public void accept(@NotNull final Context context) {
    }
  };
  private static final Handler<?> DEFAULT_MESSAGE_HANDLER = new Handler<Object>() {

    public void handle(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
    }
  };

  private final ArrayList<MatchingHandler> mMessageHandlers = new ArrayList<MatchingHandler>();
  private final ArrayList<Handler<?>> mNoMatchHandlers = new ArrayList<Handler<?>>();
  private final ArrayList<Observer<? super Context>> mStartObservers =
      new ArrayList<Observer<? super Context>>();
  private final ArrayList<Observer<? super Context>> mStopObservers =
      new ArrayList<Observer<? super Context>>();

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

    final ArrayList<Observer<? super Context>> startObservers = mStartObservers;
    final Observer<? super Context> startObserver;
    if (startObservers.isEmpty()) {
      startObserver = DEFAULT_CONTEXT_OBSERVER;

    } else if (startObservers.size() == 1) {
      startObserver = startObservers.get(0);

    } else {
      startObserver = new MultipleContextObserver(startObservers);
    }

    final ArrayList<Observer<? super Context>> stopObservers = mStopObservers;
    final Observer<? super Context> stopObserver;
    if (stopObservers.isEmpty()) {
      stopObserver = DEFAULT_CONTEXT_OBSERVER;

    } else if (stopObservers.size() == 1) {
      stopObserver = stopObservers.get(0);

    } else {
      stopObserver = new MultipleContextObserver(stopObservers);
    }

    return new DefaultBehavior(messageHandler, startObserver, stopObserver);
  }

  @NotNull
  public <T> BehaviorBuilder onAny(@NotNull final Handler<? super T> handler) {
    mMessageHandlers.add(new AnyMessageHandler(ConstantConditions.notNull("handler", handler)));
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
  public <T> BehaviorBuilder onSender(@NotNull final Tester<? super Envelop> tester,
      @NotNull final Handler<? super T> handler) {
    mMessageHandlers.add(
        new SenderTesterMessageHandler(ConstantConditions.notNull("tester", tester),
            ConstantConditions.notNull("handler", handler)));
    return this;
  }

  @NotNull
  public BehaviorBuilder onStart(@NotNull final Observer<? super Context> observer) {
    mStartObservers.add(ConstantConditions.notNull("observer", observer));
    return this;
  }

  @NotNull
  public BehaviorBuilder onStop(@NotNull final Observer<? super Context> observer) {
    mStopObservers.add(ConstantConditions.notNull("observer", observer));
    return this;
  }

  private static class AnyMessageHandler extends MatchingHandler {

    private AnyMessageHandler(@NotNull final Handler<?> handler) {
      super(handler);
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
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
        @NotNull final Context context) {
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
        @NotNull final Context context) {
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
    private final Observer<? super Context> mStartObserver;
    private final Observer<? super Context> mStopObserver;

    @SuppressWarnings("unchecked")
    private DefaultBehavior(@NotNull final Handler<?> messageHandler,
        @NotNull final Observer<? super Context> startObserver,
        @NotNull final Observer<? super Context> stopObserver) {
      mMessageHandler = (Handler<Object>) messageHandler;
      mStartObserver = startObserver;
      mStopObserver = stopObserver;
    }

    public void message(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) throws Exception {
      mMessageHandler.handle(message, envelop, context);
    }

    public void start(@NotNull final Context context) throws Exception {
      mStartObserver.accept(context);
    }

    public void stop(@NotNull final Context context) throws Exception {
      mStopObserver.accept(context);
    }
  }

  private static class EqualToMessageHandler extends MatchingHandler {

    private final Object mMessage;

    private EqualToMessageHandler(final Object message, @NotNull final Handler<?> handler) {
      super(handler);
      mMessage = message;
    }

    boolean matches(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
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
        @NotNull final Context context) throws Exception {
      return mMatcher.match(message, envelop, context);
    }
  }

  private static abstract class MatchingHandler implements Handler {

    private final Handler<Object> mHandler;

    @SuppressWarnings("unchecked")
    private MatchingHandler(@NotNull final Handler<?> handler) {
      mHandler = (Handler<Object>) handler;
    }

    abstract boolean matches(Object message, @NotNull Envelop envelop,
        @NotNull Context context) throws Exception;

    public void handle(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) throws Exception {
      if (matches(message, envelop, context)) {
        mHandler.handle(message, envelop, context);
      }
    }
  }

  private static class MultipleContextObserver implements Observer<Context> {

    private final List<Observer<? super Context>> mObservers;

    private MultipleContextObserver(@NotNull final List<Observer<? super Context>> observers) {
      mObservers = observers;
    }

    public void accept(@NotNull final Context context) throws Exception {
      for (final Observer<? super Context> handler : mObservers) {
        handler.accept(context);
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
        @NotNull final Context context) throws Exception {
      boolean hasMatch = false;
      for (final MatchingHandler handler : mHandlers) {
        if (handler.matches(message, envelop, context)) {
          handler.handle(message, envelop, context);
          hasMatch = true;
        }
      }

      if (!hasMatch) {
        for (final Handler<?> handler : mFallbacks) {
          ((Handler<Object>) handler).handle(message, envelop, context);
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
        @NotNull final Context context) throws Exception {
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
        @NotNull final Context context) throws Exception {
      return mTester.test(message);
    }
  }
}
