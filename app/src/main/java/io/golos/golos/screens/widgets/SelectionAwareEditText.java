package io.golos.golos.screens.widgets;

import android.content.Context;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.AttributeSet;


import timber.log.Timber;

import static io.golos.golos.BuildConfig.DEBUG_EDITOR;

/**
 * Created by yuri on 22.11.17.
 */

public class SelectionAwareEditText extends AppCompatEditText {
    private SelectionListener mSelectionListener;
    private boolean freezeLayout = false;
    private boolean isRequestLoWasCalled = false;
    private boolean isInvalidateWasCalled = false;

    public void setSelectionListener(SelectionListener mSelectionListener) {
        this.mSelectionListener = mSelectionListener;
    }

    public SelectionAwareEditText(Context context) {
        super(context);
    }

    public SelectionAwareEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectionAwareEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (null != mSelectionListener) {
            mSelectionListener.onSelectionChanged(selStart, selEnd);
        }
    }

    public interface SelectionListener {
        void onSelectionChanged(int selStart, int end);
    }

    public boolean isFreezeLayout() {
        return freezeLayout;
    }

    public void setFreezeLayout(boolean freezeLayout) {
        this.freezeLayout = freezeLayout;
    /*    if (!freezeLayout) {
            if (isRequestLoWasCalled) {
                requestLayout();
                isRequestLoWasCalled = false;
            }
            if (isInvalidateWasCalled) {
                invalidate();
                isInvalidateWasCalled = false;
            }
        }*/
    }

    @Override
    public void requestLayout() {
        if (DEBUG_EDITOR) Timber.e("requestLayout freezeLayout = " + freezeLayout);
        if (freezeLayout) {
            isRequestLoWasCalled = true;
        } else {
            super.requestLayout();
        }

    }

    @Override
    public void invalidate() {
        if (freezeLayout) {
            isInvalidateWasCalled = true;
        } else {
            super.invalidate();
        }
    }
}
