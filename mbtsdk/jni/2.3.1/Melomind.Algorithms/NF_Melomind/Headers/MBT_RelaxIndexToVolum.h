//
//  MBT_SmoothRelaxIndex.cpp
//
//  Created by Fanny Grosselin on 06/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
// Update:
//           Fanny Grosselin on 10/10/2017 --> Add one input parameter to have the Smoothed SNR values from calibration.
//           Fanny Grosselin on 18/12/2017 --> Change one input by another (the map of parameters from calibration to the vector of relax index from calibration)

#ifndef MBT_RELAXINDEXTOVOLUM_H_INCLUDED
#define MBT_RELAXINDEXTOVOLUM_H_INCLUDED

#include <errno.h>
#include <stdio.h>
#include <map>
#include <string>
#include <math.h>
#include <iostream>
#include <limits>
#include <vector>
#include "../../SignalProcessing.Cpp/PreProcessing/Headers/MBT_PreProcessing.h"


/*
 * @brief Transform the normalized smoothed relaxation index of the signal into volum value. For now, only takes only one value by one value into account.
 *        Only one volum value by second but this is computed on a signal of 4s with a sliding window of 1s.
 * @param smoothedRelaxIndex Float holding the normalized smoothed relaxation index.
 * @param SNRCalib A vector holding the relax index from calibration.
 * @return The volum value which corresponds to the normalized smoothed relaxation index value.
 */
float MBT_RelaxIndexToVolum(const float smoothedRelaxIndex, std::vector<float> SNRCalib);


#endif // MBT_RELAXINDEXTOVOLUM_H_INCLUDED
