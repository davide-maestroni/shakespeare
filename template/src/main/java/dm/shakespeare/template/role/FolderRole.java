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

import java.io.Serializable;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Role;
import dm.shakespeare.actor.SerializableRole;
import dm.shakespeare.log.Logger;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.template.util.Reflections;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 03/21/2019.
 */
public class FolderRole extends SerializableRole {

  private static final Object[] NO_ARGS = new Object[0];
  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  private final Serializable[] mRoleArgs;
  private final Class<? extends Role> mRoleClass;

  private transient Role mRole;

  public FolderRole(@NotNull final Class<? extends Role> roleClass) {
    mRoleClass = ConstantConditions.notNull("roleClass", roleClass);
    mRoleArgs = null;
  }

  public FolderRole(@NotNull final Class<? extends Role> roleClass,
      @NotNull final Serializable... roleArgs) {
    mRoleClass = ConstantConditions.notNull("roleClass", roleClass);
    mRoleArgs = ConstantConditions.notNull("roleArgs", roleArgs).clone();
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) throws Exception {
    return getRoleInstance().getBehavior(id);
  }

  @NotNull
  @Override
  public ExecutorService getExecutorService(@NotNull final String id) throws Exception {
    return getRoleInstance().getExecutorService(id);
  }

  @NotNull
  @Override
  public Logger getLogger(@NotNull final String id) throws Exception {
    return getRoleInstance().getLogger(id);
  }

  @Override
  public int getQuota(@NotNull final String id) throws Exception {
    return getRoleInstance().getQuota(id);
  }

  @NotNull
  protected Role newRoleInstance() {
    final Serializable[] roleArgs = mRoleArgs;
    return Reflections.newInstance(mRoleClass,
        ((roleArgs != null) && (roleArgs.length > 0)) ? roleArgs : NO_ARGS);
  }

  @NotNull
  private Role getRoleInstance() {
    if (mRole == null) {
      mRole = newRoleInstance();
    }
    return mRole;
  }
}
