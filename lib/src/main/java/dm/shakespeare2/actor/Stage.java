package dm.shakespeare2.actor;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

import dm.shakespeare.function.Tester;

/**
 * Created by davide-maestroni on 09/10/2018.
 */
public interface Stage {

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
  ActorSet getAll();

  @NotNull
  Actor newActor(@NotNull ActorScript script);

  @NotNull
  Actor newActor(@NotNull String id, @NotNull ActorScript script);
}
