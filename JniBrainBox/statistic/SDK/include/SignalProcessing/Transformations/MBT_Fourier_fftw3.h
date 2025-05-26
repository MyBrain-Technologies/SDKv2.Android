//
//  MBT_Fourier_fftw3.h
//
//  Created by Katerina Pandremmenou on 13/03/2017.
//  Copyright (c) 2017 Katerina Pandremmenou. All rights reserved.
//  Use of the fftw3 library ptrovided by http://www.fftw.org/

// This file contains
// a) a function that applies zero padding at the beginning or the end of a vector
// b) a class that operates on doubles for complex-to-complex forward and inverse fourier transforms
// c) a class that operates on doubles for real to complex forward fourier transforms
// d) a class that operates on doubles for complex to real inverse fourier transforms
// e) a class that operates on floats for complex-to-complex forward and inverse fourier transforms
// f) a class that operates on floats for real to complex forward fourier transforms
// g) a class that operates on floats for complex to real inverse fourier transforms
//
// Currently only one-dimensional transforms are supported
// To be updated in the future for multi-dimensional data
//
// Updated by Fanny Grosselin on 23/03/2017 --> Comment the float parts because fftwf can't be used for the moment in Andro�d
//            Fanny Grosselin on 24/03/2017 --> Comment DOUBLE_FFTW_R2C_1D class, because there is a bad allocation sometimes.
//            Fanny Grosselin on 27/03/2017 --> Fix all the warnings (in zero_padding function).

#ifndef __MBT__Fourier_fftw3__
#define __MBT__Fourier_fftw3__

#include <sp-global.h>

#include <vector>
#include <complex>
#include <fftw3.h>

using namespace std;

/*typedef SP_ComplexFloat ComplexFloat;
typedef SP_ComplexFloatVector CFVector;*/

typedef SP_Complex ComplexDouble;
typedef SP_ComplexVector CDVector;

// Force double usage with FFTW when using float build
#if defined(SP_ENABLE_FLOAT)
typedef std::complex<double> fftw_ComplexDouble;
typedef std::vector<std::complex<double> > fftw_CDVector;
typedef double fftw_RealType;
#else
typedef SP_Complex fftw_ComplexDouble;
typedef SP_ComplexVector fftw_CDVector;
typedef SP_RealType fftw_RealType;
#endif

// Define FFTW type and function usages, depending on the build
#if defined(ENABLE_FLOAT)
typedef fftwf_complex sp_fftw_complex;
typedef fftwf_plan sp_fftw_plan;
const auto sp_fftw_plan_dft_1d = fftwf_plan_dft_1d;
const auto sp_fftw_plan_dft_c2r_1d = fftwf_plan_dft_c2r_1d;
const auto sp_fftw_destroy_plan = fftwf_destroy_plan;
const auto sp_fftw_malloc = fftwf_malloc;
const auto sp_fftw_free = fftwf_free;
const auto sp_fftw_execute = fftwf_execute;
#elif defined(ENABLE_LONG_DOUBLE)
typedef fftwl_complex sp_fftw_complex;
typedef fftwl_plan sp_fftw_plan;
const auto sp_fftw_plan_dft_1d = fftwl_plan_dft_1d;
const auto sp_fftw_plan_dft_c2r_1d = fftwl_plan_dft_c2r_1d;
const auto sp_fftw_destroy_plan = fftwl_destroy_plan;
const auto sp_fftw_malloc = fftwl_malloc;
const auto sp_fftw_free = fftwl_free;
const auto sp_fftw_execute = fftwl_execute;
#elif defined(ENABLE_QUAD_PRECISION)
typedef fftwq_complex sp_fftw_complex;
typedef fftwq_plan sp_fftw_plan;
const auto sp_fftw_plan_dft_1d = fftwq_plan_dft_1d;
const auto sp_fftw_plan_dft_c2r_1d = fftwq_plan_dft_c2r_1d;
const auto sp_fftw_destroy_plan = fftwq_destroy_plan;
const auto sp_fftw_malloc = fftwq_malloc;
const auto sp_fftw_free = fftwq_free;
const auto sp_fftw_execute = fftwq_execute;
#else
typedef fftw_complex sp_fftw_complex;
typedef fftw_plan sp_fftw_plan;
const auto sp_fftw_plan_dft_1d = fftw_plan_dft_1d;
const auto sp_fftw_plan_dft_c2r_1d = fftw_plan_dft_c2r_1d;
const auto sp_fftw_destroy_plan = fftw_destroy_plan;
const auto sp_fftw_malloc = fftw_malloc;
const auto sp_fftw_free = fftw_free;
const auto sp_fftw_execute = fftw_execute;
#endif

