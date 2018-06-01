#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include "jni_bridge.h"
#include <android/storage_manager.h>

using namespace std;
using namespace cv;

#ifdef __cplusplus
extern "C"
{
#endif


void getSd(){


}

/*
 * 基于opencv的人脸检测
 * @param img_src [in] 输入图像，RGBA
 * @param face_rects [out] 人脸矩形框
 * @param xmlPath [in] 级联分类器的xml文件的全路径（路径+文件名称）
 * */
int  face_detect_opencv(Mat& img_src,vector<Rect>& face_rects,String& xmlPath){

    CascadeClassifier cascadeClassifier;
    cascadeClassifier.load(String(xmlPath));
    if (cascadeClassifier.empty()){
        return -1;
    }
    Mat img_gray;
    cvtColor(img_src,img_gray,COLOR_RGBA2GRAY);
    cascadeClassifier.detectMultiScale(img_gray,face_rects);
    return static_cast<int>(face_rects.size());

}

#if 0
JNIEXPORT jobjectArray JNICALL
Java_university_shiyou_com_facedetectandroid_FaceDetectCV_faceDetectCascade(JNIEnv *env,
                                                                            jobject img,
                                                                            jstring xmlPath_) {
    const char *xmlPath = env->GetStringUTFChars(xmlPath_, 0);

    // TODO
    //使用opencv进行人脸检测
    vector<Rect> face_rects;
    String xml = String(xmlPath);
    Mat image = getNativeMat(env,img)->clone();
    int num = face_detect_opencv(image,face_rects,xml);
    if (num<=0){
        return nullptr;
    }

    //将C++中vector<Rect>对象转换为Java中Rect[]类型
    jclass class_Rect = env->FindClass("org/opencv/core/Rect");
    jfieldID field_x = env->GetFieldID(class_Rect,"x","I");
    jfieldID field_y = env->GetFieldID(class_Rect,"y","I");
    jfieldID field_width = env->GetFieldID(class_Rect,"width","I");
    jfieldID field_height = env->GetFieldID(class_Rect,"height","I");
    //确保该类，以及该类的成员在Java中存在
    assert(class_Rect!= nullptr);
    assert((field_x!= nullptr)&&(field_y!= nullptr)&&(field_width!= nullptr)&&(field_height!= nullptr));

    jobjectArray rect_array = env->NewObjectArray(num,class_Rect, nullptr);
    jmethodID method_Rect = env->GetMethodID(class_Rect, "<init>", "(IIII)V");
    for (int i = 0; i < num; ++i) {
        jobject rect_object = env->NewObject(class_Rect,method_Rect,face_rects.at(i).x,face_rects.at(i).y,face_rects.at(i).width,face_rects.at(i).height);
        env->SetObjectArrayElement(rect_array,i,rect_object);
    }

    env->ReleaseStringUTFChars(xmlPath_, xmlPath);

    return rect_array;


}
#endif


JNIEXPORT jobjectArray JNICALL
Java_university_shiyou_com_facedetectandroid_FaceDetectCV_faceDetectCascade(JNIEnv *env,
                                                                            jobject instance,
                                                                            jobject img,
                                                                            jstring xmlPath_) {
    const char *xmlPath = env->GetStringUTFChars(xmlPath_, 0);

    // TODO
    //使用opencv进行人脸检测
    vector<Rect> face_rects;
    String xml = String(xmlPath);
    Mat image = getNativeMat(env,img)->clone();

    //使用imwrite()将图像写入sdcard
    string path = "/sdcard/test_imwrite.jpg";
    decltype(image) image2;
    cv::cvtColor(image,image2,cv::COLOR_RGBA2BGR);
    cv::imwrite(path,image);

    int num = face_detect_opencv(image,face_rects,xml);
    if (num<=0){
        return nullptr;
    }

    //将C++中vector<Rect>对象转换为Java中Rect[]类型
    jclass class_Rect = env->FindClass("org/opencv/core/Rect");
    jfieldID field_x = env->GetFieldID(class_Rect,"x","I");
    jfieldID field_y = env->GetFieldID(class_Rect,"y","I");
    jfieldID field_width = env->GetFieldID(class_Rect,"width","I");
    jfieldID field_height = env->GetFieldID(class_Rect,"height","I");
    //确保该类，以及该类的成员在Java中存在
    assert(class_Rect!= nullptr);
    assert((field_x!= nullptr)&&(field_y!= nullptr)&&(field_width!= nullptr)&&(field_height!= nullptr));

    jobjectArray rect_array = env->NewObjectArray(num,class_Rect, nullptr);
    jmethodID method_Rect = env->GetMethodID(class_Rect, "<init>", "(IIII)V");
    for (int i = 0; i < num; ++i) {
        jobject rect_object = env->NewObject(class_Rect,method_Rect,face_rects.at(i).x,face_rects.at(i).y,face_rects.at(i).width,face_rects.at(i).height);
        env->SetObjectArrayElement(rect_array,i,rect_object);
    }

    env->ReleaseStringUTFChars(xmlPath_, xmlPath);

    return rect_array;
}



extern "C"
JNIEXPORT void JNICALL
Java_university_shiyou_com_facedetectandroid_FaceDetectCV_getSDCardDirToNative(JNIEnv *env,
                                                                               jobject instance,
                                                                               jstring SDpath_) {
    const char *SDpath = env->GetStringUTFChars(SDpath_, 0);

    // TODO

    env->ReleaseStringUTFChars(SDpath_, SDpath);
}


#ifdef __cplusplus
}
#endif

extern "C"
JNIEXPORT jobject JNICALL
Java_university_shiyou_com_facedetectandroid_RemapActivity_remapImage(JNIEnv *env, jobject instance,
                                                                      jobject srcImage,
                                                                      jstring mapXML_) {
    const char *mapXML = env->GetStringUTFChars(mapXML_, 0);

    // TODO

    Mat src = getNativeMat(env,srcImage)->clone();
    Mat map_x,map_y;
    map_x.create(src.size(), CV_32FC1);
    map_y.create(src.size(), CV_32FC1);
    Mat dst;
    dst.create(src.size(), src.type());

    FileStorage fs(mapXML, FileStorage::READ);//创建一个读入器
    fs["mapx"] >> map_x;
    fs["mapy"] >> map_y;
    fs.release();//be tidy

    remap(src,dst, map_x, map_y, INTER_LINEAR, BORDER_CONSTANT, Scalar(0, 0, 0));//做重映射和插值

    env->ReleaseStringUTFChars(mapXML_, mapXML);
    return getJavaMat(env,dst);
}extern "C"
JNIEXPORT void JNICALL
Java_university_shiyou_com_facedetectandroid_RemapActivity_HelloWorld(JNIEnv *env,
                                                                      jobject instance) {

    // TODO

}