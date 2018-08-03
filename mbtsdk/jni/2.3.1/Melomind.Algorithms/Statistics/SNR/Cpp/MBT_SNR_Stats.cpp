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
// 												 --> Change the type of the output of sort_indexes in lines 159, 356 
// Update on 03/11/2017 by Katerina Pandremmenou --> Update the way we calculate the AreaUp by fixing several bugs (this function has to be tested more)
// Update on 08/11/2017 by Katerina Pandremmenou --> Implementation of the calculation of the AreaDown
//                                               --> Implementation of a new way to calculate the performance (time spent above a threshold)
// Update on 09/11/2017 by Katerina Pandremmenou --> Change the names of the results to be more representative for each exercise
// Update on 20/11/2017 by Katerina Pandremmenou --> 1) Fix a bug for Ycross_down
//												 --> 2) Change the place in the code where we calculate Performance
// 												 --> 3) Sort KeepXcross_up and KeepYcross_up based on the sorted KeepXcross_up
//												 --> 4) Change the way we calculate the overall area and go back to the implementation of the sum of the areas above the threshold to the overall area

#include "../Headers/MBT_SNR_Stats.h"

#define uint unsigned long

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
	vector<int> Bound1, Bound2, Bound3, Bound4, Q1, Q2, Q3, Q4, tmp2, tmp3, tmp4, tmp5;
	vector<unsigned long> tmp1;
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
	vector<size_t> indexes = sort_indexes(tmp1);

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

