#pragma once
#include "Global.hpp"
#include "Matrix.hpp"
#include "Settings.hpp"

#include <stdio.h>
#include <iostream>
#include <vector>
#include <complex>
#include <string>
#include <cmath>

namespace brainbox {
//Holds and compute the PSD of a signal
template <typename T>
class PWelchComputer {

    //Enum type of the different types of window that can be used for the computation.
    enum WindowType {RECT, HANN, HAMMING};

    //The overlaping parameter. Hard coded to 0.5.
    T m_overlap = 0.5;

    //The segmentation parameter. Hard coded to 8.
    int m_nbrWindows = 8;

    // Length of the segmented window before zero-padding
    int m_windowLength;

    // Length of the segmented window after zero-padding
    int m_zero_padding = 0;

    //The window type for the computation. Default value is RECT.
    WindowType m_windowType = RECT;

    //The sampling rate.
    T m_sampRate;

    //The number of channels in the input.
    int m_nbChannel;

    //The matrix holding the data in time, one row corresponds to one channel
    Matrix<T> input_signal;

    //The matrix holding the computed data in frequency. First row is the frequencies used. Following m_nbChannel rows are the PSD values for the corresponding channel.
    Matrix<T> m_psd;

    //Same as m_psd but we have the one-sided spectrum
    Matrix<T> m_psd_Scaled;

    const Settings<T>& m_settings;

    /*
     * @brief Computes the PSD.
     * @todo Implement for more than 2 channels.
     */
    void computePSD();

    /*
     * @brief Computes the coefficient corresponding to the type of window specified.
     * @param windowLength The length of the window.
     * @param segmentIndex The index of the considered segment.
     */
    T computeWindow(int windowLength, int segmentIndex) const;

public:

    /*
     * @brief PWelchComputer constructor.
     * @param inputData A Matrix<T> object with the input data, one row per channel.
     * @param sampRate The sampling rate.
     * @param window_type A string indicating the type of window to be used for the PSD computation.
     * @return A PWelchComputer initialized with the provided data.
     */
    PWelchComputer(Matrix<T> const& inputData, const Settings<T>& settings);

    /*
     * @brief PWelchComputer constructor.
     * @param inputData A Matrix<T> object with the input data, one row per channel.
     * @param windowLength The segmentation parameter. It's a length in samples.
     * @param overlapLength The overlaping parameter.
     * @param sampRate The sampling rate.
     * @param window_type A string indicating the type of window to be used for the PSD computation.
     * @param zero_paddingLength The length of the segmented window after zero-padding operation.
     * @return A PWelchComputer initialized with the provided data.
     */
    PWelchComputer(Matrix<T> const& inputData,  const Settings<T>& settings, const int windowLength, const T overlapLength, const int zero_paddingLength);

    /*
     * @brief PWelchComputer destructor.
     */
    ~PWelchComputer();

    /*
     * @brief Getter for the PSD matrix.
     * @return m_psd.
     */
    Matrix<T> get_PSD() const;

    /*
     * @brief Accessor to the PSD values of a specific channel.
     * @param The index of the desired channel. Channel indexing starts at 1. 0 is the frequency vector.
     * @return The PSD values for the desired channel.
     */
    std::vector<T> get_PSD(int channelIndex) const;
};

/*
 * @brief Correct the signal in order to avoir 1/f trend into the psd.
 * @param inputData A Matrix<T> object with the input data, one row per channel.
 * @param sampRate The sampling rate.
 */
template <typename T>
Matrix<T> trend_correction(const Matrix<T>& inputData, const T sampRate);

} // namespace brainbox