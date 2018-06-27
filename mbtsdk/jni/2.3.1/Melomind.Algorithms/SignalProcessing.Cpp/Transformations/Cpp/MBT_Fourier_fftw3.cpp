//
//  MBT_Fourier_fftw3.cpp
//
//  Created by Katerina Pandremmenou on 13/03/2017.
//  Copyright (c) 2017 Katerina Pandremmenou. All rights reserved.
//
// Updated by Katerina Pandremmenou on 20/03/2017
// Put an if-else in the destructors and initialize all of the vectors with a predefined size and use an assignment in the place of push_back
// Updated by Fanny Grosselin on 23/03/2017 --> Comment the float parts because fftwf can't be used for the moment in Androïd
//            Fanny Grosselin on 24/03/2017 --> Comment DOUBLE_FFTW_R2C_1D class, because there is a bad allocation sometimes.

#include "../Headers/MBT_Fourier_fftw3.h"
#include <iostream>
#include <vector>
#include <cassert>
#include <cstring>
#include <random>

#include <algorithm>
#include <iterator>
#include <iostream>
#include <functional>

using namespace std;
/*
//                               F L O A T S
//            COMPLEX TO COMPLEX - FORWARD AND INVERSE FOURIER TRANSFORMS
FLOAT_FFTW_C2C_1D::FLOAT_FFTW_C2C_1D(int size, CFVector vec, int tmp)
{
	this->N         = size;
	this->selection = tmp;
	this->input     = vec;

	if (selection == -1)
	{
		forward_output  = (fftwf_complex*)fftwf_malloc(sizeof(fftwf_complex)*N);
		forward_plan    = fftwf_plan_dft_1d(N, reinterpret_cast<fftwf_complex*>(&input[0]), forward_output, FFTW_FORWARD, FFTW_ESTIMATE);
	}
	else if (selection == 1)
	{
		backward_output = (fftwf_complex*)fftwf_malloc(sizeof(fftwf_complex)*N);
		backward_plan   = fftwf_plan_dft_1d(N, reinterpret_cast<fftwf_complex*>(&input[0]), backward_output, FFTW_BACKWARD, FFTW_ESTIMATE);
	}
}

FLOAT_FFTW_C2C_1D::~FLOAT_FFTW_C2C_1D()
{
    if (selection == -1)
    {
	   fftwf_destroy_plan(forward_plan);
       fftwf_free(forward_output);
    }
    else if (selection == 1)
    {
       fftwf_destroy_plan(backward_plan);
       fftwf_free(backward_output);
    }
}

CFVector FLOAT_FFTW_C2C_1D::fft_execute()
{
	CFVector forward_result(N);
	fftwf_execute(forward_plan);

	for (int k = 0 ; k < N; k++)
        forward_result[k] = {forward_output[k][0], forward_output[k][1]};

	return forward_result;
}

CFVector FLOAT_FFTW_C2C_1D::ifft_execute()
{
	CFVector backward_result(N);
	fftwf_execute(backward_plan);

	for (int k = 0 ; k < N; k++)
		backward_result[k] = {backward_output[k][0]/(float)N, backward_output[k][1]/(float)N};

	return backward_result;
}

//                      F L O A T S
//               ONLY REAL TO COMPLEX FOR FFT
FLOAT_FFTW_R2C_1D::FLOAT_FFTW_R2C_1D(int size, vector<float> vec)
{
    this->N         = size;
    this->input     = vec;

    forward_input           = (float*)fftwf_malloc(sizeof(float)*N);
    forward_input           = &input[0];
    forward_output          = (fftwf_complex*)fftwf_malloc(sizeof(fftwf_complex)*N/2+1);
    reverse_forward_output  = (fftwf_complex*)fftwf_malloc(sizeof(fftwf_complex)*N/2-1);
    full_forward_output     = (fftwf_complex*)fftwf_malloc(sizeof(fftwf_complex)*N);

    forward_plan = fftwf_plan_dft_r2c_1d(N, forward_input, forward_output, FFTW_ESTIMATE);
}

FLOAT_FFTW_R2C_1D::~FLOAT_FFTW_R2C_1D()
{
    fftwf_destroy_plan(forward_plan);

    fftwf_free(forward_input);
    fftwf_free(forward_output);
    fftwf_free(reverse_forward_output);
    fftwf_free(full_forward_output);
}

CFVector FLOAT_FFTW_R2C_1D::execute()
{
    CFVector forward_result(N);
    fftwf_execute(forward_plan);

    // construct a full length fft output
    int count = 0;
    int check, k1;

    // check if the length of the array is even or odd
    if (N % 2 == 0)
        check = N/2;
    else
        check = N/2+1;

    for (k1 = check; k1 >=1; k1--)
    {
        reverse_forward_output[count][0] =  forward_output[k1][0];
        reverse_forward_output[count][1] = -forward_output[k1][1];

        count = count + 1;
    }

    int k2 = 0;

    while(k2 < N)
    {
        if (k2 < N/2+1)
        {
            full_forward_output[k2][0] = forward_output[k2][0];
            full_forward_output[k2][1] = forward_output[k2][1];
        }

        if (k2 >= N/2+1)
        {
            full_forward_output[k2][0] = reverse_forward_output[k2-N/2][0];
            full_forward_output[k2][1] = reverse_forward_output[k2-N/2][1];
        }

        k2 = k2 + 1;
    }

    for (int k = 0 ; k < N; k++)
        forward_result[k] = {full_forward_output[k][0], full_forward_output[k][1]};

    return forward_result;
}


//                       F L O A T S
//              ONLY COMPLEX TO REAL FOR IFFT
FLOAT_FFTW_C2R_1D::FLOAT_FFTW_C2R_1D(int size, CFVector vec)
{
    this->N         = size;
    this->input     = vec;

    backward_output = (float*)fftwf_malloc(sizeof(float)*N);
    backward_plan   = fftwf_plan_dft_c2r_1d(N, reinterpret_cast<fftwf_complex*>(&input[0]), backward_output, FFTW_ESTIMATE);
}

FLOAT_FFTW_C2R_1D::~FLOAT_FFTW_C2R_1D()
{
    fftwf_destroy_plan(backward_plan);
    fftwf_free(backward_output);
}

vector<float> FLOAT_FFTW_C2R_1D::execute()
{
    vector<float> backward_result(N);
    fftwf_execute(backward_plan);

    for (int k = 0 ; k < N; k++)
        backward_result[k] = backward_output[k]/(double)N;

    return backward_result;
}*/

