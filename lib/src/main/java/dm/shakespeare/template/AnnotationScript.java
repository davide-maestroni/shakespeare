package dm.shakespeare.template;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.actor.Script;
import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.template.annotation.OnAny;
import dm.shakespeare.template.annotation.OnMatch;
import dm.shakespeare.template.annotation.OnMessage;
import dm.shakespeare.template.annotation.OnNoMatch;
import dm.shakespeare.template.annotation.OnParams;
import dm.shakespeare.template.annotation.OnSender;
import dm.shakespeare.template.annotation.OnStart;
import dm.shakespeare.template.annotation.OnStop;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/17/2019.
 */
public class AnnotationScript extends Script {

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

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

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
