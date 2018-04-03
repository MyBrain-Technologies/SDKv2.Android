//
// Created by Vincent on 26/11/2015.
//
#include <jni.h>
#include <map>
#include "MBT_JNI_ComputeStatistics.h"
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
#include <complex>

#include "Melomind.Algorithms/SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h"
#include "Melomind.Algorithms/SignalProcessing.Cpp/PreProcessing/Headers/MBT_PreProcessing.h"
#include "Melomind.Algorithms/SignalProcessing.Cpp/Algebra/Headers/MBT_Operations.h"
#include "Melomind.Algorithms/SignalProcessing.Cpp/Transformations/Headers/MBT_Fourier_fftw3.h"
#include "Melomind.Algorithms/SignalProcessing.Cpp/Algebra/Headers/MBT_Interpolation.h"
#include "Melomind.Algorithms/SignalProcessing.Cpp/PreProcessing/Headers/MBT_BandPass_fftw3.h"
#include "Melomind.Algorithms/SignalProcessing.Cpp/Transformations/Headers/MBT_PWelchComputer.h"
#include "Melomind.Algorithms/SignalProcessing.Cpp/Algebra/Headers/MBT_kmeans.h"
#include "Melomind.Algorithms/SignalProcessing.Cpp/DataManipulation/Headers/MBT_ReadInputOrWriteOutput.h"
#include "Melomind.Algorithms/Statistics/SNR/Headers/MBT_SNR_Stats.h"
/*
 * Class:     modules_signalprocessing_MBTComputeStatistics
 * Method:    nativeComputeRelaxIndex
 * Signature: (ILjava/util/HashMap;[[D[[D)D
 */


/**
 * Jni function to call the correct compute statistics functions
 */
JNIEXPORT jobject JNICALL
Java_mybraintech_com_mbtsdk_core_signalprocessing_MBTComputeStatistics_nativeComputeStatisticsSNR(JNIEnv *env, jclass type,
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

    //applying nan filter
    //std::vector<float> original  = SkipNaN(inputDataVector);

    //running main function
    //MBT_Matrix<float> results = runEverything(original, 0);

    /*vector<float> SNR;
    for (unsigned int a = 0 ; a < ComplexSNR.size(); a++)
        SNR.push_back(real(ComplexSNR[a]));*/

    SNR_Statistics obj(inputDataVector);
    //double ExerciseThreshold = 0.65;
    std::map<string, float> resultStatistics = obj.CalculateSNRStatistics(inputDataVector, (double)threshold);

    /*std::map<std::string, std::vector<float>> resultStatistics;
    std::vector<float> tempResult;


    //For Areas
    for(int j = 0; j < results.size().second; j++){
        tempResult.push_back(results(0, (unsigned int) j));
    }
    resultStatistics["Distances"] = tempResult;
    tempResult.clear();

    //For Timelength
    for(int j = 0; j < results.size().second; j++){
        tempResult.push_back(results(1, (unsigned int) j));
    }
    resultStatistics["Timelength"] = tempResult;
    tempResult.clear();

    //For distances
    for(int j = 0; j < results.size().second; j++){
        tempResult.push_back(results(2, (unsigned int) j));
    }
    resultStatistics["Areas"] = tempResult;
    tempResult.clear();*/

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
        /*//Create jFloatArray from vector float
        jfloatArray jValueFloatArray = env->NewFloatArray((jsize) jvalueVector.size());
        if (jValueFloatArray == NULL)
            return NULL; // ouf of memory

        jfloat temp[(jsize) jvalueVector.size()];
        for (unsigned int it = 0; it < jvalueVector.size(); it++)
            temp[it] = jvalueVector.at(it);
        env->SetFloatArrayRegion(jValueFloatArray, 0, (jsize) jvalueVector.size(), temp);*/

        env->CallObjectMethod(jResultMap, putMethod, env->NewStringUTF(key.c_str()), newFloatObj);
        env->DeleteLocalRef(newFloatObj);
    }

    env->DeleteLocalRef(hashMapClass);
    return jResultMap;
}