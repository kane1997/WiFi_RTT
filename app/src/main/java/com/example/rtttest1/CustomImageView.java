package com.example.rtttest1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

//TODO Maybe to add on in future work

public class CustomImageView extends androidx.appcompat.widget.AppCompatImageView {
    private static final String TAG = "CustomImageView";
    private Paint paint = new Paint();
    private Path path = new Path();
    Drawable floor_plan;


    public CustomImageView(Context context) {
        super(context);
        init();
    }

    public CustomImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setPathEffect(new DashPathEffect(new float[] {10,5,5,5},1));
    }

    @Override
    protected void onDraw(Canvas canvas){
        Log.d(TAG,"onDraw");

        path.moveTo(300,300);
        path.lineTo(500,500);
        canvas.drawPath(path,paint);
    }

    private void onReceiveNewLocations(){
        path.moveTo(300,300);
        path.lineTo(500,500);
        invalidate();
    }

}