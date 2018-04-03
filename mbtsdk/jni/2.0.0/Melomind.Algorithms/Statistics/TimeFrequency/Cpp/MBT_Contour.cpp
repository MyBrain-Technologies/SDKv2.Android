// 
// MBT_Contour.cpp
// Created by Katerina Pandremmenou on 03/10/2016.
// Copyright (c) 2016 Katerina Pandremmenou. All rights reserved
//
// Update on 09/01/2017 by Katerina Pandremmenou :double-->float, no armadillo
// Update on 03/04/2017 by Katerina Pandremmenou : Convert everything from float to double,
//                                                 remove constant double bound and replace it with numeric_limits<double>::epsilon ()
// Update on 5/07/2017 by Katerina Pandremmenou  : Fix all the warnings
// Update on 19/09/2017 by Katerina Pandremmenou : Change all implicit type castings to explicit ones

#include <iomanip>
#include <iostream>
#include <list>
#include <algorithm>
#include <numeric>
#include <random>
#include <vector>
#include "../Headers/MBT_Contour.h"

using namespace std;

static vector<double> this_contour1;
static vector<double> this_contour2;
static vector<double> contourc1;
static vector<double> contourc2;
static int elem;

// Add a coordinate point (x,y) to this_contour.
static void add_point (double x, double y)
{
	if (elem % CONTOUR_QUANT == 0)
	{
		this_contour1.resize(this_contour1.size()+CONTOUR_QUANT);
		this_contour2.resize(this_contour2.size()+CONTOUR_QUANT);
	}
	
    this_contour1[elem] = x;
    this_contour2[elem] = y;
      
    elem++;
}

// Add contents of current contour to contourc
static void end_contour (void)
{    
	if (elem > 2)
    {
    	this_contour2[0] = elem - 1;
    	
    	this_contour1.resize(elem);
    	this_contour2.resize(elem);
    
    	contourc1.insert(contourc1.begin(), this_contour1.begin(), this_contour1.end());
    	contourc2.insert(contourc2.begin(), this_contour2.begin(), this_contour2.end());  
    }
      
    this_contour1.clear();
    this_contour2.clear();
    
    elem = 0;
}

// Start a new contour, and add contents of current one to contourc.    
static void start_contour (double lvl, double x, double y)
{
	end_contour();
	this_contour1.resize(1,0.0);
	this_contour2.resize(1,0.0);
    add_point (lvl, 0.0);
    add_point (x, y);
}
            
