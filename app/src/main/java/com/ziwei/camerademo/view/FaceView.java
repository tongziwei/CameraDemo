package com.ziwei.camerademo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by clara.tong on 2020/5/28
 */
public class FaceView extends View {
    private ArrayList<RectF> mFaces;
    private Paint mPaint;


    public FaceView(Context context) {
        super(context);
        init(context);
    }

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FaceView(Context context,  AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mFaces!=null && mFaces.size()>0){
            for(int i=0;i<mFaces.size();i++){
                canvas.drawRect(mFaces.get(i),mPaint);
            }
        }

    }

    public void setFaces(ArrayList<RectF> faces){
        this.mFaces = faces;
        postInvalidate();
    }

    public void clearFaces(){
        if(mFaces!=null && mFaces.size()>0){
            mFaces.clear();
        }
        postInvalidate();
    }

    private void init(Context context){
        mPaint = new Paint();
        mPaint.setColor(Color.GREEN);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f,
                context.getResources().getDisplayMetrics()));
    }


}

