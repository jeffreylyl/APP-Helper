package android.app.helper.cache;

public interface ICache<DATA> {
    public void clearCache();

    public void put(String key, DATA cache);

    public DATA get(String key);

    public boolean remove(String key);

    public double getCacheSize();
}
