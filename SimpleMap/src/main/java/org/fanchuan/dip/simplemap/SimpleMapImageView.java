package org.fanchuan.dip.simplemap;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class SimpleMapImageView extends ImageView {
    String TAG = SimpleMapImageView.class.getSimpleName();
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    public SimpleMapImageView(Context context) {
        super(context);
        postConstructor(context);
    }

    public SimpleMapImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        postConstructor(context);
    }

    public SimpleMapImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        postConstructor(context);
    }

    private void postConstructor (Context context) {
        setImageResource(R.drawable.dt_kameramuseum_plech_museumsplan_2013);
        initGestureDetectors(context);
    }

    private void initGestureDetectors(Context context) {
        //Detect pinching (scale) gestures
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            float initialScaleX;
            float initialScaleY;

            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                initialScaleX = getScaleX();
                initialScaleY = getScaleY();
                return true;
            }

            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                setScaleX(initialScaleX * scaleGestureDetector.getScaleFactor());
                setScaleY(initialScaleY * scaleGestureDetector.getScaleFactor());
                return false;
            }

            public void onScaleEnd(ScaleGestureDetector detector) {
                //Don't do anything
            }
        });

        //Detect 1 finger scrolling gestures
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                super.onScroll(e1, e2, distanceX, distanceY);
                Log.i(TAG, "Scrolling");
                scrollBy((int)distanceX, (int)distanceY);
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //2 detectors can interpret the motion event in sequence
        gestureDetector.onTouchEvent(ev);
        return scaleGestureDetector.onTouchEvent(ev);
    }
}