pair<float,float> SNR_Statistics::ExerciseAreaUpAndPerformance(vector<float> SNR, double ExerciseThreshold)
{
	int AllUp = 0;
	double m_down, b_down, m_up, b_up, sumAreaUp = 0, Performance = 0;
	vector<double> Xcross_down, Ycross_down, Xcross_up, Ycross_up, tmp_All_x, tmp_All_y, AreaUp, KeepXcross_up, KeepYcross_up, TimeLength;
	vector<int> IndicesCrossUpPoints, IndicesCrossDownPoints;
	vector<double> IndicesCrossPoints;
	vector<vector<double>> Cross_point_up, Cross_point_down;
	vector<double> seq(SNR.size()), seq2(SNR.size());

	// create a vector from 0 to the length of SNR vector
	iota(seq.begin(), seq.end(), 1);

	for (uint i = 0 ; i < SNR.size()-1; i++)
	{
		if ((SNR[i] == ExerciseThreshold) && (SNR[i+1] > ExerciseThreshold))
		{
			Xcross_up.push_back(i+1);
			Ycross_up.push_back(SNR[i]);	
		}
		else if ((SNR[i] > ExerciseThreshold) && (SNR[i+1] == ExerciseThreshold))
		{
			Xcross_down.push_back(i+2);
			Ycross_down.push_back(SNR[i+1]);	
		}
		else if ((SNR[i] > ExerciseThreshold) && (SNR[i+1] < ExerciseThreshold))
		{
			m_down = (SNR[i+1] - SNR[i]) / (i+1-i);
			b_down = SNR[i] - m_down*(i+1);
			Xcross_down.push_back((ExerciseThreshold - b_down) / m_down);
			Ycross_down.push_back(m_down * Xcross_down[Xcross_down.size()-1] + b_down);
		}
		else if ((SNR[i] < ExerciseThreshold) && (SNR[i+1] > ExerciseThreshold))
		{
			m_up  = (SNR[i+1] - SNR[i]) / (i+1-i);
			b_up  = SNR[i] - m_up*(i+1);
			Xcross_up.push_back((ExerciseThreshold - b_up) / m_up);
			Ycross_up.push_back(m_up * Xcross_up[Xcross_up.size()-1] + b_up);
		}
		// if all of the points are above a threshold
		else if ((SNR[i] > ExerciseThreshold) && (SNR[i+1] > ExerciseThreshold))
			AllUp = AllUp + 1;
	}

	if (SNR[0] > ExerciseThreshold)
	{
		Xcross_up.push_back(0);
		Ycross_up.push_back(ExerciseThreshold);
	}

	if (SNR[SNR.size()-1] > ExerciseThreshold)
	{
		Xcross_down.push_back(SNR.size()+1);
		Ycross_down.push_back(ExerciseThreshold);
	}

	KeepXcross_up = Xcross_up;
	KeepYcross_up = Ycross_up;

	// sort the X-crossing points
	vector<size_t> Test_Indices;
	// find the indices of the sorting of tmp_All_x
	Test_Indices = sort_indexes(KeepXcross_up);
	// sort tmp_All_x
	sort(KeepXcross_up.begin(), KeepXcross_up.end());

	// sort accordingly all the Y-crossing points
	vector<double> tmp;
	for (uint a = 0 ; a < Test_Indices.size(); a++)
		tmp.push_back(KeepYcross_up[Test_Indices[a]]);

	KeepYcross_up.clear();
	KeepYcross_up.assign(tmp.begin(), tmp.end());

	Test_Indices.clear();
	tmp.clear();

	// IMPLEMENTATION OF AREA UP 
	double val = 0;
	// put in one vector all X-crossing points
	Xcross_up.insert(Xcross_up.end(), Xcross_down.begin(), Xcross_down.end());
	
	// put in one vector all Y-crossing points
	Ycross_up.insert(Ycross_up.end(), Ycross_down.begin(), Ycross_down.end());

	// sort the X-crossing points
	Test_Indices = sort_indexes(Xcross_up);
	sort(Xcross_up.begin(), Xcross_up.end());

	// sort accordingly all the Y-crossing points
	//vector<double> tmp;
	tmp.clear();
	for (uint a = 0 ; a < Test_Indices.size(); a++)
		tmp.push_back(Ycross_up[Test_Indices[a]]);

	Ycross_up.clear();
	Ycross_up.assign(tmp.begin(), tmp.end());
	tmp.clear();

	// keep unique Xcross_up and Ycross_up values	
	// copy Xcross_up to another vector
	for (uint a = 0 ; a < Xcross_up.size(); a++)
		tmp.push_back(Xcross_up[a]);

	// find unique values in Xcross_up
	Xcross_up.erase(unique(Xcross_up.begin(), Xcross_up.end() ), Xcross_up.end());

	vector<int> Keep_Indices;
	// find the indices of the unique values 
	for (uint a = 0 ; a < Xcross_up.size(); a++)
	{
		for (uint b = 0 ; b < tmp.size(); b++)
		{
			if (Xcross_up[a] == tmp[b]) 
			{
				Keep_Indices.push_back(b);
				break;
			}
		} 
	}
	
	// keep the values of Ycross_up that correspond to the unique values of Xcross_up
	tmp.clear();
    for (uint r = 0 ; r < Keep_Indices.size(); r++)
		tmp.push_back(Ycross_up[Keep_Indices[r]]);	

	Ycross_up.clear();
	
	for (uint a = 0 ; a < tmp.size(); a++)
		Ycross_up.push_back(tmp[a]);	

	if ((!Xcross_up.empty()) && (!Ycross_up.empty()))
    {
		// put (x,y) values together for both the "above" and "below" cases
		// the case "above the threshold"
		Cross_point_up.resize(Xcross_up.size());

		for (uint i = 0 ; i < Cross_point_up.size(); i++)
		{
			Cross_point_up[i].push_back(Xcross_up[i]);
			Cross_point_up[i].push_back(Ycross_up[i]);
		}

		int n = 0;
		for (uint t = 0 ; t < seq.size()+Cross_point_up.size(); t++)
		{
			if (t < seq.size())
			{
				tmp_All_x.push_back(seq[t]);
				tmp_All_y.push_back(SNR[t]);
			}
			else
			{
				tmp_All_x.push_back(Cross_point_up[n][0]);
				tmp_All_y.push_back(Cross_point_up[n][1]);
				n = n + 1;
			}
		}	

		vector<size_t> Indtmp_All_x = sort_indexes(tmp_All_x);
		sort(tmp_All_x.begin(), tmp_All_x.end());
		vector<double> Sorted_tmp_All_y(tmp_All_y.size()), Helper1(tmp_All_x.size()), Helper2(tmp_All_x.size());

		for (uint a = 0 ; a < tmp_All_y.size(); a++)
			Sorted_tmp_All_y[a] = tmp_All_y[(int) Indtmp_All_x[a]];
		
		if ((tmp_All_x[0] == tmp_All_x[1]) && (Sorted_tmp_All_y[0]>Sorted_tmp_All_y[1]))
		{
			val = Sorted_tmp_All_y[0];
			Sorted_tmp_All_y[0] = Sorted_tmp_All_y[1];
			Sorted_tmp_All_y[1] = val;
		}
 
		// sort the X-crossing points
		Test_Indices.clear();
		// find the indices of the sorting of tmp_All_x
		Test_Indices = sort_indexes(tmp_All_x);
		// sort tmp_All_x
		sort(tmp_All_x.begin(), tmp_All_x.end());

		// sort accordingly all the Y-crossing points
		tmp.clear();
		for (uint a = 0 ; a < Test_Indices.size(); a++)
			tmp.push_back(Sorted_tmp_All_y[Test_Indices[a]]);

		Sorted_tmp_All_y.clear();
		Sorted_tmp_All_y.assign(tmp.begin(), tmp.end());
		tmp.clear();

		// KEEP UNIQUE ROWS
		// keep unique tmp_All_x and Ycross_up values	
		// copy tmp_All_x to another vector
		for (uint a = 0 ; a < tmp_All_x.size(); a++)
			tmp.push_back(tmp_All_x[a]);

		// keep unique values in tmp_All_x
		tmp_All_x.erase(unique(tmp_All_x.begin(), tmp_All_x.end() ), tmp_All_x.end());

		Keep_Indices.clear();
		// find the indices of the unique values of tmp_All_x
		for (uint a = 0 ; a < tmp_All_x.size(); a++)
		{
			for (uint b = 0 ; b < tmp.size(); b++)
			{
				if (tmp_All_x[a] == tmp[b]) 
				{
					Keep_Indices.push_back(b);
					break;
				}
			} 
		}
		// keep the values of Sorted_tmp_All_y that correspond to the unique values of tmp_All_x
		tmp.clear();
    	for (uint r = 0 ; r < Keep_Indices.size(); r++)
			tmp.push_back(Sorted_tmp_All_y[Keep_Indices[r]]);	

		Sorted_tmp_All_y.clear();
	
		for (uint a = 0 ; a < tmp.size(); a++)
			Sorted_tmp_All_y.push_back(tmp[a]);

		// if all of the points are above the threshold
		if (AllUp == (double)SNR.size()-1)
		{
			vector<double> Up_tmp(SNR.size());
			for (uint y = 0 ; y < Up_tmp.size(); y++)
				Up_tmp[y] = SNR[y] - ExerciseThreshold;

			// create a vector from 0 to the length of Up vector
			iota(seq2.begin(), seq2.end(), 1);
			sumAreaUp = trapz(seq2, Up_tmp);
			Up_tmp.clear();

			Performance = SNR.size();
		}
		else
		{
			for (uint i = 0 ; i < KeepXcross_up.size(); i++)
			{	
				for (uint j = 0 ; j < tmp_All_x.size(); j++)
				{
					if ( (tmp_All_x[j] == KeepXcross_up[i]) && (round(Sorted_tmp_All_y[j]*pow(10,2))/pow(10,2) == round(Cross_point_up[i][1]*pow(10,2))/pow(10,2)) ) 
						IndicesCrossUpPoints.push_back(j);
				}
			}

			for (uint i = 0 ; i < Xcross_down.size(); i++)
			{
				for (uint j = 0 ; j < tmp_All_x.size(); j++)
				{
					if ( (tmp_All_x[j] == Xcross_down[i]) && (round(Sorted_tmp_All_y[j]*pow(10,2))/pow(10,2) == round(Ycross_down[i]*pow(10,2))/pow(10,2)) ) 
					{
						IndicesCrossDownPoints.push_back(j);
					}
				}
			}

			sort(IndicesCrossUpPoints.begin(), IndicesCrossUpPoints.end());
			sort(IndicesCrossDownPoints.begin(), IndicesCrossDownPoints.end());
		
			if (KeepXcross_up.size() == Xcross_down.size())
			{
				for (uint r = 0 ; r < KeepXcross_up.size(); r++)
				{
					if (KeepXcross_up[r] == 0)
						TimeLength.push_back(Xcross_down[r] - KeepXcross_up[r] - 1);
					else
						TimeLength.push_back(Xcross_down[r] - KeepXcross_up[r]);
				}	
					Performance = *max_element(TimeLength.begin(), TimeLength.end());
				}

			// AREA UP
			vector<double> Up_tmp;
			if (KeepXcross_up.size() == Xcross_down.size())
			{
				for (uint i = 0 ; i < KeepXcross_up.size(); i++)
				{
					for (int j = IndicesCrossUpPoints[i] ; j <= IndicesCrossDownPoints[i]; j++)
					{
						IndicesCrossPoints.push_back((double) j);
						Up_tmp.push_back(Sorted_tmp_All_y[j] - ExerciseThreshold);
					}
					val = trapz(IndicesCrossPoints, Up_tmp);
					AreaUp.push_back(val);

					IndicesCrossPoints.clear();
					Up_tmp.clear();
				}
				sumAreaUp = accumulate(AreaUp.begin(), AreaUp.end(), 0.0);
			}
		}	
	}
	else
	{
		sumAreaUp = 0;
		Performance = 0;
	}

	return pair<float, float> ((float) sumAreaUp, (float) Performance);
}

