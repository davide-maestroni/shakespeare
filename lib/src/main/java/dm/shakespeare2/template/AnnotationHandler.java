package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import dm.shakespeare2.actor.BehaviorBuilder;

/**
 * Created by davide-maestroni on 09/07/2018.
 */
interface AnnotationHandler<T extends Annotation> {

  void handle(@NotNull BehaviorBuilder builder, @NotNull Object object, @NotNull Method method,
      @NotNull T annotation) throws Exception;
}
