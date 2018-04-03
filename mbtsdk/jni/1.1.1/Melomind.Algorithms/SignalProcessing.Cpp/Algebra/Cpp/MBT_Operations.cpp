//
// MBT_Operations.cpp
// Created by Katerina Pandremmenou on 13/12/2016.
// Copyright (c) 2016 myBrain Technologies. All rights reserved
//
// Update: Fanny Grosselin on 06/01/2017 --> // standardDeviation: Avoid nan value if we find the standard deviation of only one value.
//         Fanny Grosselin on 10/02/2017 --> // Add a method to compute median
// Update: Katerina Pandremmenou on 10/03/2017
//         a) New way for performing convolution
//         b) Change in the name of the naive convolution
// Update: Katerina Pandremmenou on 14/03/2017
//         a) use the class based on fftw3 library for applying fourier transforms (MBT_Fourier_fftw3.h file)
//         b) free the pointers
// Update: Katerina Pandremmenou on 20/03/2017
//         a) remove the free of the pointers for the FFT-related objects, as they are already free in the destructor
// Update: Fanny Grosselin on 23/03/2017 --> Change float by double
//         Fanny Grosselin on 27/03/2017 --> Fix all the warnings (in particular in ShiftVector function).
//         Fanny Grosselin 2017/05/22 --> Fix the memory leaks because of the creation of an object without deleting it.
// Update: Katerina Pandremmenou on 19/09/2017 --> Change to explicit type casting from implicit type casting in function ConvCentralPart

#include <complex>
#include <iostream>
#include <algorithm>
#include <vector>
#include <iterator>
#include <cstring>
#include <fftw3.h>
#include "../Headers/MBT_Operations.h"
#include "../../Transformations/Headers/MBT_Fourier_fftw3.h"

double median(std::vector<double> const& input)
{
    double median = 0;
    std::vector<double> sortedInput = input;
    sort(sortedInput.begin(), sortedInput.end());

    if (sortedInput.size()  % 2 == 0)
    {
      return median = (sortedInput[sortedInput.size() / 2 - 1] + sortedInput[sortedInput.size() / 2]) / 2;
    }
    else
    {
      return median = sortedInput[sortedInput.size() / 2];
    }
}

double mean(std::vector<double> const& input)
{
    double sum = 0;

    for (unsigned int value=0; value<input.size();value++)
    {
        sum += input[value];
    }

    return sum / input.size();
}

double nanmean(std::vector<double> const& input)
{
    double sum = 0;
    std::vector<double> tmp_input = input;
    tmp_input.erase(std::remove_if(tmp_input.begin(), tmp_input.end(),[](double testNaN){return std::isnan(testNaN);}),tmp_input.end()); // remove NaN values

    for (unsigned int value=0; value<tmp_input.size();value++)
    {
        sum += tmp_input[value];
    }

    return sum / tmp_input.size();
}

double var(std::vector<double> const& input)
{
    double average = mean(input);

    return var(input, average);
}

double var(std::vector<double> const& input, const double average)
{
    double variance = 0;
    for (unsigned int i = 0; i < input.size(); i++)
    {
        variance += pow(input[i] - average, 2);
    }

    if (input.size()!= 1)
    {
        return variance = variance / (input.size()-1);
    }
    else // Avoid nan value if we find the standard deviation of only one value
    {
        return variance = variance / input.size();
    }
}

double standardDeviation(std::vector<double> const& input)
{
    double average = mean(input);

    return standardDeviation(input, average);
}


double standardDeviation(std::vector<double> const& input, const double average)
{
    double variance = var(input,average);
    return sqrt(variance);
}


double skewness(std::vector<double> const& input)
{
    // Center X, compute its third and second moments, and compute the uncorrected skewness.
    std::vector<double> x0 = input;
    std::vector<double> x2 = input;
    std::vector<double> x3 = input;
    for (unsigned int i=0; i<input.size(); i++)
    {
        x0[i] = input[i] - nanmean(input);
        x2[i] = pow(x0[i],2);
        x3[i] = pow(x0[i],3);
    }
    double s2 = nanmean(x2); //this is the biased variance estimator
    double m3 = nanmean(x3);
    double s = m3 / pow(s2,1.5);
    return s;
}


double kurtosis(std::vector<double> const& input)
{
    // Center X, compute its fourth and second moments, and compute the uncorrected kurtosis.
    std::vector<double> x0 = input;
    std::vector<double> x2 = input;
    std::vector<double> x4 = input;
    for (unsigned int i=0; i<input.size(); i++)
    {
        x0[i] = input[i] - nanmean(input);
        x2[i] = pow(x0[i],2);
        x4[i] = pow(x0[i],4);
    }
    double s2 = nanmean(x2); //this is the biased variance estimator
    double m4 = nanmean(x4);
    double k = m4 / pow(s2,2);
    return k;
}

