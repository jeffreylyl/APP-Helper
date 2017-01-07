/*
Copyright 2015 shizhefei（LuckyJayce）

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package android.app.helper.mvc;

import android.annotation.TargetApi;
import android.app.helper.mvc.imp.DefaultLoadViewFactory;
import android.app.helper.mvc.viewhandler.ListViewHandler;
import android.app.helper.mvc.viewhandler.RecyclerViewHandler;
import android.app.helper.mvc.viewhandler.ViewHandler;
import android.app.helper.task.IAsyncTask;
import android.app.helper.utils.NetworkUtils;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;

import android.app.helper.mvc.IRefreshView.OnRefreshListener;
import android.app.helper.mvc.ILoadViewFactory.ILoadMoreView;
import android.app.helper.mvc.ILoadViewFactory.ILoadView;

/**
 * <br>
 * 刷新，加载更多规则<br>
 * 当用户下拉刷新时，会取消掉当前的刷新，以及加载更多的任务<br>
 * 当用户加载更多的时候，如果有已经正在刷新或加载更多是不会再执行加载更多的操作。<br>
 * <br>
 * 注意:记得在Activity的Ondestroy方法调用destory <br>
 *
 * @param <DATA>
 */
public class MVCHelper<DATA> {
    private IDataAdapter<DATA> dataAdapter;
    private IRefreshView refreshView;
    private View contentView;
    private Context context;
    private MOnStateChangeListener<DATA> onStateChangeListener = new MOnStateChangeListener<DATA>();
    private RequestHandle cancel;
    private long loadDataTime = -1;
    /**
     * 是否还有更多数据。如果服务器返回的数据为空的话，就说明没有更多数据了，也就没必要自动加载更多数据
     */
    private boolean hasMoreData = true;
    /***
     * 加载更多的时候是否事先检查网络是否可用。
     */
    private boolean needCheckNetwork = true;
    private ILoadView mLoadView;
    private ILoadMoreView mLoadMoreView;
    public static ILoadViewFactory loadViewFactory = new DefaultLoadViewFactory();

    private ListViewHandler listViewHandler = new ListViewHandler();

    private RecyclerViewHandler recyclerViewHandler = new RecyclerViewHandler();
    private IAsyncDataSource<DATA> asyncDataSource;
    private Handler handler;

    public MVCHelper(IRefreshView refreshView) {
        this(refreshView, loadViewFactory.madeLoadView(), loadViewFactory.madeLoadMoreView());
    }

    public MVCHelper(IRefreshView refreshView, ILoadViewFactory.ILoadView loadView) {
        this(refreshView, loadView, null);
    }

    public MVCHelper(IRefreshView refreshView, ILoadViewFactory.ILoadView loadView, ILoadViewFactory.ILoadMoreView loadMoreView) {
        super();
        this.context = refreshView.getContentView().getContext().getApplicationContext();
        this.autoLoadMore = true;
        this.refreshView = refreshView;
        contentView = refreshView.getContentView();
        contentView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        refreshView.setOnRefreshListener(onRefreshListener);
        mLoadView = loadView;
        mLoadMoreView = loadMoreView;
        mLoadView.init(refreshView.getSwitchView(), onClickRefresListener);
        handler = new Handler();
    }

    /**
     * 设置LoadView的factory，用于创建使用者自定义的加载失败，加载中，加载更多等布局
     *
     * @param fractory
     */
    public static void setLoadViewFractory(ILoadViewFactory fractory) {
        loadViewFactory = fractory;
    }

    /**
     * 如果不是网络请求的业务可以把这个设置为false
     *
     * @param needCheckNetwork
     */
    public void setNeedCheckNetwork(boolean needCheckNetwork) {
        this.needCheckNetwork = needCheckNetwork;
    }

    /**
     * 设置数据源，用于加载数据
     *
     * @param asyncDataSource
     */
    public void setDataSource(IAsyncDataSource<DATA> asyncDataSource) {
        this.asyncDataSource = asyncDataSource;
    }

    /**
     * 设置数据源，用于加载数据
     *
     * @param task
     */
    public void setDataSource(IAsyncTask<DATA> task) {
        this.asyncDataSource = new ProxyAsyncDataSource<>(task);
    }

