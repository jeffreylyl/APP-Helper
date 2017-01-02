package android.app.helper.task;

import android.app.helper.mvc.RequestHandle;
import android.app.helper.mvc.ResponseSender;

/**
 * 异步Task，不能直接执行超时任务，可以执行像OKhttp异步回调那样
 */
public interface IAsyncTask<DATA> extends ISuperTask<DATA> {
    
    /**
     * @param sender 用于请求结束时发送数据给TaskHelper,MVCHelper,然后在通知CallBack的Post回调方法
     * @return 用于提供外部取消请求的处理
     * @throws Exception
     */
    RequestHandle execute(ResponseSender<DATA> sender) throws Exception;

}
