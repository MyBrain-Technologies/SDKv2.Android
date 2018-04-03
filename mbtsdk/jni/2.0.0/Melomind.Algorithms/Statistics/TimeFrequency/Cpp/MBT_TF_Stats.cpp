//
// MBT_TF_Stats.cpp
//
// Created by Katerina Pandremmenou on 30/01/2017
// Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
// Update on 03/04/2017 by Katerina Pandremmenou: Convert everything from float to double
// Update on 05/04/2017 by Katerina Pandremmenou (Fix all the warnings)

#include <iostream>
#include <algorithm>
#include <vector>
#include <functional>
#include "../Headers/MBT_TF_Stats.h"

using namespace std;
vector<int> IndicesOfSorting;
vector<int> delPosition;
vector<int> SkippedClusters;
const int LowestFreq = 1, HighestFreq = 5;
const int DefaultNegative = -1E3;

TF_Statistics::TF_Statistics()
{ 
	;
}

TF_Statistics::~TF_Statistics() 
{
	;
}

pair<MBT_Matrix<double>, MBT_Matrix<int>> TF_Statistics::SortClustersFromMinXToMaxX(MBT_Matrix<double> C1, MBT_Matrix<int> C1bounds)
{
    int cnt = -1;
    auto tmp = 0.0;
    vector<double> KeepMinX;
    
    // sort the clusters from MinX to MaxX
	for (int i1 = 0 ; i1 < C1bounds.size().second; i1++)
	{
		tmp = *std::min_element(&C1(0,C1bounds(0,i1)),&C1(0,C1bounds(1,i1)));
		KeepMinX.push_back(tmp); 
		
	}
		
	// "IndicesOfSorting" vector includes the indices of the hypothetically sorted KeepMinX vector
    IndicesOfSorting.resize(KeepMinX.size());
    
    size_t n(0);
    generate(begin(IndicesOfSorting), end(IndicesOfSorting), [&]{ return n++; });
    sort(begin(IndicesOfSorting), end(IndicesOfSorting), [&](int i1, int i2) { return KeepMinX[i1] < KeepMinX[i2]; } );
    
    vector<double> SC1, SC2;
    MBT_Matrix<int> BoundsOfSortedClusters(C1bounds.size().first, C1bounds.size().second);
    BoundsOfSortedClusters(0, 0) = 0;
        
	for (unsigned int indCnt = 0 ; indCnt < IndicesOfSorting.size() ; indCnt++)
	{
		for (int q = C1bounds(0, IndicesOfSorting[indCnt]) ; q <= C1bounds(1, IndicesOfSorting[indCnt])  ; q++)
		{
			SC1.push_back(C1(0, q));
			SC2.push_back(C1(1, q));
			cnt = cnt + 1;
		}
		
		// store the new bounds
		BoundsOfSortedClusters(1, indCnt) = cnt;
		
		if (indCnt < IndicesOfSorting.size()-1)
			BoundsOfSortedClusters(0, indCnt + 1) = cnt + 1;
		else
			continue;
	}
		
	MBT_Matrix<double> SC(2, SC1.size());
	
	for (unsigned int k1 = 0 ; k1 < SC1.size(); k1++)
	{
		SC(0, k1) = SC1[k1];
		SC(1, k1) = SC2[k1];
	}
	
	return pair<MBT_Matrix<double>, MBT_Matrix<int>> (SC, BoundsOfSortedClusters);
}
	