double trapz(std::vector<double> inputX, std::vector<double> inputY)
{
    double z = 0;
    for (unsigned int i = 0; i<inputX.size()-1; i++)
    {
        z = z + (inputX[i+1]-inputX[i])*(inputY[i]+inputY[i+1]);
    }
    z = z/2.0;

    return z;
}

//////////////////////          NAIVE CONVOLUTION          ///////////////////////
/*CFVector ShiftVector(CFVector VecToShift)
{
	CFVector temp(VecToShift.size());
    temp[0] = (0.0, 0.0);
	for (int i=1,j=0; i<temp.size(); i++,j++)
	{
		temp[i]=VecToShift[j];
	}
	return temp;
}*/
CDVector ShiftVector(CDVector VecToShift)
{
	CDVector temp(VecToShift.size());
    temp[0] = 0.0;
	for (unsigned int i=1,j=0; i<temp.size(); i++,j++)
	{
		temp[i]=VecToShift[j];
	}
	return temp;
}
/*
CFVector naive_convolve(CFVector signal, CFVector kernel)
{
	CFVector x, h;

	x = signal;
	h = kernel;

    CFVector y(x.size() + h.size() - 1);
    // initialize y to zero
    fill(y.begin(), y.end(), complex<float>(0.0, 0.0));

	int n, k; //counters

	//first step is to reverse x
	reverse(x.begin(), x.end());

    x = zero_padding(x, h.size(), -1);
    h = zero_padding(h, signal.size(), 1);

    // the additional minus 1 is after the last shift there is no overlap
	for(n=0;n < (signal.size()+h.size()-1 - 1 + min(signal.size(),h.size())) ;n++)
	{
		x = ShiftVector(x);

		for(k=0; k<kernel.size()+signal.size(); k++)
		{
			y[n]+=x[k]*h[k];
		}
	 }

	y = ConvCentralPart(y.size(), signal.size(), y);

	return y;
}*/
CDVector naive_convolve(CDVector signal, CDVector kernel)
{
	CDVector x, h;

	x = signal;
	h = kernel;

    CDVector y(x.size() + h.size() - 1);
    // initialize y to zero
    fill(y.begin(), y.end(), complex<double>(0.0, 0.0));

	unsigned int n, k; //counters

	//first step is to reverse x
	reverse(x.begin(), x.end());

    x = zero_padding(x, h.size(), -1);
    h = zero_padding(h, signal.size(), 1);

    // the additional minus 1 is after the last shift there is no overlap
	for(n=0;n < (signal.size()+h.size()-1 - 1 + min(signal.size(),h.size())) ;n++)
	{
		x = ShiftVector(x);

		for(k=0; k<kernel.size()+signal.size(); k++)
		{
			y[n]+=x[k]*h[k];
		}
	 }

	y = ConvCentralPart(y.size(), signal.size(), y);

	return y;
}

///////////////// END OF NAIVE CONVOLUTION /////////////////////////

