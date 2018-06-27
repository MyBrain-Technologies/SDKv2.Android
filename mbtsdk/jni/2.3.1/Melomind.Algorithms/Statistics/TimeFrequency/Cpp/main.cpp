//
// main.cpp
// Created by Katerina Pandremmenou on 08/12/2016 (inspired by Emma's code for reading from input/writing to output)
// Copyright (c) 2016 Katerina Pandremmenou. All rights reserved
// 
//
// Update on 31/03/2017 by Katerina Pandremmenou: Organization of the basic function that handles everything for the calculation of the TF map statistics 
// Update on 03/04/2017 by Katerina Pandremmenou: Convert everything throughout the whole code from float to double
// Update on 04/04/2017 by Katerina Pandremmenou: Include a function to skip the possible nan values present in the input vector
// Update on 04/04/2017 by Katerina Pandremmenou: Fix all the warnings
// Update on 28/04/2017 by Katerina Pandremmenou: Include some code to trigger the calculation of the SNR statistics
// Update on 06/09/2017 by Katerina Pandremmenou: Modify the header files path to fit with the new GIT architecture
// Update on 14/09/2017 by Katerina Pandremmenou: Test a new preprocessing of the signals
// Update on 19/09/2017 by Katerina Pandremmenou: Change the implicit type castings to explicit ones in line 210
//                                              : Change the order of the testing of the different functionalities

#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <iterator>
#include <iomanip>
#include <string>
#include <algorithm>
#include <ctime>
#include <chrono>
#include <math.h>
#include <stdio.h>
#include <complex>

#include "../../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h"
#include "../Headers/MBT_TF_Stats.h"
#include "../../../SignalProcessing.Cpp/PreProcessing/Headers/MBT_PreProcessing.h"
#include "../../../SignalProcessing.Cpp/Algebra/Headers/MBT_Operations.h"
#include "../../../SignalProcessing.Cpp/Transformations/Headers/MBT_Fourier_fftw3.h"
#include "../../../SignalProcessing.Cpp/Algebra/Headers/MBT_Interpolation.h"
#include "../../../SignalProcessing.Cpp/PreProcessing/Headers/MBT_BandPass_fftw3.h"
#include "../Headers/MBT_TF_map.h"
#include "../../../SignalProcessing.Cpp/Transformations/Headers/MBT_PWelchComputer.h"
#include "../Headers/MBT_Contour.h"
#include "../../../SignalProcessing.Cpp/Algebra/Headers/MBT_kmeans.h"
#include "../Headers/MBT_FinalClustering.h"
#include "../../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_ReadInputOrWriteOutput.h"
#include "../../SNR/Headers/MBT_SNR_Stats.h"

using namespace std;

// function to read from a file
/*vector<float> readInput(string fileName)
{
    ifstream file (fileName.c_str());
    vector<float> data;

    if (file.is_open())
    {  
        string number;
        while (file >> number)
        {
            istringstream numberStream(number);
            float n;
            numberStream >> n;
            data.push_back(n);
        }
        
        file.close();
    }
    else
    {
        errno = ENOENT;
        perror("ERROR: Cannot open the input file");
    }
    
    return data;
}*/

// function to write to a file
void writeOutput(vector<double> const& outputData, string fileName)
{
    ofstream file (fileName.c_str());
    for(vector<double>::const_iterator i = outputData.begin(); i != outputData.end(); ++i) 
    {
    	file << fixed << setprecision(30) << *i << '\n';
    }
    file.close();
}

// this function skips the possible NaN values that are present in a file
vector<float> SkipNaN(vector<complex<float>> Input)
{
  vector<float> InputNoNaN;
  for (unsigned int a = 0 ; a < Input.size(); a++)
  {
    if (!isnan(real(Input[a])))
      InputNoNaN.push_back(real(Input[a]));
  }

  return InputNoNaN;
}

