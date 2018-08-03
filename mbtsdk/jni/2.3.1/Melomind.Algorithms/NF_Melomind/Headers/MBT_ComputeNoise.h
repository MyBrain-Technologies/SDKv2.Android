//
//  MBT_ComputeNoise.h
//
//  Created by Fanny Grosselin on 01/12/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//

#ifndef MBT_COMPUTENOISE_H_INCLUDED
#define MBT_COMPUTENOISE_H_INCLUDED

#include <stdio.h>
#include <iostream>
#include <vector>
#include <iterator>
#include <algorithm>
#include <errno.h>
#include <limits>
#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h"
#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_ReadInputOrWriteOutput.h"



/*
 * @brief Estimate the background spectral noise by an iterative regression.
 * @param trunc_frequencies The vector that contains the frequency values of the spectrum.
 * @param trunc_channelPSD Te vector containing the power value of the spectrum (not in log scale).
 * @return The vector containing the estimated power values in log scale of the background estimated noise.
 */
std::vector<double> MBT_ComputeNoise(std::vector<double> trunc_frequencies, std::vector<double> trunc_channelPSD);


#endif // MBT_COMPUTENOISE_H_INCLUDED
