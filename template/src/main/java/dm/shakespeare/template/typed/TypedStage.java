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
import dm.shakespeare.template.typed.background.Background;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 06/17/2019.
 */
public class TypedStage extends Stage {

  private final Stage stage;

  public TypedStage(@NotNull final Stage stage) {
    this.stage = ConstantConditions.notNull("stage", stage);
  }

  @NotNull
  public static <T> T newActor(@NotNull final Class<? extends T> type,
      @NotNull final Background background, @NotNull final Class<?> roleType,
      @NotNull final Object... roleArgs) {
    try {
      final Actor actor = Stage.newActor(new TypedRole(background, roleType, roleArgs));
      return ActorHandler.createProxy(type, background, actor);

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  public static <T> T newActor(@NotNull final Class<? extends T> type,
      @NotNull final Background background, @NotNull final Object role) {
    try {
      final Actor actor = Stage.newActor(new TypedRole(background, role));
      return ActorHandler.createProxy(type, background, actor);

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  public static <T> T newActor(@NotNull final String id, @NotNull final Class<? extends T> type,
      @NotNull final Background background, @NotNull final Class<?> roleType,
      @NotNull final Object... roleArgs) {
    try {
      final Actor actor = Stage.newActor(id, new TypedRole(background, roleType, roleArgs));
      return ActorHandler.createProxy(type, background, actor);

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  public static <T> T newActor(@NotNull final String id, @NotNull final Class<? extends T> type,
      @NotNull final Background background, @NotNull final Object role) {
    try {
      final Actor actor = Stage.newActor(id, new TypedRole(background, role));
      return ActorHandler.createProxy(type, background, actor);

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void addObserver(@NotNull final Actor observer) {
    stage.addObserver(observer);
  }

  @NotNull
  @Override
  public Actor createActor(@NotNull final Role role) {
    return stage.createActor(role);
  }

  @NotNull
  @Override
  public Actor createActor(@NotNull final String id, @NotNull final Role role) {
    return stage.createActor(id, role);
  }

  @NotNull
  @Override
  public ActorSet findAll(@NotNull final Pattern idPattern) {
    return stage.findAll(idPattern);
  }

  @NotNull
  @Override
  public ActorSet findAll(@NotNull final Tester<? super Actor> tester) {
    return stage.findAll(tester);
  }

  @Nullable
  @Override
  public Actor findAny(@NotNull final Pattern idPattern) {
    return stage.findAny(idPattern);
  }

  @Nullable
  @Override
  public Actor findAny(@NotNull final Tester<? super Actor> tester) {
    return stage.findAny(tester);
  }

  @Nullable
  @Override
  public Actor get(@NotNull final String id) {
    return stage.get(id);
  }

  @NotNull
  @Override
  public ActorSet getAll() {
    return stage.getAll();
  }

  @Override
  public void removeObserver(@NotNull final Actor observer) {
    stage.removeObserver(observer);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> T createActor(@NotNull final Class<? extends T> type,
      @NotNull final Background background, @NotNull final Object role) {
    try {
      final Actor actor = stage.createActor(new TypedRole(background, role));
      return ActorHandler.createProxy(type, background, actor);

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  public <T> T createActor(@NotNull final String id, @NotNull final Class<? extends T> type,
      @NotNull final Background background, @NotNull final Class<?> roleType,
      @NotNull final Object... roleArgs) {
    try {
      final Actor actor = stage.createActor(id, new TypedRole(background, roleType, roleArgs));
      return ActorHandler.createProxy(type, background, actor);

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  public <T> T createActor(@NotNull final String id, @NotNull final Class<? extends T> type,
      @NotNull final Background background, @NotNull final Object role) {
    try {
      final Actor actor = stage.createActor(id, new TypedRole(background, role));
      return ActorHandler.createProxy(type, background, actor);

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  public <T> T createActor(@NotNull final Class<? extends T> type,
      @NotNull final Background background, @NotNull final Class<?> roleType,
      @NotNull final Object... roleArgs) {
    try {
      final Actor actor = stage.createActor(new TypedRole(background, roleType, roleArgs));
      return ActorHandler.createProxy(type, background, actor);

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
