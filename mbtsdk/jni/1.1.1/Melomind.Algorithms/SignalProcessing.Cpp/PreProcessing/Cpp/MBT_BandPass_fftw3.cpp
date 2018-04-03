// MBT_BandPass_fftw3.cpp
//
// Created by Katerina Pandremmenou on 20/10/2016
// Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
// Update on 09/12/2016 by Katerina Pandremmenou (double-->double)
//           13/02/2017 by Fanny Grosselin (allow to put the frequency bounds in input)
// 			 14/02/2017 by Katerina Pandremmenou (a small bug in line 125, this bracket was earlier closing on line 128)
//           07/03/2017 by Katerina Pandremmenou
//           a) a bug in hamming function fixed
//           b) inclusion of the fftw3 library for forward and inverse fourier transforms
//           c) change all variables inside this file to doubles, except for the type of the input and output data of BandPass filter function
// Update on 14/03/2017 by Katerina Pandremmenou (use the class based on fftw3 library for applying fourier transforms)
// Update on 20/03/2017 by Katerina Pandremmenou
//         a) remove the free of the FFT-related pointers, as they get free in the desctructor
//         b) initialize the vectors (when it is possible) with a predefined size and replace the use of push_back with an assignment
// Update on 23/03/2017 by Fanny Grosselin (Change float by double for the functions not directly used by Andro�d. For the others, keep inputs and outputs in float, but do the steps with double or create two functions : one with only float, another one with only double)
// Update on 24/03/2017 by Fanny Grosselin (Use DOUBLE_FFTW_C2C_1D instead of using DOUBLE_FFTW_R2C_1D in the BandPassFilter because with DOUBLE_FFTW_R2C_1D we have a problem of allocation)
// Update on 27/03/2017 by Fanny Grosselin (Fix all the warnings.)
// Update on 22/05/2017 by Fanny Grosselin (Fix memory leaks due to creation of object without deleting.)
// Update on 19/09/2017 by Katerina Pandremmenou : Change all implicit type castings to explicit ones 

#include <math.h>
#include <vector>
#include <complex>
#include <fftw3.h>
#include <iostream>
#include <algorithm>
#include <numeric>
#include <iomanip>
#include <fstream>
#include "../Headers/MBT_BandPass_fftw3.h"

using namespace std;

typedef complex <double> ComplexDouble;
typedef vector <ComplexDouble> CDVector;

//BandPass::BandPass()
BandPass::BandPass(vector<double> freqBounds)
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

vector<double> BandPass::MirrorSignal(vector<double> RawSignal)
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
    int nn2 = nn;
    vector<double> window(nn2);

	if (nn2 == 1)
	{
		window = {1};
	}
	else
	{
		nn2 = nn2 - 1;
    	// create a vector from 0 to nn
    	vector<double> seq(nn2);
		iota (begin(seq), end(seq), 0);

    	transform(seq.begin(), seq.end(), seq.begin(), bind2nd(multiplies<double>(), PI_F));
    	transform(seq.begin(), seq.end(), seq.begin(), bind2nd(multiplies<double>(), 2.0));
    	transform(seq.begin(), seq.end(), seq.begin(), bind2nd(divides<double>(), nn2));

    	for (unsigned int k = 0 ; k < seq.size(); k++)
    	{
            window[k] = cos(seq[k]);
    	}

    	transform(window.begin(), window.end(), window.begin(), bind2nd(multiplies<double>(), 0.46));
    	transform(window.begin(), window.end(), window.begin(), bind1st(minus<double>(), 0.54));
	}
    	return window;
}

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
        npt = (int) pow(2.0, ceil(log(nn)/log(2)));
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
    		nb = (int) ceil(nb - lap/2);
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
    intermediate = -dt * sqrt_minus_1 * double(PI_F);

    for (int pt = 0; pt < npt; pt++)
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

    for (unsigned int q = 0 ; q < HRev2.size(); q++)
    {
    	HRev2[q] = conj(HRev2[q]);
    }

    // put HRev2 at the end of H
	// it is stored in H
	move(HRev2.begin(), HRev2.end(), back_inserter(H));

    // inverse fourier transform (complex to complex)
    DOUBLE_FFTW_C2C_1D *obj = new DOUBLE_FFTW_C2C_1D(H.size(), H, 1);
    std::vector<complex<double> > IFFTH(H.size());
    IFFTH = obj->ifft_execute();
    delete obj;

    vector<double> b(nn);

    // take the real part of the fourier coefficients
    for (int q = 0; q < nn; q++)
    {
        b[q] = real(IFFTH[q]) * wind[q];
    }

	return b;
}

