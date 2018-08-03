//
//  MBT_Fourier.h
//  MBT.iOS
//
//  Created by Emma Barme on 21/10/2015.
//  Copyright (c) 2015 Emma Barme. All rights reserved.
//
//  Inspired by Math.NET Numerics, part of the Math.NET Project
//  http://numerics.mathdotnet.com
//  http://github.com/mathnet/mathnet-numerics
//  http://mathnetnumerics.codeplex.com
//
//  Update on 09/12/2016 by Katerina Pandremmenou (double-->float)
// 	Update on 23/03/2017 by Fanny Grosselin (Change float by double)


#ifndef __MBT_iOS__MBT_Fourier__
#define __MBT_iOS__MBT_Fourier__

#include <stdio.h>
#include <climits>
#include <cmath>
#include <vector>
#include <complex>
#include <iostream>
#include <errno.h>
//#include "MBT_CPPMultithreading.h"


//Flags for the Fourier Transform.
enum MBT_FourierOptions {

    //Inverse integrand exponent (forward: positive sign; inverse: negative sign).
    InverseExponent = 0x01, //1

    //Only scale by 1/N in the inverse direction; No scaling in forward direction.
    AsymmetricScaling = 0x02, //2

    //Don't scale at all (neither on forward nor on inverse transformation).
    NoScaling = 0x04, //4

    //USABILITY POINTERS:

    //Universal; Symmetric scaling and common exponent (used in Maple).
    Default = 0,

    //Only scale by 1/N in the inverse direction; No scaling in forward direction (used in Matlab). [= AsymmetricScaling]
    Matlab = AsymmetricScaling,

    //Inverse integrand exponent; No scaling at all (used in all Numerical Recipes based implementations). [= InverseExponent | NoScaling]
    NumericalRecipes = InverseExponent | NoScaling

};

//Complex Fast (FFT) Implementation of the Discrete Fourier Transform (DFT).
class MBT_Fourier {

    friend class MBT_FourierBluestein;

public:

    //Public functions

    /*
     * @brief Applies the Bluestein forward Fast Fourier Transform (FFT) to arbitrary-length sample vectors.
     * @param inputData The vector with the data, where the FFT is evaluated in place.
     */
    static void forwardBluesteinFFT(std::vector<std::complex<double> >& inputData);

    /*
     * @brief Applies the Bluestein forward Fast Fourier Transform to arbitrary-length signals.
     * @param inputData The vector with the data, where the FFT is evaluated in place.
     * @param option The FFT flag.
     */
    static void forwardBluesteinFFT(std::vector<std::complex<double> >& inputData, const MBT_FourierOptions option);

    /*
     * @brief Applies the Bluestein inverse Fast Fourier Transform to arbitrary-length signals.
     * @param inputData The vector with the data, where the FFT is evaluated in place.
     */
    static void inverseBluesteinFFT(std::vector<std::complex<double> >& inputData);

    /*
     * @brief Applies the Bluestein inverse Fast Fourier Transform to arbitrary-length signals.
     * @param inputData The vector with the data, where the FFT is evaluated in place.
     * @param option The FFT flag.
     */
    static void inverseBluesteinFFT(std::vector<std::complex<double> >& inputData, const MBT_FourierOptions option);

    /*
     * @brief Naive forward DFT, useful for example to verify faster algorithms.
     * @param Time-space data vector.
     * @param option The FFT flag.
     * @return Corresponding frequency-space vector.
     */
    static std::vector<std::complex<double> > naiveForwardFFT(std::vector<std::complex<double> >& timeSpaceData, const MBT_FourierOptions option);

    /*
     * @brief Naive inverse DFT, useful for example to verify faster algorithms.
     * @param Frequency-space data vector.
     * @param option The FFT flag.
     * @return Corresponding time-space vector.
     */
    static std::vector<std::complex<double> > naiveInverseFFT(std::vector<std::complex<double> >& frequencySpaceData, const MBT_FourierOptions option);

    /*
     * @brief Applies the Radix2 forward Fast Fourier Transform to power-of-two sized signals.
     * @param inputData The vector with the data, where the FFT is evaluated in place.
     * @param option The FFT flag.
     */
    static void forwardRadix2FFT(std::vector<std::complex<double> >& inputData, const MBT_FourierOptions option);

    /*
     * @brief Applies the Radix2 inverse Fast Fourier Transform to power-of-two sized signals.
     * @param inputData The vector with the data, where the FFT is evaluated in place.
     * @param option The FFT flag.
     */
    static void inverseRadix2FFT(std::vector<std::complex<double> >& inputData, const MBT_FourierOptions option);

