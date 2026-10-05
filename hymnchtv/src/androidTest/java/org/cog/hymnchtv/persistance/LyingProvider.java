package org.cog.hymnchtv.persistance;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Test provider of a sending app: names an arbitrary `_data` for its content (served through a pipe).
 * Plain Java: the provider is created before the Kotlin runtime of the target app is on the test class path.
 */
public class LyingProvider extends ContentProvider {
    // The provider runs in the test package's process, so the test passes its lies in the uri query:
    // ?data=<_data to report>&name=<display name>&body=<content>

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] p, String s, String[] a, String o) {
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATA});
        c.addRow(new Object[]{uri.getQueryParameter("name"), uri.getQueryParameter("data")});
        return c;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final byte[] bytes = String.valueOf(uri.getQueryParameter("body")).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            new Thread(() -> {
                try (OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
                    out.write(bytes);
                }
                catch (IOException ignored) {
                }
            }).start();
            return pipe[0];
        }
        catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public String getType(Uri uri) {
        return "text/csv";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] a) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues v, String s, String[] a) {
        return 0;
    }
}
