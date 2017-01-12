package android.app.helper.cache.cachemanager;

import android.app.helper.cache.CacheFactory;
import android.app.helper.cache.CacheMetaData;
import android.app.helper.cache.diskcache.IDiskCache;
import android.app.helper.cache.keygenerator.IKeyGenerator;
import android.app.helper.cache.memorycache.IMemoryCache;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.concurrent.Executor;

public class CacheManager extends ICacheManager<String> {
    public boolean DEBUG = true;
    public long cacheTimeInSeconds = 5 * 60;
    public String LOG_TAG = "#####CacheManager";
    private Context mContext;
    private IMemoryCache<CacheMetaData> mMemoryCache;
    private IDiskCache<CacheMetaData> mDiskLruCache;
    private IKeyGenerator keyGenerator;
    private CacheManagerConfiguration cacheManagerConfiguration;
    private Executor taskExecutor;

    public CacheManager(Context context) {
        mContext = context.getApplicationContext();
        init(CacheManagerConfiguration.createDefault(context));
        checkCacheExist();
    }

    public CacheManager(Context context, boolean disableMemoryCache, boolean disableDiskCache) {
        checkCacheAbility(disableMemoryCache, disableDiskCache);
        mContext = context.getApplicationContext();
        CacheManagerConfiguration.Builder builder = new CacheManagerConfiguration.Builder(context);
        init(builder.disableDiskCache(disableDiskCache).disableMemoryCache(disableMemoryCache).build());
    }

    public CacheManager(Context context, IMemoryCache<CacheMetaData> memoryCache, boolean disableMemoryCache, IDiskCache<CacheMetaData> diskCache, boolean disableDiskCache) {
        checkCacheAbility(disableMemoryCache, disableDiskCache);
        mContext = context.getApplicationContext();
        CacheManagerConfiguration.Builder builder = new CacheManagerConfiguration.Builder(context);
        if (memoryCache == null && !disableMemoryCache) {
            builder = builder.memoryCache(memoryCache);
        }
        if (diskCache == null && !disableDiskCache) {
            builder = builder.diskCache(diskCache);
        }
        init(builder.build());
    }

    private void checkCacheAbility(boolean disableMemoryCache, boolean disableDiskCache) {
        if (disableMemoryCache && disableDiskCache) {
            throw new IllegalArgumentException("you should define at least one cache type");
        }
    }

    private void checkCacheExist() {
        checkCacheAbility(mMemoryCache == null, mDiskLruCache == null);
    }

    @Deprecated
    private void initMemoryCache() {
        mMemoryCache = CacheFactory.getDefaultMemoryCache();
    }

    @Deprecated
    private void initDiskLruCache(Context context) {
        mDiskLruCache = CacheFactory.getDefaultDiskCache(context);
    }

    @Override
    public String get(String key) {
        return get(key, cacheTimeInSeconds);
    }

    @Override
    public void put(String key, String data) {
        addToCache(keyGenerator.generate(key), data);
    }

    @Override
    public void clearCache() {
        clearDiskCache();
        clearMemoryCache();
    }

    @Override
    public String get(String key, long cacheTimeInSeconds) {
        final String cacheKey = keyGenerator.generate(key);
        CacheMetaData mRawData = getFromMemoryCache(cacheKey);
        if (mRawData == null) {
            mRawData = getFromDiskCache(cacheKey);
        }
        if (mRawData != null) {
            boolean outOfDate = mRawData.isOutOfDateFor(cacheTimeInSeconds);
            if (outOfDate) {
                taskExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        remove(cacheKey);
                    }
                });

                if (DEBUG) {
                    Log.d(LOG_TAG, String.format("key: %s, cache file out of date", cacheKey));
                }
                return null;
            }
            if (DEBUG) {
                Log.d(LOG_TAG, String.format("key: %s, cache file exist", cacheKey));
            }
            return mRawData.getData();
        }

        if (DEBUG) {
            Log.d(LOG_TAG, String.format("key: %s, cache file NOT exist", cacheKey));
        }
        return null;
    }

    @Override
    public void init(CacheManagerConfiguration cacheManagerConfiguration) {
        if (cacheManagerConfiguration == null) {
            throw new IllegalArgumentException("CacheManagerConfiguration required ");
        }
        if (this.cacheManagerConfiguration == null) {
            this.mDiskLruCache = cacheManagerConfiguration.diskCache;
            this.mMemoryCache = cacheManagerConfiguration.memoryCache;
            this.keyGenerator = cacheManagerConfiguration.keyGenerator;
            this.taskExecutor = cacheManagerConfiguration.taskExecutor;
        }
    }

    private CacheMetaData getFromMemoryCache(String key) {
        if (mMemoryCache != null) {
            return mMemoryCache.get(key);
        }
        return null;
    }

    private CacheMetaData getFromDiskCache(String key) {
        if (mDiskLruCache != null) {
            return mDiskLruCache.get(key);
        }
        return null;
    }

    private void addToCache(final String key, final String data) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(data)) {
            return;
        }
        if (DEBUG) {
            Log.d(LOG_TAG, String.format("key: %s, addToCache", key));
        }
        taskExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        CacheMetaData cacheMetaData = CacheMetaData.createForNow(data);
                        addToMemroyCache(key, cacheMetaData);
                        addToDiskCache(key, cacheMetaData);
                    }
                }
        );
    }

    private void addToMemroyCache(String key, CacheMetaData data) {
        if (mMemoryCache != null) {
            mMemoryCache.put(key, data);
        }
    }

    private void addToDiskCache(String key, CacheMetaData cacheMetaData) {
        if (mDiskLruCache != null) {
            mDiskLruCache.put(key, cacheMetaData);
        }
    }

    @Override
    public boolean remove(String key) {
        key = keyGenerator.generate(key);

        if (mDiskLruCache == null) {
            return removeFromMemoryCache(key);
        }

        if (mMemoryCache == null) {
            return removeFromDiskCache(key);
        }

        return removeFromDiskCache(key) && removeFromMemoryCache(key);
    }

    @Override
    public double getCacheSize() {
        double size = 0;
        if (mDiskLruCache != null) {
            size += mDiskLruCache.getCacheSize();
        }
        if (mMemoryCache != null) {
            size += mMemoryCache.getCacheSize();
        }
        return size;
    }

    private boolean removeFromMemoryCache(String key) {
        return mMemoryCache.remove(key);
    }

    private boolean removeFromDiskCache(String key) {
        return mDiskLruCache.remove(key);
    }

    public void clearMemoryCache() {
        if (mMemoryCache != null) {
            mMemoryCache.clearCache();
        }
    }

    public void clearDiskCache() {
        if (mDiskLruCache != null) {
            mDiskLruCache.clearCache();
        }
    }
}