float SNR_Statistics::ExerciseAreaDown(vector<float> SNR, double ExerciseThreshold)
{
	int AllDown = 0;
	double m_down, b_down, m_up, b_up, val = 0, sumAreaDown = 0;
	vector<double> Xcross_down, Ycross_down, Xcross_up, Ycross_up, tmp_All_x, tmp_All_y, AreaDown, KeepXcross_up, KeepYcross_up;
	vector<int> IndicesCrossUpPoints, IndicesCrossDownPoints;
	vector<double> IndicesCrossPoints;
	vector<vector<double>> Cross_point_up, Cross_point_down;
	vector<double> seq(SNR.size()), seq2(SNR.size());

	// create a vector from 0 to the length of SNR vector
	iota(seq.begin(), seq.end(), 1);

	for (uint i = 0 ; i < SNR.size()-1; i++)
	{
		if ((SNR[i] == ExerciseThreshold) && (SNR[i+1] < ExerciseThreshold))
		{
			Xcross_down.push_back(i+1);
			Ycross_down.push_back(SNR[i]);	
		}
		else if ((SNR[i] < ExerciseThreshold) && (SNR[i+1] == ExerciseThreshold))
		{
			Xcross_up.push_back(i+2);
			Ycross_up.push_back(SNR[i+2]);	
		}
		else if ((SNR[i] > ExerciseThreshold) && (SNR[i+1] < ExerciseThreshold))
		{
			m_down = (SNR[i+1] - SNR[i]) / (i+1-i);
			b_down = SNR[i] - m_down*(i+1);

			Xcross_down.push_back((ExerciseThreshold - b_down) / m_down);
			Ycross_down.push_back(m_down * Xcross_down[Xcross_down.size()-1] + b_down);
		}
		else if ((SNR[i] < ExerciseThreshold) && (SNR[i+1] > ExerciseThreshold))
		{
			m_up  = (SNR[i+1] - SNR[i]) / (i+1-i);
			b_up  = SNR[i] - m_up*(i+1);
			Xcross_up.push_back((ExerciseThreshold - b_up) / m_up);
			Ycross_up.push_back(m_up * Xcross_up[Xcross_up.size()-1] + b_up);
		}
		// if all of the points are below a threshold
		else if ((SNR[i] < ExerciseThreshold) && (SNR[i+1] < ExerciseThreshold))
			AllDown = AllDown + 1;
	}

	if (SNR[0] < ExerciseThreshold)
	{
		Xcross_down.push_back(0);
		Ycross_down.push_back(ExerciseThreshold);
	}

	if (SNR[SNR.size()-1] < ExerciseThreshold)
	{
		Xcross_up.push_back(SNR.size()+1);
		Ycross_up.push_back(ExerciseThreshold);
	}

	KeepXcross_up = Xcross_up;
	KeepYcross_up = Ycross_up;

	// put in one vector all X-crossing points
	Xcross_up.insert(Xcross_up.end(), Xcross_down.begin(), Xcross_down.end());
	
	// put in one vector all Y-crossing points
	Ycross_up.insert(Ycross_up.end(), Ycross_down.begin(), Ycross_down.end());

	// sort Xcross_up and based on this sorting sort also the Ycross_up values
	// sort the X-crossing points
	vector<size_t>Test_Indices = sort_indexes(Xcross_up);
	sort(Xcross_up.begin(), Xcross_up.end());

	// sort accordingly all the Y-crossing points
	vector<double> tmp;
	for (uint a = 0 ; a < Test_Indices.size(); a++)
		tmp.push_back(Ycross_up[Test_Indices[a]]);

	Ycross_up.clear();
	Ycross_up.assign(tmp.begin(), tmp.end());
	tmp.clear();

	// =================================================================================================================================================================
	// KEEP UNIQUE ROWS
	// keep unique Xcross_up and Ycross_up values	
	// copy Xcross_up to another vector
	for (uint a = 0 ; a < Xcross_up.size(); a++)
		tmp.push_back(Xcross_up[a]);

	// find unique values in Xcross_up
	Xcross_up.erase(unique(Xcross_up.begin(), Xcross_up.end() ), Xcross_up.end());

	vector<int> Keep_Indices;
	// find the indices of the unique values 
	for (uint a = 0 ; a < Xcross_up.size(); a++)
	{
		for (uint b = 0 ; b < tmp.size(); b++)
		{
			if (Xcross_up[a] == tmp[b]) 
			{
				Keep_Indices.push_back(b);
				break;
			}
		} 
	}
	// keep the values of Ycross_up that correspond to the unique values of Xcross_up
	tmp.clear();
    for (uint r = 0 ; r < Keep_Indices.size(); r++)
		tmp.push_back(Ycross_up[Keep_Indices[r]]);	

	Ycross_up.clear();
	
	for (uint a = 0 ; a < tmp.size(); a++)
		Ycross_up.push_back(tmp[a]);	
	
	// =================================================================================================================================================================
	if ((!Xcross_up.empty()) && (!Ycross_up.empty()))
    {
		// put (x,y) values together for both the "above" and "below" cases
		// the case "above the threshold"
		Cross_point_up.resize(Xcross_up.size());

		for (uint i = 0 ; i < Cross_point_up.size(); i++)
		{
			Cross_point_up[i].push_back(Xcross_up[i]);
			Cross_point_up[i].push_back(Ycross_up[i]);
		}

    	// Area Down
		int n = 0;
		for (uint t = 0 ; t < seq.size()+Cross_point_up.size(); t++)
		{
			if (t < seq.size())
			{
				tmp_All_x.push_back(seq[t]);
				tmp_All_y.push_back(SNR[t]);
			}
			else
			{
				tmp_All_x.push_back(Cross_point_up[n][0]);
				tmp_All_y.push_back(Cross_point_up[n][1]);
				n = n + 1;
			}
		}	

		// sort tmp_All_x values
		vector<size_t> Indtmp_All_x = sort_indexes(tmp_All_x);
		sort(tmp_All_x.begin(), tmp_All_x.end());
		vector<double> Sorted_tmp_All_y(tmp_All_y.size()), Helper1(tmp_All_x.size()), Helper2(tmp_All_x.size());

		// sort tmp_All_y based on the indices of sorted tmp_All_x
		for (uint a = 0 ; a < tmp_All_y.size(); a++)
			Sorted_tmp_All_y[a] = tmp_All_y[(int) Indtmp_All_x[a]];
		
		if ((tmp_All_x[0] == tmp_All_x[1]) && (Sorted_tmp_All_y[0]>Sorted_tmp_All_y[1]))
		{
			val = Sorted_tmp_All_y[0];
			Sorted_tmp_All_y[0] = Sorted_tmp_All_y[1];
			Sorted_tmp_All_y[1] = val;
		}

		// sort the X-crossing points
		Test_Indices.clear();
		// find the indices of the sorting of tmp_All_x
		Test_Indices = sort_indexes(tmp_All_x);
		// sort tmp_All_x
		sort(tmp_All_x.begin(), tmp_All_x.end());

		// sort accordingly all the Y-crossing points
		tmp.clear();
		for (uint a = 0 ; a < Test_Indices.size(); a++)
			tmp.push_back(Sorted_tmp_All_y[Test_Indices[a]]);

		Sorted_tmp_All_y.clear();
		Sorted_tmp_All_y.assign(tmp.begin(), tmp.end());
		tmp.clear();

		// =================================================================================================================================================================
		// KEEP UNIQUE ROWS
		// keep unique tmp_All_x and Ycross_up values	
		// copy tmp_All_x to another vector
		for (uint a = 0 ; a < tmp_All_x.size(); a++)
			tmp.push_back(tmp_All_x[a]);

		// keep unique values in tmp_All_x
		tmp_All_x.erase(unique(tmp_All_x.begin(), tmp_All_x.end() ), tmp_All_x.end());

		Keep_Indices.clear();
		// find the indices of the unique values of tmp_All_x
		for (uint a = 0 ; a < tmp_All_x.size(); a++)
		{
			for (uint b = 0 ; b < tmp.size(); b++)
			{
				if (tmp_All_x[a] == tmp[b]) 
				{
					Keep_Indices.push_back(b);
					break;
				}
			} 
		}
		// keep the values of Sorted_tmp_All_y that correspond to the unique values of tmp_All_x
		tmp.clear();
    	for (uint r = 0 ; r < Keep_Indices.size(); r++)
			tmp.push_back(Sorted_tmp_All_y[Keep_Indices[r]]);	

		Sorted_tmp_All_y.clear();
	
		for (uint a = 0 ; a < tmp.size(); a++)
			Sorted_tmp_All_y.push_back(tmp[a]);
	// =================================================================================================================================================================
		
	// if all of the points are below the threshold
		if (AllDown == (double)SNR.size()-1)
		{
			vector<double> Down_tmp(SNR.size());
			for (uint y = 0 ; y < Down_tmp.size(); y++)
				Down_tmp[y] = ExerciseThreshold - SNR[y];

			// create a vector from 0 to the length of Down vector
			iota(seq2.begin(), seq2.end(), 1);
			sumAreaDown = trapz(seq2, Down_tmp);
			Down_tmp.clear();
		}
		else
		{
			for (uint i = 0 ; i < KeepXcross_up.size(); i++)
			{	
				for (uint j = 0 ; j < tmp_All_x.size(); j++)
				{
					if ( (tmp_All_x[j] == KeepXcross_up[i])  && (round(Sorted_tmp_All_y[j]*pow(10,2))/pow(10,2) == round(KeepYcross_up[i]*pow(10,2))/pow(10,2)) ) 
						IndicesCrossUpPoints.push_back(j);	
				}
			}

			for (uint i = 0 ; i < Xcross_down.size(); i++)
			{
				for (uint j = 0 ; j < tmp_All_x.size(); j++)
				{
					if ( (tmp_All_x[j] == Xcross_down[i]) && (round(Sorted_tmp_All_y[j]*pow(10,2))/pow(10,2) == round(Ycross_down[i]*pow(10,2))/pow(10,2)) ) 
						IndicesCrossDownPoints.push_back(j);
				}
			}

			sort(IndicesCrossUpPoints.begin(), IndicesCrossUpPoints.end());
			sort(IndicesCrossDownPoints.begin(), IndicesCrossDownPoints.end());

			vector<double> Down_tmp;
			if (KeepXcross_up.size() == Xcross_down.size())
			{
				for (uint i = 0 ; i < Xcross_down.size(); i++)
				{
					for (int j = IndicesCrossDownPoints[i] ; j <= IndicesCrossUpPoints[i]; j++)
					{
						IndicesCrossPoints.push_back((double) j);
						Down_tmp.push_back(ExerciseThreshold - Sorted_tmp_All_y[j]);
					}
					val = trapz(IndicesCrossPoints, Down_tmp);
					AreaDown.push_back(val);
					IndicesCrossPoints.clear();
					Down_tmp.clear();
				}
				sumAreaDown = accumulate(AreaDown.begin(), AreaDown.end(), 0.0);
			}
		}
	}
	else
		sumAreaDown = 0;

	return (float) sumAreaDown;
}

