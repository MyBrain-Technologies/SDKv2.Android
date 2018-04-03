//
// MBT_SNR_Stats.h
//
// Created by Katerina Pandremmenou on 21/04/2017
// Copyright (c) 2017 myBrain Technologies. All rights reserved.
// Update by Katerina Pandremmenou on 03/11/2017 --> Create new function for the calculation of the AreaDown
//                                              


#ifndef MBT_SNR_STATS_H
#define MBT_SNR_STATS_H

#include <vector>
#include <iostream>
#include <algorithm>
#include <numeric> 
#include <math.h>
#include <map>
#include "../../../SignalProcessing.Cpp/Algebra/Headers/MBT_Operations.h"

using namespace std;

class SNR_Statistics
{
	public: 
		SNR_Statistics(vector<float>);
		~SNR_Statistics();
		// Handles all the calculation of the SNR statistics
		map<string, float> CalculateSNRStatistics(vector<float>, float);

	private:
		vector<int> Representation;
		// these are the thresholds for the Level 1 of Exercise Switch
		double HighThreshold, LowThreshold;
		// Calculates the number of switches when SNR is below the lowest threshold or above the higest threshold for at least three seconds
		int ExerciseSwitch(vector<float>);
		//Calculates the sum of the area above a threshold
		pair<float,float> ExerciseAreaUpAndPerformance(vector<float>, double);
		// Calculates the sum of the area below a threshold
		float ExerciseAreaDown(vector<float>, double);
};

#endif // MBT_SNR_STATS_H
