package dm.shakespeare2.remote;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.function.Observer;

/**
 * Created by davide-maestroni on 10/01/2018.
 */
public interface Remote {

  void createActor(String actorId, InputStream data) throws Exception;

  void describeStage(Observer<StageDescription> observer) throws Exception;

  void getCodeEntries(Map<String, String> hashes, Observer<Collection<String>> observer) throws
      Exception;

  void sendCodeEntries(Map<String, InputStream> data) throws Exception;

  void sendMessage(String actorId, Object message, Actor sender) throws Exception;

  interface StageDescription {

    Collection<String> getActors();

    Map<String, String> getCapabilities();
  }
}
