package dm.shakespeare.remote;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import dm.shakespeare.actor.Actor.Envelop;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public interface RemoteAdapter {

  @NotNull
  String createActor(@Nullable String actorId, @NotNull InputStream data) throws Exception;

  @NotNull
  StageDescription describeStage() throws Exception;

  @NotNull
  Set<String> getCodeEntries(@NotNull Map<String, String> hashes) throws Exception;

  void sendCodeEntries(@NotNull InputStream data) throws Exception;

  void sendMessage(@NotNull String actorId, Object message, @NotNull Envelop envelop) throws
      Exception;

  interface StageDescription {

    Collection<String> getActors();

    Map<String, String> getCapabilities();
  }
}