pair<MBT_Matrix<double>, MBT_Matrix<int>> TF_Statistics::FindEdgePoints(MBT_Matrix<double>& SortedClusters, MBT_Matrix<double> TFMap, MBT_Matrix<int> NewBoundsOnSortedClusters)
{
    TF_Statistics ContourStats;
    MBT_Matrix<int> EEBounds;
	EEBounds = FindEdgeBounds(TFMap);
	vector<int> EdgePoints;
	vector<double> CleanClX, CleanClY;
	MBT_Matrix<int> UpdatedBounds;

	vector<int> freqs(EEBounds.size().first);
    iota(begin(freqs), end(freqs), 1); // fill with continuous numbers from 1 to 5
	
	for (int q = 0 ; q < SortedClusters.size().second; q++)
	{    
		if ( ((((SortedClusters(0, q) > EEBounds(0, 1))) || ((SortedClusters(0, q) <= EEBounds(0, 0)))) & ((SortedClusters(1, q)) == freqs[0])) ||
		     ((((SortedClusters(0, q) > EEBounds(1, 1))) || ((SortedClusters(0, q) <= EEBounds(1, 0)))) & ((SortedClusters(1, q)) == freqs[1])) ||
		     ((((SortedClusters(0, q) > EEBounds(2, 1))) || ((SortedClusters(0, q) <= EEBounds(2, 0)))) & ((SortedClusters(1, q)) == freqs[2])) ||
		     ((((SortedClusters(0, q) > EEBounds(3, 1))) || ((SortedClusters(0, q) <= EEBounds(3, 0)))) & ((SortedClusters(1, q)) == freqs[3])) ||
		     ((((SortedClusters(0, q) > EEBounds(4, 1))) || ((SortedClusters(0, q) <= EEBounds(4, 0)))) & ((SortedClusters(1, q)) == freqs[4]))
		    ) 
			{
				EdgePoints.push_back(q);
			}
			else
			{
				CleanClX.push_back(SortedClusters(0,q));
				CleanClY.push_back(SortedClusters(1,q));
			}
	}
	
	MBT_Matrix<double> CleanClusters(2, CleanClX.size());
	for (unsigned int v = 0 ; v < CleanClX.size(); v++)
	{
		CleanClusters(0, v) = CleanClX[v];
		CleanClusters(1, v) = CleanClY[v];
	}		
		
	if (!EdgePoints.empty())
	{	
		// updates the indices of the clusters
		UpdatedBounds = UpdateBounds(NewBoundsOnSortedClusters, EdgePoints);
		return pair<MBT_Matrix<double>, MBT_Matrix<int>> (CleanClusters, UpdatedBounds);
	}
	else
	{
		return pair<MBT_Matrix<double>, MBT_Matrix<int>> (CleanClusters, NewBoundsOnSortedClusters);
	}
}

MBT_Matrix<int> TF_Statistics::FindEdgeBounds(MBT_Matrix<double> TFMap)
{
	MBT_Matrix<int> Bounds(TFMap.size().first,2);
	
	for (int q1=0; q1<TFMap.size().first; q1++)
	{
		for (int q2=0; q2<TFMap.size().second-1; q2++)
		{
			if (TFMap(q1,q2)==0 && TFMap(q1,q2+1)!=0)
			{
				Bounds(q1,0) = q2+1;
			}
			else if (TFMap(q1,q2)!=0 && TFMap(q1,q2+1)==0)
			{
				Bounds(q1,1) = q2+1;
			}
		}
	}
	return Bounds;
}   

MBT_Matrix<int> TF_Statistics::UpdateBounds(MBT_Matrix<int> NewBoundsOnSortedClusters, vector<int> EdgePoints)
{
    MBT_Matrix<int> NewBounds(NewBoundsOnSortedClusters.size().first, NewBoundsOnSortedClusters.size().second);
    vector<int> NumOfPoints;
  
	int count = 0, countPrev = 0;
	MBT_Matrix<int> ClusterAndEdgePoints(NewBoundsOnSortedClusters.size().second, NewBoundsOnSortedClusters.size().first);
	
	for (int k1=0 ; k1<NewBoundsOnSortedClusters.size().second; k1++)
	{
		for (unsigned int k2=0 ; k2<EdgePoints.size(); k2++)
		{
			if (NewBoundsOnSortedClusters(1,k1) >= EdgePoints[k2])
			{
				count  = count + 1;
			}
			// number of clusters
			ClusterAndEdgePoints(k1, 0) = k1 + 1;		
		}
		
		// edge points detected
		ClusterAndEdgePoints(k1, 1) = count - countPrev;
		countPrev = count;
		count = 0;
	}
	
	// update the bounds
	int CumEdgePoints = 0;
	NewBounds(0,0) = NewBoundsOnSortedClusters(0,0);
	
	for (int i=0 ; i<ClusterAndEdgePoints.size().first; i++)
	{	
	    // upper bounds
	    CumEdgePoints = CumEdgePoints + ClusterAndEdgePoints(i,1); 
		NewBounds(1,i) = NewBoundsOnSortedClusters(1,i) - CumEdgePoints;
		
		if (i < ClusterAndEdgePoints.size().first-1)
			NewBounds(0,i+1) = NewBounds(1,i) + 1;
	}	
	return NewBounds;
}

