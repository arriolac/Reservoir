package com.anupcowkur.reservoir;

import android.content.Context;
import android.os.AsyncTask;
import com.google.gson.Gson;
import java.lang.reflect.Type;

/**
 * The main reservoir class.
 */
public class Reservoir {

    private static SimpleDiskCache cache;
    private static final Gson sGson = new Gson();

    /**
     * Initialize Reservoir
     *
     * @param context context.
     * @param maxSize the maximum size in bytes.
     */
    public static synchronized void init(Context context, long maxSize) throws Exception {

        cache = SimpleDiskCache.open(context.getFilesDir(), 1, maxSize);
    }

    /**
     * Check if an object with the given key exists in the Reservoir.
     *
     * @param key the key string.
     * @return true if object with given key exists.
     */
    public static boolean contains(String key) throws Exception {

        return cache.contains(key);
    }

    /**
     * Put an object into Reservoir with the given key. This a blocking IO operation. Previously
     * stored object with the same
     * key (if any) will be overwritten.
     *
     * @param key    the key string.
     * @param object the object to be stored.
     */
    public static void put(String key, Object object) throws Exception {
        String json = sGson.toJson(object);
        cache.put(key, json);
    }

    /**
     * Put an object into Reservoir with the given key asynchronously. Previously
     * stored object with the same
     * key (if any) will be overwritten.
     *
     * @param key      the key string.
     * @param object   the object to be stored.
     * @param callback a callback of type {@link com.anupcowkur.reservoirsample
     *                 .ReservoirPutCallback} which is called upon completion.
     */
    public static void putAsync(String key, Object object,
                                ReservoirPutCallback callback) {

        new PutTask(key, object, callback).execute();
    }

    /**
     * Get an object from Reservoir with the given key. This a blocking IO operation.
     *
     * @param key      the key string.
     * @param classOfT the Class type of the expected return object.
     * @return the object of the given type if it exists.
     */
    public static <T> T get(String key, Class<T> classOfT) throws Exception {

        String json = cache.getString(key).getString();
        T value = sGson.fromJson(json, classOfT);
        if(value == null)
            throw new NullPointerException();
        return value;
    }

    /**
     * Get an object from Reservoir with the given key. This a blocking IO operation.
     *
     * @param key the key String.
     * @param typeOfT the Type of the expected return object.
     * @return the object of the given type if it exists.
     */
    public static <T> T get(String key, Type typeOfT) throws Exception {
        final String json = cache.getString(key).getString();
        T value = sGson.fromJson(json, typeOfT);
        if (value == null) {
            throw new NullPointerException();
        }
        return value;
    }

    /**
     * Get an object from Reservoir with the given key asynchronously.
     *
     * @param key      the key string.
     * @param callback a callback of type {@link com.anupcowkur.reservoirsample
     *                 .ReservoirGetCallback} which is called upon completion.
     */
    public static <T> void getAsync(String key, Class<T> classOfT,
                                    ReservoirGetCallback<T> callback) {
        new GetTask<T>(key, classOfT, callback).execute();
    }

    public static <T> void getAsync(String key, Type typeOfT, ReservoirGetCallback<T> callback) {
        new GetTask<T>(key, typeOfT, callback).execute();
    }

    /**
     * Delete an object from Reservoir with the given key. This a blocking IO operation. Previously
     * stored object with the same
     * key (if any) will be deleted.
     *
     * @param key the key string.
     */
    public static void delete(String key) throws Exception {
        cache.delete(key);
    }

    /**
     * Delete an object into Reservoir with the given key asynchronously. Previously
     * stored object with the same
     * key (if any) will be deleted.
     *
     * @param key      the key string.
     * @param callback a callback of type {@link com.anupcowkur.reservoirsample
     *                 .ReservoirDeleteCallback} which is called upon completion.
     */
    public static void deleteAsync(String key, ReservoirDeleteCallback callback) {

        new DeleteTask(key, callback).execute();
    }

    /**
     * AsyncTask to perform put operation in a background thread.
     */
    private static class PutTask extends AsyncTask<Void, Void, Void> {

        private final String key;
        private Exception e;
        private final ReservoirPutCallback callback;
        final Object object;
        private static final Gson sGson = new Gson();

        private PutTask(String key, Object object, ReservoirPutCallback callback) {
            this.key = key;
            this.callback = callback;
            this.object = object;
            this.e = null;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                String json = sGson.toJson(object);
                cache.put(key, json);
            } catch (Exception e) {
                this.e = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (callback != null) {
                if (e == null) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(e);
                }
            }
        }

    }

    /**
     * AsyncTask to perform get operation in a background thread.
     */
    private static class GetTask<T> extends AsyncTask<Void, Void, T> {

        private final String key;
        private final ReservoirGetCallback callback;
        private final Class<T> classOfT;
        private final Type typeOfT;
        private Exception e;
        private static final Gson sGson = new Gson();

        private GetTask(String key, Class<T> classOfT, ReservoirGetCallback<T> callback) {
            this.key = key;
            this.callback = callback;
            this.classOfT = classOfT;
            this.typeOfT = null;
            this.e = null;
        }

        public GetTask(String key, Type typeOfT, ReservoirGetCallback<T> callback) {
            this.key = key;
            this.callback = callback;
            this.classOfT = null;
            this.typeOfT = typeOfT;
            this.e = null;
        }

        @Override
        protected T doInBackground(Void... params) {
            try {
                String json = cache.getString(key).getString();
                T value;
                if (classOfT != null) {
                    value = sGson.fromJson(json, classOfT);
                } else {
                    value = sGson.fromJson(json, typeOfT);
                }
                if(value == null)
                    throw new NullPointerException();
                return value;
            } catch (Exception e) {
                this.e = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(T object) {
            if (callback != null) {
                if (e == null) {
                    callback.onSuccess(object);
                } else {
                    callback.onFailure(e);
                }
            }
        }

    }

    /**
     * AsyncTask to perform delete operation in a background thread.
     */
    private static class DeleteTask extends AsyncTask<Void, Void, Void> {

        private final String key;
        private Exception e;
        private final ReservoirDeleteCallback callback;

        private DeleteTask(String key, ReservoirDeleteCallback callback) {
            this.key = key;
            this.callback = callback;
            this.e = null;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                cache.delete(key);
            } catch (Exception e) {
                this.e = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (callback != null) {
                if (e == null) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(e);
                }
            }
        }

    }

}
