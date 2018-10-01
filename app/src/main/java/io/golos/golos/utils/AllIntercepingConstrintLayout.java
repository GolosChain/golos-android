package io.golos.golos.utils;

import android.content.Context;
import android.graphics.Rect;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AllIntercepingConstrintLayout extends ConstraintLayout {
    private Set<View> mExceptedViews = new HashSet<>();

    public AllIntercepingConstrintLayout(Context context) {
        super(context);
    }

    public AllIntercepingConstrintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AllIntercepingConstrintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mExceptedViews.size() == 0) return true;
        else {
            float x = ev.getX();
            float y = ev.getY();
            Rect rect = new Rect();
            for (View v : mExceptedViews) {
                v.getHitRect(rect);
                if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom)
                    return false;
            }

            return true;
        }
    }

    public void removeExceptions(List<View> views) {
        mExceptedViews.removeAll(views);
    }

    public void addExceptions(List<View> views) {
        for (View v : views) {
            if (v != this && v.getParent() == this) {
                mExceptedViews.add(v);
            }
        }
    }
}
