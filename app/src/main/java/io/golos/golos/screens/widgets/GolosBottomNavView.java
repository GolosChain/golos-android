package io.golos.golos.screens.widgets;

import android.content.Context;
import android.util.AttributeSet;

import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

/**
 * Created by yuri on 05.12.17.
 */

public class GolosBottomNavView extends BottomNavigationViewEx {
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
        enableShiftingMode(false);
        setTextVisibility(false);
        enableItemShiftingMode(false);
        enableAnimation(true);
    }
}
