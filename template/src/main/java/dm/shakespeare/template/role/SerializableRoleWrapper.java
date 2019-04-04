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

package dm.shakespeare.template.role;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Role;
import dm.shakespeare.actor.SerializableRole;
import dm.shakespeare.log.Logger;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/16/2019.
 */
public class SerializableRoleWrapper extends SerializableRole {

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  private transient Role mRole;

  public SerializableRoleWrapper(@NotNull final Role role) {
    mRole = ConstantConditions.notNull("role", role);
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) throws Exception {
    return mRole.getBehavior(id);
  }

  @NotNull
  @Override
  public ExecutorService getExecutorService(@NotNull final String id) throws Exception {
    return mRole.getExecutorService(id);
  }

  @NotNull
  @Override
  public Logger getLogger(@NotNull final String id) throws Exception {
    return mRole.getLogger(id);
  }

  @Override
  public int getQuota(@NotNull final String id) throws Exception {
    return mRole.getQuota(id);
  }

  @SuppressWarnings("unchecked")
  private void readObject(@NotNull final ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    in.defaultReadObject();
    mRole = (Role) in.readObject();
  }

  private void writeObject(@NotNull final ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeObject(mRole);
  }
}