/////////////////////   FFT CONVOLUTION /////////////////////////
/*CFVector fft_convolve(CFVector signal, CFVector kernel)
{
	CFVector x, h;

	x = signal;
	h = kernel;

    // A) zero padding at the end of the vector size to the convolution size
    x = zero_padding(x, h.size()-1, -1);
    h = zero_padding(h, signal.size()-1, -1);

    // B.a) complex to complex fft of the signal
    FLOAT_FFTW_C2C_1D *obj = new FLOAT_FFTW_C2C_1D(x.size(), x, -1);
    CFVector SignalFFT = obj->fft_execute();
    // B.b) complex to complex fft of the kernel
    FLOAT_FFTW_C2C_1D *obj2 = new FLOAT_FFTW_C2C_1D(h.size(), h, -1);
    CFVector KernelFFT = obj2->fft_execute();

    // C)  multiplication of the two fft results of the signal and the kernel
    CFVector FFTProduct(SignalFFT.size());
    for (int k = 0 ; k < FFTProduct.size(); k++)
    {
        FFTProduct[k] = SignalFFT[k] * KernelFFT[k];
    }

	// D) inverse fourier transform of the product of the two fft results of the signal and the kernel
	FLOAT_FFTW_C2C_1D *obj3 = new FLOAT_FFTW_C2C_1D(FFTProduct.size(), FFTProduct, 1);
    CFVector IFFTProduct = obj3->ifft_execute();

    // keep the central part of the convolution
    CFVector Outcome(IFFTProduct.size());
    Outcome = ConvCentralPart(IFFTProduct.size(), signal.size(), IFFTProduct);

	return Outcome;
}*/
CDVector fft_convolve(CDVector signal, CDVector kernel)
{
	CDVector x, h;

	x = signal;
	h = kernel;

    // A) zero padding at the end of the vector size to the convolution size
    x = zero_padding(x, h.size()-1, -1);
    h = zero_padding(h, signal.size()-1, -1);

    // B.a) complex to complex fft of the signal
    DOUBLE_FFTW_C2C_1D *obj = new DOUBLE_FFTW_C2C_1D(x.size(), x, -1);
    CDVector SignalFFT = obj->fft_execute();
	delete obj;
    // B.b) complex to complex fft of the kernel
    DOUBLE_FFTW_C2C_1D *obj2 = new DOUBLE_FFTW_C2C_1D(h.size(), h, -1);
    CDVector KernelFFT = obj2->fft_execute();
	delete obj2;

    // C)  multiplication of the two fft results of the signal and the kernel
    CDVector FFTProduct(SignalFFT.size());
    for (unsigned int k = 0 ; k < FFTProduct.size(); k++)
    {
        FFTProduct[k] = SignalFFT[k] * KernelFFT[k];
    }

	// D) inverse fourier transform of the product of the two fft results of the signal and the kernel
	DOUBLE_FFTW_C2C_1D *obj3 = new DOUBLE_FFTW_C2C_1D(FFTProduct.size(), FFTProduct, 1);
    CDVector IFFTProduct = obj3->ifft_execute();
	delete obj3;

    // keep the central part of the convolution
    CDVector Outcome(IFFTProduct.size());
    Outcome = ConvCentralPart(IFFTProduct.size(), signal.size(), IFFTProduct);

	return Outcome;
}

///////////////// END OF FFT CONVOLUTION /////////////////////////

/*CFVector ConvCentralPart(int ConvSize, int SignalSize, CFVector y)
{
	int diff = (ConvSize - SignalSize);
	int LB = 0, UB = 0;


	if (diff % 2 != 0)
	{
		LB = ceil((double) diff / 2);
		UB = floor((double) diff / 2);
	}
	else
	{
		UB = diff / 2;
		LB = diff / 2;
	}

	CFVector::const_iterator first = y.begin() + LB;
	CFVector::const_iterator last = y.end() - UB;
	CFVector newVec(first, last);

	return newVec;
}*/
CDVector ConvCentralPart(int ConvSize, int SignalSize, CDVector y)
{
	int diff = (ConvSize - SignalSize);
	int LB = 0, UB = 0;


	if (diff % 2 != 0)
	{
		LB = (int) ceil((double) diff / 2);
		UB = (int) floor((double) diff / 2);
	}
	else
	{
		UB = (int) diff / 2;
		LB = (int) diff / 2;
	}

	CDVector::const_iterator first = y.begin() + LB;
	CDVector::const_iterator last = y.end() - UB;
	CDVector newVec(first, last);

	return newVec;
}

/*void output(CFVector FullConvOutPut)
{
	cout << endl;

	for (int i=0; i < FullConvOutPut.size(); i++)
		cout << FullConvOutPut[i] << ' ';

	cout << endl;
}*/
void output(CDVector FullConvOutPut)
{
	cout << endl;

	for (unsigned int i=0; i < FullConvOutPut.size(); i++)
		cout << FullConvOutPut[i] << ' ';

	cout << endl;
}

int get_ImageArea(MBT_Matrix<double> TFMap)
{
	int Area = 0;
	Area = TFMap.size().first * TFMap.size().second;

	return Area;
}

MBT_Matrix<double> keepPart(MBT_Matrix<double> matrix, int rows, int columns)
{
	MBT_Matrix<double> temp(rows, columns);

	for (int cp1 = 0; cp1 < rows; cp1++)
	{
		for (int cp2 = 0 ; cp2 < columns; cp2++)
		{
			temp(cp1,cp2) = matrix(cp1,cp2);
		}
	}

	return temp;
}

MBT_Matrix<double> mergeArrays(MBT_Matrix<double> contourc, MBT_Matrix<double> this_contour)
{
	MBT_Matrix<double> merged(contourc.size().first, contourc.size().second+this_contour.size().second);
	int k1, k2;
	int k3 = 0;

	for (k1 = 0 ; k1 < merged.size().first; k1++)
	{
		for (k2 = 0; k2 < contourc.size().second; k2++)
		{
			merged(k1,k2) = contourc(k1,k2);
		}

		for(k2 = contourc.size().second; k2 < merged.size().second; k2++)
		{
		    k3 = k2 - contourc.size().second;
			merged(k1,k2) = this_contour(k1, k3);
		}
		k3 = 0;
	}

	return merged;
}