pair<vector<double>, MBT_Matrix<double>> TF_Statistics::CalculateAreaAndGC(MBT_Matrix<double> SortedClustersRemovedEdgePoints, MBT_Matrix<int> NewBoundsOnRemovedEdgePoints)
{ 
	vector<double> x, y, Area, cx, cy;
	MBT_Matrix<double> GC(NewBoundsOnRemovedEdgePoints.size().first, NewBoundsOnRemovedEdgePoints.size().second);
	double ResSum = 0.0, TmpArea = 0.0;

	for (int j=0;  j<NewBoundsOnRemovedEdgePoints.size().second; j++)
	{
		for (int i=NewBoundsOnRemovedEdgePoints(0,j) ; i<=NewBoundsOnRemovedEdgePoints(1,j) ; i++)
		{
			x.push_back(SortedClustersRemovedEdgePoints(0,i));
			y.push_back(SortedClustersRemovedEdgePoints(1,i));
		}
	
    	vector<double>::const_iterator first_x = x.begin() + 1;
    	vector<double>::const_iterator last_x = x.end();
    	vector<double> x2(first_x, last_x);
    	x2.push_back(x[0]);
   
    	vector<double>::const_iterator first_y = y.begin() + 1;
    	vector<double>::const_iterator last_y = y.end();
    	vector<double> y2(first_y, last_y);
    	y2.push_back(y[0]);
   
    	vector<double> tmp_res1(x.size());
    	vector<double> tmp_res2(x.size());
    	vector<double> Res(x.size());
   
    	// calculate area	
    	transform(x.begin(), x.end(), y2.begin(), tmp_res1.begin(), multiplies<double>()); 
    	transform(x2.begin(), x2.end(), y.begin(), tmp_res2.begin(), multiplies<double>()); 
    	transform(tmp_res1.begin(), tmp_res1.end(), tmp_res2.begin(), Res.begin(), minus<double>()); 
   
    	ResSum  = accumulate(Res.begin(), Res.end(), 0.0);
    	TmpArea = ResSum/2;
    	Area.push_back(abs(TmpArea));
    	
    	vector<double> tmp_res3(x.size());
   		vector<double> tmp_res4(x.size());
   
   		// calculate gravity centers
   		transform(x.begin(), x.end(), x2.begin(), tmp_res3.begin(), plus<double>());   
   		transform(y.begin(), y.end(), y2.begin(), tmp_res4.begin(), plus<double>());   
   		transform(Res.begin(), Res.end(), Res.begin(), bind2nd(divides<double>(), this->constant));
   		transform(tmp_res3.begin(), tmp_res3.end(), Res.begin(), tmp_res3.begin(), multiplies<double>()); 
   		transform(tmp_res4.begin(), tmp_res4.end(), Res.begin(), tmp_res4.begin(), multiplies<double>()); 
   		double Sum_cx  = accumulate(tmp_res3.begin(), tmp_res3.end(), 0.0);
   		cx.push_back(Sum_cx / TmpArea);
   		double Sum_cy  = accumulate(tmp_res4.begin(), tmp_res4.end(), 0.0);
   		cy.push_back(Sum_cy / TmpArea);
   
        // clear everything
    	x.clear();
    	y.clear();
    	x2.clear();
    	y2.clear();
    	tmp_res1.clear();
    	tmp_res2.clear();
    	Res.clear();
   }
   
   for (unsigned int v=0 ; v<cx.size() ; v++)
   {
   		GC(0,v) = cx[v];
   		GC(1,v) = cy[v];
   }
  
   return pair<vector<double>, MBT_Matrix<double>> (Area, GC); 
}
  
pair<vector<double>, MBT_Matrix<double>> TF_Statistics::CalculateAreaAndGC(VVDouble XC, VVDouble YC)
{ 
	vector<double> x, y, Area, cx, cy;
	MBT_Matrix<double> GC(2, XC.size());
	double ResSum = 0.0, TmpArea = 0.0;

	for (unsigned int j=0;  j < XC.size(); j++)
	{
    	vector<double>::const_iterator first_x = XC[j].begin() + 1;
    	vector<double>::const_iterator last_x = XC[j].end();
    	vector<double> x2(first_x, last_x);
    	x2.push_back(XC[j][0]);

    	vector<double>::const_iterator first_y = YC[j].begin() + 1;
    	vector<double>::const_iterator last_y = YC[j].end();
    	vector<double> y2(first_y, last_y);
    	y2.push_back(YC[j][0]);

    	vector<double> tmp_res1(XC[j].size());
    	vector<double> tmp_res2(XC[j].size());
    	vector<double> Res(XC[j].size());

   		// calculate area	
    	transform(XC[j].begin(), XC[j].end(), y2.begin(), tmp_res1.begin(), multiplies<double>()); 
    	transform(x2.begin(), x2.end(), YC[j].begin(), tmp_res2.begin(), multiplies<double>()); 
    	transform(tmp_res1.begin(), tmp_res1.end(), tmp_res2.begin(), Res.begin(), minus<double>());  	
   
   		ResSum  = accumulate(Res.begin(), Res.end(), 0.0);
    	TmpArea = ResSum/2;
    	Area.push_back(abs(TmpArea));
    	
    	vector<double> tmp_res3(XC[j].size());
   		vector<double> tmp_res4(XC[j].size());

    	// calculate gravity centers
    	transform(XC[j].begin(), XC[j].end(), x2.begin(), tmp_res3.begin(), plus<double>());   
   		transform(YC[j].begin(), YC[j].end(), y2.begin(), tmp_res4.begin(), plus<double>());   
   		transform(Res.begin(), Res.end(), Res.begin(), bind2nd(divides<double>(), this->constant));
   		transform(tmp_res3.begin(), tmp_res3.end(), Res.begin(), tmp_res3.begin(), multiplies<double>()); 
   		transform(tmp_res4.begin(), tmp_res4.end(), Res.begin(), tmp_res4.begin(), multiplies<double>()); 
   		double Sum_cx  = accumulate(tmp_res3.begin(), tmp_res3.end(), 0.0);
   		cx.push_back(Sum_cx / TmpArea);
   		double Sum_cy  = accumulate(tmp_res4.begin(), tmp_res4.end(), 0.0);
   		cy.push_back(Sum_cy / TmpArea);
   
        // clear everything
    	XC[j].clear();
    	YC[j].clear();
    	x2.clear();
    	y2.clear();
    	tmp_res1.clear();
    	tmp_res2.clear();
    	Res.clear();
   }

   for (unsigned int v=0 ; v<cx.size() ; v++)
   {
   		GC(0,v) = cx[v];
   		GC(1,v) = cy[v];
   }
  
   return pair<vector<double>, MBT_Matrix<double>> (Area, GC); 
}