// ********************************************************************************************


//                        D O U B L E S
//    COMPLEX TO COMPLEX - FORWARD AND INVERSE FOURIER TRANSFORMS
DOUBLE_FFTW_C2C_1D::DOUBLE_FFTW_C2C_1D(int size, CDVector vec, int tmp)
{
	this->N         = size;
	this->selection = tmp;
	this->input     = vec;

	if (selection == -1)
	{
		forward_output  = (fftw_complex*)fftw_malloc(sizeof(fftw_complex)*N);
		forward_plan    = fftw_plan_dft_1d(N, reinterpret_cast<fftw_complex*>(&input[0]), forward_output, FFTW_FORWARD, FFTW_ESTIMATE);
	}
	else if (selection == 1)
	{
		backward_output = (fftw_complex*)fftw_malloc(sizeof(fftw_complex)*N);
		backward_plan   = fftw_plan_dft_1d(N, reinterpret_cast<fftw_complex*>(&input[0]), backward_output, FFTW_BACKWARD, FFTW_ESTIMATE);
	}
}

DOUBLE_FFTW_C2C_1D::~DOUBLE_FFTW_C2C_1D()
{
    if (selection == -1)
    {
	   fftw_destroy_plan(forward_plan);
       fftw_free(forward_output);
    }
    else if (selection == 1)
    {
       fftw_destroy_plan(backward_plan);
	   fftw_free(backward_output);
    }
}

