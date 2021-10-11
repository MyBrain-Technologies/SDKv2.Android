//
// Created by Michael Bacci on 16/09/2021.
//
#pragma once
#include <BrainBox/Matrix.hpp>
#include <BrainBox/Settings.hpp>
#include <BrainBox/Calibration.hpp>
#include <BrainBox/RelaxIndex.hpp>
#include <jni.h>
#include <tuple>

namespace bb = brainbox;

template <typename T>
struct Calibration {
    bb::Settings<double> settings;
    bb::CalibrationOutputData<T> calibration;
};


template <typename T>
struct RelaxIndex {
    bb::RelaxIndexSession<T> ris;
    bb::RelaxIndexSessionOutputData<T> session;
};

void ThrowException(JNIEnv *env, const char* msg);

void ThrowException(JNIEnv *env, std::string msg);

jfieldID inline getHandleField(JNIEnv *env, jobject obj);

template <typename T>
T *getHandle(JNIEnv *env, jobject obj);

template <typename T>
void setHandle(JNIEnv *env, jobject obj, T *t);

std::pair<int, int> getObjectArraySize(JNIEnv *env, jobjectArray matrix);

template <typename T>
std::vector<T> getVectorFromJFloatArray(JNIEnv *env, jfloatArray input);

template <typename T>
bb::Matrix<T> getMatrixFromObjectArray(JNIEnv *env, jobjectArray matrix);
template <typename T>
jfloatArray getJFloatArrayFromVector(JNIEnv* env, const std::vector<T>& array);