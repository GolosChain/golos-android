package io.golos.golos.screens.widgets;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

/**
 * Created by yuri on 22.11.17.
 */

public class SelectionAwareEditText extends AppCompatEditText {
    private SelectionListener mSelectionListener;

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
}
