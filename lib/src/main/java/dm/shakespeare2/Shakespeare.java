package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.concurrent.ExecutorService;

import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.ActorBuilder;
import dm.shakespeare2.actor.BehaviorBuilder;
import dm.shakespeare2.actor.Stage;
import dm.shakespeare2.function.Mapper;
import dm.shakespeare2.log.Logger;

/**
 * Created by davide-maestroni on 06/04/2018.
 */
public class Shakespeare {

  private static final String[] FIRST =
      {"alliance", "ambassador", "apollo", "arcane", "celeste", "celestial", "classic", "exalted",
       "golden", "grand", "guardian", "legacy", "lion", "little", "major", "pilgrim", "regal",
       "rose", "solar", "spring", "utopia", "white"};
  private static final String[] SECOND =
      {"ambition", "angel", "avenue", "blossom", "citadel", "emerald", "enigma", "flare", "garden",
       "gate", "heart", "light", "petal", "paragon", "phoenix", "prime", "prodigy", "shrine",
       "spirit", "supremacy", "tiara", "universe"};

  private static final Random sRandom = new Random();

  @NotNull
  public static Stage backStage() {
    return BackStage.defaultInstance();
  }

  @NotNull
  public static Mapper<? super String, ? extends ExecutorService> defaultExecutorMapper() {
    return DefaultStage.defaultExecutorMapper();
  }

  @NotNull
  public static Mapper<? super String, ? extends Logger> defaultLoggerMapper() {
    return DefaultStage.defaultLoggerMapper();
  }

  @NotNull
  public static ActorBuilder newActor() {
    return backStage().newActor();
  }

  @NotNull
  public static BehaviorBuilder newBehavior() {
    return new DefaultBehaviorBuilder();
  }

  @NotNull
  public static Stage newStage(@NotNull final String name) {
    return new DefaultStage(name);
  }

  @NotNull
  public static Stage newStage() {
    return newStage(generateStageName());
  }

  @NotNull
  public static Actor standIn() {
    return StandInActor.defaultInstance();
  }

  @NotNull
  private static String generateStageName() {
    return FIRST[sRandom.nextInt(FIRST.length)] + "-" + SECOND[sRandom.nextInt(SECOND.length)];
  }
}
