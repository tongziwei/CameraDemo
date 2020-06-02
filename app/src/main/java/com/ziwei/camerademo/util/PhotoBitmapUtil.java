package com.ziwei.camerademo.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by clara.tong on 2019/11/4
 */
public class PhotoBitmapUtil {
    /**
     * 把原图按1/10的比例压缩
     *
     * @param path 原图的路径
     * @return 压缩后的图片
     */
    public static Bitmap getCompressPhoto(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
 /*       options.inJustDecodeBounds = false;
        options.inSampleSize = 10;  // 图片的大小设置为原来的十分之一*/
        Bitmap bmp = BitmapFactory.decodeFile(path, options);
        options = null;
        return bmp;
    }

    /**
     * 读取照片旋转角度
     *
     * @param path 照片路径
     * @return 角度
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return degree;
    }

    /**
     * 旋转图片
     * @param angle 被旋转角度
     * @param bitmap 图片对象
     * @return 旋转后的图片
     */
    public static Bitmap rotaingImageView(int angle, Bitmap bitmap) {
        Bitmap returnBm = null;
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bitmap;
        }
        if (bitmap != returnBm) {
            bitmap.recycle();
        }
        return returnBm;
    }

    public static File saveBitmap(String path, Bitmap bm) {
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f;
    }

    /**
     * 处理旋转后的图片
     * @param originpath 原图路径
     * @return 返回修复完毕后的图片路径
     */
    public static File amendRotatePhoto( String originpath,int cameraId) {

        // 取得图片旋转角度
        int angle =  getCameraOritation(cameraId);
        Log.d("tzw", "amendRotatePhoto: "+angle);
        // 把原图压缩后得到Bitmap对象
        Bitmap bmp = getCompressPhoto(originpath);;

        // 修复图片被旋转的角度
        Bitmap bitmap = rotaingImageView(angle, bmp);

        if(cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ){
           Bitmap bitmap1 = convertBmp(bitmap);
           return saveBitmap(originpath,bitmap1);
        }else{
            // 保存修复后的图片并返回保存后的图片路径
            return saveBitmap(originpath,bitmap);
        }
    }

    public static int getCameraOritation(int cameraId){
        int result;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        result = info.orientation ;
     /*   if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation ) % 360;
            result = 360- result;
        } else {
            result = info.orientation ;
        }*/
        return result;
    }

    /**
     * 图片镜像翻转
     * @param bmp
     * @return
     */
    public static Bitmap convertBmp(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();

        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1); // 镜像水平翻转
        Bitmap convertBmp = Bitmap.createBitmap(bmp, 0, 0, w, h, matrix, true);

        return convertBmp;
    }

    public static Bitmap rotateBmp(Bitmap bmp,int cameraId){

        // 取得图片旋转角度
        int angle =  getCameraOritation(cameraId);//前置270，后置90
        Log.d("tzw", "rotateBmp: "+angle);
        Bitmap bitmap = rotaingImageView(angle, bmp);
        if(cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ){  //前置需要镜像翻转
             return convertBmp(bitmap);
        }
        return bitmap;
    }

}