pair<MBT_Matrix<double>, MBT_Matrix<double>> TF_Statistics::CalculateMinMax(MBT_Matrix<double> SortedClustersRemovedEdgePoints, MBT_Matrix<int> NewBoundsOnRemovedEdgePoints)
{
	vector<double> tmpX, tmpY, MinX, MaxX, MinY, MaxY;
	
	for (int j=0;  j<NewBoundsOnRemovedEdgePoints.size().second; j++)
	{
		for (int i=NewBoundsOnRemovedEdgePoints(0,j) ; i<=NewBoundsOnRemovedEdgePoints(1,j) ; i++)
		{
			tmpX.push_back(SortedClustersRemovedEdgePoints(0, i));				
			tmpY.push_back(SortedClustersRemovedEdgePoints(1, i));				
		}
		
		// for the x-axis
		auto min_x = min_element(begin(tmpX), end(tmpX));
		auto max_x = max_element(begin(tmpX), end(tmpX));
		MinX.push_back(*min_x);
		MaxX.push_back(*max_x);
		tmpX.clear();
		
		// for the y-axis
		auto min_y = min_element(begin(tmpY), end(tmpY));
		auto max_y = max_element(begin(tmpY), end(tmpY));
		MinY.push_back(*min_y);
		MaxY.push_back(*max_y);
		tmpY.clear();
	}
	
	MBT_Matrix<double> MinMaxX(2, MinX.size());
    MBT_Matrix<double> MinMaxY(2, MinY.size());
    
	// put all the values into two MBT matrices
	for (unsigned int i=0; i<MinX.size(); i++)
	{
		MinMaxX(0,i) = MinX[i];
		MinMaxX(1,i) = MaxX[i];
		MinMaxY(0,i) = MinY[i];
		MinMaxY(1,i) = MaxY[i];
	}
		
	return pair<MBT_Matrix<double>, MBT_Matrix<double>> (MinMaxX, MinMaxY); 
}

pair<MBT_Matrix<double>, MBT_Matrix<double>> TF_Statistics::CalculateMinMax(VVDouble X, VVDouble Y)
{
	MBT_Matrix<double> MinMaxX(2, X.size());
    MBT_Matrix<double> MinMaxY(2, Y.size());
    vector<double> XMinValues(X.size()), XMaxValues(X.size()), YMinValues(Y.size()), YMaxValues(Y.size());

    for (unsigned int a = 0 ; a < X.size(); a++)
    {
    	auto resultX = minmax_element(X[a].begin(), X[a].end());
    	auto resultY = minmax_element(Y[a].begin(), Y[a].end());
    	
    	MinMaxX(0,a) = *resultX.first;
    	MinMaxX(1,a) = *resultX.second;

    	MinMaxY(0,a) = *resultY.first;
    	MinMaxY(1,a) = *resultY.second;    	
    }

	return pair<MBT_Matrix<double>, MBT_Matrix<double>> (MinMaxX, MinMaxY);
}

vector<double> TF_Statistics::CalculateTimeLength(MBT_Matrix<double> MinMaxX)
{
	vector<double> TimeLength;
	double tmp = 0.0;
	
	for (int i=0; i<MinMaxX.size().second; i++)
	{
		tmp = MinMaxX(1,i) - MinMaxX(0,i);
		TimeLength.push_back(tmp);
	}
	
	return TimeLength;
}

vector<int> TF_Statistics::findOpenClusters(MBT_Matrix<double> MinMaxY)
{
    vector<int> indices;
    
    for (int i=0; i<MinMaxY.size().second; i++)
    {
        // LowestFreq and HighestFreq are the low and up bounds of the frequencies
    	if ( (MinMaxY(0,i) == LowestFreq) && (MinMaxY(1,i) == HighestFreq) )
    	{
    		indices.push_back(i);
    	}
    }
   
	return indices;
}

