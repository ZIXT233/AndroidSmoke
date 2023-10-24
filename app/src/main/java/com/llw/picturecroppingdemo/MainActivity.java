package com.llw.picturecroppingdemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.provider.MediaStore;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import java.io.File;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Objects;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import  com.yalantis.ucrop.UCrop;
public class MainActivity extends AppCompatActivity {

    /**
     * 外部存储权限请求码
     */
    public static final int REQUEST_EXTERNAL_STORAGE_CODE = 9527;
    /**
     * 打开相册请求码
     */
    private static final int OPEN_ALBUM_CODE = 100;
    private static final int OPEN_CAMERA_CODE = 101;
    /**
     * 图片剪裁请求码
     */
    public static final int PICTURE_CROPPING_CODE = 200;

    private static final String TAG = "MainActivity";
    //图片
    private ImageView ivPicture;
    private Uri photoUri,resultUri;
    private Bitmap originBitmap,cropBitmap;

    private boolean showShareButton=false;

    /**
     * Glide请求图片选项配置
     */
    private RequestOptions requestOptions = RequestOptions
            .circleCropTransform()//圆形剪裁
            .diskCacheStrategy(DiskCacheStrategy.NONE)//不做磁盘缓存
            .skipMemoryCache(true);//不做内存缓存

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivPicture = findViewById(R.id.iv_picture);
        requestPermission();
    }

    /**
     * 权限请求
     */
    @AfterPermissionGranted(REQUEST_EXTERNAL_STORAGE_CODE)
    private void requestPermission() {
        String[] param = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                          Manifest.permission.CAMERA};
        if (EasyPermissions.hasPermissions(this, param)) {
            //已有权限
            showMsg("已获得权限");
        } else {
            //无权限 则进行权限请求
            EasyPermissions.requestPermissions(this, "请求权限", REQUEST_EXTERNAL_STORAGE_CODE, param);
        }
    }

    /**
     * Toast提示
     *
     * @param msg 内容
     */
    private void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    private Uri getDestinationUri() {
        String fileName = String.format("fr_crop_%s.jpg", System.currentTimeMillis());
        File cropFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
        return Uri.fromFile(cropFile);
    }
    private Bitmap getBitmap(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 打开相册
     */
    public void openAlbum(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");

        startActivityForResult(intent, OPEN_ALBUM_CODE);
    }
    public void openCamera(View view){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoUri=getDestinationUri();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //适配Android 7.0文件权限，通过FileProvider创建一个content类型的Uri
            photoUri=FileProvider.getUriForFile(this, "com.llw.picturecroppingdemo.fileprovider",
                                                new File(photoUri.getPath()));
        }
        //android11以后强制分区存储，外部资源无法访问，所以添加一个输出保存位置，然后取值操作
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(intent, OPEN_CAMERA_CODE);
    }
    //

    /**
     * 图片剪裁
     *
     * @param uri 图片uri
     */
    private void pictureCropping(Uri uri) {
        UCrop.of(uri, getDestinationUri())
                // 长宽比
                .withAspectRatio(1, 1)
                // 图片大小
                .withMaxResultSize(200, 200)
                .start(this);
    }
    private Uri saveResultImage(Bitmap result){
        String fileName = String.format("result_%s.jpg", System.currentTimeMillis());
        File resultFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
        try{
            OutputStream out=new FileOutputStream(resultFile);
            result.compress(Bitmap.CompressFormat.JPEG,100,out);
            out.close();
            return Uri.fromFile(resultFile);
        }catch (Exception e){
        }
        return null;
    }
    /**
     * 返回Activity结果
     *
     * @param requestCode 请求码
     * @param resultCode  结果码
     * @param data        数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_ALBUM_CODE && resultCode == RESULT_OK) {
            //打开相册返回
            photoUri = Objects.requireNonNull(data).getData();
            //图片剪裁
            pictureCropping(photoUri);
        }if (requestCode == OPEN_CAMERA_CODE && resultCode == RESULT_OK) {
            //打开相册返回
            pictureCropping(photoUri);
        }
        else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            //图片剪裁返回
            final Uri cropUri = UCrop.getOutput(data);
            originBitmap=getBitmap(photoUri);
            cropBitmap=getBitmap(cropUri);
            ivPicture.setImageURI(saveResultImage(cropBitmap));//这一行是临时的，用来显示截取好的图片，最终应显示合成好的
            //从截取的bitmap获取黑度：蒋世杰
            //int blackness=getBlackness(cropBitmap);
            //拼接原图，截图和黑度信息，生成最终结果图片：李纪群(合成图片)，王鼎然(将黑度信息转图片)
            //Bitmap resultBitmap=composeImage(originBitmap,cropBitmap,blackness);
            //ivPicture.setImageBitmap(resultBitmap);
            //提供分享按钮(右上角)和对应的shareResult回调函数：聂昊
            //resultUri=saveResultImage(resultBitmap); //保存已经写好
            //showShareButton=true; //按钮在这个为true时才显示
            //回调函数:  shareResult(resultBitmap);
        }
    }

    /**
     * 权限请求结果
     *
     * @param requestCode  请求码
     * @param permissions  请求权限
     * @param grantResults 授权结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 将结果转发给 EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


}