// this function just converts the complex<float> input to float
vector<double> ConvertComplexFloatToDouble(vector<complex<float>> Input)
{
  vector<double> InputToFloat;
  for (unsigned int a = 0 ; a < Input.size(); a++)
  {
      InputToFloat.push_back(real(Input[a]));
  }
  return InputToFloat;
}

MBT_Matrix<float> runEverything(vector<float> Data, int flag)
{
  vector<double> Bounds;
  vector<float> OrigBounds;
  double threshold;
  VVDouble ClusteringResults;
  vector<VVDouble> XYValues;
  vector<int> Indices;
  vector<double> Centroids;
  MBT_Matrix<double> FinalStatistics;

	// remove DC offset
  vector<double> DataNoDC = RemoveDCF2D(Data);
  
  // apply a bandpass filter
  vector<double> FreqBounds = {2, 30};
  vector<double> BandPassedData = BandPassFilter(DataNoDC, FreqBounds); 
 
  if (flag == 0)
  {
  	// Bounds calculation
   	Bounds = CalculateBounds(BandPassedData);
  }

  // remove outliers
  vector<double> CleanData = RemoveOutliers(BandPassedData, Bounds);

  // pass data to an MBT_Matrix
  MBT_Matrix<double> PassCleanData(1, CleanData.size());
  for (unsigned int i = 0 ; i < CleanData.size() ; i++)
  {
  	PassCleanData(0, i) = CleanData[i];
  }
    
  // signal trend correction
  MBT_Matrix<double> TrendCorrected = MBT_trendCorrection(PassCleanData, Fs);
     
  // create an object of type MBT_PWelchComputer
  MBT_PWelchComputer pWelch(TrendCorrected, Fs, "RECT"); 
   
  //create the TF map
  //get the frequencies
  vector<double> frequencies = pWelch.get_PSD(0);

  // get the powers
  vector<double> powers = pWelch.get_PSD(1);
   
  // calculate the TF Map
  MBT_Matrix<double> TFMap = TF(TrendCorrected.row(0), Fs, powers, frequencies);

  // flag = 0 means that the thresholds are computed based on the calibration
  if (flag == 0)
  {
    //calculate Contour thresholds
   	threshold = ContourThresholds(TFMap);
  }
  else
  {
    threshold = 0;
  }
   
  // calculate contourc
  MBT_Matrix<double> contourc = Contourc(TFMap, threshold);
  
  // calculate the bounds of each sub-array of contourc array
  MBT_Matrix<int> ContourMatrixBounds;
   
  if (!contourc.row(1).empty())
  	ContourMatrixBounds = C2xyz(contourc);
	
  // calculate all TF map statistics	 
  pair<VVDouble, vector<VVDouble>> pair1 = CalculateStatistics(contourc, ContourMatrixBounds, TFMap);
  ClusteringResults = pair1.first;
  XYValues          = pair1.second; 

  // apply the k-means on the distances to further group the statistics
  pair<vector<int>, vector<double>> pair2 = ApplyKMeans(ClusteringResults[8]); 
  Indices   = pair2.first;
  Centroids = pair2.second;

  // apply the final clustering on the TF map statistics 
  MBT_Matrix<double> MinMaxXValues(2, ClusteringResults[0].size());

  for (unsigned int a  = 0 ; a < ClusteringResults[0].size(); a++)
  {
    MinMaxXValues(0,a) = ClusteringResults[0][a];
    MinMaxXValues(1,a) = ClusteringResults[1][a];
  }
  
  FinalStatistics = FurtherGroupStatistics(ClusteringResults[6], MinMaxXValues, Indices, Fs);
  MBT_Matrix<float> Statistics(FinalStatistics.size().first, FinalStatistics.size().second);

  // we pass every double value to a float one
  for (int t1 = 0 ; t1 < Statistics.size().first; t1++)
  {
    for (int t2 = 0 ; t2 < Statistics.size().second; t2++)
    {
      Statistics(t1,t2) = (float) FinalStatistics(t1,t2);
    }
  }

  return Statistics;
}

