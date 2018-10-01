package io.golos.golos.screens.widgets.dialogs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by yuri on 02.03.18.
 */

public class PhotosPager extends ViewPager {
    boolean isLocked = false;

    public PhotosPager(@NonNull Context context) {
        super(context);
    }

    public PhotosPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (!isLocked) {
            try {
                return super.onInterceptTouchEvent(ev);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return !isLocked && super.onTouchEvent(event);
    }

    public void toggleLock() {
        isLocked = !isLocked;
    }

    public void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }

    public boolean isLocked() {
        return isLocked;
    }
}
