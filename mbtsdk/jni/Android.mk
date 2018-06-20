LOCAL_PATH := $(call my-dir)
ROOT_PATH := $(LOCAL_PATH)

include $(call all-subdir-makefiles)

#compiling v2.3.0
include $(CLEAR_VARS)

LOCAL_PATH = $(ROOT_PATH)
LOCAL_CFLAGS := -Wall -Wextra
LOCAL_CFLAGS += -O3

MY_ALGO_VERSION := 2.3.0

LOCAL_MODULE    := libmbtalgo_$(MY_ALGO_VERSION)

LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog -lm
LOCAL_STATIC_LIBRARIES := libfftw3
LOCAL_CPPFLAGS += -std=c++11
LOCAL_SRC_FILES := $(MY_ALGO_VERSION)/MBT_JNI/MBT_JNI_ComputeQuality.cpp \
                     $(MY_ALGO_VERSION)/MBT_JNI/MBT_JNI_Calibrator.cpp \
                     $(MY_ALGO_VERSION)/MBT_JNI/MBT_JNI_ComputeRelaxIndex.cpp \
                     $(MY_ALGO_VERSION)/MBT_JNI/MBT_JNI_ComputeStatistics.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/QualityChecker/Cpp/MBT_MainQC.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/NF_Melomind/Cpp/MBT_ComputeCalibration.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/NF_Melomind/Cpp/MBT_ComputeRelaxIndex.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/NF_Melomind/Cpp/MBT_ComputeNoise.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/NF_Melomind/Cpp/MBT_ComputeSNR.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/NF_Melomind/Cpp/MBT_NormalizeRelaxIndex.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/NF_Melomind/Cpp/MBT_RelaxIndexToVolum.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/NF_Melomind/Cpp/MBT_SmoothRelaxIndex.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/Statistics/SNR/Cpp/MBT_SNR_Stats.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/SignalProcessing.Cpp/Algebra/Cpp/MBT_FindClosest.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/SignalProcessing.Cpp/Algebra/Cpp/MBT_Interpolation.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/SignalProcessing.Cpp/Algebra/Cpp/MBT_Operations.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/SignalProcessing.Cpp/Algebra/Cpp/MBT_kmeans.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/SignalProcessing.Cpp/DataManipulation/Cpp/MBT_Matrix.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/SignalProcessing.Cpp/PreProcessing/Cpp/MBT_BandPass_fftw3.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/SignalProcessing.Cpp/PreProcessing/Cpp/MBT_PreProcessing.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/SignalProcessing.Cpp/Transformations/Cpp/MBT_FindPeak.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/SignalProcessing.Cpp/Transformations/Cpp/MBT_Fourier_fftw3.cpp \
                     $(MY_ALGO_VERSION)/Melomind.Algorithms/SignalProcessing.Cpp/Transformations/Cpp/MBT_PWelchComputer.cpp


include $(BUILD_SHARED_LIBRARY)
