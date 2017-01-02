package android.app.helper.cache;

public abstract class ICacheManager<DATA> implements ICache<DATA> {
    public abstract DATA get(String key, long cacheTimeInSeconds);

    public String getCacheSizeText(){
        double size = getCacheSize();
        return Utils.getFormatSize(size);
    };
}
