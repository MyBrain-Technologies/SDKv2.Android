cmake_minimum_required(VERSION 3.4.1)

# Require C++17
# =============
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Temporary fix, waiting SDK C++ checks on CMake config file
set(lib_fftw3_DIR ${CMAKE_CURRENT_LIST_DIR}/dist/${ANDROID_ABI}/libfftw3.a)
set(lib_brainbox_DIR ${CMAKE_CURRENT_LIST_DIR}/dist/${ANDROID_ABI}/libbrainbox.a)
set(lib_brainbox_INCLUDE ${CMAKE_CURRENT_LIST_DIR}/header)

message("Current CMake prefix path : ${CMAKE_PREFIX_PATH}")

include_directories(${lib_brainbox_INCLUDE})
include_directories(JNI)
add_library(
        jni_brainboxsdk
        SHARED
        JNI/JniUtils.cpp
        JNI/JniQualityChecker.cpp
        JNI/JniCalibration.cpp
        JNI/JniRelaxIndex.cpp
)

target_link_libraries(
        jni_brainboxsdk
        ${lib_brainbox_DIR}
        ${lib_fftw3_DIR}
)
