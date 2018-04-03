//
// MBT_Bandpass.h
//
// Created by Katerina Pandremmenou on 20/10/2016
// Copyright (c) 2016 myBrain Technologies. All rights reserved.
//
// Update: 08/12/2016 by Katerina Pandremmenou (double-->float)
//         13/02/2017 by Fanny Grosselin (allow to put the frequency bounds in input)

#ifndef MBT_BANDPASS_H
#define MBT_BANDPASS_H

using namespace std;

//define here the sampling frequency
#define Fs 250

#define Fnorm Fs/2

// the function that applies the bandpass filter to the data
//vector<float> BandPassFilter(vector<float>);
vector<float> BandPassFilter(vector<float> RawSignal, vector<float> freqBounds);

class BandPass
{
	public:
		//BandPass();
		BandPass(vector<float> freqBounds); // Fanny Grosselin 13/02/2017
		~BandPass();
		//const float HighPass = 2.0;
		float HighPass; // Fanny Grosselin 13/02/2017
		//const float LowPass = 30.0;
		float LowPass; // Fanny Grosselin 13/02/2017
		float HighStop, LowStop;

		// this function mirrors the signal
		vector<float> MirrorSignal(vector<float>);
		// FIR arbitrary shape filter design using the frequency sampling method
		vector<float> fir2(int, vector<float>, vector<int>);


	private:
	    // it creates a hamming window
        vector<float> hamming(int);

};

#endif // MBT_BANDPASS_H
