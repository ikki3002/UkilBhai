package com.example.ukilbhai.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ImageView;

import com.example.ukilbhai.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Robust helper to safely store, compress (Base64 Data URI for cross-device Firestore sync),
 * and load profile avatars & chat attachments without SecurityException or tint distortion.
 */
public class ImageStorageHelper {

    private static final String PROFILE_IMAGE_FILENAME = "profile_avatar.jpg";

    /**
     * Copies an image from a content URI into the app's internal private files directory,
     * and returns a compact Base64 Data URI (data:image/jpeg;base64,...) so it syncs
     * across devices via Cloud Firestore without needing paid Cloud Storage buckets.
     */
    public static String saveImageToInternalStorage(Context context, Uri sourceUri) {
        if (context == null || sourceUri == null) return null;
        try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
            if (is == null) return null;
            Bitmap rawBmp = BitmapFactory.decodeStream(is);
            if (rawBmp != null) {
                Bitmap scaled = scaleBitmapDown(rawBmp, 280);
                File targetFile = new File(context.getFilesDir(), PROFILE_IMAGE_FILENAME);
                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                    scaled.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                    fos.flush();
                }
                return bitmapToDataUri(scaled, 75);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Encodes a chat image attachment into a compact Base64 Data URI (max 560px)
     * and saves a separate timestamped local copy (never overwriting profile_avatar.jpg).
     */
    public static String saveChatAttachment(Context context, Uri sourceUri) {
        if (context == null || sourceUri == null) return null;
        try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
            if (is == null) return null;
            Bitmap rawBmp = BitmapFactory.decodeStream(is);
            if (rawBmp != null) {
                Bitmap scaled = scaleBitmapDown(rawBmp, 560);
                String fileName = "chat_att_" + System.currentTimeMillis() + ".jpg";
                File targetFile = new File(context.getFilesDir(), fileName);
                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                    scaled.compress(Bitmap.CompressFormat.JPEG, 75, fos);
                    fos.flush();
                }
                return bitmapToDataUri(scaled, 70);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Bitmap scaleBitmapDown(Bitmap realImage, int maxImageSize) {
        int width = realImage.getWidth();
        int height = realImage.getHeight();
        if (width <= maxImageSize && height <= maxImageSize) {
            return realImage;
        }
        float ratio = Math.min((float) maxImageSize / width, (float) maxImageSize / height);
        int finalWidth = Math.max(1, Math.round(ratio * width));
        int finalHeight = Math.max(1, Math.round(ratio * height));
        return Bitmap.createScaledBitmap(realImage, finalWidth, finalHeight, true);
    }

    private static String bitmapToDataUri(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        byte[] bytes = baos.toByteArray();
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private static Bitmap decodeBitmapFromString(Context context, String photoPathOrUri) {
        if (TextUtils.isEmpty(photoPathOrUri)) return null;
        try {
            // Case 0: Base64 Data URI synced via Firestore
            if (photoPathOrUri.startsWith("data:image/")) {
                int commaIndex = photoPathOrUri.indexOf(',');
                if (commaIndex != -1 && commaIndex + 1 < photoPathOrUri.length()) {
                    String base64Part = photoPathOrUri.substring(commaIndex + 1);
                    byte[] decoded = Base64.decode(base64Part, Base64.DEFAULT);
                    return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                }
            }
            // Case 1: Local file path on disk
            if (photoPathOrUri.startsWith("/")) {
                File file = new File(photoPathOrUri);
                if (file.exists() && file.length() > 0) {
                    return BitmapFactory.decodeFile(file.getAbsolutePath());
                }
            } else if (photoPathOrUri.startsWith("file://")) {
                Uri fileUri = Uri.parse(photoPathOrUri);
                String path = fileUri.getPath();
                if (path != null) {
                    File file = new File(path);
                    if (file.exists() && file.length() > 0) {
                        return BitmapFactory.decodeFile(file.getAbsolutePath());
                    }
                }
            } else if (photoPathOrUri.startsWith("content://") && context != null) {
                try (InputStream is = context.getContentResolver().openInputStream(Uri.parse(photoPathOrUri))) {
                    if (is != null) {
                        return BitmapFactory.decodeStream(is);
                    }
                } catch (SecurityException se) {
                    context.getSharedPreferences("ukilbhai_local_session", Context.MODE_PRIVATE)
                            .edit().remove("profile_photo").apply();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    /**
     * Safely loads an avatar into an ImageView using Bitmap decoding.
     */
    public static void loadAvatar(Context context, ImageView imageView, String photoPathOrUri) {
        if (imageView == null) return;
        Bitmap bmp = decodeBitmapFromString(context, photoPathOrUri);
        if (bmp != null) {
            setBitmapAvatar(imageView, bmp);
        } else {
            setFallbackAvatar(imageView);
        }
    }

    /**
     * Loads a chat image attachment into an ImageView.
     */
    public static void loadChatImage(Context context, ImageView imageView, String attachmentPathOrUri) {
        if (imageView == null) return;
        Bitmap bmp = decodeBitmapFromString(context, attachmentPathOrUri);
        if (bmp != null) {
            imageView.setVisibility( android.view.View.VISIBLE);
            imageView.setImageTintList(null);
            imageView.setColorFilter(null);
            imageView.setPadding(0, 0, 0, 0);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageBitmap(bmp);
        } else {
            imageView.setVisibility(android.view.View.GONE);
        }
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
