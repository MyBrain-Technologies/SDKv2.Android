//
// MBT_BandPass.cpp
//
// Created by Katerina Pandremmenou on 20/10/2016
// Copyright (c) 2016 myBrain Technologies. All rights reserved.
//
// Update on 09/12/2016 by Katerina Pandremmenou (double-->float)
//           13/02/2017 by Fanny Grosselin (allow to put the frequency bounds in input)
// 			 14/02/2017 by Katerina Pandremmenou (a small bug in line 125, this bracket was earlier closing on line 128)


#include <math.h>
#include <vector>
#include <complex>
#include <iostream>
#include <algorithm>    // std::reverse
#include <numeric>
#include "../Headers/MBT_BandPass.h"
//#include "../Headers/MBT_TF_Stats.h"
#include "../../Transformations/Headers/MBT_Fourier.h"

using namespace std;

typedef complex <float> ComplexFloat;
typedef vector <ComplexFloat> CFVector;

//BandPass::BandPass()
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

vector<float> BandPass::MirrorSignal(vector<float> RawSignal)
{
    vector<float> Mirrored1, tmp(RawSignal), CopyTmp;

     // reverse the signal
     reverse(tmp.begin(), tmp.end());

	 // keep a copy of reversed signal
	 CopyTmp = tmp;

	 // keep a copy of size (reversed signal - 1)
	 vector<float>::const_iterator first = CopyTmp.begin();
     vector<float>::const_iterator last = CopyTmp.begin() + CopyTmp.size() - 1;
     vector<float> CopyTmp2(first, last);

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

vector<float> BandPass::hamming(int nn)
{
    const float  PI_F=3.14159265358979f;
	vector<float> window;
    int nn2 = nn;

	if (nn2 == 1)
	{
		window = {1};
	}
	else
	{
		nn2 = nn2 - 1;
    	// create a vector from 0 to nn
    	vector<float> seq(nn2);
    	int counter = 0;
		std::fill(seq.begin(), seq.end(), counter++); // elements are set to 1

    	transform(seq.begin(), seq.end(), seq.begin(), bind2nd(multiplies<float>(), PI_F));
    	transform(seq.begin(), seq.end(), seq.begin(), bind2nd(multiplies<float>(), 2.0));
    	transform(seq.begin(), seq.end(), seq.begin(), bind2nd(divides<float>(), nn2));

    	for (int k = 0 ; k < seq.size(); k++)
    	{
    		window.push_back(cos(seq[k]));
    	}

    	transform(window.begin(), window.end(), window.begin(), bind2nd(multiplies<float>(), 0.46));
    	transform(window.begin(), window.end(), window.begin(), bind1st(minus<float>(), 0.54));
	}
    	return window;
}

vector<float> BandPass::fir2(int MirroredSize, vector<float> Params, vector<int> CutOnOff)
{
    vector<float> wind;
    int nbrk = Params.size();
    vector<float> df(nbrk-1);
    int nn = MirroredSize + 1;
    int npt, lap, nint, nb, ne, j;
    float dt;
    complex<float> sqrt_minus_1;

    if (nn < 1024)
    {
        npt = 512;
    }
    else
    {
        npt = pow(2, ceil(log(nn)/log(2)));
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
    vector<float> inc(npt, 0.0);
    CFVector H(npt);
    CFVector rad(npt);
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
    			inc[j-1] = (float) (j-nb) / (ne-nb);
    			H[j-1] = inc[j-1] * CutOnOff[mes] + ( (1 - inc[j-1]) * CutOnOff[mes-1]);
    		}
    	}
    	nb = ne + 1;
    }

    // Fourier time-shift
    dt = 0.5 * (nn - 1);
    sqrt_minus_1 = sqrt(complex<float>(-1));
    complex<float> intermediate;
    const float  PI_F=3.14159265358979f;
    intermediate = -dt * sqrt_minus_1 * (float) PI_F;

    for (int pt = 0; pt < npt; pt++) //(int pt = 0; pt < npt; pt++)
    {
    	rad[pt] = intermediate * complex<float>(pt) / complex<float> (npt-1);
    	H[pt] = H[pt] * exp(rad[pt]);
    }

    CFVector HRev;
    HRev = H;
    // reverse the signal
    reverse(HRev.begin(), HRev.end());

	// keep the signal of size (reversed signal-1 : 2)
	CFVector::const_iterator first = HRev.begin() + 1;
    CFVector::const_iterator last = HRev.begin() + HRev.size() - 1;
    CFVector HRev2(first,last);

    for (int q = 0 ; q < HRev2.size(); q++)
    {
    	HRev2[q] = conj(HRev2[q]);
    }

    // put HRev2 at the end of H
	// it is stored in H
	move(HRev2.begin(), HRev2.end(), back_inserter(H));

	/*float U = 0;
	for (int q = 0; q < nn; q++)
	{
		U = U + wind[q];
	}*/


	MBT_Fourier *itrans = new MBT_Fourier();
	itrans -> inverseBluesteinFFT(H);

	vector<float> b(nn);
	// take the real part of the fourier coefficients
	for (int q = 0; q < nn; q++)
	{
		b[q] = real(H[q]) * wind[q];
		//b[q] = sqrt(b[q]);
	}

	return b;
}

