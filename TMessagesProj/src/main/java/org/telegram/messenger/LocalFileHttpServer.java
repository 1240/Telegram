package org.telegram.messenger;

import android.app.Application;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class LocalFileHttpServer extends NanoHTTPD {

    private final Application application;

    public LocalFileHttpServer(int port, Application application) {
        super(port);
        this.application = application;
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            String uri = session.getUri();
            Uri fileUri = Uri.parse( uri.substring(1));
            ParcelFileDescriptor parcelFileDescriptor = application.getContentResolver().openFileDescriptor(fileUri, "r");
            FileInputStream fis = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
            return newChunkedResponse(Response.Status.OK, "video/mp4", fis);
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found");
        }
    }
}