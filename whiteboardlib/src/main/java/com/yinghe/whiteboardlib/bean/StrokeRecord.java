package com.yinghe.whiteboardlib.bean;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

public class StrokeRecord {
    public static final int STROKE_TYPE_ERASER = 0;
    public static final int STROKE_TYPE_DRAW= 1;
    //    public static final int STROKE_TYPE_DRAW_BOLD= 1;
    public static final int STROKE_TYPE_LINE= 2;
    public static final int STROKE_TYPE_CIRCLE=3;
    public static final int STROKE_TYPE_RECTANGLE= 4;
    public static final int STROKE_TYPE_TEXT= 5;
    public static final int STROKE_TYPE_BITMAP= 6;

    public int type;//记录类型
    public Paint paint;//笔类
    public Path path;//画笔路径数据
    public PointF[] linePoints; //线数据
    public RectF rect; //圆数据
//    public Rect rectangleRect; //矩形数据
    public String text;//文字
    public PointF[] textLocation;//文字位置
    public Bitmap bitmap;//图形
    public Matrix matrix;//图形

    public StrokeRecord(int type) {
        this.type = type;
    }
}