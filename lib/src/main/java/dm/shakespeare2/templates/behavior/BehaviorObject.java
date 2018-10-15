package dm.shakespeare2.templates.behavior;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import dm.shakespeare2.Shakespeare;
import dm.shakespeare2.actor.BehaviorBuilder;
import dm.shakespeare2.templates.annotation.OnAny;
import dm.shakespeare2.templates.annotation.OnMatch;
import dm.shakespeare2.templates.annotation.OnMessage;
import dm.shakespeare2.templates.annotation.OnNoMatch;
import dm.shakespeare2.templates.annotation.OnParams;
import dm.shakespeare2.templates.annotation.OnSender;
import dm.shakespeare2.templates.annotation.OnStart;
import dm.shakespeare2.templates.annotation.OnStop;
import dm.shakespeare2.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/27/2018.
 */
class BehaviorObject {

  private static final HashMap<Class<? extends Annotation>, AnnotationHandler<?>>
      sAnnotationHandlers = new HashMap<Class<? extends Annotation>, AnnotationHandler<?>>() {{
    put(OnStart.class, new OnStartHandler());
    put(OnStop.class, new OnStopHandler());
    put(OnParams.class, new OnParamsHandler());
    put(OnAny.class, new OnAnyHandler());
    put(OnMatch.class, new OnMatchHandler());
    put(OnMessage.class, new OnMessageHandler());
    put(OnSender.class, new OnSenderHandler());
    put(OnNoMatch.class, new OnNoMatchHandler());
  }};

  private BehaviorObject() {
    ConstantConditions.avoid();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  static BehaviorBuilder newBehavior(@NotNull final Object object) {
    final Class<?> objectClass = object.getClass();
    final BehaviorBuilder builder = Shakespeare.newBehavior();
    final Set<Entry<Class<? extends Annotation>, AnnotationHandler<?>>> entries =
        sAnnotationHandlers.entrySet();
    for (final Method method : objectClass.getMethods()) {
      for (final Entry<Class<? extends Annotation>, AnnotationHandler<?>> entry : entries) {
        final Annotation annotation = method.getAnnotation(entry.getKey());
        if (annotation != null) {
          ((AnnotationHandler<Annotation>) entry.getValue()).handle(builder, object, method,
              annotation);
        }
      }
    }

    return builder;
  }
}
