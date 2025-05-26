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

#include <sp-global.h>

#include "Algebra/MBT_Interpolation.h"
#include "DataManipulation/MBT_Matrix.h"

#include <vector>
#include <algorithm>
#include <functional>
#include <iostream>
#include "math.h" // for nan()

using namespace std;

// Method which removes the DC offset
#ifndef SP_FLOAT_OR_NOT_LEGACY
SP_FloatVector  RemoveDC(SP_FloatVector Data);
#endif
SP_Vector RemoveDC(SP_Vector Data);
// takes float in input and returns double in output
SP_Vector RemoveDCF2D(SP_FloatVector Data);

// Methods for calculating the lower and upper bound
// for floats
#ifndef SP_FLOAT_OR_NOT_LEGACY
SP_FloatVector CalculateBounds(SP_FloatVector DataNoDC);
#endif
// for doubles
SP_Vector CalculateBounds(SP_Vector DataNoDC);

// Method that removes the outliers
SP_Vector RemoveOutliers(SP_Vector DataNoDC, SP_Vector Bounds);

// Method that interpolates the outliers  // Fanny Grosselin 2017/02/03
/*
 * Interpolate the outliers detected by Bounds from DataNoDC
 * @params: DataNoDC Vector of double which holds the data
 * @params: Bounds Vector of double which holds the low bound and the up bound to detect outliers
 * @return: the signal with the interpolated outliers
*/
SP_Vector InterpolateOutliers(SP_Vector DataNoDC, SP_Vector Bounds);

// Method that finds the quantiles
SP_Vector Quantile(SP_Vector& CalibNoDC);

// Quantile's helping function
SP_RealType Lerp(SP_RealType v0, SP_RealType v1, SP_RealType t);

/*
 * Removes the best straight-line fit from vector x and returns it in y
 * @params: the original signal and the sampling frequency
 * @return: the detrended signal
*/
SP_Vector detrend(SP_Vector original, const SP_RealType fs);

/*
 * Compute the gain of a signal of double after amplification
 * @params: the original signal, the amplified signal
 * @return: the amplification gain
*/
SP_RealType calculateGain(SP_Vector const& original, SP_Vector const& amplified);

// Method which replaces EEG values by nan values
SP_Vector MBT_remove(SP_Vector signalToRemove);

// Method which corrects artifacts
SP_Vector MBT_correctArtifact(SP_Vector signalToCorrect);

// Method which puts the outliers to nan
SP_Vector MBT_OutliersToNan(SP_Vector signal, SP_Vector bounds);

// Method which interpolates values between channels
SP_FloatMatrix MBT_InterpolateBetweenChannels(SP_FloatMatrix);

// Method which interpolates values across channels
SP_FloatMatrix MBT_InterpolateAcrossChannels(SP_FloatMatrix);

// this function skips the possible NaN values that are present in a file
SP_FloatVector SkipNaN(SP_FloatVector);

// this function removes possible remaining nan values in the beginning or the end of the MBT_Matrix
SP_FloatMatrix RemoveNaNIfAny(SP_FloatMatrix);

#endif // MBT_PREPROCESSING_H
