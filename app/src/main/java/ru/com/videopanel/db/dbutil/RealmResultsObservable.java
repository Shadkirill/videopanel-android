package ru.com.videopanel.db.dbutil;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Cancellable;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmObject;
import io.realm.RealmResults;

/* Copyright 2016 Patrick Löwenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
public class RealmResultsObservable<T extends RealmObject> implements ObservableOnSubscribe<RealmResults<T>> {

    private final RealmResults<T> realmResults;

    private RealmResultsObservable(RealmResults<T> realmResults) {
        this.realmResults = realmResults;
    }

    public static <T extends RealmObject> Observable<T> from(RealmResults<T> realmResults) {
        return Observable.fromIterable(Realm.getDefaultInstance().copyFromRealm(realmResults));
//        Observable.create(new RealmResultsObservable<>(realmResults));
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