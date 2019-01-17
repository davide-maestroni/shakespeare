package dm.shakespeare2.template.annotation;

import dm.shakespeare.function.Tester;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/06/2018.
 */
public class VoidTester implements Tester<Object> {

  private VoidTester() {
    ConstantConditions.avoid();
  }

  public boolean test(final Object value) {
    return false;
  }
}
