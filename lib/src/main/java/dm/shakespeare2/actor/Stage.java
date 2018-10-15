package dm.shakespeare2.actor;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

import dm.shakespeare2.actor.Actor.ActorSet;
import dm.shakespeare2.function.Mapper;
import dm.shakespeare2.function.Tester;

/**
 * Created by davide-maestroni on 09/10/2018.
 */
public interface Stage {

  // TODO: 22/09/2018 plugin

  void addMonitor(@NotNull Actor monitor);

  @NotNull
  ActorSet findAll(@NotNull Pattern idPattern);

  @NotNull
  ActorSet findAll(@NotNull Tester<? super Actor> tester);

  @NotNull
  Actor findAny(@NotNull Pattern idPattern);

  @NotNull
  Actor findAny(@NotNull Tester<? super Actor> tester);

  @NotNull
  Actor get(@NotNull String id);

  @NotNull
  String getName();

  @NotNull
  Actor getOrCreate(@NotNull String id,
      @NotNull Mapper<? super ActorBuilder, ? extends Actor> mapper);

  @NotNull
  ActorBuilder newActor();

  void removeMonitor(@NotNull Actor monitor);
}
