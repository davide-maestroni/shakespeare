package dm.shakespeare.plot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.actor.Script;
import dm.shakespeare.executor.ExecutorServices;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.DoubleQueue;
import dm.shakespeare.util.WeakIdentityHashMap;

/**
 * Created by davide-maestroni on 01/25/2019.
 */
class Setting {

  private static final ThreadLocal<DoubleQueue<Setting>> sLocalSetting =
      new ThreadLocal<DoubleQueue<Setting>>() {

        @Override
        protected DoubleQueue<Setting> initialValue() {
          return new DoubleQueue<Setting>();
        }
      };

  private final HashMap<Class<?>, Cache> mCaches = new HashMap<Class<?>, Cache>();
  private final ExecutorService mExecutor;
  private final Logger mLogger;

  private ExecutorService mTrampolineExecutor;

  Setting(@Nullable final ExecutorService executor, @Nullable final Logger logger) {
    mExecutor =
        (executor != null) ? asActorExecutor(executor) : asActorExecutor(Script.defaultExecutor());
    mLogger = logger;
  }

  @NotNull
  static Setting get() {
    try {
      return sLocalSetting.get().get(0);

    } catch (final IndexOutOfBoundsException e) {
      throw new IllegalStateException("code is not running inside a Play scope");
    }
  }

  static void set(@NotNull final Setting context) {
    sLocalSetting.get().add(ConstantConditions.notNull("context", context));
  }

  static void unset() {
    sLocalSetting.get().removeLast();
  }

  @NotNull
  private static ExecutorService asActorExecutor(@NotNull final ExecutorService executor) {
    return (executor instanceof ScheduledExecutorService) ? ExecutorServices.asActorExecutor(
        (ScheduledExecutorService) executor) : ExecutorServices.asActorExecutor(executor);
  }

  @NotNull
  Cache getCache(@NotNull final Class<?> type) {
    Cache cache = mCaches.get(type);
    if (cache == null) {
      cache = new Cache();
      mCaches.put(type, cache);
    }
    return cache;
  }

  @Nullable
  ExecutorService getExecutor() {
    return mExecutor;
  }

  @Nullable
  Logger getLogger() {
    return mLogger;
  }

  @NotNull
  ExecutorService getTrampolineExecutor() {
    if (mTrampolineExecutor == null) {
      mTrampolineExecutor = asActorExecutor(ExecutorServices.trampolineExecutor());
    }
    return mTrampolineExecutor;
  }

  static class Cache {

    private final WeakIdentityHashMap<Object, WeakReference<Object>> mData =
        new WeakIdentityHashMap<Object, WeakReference<Object>>();

    private Cache() {
    }

    @Nullable
    @SuppressWarnings("unchecked")
    <T> T get(@NotNull final Object key) {
      final WeakReference<Object> reference = mData.get(key);
      Object value = (reference != null) ? reference.get() : null;
      if (value == null) {
        mData.remove(key);
      }
      return (T) value;
    }

    <T> void put(@NotNull final Object key, final T value) {
      mData.put(key, new WeakReference<Object>(value));
    }
  }
}
