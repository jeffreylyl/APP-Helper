package android.app.helper.cache.cachemanager;

import android.content.Context;

public class CacheManagerFactory {
    private static ICacheManager cacheManager;

    public static ICacheManager getInstance(Context context) {
        if (cacheManager == null) {
            synchronized (CacheManagerFactory.class) {
                if (cacheManager == null) {
                    cacheManager = new CacheManager(context);
                }
            }
        }
        return cacheManager;
    }
}
