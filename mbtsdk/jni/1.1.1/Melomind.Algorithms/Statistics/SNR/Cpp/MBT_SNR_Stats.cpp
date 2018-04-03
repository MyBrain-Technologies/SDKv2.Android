//
// MBT_SNR_Stats.cpp
//
// Created by Katerina Pandremmenou on 21/04/2017
// Copyright (c) 2017 myBrain Technologies. All rights reserved.
// We miss here the implementation for the AreaDown. Also the implementation for the distances is in comments

// Update on 14/06/2017 by Katerina Pandremmenou --> Calculate normalized values of the three statistics between 0 and 1.
// Update on 25/07/2017 by Katerina Pandremmenou --> Calculate the sum of the area above a threshold and not the mean area above a threshold
// Update on 27/07/2017 by Katerina Pandremmenou --> fixed a bug for the case where all of the points are below the threshold
// Update on 19/09/2017 by Katerina Pandremmenou --> Change all implicit type castings to explicit ones
//												 --> Change the type of the output of sort_indexes in lines 159, 356 

#include "../Headers/MBT_SNR_Stats.h"

using namespace std;

SNR_Statistics::SNR_Statistics(vector<float> SNR)
{ 
	HighThreshold = 0.75;
	LowThreshold  = 0.25;

	// use another representation for the values that are above the upper threshold and below the lowest threshold
	for (uint a = 0; a < SNR.size(); a++)
	{
		if (SNR[a] >= HighThreshold)
			Representation.push_back(1);
		else if (SNR[a] <= LowThreshold)
			Representation.push_back(-1);
		else 
			Representation.push_back(0);
	}
}

SNR_Statistics::~SNR_Statistics() 
{
	;
}

