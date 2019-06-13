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

package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Role;
import dm.shakespeare.actor.SerializableAbstractBehavior;
import dm.shakespeare.actor.SerializableRole;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.log.Logger;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.plot.Event.ActorEvent;
import dm.shakespeare.plot.Story.ActorStory;
import dm.shakespeare.plot.config.BuildConfig;
import dm.shakespeare.plot.function.NullaryFunction;
import dm.shakespeare.plot.function.UnaryFunction;
import dm.shakespeare.plot.memory.ListMemory;
import dm.shakespeare.plot.memory.Memory;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.WeakValueHashMap;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
public class Plot {

  private static final DefaultActorFactory DEFAULT_ACTOR_FACTORY = new DefaultActorFactory();
  private static final DefaultEventFactory DEFAULT_EVENT_FACTORY = new DefaultEventFactory();

  private final Actor actor;
  private final Setting setting =
      new Setting(ExecutorServices.localExecutor(), Role.defaultLogger(this));

  // TODO: 15/02/2019 serialization?
  // TODO: 28/02/2019 swagger converter

  public Plot(@NotNull final Script script) throws Exception {
    this(script, DEFAULT_ACTOR_FACTORY);
  }

  public Plot(@NotNull final Script script,
      @NotNull final UnaryFunction<? super Role, ? extends Actor> actorFactory) throws Exception {
    actor = actorFactory.call(new PlotFactoryRole(script));
  }

  @NotNull
  public <T> Event<T> event(
      @NotNull final NullaryFunction<? extends Event<? extends T>> eventCreator) {
    final Actor actor = this.actor;
    Setting.set(setting);
    try {
      final Event<T> event = new ActorEvent<T>(actor);
      actor.tell(new CreateEvent(eventCreator), null, event.getActor());
      return event;

    } finally {
      Setting.unset();
    }
  }

  @NotNull
  public <T> Story<T> story(
      @NotNull final NullaryFunction<? extends Story<? extends T>> storyCreator) {
    return story(storyCreator, new ListMemory());
  }

  @NotNull
  public <T> Story<T> story(
      @NotNull final NullaryFunction<? extends Story<? extends T>> storyCreator,
      @NotNull final Memory memory) {
    final Actor actor = this.actor;
    Setting.set(setting);
    try {
      final Story<T> story = new ActorStory<T>(actor, memory);
      actor.tell(new CreateEvent(storyCreator), null, story.getActor());
      return story;

    } finally {
      Setting.unset();
    }
  }

  private static class CreateEvent implements NullaryFunction<Event<?>>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final NullaryFunction<? extends Event<?>> creator;

    private CreateEvent() {
      this(DEFAULT_EVENT_FACTORY);
    }

    private CreateEvent(@NotNull final NullaryFunction<? extends Event<?>> creator) {
      this.creator = ConstantConditions.notNull("creator", creator);
    }

    public Event<?> call() throws Exception {
      return creator.call();
    }
  }

  private static class DefaultActorFactory implements UnaryFunction<Role, Actor>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    public Actor call(final Role role) {
      return Stage.newActor(role);
    }

    Object readResolve() throws ObjectStreamException {
      return DEFAULT_ACTOR_FACTORY;
    }
  }

  private static class DefaultEventFactory implements NullaryFunction<Story<?>>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    public Story<?> call() {
      return Story.ofSingleIncident(new IllegalStateException());
    }

    Object readResolve() throws ObjectStreamException {
      return DEFAULT_EVENT_FACTORY;
    }
  }

  private static class PlotFactoryRole extends SerializableRole {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Script script;

    private transient Setting setting;

    private PlotFactoryRole() {
      this(new Script());
    }

    private PlotFactoryRole(@NotNull final Script script) {
      this.script = ConstantConditions.notNull("script", script);
    }

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) throws Exception {
      return getSetting().getExecutor();
    }

    @NotNull
    @Override
    public Logger getLogger(@NotNull final String id) throws Exception {
      return getSetting().getLogger();
    }

    @NotNull
    protected Behavior getSerializableBehavior(@NotNull final String id) throws Exception {
      final Setting setting = getSetting();
      return new SerializableAbstractBehavior() {

        private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

        private final WeakValueHashMap<Actor, Actor> actors = new WeakValueHashMap<Actor, Actor>();
        private final WeakHashMap<Actor, Actor> senders = new WeakHashMap<Actor, Actor>();

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          if (message instanceof CreateEvent) {
            Actor actor;
            Setting.set(setting);
            try {
              actor = ((CreateEvent) message).call().getActor();

            } catch (final Exception e) {
              actor = Story.ofSingleIncident(e).getActor();

            } finally {
              Setting.unset();
            }

            final Actor sender = envelop.getSender();
            senders.put(sender, actor);
            actors.put(actor, sender);

          } else {
            final Actor sender = envelop.getSender();
            Actor actor = senders.get(sender);
            if (actor == null) {
              actor = actors.get(sender);
            }
            if (actor != null) {
              actor.tell(message, envelop.getHeaders().asSentAt(envelop.getSentAt()),
                  agent.getSelf());

            } else {
              sender.tell(new Bounce(), envelop.getHeaders().threadOnly(), agent.getSelf());
            }
            envelop.preventReceipt();
          }
        }
      };
    }

    @NotNull
    private Setting getSetting() throws Exception {
      if (setting == null) {
        final Script script = this.script;
        setting = new Setting(script.getExecutorService(), script.getLogger());
      }
      return setting;
    }
  }
}
