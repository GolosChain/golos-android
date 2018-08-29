package io.golos.golos.utils;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class AllIntercepingRelative extends ConstraintLayout{
    public AllIntercepingRelative(Context context) {
        super(context);
    }

    public AllIntercepingRelative(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AllIntercepingRelative(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;

    }
}
