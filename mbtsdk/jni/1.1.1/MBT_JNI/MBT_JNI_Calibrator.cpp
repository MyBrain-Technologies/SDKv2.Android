#include <android/log.h>

#include <jni.h>
#include <math.h>
#include "../Melomind.Algorithms/SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h"

#include <Melomind.Algorithms/SignalProcessing.Cpp/PreProcessing/Headers/MBT_PreProcessing.h>
#include <Melomind.Algorithms/SignalProcessing.Cpp/PreProcessing/Headers/MBT_BandPass_fftw3.h>
#include "Melomind.Algorithms/NF_Melomind/Headers/MBT_ComputeSNR.h"
#include "Melomind.Algorithms/NF_Melomind/Headers/MBT_ComputeCalibration.h"
#include "Melomind.Algorithms/NF_Melomind/Headers/MBT_ComputeRelaxIndex.h"
#include "Melomind.Algorithms/NF_Melomind/Headers/MBT_SmoothRelaxIndex.h"
#include "Melomind.Algorithms/NF_Melomind/Headers/MBT_NormalizeRelaxIndex.h"
#include "Melomind.Algorithms/NF_Melomind/Headers/MBT_RelaxIndexToVolum.h"
#include "Melomind.Algorithms/SignalProcessing.Cpp/PreProcessing/Headers/MBT_PreProcessing.h"
#include "Melomind.Algorithms/SignalProcessing.Cpp/PreProcessing/Headers/MBT_BandPass_fftw3.h"

#include "MBT_JNI_Calibrator.h"

#define APPNAME "Calib"

/*
 * Class:     modules_signalprocessing_MBTCalibrator
 * Method:    nativeCalibrateNew
 */
JNIEXPORT jobject JNICALL Java_mybraintech_com_mbtsdk_core_signalprocessing_MBTCalibrator_nativeCalibrateNew
        (JNIEnv *env, jclass caller, jint samprate, jint packetLength, jint calibLength, jobjectArray qt_matrix, jobjectArray sig_matrix) {
    float IAFinf = 7;
    float IAFsup = 13;
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


    // Set the thresholds for the outliers
    // -----------------------------------
    MBT_Matrix<float> Bounds((unsigned int) calibrationRecordings.size().first, 2);
    MBT_Matrix<float> Test((unsigned int) calibrationRecordings.size().first,
                           (unsigned int) calibrationRecordings.size().second);

    for (unsigned int ch=0; ch<calibrationRecordings.size().first; ch++)
    {
        std::vector<float> signal_ch = calibrationRecordings.row(ch);

        if (all_of(signal_ch.begin(), signal_ch.end(), [](double testNaN){return isnan(testNaN);}) )
        {
            errno = EINVAL;
            perror("ERROR: BAD CALIBRATION - WE HAVE ONLY NAN VALUES");
        }

        // skip the NaN values in order to calculate the Bounds
        vector<float>tmp_signal_ch = SkipNaN(signal_ch);

        std::vector<float> tmp_Bounds = CalculateBounds(tmp_signal_ch); // Set the thresholds for outliers


        Bounds(ch, 0) = tmp_Bounds[0];
        Bounds(ch, 1) = tmp_Bounds[1];
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "%.8f  Bounds ch 0",Bounds(ch,0));
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "%.8f  Bounds ch 1",Bounds(ch,1));


        // basically, we convert from vector<float> to vector<double>
        vector<double> CopySignal_ch(signal_ch.begin(), signal_ch.end());
        vector<double> Copytmp_Bounds(tmp_Bounds.begin(), tmp_Bounds.end());

        // set outliers to nan
        vector<double> InterCopySignal_ch = MBT_OutliersToNan(CopySignal_ch, Copytmp_Bounds);
        for (unsigned int t = 0 ; t < InterCopySignal_ch.size(); t++)
            Test(ch,t) = (float) InterCopySignal_ch[t];
    }

//    // Set the thresholds for the outliers  >>>>>> TEST ONLY <<<<<<
//    // -----------------------------------
//    MBT_Matrix<float> Bounds((unsigned int) calibDataMatrix.size().first, 2);
//    for (int ch=0; ch<calibDataMatrix.size().first; ch++)
//    {
//        std::vector<float> signal_ch = calibDataMatrix.row(ch);
//        signal_ch.erase(std::remove_if(signal_ch.begin(), signal_ch.end(),[](float testNaN){return std::isnan(testNaN);}),signal_ch.end()); // remove NaN values
//        std::vector<float> calibrationRecordings_withoutDC = RemoveDC(signal_ch); // Remove DC
//        // Notch
//        std::vector<float> freqBounds;
//        freqBounds.assign(2,0);
//        freqBounds[0] = (float)2.0;
//        freqBounds[1] = (float) 30.0;
//
//        std::vector<float> tmp_outputBandpass = BandPassFilter(calibrationRecordings_withoutDC, freqBounds); // Bandpass
//        std::vector<float> tmp_Bounds = CalculateBounds(tmp_outputBandpass); // Set the thresholds for outliers
//        Bounds((unsigned int) ch, 0) = tmp_Bounds[0];
//        Bounds((unsigned int) ch, 1) = tmp_Bounds[1];
//    }
    // -----------------------------------



    // interpolate the nan values between the channels
    MBT_Matrix<float> FullyInterpolatedTest = MBT_InterpolateBetweenChannels(Test);

    // interpolate the nan values across each channel
    MBT_Matrix<float> InterpolatedAcrossChannels = MBT_InterpolateAcrossChannels(FullyInterpolatedTest);

    // remove the nan if there still exist in the beginning or the end of the MBT_Matrix
    //MBT_Matrix<float> DataWithoutNaN = RemoveNaNIfAny(InterpolatedAcrossChannels);

    std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(InterpolatedAcrossChannels, calibrationRecordingsQuality, samprate, packetLength, IAFinf, IAFsup, Bounds);
    //std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(calibDataMatrix,qualityDataMatrix, samprate, packetLength, IAFinf, IAFsup, Bounds);

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
