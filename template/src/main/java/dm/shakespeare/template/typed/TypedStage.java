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

package dm.shakespeare.template.typed;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorSet;
import dm.shakespeare.actor.Role;
import dm.shakespeare.function.Tester;
import dm.shakespeare.template.typed.actor.Script;
import dm.shakespeare.util.ConstantConditions;

/**
 * {@code Stage} implementation supporting the creation of typed actors, that is, actors whose roles
 * are defined by a generic object instance.<br>
 * A typed actor is exposed as an interface defining the communication protocol. It is not mandatory
 * for the role instance to implement the actor interface, since methods to invoke are resolved
 * based on the interface method name and parameters. Any method can be used, notice however that
 * calls may block waiting for return values, as specified by the actor {@link Script}. In case the
 * timeout elapses, a {@link InvocationTimeoutException} will be thrown.<p>
 * A typed actor interface may define methods with other actors and type actors parameters, however
 * only single objects and {@link java.util.List List}s of such types are supported.<br>
 * Additionally, a method may include a sender actor and headers parameters (annotated with
 * {@link dm.shakespeare.template.typed.annotation.ActorFrom ActorFrom} and
 * {@link dm.shakespeare.template.typed.annotation.HeadersFrom HeadersFrom} annotation respectively.
 * <br>
 * Such parameters will be not included when resolving the method to invoke, but they will be
 * employed to send invocation messages to the actor handling the role instance. The sender actor
 * must expect an {@link dm.shakespeare.template.typed.message.InvocationResult InvocationResult} or
 * an {@link dm.shakespeare.template.typed.message.InvocationException InvocationException} messages
 * as response.
 */
public class TypedStage extends Stage {

  private static final TypedStage BACK_STAGE = new TypedStage(Stage.back());

  private final Stage stage;

  /**
   * Creates a new stage instance backed by the specified one.
   *
   * @param stage the backing stage employed during actual actors creation.
   */
  public TypedStage(@NotNull final Stage stage) {
    this.stage = ConstantConditions.notNull("stage", stage);
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public static TypedStage back() {
    return BACK_STAGE;
  }

  /**
   * Returns the actor instance backing the specified typed actor.
   *
   * @param actor the typed actor instance.
   * @return the backing actor.
   * @throws IllegalArgumentException is the specified object is not a typed actor instance.
   */
  @NotNull
  public static Actor getActor(@NotNull final Object actor) {
    return ActorHandler.getActor(actor);
  }

  /**
   * Returns a new typed actor backed by the specified one.
   *
   * @param type  the type of interface defining the actor protocol.
   * @param actor the backing actor.
   * @param <T>   the returned actor runtime type.
   * @return the typed actor instance.
   * @throws IllegalArgumentException is the specified actor does not backed a typed instance.
   */
  @NotNull
  public static <T> T getTyped(@NotNull final Class<? extends T> type, @NotNull final Actor actor) {
    return ActorHandler.getProxy(actor, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addObserver(@NotNull final Actor observer) {
    stage.addObserver(observer);
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public Actor createActor(@NotNull final Role role) {
    return stage.createActor(role);
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public Actor createActor(@NotNull final String id, @NotNull final Role role) {
    return stage.createActor(id, role);
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public ActorSet findAll(@NotNull final Pattern idPattern) {
    return stage.findAll(idPattern);
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public ActorSet findAll(@NotNull final Tester<? super Actor> tester) {
    return stage.findAll(tester);
  }

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  public Actor findAny(@NotNull final Pattern idPattern) {
    return stage.findAny(idPattern);
  }

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  public Actor findAny(@NotNull final Tester<? super Actor> tester) {
    return stage.findAny(tester);
  }

  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  public Actor get(@NotNull final String id) {
    return stage.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public ActorSet getAll() {
    return stage.getAll();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeObserver(@NotNull final Actor observer) {
    stage.removeObserver(observer);
  }

  /**
   * Creates a new typed actor, with the specified ID, following the specified script.<br>
   * Notice that the returned instance will proxy any call to the instance methods, including
   * {@link Object#hashCode()}, {@link Object#equals(Object)} and {@link Object#toString()}.
   *
   * @param type   the type of interface defining the actor protocol.
   * @param id     the actor ID.
   * @param script the actor script.
   * @param <T>    the returned actor runtime type.
   * @return the typed actor instance.
   */
  @NotNull
  public <T> T createActor(@NotNull final Class<? extends T> type, @NotNull final String id,
      @NotNull final Script script) {
    try {
      final Actor actor = stage.createActor(id, new TypedRole(script));
      return ActorHandler.createProxy(actor, type, script);

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a new typed actor following the specified script.<br>
   * Notice that the returned instance will proxy any call to the instance methods, including
   * {@link Object#hashCode()}, {@link Object#equals(Object)} and {@link Object#toString()}.
   *
   * @param type   the type of interface defining the actor protocol.
   * @param script the actor script.
   * @param <T>    the returned actor runtime type.
   * @return the typed actor instance.
   */
  @NotNull
  public <T> T createActor(@NotNull final Class<? extends T> type, @NotNull final Script script) {
    try {
      final Actor actor = stage.createActor(new TypedRole(script));
      return ActorHandler.createProxy(actor, type, script);

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
