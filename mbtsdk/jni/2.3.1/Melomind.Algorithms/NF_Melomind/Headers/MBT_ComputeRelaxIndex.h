//
//  MBT_ComputeRelaxIndex.h
//
//  Created by Fanny Grosselin on 06/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
//  Update: 2017/03/23 by Fanny Grosselin --> Change float by double for the functions not directly used by Andro�d.
//          Fanny Grosselin : 2017/09/05 Change the pathes.
//          Fanny Grosselin : 2017/12/05 Add histFreq (the vector containing the previous frequencies) in input.
//          Fanny Grosselin : 2017/12/07 Set in output a dictionnary.
//          Fanny Grosselin : 2017/12/14 Set in input error messages instead of a dictionnary of parameters from calibration.
//          Fanny Grosselin : 2017/01/24 Optimize the way we use histFreq.

#ifndef MBT_COMPUTERELAXINDEX_H_INCLUDED
#define MBT_COMPUTERELAXINDEX_H_INCLUDED

#include <stdio.h>
#include <iostream>
#include <vector>
#include <map>
#include <string>
#include <errno.h>
#include <math.h>
#include <limits>
#include <algorithm>
#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h"
#include "MBT_ComputeSNR.h"


/*
 * @brief Computes the relaxation index of the signal of the channel which had the best quality during the calibration. For now, only takes the first two channels into account.
          The relaxation index is computed each second with segments of 4s with a sliding window of 1s.
 * @param sessionPacket MBT_Matrix holding the signal.
 * @param errorHandle A vector holding error messages.
 * @param sampRate The signal sampling rate.
 * @param IAFinf Lower bound of the frequency range which will be used to compute SNR. For example IAFinf = 7 to compute SNR alpha.
 * @param IAFsup Upper bound of the frequency range which will be used to compute SNR. For example IAFsup = 13 to compute SNR alpha.
 * @param histFreq Vector containing the previous frequencies.
* @return A dictionnary with histFreq and the relax index value.
 */
float MBT_ComputeRelaxIndex(MBT_Matrix<float> sessionPacket,std::vector<float> errorMsg, const float sampRate, double IAFinf, double IAFsup, std::vector<float> &histFreq);


#endif // MBT_COMPUTERELAXINDEX_H_INCLUDED

