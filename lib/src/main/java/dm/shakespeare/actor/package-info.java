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

/**
 * Library core interfaces and classes definitions.<br>
 * The library main concepts include:<ul>
 * <li>{@link dm.shakespeare.actor.Behavior}: implementing the actor behavior</li>
 * <li>{@link dm.shakespeare.actor.Behavior.Agent}: providing the actor execution environment</li>
 * <li>{@link dm.shakespeare.actor.Role}: defining the actor behavior and options</li>
 * <li>{@link dm.shakespeare.actor.Actor Actor}: defining the actor interface</li>
 * </ul>
 * This package provides also implementations of utility classes, like: an
 * {@link dm.shakespeare.actor.AbstractBehavior}, providing empty implementation or
 * {@link dm.shakespeare.actor.Behavior#onStart(dm.shakespeare.actor.Behavior.Agent)} and
 * {@link dm.shakespeare.actor.Behavior#onStop(dm.shakespeare.actor.Behavior.Agent)} methods
 * (and its serializable version {@link dm.shakespeare.actor.SerializableAbstractBehavior});
 * an {@link dm.shakespeare.actor.AcceptHandler} and {@link dm.shakespeare.actor.ApplyHandler},
 * wrapping generic function into message handlers; and a
 * {@link dm.shakespeare.actor.SerializableRole}, defining a {@code Role} object which is also
 * serializable.
 */
package dm.shakespeare.actor;
