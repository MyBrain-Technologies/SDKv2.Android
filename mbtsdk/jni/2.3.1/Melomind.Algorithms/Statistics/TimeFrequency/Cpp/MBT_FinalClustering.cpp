//
// MBT_FinalClustering.cpp
//
// Created by Katerina Pandremmenou on 16/03/2017
// Copyright (c) 2017 myBrain Technologies. All rights reserved.
// Update on 03/04/2017 by Katerina Pandremmenou (Convert everything from float to double)
// Update on 05/04/2017 by Katerina Pandremmenou (Fix all the warnings)

#include "../Headers/MBT_FinalClustering.h"
using namespace std;


GROUP_STATISTICS::GROUP_STATISTICS()
{
	;
}

GROUP_STATISTICS::~GROUP_STATISTICS()
{
	;
}

vector<int> GROUP_STATISTICS::FindClusterTransition(vector<int> KmeansLabels)
{
	vector<int> Trans;
    vector<int>::iterator it = KmeansLabels.begin();

    while ((it = find(it, KmeansLabels.end(), 2)) != KmeansLabels.end())
    { 
    	Trans.push_back(distance(KmeansLabels.begin(), it)); 
    	it++; 
    } 
	return Trans;
}

MBT_Matrix<double> GROUP_STATISTICS::FinalClustering(vector<double> Areas, MBT_Matrix<double> MinMaxX, vector<int> Transitions, const double Fs)
{
	vector<double> FurtherClusteredAreas, FurtherClusteredTimeLengths, FurtherClusteredDistances, MiddleMinX, MiddleMaxX;
	double FirstArea = 0, MiddleArea = 0, LastArea = 0;
	double FirstTimeLength = 0, MiddleTimeLength = 0, LastTimeLength = 0;
	double MinXFirstCluster, MaxXFirstCluster = 0, MinXLastCluster = 0, MaxXLastCluster = 0, FirstDist = 0;
	unsigned int k;

    // for the first cluster until the first transition to the next cluster
	if (Transitions[0] != 0)
	{
		FirstTimeLength = MinMaxX(1,Transitions[0]-1) - MinMaxX(0,0);
		MinXFirstCluster = MinMaxX(0,0);
		MaxXFirstCluster = MinMaxX(1,Transitions[0]-1);
		for (int h = 0; h < Transitions[0]; h++)
			FirstArea = FirstArea + Areas[h];	
	}

    
	for (k = 0 ; k < Transitions.size(); k++)
	{
		// for the last cluster
		if (k == Transitions.size()-1)
		{
			LastTimeLength = MinMaxX(1, MinMaxX.size().second-1) - MinMaxX(0, Transitions[k]);
			MinXLastCluster = MinMaxX(0, Transitions[k]);
			MaxXLastCluster = MinMaxX(1, MinMaxX.size().second-1);
			for (unsigned int j = Transitions[k]; j < Areas.size(); j++)
				LastArea = LastArea + Areas[j];
		}
		else
		// for the clusters in the middle
		{
			MiddleTimeLength = MinMaxX(1, Transitions[k+1]-1) - MinMaxX(0, Transitions[k]);
			FurtherClusteredTimeLengths.push_back(MiddleTimeLength);
			MiddleMinX.push_back(MinMaxX(0, Transitions[k]));
			MiddleMaxX.push_back(MinMaxX(1, Transitions[k+1]-1));

			for (int j = Transitions[k]; j < Transitions[k+1]; j++)
				MiddleArea = MiddleArea + Areas[j];

			FurtherClusteredAreas.push_back(MiddleArea);
			MiddleArea = 0;
		}
		MiddleTimeLength = 0;
	}

	if (Transitions[0] != 0)
	// put all the results in one vector
	{
		FurtherClusteredAreas.insert(FurtherClusteredAreas.begin(), FirstArea);
		FurtherClusteredAreas.push_back(LastArea);

		FurtherClusteredTimeLengths.insert(FurtherClusteredTimeLengths.begin(), FirstTimeLength);
		FurtherClusteredTimeLengths.push_back(LastTimeLength);

		MiddleMinX.insert(MiddleMinX.begin(), MinXFirstCluster);
		MiddleMinX.push_back(MinXLastCluster);

		MiddleMaxX.insert(MiddleMaxX.begin(), MaxXFirstCluster);
		MiddleMaxX.push_back(MaxXLastCluster);
	}
	// normally we never go into this condition
	else
	{
		FurtherClusteredAreas.push_back(LastArea);
		FurtherClusteredTimeLengths.push_back(LastTimeLength);
		MiddleMinX.push_back(MinXLastCluster);				
		MiddleMaxX.push_back(MaxXLastCluster);
	}

    // the distances are given in seconds
	FirstDist = abs(1 - MiddleMinX[0]);

	if (MiddleMaxX.size()!=1)
	{
		for (unsigned int k = 0; k < MiddleMaxX.size()-1; k++)
			FurtherClusteredDistances.push_back(abs(MiddleMaxX[k] - MiddleMinX[k+1]));
	}
	
	FurtherClusteredDistances.insert(FurtherClusteredDistances.begin(), FirstDist);

	MBT_Matrix<double> ClusteredResults(3, FurtherClusteredAreas.size());

    // check if all of the vectors of areas, timelengths, distances have the same size
	if((FurtherClusteredAreas.size() != FurtherClusteredDistances.size()) || (FurtherClusteredDistances.size() != FurtherClusteredTimeLengths.size()))
	{
		cout << "The lengths of the vectors Areas, Distances and Timelengths are not the same" << endl;
	    abort();
	}
	// put each of the vectors in an MBT_Matrix, with the following order: areas, timelengths, distances
	else
    {
		for (unsigned int k = 0 ; k < FurtherClusteredAreas.size(); k++)
		{
			ClusteredResults(0, k) = FurtherClusteredAreas[k];
			ClusteredResults(1, k) = FurtherClusteredTimeLengths[k] / Fs;
			ClusteredResults(2, k) = FurtherClusteredDistances[k] / Fs;
		}
	}

	return ClusteredResults;
}

MBT_Matrix<double> FurtherGroupStatistics(vector<double> Areas, MBT_Matrix<double> MinMaxX, vector<int> KmeansLabels, const double Fs)
{
	MBT_Matrix<double> FinalResults;
	vector<int> Transitions;

    // create an object to call the functions for finding the transitions and further grouping the results
	GROUP_STATISTICS obj;
	Transitions = obj.FindClusterTransition(KmeansLabels);
	FinalResults = obj.FinalClustering(Areas, MinMaxX, Transitions, Fs);

	return FinalResults;
}
