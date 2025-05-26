//
//  MBT_FindPeak.h
//
//  Created by Fanny Grosselin on 04/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
// 	Update: Fanny Grosselin 23/03/2017 --> Change float by double
#ifndef MBT_FINDPEAK_H_INCLUDED
#define MBT_FINDPEAK_H_INCLUDED

#include <sp-global.h>

#include <utility>
#include <stdio.h>
#include <iostream>
#include <vector>
#include <math.h>
#include <iterator>
#include <algorithm>

/*
 * @brief Find the indexes of the frequencies which are close to IAFinf and IAFsup.
 * @param frequencies A vector of double containing the values of the frequencies.
 * @param IAFinf The lower bound of the range of frequencies we want to study.
 * @param IAFsup The upper bound of the range of frequencies we want to study.
 * @return A pair of int which corresponds to the indexes of the frequency bounds.
 */
std::pair<int,int> MBT_frequencyBounds(SP_Vector frequencies, const SP_RealType IAFinf, const SP_RealType IAFsup);

/*
 * @brief Find the maximum value of the spectrum in a specific frequency range.
 * @param spectre A vector of double containing the values of the spectrum.
 * @param n_finf The index of the lower bound of the range of frequencies we want to study.
 * @param n_fsup The index of the upper bound of the range of frequencies we want to study.
 * @return The value of the peak in the spectrum in a frequency range specified by n_finf and n_fsup.
 */
 // Note: n_finf and n_fsup are the output of MBT_frequencyBounds.
SP_RealType MBT_valMaxPeak(SP_Vector spectre, const int n_finf, const int n_fsup);

/*
 * @brief Find the index of the maximum value of the spectrum in a specific frequency range.
 * @param spectre A vector of double containing the values of the spectrum.
 * @param n_finf The index of the lower bound of the range of frequencies we want to study.
 * @param n_fsup The index of the upper bound of the range of frequencies we want to study.
 * @return The index of the peak in the spectrum in a frequency range specified by n_finf and n_fsup.
 */
 // Note: n_finf and n_fsup are the output of MBT_frequencyBounds.
int MBT_indMaxPeak(SP_Vector spectre, const int n_finf, const int n_fsup);

/*
 * @brief Find the frequency of the peak into the spectrum in a specific frequency range.
 * @param frequencies A vector of double containing the values of the frequencies.
 * @param spectre A vector of double containing the values of the spectrum.
 * @param n_finf The index of the lower bound of the range of frequencies we want to study.
 * @param n_fsup The index of the upper bound of the range of frequencies we want to study.
 * @return The frequency of the peak into the spectrum in a frequency range specified by n_finf and n_fsup.
 */
 // Note: n_finf and n_fsup are the output of MBT_frequencyBounds.
 // WARNING : Maybe you need an int for the frequency. In this case, you should round the double value.
SP_RealType MBT_freqMaxPeak(SP_Vector frequencies, SP_Vector spectre, const int n_finf, const int n_fsup);

#endif // MBT_FINDPEAK_H_INCLUDED
