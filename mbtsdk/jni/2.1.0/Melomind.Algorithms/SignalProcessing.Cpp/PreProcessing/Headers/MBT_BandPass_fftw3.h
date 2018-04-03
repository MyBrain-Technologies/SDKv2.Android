//
// MBT_Bandpass.h
//
// Created by Katerina Pandremmenou on 20/10/2016
// Copyright (c) 2016 myBrain Technologies. All rights reserved.
//
// Update: 08/12/2016 by Katerina Pandremmenou (double-->double)
//         13/02/2017 by Fanny Grosselin (allow to put the frequency bounds in input)
//         07/03/2017 by Katerina Pandremmenou (change in the protocol of BandPassFilter function)
// 		   14/03/2017 by Katerina Pandremmenou (inclusion of the header file of the new file that includes the class that uses the fftw3 functions)
// 	 	   23/03/2017 by Fanny Grosselin (Change float by double for the functions not directly used by Andro?. For the others, keep inputs and outputs in float, but do the steps with double or create two functions : one with only float, another one with only double)

#ifndef MBT_BANDPASS_H
#define MBT_BANDPASS_H

#include <math.h>
#include <vector>
#include <complex>
#include <iostream>
#include <algorithm>    // std::reverse
#include <numeric>
#include <iomanip>
#include "../../Transformations/Headers/MBT_Fourier_fftw3.h"

using namespace std;

typedef complex <double> ComplexDouble;
typedef vector <ComplexDouble> CDVector;
const double  PI_F=3.14159265358979f;

//define here the sampling frequency
#define Fs 250

#define Fnorm Fs/2

// the function that applies the bandpass filter to the data
//vector<double> BandPassFilter(vector<double>);
vector<float> BandPassFilter(vector<float> tmp_RawSignal, vector<float> tmp_freqBounds);
vector<double> BandPassFilter(vector<double> RawSignal, vector<double> freqBounds);

class BandPass
{
	public:
		//BandPass();
		BandPass(vector<double> freqBounds); // Fanny Grosselin 13/02/2017
		~BandPass();
		//const double HighPass = 2.0;
		double HighPass; // Fanny Grosselin 13/02/2017
		//const double LowPass = 30.0;
		double LowPass; // Fanny Grosselin 13/02/2017
		double HighStop, LowStop;

		// this function mirrors the signal
		vector<double> MirrorSignal(vector<double>);
		// FIR arbitrary shape filter design using the frequency sampling method
		vector<double> fir2(int, vector<double>, vector<int>);

	private:
	    // it creates a hamming window
    	vector<double> hamming(int);
};

#endif // MBT_BANDPASS_H
