package university.shiyou.com.facedetectandroid;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * 鱼眼图像校正
 * map.xml自动存到sdcard/Remap文件夹，该文件夹包含两个测试图像，请复制到SD卡根目录，这样可以被图库检测到
 */
public class RemapActivity extends AppCompatActivity implements View.OnClickListener{


    //加载编译的so
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }


    private static final String mFolderPath = Environment.getExternalStorageDirectory().toString()+ File.separator+"Remap";
    private static final String[] TEST_IMAGES={
            "0.bmp",
            "1.jpg",
            "map.xml"
    };
    private static final String modelName = "map.xml";
    private static final String mapXMLFilePath = mFolderPath+File.separator+modelName;

    private Button mBtnChooseImage;
    private Button mBtnRemapImage;
    private ImageView mImageView;
    private Bitmap mBitmap;
    private Mat mImageSrc;
    private Mat mImageDst;

    private static final int REQUEST_CODE_CHOOSE_PICTURE = 1;
    private String mTestImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remap);

        initView();

        mBtnRemapImage.setOnClickListener(this);
        mBtnChooseImage.setOnClickListener(this);


        //开启一个线程新建文件夹，保存测试图片,map.xml
        new Thread(){
            @Override
            public void run() {
                File file = new File(mFolderPath);
                if (!file.exists()){
                    file.mkdir();
                    AssetManager manager =getAssets();
                    InputStream inputStream = null;
                    OutputStream outputStream = null;
                    for (String fileName:TEST_IMAGES){
                        try {
                            inputStream = manager.open(fileName);
                            if (inputStream!=null){
                                byte[] bytes = new byte[1024];
                                int byteCount = 0;
                                String outFilePath = mFolderPath+File.separator+fileName;
                                File imageFile = new File(outFilePath);
                                imageFile.createNewFile();
                                outputStream = new FileOutputStream(imageFile);
                                while ((byteCount=inputStream.read(bytes))!=-1){
                                    outputStream.write(bytes,0,byteCount);
                                }
                            }
                        }catch (IOException e){
                            e.printStackTrace();
                        }

                    }
                    try {
                        inputStream.close();
                        outputStream.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }


                }
            }
        }.start();


    }


    private void initView(){
        mBtnChooseImage = findViewById(R.id.btn_choose_test_image);
        mImageView = findViewById(R.id.image_view_test_image);
        mBtnRemapImage = findViewById(R.id.btn_remap_image);
    }




    public native void HelloWorld();

    public static native Mat remapImage(Mat srcImage,String mapXML);

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_choose_test_image:{
                //在Android中一般使用Intent实现界面的跳转
                //传入一个ACTION，一个Uri
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                //启动选择图像界面
                startActivityForResult(intent,REQUEST_CODE_CHOOSE_PICTURE);
                break;
            }
            case R.id.btn_remap_image:{
                if (mBitmap!=null){
                    mImageSrc = new Mat();
                    Utils.bitmapToMat(mBitmap,mImageSrc);
                    //开一个后台线程，图像处理
                    new RemapImageAsyncTask().execute();
                }else{
                    Toast.makeText(this, "图像为空，请选择需要校正的图像！", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            default:
                break;
        }
    }


    class RemapImageAsyncTask extends AsyncTask<Integer,Integer,Integer>{

        private ProgressDialog progressDialog;
        private long t1;
        private long t2;

        @Override
        protected Integer doInBackground(Integer... integers) {
            t1 = System.currentTimeMillis();
            mImageDst = remapImage(mImageSrc,mapXMLFilePath);
            t2 = System.currentTimeMillis();
            if (mImageDst!=null){
                if (!mImageDst.empty()){
                    Utils.matToBitmap(mImageDst,mBitmap);
                }

            }
            return 0;
        }

        @Override
        protected void onPreExecute() {
           // Toast.makeText(RemapActivity.this, "开始图像校正...", Toast.LENGTH_SHORT).show();
            progressDialog = new ProgressDialog(RemapActivity.this);
            progressDialog.setTitle("图像校正中...");
            progressDialog.show();

        }

        @Override
        protected void onPostExecute(Integer integer) {
            progressDialog.dismiss();
            mImageView.setImageBitmap(mBitmap);
            Toast.makeText(RemapActivity.this, "处理时间: "+(t2 - t1)+"ms", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       if (requestCode==REQUEST_CODE_CHOOSE_PICTURE&&resultCode==RESULT_OK){
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
                   mTestImagePath = picturePath;

                   //解析为Bitmap
                   mBitmap = BitmapFactory.decodeFile(mTestImagePath);
                   if (mBitmap==null){
                       Toast.makeText(this, "图像选择为空！", Toast.LENGTH_SHORT).show();
                   }else{
                       mImageView.setImageBitmap(mBitmap);
                   }

               }catch (NullPointerException e){
                   e.printStackTrace();
               }




           }
       }

    }
}
