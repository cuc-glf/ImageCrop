package com.example.gaolf.imagecrop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Created by gaolf on 15/12/21.
 */
public class CropImageView extends ImageView {

    private enum TouchState {
        TOUCH_STATE_UNKNOWN,            // 两根手指以上 ---- 用户想干嘛？？
        TOUCH_STATE_IDLE,               // 没有拖动或缩放
        TOUCH_STATE_DRAG,               // 拖动
        TOUCH_STATE_ZOOM,               // 缩放

    }

    private static final float MAX_ZOOM_RELATIVE_TO_INTRINSIC = 3;

    private Matrix matrix; // image's controlling matrix
    private MatrixHelper matrixHelper;
    private TouchState touchState = TouchState.TOUCH_STATE_IDLE;                  // 当前的交互

    private Rect edgeRect;              // 裁剪区域

    private ScaleGestureDetector scaleGestureDetector;                              // 多指缩放
    private GestureDetector gestureDetector;                                        // 单指手势

    public CropImageView(Context context) {
        super(context);
        init();
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        matrixHelper = new MatrixHelper();
        scaleGestureDetector = new ScaleGestureDetector(getContext(), onScaleGestureListener);
        gestureDetector = new GestureDetector(getContext(), onGestureListener);
        maskPaint = new Paint();
        maskPaint.setColor(Color.argb(255 / 2, 0, 0, 0));
        maskPaint.setStyle(Paint.Style.FILL);

        if (BuildConfig.DEBUG) {
            Rect edge = new Rect(200, 200, 1000, 1000);
            setEdge(edge);
        }
    }

    public void startCrop() {
        startCrop(null);
    }

