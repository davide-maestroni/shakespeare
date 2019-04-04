/*
 * Copyright 2019 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dm.shakespeare.template.actor;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Role;
import dm.shakespeare.template.annotation.OnAny;
import dm.shakespeare.template.annotation.OnEnvelop;
import dm.shakespeare.template.annotation.OnMatch;
import dm.shakespeare.template.annotation.OnMessage;
import dm.shakespeare.template.annotation.OnNoMatch;
import dm.shakespeare.template.annotation.OnParams;
import dm.shakespeare.template.annotation.OnStart;
import dm.shakespeare.template.annotation.OnStop;

/**
 * Created by davide-maestroni on 03/24/2019.
 */
public class AnnotationBehavior extends AbstractBehavior {

  private static final HashMap<Class<? extends Annotation>, AnnotationHandler<?>>
      sAnnotationHandlers = new HashMap<Class<? extends Annotation>, AnnotationHandler<?>>() {{
    put(OnStart.class, new OnStartHandler());
    put(OnStop.class, new OnStopHandler());
    put(OnParams.class, new OnParamsHandler());
    put(OnAny.class, new OnAnyHandler());
    put(OnMatch.class, new OnMatchHandler());
    put(OnMessage.class, new OnMessageHandler());
    put(OnEnvelop.class, new OnEnvelopHandler());
    put(OnNoMatch.class, new OnNoMatchHandler());
  }};

  private final Behavior mBehavior;

  @SuppressWarnings("unchecked")
  AnnotationBehavior(@NotNull final Object object) throws Exception {
    final Class<?> objectClass = object.getClass();
    final BehaviorBuilder builder = Role.newBehavior();
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
    mBehavior = builder.build();
  }

  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Context context) throws Exception {
    mBehavior.onMessage(message, envelop, context);
  }
}
