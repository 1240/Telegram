package org.telegram.ui.Components;


import org.telegram.messenger.video.MediaCodecVideoConvertor;

import java.util.HashMap;
import java.util.List;

public class SoundInfoHolder {

    private HashMap<String, List<MediaCodecVideoConvertor.MixedSoundInfo>> map = new HashMap<>();

    private SoundInfoHolder() {
    }

    private static class Holder {
        private static final SoundInfoHolder INSTANCE = new SoundInfoHolder();
    }

    public static SoundInfoHolder getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized void put(String key, List<MediaCodecVideoConvertor.MixedSoundInfo> data) {
        map.put(key, data);
    }

    public synchronized List<MediaCodecVideoConvertor.MixedSoundInfo> get(String key) {
        return map.get(key);
    }

    public synchronized void remove(String key) {
        map.remove(key);
    }

    public synchronized void clear() {
        map.clear();
    }
}