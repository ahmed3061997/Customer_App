package com.fc.mis.charitable.models;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.animation.AnimationUtils;

public class HideTopViewOnScrollBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {
    protected static final int ENTER_ANIMATION_DURATION = 225;
    protected static final int EXIT_ANIMATION_DURATION = 175;
    private static final int STATE_SCROLLED_DOWN = 1;
    private static final int STATE_SCROLLED_UP = 2;
    private int height = 0;
    private int currentState = 2;
    private ViewPropertyAnimator currentAnimator;

    public HideTopViewOnScrollBehavior() {
    }

    public HideTopViewOnScrollBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
        this.height = child.getMeasuredHeight();
        return super.onLayoutChild(parent, child, layoutDirection);
    }

    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V child, View directTargetChild, View target, int nestedScrollAxes) {
        return nestedScrollAxes == 2;
    }

    public void onNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        Log.d("OnScrollBehavior", "" + dyConsumed);
        if (this.currentState != 2 && dyConsumed > 0) {
            this.slideUp(child);
        } else if (this.currentState != 1 && dyConsumed < 0) {
            this.slideDown(child);
        }
    }

    protected void slideUp(V child) {
        if (this.currentAnimator != null) {
            this.currentAnimator.cancel();
            child.clearAnimation();
        }

        this.currentState = 2;
        this.animateChildTo(child, -height, 225L, AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR);
    }

    protected void slideDown(V child) {
        if (this.currentAnimator != null) {
            this.currentAnimator.cancel();
            child.clearAnimation();
        }

        this.currentState = 1;
        this.animateChildTo(child, 0, 175L, AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR);
    }

    private void animateChildTo(V child, int targetY, long duration, TimeInterpolator interpolator) {
        this.currentAnimator = child.animate().y((float) targetY).setInterpolator(interpolator).setDuration(duration).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                HideTopViewOnScrollBehavior.this.currentAnimator = null;
            }
        });
    }
}

