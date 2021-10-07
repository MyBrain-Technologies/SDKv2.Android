//
// Created by Michael Bacci on 16/09/2021.
//

#include "com_mybraintech_android_jnibrainbox_Calibration.h"
#include "JniUtils.hpp"
#include <BrainBox/Matrix.hpp>
#include <BrainBox/Settings.hpp>
#include <BrainBox/Calibration.hpp>
#include <jni.h>

JNIEXPORT void JNICALL Java_com_mybraintech_android_jnibrainbox_Calibration_new_1calibration
(JNIEnv *env, jobject obj, jint simpling_rate, jint sliding_windows_sec)
{
    try {
        auto* c = new Calibration<double>{};
        c->settings.sampling_rate = static_cast<double>(simpling_rate);
        c->settings.iaf_sliding_window_sec =static_cast<double>(sliding_windows_sec);
        setHandle(env, obj, c);
    } catch (std::exception e) {
        ThrowException(env, e.what());
    } catch (...) {
        ThrowException(env, "Unknown exception during new_calibration");
    }
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_Calibration
 * Method:    destroy_calibration
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_mybraintech_android_jnibrainbox_Calibration_destroy_1calibration
(JNIEnv *env, jobject obj)
{
    auto *c = getHandle<Calibration<double>>(env, obj);
    delete c;
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_Calibration
 * Method:    compute_calibration
 * Signature: ([[F[[F)I
 */
JNIEXPORT jint JNICALL Java_com_mybraintech_android_jnibrainbox_Calibration_compute_1calibration
        (JNIEnv * env, jobject obj, jobjectArray j_signals, jobjectArray j_qualities)
{
    int error_code = 0;
    try {
        auto signals = getMatrixFromObjectArray<double>(env, j_signals);
        auto qualities = getMatrixFromObjectArray<double>(env, j_qualities);

        auto *c = getHandle<Calibration<double>>(env, obj);
        c->calibration = bb::calibration(c->settings, signals, qualities);
        error_code = c->calibration.error_code;
    } catch (std::exception e) {
        ThrowException(env, e.what());
    } catch (...) {
        ThrowException(env, "Unknown exception during compute_quality_checker");
    }
    return error_code;
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_Calibration
 * Method:    get_rms
 * Signature: ()[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_mybraintech_android_jnibrainbox_Calibration_get_1rms
        (JNIEnv *env, jobject obj)
{
    return getJFloatArrayFromVector(env, getHandle<Calibration<double>>(env, obj)->calibration.rms);
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_Calibration
 * Method:    get_relative_rms
 * Signature: ()[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_mybraintech_android_jnibrainbox_Calibration_get_1relative_1rms
        (JNIEnv *env, jobject obj)
{
    return getJFloatArrayFromVector(env, getHandle<Calibration<double>>(env, obj)->calibration.relative_rms);
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_Calibration
 * Method:    get_relative_rms
 * Signature: ()[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_mybraintech_android_jnibrainbox_Calibration_get_1smoothed_1rms
        (JNIEnv *env, jobject obj)
{
    return getJFloatArrayFromVector(env, getHandle<Calibration<double>>(env, obj)->calibration.smoothed_rms);
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_Calibration
 * Method:    get_hist_freq
 * Signature: ()[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_mybraintech_android_jnibrainbox_Calibration_get_1hist_1freq
        (JNIEnv *env, jobject obj)
{
    return getJFloatArrayFromVector(env, getHandle<Calibration<double>>(env, obj)->calibration.hist_freq);
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_Calibration
 * Method:    get_iaf
 * Signature: ()[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_mybraintech_android_jnibrainbox_Calibration_get_1iaf
        (JNIEnv *env, jobject obj)
{
    return getJFloatArrayFromVector(env, getHandle<Calibration<double>>(env, obj)->calibration.iaf);
}