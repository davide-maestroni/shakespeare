package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import dm.shakespeare.function.Tester;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.ActorScript;
import dm.shakespeare2.actor.ActorSet;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.Stage;

/**
 * Created by davide-maestroni on 01/08/2019.
 */
public class LocalStage implements Stage {

  private static final StandInActor STAND_IN_ACTOR = new StandInActor();

  private final HashMap<String, Actor> mActors = new HashMap<String, Actor>();
  private final Object mMutex = new Object();

  @NotNull
  public static Actor standIn() {
    return STAND_IN_ACTOR;
  }

  @NotNull
  public ActorSet findAll(@NotNull final Pattern idPattern) {
    return findAll(new PatternTester(ConstantConditions.notNull("idPattern", idPattern)));
  }

  @NotNull
  public ActorSet findAll(@NotNull final Tester<? super Actor> tester) {
    ConstantConditions.notNull("tester", tester);
    final HashSet<Actor> actors;
    synchronized (mMutex) {
      actors = new HashSet<Actor>(mActors.values());
    }

    try {
      final Iterator<Actor> iterator = actors.iterator();
      while (iterator.hasNext()) {
        final Actor actor = iterator.next();
        if ((actor == null) || !tester.test(actor)) {
          iterator.remove();
        }
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    return new LocalActorSet(actors);
  }

  @NotNull
  public Actor findAny(@NotNull final Pattern idPattern) {
    return findAny(new PatternTester(ConstantConditions.notNull("idPattern", idPattern)));
  }

  @NotNull
  public Actor findAny(@NotNull final Tester<? super Actor> tester) {
    ConstantConditions.notNull("tester", tester);
    final ArrayList<Actor> actors;
    synchronized (mMutex) {
      actors = new ArrayList<Actor>(mActors.values());
    }

    try {
      for (final Actor actor : actors) {
        if ((actor != null) && tester.test(actor)) {
          return actor;
        }
      }

    } catch (final RuntimeException e) {
      throw e;

    } catch (final Exception e) {
      throw new RuntimeException(e);
    }

    throw new IllegalArgumentException("cannot find an actor satisfying the tester: " + tester);
  }

  @NotNull
  public Actor get(@NotNull final String id) {
    ConstantConditions.notNull("id", id);
    final Actor actor;
    synchronized (mMutex) {
      actor = mActors.get(id);
    }

    if (actor == null) {
      throw new IllegalArgumentException("cannot find an actor with ID: " + id);
    }
    return actor;
  }

  @NotNull
  public ActorSet getAll() {
    final HashSet<Actor> actors;
    synchronized (mMutex) {
      actors = new HashSet<Actor>(mActors.values());
    }
    actors.remove(null);
    return new LocalActorSet(actors);
  }

  @NotNull
  public Actor newActor(@NotNull final ActorScript script) {
    String id;
    synchronized (mMutex) {
      final HashMap<String, Actor> actors = mActors;
      do {
        id = UUID.randomUUID().toString();
      } while (actors.containsKey(id));
      // reserve ID
      actors.put(id, null);
    }
    return createActor(id, script);
  }

  @NotNull
  public Actor newActor(@NotNull final String id, @NotNull final ActorScript script) {
    synchronized (mMutex) {
      final HashMap<String, Actor> actors = mActors;
      if (actors.containsKey(id)) {
        throw new IllegalStateException("an actor with the same ID already exists: " + id);
      }
      // reserve ID
      actors.put(id, null);
    }
    return createActor(id, script);
  }

  void removeActor(@NotNull final String id) {
    synchronized (mMutex) {
      mActors.remove(id);
    }
  }

  @NotNull
  private Actor createActor(@NotNull final String id, @NotNull final ActorScript script) {
    LocalActor actor;
    try {
      final int quota = script.getQuota(id);
      final Logger logger = script.getLogger(id);
      final ExecutorService executor = script.getExecutor(id);
      final Behavior behavior = script.getBehavior(id);
      final LocalContext context =
          new LocalContext(LocalStage.this, behavior, quota, executor, logger);
      actor = new LocalActor(id, context);
      context.setActor(actor);

    } catch (final RuntimeException e) {
      removeActor(id);
      throw e;

    } catch (final Exception e) {
      removeActor(id);
      throw new RuntimeException(e);
    }

    synchronized (mMutex) {
      mActors.put(id, actor);
    }
    return actor;
  }

  private static class PatternTester implements Tester<Actor> {

    private final Pattern mPattern;

    PatternTester(@NotNull final Pattern idPattern) {
      mPattern = idPattern;
    }

    public boolean test(final Actor actor) {
      return mPattern.matcher(actor.getId()).matches();
    }
  }
}
