package io.golos.golos.screens.editor.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.View;

import javax.annotation.Nullable;

import io.golos.golos.R;
import timber.log.Timber;

public class CheckableButton extends AppCompatImageButton implements View.OnClickListener {
    @Nullable
    private View.OnClickListener mOnClickListener;

    public CheckableButton(Context context) {
        super(context);
        init(context, null);
    }

    public CheckableButton(Context context, AttributeSet attrs) {

        super(context, attrs);
        init(context, attrs);
    }

    public CheckableButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    private void init(Context context, @Nullable AttributeSet attrs) {
        setOnClickListener(this);
    }

    @Override
    public void setOnClickListener(@android.support.annotation.Nullable OnClickListener l) {
        if (l == this) super.setOnClickListener(l);
        else mOnClickListener = l;
    }

    @Override
    public boolean performClick() {
        return super.performClick();

    }

    @Override
    public void onClick(View v) {
        setSelected(!isSelected());
        if (mOnClickListener != null) mOnClickListener.onClick(v);
    }
}
