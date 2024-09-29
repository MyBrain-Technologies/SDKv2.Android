//
//  MBT_kmeans.h
//
//  Created by Katerina Pandremmenou on 1/03/2017.
//  Copyright (c) 2017 Katerina Pandremmenou. All rights reserved.
//  Inspired by: https://github.com/marcoscastro/kmeans
//  
//  This is a kmeans implementation that works for different number of clusters and multidimensional data
//  For the current implementation we treat with one-dimensional data 
//  The present kmeans implementation is exactly the same with the Matlab's one, using squared Euclidean distance
//  The difference is that we do not implement here the 'OnlinePhase' option of Matlab, meaning that there should be some slight differences in some cases 
//
// 	Update: Fanny Grosselin 23/03/2017 --> Change float to double
//
#ifndef __MBT_kmeans__
#define __MBT_kmeans__

#include <sp-global.h>

#include "DataManipulation/MBT_Matrix.h"

#include <vector>
#include <iostream>
#include <math.h>
#include <algorithm>

using namespace std;

class Point
{
	private:
		int id_point, id_cluster;
		SP_Vector values;
		int total_values;

	public:
		Point(int id_point, SP_Vector& values)
		{
			this->id_point = id_point;
			total_values = values.size();

			for(int i = 0; i < total_values; i++)
				this->values.push_back(values[i]);

			id_cluster = -1;
		}
		
		int getID();
		void setCluster(int);
		int getCluster();
		SP_RealType getValue(int);
		int getTotalValues();
		void addValue(SP_RealType);
};

class Cluster
{
	private:
		int id_cluster;
		SP_Vector central_values;
		vector<Point> points;

    public:
		Cluster(int id_cluster, Point point)
		{
			this->id_cluster = id_cluster;
		    int total_values = point.getTotalValues();

			for(int i = 0; i < total_values; i++)
				central_values.push_back(point.getValue(i));

			points.push_back(point);
		}
		
		void addPoint(Point);
		bool removePoint(int);
		SP_RealType getCentralValue(int);
        void setCentralValue(int, SP_RealType);
        Point getPoint(int);
        int getTotalPoints();
        int getID();
};

class KMeans
{
	private:
		int K, total_values, total_points, max_iterations;
		vector<Cluster> clusters;
		
		// return ID of nearest center (uses euclidean distance)
		int getIDNearestCenter(Point);

    public:
		KMeans(int K, int total_points, int total_values, int max_iterations)
		{
			this->K = K;
			this->total_points = total_points;
			this->total_values = total_values;
			this->max_iterations = max_iterations;
		}
		pair<vector<int>, SP_Vector> run(vector<Point> &, SP_Vector);
};

// this function is used to initiate the kmeans procedure
// it can be given in input either single or multidimensional data and can perform a clustering in as many clusters as we want
pair<vector<int>, SP_Vector> ApplyKMeans(SP_Vector ValuesToCluster);
        
#endif /* defined(__MBT_kmeans__) */


