static void drawcn (const vector<int>& X, const vector<int>& Y, MBT_Matrix<double>& Z, double lvl, int r, int c, double ct_x, double ct_y, unsigned int start_edge, bool first, MBT_Matrix<double>& mark)
{
	double px[4], py[4], pz[4], tmp;
    unsigned int stop_edge, pt[2];
    
    // Continue while next facet is not done yet.
    while (r >= 0 && c >= 0 && r < mark.size().first && c < mark.size().second && mark(r, c) > 0) 
    {
    	//get x, y, and z - lvl for current facet
        px[0] = px[3] = X[c];
        px[1] = px[2] = X[c+1];
   
        py[0] = py[1] = Y[r];
        py[2] = py[3] = Y[r+1];
   
        pz[3] = Z(r+1, c) - lvl;
        pz[2] = Z(r+1, c + 1) - lvl;
        pz[1] = Z(r, c+1) - lvl;
        pz[0] = Z(r, c) - lvl;
   
        // Facet edge and point naming assignment.
        //
        //  0-----1   .-0-.
        //  |     |   |   |
        //  |     |   3   1
        //  |     |   |   |
        //  3-----2   .-2-.
   
        // Get mark value of current facet.
       char id = static_cast<char> (mark(r,c));
   
        // Check startedge s.
        if (start_edge == 255)
        {
        	// Find start edge.
            for (unsigned int k = 0; k < 4; k++)
            	if (static_cast<char> (1 << k) & id)
                	start_edge = k;
        }
   
        if (start_edge == 255)
        	break;
   
        // Decrease mark value of current facet for start edge.
        mark(r,c) -= static_cast<char> (1 << start_edge);
        
        // Next point (clockwise).
        pt[0] = start_edge;
        pt[1] = (pt[0] + 1) % 4;
   
        // Calculate contour segment start if first of contour.
        if (first)
        {
        	tmp = fabs (pz[pt[1]]) / fabs (pz[pt[0]]);
   
            if (isnan (tmp)) // I MADE IT xisnan--> isnan 
            	ct_x = ct_y = 0.5;
            else
            {
            	ct_x = px[pt[0]] + (px[pt[1]] - px[pt[0]])/(1 + tmp);
                ct_y = py[pt[0]] + (py[pt[1]] - py[pt[0]])/(1 + tmp);
            }
   
           start_contour (lvl, ct_x, ct_y);
           first = false;
    	}
        // Find stop edge.
        for (unsigned int k = 1; k <= 4; k++)
        {
        	if (start_edge == 0 || start_edge == 2)
            	stop_edge = (start_edge + k) % 4;
            else
                stop_edge = (start_edge - k) % 4;
   
            if (static_cast<char> (1 << stop_edge) & id)
            	break;
        }
   
        pt[0] = stop_edge;
        pt[1] = (pt[0] + 1) % 4;
        tmp = fabs (pz[pt[1]]) / fabs (pz[pt[0]]);
   
        if (isnan (tmp)) 
          ct_x = ct_y = 0.5;
        else
        {
        	ct_x = px[pt[0]] + (px[pt[1]] - px[pt[0]])/(1 + tmp);
            ct_y = py[pt[0]] + (py[pt[1]] - py[pt[0]])/(1 + tmp);
        }
        
         // Add point to contour.
         add_point (ct_x, ct_y);
      
         // Decrease id value of current facet for start edge.
         mark(r,c) -= static_cast<char> (1 << stop_edge);
   
         // Find next facet.
         if (stop_edge == 0)
           r--;
         else if (stop_edge == 1)
           c++;
         else if (stop_edge == 2)
           r++;
         else if (stop_edge == 3)
           c--;
   
         // Go to next facet.
        start_edge = (stop_edge + 2) % 4;
	}
}

static void mark_facets (MBT_Matrix<double>& Z, MBT_Matrix<double>& mark, double lvl)
{ 
     unsigned int nr = Z.size().first-1;
     unsigned int nc = Z.size().second-1;
     
     double f[4];
  
     for (unsigned int c = 0; c < nc; c++)
       for (unsigned int r = 0; r < nr; r++)
       {
           f[0] = Z(r, c) - lvl;
           f[1] = Z(r, c+1) - lvl;
           f[3] = Z(r+1, c) - lvl;
           f[2] = Z(r+1, c+1) - lvl;
       
          for (unsigned int i = 0; i < 4; i++)
            if (fabs(f[i]) < numeric_limits<double>::epsilon ())
               f[i] = numeric_limits<double>::epsilon ();
   
           if (f[1] * f[2] < 0)
             mark(r,c) += 2;
           
           if (f[0] * f[3] < 0)
             mark(r,c) += 8;
       }
   
     for (unsigned int r = 0; r < nr; r++)
       for (unsigned int c = 0; c < nc; c++)
       {
           f[0] = Z(r, c) - lvl;
           f[1] = Z(r, c+1) - lvl;
           f[3] = Z(r+1, c) - lvl;
           f[2] = Z(r+1, c+1) - lvl;
    
           for (unsigned int i = 0; i < 4; i++)
            if (fabs(f[i]) < std::numeric_limits<double>::epsilon ())
               f[i] = std::numeric_limits<double>::epsilon ();
         
           if (f[0] * f[1] < 0)
             mark(r,c) += 1;
          
           if (f[2] * f[3] < 0)
             mark(r,c) += 4;
      }        
}
   
