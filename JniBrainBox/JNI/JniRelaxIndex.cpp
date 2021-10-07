//
// Created by Michael Bacci on 16/09/2021.
//

#include "com_mybraintech_android_jnibrainbox_RelaxIndex.h"
#include "JniUtils.hpp"
#include <BrainBox/Matrix.hpp>
#include <BrainBox/Settings.hpp>
#include <BrainBox/RelaxIndex.hpp>

/*
 * Class:     com_mybraintech_android_jnibrainbox_RelaxIndex
 * Method:    new_relax_index
 * Signature: (I[[FFF)V
 */
JNIEXPORT void JNICALL Java_com_mybraintech_android_jnibrainbox_RelaxIndex_new_1relax_1index
(JNIEnv *env, jobject obj, jint sampling_rate, jfloatArray j_smoothed_rms, jfloat iaf_median_inf, jfloat iaf_median_sup)
{
    try {
        bb::Settings<double> settings{};
        settings.sampling_rate = static_cast<double>(sampling_rate);
        auto csptr_settings = std::make_shared<const bb::Settings<double>>(settings);

        auto smoothed_rms = getVectorFromJFloatArray<double>(env, j_smoothed_rms);

        auto *ri = new RelaxIndex<double>{
            .ris = bb::RelaxIndexSession<double>{csptr_settings,
                                                 smoothed_rms,
                                                 iaf_median_inf,
                                                 iaf_median_sup
        }};

        setHandle(env, obj, ri);
    } catch (std::exception e) {
        ThrowException(env, e.what());
    } catch (...) {
        ThrowException(env, "Unknown exception during new_relax_index");
    }
}


/*
 * Class:     com_mybraintech_android_jnibrainbox_RelaxIndex
 * Method:    destroy_relax_index
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_mybraintech_android_jnibrainbox_RelaxIndex_destroy_1relax_1index
(JNIEnv *env, jobject obj)
{
    auto * ri = getHandle<RelaxIndex<double>>(env, obj);
    delete ri;
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_RelaxIndex
 * Method:    compute
 * Signature: ([[F[F)F
 */
JNIEXPORT jfloat JNICALL Java_com_mybraintech_android_jnibrainbox_RelaxIndex_compute
        (JNIEnv *env, jobject obj, jobjectArray j_signals, jfloatArray j_quality)
{
    jfloat volume;
    try {
        auto* ri = getHandle<RelaxIndex<double>>(env, obj);
        auto data = ri->ris.compute(getMatrixFromObjectArray<double>(env, j_signals), getVectorFromJFloatArray<double>(env, j_quality));
        volume = data.volum;
    } catch (std::exception e) {
        ThrowException(env, e.what());
    } catch (...) {
        ThrowException(env, "Unknown exception during relax_index_compute");
    }
    return volume;
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_RelaxIndex
 * Method:    end_session
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_mybraintech_android_jnibrainbox_RelaxIndex_end_1session
        (JNIEnv *env, jobject obj)
{
    try {
        auto *ri = getHandle<RelaxIndex< double>>(env, obj);
        ri->session = ri->ris.end_session();
    } catch (std::exception e) {
        ThrowException(env, e.what());
    } catch (...) {
        ThrowException(env, "Unknown exception during end_session");
    }
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_RelaxIndex
 * Method:    get_mean_alpha_power
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_mybraintech_android_jnibrainbox_RelaxIndex_get_1mean_1alpha_1power
        (JNIEnv *env, jobject obj)
{
    return getHandle<RelaxIndex<double>>(env, obj)->session.mean_alpha_power;
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_RelaxIndex
 * Method:    get_mean_relative_alpha_power
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_mybraintech_android_jnibrainbox_RelaxIndex_get_1mean_1relative_1alpha_1power
        (JNIEnv *env, jobject obj)
{
    return getHandle<RelaxIndex<double>>(env, obj)->session.mean_relative_alpha_power;
}


/*
 * Class:     com_mybraintech_android_jnibrainbox_RelaxIndex
 * Method:    get_confidence
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_mybraintech_android_jnibrainbox_RelaxIndex_get_1confidence
        (JNIEnv *env, jobject obj)
{
    return getHandle<RelaxIndex<double>>(env, obj)->session.confidence;
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_RelaxIndex
 * Method:    get_volums
 * Signature: ()[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_mybraintech_android_jnibrainbox_RelaxIndex_get_1volums
        (JNIEnv *env, jobject obj)
{
    return getJFloatArrayFromVector(env, getHandle<RelaxIndex<double>>(env, obj)->session.volums);
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_RelaxIndex
 * Method:    get_alpha_powers
 * Signature: ()[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_mybraintech_android_jnibrainbox_RelaxIndex_get_1alpha_1powers
        (JNIEnv *env, jobject obj)
{
    return getJFloatArrayFromVector(env, getHandle<RelaxIndex<double>>(env, obj)->session.alpha_powers);
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_RelaxIndex
 * Method:    get_relative_alpha_powers
 * Signature: ()[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_mybraintech_android_jnibrainbox_RelaxIndex_get_1relative_1alpha_1powers
        (JNIEnv *env, jobject obj)
{
    return getJFloatArrayFromVector(env, getHandle<RelaxIndex<double>>(env, obj)->session.relative_alpha_powers);
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_RelaxIndex
 * Method:    get_qualities
 * Signature: ()[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_mybraintech_android_jnibrainbox_RelaxIndex_get_1qualities
        (JNIEnv *env, jobject obj)
{
    return getJFloatArrayFromVector(env, getHandle<RelaxIndex<double>>(env, obj)->session.qualities);
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_RelaxIndex
 * Method:    get_past_relax_index
 * Signature: ()[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_mybraintech_android_jnibrainbox_RelaxIndex_get_1past_1relax_1index
        (JNIEnv *env, jobject obj)
{
    return getJFloatArrayFromVector(env, getHandle<RelaxIndex<double>>(env, obj)->session.past_relax_index);
}

/*
 * Class:     com_mybraintech_android_jnibrainbox_RelaxIndex
 * Method:    get_smoothed_relax_index
 * Signature: ()[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_mybraintech_android_jnibrainbox_RelaxIndex_get_1smoothed_1relax_1index
        (JNIEnv *env, jobject obj)
{
    return getJFloatArrayFromVector(env, getHandle<RelaxIndex<double>>(env, obj)->session.smoothed_relax_index);
}