package org.fanchuan.dip.simplemap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class SimpleMapWebView extends WebView {
    private GestureDetector gestureDetector;
    float currentScale;

    @SuppressWarnings("unused")
    public SimpleMapWebView(Context context) {
        super(context);
        postConstructor(context);
    }

    @SuppressWarnings("unused")
    public SimpleMapWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        postConstructor(context);
    }

    @SuppressWarnings("unused")
    public SimpleMapWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        postConstructor(context);
    }

    private void postConstructor(final Context context) {
        getSettings().setBuiltInZoomControls(true);
        //Gradle build system, place assets in /src/main/
        loadUrl("file:///android_asset/amundsen_sudpol_plano.svg");
        setWebViewClient(new WebViewClient() {
            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                super.onScaleChanged(view, oldScale, newScale);
                currentScale = newScale;
            }
        });
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public void onLongPress(MotionEvent e) {
                //super.onLongPress(e);
                Toast.makeText(context, "x: " + e.getX() + "; y: " + e.getY(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setColor(0x50600060);
        canvas.drawCircle(235.0f * currentScale, 270.0f * currentScale, 47.0f * currentScale, paint); //Zelt
        canvas.drawCircle(1035.0f * currentScale, 860.0f * currentScale, 47.0f * currentScale, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
}