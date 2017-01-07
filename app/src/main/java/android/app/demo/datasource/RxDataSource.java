package android.app.demo.datasource;

import android.util.Log;

import android.app.helper.mvc.IAsyncDataSource;
import android.app.helper.mvc.RequestHandle;
import android.app.helper.mvc.ResponseSender;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public abstract class RxDataSource<DATA> implements IAsyncDataSource<DATA> {

    @Override
    public final RequestHandle refresh(final ResponseSender<DATA> sender) throws Exception {
        DoneActionRegister<DATA> register = new DoneActionRegister<>();
        return load(sender, refreshRX(register), register);
    }

    @Override
    public final RequestHandle loadMore(ResponseSender<DATA> sender) throws Exception {
        DoneActionRegister<DATA> register = new DoneActionRegister<>();
        return load(sender, loadMoreRX(register), register);
    }

    private RequestHandle load(final ResponseSender<DATA> sender, final Observable<DATA> observable, final DoneActionRegister<DATA> register) {
        final Subscriber<DATA> subscriber = new Subscriber<DATA>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                sender.sendError(e);
            }

            @Override
            public void onNext(DATA data) {
                for (Action1<DATA> subscriber : register.subscribers) {
                    subscriber.call(data);
                }
                sender.sendData(data);
            }
        };
        observable.subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriber);
        return new RequestHandle() {
            @Override
            public void cancle() {
                Log.d("eeeeeee", "cancle:");
                if (!subscriber.isUnsubscribed()) {
                    subscriber.unsubscribe();
                }
            }

            @Override
            public boolean isRunning() {
                return false;
            }
        };
    }

    /*
    1. get cache first and then get network data.
    return Observable.just(false, true).map(new Func1<Boolean, List<String>>() {
                @Override
                public List<String> call(Boolean boolean) {
                    if(!boolean){
                        return mockCacheData();
                    }

                    -save network data.
                    return mockNetWorkData();
                }
            });
    2. cache first
     Observable<List<String>> cache = Observable.create(new Observable.OnSubscribe<List<String>>() {
                @Override
                public void call(Subscriber<? super List<String>> subscriber) {
                    List<String> data = mockCacheData();
                    if (data != null) {
                        subscriber.onNext(data);
                    } else {
                        subscriber.onCompleted();
                    }

                }
            });
            Observable<List<String>> network = Observable.create(new Observable.OnSubscribe<List<String>>() {
                @Override
                public void call(Subscriber<? super List<String>> subscriber) {
                    List<String> data = mockNetworkData();
                    if (data != null) {
                        -save network data.
                        subscriber.onNext(data);
                    } else {
                        subscriber.onCompleted();
                    }
                }
            });

            return Observable.concat(cache, network).first();
     */
    public abstract Observable<DATA> refreshRX(DoneActionRegister<DATA> register) throws Exception;

    public abstract Observable<DATA> loadMoreRX(DoneActionRegister<DATA> register) throws Exception;

    public static class DoneActionRegister<DATA> {
        private List<Action1<DATA>> subscribers = new ArrayList<>();


        public void addAction(final Action1<DATA> doneAction) {
            subscribers.add(doneAction);
        }
    }

}
