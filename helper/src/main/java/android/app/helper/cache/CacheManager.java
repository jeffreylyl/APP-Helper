package android.app.helper.cache;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class CacheManager extends ICacheManager<String> {
    public boolean DEBUG = true;
    public long cacheTimeInSeconds = 5 * 60;
    public String LOG_TAG = "#####CacheManager";
    private Context mContext;
    private IMemoryCache<CacheMetaData> mMemoryCache;
    private IDiskCache<CacheMetaData> mDiskLruCache;

    public CacheManager(Context context) {
        mContext = context;
        initMemoryCache();
        initDiskLruCache(context);
    }

    public CacheManager(Context context, boolean needMemoryCache, boolean needDiskCache) {
        mContext = context;
        if (needMemoryCache) {
            initMemoryCache();
        }
        if (needDiskCache) {
            initDiskLruCache(context);
        }
        checkCacheExist();
    }

    public CacheManager(Context context, IMemoryCache<CacheMetaData> memoryCache, boolean needMemoryCache, IDiskCache<CacheMetaData> diskCache, boolean needDiskCache) {
        mContext = context;
        if (memoryCache == null && needMemoryCache) {
            initMemoryCache();
        }
        if (diskCache == null && needDiskCache) {
            initDiskLruCache(context);
        }
        checkCacheExist();
    }

    private void checkCacheExist() {
        if (mMemoryCache == null && mDiskLruCache == null) {
            throw new IllegalArgumentException("you should define at least one cache");
        }
    }

    private void initMemoryCache() {
        mMemoryCache = CacheFactory.getDefaultMemoryCache();
    }

    private void initDiskLruCache(Context context) {
        mDiskLruCache = CacheFactory.getDefaultDiskCache(context);
    }

    @Override
    public String get(String key) {
        return get(key, cacheTimeInSeconds);
    }

    @Override
    public void put(String key, String data) {
        addToCache(key, data);
    }

    @Override
    public void clearCache() {
        clearDiskCache();
        clearMemoryCache();
    }

    @Override
    public String get(final String key, long cacheTimeInSeconds) {
        CacheMetaData mRawData = getFromMemoryCache(key);
        if (mRawData == null) {
            mRawData = getFromDiskCache(key);
        }
        if (mRawData != null) {
            boolean outOfDate = mRawData.isOutOfDateFor(cacheTimeInSeconds);
            if (outOfDate) {
                SimpleExecutor.getInstance().execute(new Runnable() {
                    @Override
                    public void run() {
                        remove(key);
                    }
                });

                if (DEBUG) {
                    Log.d(LOG_TAG, String.format("key: %s, cache file out of date", key));
                }
                return null;
            }
            if (DEBUG) {
                Log.d(LOG_TAG, String.format("key: %s, cache file exist", key));
            }
            return mRawData.getCacheData();
        }

        if (DEBUG) {
            Log.d(LOG_TAG, String.format("key: %s, cache file NOT exist", key));
        }
        return null;
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
        SimpleExecutor.getInstance().execute(
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