pair<VVDouble, VVDouble> TF_Statistics::AssignClusterValuesToVectors(MBT_Matrix<int> Boundaries, MBT_Matrix<double> ClusterValues)
{
	VVDouble x, y;

	vector<int> sizes(Boundaries.size().second);
	int tmp = 0, cursor = 0;

	// calculate the size of each of the clusters
	for (int t = 0 ; t < Boundaries.size().second; t++)
	{
		tmp = Boundaries(1, t) - Boundaries(0, t) + 1;
		sizes[t] = tmp;
	}

	// give the first dimension of the 2d vectors
	x.resize(sizes.size());
	y.resize(sizes.size());

	for (unsigned int i = 0 ; i < sizes.size(); i++)
	{
		// give the second dimension of the 2d vectors for each different cluster
 		x[i].resize(sizes[i]);
 		y[i].resize(sizes[i]);

 		// put here the values of the big array to the 2d vectors (one for the xcoord and one for the ycoord)
 		for (int j = 0 ; j < sizes[i] ; j++)
 		{
 			x[i][j] = ClusterValues(0, cursor);
 			y[i][j] = ClusterValues(1, cursor);
 			cursor++;
 		}
	}

	return pair<VVDouble, VVDouble> (x, y);
}

pair<VVDouble, VVDouble> TF_Statistics::JoinOpenClusters(vector<int> IndicesOfOpenClusters, VVDouble xcoord, VVDouble ycoord)
{
    // for each second open cluster...
	for (unsigned int i=0 ; i<IndicesOfOpenClusters.size()-1; i=i+2)
	{
		// check here if there is a need for fliplr
		if (ycoord[IndicesOfOpenClusters[i]][ycoord[IndicesOfOpenClusters[i]].size()-1] != ycoord[IndicesOfOpenClusters[i+1]][0])
		{
			// fliplr of matlab
			reverse(xcoord[IndicesOfOpenClusters[i+1]].begin(), xcoord[IndicesOfOpenClusters[i+1]].end());
			reverse(ycoord[IndicesOfOpenClusters[i+1]].begin(), ycoord[IndicesOfOpenClusters[i+1]].end());
		}
		
		// merge the clusters
		// x coordinates
		xcoord[IndicesOfOpenClusters[i]].insert(xcoord[IndicesOfOpenClusters[i]].end(), xcoord[IndicesOfOpenClusters[i+1]].begin(), xcoord[IndicesOfOpenClusters[i+1]].end());
		xcoord[IndicesOfOpenClusters[i+1]].clear();

		// y coordinates
		ycoord[IndicesOfOpenClusters[i]].insert(ycoord[IndicesOfOpenClusters[i]].end(), ycoord[IndicesOfOpenClusters[i+1]].begin(), ycoord[IndicesOfOpenClusters[i+1]].end());
		ycoord[IndicesOfOpenClusters[i+1]].clear();
	}

	// erase the empty vectors
	for (unsigned int a = 0 ; a < ycoord.size(); a++)
	{
		if (ycoord[a].empty())
		{
			xcoord.erase(xcoord.begin() + a);
			ycoord.erase(ycoord.begin() + a);
		}
	}

	return pair<VVDouble, VVDouble> (xcoord, ycoord); 
}

