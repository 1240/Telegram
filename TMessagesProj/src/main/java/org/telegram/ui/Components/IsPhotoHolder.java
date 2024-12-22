package org.telegram.ui.Components;


import java.util.HashMap;

public class IsPhotoHolder {

    private HashMap<String, Boolean> map = new HashMap<>();

    private IsPhotoHolder() {
    }

    private static class Holder {
        private static final IsPhotoHolder INSTANCE = new IsPhotoHolder();
    }

    public static IsPhotoHolder getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized void put(String key, Boolean data) {
        map.put(key, data);
    }

    public synchronized Boolean get(String key) {
        return map.get(key);
    }

    public synchronized void remove(String key) {
        map.remove(key);
    }

    public synchronized void clear() {
        map.clear();
    }
}