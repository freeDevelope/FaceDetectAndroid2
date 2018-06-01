package university.shiyou.com.facedetectandroid;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import java.io.File;

import static org.opencv.imgproc.Imgproc.rectangle;

public class PictureActivity extends AppCompatActivity {

    private Bitmap mBitmap;
    private String mPicturePath;
    private ImageView mImageView;
    private Button mBtnFaceDetect;
    String mModelPath;
    private Rect[] mFaceRects;
    Mat im = new Mat();
    private FaceDetectCV mDetector;
    private final int DETECT_START = 1;
    private final int DETECT_SUCCESS = 2;
    private ProgressDialog mDialog;

    //@SuppressLint("HandlerLeak")
//    @SuppressLint("HandlerLeak")
//    private Handler mHandler = new Handler(){
//        @Override
//        public void handleMessage(Message msg) {
//            //super.handleMessage(msg);
//            if (msg.what == DETECT_START){
//                mDialog = new ProgressDialog(getApplicationContext());
//                mDialog.setTitle("正在进行人脸检测...");
//                mDialog.show();
//
//            }else if (msg.what == DETECT_SUCCESS){
//                mDialog.cancel();
//                if (mFaceRects ==null){
//                    Toast.makeText(PictureActivity.this, "未检测到人脸！", Toast.LENGTH_SHORT).show();
//                }else{
//                    Toast.makeText(PictureActivity.this, "检测到人脸数目: "+mFaceRects.length, Toast.LENGTH_SHORT).show();
//                    //绘制矩形框
//                    Canvas canvas = new Canvas(mBitmap);
//                    Paint paint = new Paint();
//                    paint.setColor(Color.BLUE);
//                    paint.setStyle(Paint.Style.FILL);
//                    for (Rect i:mFaceRects){
//                        android.graphics.Rect r = new android.graphics.Rect(i.x,i.y,i.x+i.width,i.y+i.height);
//                        canvas.drawRect(r,paint);
//                    }
//
//                    //显示
//                    mImageView.setImageBitmap(mBitmap);
//                }
//            }
//
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);
        mImageView = findViewById(R.id.imageview_show_picture);
        Intent intent = getIntent();
        if (intent.hasExtra("picture_path")) {
            mPicturePath = intent.getStringExtra("picture_path");
            mBitmap = BitmapFactory.decodeFile(mPicturePath);
            if (mBitmap != null) {
                mImageView.setImageBitmap(mBitmap);
            }
        }

        mModelPath = WelcomeActivity.xmlFolder + File.separator + "haarcascade_frontalface_default.xml";
        mDetector = new FaceDetectCV(); //实例化人脸检测器


        mBtnFaceDetect = findViewById(R.id.btn_face_detect);
        mBtnFaceDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBitmap != null) {
                    //开启一个异步任务，本质就是一个个线程
                    new FaceDetectTask().execute();

                }
            }
        });


    }

//    class FaceDetectThread extends Thread{
//
//        @Override
//        public void run() {
//            super.run();
//            Message msg = Message.obtain();
//            msg.what = DETECT_START;
//            //mHandler.sendMessage(msg);
//            Utils.bitmapToMat(mBitmap,im);
//            //人脸检测
//            mFaceRects = mDetector.faceDetectCascade(im,mModelPath);
//            msg.what = DETECT_SUCCESS;
//            mHandler.sendMessage(msg);
//
//        }
//    }


    @SuppressLint("StaticFieldLeak")
    class FaceDetectTask extends AsyncTask<Integer, Integer, Integer> {

        private long time;
        @Override
        protected Integer doInBackground(Integer... integers) {

            Utils.bitmapToMat(mBitmap, im);
            //人脸检测
            mFaceRects = mDetector.faceDetectCascade(im, mModelPath);
            Integer integer;
            if (mFaceRects == null) {
                integer = 0;
            } else {
                integer = mFaceRects.length;
            }
            return integer;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(PictureActivity.this, "检测中...", Toast.LENGTH_SHORT).show();
            time = System.currentTimeMillis();

        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);

            long costTime = System.currentTimeMillis() - time;
            if (integer == 0) {
                Toast.makeText(PictureActivity.this, "未检测到人脸，耗时: "+costTime+"ms", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PictureActivity.this, "检测到人脸数目: " + mFaceRects.length+" 耗时: "+costTime+"ms", Toast.LENGTH_SHORT).show();
                //绘制并且显示矩形框
                showDetectedFaces();

            }

        }
    }

    private void showDetectedFaces() {

        if (mFaceRects == null) {
            return;
            //Toast.makeText(PictureActivity.this, "未检测到人来拿", Toast.LENGTH_SHORT).show();
        } else {
            //Toast.makeText(PictureActivity.this, "检测到人脸数目: " + mFaceRects.length, Toast.LENGTH_SHORT).show();
            Mat im_temp = im.clone();
            for (Rect i : mFaceRects) {
                rectangle(im_temp,i.tl(),new Point(i.x+i.width,i.y+i.height),new Scalar(255,255,0));
            }
            Bitmap bitmap = Bitmap.createBitmap(im_temp.width(),im_temp.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(im_temp,bitmap);
            //显示
            mImageView.setImageBitmap(bitmap);

        }


    }

}