pair<VVDouble, vector<VVDouble>> TF_Statistics::FixFullyOverlappingClusters(VVDouble XValues, VVDouble YValues, MBT_Matrix<double> MinMaxX, MBT_Matrix<double> MinMaxY, vector<double> Areas, MBT_Matrix<double> GC, vector<double> TL)
{
	
	unsigned int z = 0;
	int zOld = 0;
	double differ1 = 0, differ2 = 0, minz = 0, maxz = 0, minzplus1 = 0, maxzplus1 = 0, minzOld = 0, maxzOld = 0;

	while (z < XValues.size()-1)
	{
		// if the previous cluster did not get to empty
		if (!XValues[z].empty())
		{
			differ1   = MinMaxX(0, z+1) - MinMaxX(1, z);
			differ2   = MinMaxX(1, z+1) - MinMaxX(1, z);
			minz      = MinMaxY(0, z);
			maxz      = MinMaxY(1, z);
			minzplus1 = MinMaxY(0, z+1);
			maxzplus1 = MinMaxY(1, z+1);
			

			// at the edges - low y or high y
			if ((differ1<0) && (differ2<0) && ((minz == 1 && minzplus1 == 1) || ((maxz == 5) && (maxzplus1 == 5))))
			{
				Areas[z] = Areas[z] - Areas[z+1];
				 
				// delete the statistics of the following cluster (the one that overlaps with the previous)
				XValues[z+1].clear();
				YValues[z+1].clear();

				GC(0,z+1)       = 0;
				GC(1,z+1)       = 0;
				
				MinMaxX(0, z+1) = 0;
				MinMaxX(1, z+1) = 0; 
				MinMaxY(0, z+1) = 0;
				MinMaxY(1, z+1) = 0;
				
				TL[z+1]         = 0;
				Areas[z+1]      = 0;

				zOld            = z;
			}
			// in the middle
			else if ((differ1 < 0) && (differ2 < 0) && ((minz != minzplus1) || (maxz != maxzplus1)))
			{
				Areas[z] = Areas[z] + Areas[z+1];
				GC(0,z)  = (GC(0,z) + GC(0,z+1)) / 2;
				GC(1,z)  = (GC(1,z) + GC(1,z+1)) / 2;

				XValues[z+1].clear();
				YValues[z+1].clear();
				
				GC(0,z+1)       = 0;
				GC(1,z+1)       = 0;

				MinMaxX(0, z+1) = 0;
				MinMaxX(1, z+1) = 0; 
				MinMaxY(0, z+1) = 0;
				MinMaxY(1, z+1) = 0;

				TL[z+1]         = 0;
				Areas[z+1]      = 0;

				zOld            = z;
			}
		}
		// if the previous cluster got to empty
		else
		{
			z = z + 1;
			differ1 = MinMaxX(0,z) - MinMaxX(1,zOld);
			differ2 = MinMaxX(1,z) - MinMaxX(1,zOld);
			minz    = MinMaxY(0,z);
			maxz    = MinMaxY(1,z);
			minzOld = MinMaxY(0,zOld);
			maxzOld = MinMaxY(1,zOld); 

			// at the edges - low y or high y
			if ((differ1 < 0) && (differ2 < 0) && ((minz == 1 && minzOld == 1) || (maxz == 5 && maxzOld == 5)))
			{
				Areas[zOld] = Areas[zOld] - Areas[z];

				XValues[z].clear();
				YValues[z].clear();

				GC(0,z) = 0;
				GC(1,z) = 0;	

				MinMaxX(0, z) = 0;
				MinMaxX(1, z) = 0; 
				MinMaxY(0, z) = 0;
				MinMaxY(1, z) = 0;

				TL[z]         = 0;
				Areas[z]      = 0;
			}
			// in the middle
			else if((differ1 < 0) && (differ2 < 0) && ((minz != minzOld) || (maxz != maxzOld)))
			{
				Areas[zOld] = Areas[zOld] + Areas[z]; 	

				GC(0,zOld)  = (GC(0,zOld) + GC(0,z)) / 2;
				GC(1,zOld)  = (GC(1,zOld) + GC(1,z)) / 2;

				MinMaxY(0,z) = MinMaxY(0,zOld);
				MinMaxY(1,z) = MinMaxY(1,zOld);  

				XValues[z].clear();
				YValues[z].clear();

				GC(0,z)       = 0;
				GC(1,z)       = 0;	

				MinMaxX(0, z) = 0;
				MinMaxX(1, z) = 0; 
				MinMaxY(0, z) = 0;
				MinMaxY(1, z) = 0;

				TL[z]         = 0;
				Areas[z]      = 0;				
			}
			z = z - 1;
		}
		z = z + 1;
	}

	// delete the empty registrations
	for (unsigned int a = 0 ; a < XValues.size(); a++)
	{
		if (XValues[a].empty())
		{
			XValues.erase(XValues.begin() + a);
			YValues.erase(YValues.begin() + a);
			a = a - 1;
		}
	}

	// put all MBT_Matrix values into vectors
	vector<double> tmpGCX, tmpGCY, tmpMinX, tmpMaxX, tmpMinY, tmpMaxY;
	tmpGCX = GC.row(0);
	tmpGCY = GC.row(1);
	tmpMinX = MinMaxX.row(0);
	tmpMaxX = MinMaxX.row(1);
	tmpMinY = MinMaxY.row(0);
	tmpMaxY = MinMaxY.row(1);

	for (unsigned int a = 0 ; a < tmpGCX.size(); a++)
	{
		if (tmpGCX[a] == 0 && tmpGCY[a] == 0)
		{
			tmpGCX.erase(tmpGCX.begin() + a);
			tmpGCY.erase(tmpGCY.begin() + a);
			tmpMinX.erase(tmpMinX.begin() + a);
			tmpMaxX.erase(tmpMaxX.begin() + a);
			tmpMinY.erase(tmpMinY.begin() + a);
			tmpMaxY.erase(tmpMaxY.begin() + a);
			TL.erase(TL.begin() + a);
			Areas.erase(Areas.begin() + a);	
			a = a - 1;		
		}
	}

	//assign everything to VVDouble and vector<VVDouble> in order to return the results
	VVDouble MinMaxXYandGC;
	MinMaxXYandGC.resize(8);
    vector<VVDouble> XYCoord;
    XYCoord.resize(2);

	MinMaxXYandGC[0] = tmpMinX;
	MinMaxXYandGC[1] = tmpMaxX;
	MinMaxXYandGC[2] = tmpMinY;
	MinMaxXYandGC[3] = tmpMaxY;
	MinMaxXYandGC[4] = tmpGCX;
	MinMaxXYandGC[5] = tmpGCY;
	MinMaxXYandGC[6] = Areas;
    MinMaxXYandGC[7] = TL;

    XYCoord[0] = XValues;
    XYCoord[1] = YValues;

    return pair<VVDouble, vector<VVDouble>> (MinMaxXYandGC, XYCoord); 
}