    /*
     * @brief Generate the frequencies corresponding to each index in frequency space. The frequency space has a resolution of sampleRate/N.
     * Index 0 corresponds to the DC part, the following indices correspond to the positive frequencies up to the Nyquist frequency (sampleRate/2), followed by the negative frequencies wrapped around.
     * @param nbSample Number of samples/data points.
     * @param sampRate The sampling rate of the time-space data.
     * @return A vector with the frequencies.
     */
    static std::vector<double> frequencyValues(const int nbSample, const double sampRate);

private:

    //General private functions.

    /*
     * @brief Extract the exponent sign to be used in transforms, according to the provided flag.
     * @param option The provided flag.
     * @return Fourier series exponent sign (1 or -1).
     */
    static int getExponentSign(const MBT_FourierOptions option);

    /*
     * @brief Rescale the values in a vector according to the provided flags, after forward FFT.
     * @param inputData The vector with the data to be rescaled.
     * @param option The FFT flag.
     */
    static void forwardScaling(std::vector<std::complex<double> >& inputData, const MBT_FourierOptions option);

    /*
     * @brief Rescale the values in a vector according to the provided flags, after inverse FFT.
     * @param inputData The vector with the data to be rescaled.
     * @param option The FFT flag.
     */
    static void inverseScaling(std::vector<std::complex<double> >& inputData, const MBT_FourierOptions option);


    //Private helper functions for Naive and Radix transforms.

    //Helper function for multithreading
    static void * workerNaive(void * arg);

    /*
     * @brief Naive generic Discrete Fourier Transform, useful for example to verify faster algorithms.
     * @param inputData A reference to the time-space data vector.
     * @param exponentSign The Fourier series exponent sign. See MBT_FourierOptions for more information.
     * @return The corresponding frequency-space vector.
     */
    static std::vector<std::complex<double> > naive(std::vector<std::complex<double> > const& inputData, const int exponentSign);

    /*
     * @brief Radix2 reorder helper method.
     * @typename T The type of the data.
     * @param inputData The data to be reordered.
     */
    template<typename T>
    static void radix2Reorder(std::vector<T>& inputData);

    /*
     * @brief Radix2 step helper method.
     * @param inputData The data to be modified.
     * @param exponentSign The Fourier series exponent sign. See MBT_FourierOptions for more information.
     * @param levelSize The level group size.
     * @param indexInLevel Index inside the level.
     */
    static void radix2Step(std::vector<std::complex<double> >& inputData, const int exponentSign, const int levelSize, const int indexInLevel);

    /*
     * @brief Radix2 generic Fast Fourier Transform for power-of-two sized signals.
     * @param inputData A reference to the time-space data vector, where the FFT is evaluated in place.
     * @param exponentSign The Fourier series exponent sign. See MBT_FourierOptions for more information.
     * @return The corresponding frequency-space vector.
     */
    static void radix2(std::vector<std::complex<double> >& inputData, const int exponentSign);
};

//Helper functions for the Bluestein version of the FFT.
class MBT_FourierBluestein {

    friend class MBT_Fourier;

private:

    /*
     * @brief Generate the Bluestein sequence for the provided data.
     * @param  nbSamble Number of samples/data points.
     * @return The Bluestein sequence: exp(I*Pi*k^2/N).
     */
    static std::vector<std::complex<double> > bluesteinSequence(const int nbSample);

    //Helper functions for multithreading
    static void * workerBluesteinConvolutionParallelA(void * arg);
    static void * workerBluesteinConvolutionParallelB(void * arg);

    /*
     * @brief Convolution with the Bluestein sequence (parallel version).
     * @param inputData A reference to the data vector.
     */
    static void bluesteinConvolutionParallel(std::vector<std::complex<double> >& inputData);

    /*
     * @brief Swap the real and imaginary parts of each data point.
     * @param inputData A reference to the data vector.
     */
    static void swapComplexParts(std::vector<std::complex<double> >& inputData);

    /*
     * @brief The Bluestein generic Fast Fourier Transform for arbitrary long signals.
     * @param inputData A reference to the data vector.
     * @param exponentSign The Fourier series exponent sign. See MBT_FourierOptions for more information.
     */
    static void bluestein(std::vector<std::complex<double> >& inputData, const int exponentSign);
};

#endif /* defined(__MBT_iOS__MBT_Fourier__) */
