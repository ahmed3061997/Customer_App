package com.fc.mis.charitable.models;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.view.View;

public class AnimUtil {

    public static final String EXTRA_CIRCULAR_REVEAL_X = "EXTRA_CIRCULAR_REVEAL_X";
    public static final String EXTRA_CIRCULAR_REVEAL_Y = "EXTRA_CIRCULAR_REVEAL_Y";

    public static void prepareCircularReveal(Activity curActivity, Class<?> activity, View view) {
        ActivityOptions options = ActivityOptions
                .makeSceneTransitionAnimation(curActivity, view, "transition");

        int revealX = (int) ((view.getX() + view.getWidth()) / 2);
        int revealY = (int) ((view.getY() + view.getHeight()) / 2);

        Intent intent = new Intent(curActivity, activity);

        intent.putExtra(EXTRA_CIRCULAR_REVEAL_X, revealX);
        intent.putExtra(EXTRA_CIRCULAR_REVEAL_Y, revealY);

        curActivity.startActivity(intent, options.toBundle());
    }

    public static void circularReveal(){

    }
}