// generic function for applying zero padding
// use type ==  1 for zero padding at the beginning of a vector
// use type == -1 for zero padding at the end of a vector
template <typename T>
inline vector<T> zero_padding(vector<T> vec_to_be_zero_padded, int length, int type)
{
	vector<T> temp(length);

	if (type == 1)
	{
		temp.insert(temp.end(), vec_to_be_zero_padded.begin(), vec_to_be_zero_padded.end());
		return temp;
	}
	else if (type == -1)
	{
		vec_to_be_zero_padded.insert(vec_to_be_zero_padded.end(), temp.begin(), temp.end());
		return vec_to_be_zero_padded;
	}
	else
    {
        return temp;
    }
}

//***************************  FLOATS ***********************************
// class for complex to complex fourier transforms (forward and backward), for floats
/*class FLOAT_FFTW_C2C_1D
{
	public:
		// depending on the tmp values, we allocate memory and create a plan
		// either for forward or backward fourier transform
		FLOAT_FFTW_C2C_1D(int size, CFVector vec, int tmp);
		// we destroy the plans and free the pointers
		~FLOAT_FFTW_C2C_1D();
		// we apply forward fft
		CFVector fft_execute();
		// we apply backward fft
		CFVector ifft_execute();

	private:
		fftwf_complex *forward_output, *backward_output;
		fftwf_plan forward_plan, backward_plan;
		int N, selection;
		CFVector input;
};

// class for real to complex, for floats
// it works ONLY for forward fourier transform
class FLOAT_FFTW_R2C_1D
{
	public:
		FLOAT_FFTW_R2C_1D(int size, SP_FloatVector vec);
		// we destroy the plans and free the pointers
		~FLOAT_FFTW_R2C_1D();
		// we apply forward fourier transform
		CFVector execute();

	private:
		fftwf_complex *forward_output, *reverse_forward_output, *full_forward_output;
		SP_FloatType *forward_input;
		fftwf_plan forward_plan;
		int N;
		SP_FloatVector input;
};

// class for complex to real, for floats
// it works ONLY for backward fourier transform
class FLOAT_FFTW_C2R_1D
{
	public:
		FLOAT_FFTW_C2R_1D(int size, CFVector vec);
		// we destroy the plans and free the pointers
		~FLOAT_FFTW_C2R_1D();
		// we apply inverse fourier transform
		SP_FloatVector execute();

	private:
		SP_FloatType *backward_output;
		fftwf_plan backward_plan;
		int N;
		CFVector input;
};*/

//***************************  DOUBLES ***********************************
// class for complex to complex fourier transforms (forward and backward), for doubles
class DOUBLE_FFTW_C2C_1D
{
	public:
		// depending on the tmp values, we allocate memory and create a plan
		// either for forward or backward fourier transform
		DOUBLE_FFTW_C2C_1D(int size, CDVector vec, int tmp);
		// we destroy the plans and free the pointers
		~DOUBLE_FFTW_C2C_1D();
		// we apply forward fourier transform
		CDVector fft_execute();
		// we apply backward fourier transform
		CDVector ifft_execute();

	private:
		sp_fftw_complex *forward_output, *backward_output;
		sp_fftw_plan forward_plan, backward_plan;
		int N, selection;
		fftw_CDVector input;
};

// class for real to complex, for doubles
// it works ONLY for forward fourier transform
// Fanny Grosselin : FIXE ME! Sometimes there is a problem of bad allocation when we use this class.
/*class DOUBLE_FFTW_R2C_1D
{
	public:
		DOUBLE_FFTW_R2C_1D(int size, SP_Vector vec);
		// we destroy the plans and free the pointers
		~DOUBLE_FFTW_R2C_1D();
		// we apply forward fourier transform
		CDVector execute();

	private:
		sp_fftw_complex *forward_output, *reverse_forward_output, *full_forward_output;
		fftw_RealType *forward_input;
		sp_fftw_plan forward_plan;
		int N;
		SP_Vector input;
};*/

// class for complex to real, for doubles
// it works ONLY for backward fourier transform
class DOUBLE_FFTW_C2R_1D
{
	public:
		DOUBLE_FFTW_C2R_1D(int size, CDVector vec);
		// we destroy the plans and free the pointers
		~DOUBLE_FFTW_C2R_1D();
		// we apply inverse fourier transform
		SP_Vector execute();

	private:
		fftw_RealType *backward_output;
		sp_fftw_plan backward_plan;
		int N;
		fftw_CDVector input;
};

#endif /* defined(__MBT__Fourier_fftw3__) */
