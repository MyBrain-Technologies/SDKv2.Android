// 
// MBT_TF_map.cpp
// Created by Katerina Pandremmenou on 06/06/2016.
// Copyright (c) 2016 Katerina Pandremmenou. All rights reserved
//
// Update on 09/12/2016 by Katerina Pandremmenou (no Armadillo, double-->float, no Boost)
// Update on 10/03/2017 by Katerina Pandremmenou (use of the new name for the convolution (fft_convolve))
// Update on 20/03/2017 by Katerina Pandremmenou (delete the map object)
// Update on 03/04/2017 by Katerina Pandremmenou (convert everything from float to double, remove +1 on the upper bound of max_element, line 196)
// Update on 05/04/2017 by Katerina Pandremmenou (Fix all the warnings)
// Update on 19/09/2017 by Katerina Pandremmenou (Change all implicit type castings to explicit ones)

#include <complex>
#include <cmath>
#include <iomanip>
#include <iostream>
#include <vector>
#include <algorithm>
#include <math.h>
#include <ctime>
#include <chrono>
#include "../Headers/MBT_TF_map.h"

using namespace std;

TF_map::TF_map() 
{
    ;
}

TF_map::~TF_map()
{
    ;
}

MBT_Matrix<double> TF(vector<double> signal, const double fs, vector<double> powers, vector<double> frequencies)
{
    const double fc = 1;
    const double Ts = 1/fs;
    const double FWHM_tc = 3;
    int precision = 3;
    const double sigma_tc = FWHM_tc / sqrt(8*log(2));
    ComplexDouble i = -1, tmp;
    i = sqrt(i);

    // calculate the frequency peak
	int freqPeak = get_freqPeak(powers, frequencies);
	   
    vector<double> freqs(5);
    int counter = 0, j =0;
	fill(freqs.begin(), freqs.end(), counter++); // elements are set to 1
			        	
    vector<double> DoubleRes(signal.size());
    vector<double> Imag(signal.size()); // create an imaginary part
    MBT_Matrix<int> Mask;
    
    TF_map *map = new TF_map();

	int signal_size = signal.size(); 
	int nscales = freqs.size() + 10; // the maximum frequency we can estimate is at 15 Hz
    vector<double> sigma_t(nscales);
    CDVector Kernels[nscales], ConvOutPut, DoubleToComplexDouble;
    DoubleToComplexDouble.reserve(signal.size());

    // convert the vector<double> signal to vector<complex<double>>
    transform(begin(signal), end(signal), begin(Imag), back_inserter(DoubleToComplexDouble), [](double r, double i) 
    { 
      	return complex<double>(r, i); 
    });
    
    // calculates the TF mask
    Mask = TF_mask(signal_size, freqPeak);
    
    // this code is used to print the TF mask
    /*for (int k1 = 0; k1 < Mask.size().first; k1++)
    {
    	for (int k2 = 0 ; k2 < Mask.size().second; k2++)
    	{
    		cout << Mask(k1,k2) << " ";
    	}
    	cout << endl;
    }*/
    
	MBT_Matrix<double> TFFinal(Mask.size().first, Mask.size().second);
    
    //calculates wavelet kernels for each scale
    // here we consider the frequencies from 7 Hz to 13 Hz
	for (int k = freqPeak-2 ; k <= freqPeak+2; k++) //(int k=nscales-4; k<=nscales; k++)
    { 
        vector<int> extractedRow = Mask.row(j);
   
    	sigma_t[k-1] = sigma_tc / k;
    	double lbound = - precision * sigma_t[k-1];
    	double ubound = precision * sigma_t[k-1];
    	
    	for (double j=lbound; j<=ubound; j+=Ts)
    	{
    	    tmp =  (ComplexDouble)( sqrt(k) * ( pow((sigma_tc * sqrt(M_PI)), (-0.5)) ) * exp( -(pow((k*j),2)) / (2 * pow(sigma_tc,2)) )) * exp(2*M_PI*i*fc*(k*j));
    	    Kernels[k-1].push_back(tmp); 	
    	}
    	
        // this code is used to measure the execution time required for the convolution
    	// chrono::steady_clock::time_point begin = chrono::steady_clock::now();
    	// perform the convolution between the signal and the kernel
    	ConvOutPut = fft_convolve(DoubleToComplexDouble,Kernels[k-1]);
    	//chrono::steady_clock::time_point end = chrono::steady_clock::now();
        //cout << chrono::duration_cast<chrono::milliseconds>(end - begin).count() << endl;
    
    	for (unsigned int q = 0 ; q < ConvOutPut.size(); q++)
    	{
    		DoubleRes[q] = pow((abs(ConvOutPut[q]) * Ts),2);
    		
    		// Apply the TF mask
    		TFFinal(j,q) = DoubleRes[q] * extractedRow[q];
    	}
    	j = j + 1;
    }
     
    // this code is used just to print the TF map
    /*for (int i1 = 0; i1 < TFFinal.size().first ; i1++)
    {
    	for (int i2 = 0; i2 < TFFinal.size().second ; i2++)
    	{
    		cout << TFFinal(i1,i2) << " ";
    	}
    	cout << endl;
    }*/

    delete(map);
    return TFFinal;   
}

