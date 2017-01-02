package android.app.demo;

import android.app.cacheManager.R;
import android.app.helper.cache.CacheManagerFactory;
import android.app.helper.cache.ICacheManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class CacheActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ICacheManager<String> cacheManager = CacheManagerFactory.getInstance(this);

        cacheManager.clearCache();

        final List<String> keyList = new ArrayList<>(10);
        String k = "key-";
        for (int i = 0; i < 10; i++) {
            keyList.add(k + i);
        }

        for (int i = 0; i < 10; i++) {
            String cache = cacheManager.get(keyList.get(i));
        }

        for (int i = 0; i < 10; i++) {
            cacheManager.put(keyList.get(i), "content-" + i);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    String cache = cacheManager.get(keyList.get(i));
                }
            }
        }, 20);

    }
}