pair<VVDouble, vector<VVDouble>> TF_Statistics::FixPartiallyOverlappingClusters(VVDouble AllResults, vector<VVDouble> XYCoord)
{
    vector<double> Xdifference, Distances;
    vector<double> MinX = AllResults[0];
    unsigned int z = 0, w = 0;
  
    while(z <= AllResults[0].size()-2)
    {
        Xdifference.push_back(AllResults[0][z+1] - AllResults[1][z]);
        z = z + 1;
    }

    while (w < Xdifference.size()-1)
    {
        if (Xdifference[w] > 0)
        {
            w = w + 1;
        }
        else
        {
            Xdifference.erase(Xdifference.begin() + w);
            
            //x-coordinates
            XYCoord[0][w].insert(XYCoord[0][w].end(), XYCoord[0][w+1].begin(), XYCoord[0][w+1].end());
            
            //y-coordinates
            XYCoord[1][w].insert(XYCoord[1][w].end(), XYCoord[1][w+1].begin(), XYCoord[1][w+1].end());
            
            // areas
            AllResults[6][w] = AllResults[6][w] + AllResults[6][w+1];
            AllResults[6].erase(AllResults[6].begin() + w + 1);
            
            // GCx
            AllResults[4][w] = (AllResults[4][w] + AllResults[4][w+1])/2;
            AllResults[4].erase(AllResults[4].begin() + w + 1);
            // GCy
            AllResults[5][w] = (AllResults[5][w] + AllResults[5][w+1])/2;
            AllResults[5].erase(AllResults[5].begin() + w + 1);

            // MinX and MaxX
            auto resultX = minmax_element(XYCoord[0][w].begin(), XYCoord[0][w].end()); 
            AllResults[0][w] = *resultX.first;
            AllResults[1][w] = *resultX.second;

            // MinY and MaxY
            auto resultY = minmax_element(XYCoord[1][w].begin(), XYCoord[1][w].end()); 
            AllResults[2][w] = *resultY.first;
            AllResults[3][w] = *resultY.second;

            // timelength
            AllResults[7][w] = AllResults[1][w] - AllResults[0][w];
            AllResults[7].erase(AllResults[7].begin() + w + 1);

            XYCoord[0].erase(XYCoord[0].begin() + w + 1);
            XYCoord[1].erase(XYCoord[1].begin() + w + 1);

            AllResults[0].erase(AllResults[0].begin() + w + 1);  
            AllResults[1].erase(AllResults[1].begin() + w + 1);        
            AllResults[2].erase(AllResults[2].begin() + w + 1);  
            AllResults[3].erase(AllResults[3].begin() + w + 1);        
        }
    }

    AllResults.resize(9);

    // calculate here the distances 
    AllResults[8] = CalculateDistances(AllResults);

    return pair<VVDouble, vector<VVDouble>> (AllResults, XYCoord); 
}

vector<double> TF_Statistics::CalculateDistances(VVDouble AllResults)
{
	vector<double> Distances;
	
	AllResults.resize(9);
    for (unsigned int a = 0 ; a < AllResults[0].size(); a++)
    {
        // store distances
        Distances.push_back(AllResults[1][a] - AllResults[0][a+1]);
    }

    Distances.insert(Distances.begin(), (1-AllResults[0][0]));
   
    // calculate distances 
    for (unsigned int a = 0 ; a < Distances.size(); a++)
    {
        if (a < Distances.size()-1)
            Distances[a] = abs(Distances[a]);
        else
            Distances.erase(Distances.begin() + a);     
    }

	return Distances;
}