    /**
     * 设置适配器，用于显示数据
     *
     * @param adapter 适配器
     */
    public void setAdapter(IDataAdapter<DATA> adapter) {
        if (contentView instanceof ListView) {
            setAdapter(adapter, listViewHandler);
        } else if (contentView instanceof RecyclerView) {
            setAdapter(adapter, recyclerViewHandler);
        } else {
            setAdapter(adapter, null);
        }
    }

    /**
     * 设置适配器，用于显示数据
     *
     * @param adapter     适配器
     * @param viewHandler 用于处理contentView的添加滚动末尾加载更多，添加底部加载更多布局等操作
     */
    public void setAdapter(IDataAdapter<DATA> adapter, ViewHandler viewHandler) {
        hasInitLoadMoreView = false;
        if (viewHandler != null) {
            View view = getContentView();
            hasInitLoadMoreView = viewHandler.handleSetAdapter(view, adapter, mLoadMoreView, onClickLoadMoreListener);
            viewHandler.setOnScrollBottomListener(view, onScrollBottomListener);
        }
        this.dataAdapter = adapter;
    }

    private boolean hasInitLoadMoreView = false;

    /**
     * 设置状态监听，监听开始刷新，刷新成功，开始加载更多，加载更多成功
     *
     * @param onStateChangeListener
     */
    public void setOnStateChangeListener(OnStateChangeListener<DATA> onStateChangeListener) {
        this.onStateChangeListener.setOnStateChangeListener(onStateChangeListener);
    }

    /**
     * 设置状态监听，监听开始刷新，刷新成功
     *
     * @param onRefreshStateChangeListener
     */
    public void setOnStateChangeListener(OnRefreshStateChangeListener<DATA> onRefreshStateChangeListener) {
        this.onStateChangeListener.setOnRefreshStateChangeListener(onRefreshStateChangeListener);
    }

