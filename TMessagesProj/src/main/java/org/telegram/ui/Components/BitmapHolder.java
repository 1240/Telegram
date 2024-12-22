package org.telegram.ui.Components;

import android.graphics.Bitmap;

import java.util.HashMap;

public class BitmapHolder {

    private HashMap<String, Bitmap> bitmapMap = new HashMap<>();

    private BitmapHolder() {
    }

    private static class Holder {
        private static final BitmapHolder INSTANCE = new BitmapHolder();
    }

    public static BitmapHolder getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized void putBitmap(String key, Bitmap bitmap) {
        bitmapMap.put(key, bitmap);
    }

    public synchronized Bitmap getBitmap(String key) {
        return bitmapMap.get(key);
    }

    public synchronized void removeBitmap(String key) {
        bitmapMap.remove(key);
    }

    public synchronized void clear() {
        bitmapMap.clear();
    }
}