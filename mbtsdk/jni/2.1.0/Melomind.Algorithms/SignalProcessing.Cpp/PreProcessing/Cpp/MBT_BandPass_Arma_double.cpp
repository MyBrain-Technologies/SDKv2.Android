//
// MBT_BandPass_Arma_double.cpp
//
// Created by Katerina Pandremmenou on 10/10/2016
// Copyright (c) 2016 myBrain Technologies. All rights reserved.
// Updated by Katerina Pandremmenou on 10/03/2017 (Change everything internally to double, except for the input and output types of the BandPassFilter function)

#include <math.h>  
#include <vector>
#include <complex>
#include <iostream>
#include <algorithm>    
#include <numeric>
#include <iomanip>
#include <boost/iterator/counting_iterator.hpp>
#include <boost/config.hpp>
#include "../Headers/MBT_BandPass_fftw3.h"
#include <armadillo>

using namespace std;
using namespace arma;

typedef complex <double> ComplexDouble;
typedef vector <ComplexDouble> CDVector;

BandPass::BandPass(vector<float> freqBounds)
{   
    HighPass = freqBounds[0]; //  Fanny Grosselin 13/02/2017
    LowPass = freqBounds[1]; //  Fanny Grosselin 13/02/2017
    HighStop = HighPass - min(2.0, HighPass / 2.0);
    LowStop = LowPass + min(10.0, LowPass * 0.2);
    
    if ((LowStop > Fnorm) || (LowPass > Fnorm) || (HighStop > Fnorm) || (HighPass > Fnorm))
    {
    	cout << "Cutoff frequencies are too high with respect to the sampling frequency." << endl;
    	exit(0);
    }
}

BandPass::~BandPass() 
{
	;
}

vector<double> BandPass::MirrorSignal(vector<float> RawSignal)
{
     vector<double> Mirrored1, CopyTmp;
     vector<double> tmp(RawSignal.begin(), RawSignal.end());
    
     // reverse the signal
     reverse(tmp.begin(), tmp.end());
     
	 // keep a copy of reversed signal
	 CopyTmp = tmp;
	 
	 // keep a copy of size (reversed signal - 1)
	 vector<double>::const_iterator first = CopyTmp.begin();
     vector<double>::const_iterator last = CopyTmp.begin() + CopyTmp.size() - 1;
     vector<double> CopyTmp2(first, last);
	 
	 // put RawSignal at the end of the reversed signal
	 // it is stored in variable "tmp"
	 move(RawSignal.begin(), RawSignal.end(), back_inserter(tmp));
	 
	 // put the reversed signal at the end of the aforementioned concatenated signal 
	 if (remainder(RawSignal.size(), 2))
	 {
	 	move(CopyTmp2.begin(), CopyTmp2.end(), back_inserter(tmp));
	 }
	 else
	 {
		move(CopyTmp.begin(), CopyTmp.end(), back_inserter(tmp));
	 }
	
	 return tmp;
}

vector<double> BandPass::hamming(int nn)
{
	vector<double> window;
    int nn2 = nn;

	if (nn2 == 1)
	{
		window = {1};
	}
	else 
	{
		nn2 = nn2 - 1;  
    	// create a vector from 0 to nn
    	vector<double> seq( boost::counting_iterator<unsigned int>(0), boost::counting_iterator<unsigned int>(nn2)); 
    
    	transform(seq.begin(), seq.end(), seq.begin(), bind2nd(multiplies<double>(), M_PI));    
    	transform(seq.begin(), seq.end(), seq.begin(), bind2nd(multiplies<double>(), 2.0));
    	transform(seq.begin(), seq.end(), seq.begin(), bind2nd(divides<double>(), nn2));
    
    	for (int k = 0 ; k < seq.size(); k++)
    	{
    		window.push_back(cos(seq[k]));
    	}
    
    	transform(window.begin(), window.end(), window.begin(), bind2nd(multiplies<double>(), 0.46)); 
    	transform(window.begin(), window.end(), window.begin(), bind1st(minus<double>(), 0.54));
	}    
    	return window;
}