int SNR_Statistics::ExerciseSwitch(vector<float> SNR)
{
	int Switch = 0, closestValues;
	vector<int> Bound1, Bound2, Bound3, Bound4, Q1, Q2, Q3, Q4, tmp1, tmp2, tmp3, tmp4, tmp5;
	vector<vector<double>> Vec_Of_Inter;
	vector<double> InterValues;

	for (uint i = 3; i < SNR.size()-3; i++)
	{
		// B1
		if ((Representation[i-2] == 1) && (Representation[i-1] == 1) && (Representation[i] == 1) && (Representation[i+1] == 0)) 
			Bound1.push_back(i);
		// B2
    	else if ((Representation[i] == 0) && (Representation[i+1] == -1) && (Representation[i+2] == -1) && (Representation[i+3] == -1))
            Bound2.push_back(i+1);
        // B3        
    	else if ((Representation[i-2] == -1) && (Representation[i-1] == -1) && (Representation[i] == -1) && (Representation[i+1] == 0)) 
            Bound3.push_back(i);   
		// B4        
    	else if ((Representation[i] == 0) && (Representation[i+1] == 1) && (Representation[i+2] == 1) && (Representation[i+3] == 1))
            Bound4.push_back(i+1);  
        // B5
    	else if ((Representation[i-2] == 1) && (Representation[i-1] == 1) && (Representation[i] == 1) && (Representation[i+1] == -1) && (Representation[i+2] == -1) && (Representation[i+3] == -1))
           Switch = Switch + 1;
        // B6
    	else if ((Representation[i-2] == -1) && (Representation[i-1] == -1) && (Representation[i] == -1) && (Representation[i+1] == 1) && (Representation[i+2] == 1) && (Representation[i+3] == 1))
           Switch  = Switch + 1;
	}

	// top to bottom
	if (!Bound1.empty() && !Bound2.empty())
	{
		// if the length of B1 > length(B2)
		if (Bound1.size() > Bound2.size())
		{
			for (uint a = 0 ; a < Bound2.size(); a++)
			{
				for (uint b = 0 ; b < Bound1.size(); b++)
				{
					if (Bound1[b] < Bound2[a])
						Q1.push_back(Bound1[b]);
				}
				if (!Q1.empty())
				{
					closestValues = *max_element(Q1.begin(), Q1.end());
					tmp1.push_back(closestValues);
					tmp2.push_back(Bound2[a]);
					Q1.clear();
				}
			}
		}
		// if the length of B1 <= length(B2)
		else if (Bound1.size() <= Bound2.size())
		{
			for (uint c = 0 ; c < Bound1.size(); c++)
			{
				for (uint d = 0 ; d < Bound2.size(); d++)
				{
					if (Bound2[d] > Bound1[c])
						Q2.push_back(Bound2[d]);
				}
				if (!Q2.empty())
				{
					closestValues = *min_element(Q2.begin(), Q2.end());
					tmp1.push_back(Bound1[c]);
					tmp2.push_back(closestValues);
					Q2.clear();
				}
			}
		}
	}

	// bottom to top
	if (!Bound3.empty() && !Bound4.empty())
	{
		// if the length of B3 > length(B4)
		if (Bound3.size() > Bound4.size())
		{
			for (uint a = 0 ; a < Bound4.size(); a++)
			{
				for (uint b = 0 ; b < Bound3.size(); b++)
				{
					if (Bound3[b] < Bound4[a])
						Q3.push_back(Bound3[b]);
				}
				if (!Q3.empty())
				{
					closestValues = *max_element(Q3.begin(), Q3.end());
					tmp3.push_back(closestValues);
					tmp4.push_back(Bound4[a]);
					Q3.clear();
				}
			}
		}
		// if the length of B3 <= length(B4)
		else if (Bound3.size() <= Bound4.size())
		{
			for (uint c = 0 ; c < Bound3.size(); c++)
			{
				for (uint d = 0 ; d < Bound4.size(); d++)
				{
					if (Bound4[d] > Bound3[c])
						Q4.push_back(Bound4[d]);
				}
				if (!Q4.empty())
				{
					closestValues = *min_element(Q4.begin(), Q4.end());
					tmp3.push_back(Bound3[c]);
					tmp4.push_back(closestValues);
					Q4.clear();
				}
			}
		}
	}

	tmp1.insert(tmp1.end(), tmp3.begin(), tmp3.end());
	tmp2.insert(tmp2.end(), tmp4.begin(), tmp4.end());

    // sort "rows"
	vector<unsigned long> indexes = sort_indexes(tmp1);
	sort(tmp1.begin(), tmp1.end());

	for (uint a = 0 ; a < tmp2.size(); a++)
		tmp5.push_back(tmp2[indexes[a]]);

	if (tmp1.empty() && tmp2.empty())
	{
		Switch = 0;
	}
	else
	{
		Vec_Of_Inter.resize(tmp1.size());

		for (uint m = 0 ; m < tmp1.size(); m++)
		{
			for (int k = tmp1[m]+1; k < tmp5[m]; k++)
				InterValues.push_back(SNR[k]);

			Vec_Of_Inter[m].resize(InterValues.size());

			for (uint t = 0 ; t < InterValues.size(); t++)
				Vec_Of_Inter[m][t] = InterValues[t];	

			InterValues.clear();

			if (Vec_Of_Inter[m].size() == 1)
				Switch = Switch + 1;
			else if (is_sorted(Vec_Of_Inter[m].begin(), Vec_Of_Inter[m].end()))
				Switch = Switch + 1;
			else
			{ 
				reverse(Vec_Of_Inter[m].begin(),Vec_Of_Inter[m].end());
				if (is_sorted(Vec_Of_Inter[m].begin(), Vec_Of_Inter[m].end()))
					Switch = Switch + 1;
			}
		}
	}
	return Switch;
}

