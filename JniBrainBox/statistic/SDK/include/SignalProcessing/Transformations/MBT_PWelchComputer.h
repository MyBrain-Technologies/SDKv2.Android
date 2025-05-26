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
//          Fanny Grosselin 2017/11/30 --> Add some parameters (the length of the window, the overlap and the length of zero-padding) in input.


#ifndef __MBT_iOS__MBT_PWelchComputer__
#define __MBT_iOS__MBT_PWelchComputer__

#include <sp-global.h>

//#include "MBT_Fourier.h"
#include "Transformations/MBT_Fourier_fftw3.h"

#include "DataManipulation/MBT_Matrix.h"

#include <stdio.h>
#include <iostream>
#include <vector>
#include <complex>
#include <string>
#include <cmath>
#include <errno.h>

//Holds and compute the PSD of a signal
class MBT_PWelchComputer {

private:

    //Enum type of the different types of window that can be used for the computation.
    enum WindowType {RECT, HANN, HAMMING};

    //The overlaping parameter. Hard coded to 0.5.
    SP_RealType m_overlap = 0.5;

    //The segmentation parameter. Hard coded to 8.
    int m_nbrWindows = 8;

    // Length of the segmented window before zero-padding
    int m_windowLength;

    // Length of the segmented window after zero-padding
    int m_zeropadding = 0;

    //The window type for the computation. Default value is RECT.
    WindowType m_windowType = RECT;

    //The sampling rate.
    SP_RealType m_sampRate;

    //The number of channels in the input.
    int m_nbChannel;

    //The matrix holding the data in time, one row corresponds to one channel
    SP_Matrix m_inputData;

    //The matrix holding the computed data in frequency. First row is the frequencies used. Following m_nbChannel rows are the PSD values for the corresponding channel.
    SP_Matrix m_psd;

    //Same as m_psd but we have the one-sided spectrum
    SP_Matrix m_psd_Scaled;

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
    SP_RealType computeWindow(int windowLength, int segmentIndex) const;

public:

    /*
     * @brief MBT_PWelchComputer constructor.
     * @param inputData A SP_Matrix object with the input data, one row per channel.
     * @param sampRate The sampling rate.
     * @param windowType A string indicating the type of window to be used for the PSD computation.
     * @return A MBT_PWelchComputer initialized with the provided data.
     */
    MBT_PWelchComputer(SP_Matrix const& inputData, const SP_RealType sampRate, std::string windowType);

    /*
     * @brief MBT_PWelchComputer constructor.
     * @param inputData A SP_Matrix object with the input data, one row per channel.
     * @param windowLength The segmentation parameter. It's a length in samples.
     * @param overlapLength The overlaping parameter.
     * @param sampRate The sampling rate.
     * @param windowType A string indicating the type of window to be used for the PSD computation.
     * @param zeropaddingLength The length of the segmented window after zero-padding operation.
     * @return A MBT_PWelchComputer initialized with the provided data.
     */
    MBT_PWelchComputer(SP_Matrix const& inputData, const SP_RealType sampRate, std::string windowType, const int windowLength, const SP_RealType overlapLength, const int zeropaddingLength);

    /*
     * @brief MBT_PWelchComputer destructor.
     */
    ~MBT_PWelchComputer();

    /*
     * @brief Getter for the PSD matrix.
     * @return m_psd.
     */
    SP_Matrix get_PSD() const;

    /*
     * @brief Accessor to the PSD values of a specific channel.
     * @param The index of the desired channel. Channel indexing starts at 1. 0 is the frequency vector.
     * @return The PSD values for the desired channel.
     */
    SP_Vector get_PSD(int channelIndex) const;
};

/*
 * @brief Correct the signal in order to avoir 1/f trend into the psd.
 * @param inputData A SP_Matrix object with the input data, one row per channel.
 * @param sampRate The sampling rate.
 */
SP_Matrix MBT_trendCorrection(SP_Matrix inputData, const SP_RealType sampRate);


#endif /* defined(__MBT_iOS__MBT_PWelchComputer__) */
