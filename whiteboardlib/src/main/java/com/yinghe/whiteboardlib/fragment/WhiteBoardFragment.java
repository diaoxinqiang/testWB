package com.yinghe.whiteboardlib.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.yinghe.whiteboardlib.MultiImageSelector;
import com.yinghe.whiteboardlib.R;
import com.yinghe.whiteboardlib.Utils.BitmapUtils;
import com.yinghe.whiteboardlib.bean.StrokeRecord;
import com.yinghe.whiteboardlib.view.ScaleView;
import com.yinghe.whiteboardlib.view.SketchView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_BITMAP;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_CIRCLE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_DRAW;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_ERASER;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_LINE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_RECTANGLE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_TEXT;

public class WhiteBoardFragment extends Fragment implements SketchView.OnDrawChangedListener, View.OnClickListener {

    public static final int COLOR_BLACK = Color.parseColor("#ff000000");
    public static final int COLOR_RED = Color.parseColor("#ffff4444");
    public static final int COLOR_GREEN = Color.parseColor("#ff99cc00");
    public static final int COLOR_ORANGE = Color.parseColor("#ffffbb33");
    public static final int COLOR_BLUE = Color.parseColor("#ff33b5e5");
    private static final int REQUEST_IMAGE = 2;

    public static int bitmapSize = 300;

    ScaleView scaleView;

    RelativeLayout whiteBoardLayout;//画板布局
    SketchView mSketchView;//画板
    ImageView ivBg;//画板背景图片
    ImageView ivBgColor;//画板背景颜色

    View controlLayout;//控制布局
    ImageView stroke;//画笔
    ImageView eraser;//橡皮擦
    ImageView undo;//撤销
    ImageView redo;//取消撤销
    ImageView erase;//清空
    ImageView sketchSave;//保存
    ImageView sketchPhoto;//加载图片

    View sureActionLayout;//确认布局
    ImageView sureAction;
    ImageView cancelAction;

    RadioGroup strokeTypeRG,strokeColorRG;

    Button btShowBg;
    Button btShowBgGray;

    Activity activity;//上下文

    int strokeMode;//模式
    int strokeType;//模式

    int pupWindowsDPWidth = 300;//弹窗宽度，单位DP
    int strokePupWindowsDPHeight = 275;//画笔弹窗高度，单位DP
    int eraserPupWindowsDPHeight = 90;//橡皮擦弹窗高度，单位DP

    PopupWindow strokePopupWindow, eraserPopupWindow;//画笔、橡皮擦参数设置弹窗实例
    private View popupStrokeLayout, popupEraserLayout;//画笔、橡皮擦弹窗布局
    private SeekBar strokeSeekBar, strokeAlphaSeekBar, eraserSeekBar;
    private ImageView strokeImageView, strokeAlphaImage, eraserImageView;//画笔宽度，画笔不透明度，橡皮擦宽度IV
    private int size;
    private AlertDialog dialog;
    private ArrayList<String> mSelectPath;
    public static int sketchViewHight;
    public static int sketchViewWidth;

