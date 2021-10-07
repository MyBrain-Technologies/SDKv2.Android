//
// Created by Michael Bacci on 16/09/2021.
//

#include "com_mybraintech_android_jnibrainbox_QualityChecker.h"
#include "JniUtils.hpp"
#include <BrainBox/QualityChecker.hpp>
#include <BrainBox/Math.hpp>
#include <BrainBox/TrainingData.hpp>
#include <BrainBox/Matrix.hpp>
#include <BrainBox/Settings.hpp>

/*
 * Class:     com_mybraintech_android_jnibrainbox_QualityChecker
 * Method:    new_quality_checker
 * Signature: ()J
 */
JNIEXPORT void JNICALL
Java_com_mybraintech_android_jnibrainbox_QualityChecker_new_1quality_1checker
(JNIEnv *env, jobject obj, jint sampling_rate)
{   try {
        bb::Settings<double> settings{};
        settings.sampling_rate = static_cast<double>(sampling_rate);
        auto csptr_settings = std::make_shared<const bb::Settings<double>>(settings);
        auto *qc = new bb::QualityChecker<double>{bb::TrainingData<double>::GetGoodTraining()};
        qc->WithSettings(csptr_settings);
        setHandle(env, obj, qc);
    } catch (std::exception e) {
        ThrowException(env, e.what());
    } catch (...) {
        ThrowException(env, "Unknown exception during new_quality_checker");
    }
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_QualityChecker
 * Method:    destroy_quality_checker
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_mybraintech_android_jnibrainbox_QualityChecker_destroy_1quality_1checker
(JNIEnv *env , jobject obj)
{
    auto * p = getHandle<bb::QualityChecker<double>>(env, obj);
    delete p;
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_QualityChecker
 * Method:    compute_quality_checker
 * Signature: ([[FI)[F
 */
JNIEXPORT jfloatArray JNICALL
Java_com_mybraintech_android_jnibrainbox_QualityChecker_compute_1quality_1checker
(JNIEnv *env, jobject obj, jobjectArray matrix)
{
    jfloatArray result;
    try {
        auto signals = getMatrixFromObjectArray<double>(env, matrix);
        auto qr = getHandle<bb::QualityChecker<double>>(env, obj)->compute(signals);
        result = getJFloatArrayFromVector(env, qr.quality);
    } catch (std::exception e) {
        ThrowException(env, e.what());
    } catch (...) {
        ThrowException(env, "Unknown exception during compute_quality_checker");
    }
    return result;
}

