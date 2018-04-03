//
// MBT_PreProcess.h
//
// Created by Katerina Pandremmenou on 2016/09/29
// Copyright (c) 2016 myBrain Technologies. All rights reserved.
//
// Update: 08/12/2016 - Inclusion of detrend, calculateGain
//         03/02/2017 by Fanny Grosselin : Add a method to interpolate outliers
// 		   23/03/2017 by Fanny Grosselin : Change float by double for the functions not directly used by Andro�d. For the others, keep inputs and outputs in float, but do the steps with double or create two functions : one with only float, another one with only double.
// Update on 31/03/2017 by Katerina Pandremmenou (Inclusion of RemoveDCF2D function)
// Update on 03/04/2017 by Katerina Pandremmenou (Inclusion of CalculateBounds function for doubles)
// Update on 14/09/2017 by Katerina Pandremmenou (Inclusion of OutliersToNan function)
// Update on 18/09/2017 by Katerina Pandremmenou (Inclusion of MBT_InterpolateBetweenChannels and MBT_InterpolateAcrossChannels functions)
// Update on 20/09/2017 by Katerina Pandremmenou (Inclusion of a function to skip the NaN values when present)
// Update on 21/09/2017 by Katerina Pandremmenou (Inclusion of a function to remove possible NaN values in the beggining or the end of an MBT_Matrix)

#ifndef MBT_PREPROCESSING_H
#define MBT_PREPROCESSING_H


#include <vector>
#include <algorithm>
#include <functional>
#include <iostream>
#include "math.h" // for nan()

#include "../../Algebra/Headers/MBT_Interpolation.h"
#include "../../DataManipulation/Headers/MBT_Matrix.h"

using namespace std;

// Method which removes the DC offset
vector<float>  RemoveDC(vector<float> Data);
vector<double> RemoveDC(vector<double> Data);
// takes float in input and returns double in output
vector<double> RemoveDCF2D(vector<float> Data);

// Methods for calculating the lower and upper bound
// for floats
vector<float> CalculateBounds(vector<float> DataNoDC);
// for doubles
vector<double> CalculateBounds(vector<double> DataNoDC);

// Method that removes the outliers
vector<double> RemoveOutliers(vector<double> DataNoDC, vector<double> Bounds);

// Method that interpolates the outliers  // Fanny Grosselin 2017/02/03
/*
 * Interpolate the outliers detected by Bounds from DataNoDC
 * @params: DataNoDC Vector of double which holds the data
 * @params: Bounds Vector of double which holds the low bound and the up bound to detect outliers
 * @return: the signal with the interpolated outliers
*/
vector<double> InterpolateOutliers(vector<double> DataNoDC, vector<double> Bounds);

// Method that finds the quantiles
vector<double> Quantile(vector<double>& CalibNoDC);

// Quantile's helping function
double Lerp(double v0, double v1, double t);

/*
 * Removes the best straight-line fit from vector x and returns it in y
 * @params: the original signal and the sampling frequency
 * @return: the detrended signal
*/
vector<double> detrend(vector<double> original, const double fs);

/*
 * Compute the gain of a signal of double after amplification
 * @params: the original signal, the amplified signal
 * @return: the amplification gain
*/
double calculateGain(vector<double> const& original, vector<double> const& amplified);

// Method which replaces EEG values by nan values
vector<double> MBT_remove(vector<double> signalToRemove);

// Method which corrects artifacts
vector<double> MBT_correctArtifact(vector<double> signalToCorrect);

// Method which puts the outliers to nan
vector<double> MBT_OutliersToNan(vector<double> signal, vector<double> bounds);

// Method which interpolates values between channels
MBT_Matrix<float> MBT_InterpolateBetweenChannels(MBT_Matrix<float>);

// Method which interpolates values across channels
MBT_Matrix<float> MBT_InterpolateAcrossChannels(MBT_Matrix<float>);

// this function skips the possible NaN values that are present in a file
vector<float> SkipNaN(vector<float>);

// this function removes possible remaining nan values in the beginning or the end of the MBT_Matrix
MBT_Matrix<float> RemoveNaNIfAny(MBT_Matrix<float>);

#endif // MBT_PREPROCESSING_H