    public static WhiteBoardFragment newInstance() {
        return new WhiteBoardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();//初始化上下文
        bitmapSize = Math.min(activity.getWindowManager().getDefaultDisplay().getWidth(), activity.getWindowManager().getDefaultDisplay().getHeight()) ;

        Log.i("xxx", "WhiteBoardFragment onCreate()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_white_board, container, false);
        findView(view);//载入所有的按钮实例
        initDrawParams();//初始化绘画参数
        initPopupWindows();//初始化弹框
        return view;
    }

    private void initDrawParams() {
        //默认为画笔模式
        strokeMode = STROKE_TYPE_DRAW;

        //画笔宽度缩放基准参数
        Drawable circleDrawable = getResources().getDrawable(R.drawable.circle);
        assert circleDrawable != null;
        size = circleDrawable.getIntrinsicWidth();
    }
    private void initPopupWindows() {
        //画笔弹窗
        strokePopupWindow = new PopupWindow(activity);
        strokePopupWindow.setContentView(popupStrokeLayout);//设置主体布局
        strokePopupWindow.setWidth(BitmapUtils.dip2px(getActivity(), pupWindowsDPWidth));//宽度
//        strokePopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        strokePopupWindow.setHeight(BitmapUtils.dip2px(getActivity(), strokePupWindowsDPHeight));//高度
        strokePopupWindow.setFocusable(true);
        strokePopupWindow.setBackgroundDrawable(new BitmapDrawable());//设置空白背景
        strokePopupWindow.setAnimationStyle(R.style.mypopwindow_anim_style);//动画
        strokeTypeRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.stroke_type_rbtn_draw) {
                    strokeType = STROKE_TYPE_DRAW;
                } else if (checkedId == R.id.stroke_type_rbtn_line) {
                    strokeType = STROKE_TYPE_LINE;
                } else if (checkedId == R.id.stroke_type_rbtn_circle) {
                    strokeType = STROKE_TYPE_CIRCLE;
                } else if (checkedId == R.id.stroke_type_rbtn_rectangle) {
                    strokeType = STROKE_TYPE_RECTANGLE;
                }else if (checkedId == R.id.stroke_type_rbtn_text) {
                    strokeType = STROKE_TYPE_TEXT;
                }
                mSketchView.setStrokeType(strokeType);
            }
        });
        strokeColorRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int color = COLOR_BLACK;
                if (checkedId == R.id.stroke_color_black) {
                    color = COLOR_BLACK;
                } else if (checkedId == R.id.stroke_color_red) {
                    color = COLOR_RED;
                } else if (checkedId == R.id.stroke_color_green) {
                    color = COLOR_GREEN;
                } else if (checkedId == R.id.stroke_color_orange) {
                    color = COLOR_ORANGE;
                } else if (checkedId == R.id.stroke_color_blue) {
                    color = COLOR_BLUE;
                }
                mSketchView.setStrokeColor(color);
            }
        });
        //画笔宽度拖动条
        strokeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                setSeekBarProgress(progress, STROKE_TYPE_DRAW);
            }
        });
        strokeSeekBar.setProgress(SketchView.DEFAULT_STROKE_SIZE);
//        strokeColorRG.check(R.id.stroke_color_black);

        //画笔不透明度拖动条
        strokeAlphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                int alpha = (progress * 255) / 100;//百分比转换成256级透明度
                mSketchView.setStrokeAlpha(alpha);
                strokeAlphaImage.setAlpha(alpha);
            }
        });
        strokeAlphaSeekBar.setProgress(SketchView.DEFAULT_STROKE_ALPHA);

        //橡皮擦弹窗
        eraserPopupWindow = new PopupWindow(activity);
        eraserPopupWindow.setContentView(popupEraserLayout);//设置主体布局
        eraserPopupWindow.setWidth(BitmapUtils.dip2px(getActivity(), pupWindowsDPWidth));//宽度200dp
