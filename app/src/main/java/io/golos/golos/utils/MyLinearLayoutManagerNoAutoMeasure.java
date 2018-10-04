package io.golos.golos.utils;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MyLinearLayoutManagerNoAutoMeasure extends LinearLayoutManager {
    private boolean canScroll = true;

    public MyLinearLayoutManagerNoAutoMeasure(Context context) {
        super(context);
    }

    public MyLinearLayoutManagerNoAutoMeasure(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public MyLinearLayoutManagerNoAutoMeasure(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            super.onLayoutChildren(recycler, state);
        } catch (IndexOutOfBoundsException ignored) {
            //ignore, due to bug in android support library
        }
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        return false;
    }

    @Override
    public boolean canScrollHorizontally() {
        return super.canScrollHorizontally() && canScroll;
    }

    @Override
    public boolean canScrollVertically() {
        return super.canScrollVertically() && canScroll;
    }

    public void setCanScroll(boolean canScroll) {
        this.canScroll = canScroll;
    }
}