    /**
     * 设置状态监听，监听开始加载更多，加载更多成功
     *
     * @param onLoadMoreStateChangeListener
     */
    public void setOnStateChangeListener(OnLoadMoreStateChangeListener<DATA> onLoadMoreStateChangeListener) {
        this.onStateChangeListener.setOnLoadMoreStateChangeListener(onLoadMoreStateChangeListener);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void refresh() {
        if (dataAdapter == null || asyncDataSource == null) {
            if (refreshView != null) {
                refreshView.showRefreshComplete();
            }
            return;
        }
        //if there is a running task, cancel it first
        if (cancel != null) {
            cancel.cancle();
            cancel = null;
        }
        //start a new task
        MResponseSender responseSender = new RefreshResponseSender(asyncDataSource, dataAdapter);
        responseSender.onPreExecute();
        cancel = responseSender.execute();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void loadMore() {
        if (dataAdapter == null || asyncDataSource == null) {
            if (refreshView != null) {
                refreshView.showRefreshComplete();
            }
            return;
        }
        //do nothing if there is a running task.
        if (isLoading()) {
            return;
        }

        if (dataAdapter.isEmpty()) {
            refresh();
            return;
        }
        LoadMoreResponseSender responseSender = new LoadMoreResponseSender(asyncDataSource, dataAdapter);
        responseSender.onPreExecute();
        cancel = responseSender.execute();
    }

    /**
     * 做销毁操作，比如关闭正在加载数据的异步线程等
     */
    public void destory() {
        if (cancel != null) {
            cancel.cancle();
            cancel = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    private abstract class MResponseSender implements ResponseSender<DATA> {

        protected abstract void onPreExecute();

        @Override
        public final void sendError(final Throwable e) {
            if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onPostExecute(null, e);
                        cancel = null;
                    }
                });
            } else {
                onPostExecute(null, e);
                cancel = null;
            }
        }

        @Override
        public final void sendData(final DATA data) {
            if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onPostExecute(data, null);
                        cancel = null;
                    }
                });
            } else {
                onPostExecute(data, null);
                cancel = null;
            }
        }

        protected abstract void onPostExecute(DATA data, Throwable e);

        public RequestHandle execute() {
            try {
                return executeImp();
            } catch (Exception e) {
                e.printStackTrace();
                onPostExecute(null, e);
            }
            return null;
        }

        public abstract RequestHandle executeImp() throws Exception;
    }

    private class RefreshResponseSender extends MResponseSender {
        private IAsyncDataSource<DATA> tDataSource;
        private IDataAdapter<DATA> tDataAdapter;
        private Runnable showRefreshing;

        public RefreshResponseSender(IAsyncDataSource<DATA> tDataSource, IDataAdapter<DATA> tDataAdapter) {
            super();
            this.tDataSource = tDataSource;
            this.tDataAdapter = tDataAdapter;
        }

        @Override
        protected void onPreExecute() {
            if (hasInitLoadMoreView && mLoadMoreView != null) {
                mLoadMoreView.showNormal();
            }
            if (tDataSource instanceof IDataCacheLoader) {
                @SuppressWarnings("unchecked")
                IDataCacheLoader<DATA> cacheLoader = (IDataCacheLoader<DATA>) tDataSource;
                final DATA data = cacheLoader.loadCache(tDataAdapter.isEmpty());
                if (data != null) {
                    tDataAdapter.notifyDataChanged(data, true);
                }
            }
            handler.post(showRefreshing = new Runnable() {
                @Override
                public void run() {
                    if (tDataAdapter.isEmpty()) {
                        mLoadView.showLoading();
                        refreshView.showRefreshComplete();
                    } else {
                        mLoadView.restore();
                        refreshView.showRefreshing();
                    }
                }
            });
            onStateChangeListener.onStartRefresh(tDataAdapter);
        }

        @Override
        public RequestHandle executeImp() throws Exception {
            return tDataSource.refresh(this);
        }

        @Override
        protected void onPostExecute(DATA result, Throwable e) {
            handler.removeCallbacks(showRefreshing);
            if (result == null) {
                if (tDataAdapter.isEmpty()) {
                    mLoadView.showFail(e);
                } else {
                    mLoadView.tipFail(e);
                }
            } else {
                loadDataTime = System.currentTimeMillis();
                tDataAdapter.notifyDataChanged(result, true);
                if (tDataAdapter.isEmpty()) {
                    mLoadView.showEmpty();
                } else {
                    mLoadView.restore();
                }
                hasMoreData = tDataSource.hasMore();
                if (hasInitLoadMoreView && mLoadMoreView != null) {
                    if (hasMoreData) {
                        mLoadMoreView.showNormal();
                    } else {
                        mLoadMoreView.showNomore();
                    }
                }
            }
            onStateChangeListener.onEndRefresh(tDataAdapter, result);
            refreshView.showRefreshComplete();
        }

        @Override
        public void sendProgress(long current, long total, Object exraData) {

        }
    }

    private class LoadMoreResponseSender extends MResponseSender {
        private IAsyncDataSource<DATA> tDataSource;
        private IDataAdapter<DATA> tDataAdapter;

        public LoadMoreResponseSender(IAsyncDataSource<DATA> tDataSource, IDataAdapter<DATA> tDataAdapter) {
            super();
            this.tDataSource = tDataSource;
            this.tDataAdapter = tDataAdapter;
        }

        @Override
        protected void onPreExecute() {
            onStateChangeListener.onStartLoadMore(tDataAdapter);
            if (hasInitLoadMoreView && mLoadMoreView != null) {
                mLoadMoreView.showLoading();
            }
        }

        @Override
        public RequestHandle executeImp() throws Exception {
            return tDataSource.loadMore(this);
        }

        @Override
        protected void onPostExecute(DATA result, Throwable e) {
            if (result == null) {
                mLoadView.tipFail(e);
                if (hasInitLoadMoreView && mLoadMoreView != null) {
                    mLoadMoreView.showFail(e);
                }
            } else {
                tDataAdapter.notifyDataChanged(result, false);
                if (tDataAdapter.isEmpty()) {
                    mLoadView.showEmpty();
                } else {
                    mLoadView.restore();
                }
                hasMoreData = tDataSource.hasMore();
                if (hasInitLoadMoreView && mLoadMoreView != null) {
                    if (hasMoreData) {
                        mLoadMoreView.showNormal();
                    } else {
                        mLoadMoreView.showNomore();
                    }
                }
            }
            onStateChangeListener.onEndLoadMore(tDataAdapter, result);
        }

        @Override
        public void sendProgress(long current, long total, Object exraData) {

        }
    }

    /**
     * 是否正在加载中
     *
     * @return
     */
    public boolean isLoading() {
        if (asyncDataSource != null) {
            return cancel != null;
        }
        return false;
    }

    private OnRefreshListener onRefreshListener = new OnRefreshListener() {

        @Override
        public void onRefresh() {
            refresh();
        }
    };

    @SuppressWarnings("unchecked")
    public <T extends View> T getContentView() {
        return (T) refreshView.getContentView();
    }

    /**
     * 获取上次刷新数据的时间（数据成功的加载），如果数据没有加载成功过，那么返回-1
     *
     * @return
     */
    public long getLoadDataTime() {
        return loadDataTime;
    }

    public IDataAdapter<DATA> getAdapter() {
        return dataAdapter;
    }

    public ILoadViewFactory.ILoadView getLoadView() {
        return mLoadView;
    }

    public ILoadViewFactory.ILoadMoreView getLoadMoreView() {
        return mLoadMoreView;
    }

    public void setAutoLoadMore(boolean autoLoadMore) {
        this.autoLoadMore = autoLoadMore;
    }

    private boolean autoLoadMore = true;

    public boolean isAutoLoadMore() {
        return autoLoadMore;
    }

    private OnClickListener onClickLoadMoreListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            loadMore();
        }
    };

    private OnClickListener onClickRefresListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            refresh();
        }
    };

    protected IRefreshView getRefreshView() {
        return refreshView;
    }

    /**
     * 加载监听
     *
     * @param <DATA>
     * @author zsy
     */
    private static class MOnStateChangeListener<DATA> implements OnStateChangeListener<DATA> {
        private OnStateChangeListener<DATA> onStateChangeListener;
        private OnRefreshStateChangeListener<DATA> onRefreshStateChangeListener;
        private OnLoadMoreStateChangeListener<DATA> onLoadMoreStateChangeListener;

        public void setOnStateChangeListener(OnStateChangeListener<DATA> onStateChangeListener) {
            this.onStateChangeListener = onStateChangeListener;
        }

        public void setOnRefreshStateChangeListener(OnRefreshStateChangeListener<DATA> onRefreshStateChangeListener) {
            this.onRefreshStateChangeListener = onRefreshStateChangeListener;
        }

        public void setOnLoadMoreStateChangeListener(OnLoadMoreStateChangeListener<DATA> onLoadMoreStateChangeListener) {
            this.onLoadMoreStateChangeListener = onLoadMoreStateChangeListener;
        }

        @Override
        public void onStartRefresh(IDataAdapter<DATA> adapter) {
            if (onStateChangeListener != null) {
                onStateChangeListener.onStartRefresh(adapter);
            } else if (onRefreshStateChangeListener != null) {
                onRefreshStateChangeListener.onStartRefresh(adapter);
            }
        }

        @Override
        public void onEndRefresh(IDataAdapter<DATA> adapter, DATA result) {
            if (onStateChangeListener != null) {
                onStateChangeListener.onEndRefresh(adapter, result);
            } else if (onRefreshStateChangeListener != null) {
                onRefreshStateChangeListener.onEndRefresh(adapter, result);
            }
        }

        @Override
        public void onStartLoadMore(IDataAdapter<DATA> adapter) {
            if (onStateChangeListener != null) {
                onStateChangeListener.onStartLoadMore(adapter);
            } else if (onLoadMoreStateChangeListener != null) {
                onLoadMoreStateChangeListener.onStartLoadMore(adapter);
            }
        }

        @Override
        public void onEndLoadMore(IDataAdapter<DATA> adapter, DATA result) {
            if (onStateChangeListener != null) {
                onStateChangeListener.onEndLoadMore(adapter, result);
            } else if (onLoadMoreStateChangeListener != null) {
                onLoadMoreStateChangeListener.onEndLoadMore(adapter, result);
            }
        }

    }

    private OnScrollBottomListener onScrollBottomListener = new OnScrollBottomListener() {

        @Override
        public void onScorllBootom() {
            if (autoLoadMore && hasMoreData && !isLoading()) {
                // 如果网络可以用
                if (needCheckNetwork && !NetworkUtils.hasNetwork(context)) {
                    mLoadMoreView.showFail(new Exception("网络不可用"));
                } else {
                    loadMore();
                }
            }
        }
    };

    public static interface OnScrollBottomListener {
        public void onScorllBootom();
    }

    private class ProxyAsyncDataSource<DATA> implements IAsyncDataSource<DATA> {
        private IAsyncTask<DATA> task;

        public ProxyAsyncDataSource(IAsyncTask<DATA> task) {
            this.task = task;
        }

        @Override
        public RequestHandle refresh(ResponseSender<DATA> sender) throws Exception {
            return task.execute(sender);
        }

        @Override
        public RequestHandle loadMore(ResponseSender<DATA> sender) throws Exception {
            return null;
        }

        @Override
        public boolean hasMore() {
            return false;
        }
    }

}
