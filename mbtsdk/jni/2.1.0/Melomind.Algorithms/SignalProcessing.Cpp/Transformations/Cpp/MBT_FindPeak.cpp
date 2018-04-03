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

#include "../Headers/MBT_FindPeak.h"

std::pair<int,int> MBT_frequencyBounds(std::vector<double> frequencies, const double IAFinf, const double IAFsup)
{
    std::pair<int,int> freqBounds;

    // Round frequencies
    std::vector<int> round_frequencies;
    round_frequencies.assign(frequencies.size(), 0);
    for (unsigned int f=0;f<frequencies.size();f++)
    {
        round_frequencies[f] = (int) round(frequencies[f]);
    }

    // Find the first index of the frequencies which is higher or equal to IAFinf
	std::vector<int>::iterator all_n_finf = std::find_if(round_frequencies.begin(),round_frequencies.end(),std::bind2nd(std::greater_equal<int>(),IAFinf)); // Fanny Grosselin 2017/02/16
    int n_finf = all_n_finf[0];

    // Find the last index of the frequencies which is lower or equal to IAFsup
	std::vector<int>::iterator all_n_fsup = std::find_if(round_frequencies.begin(),round_frequencies.end(),std::bind2nd(std::less_equal<int>(),IAFsup)); // Fanny Grosselin 2017/02/16
    int count_all_n_fsup = std::count_if (round_frequencies.begin(), round_frequencies.end(), std::bind2nd(std::less_equal<int>(),IAFsup));
    int n_fsup = all_n_fsup[count_all_n_fsup-1];

    freqBounds.first = n_finf;
    freqBounds.second = n_fsup;

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
