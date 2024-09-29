//
// Created by Vincent on 26/11/2015.
//

#include "MBT_JNI_ComputeStatistics.h"

#include <DataManipulation/MBT_Matrix.h>
#include <DataManipulation/MBT_ReadInputOrWriteOutput.h>

#include <PreProcessing/MBT_PreProcessing.h>
#include <PreProcessing/MBT_BandPass_fftw3.h>

#include <Algebra/MBT_Operations.h>
#include <Algebra/MBT_Interpolation.h>
#include <Algebra/MBT_kmeans.h>

#include <Transformations/MBT_Fourier_fftw3.h>
#include <Transformations/MBT_PWelchComputer.h>

#include <SNR/MBT_SNR_Stats.h>

#include <jni.h>
#include <map>
#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <iterator>
#include <iomanip>
#include <string>
#include <algorithm>
#include <ctime>
#include <chrono>
#include <math.h>
#include <stdio.h>
#include <complex>/*
 * Class:     mybraintech_com_mbtsdk_modules_signalprocessing_MBTComputeStatistics
 * Method:    nativeComputeRelaxIndex
 * Signature: (ILjava/util/HashMap;[[D[[D)D
 */


/**
 * Jni function to call the correct compute statistics functions
 */
JNIEXPORT jobject JNICALL
Java_com_mybraintech_android_jnibrainbox_ComputeStatistics_nativeComputeStatisticsSNR(JNIEnv *env, jclass type,
                                                                                      jfloat threshold,
                                                                                      jint size,
                                                                                      jfloatArray inputData_) {
    vector<float> inputDataVector((unsigned long) size);

    //First fill vector with input values
    jfloat *inputData = env->GetFloatArrayElements(inputData_, NULL);
    for (unsigned int dataPoint = 0; dataPoint < size; dataPoint++)
    {
        inputDataVector[dataPoint] = inputData[dataPoint];
    }
    env->ReleaseFloatArrayElements(inputData_, inputData, 0);
    env->DeleteLocalRef(inputData_);

    SNR_Statistics obj(inputDataVector);
    //double ExerciseThreshold = 0.65;
    std::map<string, float> resultStatistics = obj.CalculateSNRStatistics(inputDataVector, (double)threshold);

    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID initHashMap = env->GetMethodID(hashMapClass, "<init>", "(I)V");
    jmethodID putMethod = env->GetMethodID(hashMapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jobject jResultMap = env->NewObject(hashMapClass, initHashMap, 0);//calibParams.size());


    // filling Java HashMap
    for (auto resultValue : resultStatistics)
    {
        std::string key = resultValue.first;
        jfloat jvalueVector = resultValue.second;


        //Create Integer class, get constructor and create Integer object
        jclass floatClass = env->FindClass("java/lang/Float");
        jmethodID initFloat = env->GetMethodID(floatClass, "<init>", "(F)V");
        if (NULL == initFloat)
            return NULL;

        jobject newFloatObj = env->NewObject(floatClass, initFloat, jvalueVector);

        env->CallObjectMethod(jResultMap, putMethod, env->NewStringUTF(key.c_str()), newFloatObj);
        env->DeleteLocalRef(newFloatObj);
    }

    env->DeleteLocalRef(hashMapClass);
    return jResultMap;
}