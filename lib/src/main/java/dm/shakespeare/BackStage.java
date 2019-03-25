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

package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Script;
import dm.shakespeare.function.Observer;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/27/2019.
 */
public class BackStage {

  private static final Observer<Actor> EMPTY_REMOVER = new Observer<Actor>() {

    public void accept(final Actor actor) {
    }
  };
  private static final StandInActor STAND_IN_ACTOR = new StandInActor();

  private BackStage() {
    ConstantConditions.avoid();
  }

  @NotNull
  public static Actor newActor(@NotNull final Script script) {
    return newActor(UUID.randomUUID().toString(), script);
  }

  @NotNull
  public static Actor newActor(@NotNull final String id, @NotNull final Script script) {
    try {
      return newActor(id, script, EMPTY_REMOVER);

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  public static Actor standIn() {
    return STAND_IN_ACTOR;
  }

  @NotNull
  static Actor newActor(@NotNull final String id, @NotNull final Script script,
      @NotNull final Observer<Actor> remover) throws Exception {
    final int quota = script.getQuota(id);
    final Logger logger = script.getLogger(id);
    final ExecutorService executorService = script.getExecutorService(id);
    final Behavior behavior = script.getBehavior(id);
    final LocalContext context = new LocalContext(remover, behavior, executorService, logger);
    final LocalActor actor = new LocalActor(id, quota, context);
    context.setActor(actor);
    return actor;
  }
}
