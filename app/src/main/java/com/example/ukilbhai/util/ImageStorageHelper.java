package com.example.ukilbhai.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;

import com.example.ukilbhai.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Robust helper to safely store and load profile avatars without SecurityException
 * or unwanted tint distortion on custom user photos.
 */
public class ImageStorageHelper {

    private static final String PROFILE_IMAGE_FILENAME = "profile_avatar.jpg";

    /**
     * Copies an image from a content URI into the app's internal private files directory.
     * Returns the absolute path of the saved file, or null on failure.
     */
    public static String saveImageToInternalStorage(Context context, Uri sourceUri) {
        if (context == null || sourceUri == null) return null;
        try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
            if (is == null) return null;
            File targetFile = new File(context.getFilesDir(), PROFILE_IMAGE_FILENAME);
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
            }
            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Safely loads an avatar into an ImageView using Bitmap decoding.
     * Clears any XML tint filter when loading a custom photo so it never appears as a solid color square.
     */
    public static void loadAvatar(Context context, ImageView imageView, String photoPathOrUri) {
        if (imageView == null) return;
        if (context == null || TextUtils.isEmpty(photoPathOrUri)) {
            setFallbackAvatar(imageView);
            return;
        }

        try {
            // Case 1: Local file path on disk
            if (photoPathOrUri.startsWith("/")) {
                File file = new File(photoPathOrUri);
                if (file.exists() && file.length() > 0) {
                    Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                    if (bmp != null) {
                        setBitmapAvatar(imageView, bmp);
                        return;
                    }
                }
            } else if (photoPathOrUri.startsWith("file://")) {
                Uri fileUri = Uri.parse(photoPathOrUri);
                String path = fileUri.getPath();
                if (path != null) {
                    File file = new File(path);
                    if (file.exists() && file.length() > 0) {
                        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                        if (bmp != null) {
                            setBitmapAvatar(imageView, bmp);
                            return;
                        }
                    }
                }
            } else if (photoPathOrUri.startsWith("content://")) {
                // Case 2: Content URI. Safely attempt to decode stream.
                try (InputStream is = context.getContentResolver().openInputStream(Uri.parse(photoPathOrUri))) {
                    if (is != null) {
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        if (bmp != null) {
                            // Copy to internal storage so future launches use local file
                            String localPath = saveImageToInternalStorage(context, Uri.parse(photoPathOrUri));
                            if (!TextUtils.isEmpty(localPath)) {
                                context.getSharedPreferences("ukilbhai_local_session", Context.MODE_PRIVATE)
                                        .edit().putString("profile_photo", localPath).apply();
                            }
                            setBitmapAvatar(imageView, bmp);
                            return;
                        }
                    }
                } catch (SecurityException se) {
                    // Expired picker permission: remove invalid URI so it never fails again
                    context.getSharedPreferences("ukilbhai_local_session", Context.MODE_PRIVATE)
                            .edit().remove("profile_photo").apply();
                } catch (Exception ignored) {}
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        setFallbackAvatar(imageView);
    }

    private static void setBitmapAvatar(ImageView imageView, Bitmap bmp) {
        imageView.setImageTintList(null);
        imageView.setColorFilter(null);
        imageView.setPadding(0, 0, 0, 0);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageBitmap(bmp);
    }

    private static void setFallbackAvatar(ImageView imageView) {
        imageView.setImageTintList(null);
        imageView.setColorFilter(null);
        int pad = (int) (imageView.getResources().getDisplayMetrics().density * 8);
        imageView.setPadding(pad, pad, pad, pad);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setImageResource(R.drawable.ic_avatar);
    }
}