//vector<float> BandPassFilter(vector<float> RawSignal)
vector<float> BandPassFilter(vector<float> RawSignal, vector<float> freqBounds)
{
	vector<float> BandPassedData;
	vector<float> Mirrored, H;
	int MirroredSize;

	//BandPass *filter = new BandPass();
	BandPass *filter = new BandPass(freqBounds); // Fanny Grosselin 13/02/2017
	Mirrored = filter->MirrorSignal(RawSignal);
    MirroredSize = Mirrored.size();

    vector<float> Params = {0, filter->HighStop, filter->HighPass, filter->LowPass, filter->LowStop, Fnorm};
	vector<int> CutOnOff = {0, 0, 1, 1, 0, 0};

    transform(Params.begin(), Params.end(), Params.begin(), bind2nd(divides<float>(), Fnorm));

    H = filter->fir2(MirroredSize-1, Params, CutOnOff);

    // type casting actually: from vector<double> to CDVector
    CFVector Hhelp(H.begin(), H.end());

    MBT_Fourier *trans = new MBT_Fourier();
	trans -> forwardBluesteinFFT(Hhelp);

	// Fanny Grosselin 2017/01/17
	/*int nn = MirroredSize + 1;
	vector<float> wind;
	wind = filter->hamming(nn);
    float U = 0;
	for (int q = 0; q < Hhelp.size(); q++)
	{
		U = U + wind[q];
	}*/

	//complex<float> FsComplex(Fs,0);
	for (int q = 0; q < Hhelp.size(); q++)
	{
	    Hhelp[q] = abs(Hhelp[q]);
	    //Hhelp[q] = abs(Hhelp[q])/U;
		//Hhelp[q] = (pow(abs(Hhelp[q]),2.0))/U;
		//Hhelp[q] = sqrt(Hhelp[q])/FsComplex;
	}

    int SizeHhelp = Hhelp.size();

    if (filter->HighPass != 0)
    {
    	Hhelp[0] = 0;
    	Hhelp[SizeHhelp-1] = 0;
    }

    // type casting actually: from vector<double> to CDVector
    CFVector MirroredHelp(Mirrored.begin(), Mirrored.end());
    MBT_Fourier *MirroredTrans = new MBT_Fourier();
    // fft of the mirrored signal
	MirroredTrans -> forwardBluesteinFFT(MirroredHelp);

	// FIXME --- find a better way to do it
	// multiply fft of the mirrored signal with the coefficients of fir2
    CFVector MirroredHhelp;
    vector<float> MirroredHhelp2;


    for(int i=0; i < MirroredHelp.size(); i++)
    {
    	MirroredHhelp.push_back(MirroredHelp[i]*Hhelp[i]);
    }


	MirroredTrans -> inverseBluesteinFFT(MirroredHhelp);

    for(int i = 0; i < MirroredHhelp.size(); i++)
    {
        MirroredHhelp2.push_back(real(MirroredHhelp[i]));
    	//MirroredHhelp2.push_back(real(MirroredHhelp[i])/Fs/1000);
    }

    // keep only the initial part of the signal (the central part)
    vector<float>::const_iterator firstpoint = MirroredHhelp2.begin() + RawSignal.size();
    vector<float>::const_iterator lastpoint = MirroredHhelp2.begin() + 2*RawSignal.size();
    vector<float> CentralPart(firstpoint, lastpoint);

    return CentralPart;
}


