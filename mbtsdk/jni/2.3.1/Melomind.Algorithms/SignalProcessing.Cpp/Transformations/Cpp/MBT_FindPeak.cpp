//
//  MBT_FindPeak.cpp
//
//  Created by Fanny Grosselin on 04/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
//  Update: Fanny Grosselin 2017/02/16 --> Change the bounds (remove the +1 for the upper bound) of "find_if", "max_element" and "min_element".
// 	Update: Fanny Grosselin 23/03/2017 --> Change float by double
//  Update: Fanny Grosselin 2017/03/27 --> Fix all the warnings.
//  Update: Fanny Grosselin 2017/07/27 --> Change the bounds (add the +1 for the upper bound) of "MBT_valMaxPeak", "MBT_indMaxPeak" and "MBT_freqMaxPeak" because the upper bound is defined with ".begin + length" and not ".end".
//  Update: Katerina Pandremmenou 2017/09/20 --> Change all implicit type castings to explicit ones
//  Update: Fanny Grosselin 2017/11/30 --> Fix the error in MBT_frequencyBounds : we found the values of frequencies closest than IAFinf and IAFsup instead of finding the corresponding indexes.

#include "../Headers/MBT_FindPeak.h"

std::pair<int,int> MBT_frequencyBounds(std::vector<double> frequencies, const double IAFinf, const double IAFsup)
{
    std::pair<int,int> freqBounds;

    std::vector<int> bound_frequencies;
    for (unsigned int f=0;f<frequencies.size();f++)
    {
        if (frequencies[f]>=IAFinf && frequencies[f]<=IAFsup)
        {
            bound_frequencies.push_back(f);
        }
    }

    freqBounds.first = bound_frequencies[0]; //Find the first index of the frequencies which is higher or equal to IAFinf
    freqBounds.second = bound_frequencies[bound_frequencies.size()-1]; // Find the last index of the frequencies which is lower or equal to IAFsup

    return freqBounds;
}


double MBT_valMaxPeak(std::vector<double> spectre, const int n_finf, const int n_fsup)
{
	double valMaxPeak = *std::max_element(spectre.begin()+n_finf, spectre.begin()+n_fsup+1); // Fanny Grosselin 2017/02/16
    return valMaxPeak;
}


int MBT_indMaxPeak(std::vector<double> spectre, const int n_finf, const int n_fsup)
{
	int indMaxPeak = max_element(spectre.begin()+n_finf, spectre.begin()+n_fsup+1) - spectre.begin(); // Fanny Grosselin 2017/02/16
    return indMaxPeak;
}


double MBT_freqMaxPeak(std::vector<double> frequencies, std::vector<double> spectre, const int n_finf, const int n_fsup)
{
    // get the index of the maximum power between n_finf and n_fsup
	int indMaxPeak = max_element(spectre.begin()+n_finf, spectre.begin()+n_fsup+1) - spectre.begin(); // Fanny Grosselin 2017/02/16
    // get the frequency that corresponds to the maximum power
    double freqMaxPeak = frequencies[indMaxPeak];

	return freqMaxPeak;
}
