package io.golos.golos.screens.widgets;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * @author yuri_borodkin on 17.05.2017.
 */

public class UnScrollableViewPager extends ViewPager {

    public UnScrollableViewPager(Context context) {
        super(context);
        init();
    }

    public UnScrollableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }

    private void init() {
        setOverScrollMode(OVER_SCROLL_NEVER);
    }
}