pair<float, float> SNR_Statistics::ExerciseAreaTimeLength(vector<float> SNR, double ExerciseThreshold)
{
	int Cross = 0, AllUp = 0 , AllDown = 0;
	double m_down, b_down, m_up, b_up, val, sumAreaUp = 0, performance = 0, OverallArea = 0;
	vector<double> Xcross_down, Ycross_down, Xcross_up, Ycross_up, temp1, temp2, SortPoints, timelength, distance, tmp_All_x, tmp_All_y, Up, Down, tmpIndices, AreaUp, AreaDown;
	vector<int> IndicesCrossUpPoints, IndicesCrossDownPoints, IndicesCrossPoints;
	vector<vector<double>> Cross_point_up, Cross_point_down, Cross_point;
	vector<double> seq(SNR.size()), seq2(SNR.size());
	vector<double> vec_for_trapz(SNR.size());

	// create a vector from 0 to the length of SNR vector
	iota(seq.begin(), seq.end(), 1);

	for (uint i = 0 ; i < SNR.size()-1; i++)
	{
		if ((SNR[i] >= ExerciseThreshold) && (SNR[i+1] < ExerciseThreshold))
		{
			Cross = Cross + 1;
			m_down = (SNR[i+1] - SNR[i]) / (i+1-i);
			b_down = SNR[i] - m_down*(i+1);

			Xcross_down.push_back((ExerciseThreshold - b_down) / m_down);
			Ycross_down.push_back(m_down * Xcross_down[Xcross_down.size()-1] + b_down);
		}
		else if ((SNR[i] < ExerciseThreshold) && (SNR[i+1] >= ExerciseThreshold))
		{
			Cross = Cross + 1;
			m_up  = (SNR[i+1] - SNR[i]) / (i+1-i);
			b_up  = SNR[i] - m_up*(i+1);
			Xcross_up.push_back((ExerciseThreshold - b_up) / m_up);
			Ycross_up.push_back(m_up * Xcross_up[Xcross_up.size()-1] + b_up);
		}
		// if all of the points are above a threshold
		else if ((SNR[i] >= ExerciseThreshold) && (SNR[i+1] >= ExerciseThreshold))
			AllUp = AllUp + 1;
		// if all of the points are below a threshold
		else if ((SNR[i] < ExerciseThreshold) && (SNR[i+1] < ExerciseThreshold))
			AllDown = AllDown + 1;
	}

	if ((!Xcross_down.empty()) && (!Xcross_up.empty()))
    {
		// put (x,y) values together for both the "above" and "below" cases
		// the case "above the threshold"
		Cross_point_up.resize(Xcross_up.size());
		for (uint i = 0 ; i < Cross_point_up.size(); i++)
		{
			Cross_point_up[i].push_back(Xcross_up[i]);
			Cross_point_up[i].push_back(Ycross_up[i]);
		}

		// the case "below the threshold"
		Cross_point_down.resize(Xcross_down.size());
		for (uint i = 0 ; i < Cross_point_down.size(); i++)
		{
			Cross_point_down[i].push_back(Xcross_down[i]);
			Cross_point_down[i].push_back(Ycross_down[i]);
		}

		// Performance: maximum time over a threshold
		// if the first SNR is not a crossing point
		if (SNR[0] != ExerciseThreshold)
		{
			temp1 = {1, ExerciseThreshold};
			temp2 = {(double)SNR.size(), ExerciseThreshold};

			// artificially close the beginning of the curve by a Cross_point_down
        	if (Cross_point_up[0][0] < Cross_point_down[0][0]) 
        		Cross_point_down.insert(Cross_point_down.begin(), temp1);
        	// artificially close the beginning of the curve by a Cross_point_up
        	else if (Cross_point_up[0][0] > Cross_point_down[0][0]) 
        		Cross_point_up.insert(Cross_point_up.begin(), temp1);       
    	}
    	// if the last SNR is not a crossing point
    	if (SNR[SNR.size()-1] != ExerciseThreshold)
    	{
    		// artificially close the beginning of the curve by a Cross_point_down
    		if (Cross_point_down[Cross_point_down.size()-1][0] < Cross_point_up[Cross_point_up.size()-1][0])
    			Cross_point_down.push_back(temp2);
    		// artificially close the beginning of the curve by a Cross_point_up
    		else if (Cross_point_down[Cross_point_down.size()-1][0] > Cross_point_up[Cross_point_up.size()-1][0])
    			Cross_point_up.push_back(temp2);
    	}			

    	Cross_point.resize(Cross_point_up.size()+Cross_point_down.size());

    	int w = 0;
    	for (uint h = 0 ; h < Cross_point.size(); h++)
    	{
    		if (h < Cross_point_down.size())
    		{
    			Cross_point[h].resize(Cross_point_down[h].size());
    			for (uint y = 0 ; y < Cross_point_down[h].size(); y++)
    				Cross_point[h][y] = Cross_point_down[h][y];
    		}
    		else
    		{
    			Cross_point[h].resize(Cross_point_up[w].size());
    			for (uint y = 0 ; y < Cross_point_up[w].size(); y++)
    				Cross_point[h][y] = Cross_point_up[w][y];
    			w = w + 1; 
    		}
    	}

    	for (uint a = 0 ; a < Cross_point.size(); a++)
    		SortPoints.push_back(Cross_point[a][0]);

    	sort(SortPoints.begin(), SortPoints.end());

    	// no need to sort the second dimension, it always includes the threshold
    	for (uint a = 0 ; a < SortPoints.size(); a++)
    		Cross_point[a][0] = SortPoints[a];
	
    	// if the SNR values cross the threshold
    	vector<double> comp(SortPoints.size());
   		adjacent_difference (SortPoints.begin(), SortPoints.end(), comp.begin());
    	
   		comp.erase(comp.begin());

   		// go above the threshold
   		if (Cross_point_up[0][0] < Cross_point_down[0][0])
   		{
   			for (uint g = 0 ; g < comp.size(); g=g+2)
   				timelength.push_back(comp[g]);
   			//for (uint g = 1 ; g < comp.size(); g=g+2)
   			//	distance.push_back(comp[g]);
   		}
   		// go below the threshold
    	else if (Cross_point_up[0][0] > Cross_point_down[0][0])
    	{
        	// the timelength over the threshold in the beginning and the end of the session
    		for (uint g = 1 ; g < comp.size(); g=g+2)
   				timelength.push_back(comp[g]);
   			//for (uint g = 0 ; g < comp.size(); g=g+2)
   			//	distance.push_back(comp[g]);
   		}

   		performance = *max_element(timelength.begin(), timelength.end());
    
    	// Area Up
		int n = 0;
		for (uint t = 0 ; t < seq.size()+Cross_point.size(); t++)
		{
			if (t < seq.size())
			{
				tmp_All_x.push_back(seq[t]);
				tmp_All_y.push_back(SNR[t]);
			}
			else
			{
				tmp_All_x.push_back(Cross_point[n][0]);
				tmp_All_y.push_back(Cross_point[n][1]);
				n = n + 1;
			}
		}	

		vector<unsigned long> Indtmp_All_x = sort_indexes(tmp_All_x);
		sort(tmp_All_x.begin(), tmp_All_x.end());
		vector<double> Sorted_tmp_All_y(tmp_All_y.size()), Helper1(tmp_All_x.size()), Helper2(tmp_All_x.size());

		for (uint a = 0 ; a < tmp_All_y.size(); a++)
		{
			Sorted_tmp_All_y[a] = tmp_All_y[(int) Indtmp_All_x[a]];
			Helper1[a] = round((tmp_All_x[a]*100)) / 100;
			Helper2[a] = round((Sorted_tmp_All_y[a]*100)) / 100;

			if (Sorted_tmp_All_y[a] < ExerciseThreshold)
				Sorted_tmp_All_y[a] = ExerciseThreshold;
		}

		ExerciseThreshold = round((ExerciseThreshold*100)) / 100;
	
		// Remove the points that we have artificially added at the begining and the end of the curve to close the curve
		if (Helper1[0] == Helper1[1])
		{
			if (Helper2[0] == ExerciseThreshold) 
			{
				tmp_All_x.erase(tmp_All_x.begin());
				Sorted_tmp_All_y.erase(Sorted_tmp_All_y.begin());
			}
			else if (Helper2[1] == ExerciseThreshold)
			{
				tmp_All_x.erase(tmp_All_x.begin()+1);
				Sorted_tmp_All_y.erase(Sorted_tmp_All_y.begin()+1);	
			}
		}

		if (Helper1[Helper1.size()-1] == Helper1[Helper1.size()-2])
		{
			if (Helper2[Helper2.size()-1] == ExerciseThreshold)
			{
				tmp_All_x.erase(tmp_All_x.begin()+tmp_All_x.size()-1);
				Sorted_tmp_All_y.erase(Sorted_tmp_All_y.begin()+Sorted_tmp_All_y.size()-1);	
			}
			else if (Helper2[Helper2.size()-2] == ExerciseThreshold)
			{
				tmp_All_x.erase(tmp_All_x.begin()+tmp_All_x.size()-2);
				Sorted_tmp_All_y.erase(Sorted_tmp_All_y.begin()+Sorted_tmp_All_y.size()-2);	
			}
		}

		for (uint y = 0 ; y < Cross_point_up.size(); y++)
		{
			for (uint y2 = 0; y2 < tmp_All_x.size(); y2++)
			{
				if (tmp_All_x[y2] == Cross_point_up[y][0])
					IndicesCrossUpPoints.push_back(y2);
			}
		}

		for (uint y = 0 ; y < Cross_point_down.size(); y++)
		{
			for (uint y2 = 0; y2 < tmp_All_x.size(); y2++)
			{
				if (tmp_All_x[y2] == Cross_point_down[y][0])
					IndicesCrossDownPoints.push_back(y2);
			}
		}

		IndicesCrossUpPoints.insert(IndicesCrossUpPoints.end(), IndicesCrossDownPoints.begin(), IndicesCrossDownPoints.end());
		sort(IndicesCrossUpPoints.begin(), IndicesCrossUpPoints.end());
		IndicesCrossPoints = IndicesCrossUpPoints;

		// go above the threshold
		if (Cross_point_up[0][0] <= Cross_point_down[0][0])
		{
			for (uint i = 0 ; i < IndicesCrossPoints.size()-1; i = i+2)
			{
				for (int t = IndicesCrossPoints[i]; t <= IndicesCrossPoints[i+1]; t++)
				{
					Up.push_back(Sorted_tmp_All_y[t]-ExerciseThreshold);
					tmpIndices.push_back(t);
				}
				val = trapz(tmpIndices, Up);
				AreaUp.push_back(val);
				tmpIndices.clear();
				Up.clear();	
			}
		}
		// go below the threshold
		else if (Cross_point_up[0][0] > Cross_point_down[0][0])
		{
			for (uint i = 1 ; i < IndicesCrossPoints.size()-1; i = i+2)
			{
				for (int t = IndicesCrossPoints[i]; t <= IndicesCrossPoints[i+1]; t++)
				{
					Up.push_back(Sorted_tmp_All_y[t]-ExerciseThreshold);
		    		tmpIndices.push_back(t);
				}	
				val = trapz(tmpIndices, Up);
				AreaUp.push_back(val);
				tmpIndices.clear();
				Up.clear();	
			}
		}

		sumAreaUp = accumulate(AreaUp.begin(), AreaUp.end(), 0.0);
	}
	// in the case where all of the points are above or below the threshold
	else 
	{
		Cross = 0;
		// in the case where all of the points are above the threshold
		if (AllUp == (double)SNR.size()-1)
		{
			performance = SNR.size();

			vector<double> Up_tmp(SNR.size());
			for (uint y = 0 ; y < Up_tmp.size(); y++)
				Up_tmp[y] = SNR[y] - ExerciseThreshold;

			// create a vector from 0 to the length of Up vector
			iota(seq2.begin(), seq2.end(), 1);
			val = trapz(seq2, Up_tmp);

			sumAreaUp = val;
		}
		// in the case where all of the points are below the threshold
		else if (AllUp == 0) 
		{
			sumAreaUp = 0.0;			
		}
	}

	// calculate the overall area above the threshold
	for (uint i = 0 ; i < vec_for_trapz.size(); i++)
	{
		vec_for_trapz[i] = 1 - ExerciseThreshold;
	}

	OverallArea = trapz(seq, vec_for_trapz);

	return pair<float, float> ((float)sumAreaUp/OverallArea, (float)performance/SNR.size());
}

map<string, float> SNR_Statistics::CalculateSNRStatistics(vector<float> SNR, float ExerciseThreshold)
{
	int NumberOfSwitches = ExerciseSwitch(SNR);
	pair<float, float> res = ExerciseAreaTimeLength(SNR, ExerciseThreshold);
	float SumAreaAboveThresh = res.first;
	float Performance = res.second;
	
	map<string, float> SnrStatistics;
	
	// we divide by 44 for the 3-minute duration of the exercise Switch
	// we consider only the session data (180-45 = 135" - 3" = 132"/3 = 44)
	SnrStatistics["Switch_Percentage"] = (float)NumberOfSwitches/44;

	SnrStatistics["SumAreaAboveThresh_Percentage"] = SumAreaAboveThresh;
	SnrStatistics["Performance"] = Performance;

	return SnrStatistics;
}