MBT_Matrix<int> TF_mask(int signal_size, int freqPeak)
{
	// keep edge points for 5 frequencies, centered around the frequency peak
	// -5 is used to make the correspondence with the vector of AllEdgeEffects
	int Start_BegEdgePoint = freqPeak - 2 - 5;
	vector<int> BegEdgePoints(5); 
	
	for (int step = 0; step < 5; step++)
	{
		BegEdgePoints[step] = AllEdgePoints[Start_BegEdgePoint];
		Start_BegEdgePoint = Start_BegEdgePoint + 1;
	}
	
	vector<int> EndEdgePoints(5);
	MBT_Matrix<int> Mask(5,signal_size);
    int num;
    unsigned int i, j;
	
	for (i = 0; i < BegEdgePoints.size(); i++) 
	{
	    num = BegEdgePoints[i];
	    EndEdgePoints[i] = signal_size - num;
	    
	    vector<int> tempoZerosBeg(num);
	    vector<int> tempoOnes(signal_size-2*num);
	    vector<int> tempoZerosEnd(num);
	    fill(tempoOnes.begin(), tempoOnes.end(), 1);
	    
	    tempoZerosBeg.insert(tempoZerosBeg.end(), tempoOnes.begin(), tempoOnes.end());
	    tempoZerosBeg.insert(tempoZerosBeg.end(), tempoZerosEnd.begin(), tempoZerosEnd.end());
	    
	    for (j = 0 ; j < tempoZerosBeg.size(); j++)
	    {
	    	Mask(i,j) = tempoZerosBeg[j];
	    }
	}			
	return Mask;
}

int get_freqPeak(vector<double> powers, vector<double> frequencies)
{
    int ind1 = 0, ind2 = 0;
    int flag = 0;
      
    // find the index with frequency equal to 7 Hz
    for (unsigned int k = 0 ; k < frequencies.size(); k++)
    {
        // the first appearance of 7
    	if ((round(frequencies[k]) == 7) && (flag == 0))
        {
        	ind1 = k;
        	flag = 1;
        }
        // the last appearance of 13
        else if (round(frequencies[k]) == 13)
        {
        	ind2 = k;
        }
    }
    
    // get the index of the maximum power between ind1 and ind2
    int pos = max_element(powers.begin()+ind1, powers.begin()+ind2) - powers.begin();
       
    // get the frequency that corresponds to the maximum power
    int freqPeak = (int) round(frequencies[pos]);

	return freqPeak;
}

bool great(double value)
{
	return (value>0.0);
}
		
vector<double> MinMaxTFMap(MBT_Matrix<double> TFMap)
{
	vector<double> MinMaxValues(2);
	vector<double> Row(TFMap.size().first);
	vector<double> MaxValues(TFMap.size().first), MinValues(TFMap.size().first);
	double Maximum = 0.0, Minimum = 0.0;
	
    vector<double>::const_iterator result;

	for (int dim1 = 0 ; dim1 < TFMap.size().first; dim1++)
	{
	    Row = TFMap.row(dim1);
	    sort(Row.begin(), Row.end());
	    
	    // save max value
	    MaxValues[dim1] = Row[Row.size()-1];

		// find the value just after zero
	    result = find_if(Row.begin(), Row.end()+1, great);
	    // save min value (after zero)
	    MinValues[dim1] = *(result);
	}
	
	for (unsigned int p = 0 ; p < MinValues.size(); p++)
	{
		Maximum = *std::max_element(MaxValues.begin(), MaxValues.end());
	 	Minimum = *std::min_element(MinValues.begin(), MinValues.end());
	}
	
	MinMaxValues = {Minimum, Maximum};
	return MinMaxValues;
}




































