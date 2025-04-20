package com.example.newearningapp.spin;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

import java.util.List;

public class PieView extends View {
    private RectF range = new RectF();
    private int radius;
    private Paint mArcPaint, mTextPaint, mBackgroundPaint;
    private float mStartAngle = 0;
    private int center, padding, targetIndex, roundOfNumber = 4;
    private boolean isRunning = false;
    private int defaultBackgroundColor = -1;
    private Drawable drawableCenterImage;
    private int textColor = 0xffffffff;
    private List<SpinItem> spinItemsList;
    private PieRotateListener pieRotateListener;

    public interface PieRotateListener {
        void rotateDone(int index);
    }

    public PieView(Context context) {
        super(context);
        init();
    }

    public PieView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setPieRotateListener(PieRotateListener listener) {
        this.pieRotateListener = listener;
    }

    private void init() {
        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(true);
        mArcPaint.setDither(true);

        mTextPaint = new Paint();
        mTextPaint.setColor(textColor);
        mTextPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
    }

    public void setData(List<SpinItem> spinItemsList) {
        this.spinItemsList = spinItemsList;
        invalidate();
    }

    public void setPieCenterImage(Drawable drawable) {
        drawableCenterImage = drawable;
        invalidate();
    }

    public void drawBackgroundColor(Canvas canvas, int color) {
        if (color == -1) {
            return;
        }
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(color);
        canvas.drawCircle(center, center, center, mBackgroundPaint);
    }

    public void setPieTextColor(int color) {
        textColor = color;
        invalidate();
    }
    public void setPieBackgroundColor(int color) {
        defaultBackgroundColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (spinItemsList == null) {
            return;
        }
        drawBackgroundColor(canvas, defaultBackgroundColor);
        range = new RectF(padding, padding, padding + radius, padding + radius);

        float tmpAngle = mStartAngle;
        float sweepAngle = 360 / spinItemsList.size();
        for (int i = 0; i < spinItemsList.size(); i++) {
            mArcPaint.setColor(spinItemsList.get(i).color);
            canvas.drawArc(range, tmpAngle, sweepAngle, true, mArcPaint);
            drawText(canvas, tmpAngle, sweepAngle, spinItemsList.get(i).Text);
            tmpAngle += sweepAngle;
        }
        drawCenterImage(canvas, drawableCenterImage);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = Math.min(getMeasuredWidth(), getMeasuredHeight());
        padding = getPaddingLeft() == 0 ? 10 : getPaddingLeft();
        radius = width - padding * 2;
        center = width / 2;
        setMeasuredDimension(width, width);
    }

    private void drawText(Canvas canvas, float tmpAngle, float sweepAngle, String string) {
        Path path = new Path();
        path.addArc(range, tmpAngle, sweepAngle);
        float txtWidth = mTextPaint.measureText(string);
        int offSet = (int) (radius * Math.PI / spinItemsList.size() / 2 - txtWidth / 2);
        int vOffSet = (int) (radius / 2 / 4);
        canvas.drawTextOnPath(string, path, offSet, vOffSet, mTextPaint);
    }

    private void drawCenterImage(Canvas canvas, Drawable drawable) {
        if (drawable == null) return;
        Bitmap bitmap = WheelUtils.bitmapToDrawable(drawable);
        bitmap = Bitmap.createScaledBitmap(bitmap, 90, 90, false);
        canvas.drawBitmap(bitmap, getMeasuredWidth() / 2 - bitmap.getWidth() / 2, getMeasuredHeight() / 2 - bitmap.getHeight() / 2, null);
    }

    private float getAngleOfTargetIndex() {
        return (360f / spinItemsList.size() * targetIndex);
    }

    public void setRound(int roundOfNumber) {
        this.roundOfNumber = roundOfNumber;
    }

    public void rotateTo(int index) {
        if (isRunning || spinItemsList == null || spinItemsList.size() == 0) {
            return;
        }
        targetIndex = index;
        setRotation(0);
        float targetAngle = 360 * roundOfNumber + 270 - getAngleOfTargetIndex() + (360f / spinItemsList.size());

        animate().setInterpolator(new DecelerateInterpolator())
                .setDuration(roundOfNumber * 500 + 900L)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(@NonNull Animator animation) {
                        isRunning = true;
                    }

                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
                        isRunning = false;
                        if (pieRotateListener != null) {
                            pieRotateListener.rotateDone(targetIndex);
                        }
                    }

                    @Override
                    public void onAnimationCancel(@NonNull Animator animation) {
                        isRunning = false;
                    }

                    @Override
                    public void onAnimationRepeat(@NonNull Animator animation) {
                    }
                })
                .rotation(targetAngle)
                .start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}