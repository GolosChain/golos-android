package io.golos.golos.screens.editor.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.View;

import javax.annotation.Nullable;

import io.golos.golos.R;

public class CheckableButton extends AppCompatImageButton implements View.OnClickListener {
    @Nullable
    private View.OnClickListener mOnClickListener;
    private boolean isSelectibilityTurnedOn = true;

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
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        if (selected) {
            Drawable d = getDrawable();
            if (d == null) return;
            d.setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_IN);
        } else {
            Drawable d = getDrawable();
            if (d == null) return;
            d.setColorFilter(null);
        }
    }

    @Override
    public void onClick(View v) {
        if (isSelectibilityTurnedOn) setSelected(!isSelected());
        if (mOnClickListener != null) mOnClickListener.onClick(v);
    }

    @Nullable
    public OnClickListener getmOnClickListener() {
        return mOnClickListener;
    }

    public void setmOnClickListener(@Nullable OnClickListener mOnClickListener) {
        this.mOnClickListener = mOnClickListener;
    }

    public boolean isSelectibilityTurnedOn() {
        return isSelectibilityTurnedOn;
    }

    public void setSelectibilityTurnedOn(boolean selectivityTurnedOn) {
        if (selectivityTurnedOn != isSelectibilityTurnedOn) {
            if (!selectivityTurnedOn) setSelected(false);
            isSelectibilityTurnedOn = selectivityTurnedOn;
        }
    }
}
