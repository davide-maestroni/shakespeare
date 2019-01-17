package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/07/2018.
 */
class Methods {

  private Methods() {
    ConstantConditions.avoid();
  }

  /**
   * Makes the specified method accessible.
   *
   * @param method the method instance.
   * @return the method.
   */
  @NotNull
  public static Method makeAccessible(@NotNull final Method method) {
    if (!method.isAccessible()) {
      AccessController.doPrivileged(new SetAccessibleMethodAction(method));
    }
    return method;
  }

  /**
   * Privileged action used to grant accessibility to a method.
   */
  private static class SetAccessibleMethodAction implements PrivilegedAction<Void> {

    private final Method mMethod;

    /**
     * Constructor.
     *
     * @param method the method instance.
     */
    private SetAccessibleMethodAction(@NotNull final Method method) {
      mMethod = method;
    }

    public Void run() {
      mMethod.setAccessible(true);
      return null;
    }
  }
}
