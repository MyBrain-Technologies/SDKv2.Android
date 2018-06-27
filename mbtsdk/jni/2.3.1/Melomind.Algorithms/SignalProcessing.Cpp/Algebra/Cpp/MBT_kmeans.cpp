//
// MBT_kmeans.cpp
//
// Created by Katerina Pandremmenou on 1/03/2017
// Copyright (c) 2017 myBrain Technologies. All rights reserved.
// Inspired by: https://github.com/marcoscastro/kmeans
//
// Update: Fanny Grosselin 23/03/2017 --> Change float to double
//

#include "../../DataManipulation/Headers/MBT_Matrix.h"
#include "../Headers/MBT_kmeans.h"
//#include <time.h>

using namespace std;

// class Point
int Point::getID()
{
	return id_point;
}

void Point::setCluster(int id_cluster)
{
	this->id_cluster = id_cluster;
}

int Point::getCluster()
{
	return id_cluster;
}

double Point::getValue(int index)
{
	return values[index];
}

int Point::getTotalValues()
{
	return total_values;
}

void Point::addValue(double value)
{
	values.push_back(value);
}

// class Cluster
void Cluster::addPoint(Point point)
{
	points.push_back(point);
}

bool Cluster::removePoint(int id_point)
{
	int total_points = points.size();
	for(int i = 0; i < total_points; i++)
	{
		if(points[i].getID() == id_point)
		{
			points.erase(points.begin() + i);
			return true;
		}
	}
	return false;
}

double Cluster::getCentralValue(int index)
{
	return central_values[index];
}

void Cluster::setCentralValue(int index, double value)
{
	central_values[index] = value;
}

Point Cluster::getPoint(int index)
{
	return points[index];
}

int Cluster::getTotalPoints()
{
	return points.size();
}

int Cluster::getID()
{
	return id_cluster;
}

// class KMeans

// return ID of nearest center (uses squared euclidean distance)
int KMeans::getIDNearestCenter(Point point)
{
	double sum = 0.0, min_dist;
	int id_cluster_center = 0;

	for(int i = 0; i < total_values; i++)
	{
		sum += pow(clusters[0].getCentralValue(i) -
				   point.getValue(i), 2.0);
	}

	min_dist = sqrt(sum);

	for(int i = 1; i < K; i++)
	{
		double dist;
		sum = 0.0;

		for(int j = 0; j < total_values; j++)
		{
			sum += pow(clusters[i].getCentralValue(j) -
					   point.getValue(j), 2.0);
		}

		dist = sqrt(sum);

		if(dist < min_dist)
		{
			min_dist = dist;
			id_cluster_center = i;
		}
	}

	return id_cluster_center;
}

pair<vector<int>, vector<double>> KMeans::run(vector<Point> & points, vector<double> valuesToCluster)
{
    vector<int> Out(1);
	vector<double> c(1);
	
	if(K > total_points)
	{
		cout << "The number of clusters is higher than the number of data points" << endl;
		return pair<vector<int>, vector<double>> (Out, c);
	}
	
	vector<int> prohibited_indexes;
    
	// choose two centers for the clustering on the distances
	// we choose the min and max distances
	vector<double>::iterator p1 = min_element(begin(valuesToCluster), end(valuesToCluster));
	vector<double>::iterator p2 = max_element(begin(valuesToCluster), end(valuesToCluster));
	
	auto index1 = distance(begin(valuesToCluster), p1);
	auto index2 = distance(begin(valuesToCluster), p2);
	
	// we force the centers to correspond to the min and max distances
	vector<int> initialCenters_positions = {(int)index1, (int)index2};
	
	for(int i = 0; i < K; i++)
	{
		while(true)
		{
	        // int index_point = rand() % total_points;
			int index_point = initialCenters_positions[i];

			if(find(prohibited_indexes.begin(), prohibited_indexes.end(),
					index_point) == prohibited_indexes.end())
			{
				prohibited_indexes.push_back(index_point);
				points[index_point].setCluster(i);
				Cluster cluster(i, points[index_point]);
				clusters.push_back(cluster);
				break;
			}
		}
	}

	int iter = 1;

	while(true)
	{
		bool done = true;

		// associates each point to the nearest center
		for(int i = 0; i < total_points; i++)
		{
			int id_old_cluster = points[i].getCluster();
			int id_nearest_center = getIDNearestCenter(points[i]);

			if(id_old_cluster != id_nearest_center)
			{
				if(id_old_cluster != -1)
					clusters[id_old_cluster].removePoint(points[i].getID());

				points[i].setCluster(id_nearest_center);
				clusters[id_nearest_center].addPoint(points[i]);
				done = false;
			}
		}

		// recalculating the center of each cluster
		for(int i = 0; i < K; i++)
		{
			for(int j = 0; j < total_values; j++)
			{
				int total_points_cluster = clusters[i].getTotalPoints();
				double sum = 0.0;

				if(total_points_cluster > 0)
				{
					for(int p = 0; p < total_points_cluster; p++)
						sum += clusters[i].getPoint(p).getValue(j);
					clusters[i].setCentralValue(j, sum / total_points_cluster);
				}
			}
		}

		if(done == true || iter >= max_iterations)
		{
		//	cout << "Break in iteration " << iter << "\n\n";
			break;
		}

		iter++;
	}

    int all_cluster_points = 0;
    // shows elements of clusters
	for(int i = 0; i < K; i++)
	{
		all_cluster_points +=  clusters[i].getTotalPoints();
	}
	
	// re-initialize the size of Out (Index NUmbers) and c (Centroid values)
	Out.resize(all_cluster_points);
	c.resize(0);
	
	// shows elements of clusters
	for(int i = 0; i < K; i++)
	{
		int total_points_cluster =  clusters[i].getTotalPoints();

		//cout << "Cluster " << clusters[i].getID() + 1 << endl;
		for(int j = 0; j < total_points_cluster; j++)
		{
		    // the points indices
			int PointIndex = clusters[i].getPoint(j).getID();
			// assign a label to each data point
			Out[PointIndex] = i+1;
			
			// the points values
			//for(int p = 0; p < total_values; p++)
			//	cout << clusters[i].getPoint(j).getValue(p) << " ";
		}

		for(int j = 0; j < total_values; j++)
		{
        	double centroid = clusters[i].getCentralValue(j);
        	c.push_back(centroid);
        }
	}
	
	return pair<vector<int>, vector<double>> (Out, c);
}

// in our case we provide an 1-dimensional data (distances)
pair<vector<int>, vector<double>> ApplyKMeans(vector<double> ValuesToCluster)
{
    //srand (time(NULL));
    
    // number of clusters
	int K = 2; 
	// 1-d data
	int total_values = 1; 
	// maximum allowed number of iterations
	int max_iterations = 100; 
	// the number of values to be clustered    
    int total_points = ValuesToCluster.size();
     
    //vector<double> values;
    vector<Point> points;
    
    for (int i = 0 ; i < total_points; i++)
    {    
        vector<double> values;
        values.push_back(ValuesToCluster[i]);
    	Point p (i, values);
    	points.push_back(p);
    }
    
	KMeans kmeans(K, total_points, total_values, max_iterations);
	pair<vector<int>, vector<double>> result = kmeans.run(points, ValuesToCluster);
	
	return pair<vector<int>, vector<double>> (result.first, result.second);
}


