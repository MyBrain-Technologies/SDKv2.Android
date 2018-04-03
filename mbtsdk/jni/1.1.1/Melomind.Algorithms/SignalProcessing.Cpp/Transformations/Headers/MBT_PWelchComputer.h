//
//  MBT_PWelchComputer.h
//  MBT.iOS
//
//  Created by Emma Barme on 19/10/2015.
//  Copyright (c) 2015 Emma Barme. All rights reserved.
//
//  Update: Fanny Grosselin 2016/12/13 --> add a method trendCorrection
//          Fanny Grosselin 2017/01/17 --> Add m_psd_Scaled which holds the one-sided spectrum
//          Fanny Grosselin 2017/01/19 --> Add a method fft
//          Fanny Grosselin 2017/01/20 --> Remove the method fft
//          Fanny Grosselin 2017/03/16 --> Use MBT_Fourier_fftw3 instead of using MBT_Fourier and change float to double


#ifndef __MBT_iOS__MBT_PWelchComputer__
#define __MBT_iOS__MBT_PWelchComputer__

#include <stdio.h>
#include <iostream>
#include <vector>
#include <complex>
#include <string>
#include <cmath>
#include <errno.h>
#include "../../DataManipulation/Headers/MBT_Matrix.h"
//#include "MBT_Fourier.h"
#include "MBT_Fourier_fftw3.h"

//Holds and compute the PSD of a signal
class MBT_PWelchComputer {

private:

    //Enum type of the different types of window that can be used for the computation.
    enum WindowType {RECT, HANN, HAMMING};

    //The overlaping parameter. Hard coded to 0.5.
    const double m_overlap = 0.5;

    //The segmentation parameter. Hard coded to 8.
    const int m_nbrWindows = 8;

    //The window type for the computation. Default value is RECT.
    WindowType m_windowType = RECT;

    //The sampling rate.
    double m_sampRate;

    //The number of channels in the input.
    int m_nbChannel;

    //The matrix holding the data in time, one row corresponds to one channel
    MBT_Matrix<double> m_inputData;

    //The matrix holding the computed data in frequency. First row is the frequencies used. Following m_nbChannel rows are the PSD values for the corresponding channel.
    MBT_Matrix<double> m_psd;

    //Same as m_psd but we have the one-sided spectrum
    MBT_Matrix<double> m_psd_Scaled;

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
    double computeWindow(int windowLength, int segmentIndex) const;

public:

    /*
     * @brief MBT_PWelchComputer constructor.
     * @param inputData A MBT_Matrix<double> object with the input data, one row per channel.
     * @param sampRate The sampling rate.
     * @param windowType A string indicating the type of window to be used for the PSD computation.
     * @return A MBT_PWelchComputer initialized with the provided data.
     */
    MBT_PWelchComputer(MBT_Matrix<double> const& inputData, const double sampRate, std::string windowType);

    /*
     * @brief MBT_PWelchComputer destructor.
     */
    ~MBT_PWelchComputer();

    /*
     * @brief Getter for the PSD matrix.
     * @return m_psd.
     */
    MBT_Matrix<double> get_PSD() const;

    /*
     * @brief Accessor to the PSD values of a specific channel.
     * @param The index of the desired channel. Channel indexing starts at 1. 0 is the frequency vector.
     * @return The PSD values for the desired channel.
     */
    std::vector<double> get_PSD(int channelIndex) const;
};

/*
 * @brief Correct the signal in order to avoir 1/f trend into the psd.
 * @param inputData A MBT_Matrix<double> object with the input data, one row per channel.
 * @param sampRate The sampling rate.
 */
MBT_Matrix<double> MBT_trendCorrection(MBT_Matrix<double> inputData, const double sampRate);


#endif /* defined(__MBT_iOS__MBT_PWelchComputer__) */