CDVector DOUBLE_FFTW_C2C_1D::fft_execute()
{
	CDVector forward_result(N);
	fftw_execute(forward_plan);

	for (int k = 0 ; k < N; k++)
        forward_result[k] = {forward_output[k][0], forward_output[k][1]};

	return forward_result;
}

CDVector DOUBLE_FFTW_C2C_1D::ifft_execute()
{
	CDVector backward_result(N);
	fftw_execute(backward_plan);

	for (int k = 0 ; k < N; k++)
		backward_result[k] = {backward_output[k][0]/(double)N, backward_output[k][1]/(double)N};

	return backward_result;
}

//                      D O U B L E S
//               ONLY REAL TO COMPLEX FOR FFT
// Fanny Grosselin : FIXE ME! Sometimes there is a problem of bad allocation when we use this class.
/*DOUBLE_FFTW_R2C_1D::DOUBLE_FFTW_R2C_1D(int size, vector<double> vec)
{
	this->N       = size;
	this->input   = vec;

    forward_input           = (double*)fftw_malloc(sizeof(double)*N);
	forward_input           = &input[0];
	forward_output          = (fftw_complex*)fftw_malloc(sizeof(fftw_complex)*N/2+1);
	reverse_forward_output  = (fftw_complex*)fftw_malloc(sizeof(fftw_complex)*N/2-1);
	full_forward_output     = (fftw_complex*)fftw_malloc(sizeof(fftw_complex)*N);

	forward_plan = fftw_plan_dft_r2c_1d(N, forward_input, forward_output, FFTW_ESTIMATE);
}

DOUBLE_FFTW_R2C_1D::~DOUBLE_FFTW_R2C_1D()
{
	fftw_destroy_plan(forward_plan);

	fftw_free(forward_input);
	fftw_free(forward_output);
	fftw_free(reverse_forward_output);
	fftw_free(full_forward_output);
}

CDVector DOUBLE_FFTW_R2C_1D::execute()
{
	CDVector forward_result(N);
	fftw_execute(forward_plan);

	// construct a full length fft output
    int count = 0;
    int check, k1;

     // check if the length of the array is even or odd
    if (N % 2 == 0)
        check = N/2;
    else
        check = N/2+1;

    for (k1 = check; k1 >=1; k1--)
    {
    	reverse_forward_output[count][0] =  forward_output[k1][0];
    	reverse_forward_output[count][1] = -forward_output[k1][1];

    	count = count + 1;
    }

    int k2 = 0;
    while(k2 < N)
    {
    	if (k2 < N/2+1)
    	{
			full_forward_output[k2][0] = forward_output[k2][0];
			full_forward_output[k2][1] = forward_output[k2][1];
		}

		if (k2 >= N/2+1)
		{
			full_forward_output[k2][0] = reverse_forward_output[k2-N/2][0];
			full_forward_output[k2][1] = reverse_forward_output[k2-N/2][1];
		}

		k2 = k2 + 1;
    }

	for (int k = 0 ; k < N; k++)
		forward_result[k] = {full_forward_output[k][0], full_forward_output[k][1]};

	return forward_result;
}*/


//                       D O U B L E S
//              ONLY COMPLEX TO REAL FOR IFFT
DOUBLE_FFTW_C2R_1D::DOUBLE_FFTW_C2R_1D(int size, CDVector vec)
{
    this->N         = size;
    this->input     = vec;

    backward_output = (double*)fftw_malloc(sizeof(double)*N);
    backward_plan   = fftw_plan_dft_c2r_1d(N, reinterpret_cast<fftw_complex*>(&input[0]), backward_output, FFTW_ESTIMATE);
}

DOUBLE_FFTW_C2R_1D::~DOUBLE_FFTW_C2R_1D()
{
    fftw_destroy_plan(backward_plan);
    fftw_free(backward_output);
}

vector<double> DOUBLE_FFTW_C2R_1D::execute()
{
    vector<double> backward_result(N);
    fftw_execute(backward_plan);

    for (int k = 0 ; k < N; k++)
        backward_result[k] = backward_output[k]/(double)N;

    return backward_result;
}