pair<VVDouble, vector<VVDouble>> CalculateStatistics(MBT_Matrix<double> C1, MBT_Matrix<int> C1Bounds, MBT_Matrix<double> TFMap)
{
	TF_Statistics ContourStats;

	MBT_Matrix<double> SortedClusters;
	MBT_Matrix<double> SortedClustersRemovedEdgePoints;
	MBT_Matrix<int> NewBoundsOnSortedClusters;
	MBT_Matrix<int> NewBoundsOnRemovedEdgePoints;

	vector<double> Area, UpdatedArea;
	MBT_Matrix<double> GC, UpdatedGC;
	MBT_Matrix<double> MinMaxX, MinMaxY, UpdatedMinMaxX, UpdatedMinMaxY;
	vector<double> TimeLength, UpdatedTimeLength;
	
	vector<int> IndicesOfOpenClusters;
	VVDouble xcoord, ycoord;
	VVDouble UpdatedXcoord, UpdatedYcoord;
    VVDouble AllResults, UpdatedAllResults;
    vector<VVDouble> XYCoord, UpdatedXYCoord;
	
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	pair<MBT_Matrix<double>, MBT_Matrix<int>> pair1 = ContourStats.SortClustersFromMinXToMaxX(C1, C1Bounds);
	// the sorted clusters
	SortedClusters = pair1.first;
	// the bounds based on the sorted clusters
	NewBoundsOnSortedClusters = pair1.second;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	pair<MBT_Matrix<double>, MBT_Matrix<int>> pair2 = ContourStats.FindEdgePoints(SortedClusters, TFMap, NewBoundsOnSortedClusters);

	// the sorted clusters after the removal of the edge points
	SortedClustersRemovedEdgePoints = pair2.first;
	
	// the bounds on the sorted clusters after the removal of the edge points
	NewBoundsOnRemovedEdgePoints = pair2.second;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	pair<vector<double>, MBT_Matrix<double>> pair3 = ContourStats.CalculateAreaAndGC(SortedClustersRemovedEdgePoints, NewBoundsOnRemovedEdgePoints);
	
	// the areas of the clusters
	Area = pair3.first;

	/*for (int a = 0 ; a < Area.size(); a++)
		cout << Area[a] << endl;*/

	// the gravity centers of the clusters
	GC = pair3.second;
	
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	pair<MBT_Matrix<double>, MBT_Matrix<double>> pair4 = ContourStats.CalculateMinMax(SortedClustersRemovedEdgePoints, NewBoundsOnRemovedEdgePoints);

	// the min and max values in the x axis
	MinMaxX = pair4.first;

	// the min and max values in the y axis 
	MinMaxY = pair4.second;
	
	// the timelengths of the clusters
	TimeLength = ContourStats.CalculateTimeLength(MinMaxX);
	
	// indices of the clusters that do not close
	IndicesOfOpenClusters = ContourStats.findOpenClusters(MinMaxY);

	// assign each of the clusters separately into 2d vectors, in different vectors for x and y coordinate
	pair<VVDouble, VVDouble> coordinates = ContourStats.AssignClusterValuesToVectors(NewBoundsOnRemovedEdgePoints, SortedClustersRemovedEdgePoints);
	xcoord = coordinates.first;
	ycoord = coordinates.second;

	pair<VVDouble, vector<VVDouble>> pair8;

	if (!IndicesOfOpenClusters.empty())
	{
		pair<VVDouble, VVDouble> pair5 = ContourStats.JoinOpenClusters(IndicesOfOpenClusters, xcoord, ycoord);
		UpdatedXcoord = pair5.first;
		UpdatedYcoord = pair5.second;

		// areas and GC
		pair<vector<double>, MBT_Matrix<double>> pair6 = ContourStats.CalculateAreaAndGC(UpdatedXcoord, UpdatedYcoord);
		UpdatedArea = pair6.first;
		UpdatedGC = pair6.second;

        // Min and Max Y and Y
		pair<MBT_Matrix<double>, MBT_Matrix<double>> pair7 = ContourStats.CalculateMinMax(UpdatedXcoord, UpdatedYcoord);
		UpdatedMinMaxX = pair7.first;
		UpdatedMinMaxY = pair7.second;

        // timelength
		UpdatedTimeLength = ContourStats.CalculateTimeLength(UpdatedMinMaxX);
	
		pair8 = ContourStats.FixFullyOverlappingClusters(UpdatedXcoord, UpdatedYcoord, UpdatedMinMaxX, UpdatedMinMaxY, UpdatedArea, UpdatedGC, UpdatedTimeLength);
	}
	else
	{
		pair8 = ContourStats.FixFullyOverlappingClusters(xcoord, ycoord, MinMaxX, MinMaxY, Area, GC, TimeLength);  
	}

	AllResults = pair8.first;
    XYCoord    = pair8.second;

    pair<VVDouble, vector<VVDouble>> pair9 = ContourStats.FixPartiallyOverlappingClusters(AllResults, XYCoord);
    UpdatedAllResults = pair9.first;
    UpdatedXYCoord    = pair9.second;

	return pair<VVDouble, vector<VVDouble>> (UpdatedAllResults, UpdatedXYCoord); 
}



































 

