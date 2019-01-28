package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Script;
import dm.shakespeare.function.Observer;

/**
 * Created by davide-maestroni on 01/08/2019.
 */
public class LocalStage extends AbstractStage {

  private final Observer<Actor> mRemover = new Observer<Actor>() {

    public void accept(final Actor actor) {
      removeActor(actor.getId());
    }
  };

  @NotNull
  protected Actor createActor(@NotNull final String id, @NotNull final Script script) throws
      Exception {
    final Actor actor = BackStage.newActor(id, script, mRemover);
    addActor(id, actor);
    return actor;
  }
}
