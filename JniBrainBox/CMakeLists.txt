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

set(FFTW3_DIR ${CMAKE_CURRENT_LIST_DIR}/statistic/SDK/lib/${ANDROID_ABI}/cmake/fftw3/)
set(libSignalProcessing_DIR ${CMAKE_CURRENT_LIST_DIR}/statistic/SDK/lib/${ANDROID_ABI}/cmake/SignalProcessing/)
set(libMyBrainTechSDK_DIR ${CMAKE_CURRENT_LIST_DIR}/statistic/SDK/lib/${ANDROID_ABI}/cmake/MyBrainTechSDK/)

find_package(libMyBrainTechSDK)

message("Current CMake prefix path : ${CMAKE_PREFIX_PATH}")

include_directories(${lib_brainbox_INCLUDE})
include_directories(JNI)
include_directories(statistic)
add_library(
        mbt_statistic
        SHARED
        statistic/MBT_Statistic/MBT_JNI_ComputeStatistics.cpp
        statistic/MBT_Statistic/MBT_JNI_ComputeStatistics.h
        statistic/MBT_Statistic/modules_signalprocessing_MBTSignalComputeStatistics.h
)

target_link_libraries( mbt_statistic
        #    ${log-lib}
        #    MyBrainTechSDK::NF_Melomind
        #    MyBrainTechSDK::QualityChecker
       MyBrainTechSDK::SNR
        #    MyBrainTechSDK::TimeFrequency
       SignalProcessing::DataManipulation
       SignalProcessing::Algebra
       SignalProcessing::PreProcessing
       SignalProcessing::Transformations
)


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
