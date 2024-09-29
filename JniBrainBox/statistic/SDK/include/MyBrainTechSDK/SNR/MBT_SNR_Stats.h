//
// MBT_SNR_Stats.h
//
// Created by Katerina Pandremmenou on 21/04/2017
// Copyright (c) 2017 myBrain Technologies. All rights reserved.
// Update by Katerina Pandremmenou on 03/11/2017 --> Create new function for the calculation of the AreaDown
//                                              


#ifndef MBT_SNR_STATS_H
#define MBT_SNR_STATS_H

#include "Algebra/MBT_Operations.h"

#include <vector>
#include <iostream>
#include <algorithm>
#include <numeric> 
#include <math.h>
#include <map>

using namespace std;

class SNR_Statistics
{
	public: 
		SNR_Statistics(SP_FloatVector);
		~SNR_Statistics();
		// Handles all the calculation of the SNR statistics
		map<string, SP_FloatType> CalculateSNRStatistics(SP_FloatVector, SP_FloatType);

	private:
		vector<int> Representation;
		// these are the thresholds for the Level 1 of Exercise Switch
		SP_RealType HighThreshold, LowThreshold;
		// Calculates the number of switches when SNR is below the lowest threshold or above the higest threshold for at least three seconds
		int ExerciseSwitch(SP_FloatVector);
		//Calculates the sum of the area above a threshold
		pair<SP_FloatType,SP_FloatType> ExerciseAreaUpAndPerformance(SP_FloatVector, SP_RealType);
		// Calculates the sum of the area below a threshold
		SP_FloatType ExerciseAreaDown(SP_FloatVector, SP_RealType);
};

#endif // MBT_SNR_STATS_H
