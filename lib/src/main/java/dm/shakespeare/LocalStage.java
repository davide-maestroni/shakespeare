package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Script;
import dm.shakespeare.function.Observer;
import dm.shakespeare.log.Logger;

/**
 * Created by davide-maestroni on 01/08/2019.
 */
public class LocalStage extends AbstractStage {

  private static final StandInActor STAND_IN_ACTOR = new StandInActor();

  private final Observer<Actor> mRemover = new Observer<Actor>() {

    public void accept(final Actor actor) {
      removeActor(actor.getId());
    }
  };

  @NotNull
  public static Actor standIn() {
    return STAND_IN_ACTOR;
  }

  @NotNull
  protected Actor createActor(@NotNull final String id, @NotNull final Script script) throws
      Exception {
    final int quota = script.getQuota(id);
    final Logger logger = script.getLogger(id);
    final ExecutorService executor = script.getExecutor(id);
    final Behavior behavior = script.getBehavior(id);
    final LocalContext context = new LocalContext(mRemover, behavior, executor, logger);
    final LocalActor actor = new LocalActor(id, quota, context);
    context.setActor(actor);
    addActor(id, actor);
    return actor;
  }
}