//        eraserPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        eraserPopupWindow.setHeight(BitmapUtils.dip2px(getActivity(), eraserPupWindowsDPHeight));//高度自适应
        eraserPopupWindow.setFocusable(true);
        eraserPopupWindow.setBackgroundDrawable(new BitmapDrawable());//设置空白背景
        eraserPopupWindow.setAnimationStyle(R.style.mypopwindow_anim_style);//动画
        //橡皮擦宽度拖动条
        eraserSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                setSeekBarProgress(progress, STROKE_TYPE_ERASER);
            }
        });
        eraserSeekBar.setProgress(SketchView.DEFAULT_ERASER_SIZE);
    }


    private void findView(View view) {

        scaleView = (ScaleView) view.findViewById(R.id.scale_view);
//        scaleView.setEnabled(false);
        //画板整体布局
        whiteBoardLayout = (RelativeLayout) view.findViewById(R.id.white_board);
        mSketchView = (SketchView) view.findViewById(R.id.drawing);
        ivBg = (ImageView) view.findViewById(R.id.iv_bg);
        ivBgColor = (ImageView) view.findViewById(R.id.iv_bg_color);
        stroke = (ImageView) view.findViewById(R.id.sketch_stroke);
        eraser = (ImageView) view.findViewById(R.id.sketch_eraser);
        undo = (ImageView) view.findViewById(R.id.sketch_undo);
        redo = (ImageView) view.findViewById(R.id.sketch_redo);
        erase = (ImageView) view.findViewById(R.id.sketch_erase);
        sketchSave = (ImageView) view.findViewById(R.id.sketch_save);
        sketchPhoto = (ImageView) view.findViewById(R.id.sketch_photo);
        sketchPhoto.setAlpha(0.4f);
        controlLayout = view.findViewById(R.id.controlLayout);
        sureActionLayout = view.findViewById(R.id.sure_action_layout);
        sureAction = (ImageView) view.findViewById(R.id.sure_action);
        cancelAction = (ImageView) view.findViewById(R.id.cancel_action);
        btShowBg = (Button) view.findViewById(R.id.bt_show_bg);
        btShowBgGray = (Button) view.findViewById(R.id.bt_show_bg_gray);

        //设置点击监听
        mSketchView.setOnDrawChangedListener(this);//设置撤销动作监听器
        stroke.setOnClickListener(this);
        eraser.setOnClickListener(this);
        undo.setOnClickListener(this);
        redo.setOnClickListener(this);
        erase.setOnClickListener(this);
        sketchSave.setOnClickListener(this);
        sketchPhoto.setOnClickListener(this);
        sureAction.setOnClickListener(this);
        cancelAction.setOnClickListener(this);

        // popupWindow布局
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity
                .LAYOUT_INFLATER_SERVICE);
        //画笔参数布局
        popupStrokeLayout = inflater.inflate(R.layout.popup_sketch_stroke, null);
        strokeImageView = (ImageView) popupStrokeLayout.findViewById(R.id.stroke_circle);
        strokeAlphaImage = (ImageView) popupStrokeLayout.findViewById(R.id.stroke_alpha_circle);
        strokeSeekBar = (SeekBar) (popupStrokeLayout.findViewById(R.id.stroke_seekbar));
        strokeAlphaSeekBar = (SeekBar) (popupStrokeLayout.findViewById(R.id.stroke_alpha_seekbar));
        //画笔颜色
        strokeTypeRG = (RadioGroup) popupStrokeLayout.findViewById(R.id.stroke_type_radio_group);
        strokeColorRG = (RadioGroup) popupStrokeLayout.findViewById(R.id.stroke_color_radio_group);
        // popupWindow布局
        LayoutInflater inflater2 = (LayoutInflater) getActivity().getSystemService(Activity
                .LAYOUT_INFLATER_SERVICE);
        //橡皮擦参数布局
        popupEraserLayout = inflater2.inflate(R.layout.popup_sketch_eraser, null);
        eraserImageView = (ImageView) popupEraserLayout.findViewById(R.id.stroke_circle);
        eraserSeekBar = (SeekBar) (popupEraserLayout.findViewById(R.id.stroke_seekbar));
        getSketchSize();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
       getSketchSize();
    }
    private void getSketchSize() {
        ViewTreeObserver vto = mSketchView.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                if(sketchViewHight==0&&sketchViewWidth==0) {
                    int height = mSketchView.getMeasuredHeight();
                    int width = mSketchView.getMeasuredWidth();
                    sketchViewHight = height;
                    sketchViewWidth = width;
                    Log.i("onPreDraw", sketchViewHight + "  " + sketchViewWidth);
                }
//                Log.i("onPreDraw h w", height + "  " + width);

                return true;
            }
        });
        Log.i("getSketchSize", sketchViewHight + "  " + sketchViewWidth);
    }

    protected void setSeekBarProgress(int progress, int drawMode) {
        int calcProgress = progress > 1 ? progress : 1;
        int newSize = Math.round((size / 100f) * calcProgress);
        int offset = Math.round((size - newSize) / 2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(newSize, newSize);
        lp.setMargins(offset, offset, offset, offset);
        if (drawMode == STROKE_TYPE_DRAW) {
            strokeImageView.setLayoutParams(lp);
        } else {
            eraserImageView.setLayoutParams(lp);
        }
        mSketchView.setSize(newSize, drawMode);
    }


    @Override
    public void onDrawChanged() {
        // Undo
        if (mSketchView.getRecordCount() > 0)
            setAlpha(undo, 1f);
        else
            setAlpha(undo, 0.4f);
        // Redo
        if (mSketchView.getRedoCount() > 0)
            setAlpha(redo, 1f);
        else
            setAlpha(redo, 0.4f);
    }

    void setAlpha(View v, float alpha) {
        v.setAlpha(alpha);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.sketch_stroke) {
            sketchPhoto.setAlpha(0.4f);
            if (mSketchView.getStrokeType() != STROKE_TYPE_ERASER) {
                showPopup(v, STROKE_TYPE_DRAW);
            } else {
                int checkedId = strokeTypeRG.getCheckedRadioButtonId();
                if (checkedId == R.id.stroke_type_rbtn_draw) {
                    strokeType = STROKE_TYPE_DRAW;
                } else if (checkedId == R.id.stroke_type_rbtn_line) {
                    strokeType = STROKE_TYPE_LINE;
                } else if (checkedId == R.id.stroke_type_rbtn_circle) {
                    strokeType = STROKE_TYPE_CIRCLE;
                } else if (checkedId == R.id.stroke_type_rbtn_rectangle) {
                    strokeType = STROKE_TYPE_RECTANGLE;
                } else if (checkedId == R.id.stroke_type_rbtn_text) {
                    strokeType = STROKE_TYPE_TEXT;
                }
                mSketchView.setStrokeType(strokeType);
                setAlpha(eraser, 0.4f);
                setAlpha(stroke, 1f);
            }
        } else if (id == R.id.sketch_eraser) {
            sketchPhoto.setAlpha(0.4f);
            if (mSketchView.getStrokeType() == STROKE_TYPE_ERASER) {
                showPopup(v, STROKE_TYPE_ERASER);
            } else {
                mSketchView.setStrokeType(STROKE_TYPE_ERASER);
                setAlpha(stroke, 0.4f);
                setAlpha(eraser, 1f);
            }
        } else if (id == R.id.sketch_undo) {
            mSketchView.undo();
        } else if (id == R.id.sketch_redo) {
            mSketchView.redo();
        } else if (id == R.id.sketch_erase) {
            askForErase();
        } else if (id == R.id.sketch_save) {
            if (mSketchView.getRecordCount() == 0) {
                Toast.makeText(getActivity(), "你还没有手绘", Toast.LENGTH_SHORT).show();
                return;
            }
            //保存
            final EditText et = new EditText(activity);
            et.setText("新文件名");
            et.setHint("新文件名");
            et.setGravity(Gravity.CENTER);
            et.setSelectAllOnFocus(true);
            new AlertDialog.Builder(getActivity())
                    .setTitle("请输入保存文件名")
                    .setMessage("")
                    .setView(et)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String input = et.getText().toString();
                            save(input + ".png");
                        }
                    })
                    .show();
        } else if (id == R.id.sketch_photo) {
            MultiImageSelector selector = MultiImageSelector.create(getActivity());
            selector.showCamera(true);
            selector.count(9);
            selector.single();
            selector.origin(mSelectPath);
            selector.start(this, REQUEST_IMAGE);
        } else if (id == R.id.sure_action) {
            StrokeRecord record = new StrokeRecord(STROKE_TYPE_BITMAP);
            record.bitmap = scaleView.getPhotoSampleBM();
            record.matrix = new Matrix(scaleView.getPhotoMatrix());
            mSketchView.addRecord(record);
            scaleView.setImageBitmap(null);
            scaleView.setVisibility(View.GONE);
            sureActionLayout.setVisibility(View.GONE);
        } else if (id == R.id.cancel_action) {
            scaleView.setImageBitmap(null);
            scaleView.setVisibility(View.GONE);
            sureActionLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == getActivity().RESULT_OK) {
                mSelectPath = data.getStringArrayListExtra(MultiImageSelector.EXTRA_RESULT);
                String path = "";
                if (mSelectPath.size() == 1) {
                    path = mSelectPath.get(0);
                } else if (mSelectPath == null || mSelectPath.size() == 0) {
                    Toast.makeText(getActivity(), "图片加载失败,请重试!", Toast.LENGTH_LONG).show();
                }
                //加载图片
                scaleView.setPhotoPath(path);
                sketchPhoto.setAlpha(1.0f);
                scaleView.setVisibility(View.VISIBLE);
                sureActionLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showPopup(View anchor, int drawMode) {
        if (BitmapUtils.isLandScreen(activity)) {
            if (drawMode == STROKE_TYPE_DRAW) {
                strokePopupWindow.showAsDropDown(anchor, BitmapUtils.dip2px(activity, -pupWindowsDPWidth), -anchor.getHeight());
            } else {
                eraserPopupWindow.showAsDropDown(anchor, BitmapUtils.dip2px(activity, -pupWindowsDPWidth), -anchor.getHeight());
            }
        } else {
            if (drawMode == STROKE_TYPE_DRAW) {
                strokePopupWindow.showAsDropDown(anchor, 0, BitmapUtils.dip2px(activity, -strokePupWindowsDPHeight) - anchor.getHeight());
            } else {
                eraserPopupWindow.showAsDropDown(anchor, 0, BitmapUtils.dip2px(activity, -eraserPupWindowsDPHeight) - anchor.getHeight());
            }
        }
    }

    public void save(final String imgName) {
        dialog = new AlertDialog.Builder(activity)
                .setTitle("保存手绘")
                .setMessage("保存中...")
                .show();
        final Bitmap newBM = getResultBitmap();

        new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] params) {

                if (newBM != null) {
                    String str = System.currentTimeMillis() + "";

                    try {
                        String filePath = "/mnt/sdcard/YingHe/";
                        File dir = new File(filePath);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        File f = new File(filePath, imgName);
                        if (!f.exists()) {
                            f.createNewFile();
                        } else {
                            f.delete();
                        }
                        FileOutputStream out = new FileOutputStream(f);
                        newBM.compress(Bitmap.CompressFormat.PNG, 90, out);
                        out.close();

                        dialog.dismiss();
                        return "保存手绘成功" + filePath;
                    } catch (Exception e) {

                        dialog.dismiss();
                        Log.i("AAA", e.getMessage());
                        return "保存手绘失败" + e.getMessage();
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);

                Toast.makeText(getActivity(), (String) o, Toast.LENGTH_SHORT).show();

            }
        }.execute("");

    }

    @NonNull
    public Bitmap getResultBitmap() {
        Bitmap bitmap1 = mSketchView.getBitmap();
        int bgWidth = mSketchView.getWidth();
        int bgHeight =mSketchView.getHeight();
        final Bitmap newBM = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(newBM);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));//抗锯齿
        canvas.drawBitmap(bitmap1, 0, 0, null);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
//        newBM.compress(Bitmap.CompressFormat.PNG,80)
        return newBM;
    }

    private void askForErase() {
        new AlertDialog.Builder(getActivity())
                .setMessage("擦除手绘?")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSketchView.erase();
                    }
                })
                .create()
                .show();
    }
}
