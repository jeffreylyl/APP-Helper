package android.app.helper.task.imp;

import android.app.helper.task.ICacheStore;
import android.support.v4.util.LruCache;


public class MemoryCacheStore implements ICacheStore {

    private LruCache<String, Cache> lruCache;

    public MemoryCacheStore(int maxSize) {
        lruCache = new LruCache<String, Cache>(maxSize) {
            @Override
            protected int sizeOf(String key, Cache value) {
                return 1;
            }
        };
    }

    @Override
    public Cache getCache(String taskKey) {
        return lruCache.get(taskKey);
    }

    @Override
    public void saveCache(String taskKey, long requestTime, long saveTime, Object result) {
        lruCache.put(taskKey, new Cache(requestTime, saveTime, result));
    }
}