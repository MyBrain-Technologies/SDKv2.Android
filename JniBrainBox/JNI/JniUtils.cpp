//
// Created by Michael Bacci on 16/09/2021.
//
#include "JniUtils.hpp"
#include <BrainBox/QualityChecker.hpp>
#include <BrainBox/RelaxIndex.hpp>

void ThrowException(JNIEnv *env, const char* msg) {
    env->ThrowNew(env->FindClass("java/lang/Exception"), msg);
}

void ThrowException(JNIEnv *env, std::string msg) {
    ThrowException(env, msg.c_str());
}

jfieldID inline getHandleField(JNIEnv *env, jobject obj) {
    auto c = env->GetObjectClass(obj);
    if (!c) ThrowException(env, "Can't get Object Class");
    auto fieldID = env->GetFieldID(c, "jniObjectPointer", "J");
    if (!fieldID) ThrowException(env, "Can't read jniObjectPointer field");
    return fieldID;
}

template <typename T>
T *getHandle(JNIEnv *env, jobject obj) {
    jlong handle = env->GetLongField(obj, getHandleField(env, obj));
    return reinterpret_cast<T*>(handle);
}

template <typename T>
void setHandle(JNIEnv *env, jobject obj, T *t) {
    jlong handle = reinterpret_cast<jlong>(t);
    env->SetLongField(obj, getHandleField(env, obj), handle);
}

std::pair<int, int> getObjectArraySize(JNIEnv *env, jobjectArray matrix) {
    int rows = env->GetArrayLength(matrix);
    jobjectArray firstRow = (jobjectArray)(env->GetObjectArrayElement(matrix, 0));
    int columns = env->GetArrayLength(firstRow);
    return {rows, columns};
}

template <typename T>
std::vector<T> getVectorFromJFloatArray(JNIEnv *env, jfloatArray input) {

    jint size = env->GetArrayLength(input);
    std::vector<T> vector((unsigned long) size);

    jfloat *inputData = env->GetFloatArrayElements(input, NULL);
    for (unsigned int dataPoint = 0; dataPoint < size; dataPoint++)
    {
            vector[dataPoint] = inputData[dataPoint];
    }
    env->ReleaseFloatArrayElements(input, inputData, 0);
    env->DeleteLocalRef(input);

    return vector;
}

template <typename T>
bb::Matrix<T> getMatrixFromObjectArray(JNIEnv *env, jobjectArray matrix) {
    bb::Matrix<T> bb_matrix;
    try {
        jboolean isCopy;
        int rows, columns;
        std::tie(rows, columns) = getObjectArraySize(env, matrix);
        bb_matrix = bb::Matrix<double>(rows, columns);

        for (unsigned int channelIndex = 0; channelIndex < rows; channelIndex++) {
            jfloatArray current = (jfloatArray) env->GetObjectArrayElement(matrix, channelIndex);
            jfloat *channel = env->GetFloatArrayElements(current, &isCopy);
            if (channel == NULL)
                throw std::runtime_error("Can't allocate data for signal matrix");
            for (unsigned int dataPoint = 0; dataPoint < columns; dataPoint++) {
                bb_matrix(channelIndex, dataPoint) = static_cast<double>(channel[dataPoint]);
            }
            env->ReleaseFloatArrayElements(current, channel, JNI_ABORT);
            env->DeleteLocalRef(current);
        }
    } catch (std::exception e) {
        ThrowException(env, e.what());
    } catch (...) {
        ThrowException(env, "Unknown exception during getMatrixFromObjectArray");
    }
    return bb_matrix;
}

template <typename T>
jfloatArray getJFloatArrayFromVector(JNIEnv* env, const std::vector<T>& array) {
    jfloat temp[array.size()];
    jfloatArray result = env->NewFloatArray(array.size());
    if (result == NULL)
        throw std::runtime_error("Can't allocate data for vector conversion");

    for (unsigned int it = 0; it < array.size(); it++)
        temp[it] = array[it];
    env->SetFloatArrayRegion(result, 0, array.size(), temp);

    return result;
}

template bb::Matrix<double> getMatrixFromObjectArray(JNIEnv *env, jobjectArray matrix);
template std::vector<double> getVectorFromJFloatArray(JNIEnv *env, jfloatArray input);
template jfloatArray getJFloatArrayFromVector(JNIEnv* env, const std::vector<double>& array);
template bb::QualityChecker<double> *getHandle(JNIEnv *env, jobject obj);
template void setHandle(JNIEnv *env, jobject obj, bb::QualityChecker<double> *t);
template Calibration<double> *getHandle(JNIEnv *env, jobject obj);
template void setHandle(JNIEnv *env, jobject obj, Calibration<double> *t);
template RelaxIndex<double> *getHandle(JNIEnv *env, jobject obj);
template void setHandle(JNIEnv *env, jobject obj, RelaxIndex<double> *t);
