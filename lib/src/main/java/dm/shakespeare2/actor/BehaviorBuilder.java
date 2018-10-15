package dm.shakespeare2.actor;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import dm.shakespeare2.actor.Actor.Envelop;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.function.Observer;
import dm.shakespeare2.function.Tester;

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
