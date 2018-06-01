package university.shiyou.com.facedetectandroid;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * 基于opencv的人脸检测
 * 人脸检测算法采用opencv自带的Cascade分类器，代码所需的xml模型文件自动复制到sd卡 opencv_face_detect/model文件夹内
 * 人脸检测代码为C++,在C++中进行图像处理，检测的结果为一系列人脸矩形框，这些矩形框数据通过JNI传给Java代码
 * Require:Android studio3.0/3.1, opencv-3.2.0-android sdk
 * Author:weipenghui@Xidian
 * Date:2017-4
 */
public class WelcomeActivity extends AppCompatActivity implements View.OnClickListener{

    //加载编译的so
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }


    public static String xmlFolder = Environment.getExternalStorageDirectory().toString()+ File.separator + "face_detect_opencv" + File.separator + "model";
    private Button mBtnChoosePicture;
    private Button mBtnOpenCamera;
    private static final int REQUEST_CODE_CHOOSE_PICTURE = 1;
    private static final int REQUEST_CODE_OPEN_CAMERA = 4;
    private static String[] PERMISSION_STROAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    private static final int REQUEST_CODE_PERMISSION_STROAGE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        initView();
        mBtnOpenCamera.setOnClickListener(this);
        mBtnChoosePicture.setOnClickListener(this);

        //检查权限,如果没有权限，动态申请
        if ((checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)||
                (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)){
            //android高版本要动态申请权限
            requestPermissions(PERMISSION_STROAGE,REQUEST_CODE_PERMISSION_STROAGE);
        }

        
        //创建一个子线程用于将模型复制到sd卡，在Android中耗时任务一般在子线程中处理，
        //避免阻塞UI线程
        final File file = new File(xmlFolder);
        if (!file.exists()){
            boolean isOk = file.mkdirs();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    
                    AssetManager manager = getAssets();
                    InputStream input = null;
                    try {
                        //打开Asset文件夹里的xml模型，这里可以改为其他xml模型
                       input = manager.open("haarcascade_frontalface_default.xml");
                       //将其保存的sd卡
                        File xmlFile = new File(xmlFolder+File.separator+"haarcascade_frontalface_default.xml");
                        if (!xmlFile.exists()){
                           // xmlFile.createNewFile();

                            //写入文件
                            OutputStream out = new FileOutputStream(xmlFile);
                            int byteCount = 0;
                            byte[] bytes = new byte[1024];
                            while ((byteCount = input.read(bytes))!=-1){
                                out.write(bytes,0,byteCount);
                            }
                            input.close();
                            out.close();

                        }

                    }catch (IOException e){
                        e.printStackTrace();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WelcomeActivity.this, "haarcascade模型保存的目录"+xmlFolder, Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            }).start();
        }



    }

    private void initView(){
        mBtnChoosePicture = findViewById(R.id.btn_choose_image);
        mBtnOpenCamera = findViewById(R.id.btn_open_camera);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_choose_image:{
                //在Android中一般使用Intent实现界面的跳转
                //传入一个ACTION，一个Uri
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                //启动选择图像界面
                startActivityForResult(intent,REQUEST_CODE_CHOOSE_PICTURE);
                break;
            }
            case R.id.btn_open_camera:{
                //Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
               // intent.putExtra(MediaStore.EXTRA_OUTPUT,)
               // startActivityForResult(intent,REQUEST_CODE_OPEN_CAMERA);
                break;
            }
            default:
                break;
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CHOOSE_PICTURE && resultCode == RESULT_OK){

            //返回所选照片的Uri
            //从Uri中解析出资源路径
            Uri uri = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            //查询我们需要的数据
            Cursor cursor = null;
            int index = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                cursor = this.getContentResolver().query(uri,filePathColumn,null,null);
                index = cursor.getColumnIndex(filePathColumn[0]);
                cursor.moveToFirst();

            }else {
                cursor = managedQuery(uri,filePathColumn,null,null,null);
                if (cursor!=null){
                    index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    cursor.moveToFirst();

                }

                //图库中照片的路径
                String picturePath = null;
                try {
                    picturePath= cursor.getString(index);
                }catch (NullPointerException e){
                    e.printStackTrace();
                }


                //启动界面
                Intent intent = new Intent(WelcomeActivity.this,PictureActivity.class);
                //将照片的路径传入另一个界面
                intent.putExtra("picture_path",picturePath);
                startActivity(intent);

            }





        }else if (requestCode==REQUEST_CODE_OPEN_CAMERA&&resultCode==RESULT_OK){

        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==REQUEST_CODE_PERMISSION_STROAGE){
            if (grantResults.length==0){
                Toast.makeText(this, "您拒绝了APP权限申请！", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
