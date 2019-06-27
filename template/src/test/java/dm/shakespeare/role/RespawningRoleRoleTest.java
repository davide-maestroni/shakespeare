package dm.shakespeare.role;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Role;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.template.role.RespawningRole;
import dm.shakespeare.template.typed.TypedStage;
import dm.shakespeare.template.typed.actor.Script;
import dm.shakespeare.template.typed.annotation.FromActor;
import dm.shakespeare.template.typed.message.InvocationResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by davide-maestroni on 04/06/2019.
 */
public class RespawningRoleRoleTest {

  @Test
  public void serialization() throws IOException, ClassNotFoundException {
    final RespawningRole role = new RespawningRole(Role.class);
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    objectOutputStream.writeObject(role);
    objectOutputStream.close();
    final ObjectInputStream objectInputStream =
        new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
    final Object o = objectInputStream.readObject();
    assertThat(o).isExactlyInstanceOf(RespawningRole.class);
  }

  @Test
  public void typed() {
    final TypedStage stage = new TypedStage(new Stage());
    final ToUpper test = stage.createActor(ToUpper.class, new TestScript(), "test");
    final Actor actor = Stage.newActor(new Role() {

      @NotNull
      @Override
      public Behavior getBehavior(@NotNull final String id) {
        return new AbstractBehavior() {

          public void onMessage(final Object message, @NotNull final Envelop envelop,
              @NotNull final Agent agent) {
            if (message instanceof InvocationResult) {
              System.out.println(((InvocationResult) message).getResult());
            }
          }
        };
      }

      @NotNull
      @Override
      public ExecutorService getExecutorService(@NotNull final String id) {
        return ExecutorServices.localExecutor();
      }
    });
    test.toUpperCase(actor);
    System.out.println(test.toUpperCase());
  }

  private interface ToUpper {

    void toUpperCase(@FromActor @NotNull Actor sender);

    void toUpperCase(@NotNull Locale locale, @FromActor @NotNull Actor sender);

    String toUpperCase();

    String toUpperCase(@NotNull Locale locale);
  }

  private static class TestScript extends Script {

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }
  }
}
