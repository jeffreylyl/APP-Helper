/*
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
package android.app.demo;

import android.app.demo.footer.LoadingMoreFooter;
import android.app.helper.mvc.ILoadViewFactory;
import android.app.helper.view.vary.VaryViewHelperXV;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class MyLoadViewFactory implements ILoadViewFactory {

    @Override
    public ILoadMoreView madeLoadMoreView() {
        return new LoadMoreHelper();
    }

    @Override
    public ILoadView madeLoadView() {
        return new LoadViewHelper();
    }

    private class LoadMoreHelper implements ILoadMoreView {

        protected LoadingMoreFooter footView;

        protected OnClickListener onClickRefreshListener;

        @Override
        public void init(FootViewAdder footViewHolder, OnClickListener onClickRefreshListener) {
            View contentView = footViewHolder.getContentView();

            Context context = contentView.getContext();
            LoadingMoreFooter footer = new LoadingMoreFooter(context);
            footViewHolder.addFootView(footer);

            footView = footer;
            this.onClickRefreshListener = onClickRefreshListener;
            showNormal();
        }

        @Override
        public void showNormal() {
            footView.setState(LoadingMoreFooter.STATE_COMPLETE);
            footView.setOnClickListener(onClickRefreshListener);
        }

        @Override
        public void showLoading() {
            footView.setState(LoadingMoreFooter.STATE_LOADING);
            footView.setOnClickListener(null);
        }

        @Override
        public void showFail(Throwable e) {
            footView.setState(LoadingMoreFooter.STATE_FAIL);
            footView.setOnClickListener(onClickRefreshListener);
        }

        @Override
        public void showNomore() {
            footView.setState(LoadingMoreFooter.STATE_NOMORE);
            footView.setOnClickListener(null);
        }

    }

    private class LoadViewHelper implements ILoadView {
        private VaryViewHelperXV helper;
        private OnClickListener onClickRefreshListener;
        private Context context;

        @Override
        public void init(View switchView, OnClickListener onClickRefreshListener) {
            this.context = switchView.getContext().getApplicationContext();
            this.onClickRefreshListener = onClickRefreshListener;
            helper = new VaryViewHelperXV(switchView);
        }

        @Override
        public void restore() {
            helper.restoreView();
        }

        @Override
        public void showLoading() {
            Context context = helper.getContext();

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);

            ProgressBar progressBar = new ProgressBar(context);
            layout.addView(progressBar);

            TextView textView = new TextView(context);
            textView.setText("加载中...");
            textView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            int top = dip2px(context, 12);
            params.setMargins(0, top, 0, 0);
            layout.addView(textView, params);

            helper.showLayout(layout);
        }

        @Override
        public void tipFail(Throwable e) {
            Toast.makeText(context, "网络加载失败", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void showFail(Throwable e) {
            Context context = helper.getContext();

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);

            TextView textView = new TextView(context);
            textView.setText("网络加载失败");
            textView.setGravity(Gravity.CENTER);
            layout.addView(textView);

            Button button = new Button(context);
            button.setText("重试");
            button.setOnClickListener(onClickRefreshListener);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            int top = dip2px(context, 12);
            params.setMargins(0, top, 0, 0);
            layout.addView(button, params);

            helper.showLayout(layout);
        }

        @Override
        public void showEmpty() {
            Context context = helper.getContext();

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);

            TextView textView = new TextView(context);
            textView.setText("暂无数据");
            textView.setGravity(Gravity.CENTER);
            layout.addView(textView);

            Button button = new Button(context);
            button.setText("重试");
            button.setOnClickListener(onClickRefreshListener);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            int top = dip2px(context, 12);
            params.setMargins(0, top, 0, 0);
            layout.addView(button, params);

            helper.showLayout(layout);
        }

    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
