package university.shiyou.com.facedetectandroid;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class FaceDetectCV {

    /**
     * @param img 输入图像RGB
     * @param xmlPath Cascade.xml的路径，文件名称
     * @return 人脸检测矩形框
     */
    public native Rect[] faceDetectCascade(Mat img, String xmlPath);

    public native void getSDCardDirToNative(String SDpath);

}