map<string, float> SNR_Statistics::CalculateSNRStatistics(vector<float> SNR, float ExerciseThreshold)
{
	float Performance = 0, OverallArea = 0, RelaxationPercentage = 0;
	float SumAreaAboveThresh = 0, MeanHighSNR;
	vector<double> seq(SNR.size()), ThresholdVec(SNR.size());
	vector<double> vec_for_trapz(SNR.size());
	vector<double> temporary;

	iota(seq.begin(), seq.end(), 1);
	fill (ThresholdVec.begin(), ThresholdVec.end(), ExerciseThreshold);

	// exercise Switch
	int NumberOfSwitches = ExerciseSwitch(SNR);

	// exercise Journey - Area Up and exercise Stability
	pair<float, float> res = ExerciseAreaUpAndPerformance(SNR, ExerciseThreshold);
	SumAreaAboveThresh = res.first;
	Performance        = res.second;

	// exercise Journey - Area Down
	//float SumAreaBelowThresh = ExerciseAreaDown(SNR, ExerciseThreshold);


	// calculate the mean SNR value of all the SNR values above the threshold
	for (uint a = 0 ; a < SNR.size(); a++)
	{
		if (SNR[a] > ExerciseThreshold)
			temporary.push_back(SNR[a]);
	}

	MeanHighSNR = (float) accumulate(temporary.begin(), temporary.end(), 0.0)/temporary.size();
	
	// normalize the area
	// calculate the overall area above the threshold
	for (uint i = 0 ; i < vec_for_trapz.size(); i++)
		vec_for_trapz[i] = MeanHighSNR - ExerciseThreshold;
	
	if (!isnan(MeanHighSNR))
	{
		OverallArea = (float) trapz(seq, vec_for_trapz);
		// calculate the relaxation percentage
		RelaxationPercentage = (SumAreaAboveThresh / OverallArea) * 100;
	}
	else	
		RelaxationPercentage = 0;
	
	//RelaxationPercentage = (1 -(SumAreaBelowThresh / OverallArea)) * 100;

	map<string, float> SnrStatistics;
	
	// the formula for the max nb of switches is: max nb switches = ((nb minutes * 60 - 3)/3)-1
	SnrStatistics["switch"] = (float)NumberOfSwitches/(SNR.size()/3-1);
	SnrStatistics["journey"] = RelaxationPercentage;
	SnrStatistics["stability"] = Performance;

	return SnrStatistics;
}