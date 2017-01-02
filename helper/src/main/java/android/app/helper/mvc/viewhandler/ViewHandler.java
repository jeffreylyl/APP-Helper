package android.app.helper.mvc.viewhandler;

import android.app.helper.mvc.IDataAdapter;
import android.app.helper.mvc.ILoadViewFactory;
import android.app.helper.mvc.MVCHelper;
import android.view.View;
import android.view.View.OnClickListener;


public interface ViewHandler {

	/**
	 *
	 * @param contentView
	 * @param adapter
	 * @param loadMoreView
	 * @param onClickLoadMoreListener
     * @return 是否有 init ILoadMoreView
     */
	public boolean handleSetAdapter(View contentView, IDataAdapter<?> adapter, ILoadViewFactory.ILoadMoreView loadMoreView, OnClickListener onClickLoadMoreListener);

	public void setOnScrollBottomListener(View contentView, MVCHelper.OnScrollBottomListener onScrollBottomListener);

}
