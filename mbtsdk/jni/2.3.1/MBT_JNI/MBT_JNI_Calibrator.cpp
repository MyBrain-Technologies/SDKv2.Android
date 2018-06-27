#include <android/log.h>

#include <jni.h>
#include <math.h>
#include "../Melomind.Algorithms/SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h"

#include "../Melomind.Algorithms/SignalProcessing.Cpp/PreProcessing/Headers/MBT_PreProcessing.h"
#include "../Melomind.Algorithms/SignalProcessing.Cpp/PreProcessing/Headers/MBT_BandPass_fftw3.h"
#include "../Melomind.Algorithms/NF_Melomind/Headers/MBT_ComputeSNR.h"
#include "../Melomind.Algorithms/NF_Melomind/Headers/MBT_ComputeCalibration.h"
#include "../Melomind.Algorithms/NF_Melomind/Headers/MBT_ComputeRelaxIndex.h"
#include "../Melomind.Algorithms/NF_Melomind/Headers/MBT_SmoothRelaxIndex.h"
#include "../Melomind.Algorithms/NF_Melomind/Headers/MBT_NormalizeRelaxIndex.h"
#include "../Melomind.Algorithms/NF_Melomind/Headers/MBT_RelaxIndexToVolum.h"


#include "MBT_JNI_Calibrator.h"

#define APPNAME "Calib"

#define IAFinf    6
#define IAFsup    13


/*
 * Class:     mybraintech_com_mbtsdk_core_signalprocessing_MBTCalibrator
 * Method:    nativeCalibrateNew
 */
JNIEXPORT jobject JNICALL Java_core_eeg_signalprocessing_MBTCalibrator_nativeCalibrateNew
        (JNIEnv *env, jclass caller, jint sampRate, jint packetLength, jint calibLength, jint smoothingDuration, jobjectArray qt_matrix, jobjectArray sig_matrix) {

    jboolean isCopy;
    unsigned int height;

    //Create quality matrix from qt_matrix
    height = (unsigned int) env->GetArrayLength(qt_matrix);
    MBT_Matrix<float> calibrationRecordingsQuality(height, (unsigned int) calibLength);

    for (unsigned int channelIndex = 0; channelIndex < height; channelIndex++)
    {
        jfloatArray current = (jfloatArray) env->GetObjectArrayElement(qt_matrix, channelIndex);
        jfloat* channel = env->GetFloatArrayElements(current, &isCopy);
        if (channel == NULL)
            return NULL; // out of memory
        for (unsigned int dataPoint = 0; dataPoint < calibLength; dataPoint++)
        {
            calibrationRecordingsQuality(channelIndex, dataPoint) = channel[dataPoint];
        }
        env->ReleaseFloatArrayElements(current, channel, JNI_ABORT);
        env->DeleteLocalRef(current);
    }

    //Create data matrix from sig_matrix
    height = (unsigned int) env->GetArrayLength(sig_matrix);
    MBT_Matrix<float> calibrationRecordings(height, (unsigned int) (packetLength*calibLength));

    for (unsigned int channelIndex = 0; channelIndex < height; channelIndex++)
    {
        jfloatArray current = (jfloatArray) env->GetObjectArrayElement(sig_matrix, channelIndex);
        jfloat* channel = env->GetFloatArrayElements(current, &isCopy);
        if (channel == NULL)
            return NULL; // out of memory
        for (unsigned int dataPoint = 0; dataPoint < (packetLength*calibLength); dataPoint++)
        {
            calibrationRecordings(channelIndex, dataPoint) = channel[dataPoint];
        }
        env->ReleaseFloatArrayElements(current, channel, JNI_ABORT);
        //env->DeleteLocalRef(current);
    }

    std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(calibrationRecordings,calibrationRecordingsQuality, sampRate, packetLength, IAFinf, IAFsup, smoothingDuration);

    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID initHasMap = env->GetMethodID(hashMapClass, "<init>", "(I)V");
    jmethodID putMethod = env->GetMethodID(hashMapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jobject jCalibParams = env->NewObject(hashMapClass, initHasMap, 0);//calibParams.size());

    // Defining Double for JNI
    //jclass floatArrayClass = env->FindClass("[F");

    // filling Java HasMap
    for (auto calibParam : paramCalib)
    {
        std::string key = calibParam.first;
        std::vector<float> jvalueVector = calibParam.second;

        //Create jFloatArray from vector float
        jfloatArray jValueFloatArray = env->NewFloatArray((jsize) jvalueVector.size());
        if (jValueFloatArray == NULL)
            return NULL; // ouf of memory

        jfloat temp[(jsize) jvalueVector.size()];
        for (unsigned int it = 0; it < jvalueVector.size(); it++)
            temp[it] = jvalueVector.at(it);
        env->SetFloatArrayRegion(jValueFloatArray, 0, (jsize) jvalueVector.size(), temp);

        env->CallObjectMethod(jCalibParams, putMethod, env->NewStringUTF(key.c_str()), jValueFloatArray);
        //env->ReleaseFloatArrayElements(jValueFloatArray, temp, JNI_ABORT);
        env->DeleteLocalRef(jValueFloatArray);
    }

    env->DeleteLocalRef(hashMapClass);
    return jCalibParams;

}