// this function returns exactly the same results as the matlab's fir2 function
vector<double> BandPass::fir2(int MirroredSize, vector<double> Params, vector<int> CutOnOff)
{
    vector<double> wind; 
    int nbrk = Params.size();
    vector<double> df(nbrk);
    int nn = MirroredSize + 1;
    int npt, lap, nint, nb, ne, j;
    double dt;
    complex<double> sqrt_minus_1;
    
    if (nn < 1024)
    {
        npt = 512;
    }
    else
    {
        npt = pow(2.0, ceil(log(nn)/log(2)));
    }
    
    wind = hamming(nn);
    lap = static_cast<int> (npt/25);
    
    
    nbrk = Params.size();
    Params[0] = 0;
    Params[nbrk-1] = 1;
     
    // we are interested between df[1] ~ df[end], not on df[0]
    adjacent_difference(Params.begin(), Params.end(), df.begin());
    npt = npt + 1;
    nb = 1;
    
    // interpolate breakpoints onto large grid    
    vector<double> inc(npt, 0.0);
    CDVector H(npt);
    CDVector rad(npt);
    nint = nbrk - 1;
    
	H[0] = CutOnOff[0];
     
    for (int mes = 1; mes <= nint; mes++)
    {    
    	if (df[mes] == 0)
    	{
    		nb = ceil(nb - lap/2);
    		ne = nb + lap;
    	}
    	else
    	{
    		ne = static_cast<int> (Params[mes]*npt);
    	}
    	
    	if ((nb < 0) || (ne > npt))
    	{
    		cout << "signal:fir2:SignalErr" << endl;
    		exit(0);
    	}
    	
    	for (j = nb; j <= ne ; j++)
    	{
    		if (nb == ne)
    		{
    			inc[j-1] = 0.0;
    			H[j-1] = inc[j-1] * CutOnOff[mes] + ( (1 - inc[j-1]) * CutOnOff[mes-1]);
    		}
    		else
    		{
    			inc[j-1] = (double) (j-nb) / (ne-nb);
    			H[j-1] = inc[j-1] * CutOnOff[mes] + ( (1 - inc[j-1]) * CutOnOff[mes-1]);
    		}
    	}
    	nb = ne + 1;
    }
    
    // Fourier time-shift
    dt = 0.5 * (nn - 1);
    sqrt_minus_1 = sqrt(complex<double>(-1));
    complex<double> intermediate;
    intermediate = -dt * sqrt_minus_1 * M_PI;
    
    for (int pt = 0; pt < npt; pt++) //(int pt = 0; pt < npt; pt++)
    {
    	rad[pt] = intermediate * complex<double>(pt) / complex<double> (npt-1);
    	H[pt] = H[pt] * exp(rad[pt]);
    }
    
    CDVector HRev;
    HRev = H;
    // reverse the signal
    reverse(HRev.begin(), HRev.end());
    
	// keep the signal of size (reversed signal-1 : 2)
	CDVector::const_iterator first = HRev.begin() + 1;
    CDVector::const_iterator last = HRev.begin() + HRev.size() - 1;
    CDVector HRev2(first,last);
    
    for (int q = 0 ; q < HRev2.size(); q++)
    {
    	HRev2[q] = conj(HRev2[q]);
    }
      
    // put HRev2 at the end of H
	// it is stored in H
	move(HRev2.begin(), HRev2.end(), back_inserter(H));	

	// first way using Armadillo
	cx_vec Q2(H);

	//results exactly the same with matlab
	cx_vec Y = ifft(Q2);
	
	vector<double> b(nn);
	// take the real part of the fourier coefficients
	for (int q = 0; q < nn; q++)
	{
		b[q] = real(Y[q]) * wind[q];
	}
	
	return b;
}

vector<float> BandPassFilter(vector<float> RawSignal, vector<float> freqBounds)
{
	vector<double> BandPassedData;
	vector<double> Mirrored, H;
	int MirroredSize;
	
	BandPass *filter = new BandPass(freqBounds);
	Mirrored = filter->MirrorSignal(RawSignal);
    MirroredSize = Mirrored.size();
    
    vector<double> Params = {0, filter->HighStop, filter->HighPass, filter->LowPass, filter->LowStop, Fnorm};
	vector<int> CutOnOff = {0, 0, 1, 1, 0, 0};
    
    transform(Params.begin(), Params.end(), Params.begin(), bind2nd(divides<double>(), Fnorm));
    
    H = filter->fir2(MirroredSize-1, Params, CutOnOff);
    
    // convert from vector<double> to <vec>
    vec K = conv_to<vec>::from(H);
    cx_vec Q = fft(K);
    
    //Not exactly the same results with Matlab here
    vec M = abs(Q);
    
    int MSize = M.n_elem;
    
    if (filter->HighPass != 0)
    {
    	M(0) = 0;
    	M(MSize-1) = 0;
    }
      
    // convert from vector<double> to <vec>
    vec MirroredHelp = conv_to<vec>::from(Mirrored);
    //results almost similar to those of matlab
    cx_vec IntermediateResult = fft(MirroredHelp) % M; 
    
    cx_vec FinalResult = ifft(IntermediateResult);
     
    // results just the same as matlab at this point
    vec RealFinalResultTmp = real(FinalResult);
    
    fvec RealFinalResult = conv_to<fvec>::from(RealFinalResultTmp);

    vector<float> RealResult = conv_to<vector<float>>::from(RealFinalResult);
    
    // keep only the initial part of the signal (the central part)
    vector<float>::const_iterator firstpoint = RealResult.begin() + RawSignal.size();
    vector<float>::const_iterator lastpoint = RealResult.begin() + 2*RawSignal.size();
    vector<float> CentralPart(firstpoint, lastpoint);

    return CentralPart;
}














