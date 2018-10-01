package io.golos.golos.screens.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;


import androidx.core.view.GravityCompat;
import io.golos.golos.R;
import io.golos.golos.utils.BottomViewEx;

/**
 * Created by yuri on 05.12.17.
 */

public class GolosBottomNavView extends BottomNavigationView {
    public GolosBottomNavView(Context context) {
        super(context);
        init();
    }

    public GolosBottomNavView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GolosBottomNavView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_UNLABELED);
        //setTextVisibility(false);
        setItemHorizontalTranslationEnabled(false);
      //  enableAnimation(true);
    }

    public void setCounterAt(int positionOfMenuItem, int count) {
        ViewGroup menuView = (ViewGroup) getChildAt(0);
        FrameLayout menuItem = (FrameLayout) menuView.getChildAt(positionOfMenuItem);
        View counter = menuItem.findViewById(R.id.counter);
        if (counter == null) {
            counter = LayoutInflater.from(getContext()).inflate(R.layout.v_counter, menuItem, false);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = GravityCompat.END | Gravity.TOP;
            lp.rightMargin = (int) getContext().getResources().getDimension(R.dimen.material_big);
            counter.setLayoutParams(lp);
            menuItem.addView(counter);
            counter.setFocusableInTouchMode(false);
            counter.setClickable(false);
        }
        TextView counterText = (TextView) counter;
        counterText.setText(String.valueOf(count));
        if (count <= 0) counter.setVisibility(View.GONE);
        else counter.setVisibility(View.VISIBLE);

    }
}
