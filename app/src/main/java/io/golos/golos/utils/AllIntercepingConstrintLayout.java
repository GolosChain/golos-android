package io.golos.golos.utils;

import android.content.Context;
import android.graphics.Rect;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class AllIntercepingConstrintLayout extends ConstraintLayout {
    private List<View> mExceptedViews = new ArrayList<>();

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

    public void setExcpetedViews(List<View> views) {
        for (View v : views) {
            if (v.getParent() == this) {
                mExceptedViews.add(v);
            }
        }
    }
}
