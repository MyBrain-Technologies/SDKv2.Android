//
//  MBT_ComputeSNR.h
//
//  Created by Fanny Grosselin on 03/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
//  Update: Fanny Grosselin : 2017/02/03 Add in inputs, the Bounds to detect the outliers of the signal.
//          Fanny Grosselin : 2017/03/21 Use MBT_BandPass_fftw3 instead of MBT_BandPass.
//          Fanny Grosselin : 2017/03/23 Change float by double.
//          Fanny Grosselin : 2017/09/05 Change the pathes.
//          Fanny Grosselin : 2017/09/18 Change the input of MBT_ComputeSNR function.

#ifndef MBT_COMPUTESNR_H_INCLUDED
#define MBT_COMPUTESNR_H_INCLUDED

#include <stdio.h>
#include <iostream>
#include <vector>
#include <iterator>
#include <algorithm>
#include <errno.h>
#include <limits>
#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h"
#include "../../SignalProcessing.Cpp/Transformations/Headers/MBT_PWelchComputer.h"
#include "../../SignalProcessing.Cpp/Transformations/Headers/MBT_FindPeak.h"
#include "../../SignalProcessing.Cpp/Algebra/Headers/MBT_FindClosest.h"
#include "../../SignalProcessing.Cpp/Algebra/Headers/MBT_Interpolation.h"
#include "../../SignalProcessing.Cpp/PreProcessing/Headers/MBT_PreProcessing.h"
#include "../../SignalProcessing.Cpp/PreProcessing/Headers/MBT_BandPass_fftw3.h"
#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_ReadInputOrWriteOutput.h"



/*
 * @brief Compute the SNR in a specific frequency band thanks to a linear interpolation of the noise.
 *        The SNR is computed each second with segments of 4s with a sliding window of 1s.
 * @param signal The matrix holding the EEG values. These signals should be preprocessed before using (DC removal, notch, bandpass, outliers removal).
 * @param sampRate The sample rate.
 * @param IAFinf Lower bound of the frequency range which will be used to compute SNR. For example IAFinf = 7 to compute SNR alpha.
 * @param IAFsup Upper bound of the frequency range which will be used to compute SNR. For example IAFsup = 13 to compute SNR alpha.
 * @return The vector containing one SNR value by channel.
 */
std::vector<double> MBT_ComputeSNR(MBT_Matrix<double> const signal, const double sampRate, const double IAFinf, const double IAFsup);


#endif // MBT_COMPUTESNR_H_INCLUDED
