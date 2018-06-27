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
 * Class:     core_eeg_signalprocessing_MBTComputeRelaxIndex
 * Method:    nativeComputeRelaxIndex
 * Signature: (ILjava/util/HashMap;[[D[[D)D
 */


#define IAFinf 6
#define IAFsup 13
static std::vector<float> pastRelaxIndex;
static std::vector<float> smoothedRelaxIndex;
static std::vector<float> volum;
static std::vector<float> histFreq;


/**
 * TODO move this function out of JNI. Should be accessible from C++ lib if possible
 * @param sampRate
 * @param paramCalib
 * @param sessionPacket
 * @param histFreq
 * @param pastRelaxIndex
 * @param resultSmoothedSNR
 * @param resultVolum
 * @return
 */
static float main_relaxIndex(const float sampRate, std::map<std::string, std::vector<float> > paramCalib,
                             const MBT_Matrix<float> &sessionPacket, std::vector<float> &histFreq, std::vector<float> &pastRelaxIndex, std::vector<float> &resultSmoothedSNR, std::vector<float> &resultVolum, int smoothingDuration)
{

    std::vector<float> errorMsg = paramCalib["errorMsg"];
    std::vector<float> snrCalib = paramCalib["snrCalib"];

    // Session-----------------------------------
    float snrValue = MBT_ComputeRelaxIndex(sessionPacket, errorMsg, sampRate, IAFinf, IAFsup, histFreq);

    pastRelaxIndex.push_back(snrValue); // incrementation of pastRelaxIndex
    float smoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex, smoothingDuration);
    float volum = MBT_RelaxIndexToVolum(smoothedRelaxIndex, snrCalib); // warning it's not the same inputs than previously

    //resultSmoothedSNR.assign(1,smoothedRelaxIndex);
    resultSmoothedSNR.push_back(smoothedRelaxIndex);

    //resultVolum.assign(1,volum);
    resultVolum.push_back(volum);
    // WARNING: If it's possible, I would like to save in the .json file, pastRelaxIndex and smoothedRelaxIndex (both)

    return volum;
}


/**
 * Function nativeComputeRelaxIndexNew
 */
JNIEXPORT jfloat JNICALL
Java_core_eeg_signalprocessing_MBTComputeRelaxIndex_nativeComputeRelaxIndex(JNIEnv *env,
                                                                              jclass type,
                                                                              jint samprate,
                                                                              jint smoothingDuration,
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
        //calibrationParameters.insert(std::pair<std::string, std::vector<float>>(keyStr, dataVector));
        calibrationParameters[keyStr] = dataVector;


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



//    //SNR Computation pipeline
//    float RelaxationIndex = MBT_ComputeRelaxIndex(signalMatrix, calibrationParameters, samprate, IAFinf, IAFsup);
//    pastRelaxIndex.push_back(RelaxationIndex);
//    float tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex,smoothingDuration);
//    float tmp_Volum = MBT_RelaxIndexToVolum(tmp_SmoothedRelaxIndex, calibrationParameters);

    //Completing histFreq at first occurence
    if(histFreq.size() == 0){
        histFreq = calibrationParameters["histFrequencies"];
    }

    float newVol = main_relaxIndex(samprate, calibrationParameters, signalMatrix, histFreq, pastRelaxIndex, smoothedRelaxIndex, volum, smoothingDuration);

    env->DeleteLocalRef(matrix);

    return newVol;
}


JNIEXPORT jobject JNICALL
core_eeg_signalprocessing_MBTComputeRelaxIndex_nativeGetSessionMetadata(JNIEnv *env,
                                                                            jclass type){

    // first, constructing c++ map
    std::map<std::string, std::vector<float> > sessionMetadata;
    sessionMetadata["rawRelaxIndexes"] = pastRelaxIndex;
    sessionMetadata["smoothedRelaxIndexes"] = smoothedRelaxIndex;
    //sessionMetadata["volum"] = volum;
    sessionMetadata["histFrequencies"] = histFreq;

    //then constructing jni map from c++ map

    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID initHasMap = env->GetMethodID(hashMapClass, "<init>", "(I)V");
    jmethodID putMethod = env->GetMethodID(hashMapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jobject jHashMap = env->NewObject(hashMapClass, initHasMap, 0);

    // Defining Double for JNI
    //jclass floatArrayClass = env->FindClass("[F");

    // filling Java HasMap
    for (auto data : sessionMetadata)
    {
        std::string key = data.first;
        std::vector<float> jvalueVector = data.second;

        //Create jFloatArray from vector float
        jfloatArray jValueFloatArray = env->NewFloatArray((jsize) jvalueVector.size());
        if (jValueFloatArray == NULL)
            return NULL; // ouf of memory

        jfloat temp[(jsize) jvalueVector.size()];
        for (unsigned int it = 0; it < jvalueVector.size(); it++)
            temp[it] = jvalueVector.at(it);
        env->SetFloatArrayRegion(jValueFloatArray, 0, (jsize) jvalueVector.size(), temp);

        env->CallObjectMethod(jHashMap, putMethod, env->NewStringUTF(key.c_str()), jValueFloatArray);
        //env->ReleaseFloatArrayElements(jValueFloatArray, temp, JNI_ABORT);
        env->DeleteLocalRef(jValueFloatArray);
    }

    env->DeleteLocalRef(hashMapClass);
    return jHashMap;

}



JNIEXPORT void JNICALL
core_eeg_signalprocessing_MBTComputeRelaxIndex_nativeReinitRelaxIndexVariables(JNIEnv *env,
                                                                                   jclass type) {
    pastRelaxIndex.clear();
    smoothedRelaxIndex.clear();
    volum.clear();
    histFreq.clear();
}

