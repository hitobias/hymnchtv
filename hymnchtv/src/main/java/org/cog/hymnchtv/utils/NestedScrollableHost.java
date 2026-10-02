/*
 * hymnchtv: COG hymns' lyrics and sheet music viewing/player application
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.cog.hymnchtv.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Wraps a vertically scrolling child (the lyrics ScrollView) that lives inside a horizontal {@link ViewPager2}.
 * <p>
 * ViewPager2's RecyclerView takes over a gesture as soon as the horizontal travel passes the touch slop, whatever the
 * vertical travel is, and it is asked before the child. A slightly diagonal scroll therefore often turns the page.
 * Here the pager is shut out from the start of every gesture, and is only let back in once the gesture is clearly
 * horizontal. Based on the NestedScrollableHost of Google's ViewPager2 samples.
 * <p>
 * Decision, from the accumulated travel since ACTION_DOWN:
 * <ul>
 * <li>vertical: |dy| &gt; slop and |dy| &gt; |dx| * {@link #VERTICAL_BIAS}: the pager stays locked out for the rest of the gesture;</li>
 * <li>horizontal: |dx| &gt; slop * {@link #HORIZONTAL_COMMIT_SLOPS} and |dx| &gt; |dy| * {@link #HORIZONTAL_DOMINANCE}: the pager may take over;</li>
 * <li>otherwise (small or ambiguous travel, e.g. a thumb drifting sideways first): keep waiting.</li>
 * </ul>
 * Multi-touch (pinch) never hands the gesture to the pager.
 */
public class NestedScrollableHost extends FrameLayout {
    /** A drag counts as vertical when |dy| exceeds |dx| by this factor (0.5: anything steeper than about 27 degrees from horizontal). */
    static final float VERTICAL_BIAS = 0.5f;

    /** A drag counts as horizontal only when |dx| exceeds |dy| by this factor (the mirror of {@link #VERTICAL_BIAS}). */
    static final float HORIZONTAL_DOMINANCE = 2f;

    /** Horizontal travel, in touch slops, needed before the pager may take over (a deliberate swipe, not a drift). */
    static final float HORIZONTAL_COMMIT_SLOPS = 2f;

    private final int touchSlop;
    private float initialX;
    private float initialY;
    private boolean decided;

    public NestedScrollableHost(@NonNull Context context) {
        this(context, null);
    }

    public NestedScrollableHost(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        handleInterceptTouchEvent(e);
        return false;
    }

    private void handleInterceptTouchEvent(MotionEvent e) {
        ViewParent parent = getParent();
        if (parent == null) {
            return;
        }
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialX = e.getX();
                initialY = e.getY();
                decided = false;
                // The pager is shut out until the gesture proves horizontal
                parent.requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // Pinch (zoomable lyrics text): never a page turn
                decided = true;
                parent.requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_MOVE:
                if (!decided && e.getPointerCount() == 1) {
                    decide(parent, Math.abs(e.getX() - initialX), Math.abs(e.getY() - initialY));
                }
                break;

            default:
                break;
        }
    }

    private void decide(ViewParent parent, float absDx, float absDy) {
        if (absDy > touchSlop && absDy > absDx * VERTICAL_BIAS) {
            decided = true; // stays locked out
        }
        else if (absDx > touchSlop * HORIZONTAL_COMMIT_SLOPS && absDx > absDy * HORIZONTAL_DOMINANCE) {
            decided = true;
            parent.requestDisallowInterceptTouchEvent(false);
        }
    }
}
