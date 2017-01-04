package android.app.demo;

import android.app.Activity;
import android.app.cacheManager.R;
import android.app.helper.cache.cachemanager.CacheManagerFactory;
import android.app.helper.cache.cachemanager.ICacheManager;
import android.app.helper.mvc.IAsyncDataSource;
import android.app.helper.mvc.IDataAdapter;
import android.app.helper.mvc.IDataCacheLoader;
import android.app.helper.mvc.MVCHelper;
import android.app.helper.mvc.MVCUltraHelper;
import android.app.helper.mvc.RequestHandle;
import android.app.helper.mvc.ResponseSender;
import android.app.helper.mvc.RxDataSource;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.header.MaterialHeader;
import rx.Observable;
import rx.Subscriber;

public class HelperActivity extends Activity {
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView rv = (RecyclerView) findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));

        PtrClassicFrameLayout mPtrFrameLayout = (PtrClassicFrameLayout) findViewById(R.id.ptr);
        final MaterialHeader header = new MaterialHeader(this);
        header.setLayoutParams(new PtrFrameLayout.LayoutParams(-1, -2));
        header.setPadding(0, 20, 0, 20);
        header.setPtrFrameLayout(mPtrFrameLayout);
        mPtrFrameLayout.setLoadingMinTime(800);
        mPtrFrameLayout.setDurationToCloseHeader(800);
        mPtrFrameLayout.setHeaderView(header);
        mPtrFrameLayout.addPtrUIHandler(header);

        final MVCHelper<List<String>> helper = new MVCUltraHelper<List<String>>(mPtrFrameLayout);
        helper.setAdapter(new HelperDataAdapter(this));
        helper.setDataSource(new HelperRxDataSource());
        helper.refresh();
    }

    private class HelperRxDataSource extends RxDataSource<List<String>>{
        final ICacheManager<String> cacheManager = CacheManagerFactory.getInstance(getApplicationContext());
        @Override
        public Observable<List<String>> refreshRX(DoneActionRegister<List<String>> register) throws Exception {
            Observable<List<String>> cache = Observable.create(new Observable.OnSubscribe<List<String>>() {
                @Override
                public void call(Subscriber<? super List<String>> subscriber) {
                    String cache = cacheManager.get("c");
                    if (cache != null) {
                        Log.e("eeeeeeee", "###### cache key c = " + cache);
                        subscriber.onNext(mockCacheData());
                    } else {
                        subscriber.onCompleted();
                    }

                }
            });
            Observable<List<String>> network = Observable.create(new Observable.OnSubscribe<List<String>>() {
                @Override
                public void call(Subscriber<? super List<String>> subscriber) {
                    if (mockNetWorkData() != null) {
                        cacheManager.put("c", "{1,2,3,4,5,6,7}");
                        subscriber.onNext(mockNetWorkData());
                    } else {
                        subscriber.onCompleted();
                    }
                }
            });

            return Observable.concat(cache, network).first();
        }

        private List<String> mockNetWorkData() {
            List<String> list = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                list.add("refresh item - " + i);
            }
            return list;
        }
        private List<String> mockCacheData() {
            List<String> list = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                list.add("shit item - " + i);
            }
            return list;
        }

        @Override
        public Observable<List<String>> loadMoreRX(DoneActionRegister<List<String>> register) throws Exception {
            return Observable.error(new RuntimeException("eeeeeee"));
        }

        @Override
        public boolean hasMore() {
            return true;
        }
    }

    private class HelperDataSource implements IAsyncDataSource<List<String>>, IDataCacheLoader<List<String>> {
        @Override
        public RequestHandle refresh(final ResponseSender<List<String>> sender) throws Exception {

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sender.sendData(mockData());
                }
            }, 2500);
            return null;
        }

        @Override
        public RequestHandle loadMore(final ResponseSender<List<String>> sender) throws Exception {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sender.sendData(mockData());
                }
            }, 2500);
            return null;
        }

        private List<String> mockData() {
            List<String> list = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                list.add("item - " + i);
            }
            return list;
        }

        @Override
        public boolean hasMore() {
            return true;
        }

        @Override
        public List<String> loadCache(boolean isEmpty) {
            List<String> list = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                list.add("cache item - " + i);
            }
            return list;
        }
    }

    private class HelperDataAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements IDataAdapter<List<String>> {

        private LayoutInflater inflater;
        private List<String> data = new ArrayList<String>();

        public HelperDataAdapter(Context context) {
            super();
            inflater = LayoutInflater.from(context);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(inflater.inflate(android.R.layout.simple_list_item_1, parent, false)) {
            };
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((TextView) holder.itemView.findViewById(android.R.id.text1)).setText(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        public void notifyDataChanged(List<String> list, boolean isRefresh) {
            if (isRefresh) {
                data.clear();
            }
            data.addAll(list);
            notifyDataSetChanged();
        }

        @Override
        public List<String> getData() {
            return data;
        }

        @Override
        public boolean isEmpty() {
            return data.isEmpty();
        }
    }
}
