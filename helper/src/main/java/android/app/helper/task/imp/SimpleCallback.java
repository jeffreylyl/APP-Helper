package android.app.helper.task.imp;


import android.app.helper.task.ICallback;

/**
 * ICallback的空实现.
 *
 */
public abstract class SimpleCallback<DATA> implements ICallback<DATA> {

    @Override
    public void onPreExecute(Object task) {

    }

    @Override
    public void onProgress(Object task, int percent, long current, long total, Object extraData) {

    }
}