vector<float> BandPassFilter(vector<float> tmp_RawSignal, vector<float> tmp_freqBounds)
{
    vector<double> RawSignal(tmp_RawSignal.begin(), tmp_RawSignal.end());
    vector<double> freqBounds(tmp_freqBounds.begin(), tmp_freqBounds.end());
	vector<double> Mirrored;
	int MirroredSize;

	BandPass *filter = new BandPass(freqBounds); // Fanny Grosselin 13/02/2017
	Mirrored = filter->MirrorSignal(RawSignal);
    MirroredSize = Mirrored.size();

    vector<double> Params = {0, filter->HighStop, filter->HighPass, filter->LowPass, filter->LowStop, Fnorm};
	vector<int> CutOnOff = {0, 0, 1, 1, 0, 0};

    transform(Params.begin(), Params.end(), Params.begin(), bind2nd(divides<double>(), Fnorm));

    vector<double> H(MirroredSize);
    H = filter->fir2(MirroredSize-1, Params, CutOnOff);

    // apply the Forward Fourier Transform
	// Complex to Complex
    //DOUBLE_FFTW_R2C_1D *trans1 = new DOUBLE_FFTW_R2C_1D(H.size(), H);
    CDVector tmp_H;
    for (unsigned int ki=0;ki<H.size();ki++)
    {
        tmp_H.push_back(complex<float>((float) H[ki], 0));
    }
    int HSize = H.size();
    DOUBLE_FFTW_C2C_1D *trans1 = new DOUBLE_FFTW_C2C_1D(HSize, tmp_H,-1);
    std::vector<complex<double> > Vec1(HSize);
    Vec1 = trans1 -> fft_execute();
    delete trans1;

    vector<double> allRealPart(H.size());

    for (unsigned int q = 0; q < Vec1.size(); q++)
    {
         allRealPart[q] = sqrt((real(Vec1[q])*real(Vec1[q])) + (imag(Vec1[q]) * imag(Vec1[q])));
    }

    int SizeHhelp = allRealPart.size();

    if (filter->HighPass != 0)
    {
    	allRealPart[0] = 0;
    	allRealPart[SizeHhelp-1] = 0;
    }

	// apply the Forward Fourier Transform
    // Complex to Complex
    //DOUBLE_FFTW_R2C_1D *trans2 = new DOUBLE_FFTW_R2C_1D(Mirrored.size(), Mirrored);
    std::vector<std::complex<double> > tmp_Mirrored;
    for (unsigned int ki=0;ki<Mirrored.size();ki++)
    {
        tmp_Mirrored.push_back(std::complex<float>((float) Mirrored[ki], 0));
    }
    DOUBLE_FFTW_C2C_1D *trans2 = new DOUBLE_FFTW_C2C_1D(Mirrored.size(), tmp_Mirrored,-1);
    std::vector<complex<double> > Vec2(Mirrored.size());
    Vec2 = trans2 -> fft_execute();
    delete trans2;

    std::vector<complex<double> > IntermediateResult(Vec2.size());
	for (unsigned int a = 0 ; a < Vec2.size(); a++)
	{
		IntermediateResult[a] = Vec2[a] * allRealPart[a];
	}

    // inverse fourier transform (complex to complex)
    DOUBLE_FFTW_C2C_1D *obj = new DOUBLE_FFTW_C2C_1D(IntermediateResult.size(), IntermediateResult, 1);
    std::vector<complex<double> > FinalResult(IntermediateResult.size());
    FinalResult = obj->ifft_execute();
    delete obj;


    vector<double> RealFinalResult(FinalResult.size());

    for (unsigned int q = 0; q < FinalResult.size(); q++)
    {
        RealFinalResult[q] = real(FinalResult[q]);
    }

    // keep only the initial part of the signal (the central part)
    vector<double>::const_iterator firstpoint = RealFinalResult.begin() + RawSignal.size();
    vector<double>::const_iterator lastpoint = RealFinalResult.begin() + 2*RawSignal.size();
    vector<float> CentralPart(firstpoint, lastpoint);

    delete filter;

    return CentralPart;
}

