//
//  MBT_NormalizeCalibration.h
//
//  Created by Fanny Grosselin on 16/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
//  Update: Fanny Grosselin : 2017/09/05 Change the pathes.


#ifndef MBT_NORMALIZERELAXINDEX_H_INCLUDED
#define MBT_NORMALIZERELAXINDEX_H_INCLUDED

#include <vector>
#include <errno.h>
#include <limits>
#include "../../SignalProcessing.Cpp/Algebra/Headers/MBT_Operations.h"


/*
 * @brief Takes a smoothed SNR value from the session recordings and normalize it with the mean and the standard deviation
 *        of the smoothed SNR values from the calibration. Only one normalized smoothed relaxation index by second but this is
 *        computed on a signal of 4s with a sliding window of 1s.
 * @param relaxIndexSession A float holding a smoothed SNR value from the session.
 * @param relaxIndexCalibration A vector holding the smoothed SNR values from the calibration.
 * @return The smoothed SNR value from the session.
 */
float MBT_NormalizeRelaxIndex(float relaxIndexSession, std::vector<float> relaxIndexCalibration);

#endif // MBT_NORMALIZERELAXINDEX_H_INCLUDED