int main()
{  
	  //chrono::steady_clock::time_point begin = chrono::steady_clock::now();
  
    /*///////////////////////// CHECK THE TIME FREQUENCY MAP STATISTICS ///////////////////
    //load signal
    //vector<float> original = readInput("../Files/ArduinoData1606ecVR7_Ch2.txt"); 
    vector<complex<float>> OriginalComplex = MBT_readVector("../Files/ArduinoData1606ecVR7_Ch2.txt"); 
    // skip possible NaN values present in a file
    vector<float> original = SkipNaN(OriginalComplex);
    
     // this code is used to test the overall execution of the program
     // calibration data
     vector<float>::const_iterator firstCalib = original.begin();
     vector<float>::const_iterator lastCalib  = original.begin() + original.size();
   
     vector<float> Calib(firstCalib, lastCalib);
 
     // this function takes in input 0-for calibration and 1-for session data, 
     // in the case we want to keep some statistics from the calibration and to pass them as parameters to session 
     // for the time being, we leave it as it is to read and process all file data, without keeping parameters 
     // from a part of the data
     MBT_Matrix<float> Results = runEverything(Calib, 0);
 
     // this code is used to verify the final TF-Statistics 
     // the order is: areas, timelengths, distances
     for (int k1 = 0 ; k1 < Results.size().first; k1++)
     {
       for (int k2 = 0 ; k2 < Results.size().second; k2++)
           cout << Results(k1,k2) << " ";
       cout << endl;
     }*/

    /////////////////// CHECK THE NEW PREPROCESSING PROPOSED ON 14/09/2017 ////////////////////
    // load the data
    vector<complex<float>> OriginalData = MBT_readVector("../../SNR/Files/SNRTest1.txt"); 
    // convert the input to vector<double>
    vector<double> Data = ConvertComplexFloatToDouble(OriginalData);
    // apply an IQR to check for outliers in raw data
    // a) Bounds calculation
    vector<double> OrigBounds = CalculateBounds(Data);
    // b) set all the outliers to NaN and then interpolate them
    vector<double> FixedOutliers = MBT_OutliersToNan(Data, OrigBounds);
    // apply DC Removal
    vector<double> DataNoDC = RemoveDC(FixedOutliers);
    // apply a bandpass filter
    vector<double> FreqBounds = {2, 40};
    vector<double> BandPassedData = BandPassFilter(DataNoDC, FreqBounds); 

    writeOutput(BandPassedData, "../../SNR/Files/Output.txt");

    //////////////////////////////////////////////////////////////////////////////////////////*/

    ////////////////////////////// CHECK THE SNR STATISTICS ////////////////////////////
    vector<complex<float>> ComplexSNR = MBT_readVector("../../SNR/Files/SNRTest9.txt");   
  
    // convert from complex float to float
    vector<float> SNR;
    for (unsigned int a = 0 ; a < ComplexSNR.size(); a++)
        SNR.push_back(real(ComplexSNR[a]));

    SNR_Statistics obj(SNR);
    float ExerciseThreshold = 0.95f;

    map<string, float> SNR_Statistics = obj.CalculateSNRStatistics(SNR, ExerciseThreshold);

    for (const auto &p : SNR_Statistics) 
    {
      cout << p.first << " = " << p.second << '\n';
    }

    // session data
    /*vector<double>::const_iterator firstSession = original.begin() + original.size();
    vector<double>::const_iterator lastSession = original.end();
    vector<double> Session(firstSession, lastSession);
    runEverything(Session, 1);
    
    chrono::steady_clock::time_point end = chrono::steady_clock::now();
    cout << endl;

    // we measure the time here in milliseconds
    //cout << chrono::duration_cast<chrono::microseconds>(end - begin).count() << endl;*/
    return 0;
}






