package android.app.helper.cache;

import android.content.Context;
import android.support.v4.util.LruCache;

import java.io.File;
import java.io.IOException;

public class CacheFactory {
    public static IMemoryCache<CacheMetaData> getDefaultMemoryCache(){
        return new DefaultMemoryCache();
    }
    public static IDiskCache<CacheMetaData> getDefaultDiskCache(Context context){
        return new DefaultDiskCache(context);
    }

    public static class DefaultDiskCache implements IDiskCache<CacheMetaData>{
        private DiskLruCache mDiskLruCache;
        private Context mContext;

        public DefaultDiskCache(Context context){
            initDiskLruCache(context);
            mContext = context;
        }

        public void initDiskLruCache(Context context) {
            try {
                File cacheDir = Utils.getDiskCacheDir(context, "thumb");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }
                mDiskLruCache = DiskLruCache
                        .open(cacheDir, Utils.getAppVersion(context), 1, 10 * 1024 * 1024);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void clearCache() {
            if (!mDiskLruCache.isClosed() && mDiskLruCache != null) {
                try {
                    mDiskLruCache.delete();
                    initDiskLruCache(mContext);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void put(String key, CacheMetaData cacheMetaData) {
            DiskLruCache.Editor editor = null;
            try {
                editor = mDiskLruCache.edit(key);
                if (editor != null && !mDiskLruCache.isClosed()) {
                    editor.set(0, cacheMetaData.getCacheData());
                    editor.commit();
                    flushCache();
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    editor.abort();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        private void flushCache() {
            if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                try {
                    mDiskLruCache.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        @Override
        public CacheMetaData get(String key) {
            DiskLruCache.Snapshot snapshot = null;
            try {
                snapshot = mDiskLruCache.get(key);
                if (snapshot != null) {
                    String cacheContent = snapshot.getString(0);
                    JsonData jsonData = JsonData.create(cacheContent);
                    return CacheMetaData.createFromJson(jsonData);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public boolean remove(String key) {
            try {
                return mDiskLruCache.remove(key);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public double getCacheSize() {
            return mDiskLruCache.size();
        }
    }

    public static class DefaultMemoryCache implements IMemoryCache<CacheMetaData>{
        private LruCache<String, CacheMetaData> mMemoryCache;

        public DefaultMemoryCache(){
            int maxMemoery = (int) Runtime.getRuntime().maxMemory();
            int cacheSize = maxMemoery / 16; // memory cache size
            mMemoryCache = new LruCache<String, CacheMetaData>(cacheSize) {
                @Override
                protected int sizeOf(String key, CacheMetaData cacheMetaData) {
                    return (cacheMetaData.getSize() + key.getBytes().length);
                }
            };
        }

        @Override
        public void clearCache() {
            if (mMemoryCache != null) {
                mMemoryCache.evictAll();
            }
        }

        @Override
        public void put(String key, CacheMetaData cacheMetaData) {
            mMemoryCache.put(key, cacheMetaData);
        }

        @Override
        public CacheMetaData get(String key) {
            return mMemoryCache.get(key);
        }

        @Override
        public boolean remove(String key) {
            CacheMetaData mRawData = mMemoryCache.remove(key);
            return mRawData == null ? false : true;
        }

        @Override
        public double getCacheSize() {
            return mMemoryCache.size();
        }
    }
}