static void cntr (const vector<int>& X, const vector<int>& Y, MBT_Matrix<double>& Z, double lvl)
{   
    unsigned int nr = Y.size();
    unsigned int nc = X.size();
               
    MBT_Matrix<double> mark(nr-1, nc-1);
    
    for (unsigned int k1 = 0 ; k1 < nr-1 ; k1++)
    {
    	for (unsigned int k2 = 0; k2 < nc-1 ; k2++)
    	{
    		mark(k1,k2) = 0;
    	}
    }
    
    mark_facets (Z, mark, lvl);
    
    // Find contours that start at a domain edge.
    for (unsigned int c = 0; c < nc - 1; c++)
    {  
        // bitwise representation of the number in mark[0][c] and 1 (4 digits)
        // The condition is true if the result is different than zero 
        // Top.
         if ((int) mark(0,c) & 1) 
           drawcn (X, Y, Z, lvl, 0, c, 0.0, 0.0, 0, true, mark);
   
         // Bottom.
         if ((int) mark(nr-2,c) & 4)
   	       drawcn (X, Y, Z, lvl, nr - 2, c, 0.0, 0.0, 2, true, mark);
    }
   
    for (unsigned int r = 0; r < nr - 1; r++)
    {
    	// Left.
        if ((int) mark(r,0) & 8)
        	drawcn (X, Y, Z, lvl, r, 0, 0.0, 0.0, 3, true, mark);
   
        // Right.
        if ((int) mark(r, nc-2) & 2)
        	drawcn (X, Y, Z, lvl, r, nc - 2, 0.0, 0.0, 1, true, mark);
    }
   
    for (unsigned int r = 0; r < nr - 1; r++)
     	for (unsigned int c = 0; c < nc - 1; c++)
        	if ((int) mark(r,c) > 0)
            	drawcn (X, Y, Z, lvl, r, c, 0.0, 0.0, 255, true, mark);
}
  
double ContourThresholds(MBT_Matrix<double> TFMap)
{
    
    vector<double> MinMaxTFValues(2); 
    MinMaxTFValues = MinMaxTFMap(TFMap);
    double IsoRange = 0.0;
    
    IsoRange = (MinMaxTFValues[1] - Threshold*MinMaxTFValues[1] - MinMaxTFValues[0]) / (isolines+1);
  
    return IsoRange;
}  
  
MBT_Matrix<double> Contourc(MBT_Matrix<double> TFMap, double threshold)
{     
    int rows = TFMap.size().first;
    int cols = TFMap.size().second;
        
    vector<int> x(rows); 
    iota(begin(x), end(x), 1); // fills the vector with continuous numbers from 1 to the number of rows
    vector<int> y(cols); 
    iota(begin(y), end(y), 1); // fills the vector with continuous numbers from 1 to the number of columns
   
    // call cntr function
    cntr (y, x, TFMap, threshold);
    end_contour();         
    
    MBT_Matrix<double> ContourResults(2, contourc1.size());
       
    for(unsigned int k1 = 0 ; k1 < contourc1.size(); k1++)
    {
    	ContourResults(0,k1) = contourc1[k1];
    	ContourResults(1,k1) = contourc2[k1]; 
    }
    
    return ContourResults;
}

MBT_Matrix<int> C2xyz(MBT_Matrix<double> ContourResults)
{
	vector<double> m(ContourResults.size().second,0.0);
	
	int n = 1;
	m[0] = 0;
	int counter = 0;
	
    while (n < ContourResults.size().second) // because we start from 0
	{
	    n = n + 1;
	    m[n] = m[n-1] + ContourResults(1,(int) m[n-1])+1;	    
	    m[n-1] = m[n];
	    if ((m[n-1]) >= ContourResults.size().second-1)
	    {   
	        m[n] = 0;
	     	break;
	    }
	}
	
	n = n - 1;

    // save the limits in bounds
    // bounds[0]: low bound - bounds[1]: high bound 
	MBT_Matrix<int> bounds(2, n);
		
	for (counter = 0; counter < n; counter++)
	{  
	    bounds(0,counter) = (int) m[counter] + 1;
	    bounds(1,counter) = (int) m[counter+1] - 1;
	}
	
	return bounds;
}


