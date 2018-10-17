package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Set;

import dm.shakespeare.actor.Actor;

/**
 * Created by davide-maestroni on 09/12/2018.
 */
class BackStage extends DefaultStage {

  private static final BackStage sInstance = new BackStage();

  private BackStage() {
    super("back", new EmptyMap());
  }

  @NotNull
  static BackStage defaultInstance() {
    return sInstance;
  }

  private static class EmptyMap extends AbstractMap<String, Actor> {

    @Override
    public Actor put(final String id, final Actor actor) {
      return null;
    }

    @NotNull
    @Override
    public Set<Entry<String, Actor>> entrySet() {
      return Collections.emptySet();
    }
  }
}
