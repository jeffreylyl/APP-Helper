package android.app.helper.cache.cachemanager;

import android.app.helper.cache.CacheFactory;
import android.app.helper.cache.CacheMetaData;
import android.app.helper.cache.SimpleExecutor;
import android.app.helper.cache.diskcache.IDiskCache;
import android.app.helper.cache.keygenerator.IKeyGenerator;
import android.app.helper.cache.keygenerator.Md5KeyGenerator;
import android.app.helper.cache.memorycache.IMemoryCache;
import android.content.Context;

import java.util.concurrent.Executor;

public class CacheManagerConfiguration {

    IDiskCache<CacheMetaData> diskCache;
    IMemoryCache<CacheMetaData> memoryCache;
    Executor taskExecutor;
    IKeyGenerator keyGenerator;

    private CacheManagerConfiguration(final Builder builder) {
        taskExecutor = builder.taskExecutor;
        diskCache = builder.diskCache;
        keyGenerator = builder.keyGenerator;
        memoryCache = builder.memoryCache;
    }

    public static CacheManagerConfiguration createDefault(Context context) {
        return new Builder(context).build();
    }

    public static class Builder {
        private Executor taskExecutor = null;
        private IDiskCache<CacheMetaData> diskCache;
        private IMemoryCache<CacheMetaData> memoryCache;
        private IKeyGenerator keyGenerator;
        private int memoryCacheSize = 0;
        private long diskCacheSize = 0;
        private boolean disableDiskCache = false;
        private boolean disableMemoryCache = false;
        private Context context;

        public Builder memoryCacheSize(int memoryCacheSize) {
            if (memoryCacheSize <= 0)
                throw new IllegalArgumentException("memoryCacheSize must be a positive number");
            this.memoryCacheSize = memoryCacheSize;
            return this;
        }

        public Builder memoryCache(IMemoryCache<CacheMetaData> memoryCache) {
            this.memoryCache = memoryCache;
            return this;
        }

        public Builder diskCacheSize(int maxCacheSize) {
            if (maxCacheSize <= 0)
                throw new IllegalArgumentException("maxCacheSize must be a positive number");

            this.diskCacheSize = maxCacheSize;
            return this;
        }

        public Builder diskCache(IDiskCache<CacheMetaData> diskCache) {
            this.diskCache = diskCache;
            return this;
        }

        public Builder diskCacheKeyGenerator(IKeyGenerator keyGenerator) {
            this.keyGenerator = keyGenerator;
            return this;
        }

        public Builder taskExecutor(Executor executor) {
            this.taskExecutor = executor;
            return this;
        }
        public Builder disableDiskCache(boolean disableDiskCache) {
            this.disableDiskCache = disableDiskCache;
            return this;
        }
        public Builder disableMemoryCache(boolean disableMemoryCache) {
            this.disableMemoryCache = disableMemoryCache;
            return this;
        }

        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        public CacheManagerConfiguration build() {
            initEmptyFieldsWithDefaultValues();
            return new CacheManagerConfiguration(this);
        }

        private void initEmptyFieldsWithDefaultValues() {
            if (taskExecutor == null) {
                taskExecutor = SimpleExecutor.getInstance().getThreadPool();
            }
            if (keyGenerator == null && !disableDiskCache) {
                keyGenerator = new Md5KeyGenerator();
            }
            if (diskCache == null && !disableDiskCache) {
                if (diskCacheSize <= 0) {
                    diskCache = CacheFactory.getDefaultDiskCache(context);
                } else {
                    diskCache = CacheFactory.getDefaultDiskCache(context, diskCacheSize);
                }
            }
            if (memoryCache == null && !disableMemoryCache) {
                if (memoryCacheSize <= 0) {
                    memoryCache = CacheFactory.getDefaultMemoryCache();
                } else {
                    memoryCache = CacheFactory.getDefaultMemoryCache(memoryCacheSize);
                }
            }
        }

    }
}
