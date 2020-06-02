package com.ziwei.camerademo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ziwei.camerademo.util.PhotoBitmapUtil;
import com.ziwei.camerademo.view.FaceView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 参考https://blog.csdn.net/u010126792/article/details/86529646
 * 相机接口相关知识https://www.jianshu.com/p/f8d0d1467584
 * 人脸检测 https://www.jianshu.com/p/0f8571f7decc，https://www.jianshu.com/p/3bb301c302e8
 */
public class Main2Activity extends AppCompatActivity implements SurfaceHolder.Callback,Camera.PreviewCallback{
    private static final String[] permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private SurfaceView mSurfaceView;
    private ImageButton mIbtnTakePicture;
    private ImageButton mIbtnSwitchCamera;
    private FaceView mFaceView;
    private boolean hasPermission;

    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private int useWidth;
    private int useHeight;
    private int mDisplayOrientation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initView();
        requestPermission();
        initSurfaceView();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    private void initView(){
        mSurfaceView = (SurfaceView)findViewById(R.id.sv_pre);
        mIbtnTakePicture = (ImageButton)findViewById(R.id.ibtn_take_picture);
        mIbtnSwitchCamera = (ImageButton)findViewById(R.id.ibtn_switch_camera);
        mFaceView = (FaceView)findViewById(R.id.faceview);

        WindowManager wm = (WindowManager) Main2Activity.this.getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        Log.d("tzw", "initView: width"+width);
        int height = wm.getDefaultDisplay().getHeight();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height =  width*4/3;//height;  //初始化设置surfaceView宽高比为3:4
        useWidth = width;
        useHeight = width*4/3; //height;
        mSurfaceView.setLayoutParams(layoutParams);

        mIbtnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(mCamera!=null){
                   mCamera.takePicture(null, null, new Camera.PictureCallback() {
                       @Override
                       public void onPictureTaken(byte[] data, Camera camera) {
                           savePic(data);
                          mCamera.startPreview();
                          startFaceDetect();

                       }
                   });
               }
            }
        });

        mIbtnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               switchCamera();
            }
        });

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(mCamera!=null && mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
                    handleFocus(event,mCamera);
                }
                return false;
            }
        });

    }



    private void requestPermission(){
        if(Build.VERSION.SDK_INT>=23){
            ActivityCompat.requestPermissions(Main2Activity.this,permissions,1);
        }else{
            hasPermission = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if(isGrantedResult()){
                    hasPermission = true;
                }else{
                    hasPermission = false;
                }
                break;
        }
    }

    private boolean isGrantedResult(int... grantResults) {
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) return false;
        }
        return true;
    }
    /*-------------------------------------------------------------------------------------------*/

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
            //相机预览回调，data默认数据格式为NV21
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("tzw", "surfaceCreated: ");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("tzw", "surfaceChanged: ");
        initCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("tzw", "surfaceDestroyed: ");
         mCamera.stopPreview();
    }

    /*-------------------------------------------------------------------------------------------*/
    private void initSurfaceView(){
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    /**
     * 初始化相机
     */
    private void initCamera(){
         if(mCamera!=null){
             releaseCamera();
         }

         mCamera = Camera.open(mCameraId);
         if(mCamera!=null){
             try {
                 mCamera.setPreviewDisplay(mSurfaceHolder);
                 mCamera.setPreviewCallback(this);
             } catch (IOException e) {
                 e.printStackTrace();
             }
             Camera.Parameters parameters= mCamera.getParameters();
             parameters.setPreviewFormat(ImageFormat.NV21);
             if(mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
                 parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); //
             }
             mCamera.setParameters(parameters);
             Camera.Size tempSize = setPreviewSize(mCamera,useHeight,useWidth);//设置预览大小
             setPictureSize(mCamera,useHeight,useWidth);  //设置图片大小

              mDisplayOrientation = calculateCameraPreviewOrientation(Main2Activity.this,mCameraId);
             mCamera.setDisplayOrientation(mDisplayOrientation);  //设置预览方向
             mCamera.startPreview();
             startFaceDetect();

         }

    }

    private void releaseCamera(){
        if(mCamera!=null){
            mSurfaceHolder.removeCallback(this);
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.lock();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 摄像头切换
     */
    private void switchCamera(){
        if(mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }else{
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        mFaceView.clearFaces();
        initCamera();
    }


    /**
     * 设置预览角度，setDisplayOrientation本身只能改变预览的角度
     * previewFrameCallback以及拍摄出来的照片是不会发生改变的，拍摄出来的照片角度依旧不正常的
     * 拍摄的照片需要自行处理
     * 这里Nexus5X的相机简直没法吐槽，后置摄像头倒置了，切换摄像头之后就出现问题了。
     * @param activity
     */
    public static int calculateCameraPreviewOrientation(Activity activity,int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;   //info.orientation指的是相机采集图片的角度
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * 保存图片
     * @param data
     */
    private void savePic(final byte[] data) {
        String path = Environment.getExternalStorageDirectory().getPath() + "/CameraDemo/";
        File pathDir = new File(path);
        if (!pathDir.exists()) {
            pathDir.mkdir();
        }
        String imgPath = path + System.currentTimeMillis() + ".jpg";
        File imgFile = new File(imgPath);
        if (imgFile.exists()) {
            imgFile.delete();
        }

        Bitmap rawBitmap = BitmapFactory.decodeByteArray(data,0,data.length);
        Bitmap destBitmap = PhotoBitmapUtil.rotateBmp(rawBitmap,mCameraId);

        try {
            FileOutputStream fos = new FileOutputStream(imgFile);
            destBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            Toast.makeText(Main2Activity.this,"图片已保存",Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
/***************************************聚焦相关**************************************************************/
    private  void handleFocus(MotionEvent event, Camera camera) {
        int viewWidth = useWidth;
        int viewHeight = useHeight;
        Rect focusRect = calculateTapArea(event.getX(), event.getY(),  viewWidth, viewHeight,1.0f);


        Camera.Parameters params = camera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        //getMaxNumFocusAreas目前市场大部分手机这个方法的返回值，前置摄像头都是0，后置摄像头都是1，
        // 说明前置摄像头一般不支持设置聚焦，而后置摄像头一般也只支持单个区域的聚焦
        if (params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 800));
            params.setFocusAreas(focusAreas);
        } else {
            //focus areas not supported
        }
        //首先保存原来的对焦模式，然后设置为macro，对焦回调后设置为保存的对焦模式
        final String currentFocusMode = params.getFocusMode();
        //一定要首先取消
        camera.cancelAutoFocus();
        camera.setParameters(params);

        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                //回调后 还原模式
                Camera.Parameters params = camera.getParameters();
                params.setFocusMode(currentFocusMode);
                camera.setParameters(params);
                if(success){
                    Toast.makeText(Main2Activity.this,"对焦区域对焦成功",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 转换对焦区域
     * 范围(-1000, -1000, 1000, 1000)
     */
    private  Rect calculateTapArea(float x, float y,  int width, int height, float coefficient) {
        float focusAreaSize = 200;
        int areaSize = (int) (focusAreaSize * coefficient);
        int surfaceWidth = width;
        int surfaceHeight = height;
        int centerX = (int) (x / surfaceHeight * 2000 - 1000);
        int centerY = (int) (y / surfaceWidth * 2000 - 1000);
        int left = clamp(centerX - (areaSize / 2), -1000, 1000);
        int top = clamp(centerY - (areaSize / 2), -1000, 1000);
        int right = clamp(left + areaSize, -1000, 1000);
        int bottom = clamp(top + areaSize, -1000, 1000);
        return new Rect(left, top, right, bottom);
    }

    //不大于最大值，不小于最小值
    private  int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

/************************************设置图片大小和预览界面的大小*********************************************/
    /**
     * 设置拍摄图片的大小
     * @param camera
     * @param expectWidth 期望的宽
     * @param expectHeight 期望的高
     * @return
     */
    private void setPictureSize(Camera camera ,int expectWidth,int expectHeight){
        Camera.Parameters parameters = camera.getParameters();
        Point point = new Point(expectWidth, expectHeight);
        Camera.Size size = findProperSize(point,parameters.getSupportedPictureSizes());
        if(size!=null){
            Log.d("tzw", "setPictureSize:width: "+size.width+"height:"+size.height);
            parameters.setPictureSize(size.width, size.height);//必须从图片支持的大小中选取设置
            camera.setParameters(parameters);
        }

    }

    /**
     * 设置预览界面大小
     * @param camera
     * @param expectWidth 期望的宽
     * @param expectHeight 期望的高
     * @return
     */
    private Camera.Size setPreviewSize(Camera camera, int expectWidth, int expectHeight) {
        Camera.Parameters parameters = camera.getParameters();
        Point point = new Point(expectWidth, expectHeight);
        Camera.Size size = findProperSize(point,parameters.getSupportedPreviewSizes());
        if(size!=null){
            Log.d("tzw", "setPreviewSize:  "+size.width+"height:"+size.height);
            parameters.setPreviewSize(size.width, size.height);//必须从预览支持的大小中选取设置
            camera.setParameters(parameters);
        }
        return size;
    }

    /**
     * 找出最合适的尺寸，规则如下：
     * 1.将尺寸按比例分组，找出比例最接近屏幕比例的尺寸组
     * 2.在比例最接近的尺寸组中找出最接近屏幕尺寸且大于屏幕尺寸的尺寸
     * 3.如果没有找到，则忽略2中第二个条件再找一遍，应该是最合适的尺寸了
     */
    private static Camera.Size findProperSize(Point surfaceSize, List<Camera.Size> sizeList) {
        if (surfaceSize.x <= 0 || surfaceSize.y <= 0 || sizeList == null) {
            return null;
        }

        int surfaceWidth = surfaceSize.x;
        int surfaceHeight = surfaceSize.y;

        //将尺寸按比例分组
        List<List<Camera.Size>> ratioListList = new ArrayList<>();
        for (Camera.Size size : sizeList) {
            Log.d("tzw", "width:"+size.width+"height:"+size.height);
            addRatioList(ratioListList, size);
        }

        //找出比例最接近屏幕比例的尺寸组bestRatioList
        final float surfaceRatio = (float) surfaceWidth / surfaceHeight;
        List<Camera.Size> bestRatioList = null;
        float ratioDiff = Float.MAX_VALUE;
        for (List<Camera.Size> ratioList : ratioListList) {
            float ratio = (float) ratioList.get(0).width / ratioList.get(0).height;
            float newRatioDiff = Math.abs(ratio - surfaceRatio);
            if (newRatioDiff < ratioDiff) {
                bestRatioList = ratioList;
                ratioDiff = newRatioDiff;
            }
        }
        //在比例最接近的尺寸组中找出最接近屏幕尺寸且大于屏幕尺寸的尺寸bestSize
        Camera.Size bestSize = null;
        int diff = Integer.MAX_VALUE;
        assert bestRatioList != null;
        for (Camera.Size size : bestRatioList) {
            int newDiff = Math.abs(size.width - surfaceWidth) + Math.abs(size.height - surfaceHeight);
            if (size.height >= surfaceHeight && newDiff < diff) {
                bestSize = size;
                diff = newDiff;
            }
        }

        if (bestSize != null) {
            return bestSize;
        }

        //如果没有找到，则忽略2中第二个条件再找一遍，应该是最合适的尺寸了
        diff = Integer.MAX_VALUE;
        for (Camera.Size size : bestRatioList) {
            int newDiff = Math.abs(size.width - surfaceWidth) + Math.abs(size.height - surfaceHeight);
            if (newDiff < diff) {
                bestSize = size;
                diff = newDiff;
            }
        }

        return bestSize;
    }

    /**
     * 将尺寸按比例分组，相同比例的在一组
     * @param ratioListList 存放分组后的尺寸列表，每项为同比例的一组尺寸列表ratioList
     * @param size
     */
    private static void addRatioList(List<List<Camera.Size>> ratioListList, Camera.Size size) {
        float ratio = (float) size.width / size.height;
        for (List<Camera.Size> ratioList : ratioListList) {
            float mine = (float) ratioList.get(0).width / ratioList.get(0).height;
            if (ratio == mine) {
                ratioList.add(size);
                return;
            }
        }

        List<Camera.Size> ratioList = new ArrayList<>();
        ratioList.add(size);
        ratioListList.add(ratioList);
    }
    /**
     * 排序
     * @param list
     */
    private static void sortList(List<Camera.Size> list) {
        Collections.sort(list, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size pre, Camera.Size after) {
                if (pre.width > after.width) {
                    return 1;
                } else if (pre.width < after.width) {
                    return -1;
                }
                return 0;
            }
        });
    }

    /*-----------------------------------------人脸检测------------------------------------------*/
    public void startFaceDetect(){
        if(mCamera!=null){
           mCamera.startFaceDetection();
           mCamera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
               @Override
               public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                   if(faces!=null && faces.length>0){
                       mFaceView.setFaces(transform(faces));
                   }else{
                       mFaceView.clearFaces();
                   }

               }
           });
        }

    }


    /**
     * 将相机中用于表示人脸矩形的坐标转换成UI页面的坐标
     *
     * 人脸的边界。它所使用的坐标系中，左上角的坐标是（-1000，-1000），右下角的坐标是（1000,1000）
     * 例如：假设屏幕的尺寸是800 * 480，有一个矩形在相机的坐标系中的位置是(-1000,-1000,0,0)，它相对应的在安卓屏幕坐标系中的位置就是(0,0,400,240)
     *
     * 作者：Smashing丶
     * 链接：https://www.jianshu.com/p/3bb301c302e8
     * 来源：简书
     * 著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
     */
    private ArrayList<RectF> transform(Camera.Face[] faces) {
        Matrix matrix = new Matrix();
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            matrix.setScale(-1, 1); //Need mirror for front camera.
        } else {
            matrix.setScale(1, 1);
        }
        int width = mSurfaceView.getWidth();
        int height = mSurfaceView.getHeight();
        matrix.postRotate(mDisplayOrientation);//This is the value for android.hardware.Camera.setDisplayOrientation.
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(width / 2000F, height / 2000F);
        matrix.postTranslate(width / 2F, height / 2F);
        ArrayList<RectF> rectFS = new ArrayList<>();
        for (Camera.Face face : faces) {
            RectF srcRectF = new RectF(face.rect);
            RectF descRectF = new RectF(0, 0, 0, 0);
            matrix.mapRect(descRectF, srcRectF);
            rectFS.add(descRectF);
        }
        return rectFS;
    }

}
