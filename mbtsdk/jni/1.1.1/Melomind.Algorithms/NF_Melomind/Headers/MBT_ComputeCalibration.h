//
//  MBT_ComputeCalibration.h
//
//  Created by Fanny Grosselin on 04/01/17.
//  Inspired by MBT_ComputeCalibration.h of Emma Barme on 20/10/2015.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
//  Update: Fanny Grosselin : 2017/02/03 Add in inputs and for MBT_ComputeSNR, the Bounds to detect the outliers of the signal.
//          Fanny Grosselin : 2017/09/05 Change the pathes.

#ifndef MBT_COMPUTECALIBRATION_H_INCLUDED
#define MBT_COMPUTECALIBRATION_H_INCLUDED

#include <iostream>
#include <stdio.h>
#include <map>
#include <string>
#include <errno.h>
#include "MBT_ComputeSNR.h"
#include "MBT_SmoothRelaxIndex.h"
#include <limits>
#include "../../SignalProcessing.Cpp/Transformations/Headers/MBT_PWelchComputer.h"
#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h"

//Calibration process generating the parameters necessary for the computation of the relaxation index. For now, the calibration is only taking the first two channels into account.

/*
 * @brief Takes the data from the calibration recordings and compute the necessary parameters that is to say the channel
          with the best quality (in a vector named calibrationParameters["BestChannel"]) and a vector containing the Smoothed SNR values from
          the best channel of the calibration, stored in a vector named calibrationParameters["SNRCalib_ofBestChannel"]. The Smoothed SNR values
          are computed each second with segments of 4s with a sliding window of 1s.
 * @param calibrationRecordings A matrix holding the concatenation of the calibration recordings, one channel per row. (No GPIOs)
 * @param calibrationRecordingsQuality A matrix holding the quality values, one channel per row, in the same order as in the matrix. (No GPIOs) Each quality value is between 0 and 1, and is the quality for a packet.
 * @param sampRate The signal sampling rate.
 * @param packetLength The number of data points in a packet.
 * @param IAFinf Lower bound of the frequency range which will be used to compute SNR. For example IAFinf = 7 to compute SNR alpha.
 * @param IAFsup Upper bound of the frequency range which will be used to compute SNR. For example IAFsup = 13 to compute SNR alpha.
 * @param Bounds A MBT_Matrix of float which holds the thresholds (low and up) to detect the outliers of each channel of the signal. Number of row = number of channel.
 * @return A dictionnary with the value for the various parameters.
 * @todo Use an enum for the output parameters?
 * @warning Works with any number of channels, but only keeps the best channel.
 */
std::map<std::string, std::vector<float> > MBT_ComputeCalibration(MBT_Matrix<float> calibrationRecordings, MBT_Matrix<float> calibrationRecordingsQuality, const float sampRate, const int packetLength, const float IAFinf, const float IAFsup, MBT_Matrix<float> Bounds);


#endif // MBT_COMPUTECALIBRATION_H_INCLUDED
