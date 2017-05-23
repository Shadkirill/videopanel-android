package ru.com.videopanel.db.dbutil;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Cancellable;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmObject;
import io.realm.RealmResults;

/**
 * RxJava Observable which returns from Realm db
 *
 * @param <T> Realm result DAO type
 */
public class RealmResultsObservable<T extends RealmObject> implements ObservableOnSubscribe<RealmResults<T>> {

    private final RealmResults<T> realmResults;

    private RealmResultsObservable(RealmResults<T> realmResults) {
        this.realmResults = realmResults;
    }

    /**
     * Create observable from Realm response
     * @param realmResults Db request result
     * @return Rx observable witch emmit all results from db
     */
    public static <T extends RealmObject> Observable<T> from(RealmResults<T> realmResults) {
        return Observable.fromIterable(Realm.getDefaultInstance().copyFromRealm(realmResults));
    }

    @Override
    public void subscribe(ObservableEmitter<RealmResults<T>> emitter) throws Exception {
        // Initial element
        emitter.onNext(realmResults);

        RealmChangeListener<RealmResults<T>> changeListener = new RealmChangeListener<RealmResults<T>>() {
            @Override
            public void onChange(RealmResults<T> element) {
                emitter.onNext(element);
            }
        };

        realmResults.addChangeListener(changeListener);

        emitter.setCancellable(new Cancellable() {
            @Override
            public void cancel() throws Exception {
                realmResults.removeChangeListener(changeListener);
            }
        });
    }
}