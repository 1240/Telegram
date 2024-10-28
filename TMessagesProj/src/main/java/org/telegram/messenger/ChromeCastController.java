package org.telegram.messenger;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.cast.CastPlayer;
import androidx.media3.cast.SessionAvailabilityListener;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@UnstableApi
public class ChromeCastController implements Player.Listener, SessionAvailabilityListener {

    private CastContext castContext;
    public boolean isGooglePlayServicesAvailable;
    private CastPlayer castPlayer;
    private final ArrayList<MediaItem> mediaQueue = new ArrayList<>();
    private LocalFileHttpServer httpServer;
    private Application application;
    int port = 8080;
    @Nullable
    private VideoPlayerPausePlay listener;

    private ChromeCastController() {
    }

    public static class SingletonHolder {
        public static final ChromeCastController HOLDER_INSTANCE = new ChromeCastController();
    }

    public static ChromeCastController getInstance() {
        return SingletonHolder.HOLDER_INSTANCE;
    }

    public void init(Application application) {
        isGooglePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(application) == ConnectionResult.SUCCESS;
        if (isGooglePlayServicesAvailable) {
            this.application = application;
            castContext = CastContext.getSharedInstance(application);
            castPlayer = new CastPlayer(castContext);
            castPlayer.addListener(this);
            castPlayer.setSessionAvailabilityListener(this);
        }
    }

    public void addVideoPlayerListener(VideoPlayerPausePlay listener) {
        this.listener = listener;
    }

    @Override
    public void onCastSessionAvailable() {
        if (httpServer != null) {
            httpServer.stop();
        }
        httpServer = new LocalFileHttpServer(port, application);
        try {
            httpServer.start();
        } catch (IOException e) {
            //
        }
        play();
    }

    private void play() {
        castPlayer.setMediaItems(mediaQueue, C.INDEX_UNSET, C.TIME_UNSET);
        castPlayer.setPlayWhenReady(true);
        castPlayer.prepare();
        if (listener != null) {
            AndroidUtilities.runOnUIThread(() -> {
                if (listener != null) {
                    listener.pause();
                }
            }, 3000);
        }
    }

    @Override
    public void onCastSessionUnavailable() {
        castPlayer.stop();
        castPlayer.clearMediaItems();
        if (listener != null) listener.play();
    }

    public void setItem(Uri uri, String type) {
        MediaItem item = getMediaItem(uri, type);
        mediaQueue.clear();
        castPlayer.clearMediaItems();
        mediaQueue.add(item);
        castPlayer.addMediaItem(item);
        if (castPlayer.isCastSessionAvailable()) {
            play();
        }
    }

    private @NonNull MediaItem getMediaItem(Uri uri, String type) {
        return new MediaItem.Builder().setUri("http://" + getDeviceIpAddress() + ":" + port + "/" + uri.toString())
                .setMimeType(type)
                .build();
    }


    private static String getDeviceIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress inetAddress : addresses) {
                    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            //
        }
        return null;
    }

    public void release() {
//        mediaQueue.clear();
//        castPlayer.setSessionAvailabilityListener(null);
//        castPlayer.release();
        listener = null;
        castPlayer.stop();
    }

    public interface VideoPlayerPausePlay {
        void pause();
        void play();
    }
}
