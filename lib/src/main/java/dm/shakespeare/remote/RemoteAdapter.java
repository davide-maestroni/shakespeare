package dm.shakespeare.remote;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import dm.shakespeare.actor.Actor;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public interface RemoteAdapter {

  void createActor(String actorId, InputStream data) throws Exception;

  void describeStage(dm.shakespeare.function.Observer<? super StageDescription> observer) throws Exception;

  void getCodeEntries(Map<String, String> hashes, dm.shakespeare.function.Observer<? super Set<String>> observer) throws
      Exception;

  void sendCodeEntries(Map<String, ? extends InputStream> data) throws Exception;

  void sendMessage(String actorId, Object message, Actor sender) throws Exception;

  interface StageDescription {

    Collection<String> getActors();

    Map<String, String> getCapabilities();
  }
}
