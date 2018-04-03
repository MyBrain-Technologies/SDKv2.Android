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
//          Fanny Grosselin : 2017/12/01 Add the path to include MBT_Operations.h
//          Fanny Grosselin : 2017/12/01 Add the path to include MBT_ComputeNoise.h
//          Fanny Grosselin : 2017/12/05 Add histFreq (the vector containing the previous frequencies) in input.
//          Fanny Grosselin : 2017/12/06 Change the output of the function by a dictionnary.
//          Fanny Grosselin : 2017/01/24 Optimize the way we use histFreq.

#ifndef MBT_COMPUTESNR_H_INCLUDED
#define MBT_COMPUTESNR_H_INCLUDED

#include <stdio.h>
#include <iostream>
#include <vector>
#include <iterator>
#include <algorithm>
#include <errno.h>
#include <limits>
#include <map>
#include "MBT_ComputeNoise.h"
#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h"
#include "../../SignalProcessing.Cpp/Transformations/Headers/MBT_PWelchComputer.h"
#include "../../SignalProcessing.Cpp/Transformations/Headers/MBT_FindPeak.h"
#include "../../SignalProcessing.Cpp/Algebra/Headers/MBT_FindClosest.h"
#include "../../SignalProcessing.Cpp/Algebra/Headers/MBT_Operations.h"
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
 * @param histFreq Vector containing the previous frequencies.
 * @return A dictionnary containing one SNR value by channel, one quality value of the relax index for each channel and the updated vector histFreq.
 */
std::map<std::string, std::vector<double> >  MBT_ComputeSNR(MBT_Matrix<double> const signal, const double sampRate, const double IAFinf, const double IAFsup, std::vector<float> &histFreq);


#endif // MBT_COMPUTESNR_H_INCLUDED
