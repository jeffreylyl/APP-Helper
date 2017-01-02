package android.app.helper.mvc;

/**
 * 用于请求结束时发送数据或者发送异常
 *
 * @param <DATA>
 */
public interface ResponseSender<DATA> extends ProgressSender {

    void sendError(Throwable e);

    void sendData(DATA data);

    void sendProgress(long current, long total, Object extraData);

}