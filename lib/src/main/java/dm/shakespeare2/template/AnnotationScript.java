package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.ActorScript;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.BehaviorBuilder;
import dm.shakespeare2.template.annotation.OnAny;
import dm.shakespeare2.template.annotation.OnMatch;
import dm.shakespeare2.template.annotation.OnMessage;
import dm.shakespeare2.template.annotation.OnNoMatch;
import dm.shakespeare2.template.annotation.OnParams;
import dm.shakespeare2.template.annotation.OnSender;
import dm.shakespeare2.template.annotation.OnStart;
import dm.shakespeare2.template.annotation.OnStop;

/**
 * Created by davide-maestroni on 01/17/2019.
 */
public class AnnotationScript extends ActorScript {

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

  private final Object mObject;

  public AnnotationScript() {
    mObject = this;
  }

  public AnnotationScript(@NotNull final Object object) {
    mObject = ConstantConditions.notNull("object", object);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public Behavior getBehavior(@NotNull final String id) throws Exception {
    final Object object = mObject;
    final Class<?> objectClass = object.getClass();
    final BehaviorBuilder builder = newBehavior();
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
    return builder.build();
  }
}
