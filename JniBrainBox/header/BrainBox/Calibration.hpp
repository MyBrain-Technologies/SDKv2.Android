#pragma once
#include "Matrix.hpp"
#include "Settings.hpp"

namespace brainbox {

template <typename T>
struct CalibrationOutputData {
    int error_code{0};
    std::vector<T> rms;
    std::vector<T> relative_rms;
    std::vector<T> smoothed_rms;
    std::vector<T> hist_freq;
    std::vector<T> iaf;
};

/**
 * @brief Compute the calibration parameters
 * 
 * @param settings Neurofeedback settings
 * @param calibrationRecordings The EEG signals as it's returned by the QualityChecker (number of rows = number of channels)
 * @param calibrationRecordingsQuality The quality of each EEG signal as it's returned by the QualityChecker (number of rows = number of channels)
 * @return CalibrationOutputData<T>
 */

template <typename T>
CalibrationOutputData<T> calibration(const Settings<T>& settings, const Matrix<T>& calibrationRecordings,
                                     const Matrix<T>& calibrationRecordingsQuality);


}  // namespace brainbox

