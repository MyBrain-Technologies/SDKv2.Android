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
// 	 	   23/03/2017 by Fanny Grosselin (Change float by double for the functions not directly used by Andro?. For the others, keep inputs and outputs in float, but do the steps with SP_RealType or create two functions : one with only float, another one with only double)

#ifndef MBT_BANDPASS_H
#define MBT_BANDPASS_H

#include <sp-global.h>

#include "Transformations/MBT_Fourier_fftw3.h"

#include <math.h>
#include <vector>
#include <complex>
#include <iostream>
#include <algorithm>    // std::reverse
#include <numeric>
#include <iomanip>

using namespace std;

typedef SP_Complex ComplexDouble;
typedef SP_ComplexVector CDVector;

//define here the sampling frequency
#define Fs 250

#define Fnorm Fs/2

// the function that applies the bandpass filter to the data
//SP_Vector BandPassFilter(SP_Vector);
#ifndef SP_FLOAT_OR_NOT_LEGACY
SP_FloatVector BandPassFilter(SP_FloatVector tmp_RawSignal, SP_FloatVector tmp_freqBounds);
#endif
SP_Vector BandPassFilter(SP_Vector RawSignal, SP_Vector freqBounds);

class BandPass
{
	public:
		//BandPass();
		BandPass(SP_Vector freqBounds); // Fanny Grosselin 13/02/2017
		~BandPass();
		//const SP_RealType HighPass = 2.0;
		SP_RealType HighPass; // Fanny Grosselin 13/02/2017
		//const SP_RealType LowPass = 30.0;
		SP_RealType LowPass; // Fanny Grosselin 13/02/2017
		SP_RealType HighStop, LowStop;

		// this function mirrors the signal
		SP_Vector MirrorSignal(SP_Vector);
		// FIR arbitrary shape filter design using the frequency sampling method
		SP_Vector fir2(int, SP_Vector, vector<int>);

	private:
	    // it creates a hamming window
    	SP_Vector hamming(int);
};

#endif // MBT_BANDPASS_H