    public void startCrop(final Runnable onMatrixObtainedRunnable) {
        requestLayout();
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (isLayoutRequested()) {
                    return true;
                }
                getViewTreeObserver().removeOnPreDrawListener(this);
                matrix = getImageMatrix();
                setImageMatrix(matrix);
                setScaleType(ScaleType.MATRIX);
                matrixHelper.init(matrix, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
                onScale();
                onTranslate();
                if (onMatrixObtainedRunnable != null) {
                    onMatrixObtainedRunnable.run();
                }
                return false;
            }
        });
    }


    public void setEdge(Rect edgeRect) {
        this.edgeRect = new Rect(edgeRect);
        invalidate();
    }

    public RectF getCurrentRect() {
        RectF rect = new RectF(0, 0, matrixHelper.getIntrinsicWidth(), matrixHelper.getIntrinsicHeight());
        matrix.mapRect(rect);
        return rect;
    }

    public float getRawWidth() {
        return matrixHelper.getIntrinsicWidth();
    }

    public float getRawHeight() {
        return matrixHelper.getIntrinsicHeight();
    }

    private void afterScale() {
        onScale();
        onTranslate();
    }

    private void onScale() {
        matrixHelper.update(matrix);
        if (matrixHelper.getWidth() < edgeRect.width()) {
            scaleX = edgeRect.width() / matrixHelper.getWidth();
        }
        if (matrixHelper.getHeight() < edgeRect.height()) {
            scaleY = edgeRect.height() / matrixHelper.getHeight();
        }

        if (scaleX != 1 || scaleY != 1) {
            scale = Math.max(scaleX, scaleY);
            matrix.postScale(scale, scale, getWidth() / 2, getHeight() / 2);
            onTranslate();
            matrixHelper.update(matrix);
            scaleX = 1;
            scaleY = 1;
            scale = 1;
        }
        invalidate();
    }

    private void afterTranslate() {
        // check translate first..
        onScale();
        onTranslate();
    }

    private void onTranslate() {
        matrixHelper.update(matrix);
        if (matrixHelper.getLeft() > edgeRect.left) {
            dx = edgeRect.left - matrixHelper.getLeft();
        } else if (matrixHelper.getRight() < edgeRect.right) {
            dx = edgeRect.right - matrixHelper.getRight();
        }
        if (matrixHelper.getTop() > edgeRect.top) {
            dy = edgeRect.top - matrixHelper.getTop();
        } else if (matrixHelper.getBottom() < edgeRect.bottom) {
            dy = edgeRect.bottom - matrixHelper.getBottom();
        }
        if (dx != 0 || dy != 0) {
            matrix.postTranslate(dx, dy);
            matrixHelper.update(matrix);
            setImageMatrix(matrix);
            dx = 0;
            dy = 0;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        try {
            // 截个圆出来
            canvas.save();
            circlePath.reset();
            circlePath.addCircle((edgeRect.right + edgeRect.left) / 2, (edgeRect.bottom + edgeRect.top) / 2, (edgeRect.bottom - edgeRect.top) / 2, Path.Direction.CW);
            canvas.clipPath(circlePath, Region.Op.XOR);
            canvas.drawRect(0, 0, getWidth(), getHeight(), maskPaint);
            canvas.restore();
        } catch (Exception e) {
            e.printStackTrace();
            // 画成方的吧
            canvas.restore();
            canvas.drawRect(0, 0, getWidth(), edgeRect.top, maskPaint);                                         //top
            canvas.drawRect(0, edgeRect.bottom, getWidth(), getHeight(), maskPaint);                            //bottom
            canvas.drawRect(0, edgeRect.top, edgeRect.left, edgeRect.bottom, maskPaint);                        //left
            canvas.drawRect(edgeRect.right, edgeRect.top, getWidth(), edgeRect.bottom, maskPaint);              //right
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.e("gaolf", "action pointer down: " + event.getActionIndex());
                if (event.getActionIndex() == 1) {
                    touchState = TouchState.TOUCH_STATE_ZOOM;
                } else {
                    touchState = TouchState.TOUCH_STATE_UNKNOWN;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.e("gaolf", "action pointer up: " + event.getActionIndex());
                if (event.getActionIndex() == 2) {
                    touchState = TouchState.TOUCH_STATE_ZOOM;
                } else if (event.getActionIndex() == 1){
                    touchState = TouchState.TOUCH_STATE_DRAG;
                } else {
                    touchState = TouchState.TOUCH_STATE_UNKNOWN;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                Log.e("gaolf", "action down: " + event.getActionIndex());
                touchState = TouchState.TOUCH_STATE_DRAG;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                Log.e("gaolf", "action up/cancel: " + event.getActionIndex());
                touchState = TouchState.TOUCH_STATE_IDLE;
                break;
        }

        Log.e("gaolf", "touchState: " + touchState);

        switch (touchState) {
            case TOUCH_STATE_DRAG:
                gestureDetector.onTouchEvent(event);
                break;
            case TOUCH_STATE_ZOOM:
                scaleGestureDetector.onTouchEvent(event);
                break;
        }

        return true;
    }

    private ScaleGestureDetector.OnScaleGestureListener onScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor = detector.getScaleFactor();
            matrix.getValues(matrixValues);
            if (matrixValues[Matrix.MSCALE_X] * scaleFactor > MAX_ZOOM_RELATIVE_TO_INTRINSIC) {
                // 缩放后，缩放比过大
                if (edgeRect.width() / matrixHelper.getIntrinsicWidth() > MAX_ZOOM_RELATIVE_TO_INTRINSIC
                        || edgeRect.height() / matrixHelper.getIntrinsicHeight() > MAX_ZOOM_RELATIVE_TO_INTRINSIC) {
                    // 原图就比裁剪框小这么多倍..完全没有意义，不让用户缩放
                    // do nothing
                    scaleFactor = 1;
                } else {
                    scaleFactor = MAX_ZOOM_RELATIVE_TO_INTRINSIC / matrixValues[Matrix.MSCALE_X];
                }
            }
            matrix.postScale(scaleFactor, scaleFactor, edgeRect.left + edgeRect.width() / 2, edgeRect.top + edgeRect.height() / 2);

            afterScale();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
//            onScale();
        }
    };

    private GestureDetector.OnGestureListener onGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            matrix.postTranslate(-distanceX, -distanceY);
            afterTranslate();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {

            return true;
        }
    };

    private float dx = 0, dy = 0;
    private float scale = 1, scaleX = 1, scaleY = 1;
    private Paint maskPaint;
    private Path circlePath = new Path();
    private float[] matrixValues = new float[9];
    private float scaleFactor = 1/*, scaleFactorX = 1, getScaleFactorY = 1*/;
}
