//
// MBT_TF_Stats.h
//
// Created by Katerina Pandremmenou on 30/01/2017
// Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
// Update on 03/04/2017 by Katerina Pandremmenou: Convert everything from float to double

#ifndef MBT_TF_STATS_H
#define MBT_TF_STATS_H

#include "../../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h"
#include "../../../SignalProcessing.Cpp/PreProcessing/Headers/MBT_BandPass_fftw3.h"
#include "./MBT_TF_map.h"

using namespace std;

typedef vector<vector<double>> VVDouble;
pair<VVDouble, vector<VVDouble>> CalculateStatistics(MBT_Matrix<double>, MBT_Matrix<int>, MBT_Matrix<double>);

class TF_Statistics
{
	public: 
		TF_Statistics();
		~TF_Statistics();
		
		// this function sorts the clusters from MinX to MaxX. Actually it reorders the clusters based on the one that is found first in time (x-axis)
	    pair<MBT_Matrix<double>, MBT_Matrix<int>> SortClustersFromMinXToMaxX(MBT_Matrix<double>, MBT_Matrix<int>);
	    
	    // finds the edge points and removes them from the clusters
	    // also, updates the bounds of the new clusters, after the removal of some points
	    pair<MBT_Matrix<double>, MBT_Matrix<int>> FindEdgePoints(MBT_Matrix<double>&, MBT_Matrix<double>, MBT_Matrix<int>);
	    
	    // calculates the area and the gravity center ofeach cluster
	    pair<vector<double>, MBT_Matrix<double>> CalculateAreaAndGC(MBT_Matrix<double>, MBT_Matrix<int>);
	    pair<vector<double>, MBT_Matrix<double>> CalculateAreaAndGC(VVDouble, VVDouble);
	    
	    // calculates the min and max values in x and y axis
	    pair<MBT_Matrix<double>, MBT_Matrix<double>> CalculateMinMax(MBT_Matrix<double>, MBT_Matrix<int>);
	    pair<MBT_Matrix<double>, MBT_Matrix<double>> CalculateMinMax(VVDouble, VVDouble);
	    
	    // calculates the timelengths of the clusters
	    vector<double> CalculateTimeLength(MBT_Matrix<double>);
	    
	    // finds the indices of the clusters that do not close
	    vector<int> findOpenClusters(MBT_Matrix<double>);
	    	    
	    // updates the bounds to merge the clusters that do not close
	    pair<MBT_Matrix<int>, MBT_Matrix<double>> UpdateBoundsToMergeClusters(vector<int>, MBT_Matrix<int>, MBT_Matrix<double>);

	    // assigns the full array values to two 2d vectors, one for the xcoord and one for the ycoord
	    pair<VVDouble,VVDouble> AssignClusterValuesToVectors(MBT_Matrix<int>, MBT_Matrix<double>);

	    // joins the clusters that do not close and performs fliplr if needed
	    pair<VVDouble, VVDouble> JoinOpenClusters(vector<int>, VVDouble, VVDouble);
	    
	    // we check for continuously (or not) fully overlapping clusters and update the statistics accordingly
	    pair<VVDouble, vector<VVDouble>> FixFullyOverlappingClusters(VVDouble, VVDouble, MBT_Matrix<double>, MBT_Matrix<double>, vector<double>, MBT_Matrix<double>, vector<double>);

	    // we check for continuously (or not) partially overlapping clusters and update the statistics accordingly
	    pair<VVDouble, vector<VVDouble>> FixPartiallyOverlappingClusters(VVDouble, vector<VVDouble>);

	    
	private:
		// calculates the last edge effect from the beginning and the first edge effect from the end, for all the frequencies
	    MBT_Matrix<int> FindEdgeBounds(MBT_Matrix<double>);
	    
	    // updates the bounds after removing the edge points
	    MBT_Matrix<int> UpdateBounds(MBT_Matrix<int>, vector<int>);
	    const int constant = 6;
	   
	    // calculates the distances between the clusters
	    vector<double> CalculateDistances(VVDouble);
	    
};

#endif // MBT_TF_STATS_H
