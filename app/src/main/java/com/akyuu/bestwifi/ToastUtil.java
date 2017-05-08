package com.akyuu.bestwifi;

import android.content.Context;
import android.support.annotation.StringRes;
import android.widget.Toast;

public class ToastUtil {
    private static Toast sToast;

    public static void showToast(Context context, CharSequence text, int duration) {
        if (sToast == null) {
            sToast = Toast.makeText(context, text, duration);
        } else {
            sToast.setText(text);
            sToast.setDuration(duration);
        }
        sToast.show();
    }

    public static void showToast(Context context, @StringRes int resId, int duration) {
        String text = context.getResources().getString(resId);
        showToast(context, text, duration);
    }
}
