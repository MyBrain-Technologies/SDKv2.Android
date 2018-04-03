//
// Created by Vincent on 26/11/2015.
//

#include <map>
#include <android/log.h>
#include "../Melomind.Algorithms/SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h"

#include "../Melomind.Algorithms/NF_Melomind/Headers/MBT_ComputeSNR.h"
#include "../Melomind.Algorithms/NF_Melomind/Headers/MBT_ComputeCalibration.h"
#include "../Melomind.Algorithms/NF_Melomind/Headers/MBT_ComputeRelaxIndex.h"
#include "../Melomind.Algorithms/NF_Melomind/Headers/MBT_SmoothRelaxIndex.h"
#include "../Melomind.Algorithms/NF_Melomind/Headers/MBT_NormalizeRelaxIndex.h"
#include "../Melomind.Algorithms/NF_Melomind/Headers/MBT_RelaxIndexToVolum.h"
#include "../Melomind.Algorithms/SignalProcessing.Cpp/PreProcessing/Headers/MBT_PreProcessing.h"
#include "../Melomind.Algorithms/SignalProcessing.Cpp/PreProcessing/Headers/MBT_BandPass_fftw3.h"
#include "MBT_JNI_ComputeRelaxIndex.h"
#include "../Melomind.Algorithms/SignalProcessing.Cpp/Transformations/Headers/MBT_PWelchComputer.h"

#define APPNAME "testSNRvalues"
/*
 * Class:     modules_signalprocessing_MBTComputeRelaxIndex
 * Method:    nativeComputeRelaxIndex
 * Signature: (ILjava/util/HashMap;[[D[[D)D
 */


float IAFinf = 7;
float IAFsup = 13;
static std::vector<float> pastRelaxIndex;
/**
 * Function nativeComputeRelaxIndexNew
 */
JNIEXPORT jfloat JNICALL
Java_mybraintech_com_mbtsdk_core_signalprocessing_MBTComputeRelaxIndex_nativeComputeRelaxIndexNew(JNIEnv *env,
                                                                              jclass type,
                                                                              jint samprate,
                                                                              jobject parameters,
                                                                              jobjectArray qualities,
                                                                              jobjectArray matrix) {
    //Transform java hashmap to c++ map
    // Defining the MBTCalibrationParameters
    jclass calibParamClass = env->FindClass("modules/signalprocessing/MBTCalibrationParameters");
    jmethodID getSize = env->GetMethodID(calibParamClass, "getSize", "()I");
    jmethodID getKey = env->GetMethodID(calibParamClass, "getKey", "(I)Ljava/lang/String;");
    jmethodID getValue = env->GetMethodID(calibParamClass, "getValue", "(I)[F");

    std::map<std::string, std::vector<float>> calibrationParameters;
    jint calibSize = env->CallIntMethod(parameters, getSize);
    jboolean isCopy;
    for (jint it = 0; it < calibSize; it++)
    {
        jstring key = (jstring) env->CallObjectMethod(parameters, getKey, it);

        jfloatArray dataArray = (jfloatArray) env->CallObjectMethod(parameters, getValue, it);
        jsize size = env->GetArrayLength(dataArray);
        jfloat* dataArrayPtr = env->GetFloatArrayElements(dataArray, &isCopy);
        std::vector<float> dataVector;
        dataVector.assign((unsigned long) size, 0);

        for(int i = 0; i < size; i++){
            dataVector[i] = dataArrayPtr[i];
        }

        const char* keyStr = env->GetStringUTFChars(key, &isCopy);

        // inserting data
        calibrationParameters.insert(std::pair<std::string, std::vector<float>>(keyStr, dataVector));

        // releasing memory
        env->ReleaseStringUTFChars(key, keyStr);
        env->ReleaseFloatArrayElements(dataArray, dataArrayPtr, JNI_ABORT);
        env->DeleteLocalRef(key);
        env->DeleteLocalRef(dataArray);
    }

    env->DeleteLocalRef(calibParamClass);

    //Transform data to mbtmatrix
    unsigned int nbChannels = (unsigned int) env->GetArrayLength(matrix);

    jdoubleArray channels1 = (jdoubleArray) env->GetObjectArrayElement(matrix, 0);
    jsize chan1TotalSamp = env->GetArrayLength(channels1);

    MBT_Matrix<float> signalMatrix(nbChannels, (unsigned int) chan1TotalSamp);

    for (unsigned int channelIndex = 0; channelIndex < nbChannels; channelIndex++)
    {
        // Merging channels
        jfloatArray currentChan = (jfloatArray) env->GetObjectArrayElement(matrix, channelIndex);
        jfloat* channel = env->GetFloatArrayElements(currentChan, &isCopy);
        if (channel == NULL)
            return NULL; // Out of memory
        for (unsigned int dataPoint = 0; dataPoint < chan1TotalSamp; dataPoint++)
        {
            signalMatrix(channelIndex, dataPoint) = channel[dataPoint];
        }
        env->ReleaseFloatArrayElements(currentChan, channel, JNI_ABORT);
        env->DeleteLocalRef(currentChan);
    }

    env->DeleteLocalRef(channels1);
    //SNR Computation pipeline
    float RelaxationIndex = MBT_ComputeRelaxIndex(signalMatrix, calibrationParameters, samprate, IAFinf, IAFsup);
    pastRelaxIndex.push_back(RelaxationIndex);
    float tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
    float tmp_Volum = MBT_RelaxIndexToVolum(tmp_SmoothedRelaxIndex, calibrationParameters);
    env->DeleteLocalRef(matrix);

    return tmp_Volum;
}


JNIEXPORT void JNICALL
Java_mybraintech_com_mbtsdk_core_signalprocessing_MBTComputeRelaxIndex_nativeReinitRelaxIndexVariables(JNIEnv *env,
                                                                                   jclass type) {
    pastRelaxIndex.clear();
}

