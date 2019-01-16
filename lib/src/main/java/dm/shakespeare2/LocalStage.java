package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.log.Logger;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.ActorScript;
import dm.shakespeare2.actor.Behavior;

/**
 * Created by davide-maestroni on 01/08/2019.
 */
public class LocalStage extends AbstractStage {

  private static final StandInActor STAND_IN_ACTOR = new StandInActor();

  @NotNull
  public static Actor standIn() {
    return STAND_IN_ACTOR;
  }

  @NotNull
  protected Actor createActor(@NotNull final String id, @NotNull final ActorScript script) throws
      Exception {
    final int quota = script.getQuota(id);
    final Logger logger = script.getLogger(id);
    final ExecutorService executor = script.getExecutor(id);
    final Behavior behavior = script.getBehavior(id);
    final LocalContext context =
        new LocalContext(LocalStage.this, behavior, quota, executor, logger);
    final LocalActor actor = new LocalActor(id, context);
    context.setActor(actor);
    addActor(id, actor);
    return actor;
  }
}
