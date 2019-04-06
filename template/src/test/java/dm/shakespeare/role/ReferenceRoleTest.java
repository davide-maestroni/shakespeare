package dm.shakespeare.role;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import dm.shakespeare.template.role.ReferenceRole;
import dm.shakespeare.template.role.ReflectionRole;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by davide-maestroni on 04/06/2019.
 */
public class ReferenceRoleTest {

  @Test
  public void serialization() throws IOException, ClassNotFoundException {
    final ReferenceRole role = new ReferenceRole(ReflectionRole.class, "");
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    objectOutputStream.writeObject(role);
    objectOutputStream.close();
    final ObjectInputStream objectInputStream =
        new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
    final Object o = objectInputStream.readObject();
    assertThat(o).isExactlyInstanceOf(ReferenceRole.class);
  }
}
