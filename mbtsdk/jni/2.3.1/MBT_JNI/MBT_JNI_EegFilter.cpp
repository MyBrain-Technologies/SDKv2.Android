//
// Created by Mickael on 30/11/2018.
//

#include <jni.h>
#include "MBT_JNI_EegFilter.h"

#include "../Melomind.Algorithms/SignalProcessing.Cpp/PreProcessing/Headers/MBT_BandPass_fftw3.h"


/**
 * Jni function to call the correct compute statistics functions
 */
JNIEXPORT jfloatArray JNICALL Java_core_eeg_signalprocessing_MBTEegFilter_nativeBandpassFilter(JNIEnv *env, jobject instance,
                                                                               jfloat freqBound1,
                                                                               jfloat freqBound2,
                                                                               jint size,
                                                                               jfloatArray inputData_) {

    std::vector<double> inputDataVector((unsigned long) size);

    //First fill vector with input values
    jfloat *inputData = env->GetFloatArrayElements(inputData_, NULL);
    for (unsigned int dataPoint = 0; dataPoint < size; dataPoint++)
    {
        inputDataVector[dataPoint] = inputData[dataPoint];
    }
    env->ReleaseFloatArrayElements(inputData_, inputData, 0);
    env->DeleteLocalRef(inputData_);

    std::vector<double> freqBounds;
    freqBounds.assign(2,0);
    freqBounds[0] = freqBound1;
    freqBounds[1] = freqBound2;

    std::vector<double> filteredSignal = BandPassFilter(inputDataVector,freqBounds);

    std::vector<float> floatSignal;
    for(int i = 0; i < filteredSignal.size(); i++){

        floatSignal.push_back((float)filteredSignal[i]);
    }

    //Create jFloatArray from vector float
    jfloatArray jValueFloatArray = env->NewFloatArray((jsize) floatSignal.size());

    if (jValueFloatArray == NULL)
        return NULL; // ouf of memory

    jfloat temp[(jsize) floatSignal.size()];
    for (unsigned int it = 0; it < floatSignal.size(); it++)
        temp[it] = floatSignal.at(it);
    env->SetFloatArrayRegion(jValueFloatArray, 0, (jsize) floatSignal.size(), temp);
    //env->ReleaseFloatArrayElements(jValueFloatArray, temp, JNI_ABORT);

    return jValueFloatArray;
}