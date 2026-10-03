package com.example.pilinara;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.content.pm.verify.domain.DomainVerificationUserState;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Rational;
import android.view.WindowManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

@Keep
public final class AndroidHelper {
    public static final boolean isFoldable;
    public static final boolean isPipAvailable;
    public static volatile boolean isPipMode = false;

    static {
        isFoldable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                android.view.WindowManager.class.getPackage().getName().equals("android");
        isPipAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    @Keep
    public static void clearActivityReference() {
        // Stub - Flutter plugin not available
    }
}