vector<double> BandPassFilter(vector<double> RawSignal, vector<double> freqBounds)
{
	vector<double> Mirrored;
	int MirroredSize;

	BandPass *filter = new BandPass(freqBounds); // Fanny Grosselin 13/02/2017
	Mirrored = filter->MirrorSignal(RawSignal);
    MirroredSize = Mirrored.size();

    vector<double> Params = {0, filter->HighStop, filter->HighPass, filter->LowPass, filter->LowStop, Fnorm};
	vector<int> CutOnOff = {0, 0, 1, 1, 0, 0};

    transform(Params.begin(), Params.end(), Params.begin(), bind2nd(divides<double>(), Fnorm));

    vector<double> H(MirroredSize);
    H = filter->fir2(MirroredSize-1, Params, CutOnOff);

    // apply the Forward Fourier Transform
	// Complex to Complex
	//DOUBLE_FFTW_R2C_1D *trans1 = new DOUBLE_FFTW_R2C_1D(H.size(), H);
    std::vector<std::complex<double> > tmp_H;
    for (unsigned int ki=0;ki<H.size();ki++)
    {
        tmp_H.push_back(std::complex<float>((float) H[ki], 0));
    }
    DOUBLE_FFTW_C2C_1D *trans1 = new DOUBLE_FFTW_C2C_1D(H.size(), tmp_H,-1);
    std::vector<complex<double> > Vec1(H.size());
    Vec1 = trans1 -> fft_execute();
    delete trans1;

    vector<double> allRealPart(H.size());

    for (unsigned int q = 0; q < Vec1.size(); q++)
    {
         allRealPart[q] = sqrt((real(Vec1[q])*real(Vec1[q])) + (imag(Vec1[q]) * imag(Vec1[q])));
    }

    int SizeHhelp = allRealPart.size();

    if (filter->HighPass != 0)
    {
    	allRealPart[0] = 0;
    	allRealPart[SizeHhelp-1] = 0;
    }

	// apply the Forward Fourier Transform
    // Complex to Complex
    //DOUBLE_FFTW_R2C_1D *trans2 = new DOUBLE_FFTW_R2C_1D(Mirrored.size(), Mirrored);
    std::vector<std::complex<double> > tmp_Mirrored;
    for (unsigned int ki=0;ki<Mirrored.size();ki++)
    {
        tmp_Mirrored.push_back(std::complex<float>((float) Mirrored[ki], 0));
    }
    DOUBLE_FFTW_C2C_1D *trans2 = new DOUBLE_FFTW_C2C_1D(Mirrored.size(), tmp_Mirrored,-1);
    std::vector<complex<double> > Vec2(Mirrored.size());
    Vec2 = trans2 -> fft_execute();
    delete trans2;

    std::vector<complex<double> > IntermediateResult(Vec2.size());
	for (unsigned int a = 0 ; a < Vec2.size(); a++)
	{
		IntermediateResult[a] = Vec2[a] * allRealPart[a];
	}

    // inverse fourier transform (complex to complex)
    DOUBLE_FFTW_C2C_1D *obj = new DOUBLE_FFTW_C2C_1D(IntermediateResult.size(), IntermediateResult, 1);
    std::vector<complex<double> > FinalResult(IntermediateResult.size());
    FinalResult = obj->ifft_execute();
    delete obj;


    vector<double> RealFinalResult(FinalResult.size());

    for (unsigned int q = 0; q < FinalResult.size(); q++)
    {
        RealFinalResult[q] = real(FinalResult[q]);
    }

    // keep only the initial part of the signal (the central part)
    vector<double>::const_iterator firstpoint = RealFinalResult.begin() + RawSignal.size();
    vector<double>::const_iterator lastpoint = RealFinalResult.begin() + 2*RawSignal.size();
    vector<double> CentralPart(firstpoint, lastpoint);

    delete filter;

    return CentralPart;
}


