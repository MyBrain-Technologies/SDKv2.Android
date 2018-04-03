//
//  MBT_Fourier.cpp
//  MBT.iOS
//
//  Created by Emma Barme on 21/10/2015.
//  Copyright (c) 2015 Emma Barme. All rights reserved.
//
//  Update on 09/12/2016 by Katerina Pandremmenou (double-->float)
//  Update on 12/01/2017 by Fanny Grosselin (zero-padding instead of padding by the mirror of the signal)
//  Update on 19/01/2017 by Fanny Grosselin (change ceil to floor in the computation of powerOfTwoLength)
//  Update on 20/01/2017 by Fanny Grosselin (modify MBT_Fourier::forwardBluesteinFFT (don't use forwardScaling), MBT_Fourier::radix2Reorder and MBT_radix2step according to http://stackoverflow.com/questions/10121574/safe-and-fast-fft)
//                                          WARNING: only forward transformation was checked in comparison to Matlab; ifft was not checked.
//  Update on 03/02/2017 by Fanny Grosselin (rechange floor to ceil in the computation of powerOfTwoLength and redo padding with the mirror of the signal)
// 	Update on 23/03/2017 by Fanny Grosselin (Change float by double)

#include "../Headers/MBT_Fourier.h"


void MBT_Fourier::forwardBluesteinFFT(std::vector<std::complex<double> >& inputData)
{
    forwardBluesteinFFT(inputData, Default);
}


void MBT_Fourier::forwardBluesteinFFT(std::vector<std::complex<double> >& inputData, const MBT_FourierOptions option)
{
    MBT_FourierBluestein::bluestein(inputData, getExponentSign(option));
    //forwardScaling(inputData, option); // commented by Fanny Grosselin 2017/01/20
}


void MBT_Fourier::inverseBluesteinFFT(std::vector<std::complex<double> >& inputData)
{
    inverseBluesteinFFT(inputData, Default);
}


void MBT_Fourier::inverseBluesteinFFT(std::vector<std::complex<double> >& inputData, const MBT_FourierOptions option)
{
    MBT_FourierBluestein::bluestein(inputData, -getExponentSign(option));
    //inverseScaling(inputData, option);
}


std::vector<std::complex<double> > MBT_Fourier::naiveForwardFFT(std::vector<std::complex<double> >& timeSpaceData, const MBT_FourierOptions option)
{
    std::vector<std::complex<double> > frequencySpaceData = naive(timeSpaceData, getExponentSign(option));
    forwardScaling(frequencySpaceData, option);
    return frequencySpaceData;
}


std::vector<std::complex<double> > MBT_Fourier::naiveInverseFFT(std::vector<std::complex<double> >& frequencySpaceData, const MBT_FourierOptions option)
{
    std::vector<std::complex<double> > timeSpaceData = naive(frequencySpaceData, -getExponentSign(option));
    inverseScaling(timeSpaceData, option);
    return timeSpaceData;
}


void MBT_Fourier::forwardRadix2FFT(std::vector<std::complex<double> >& inputData, const MBT_FourierOptions option)
{
    radix2(inputData, getExponentSign(option));
    forwardScaling(inputData, option);
}


void MBT_Fourier::inverseRadix2FFT(std::vector<std::complex<double> >& inputData, const MBT_FourierOptions option)
{
    radix2(inputData, -getExponentSign(option));
    inverseScaling(inputData, option);
}


std::vector<double> MBT_Fourier::frequencyValues(const int nbSample, const double sampRate)
{
    std::vector<double> scale;
    scale.resize(nbSample);

    double freq = 0;
    double step = sampRate / nbSample;
    int secondHalf = (nbSample >> 1) + 1;

    for (int i = 0; i < secondHalf; i++)
    {
        scale[i] = freq;
        freq += step;
    }

    freq = -step * (secondHalf - 2);
    for (int i = secondHalf; i < nbSample; i++)
    {
        scale[i] = freq;
        freq += step;
    }

    return scale;
}


int MBT_Fourier::getExponentSign(const MBT_FourierOptions option)
{
    return (option & InverseExponent) == InverseExponent ? 1 : -1;
}


void MBT_Fourier::forwardScaling(std::vector<std::complex<double> >& inputData, const MBT_FourierOptions option)
{
    if ((option & NoScaling) == NoScaling ||
        (option & AsymmetricScaling) == AsymmetricScaling)
    {
        return;
    }

    double scalingFactor = sqrt(1.0 / inputData.size());

    for (int i = 0; i < inputData.size(); i++)
    {
        inputData[i] *= scalingFactor;
    }
}


void MBT_Fourier::inverseScaling(std::vector<std::complex<double> >& inputData, const MBT_FourierOptions option)
{
    if ((option & NoScaling) == NoScaling)
    {
        return;
    }

    double scalingFactor = 1.0 / inputData.size();

    if ((option & AsymmetricScaling) != AsymmetricScaling)
    {
        scalingFactor = sqrt(scalingFactor);
    }

    for (int i = 0; i < inputData.size(); i++)
    {
        inputData[i] *= scalingFactor;
    }
}

/*
//Helper function and structure for MBT_Fourier::naive.
typedef struct {
    int w0;
    int index;
    int len;
    std::vector<std::complex<double> > const* inputData;
    std::vector<std::complex<double> > * spectrum;
} WorkerArgNaive;
void * MBT_Fourier::workerNaive(void * arg) {
    int w0 = ((WorkerArgNaive *) arg)->w0;
    int i = ((WorkerArgNaive *) arg)->index;
    int len = ((WorkerArgNaive *) arg)->len;
    std::vector<std::complex<double> > const* inputData= ((WorkerArgNaive *) arg)->inputData;
    std::vector<std::complex<double> > * spectrum = ((WorkerArgNaive *) arg)->spectrum;

    double wi = w0 * i;
    std::complex<double> sum(0,0);
    for (int n = 0; n < len; n++)
    {
        double w = n * wi;
        std::complex<double> multiplier(cos(w),sin(w));
        sum += inputData->at(n) * multiplier;
    }

    spectrum->at(i) = sum;

    return NULL;
}
//End of helper function and structure for MBT_Fourier::naive.
*/

std::vector<std::complex<double> > MBT_Fourier::naive(std::vector<std::complex<double> > const& inputData, const int exponentSign)
{
    const double  PI_F=3.14159265358979f;
    int len = (int)inputData.size();
    double w0 = exponentSign * PI_F * 2 / len;
    std::vector<std::complex<double> > spectrum;
    spectrum.resize(len);

    //Multihtreading, not working because vectors are not thread-safe.
    /*
    //Creating the threads
    pthread_t threadsID[len];
    for (int t = 0; t < len; t++)
    {
        WorkerArgNaive arg;
        arg.w0 = w0;
        arg.index = t;
        arg.len = len;
        arg.inputData = &inputData;
        MBT_CPPMultithreading::thread_create(&(threadsID[t]), NULL, workerNaive, &arg);
    }

    //Joining the threads
    for(int t = 0; t < len; t++){
        MBT_CPPMultithreading::thread_join(threadsID[t], NULL);
    }

    if (errno != 0)
    {
        perror("Cannot join workers in naive");
    }
    */

    //Without multithreading
    for (int i = 0; i < len; i++)
    {
        double wi = w0 * i;
        std::complex<double> sum(0,0);
        for (int n = 0; n < len; n++)
        {
            double w = n * wi;
            std::complex<double> multiplier(cos(w),sin(w));
            sum += inputData[n] * multiplier;
        }

        spectrum[i] = sum;

    }

    return spectrum;
}


template<typename T>
void MBT_Fourier::radix2Reorder(std::vector<T>& inputData)
{
    int j = 0;
    for (int i = 0; i < inputData.size() - 1; i++)
    {
        //Swap values
        if (i < j)
        {
            T temp = inputData[i];
            inputData[i] = inputData[j];
            inputData[j] = temp;
        }

        //int len = (int)inputData.size();
        int len = (int)inputData.size()/2; // Fanny Grosselin 2017/01/20

        //do // Comment by Fanny Grosselin on 2017/01/20
        //{ // Comment by Fanny Grosselin on 2017/01/20
            //len >>= 1; // Comment by Fanny Grosselin on 2017/01/20
            j ^= len;
        //} // Comment by Fanny Grosselin on 2017/01/20
        while ((j & len) == 0)
        {// add by Fanny Grosselin 2017/01/20
            len/=2;
            j^=len;
        };
    }
}


void MBT_Fourier::radix2Step(std::vector<std::complex<double> >& inputData, const int exponentSign, const int levelSize, const int indexInLevel)
{
    // Twiddle Factor
    const double  PI_F=3.14159265358979f;
    double exponent = (exponentSign * indexInLevel) * PI_F / levelSize;
    std::complex<double> w (cos(exponent), sin(exponent));

    //int step = levelSize << 1; // Commented by Fanny Grosselin 2017/01/20
    //for (int i = indexInLevel; i < inputData.size(); i += step)
    for (int i = indexInLevel; i < inputData.size(); i += 2*levelSize) // Fanny Grosselin 2017/01/20
    {
        std::complex<double> value = inputData[i];
        std::complex<double> modifier = w * inputData[i + levelSize];
        inputData[i] = value + modifier;
        inputData[i + levelSize] = value - modifier;
    }
}


void MBT_Fourier::radix2(std::vector<std::complex<double> >& inputData, const int exponentSign)
{
    int len = (int)inputData.size();
    if (len == 0 || (len & (len - 1))) //If the length of inputData is not a positive power of two.
    {
        errno = EINVAL;
        perror("ERROR: FFT CANNOT BE COMPUTED WITH RADIX2 WHEN THE DATA LENGTH IS NOT A POSITIVE POWER OF TWO");
    }

    radix2Reorder<std::complex<double> >(inputData);

    for (int levelSize = 1; levelSize < len; levelSize *= 2)
    {
        for (int indexInLevel = 0; indexInLevel < levelSize; indexInLevel++)
        {
            radix2Step(inputData, exponentSign, levelSize, indexInLevel);
        }
    }
}


std::vector<std::complex<double> > MBT_FourierBluestein::bluesteinSequence(const int nbSample)
{
    const double  PI_F=3.14159265358979f;
    double multiplier = PI_F / nbSample;
    std::vector<std::complex<double> > sequence;
    sequence.resize(nbSample);

    if (nbSample > sqrt(LONG_MAX)) //sqrt(LONG_MAX) is the maximum possible length for the Bluestein sequence before overflow.
    {
        for (int k = 0; k < nbSample; k++)
        {
            double t = (multiplier * k) * k; //Avoiding overflow.
            sequence[k] = std::complex<double>(cos(t), sin(t));
        }
    }
    // TODO: benchmark whether the second variation is significantly
    // faster than the former one. If not just use the former one always.
    else
    {
        for (int k = 0; k < nbSample; k++)
        {
            double t = multiplier * (k * k);
            sequence[k] = std::complex<double>(cos(t), sin(t));
        }
    }

    return sequence;
}


//Helper functions and structures for MBT_Fourier::bluesteinConvolutionParallel.
typedef struct {
    std::vector<std::complex<double> >* inputData;
    std::vector<std::complex<double> >* a;
    std::vector<std::complex<double> >* sequence;
    int len;
} WorkerArgBluesteinConvolutionParallelA;

typedef struct {
    int len;
    int powerOfTwoLength;
    std::vector<std::complex<double> >* b;
    std::vector<std::complex<double> >* sequence;
} WorkerArgBluesteinConvolutionParallelB;

void * MBT_FourierBluestein::workerBluesteinConvolutionParallelA(void * arg){
    int len = ((WorkerArgBluesteinConvolutionParallelA *) arg)->len;
    std::vector<std::complex<double> >* inputData= ((WorkerArgBluesteinConvolutionParallelA *) arg)->inputData;
    std::vector<std::complex<double> >* a= ((WorkerArgBluesteinConvolutionParallelA *) arg)->a;
    std::vector<std::complex<double> >* sequence = ((WorkerArgBluesteinConvolutionParallelA *) arg)->sequence;

    for (int i = 0; i < len; i++)
    {
        a->at(i) = std::conj(sequence->at(i)) * inputData->at(i);
    }

    MBT_Fourier::radix2(*a, -1);

    return NULL;
}

void * MBT_FourierBluestein::workerBluesteinConvolutionParallelB(void * arg){
    int len = ((WorkerArgBluesteinConvolutionParallelB *) arg)->len;
    int powerOfTwoLength = ((WorkerArgBluesteinConvolutionParallelB *) arg)->powerOfTwoLength;
    std::vector<std::complex<double> >* b = ((WorkerArgBluesteinConvolutionParallelB *) arg)->b;
    std::vector<std::complex<double> >* sequence = ((WorkerArgBluesteinConvolutionParallelB *) arg)->sequence;

    for (int i = 0; i < len; i++)
    {
        b->at(i) = sequence->at(i);
    }

    for (int i = powerOfTwoLength - len + 1; i < powerOfTwoLength; i++)
    {
        b->at(i) = sequence->at(powerOfTwoLength - i);
        //b->at(i) = 0; // Fanny Grosselin 2017/01/12
    }

    MBT_Fourier::radix2(*b, -1);

    return NULL;
}
//End of helper functions and structures for MBT_Fourier::bluesteinConvolutionParallel.

void MBT_FourierBluestein::bluesteinConvolutionParallel(std::vector<std::complex<double> >& inputData)
{
    int len = (int)inputData.size();
    std::vector<std::complex<double> > sequence = bluesteinSequence(len);

    // Padding to power of two >= 2N–1 so we can apply Radix-2 FFT.
    int powerOfTwoLength = pow(2, ceil(log(2 * len - 1)/log(2)));
    //int tmp_powerOfTwoLength = pow(2, floor(log(2 * len - 1)/log(2))); // Fanny Grosselin 2017/01/19
    //int powerOfTwoLength = std::max(256,tmp_powerOfTwoLength); // Fanny Grosselin 2017/01/19
    std::vector<std::complex<double> > b;
    b.assign(powerOfTwoLength, 0);
    std::vector<std::complex<double> > a;
    a.assign(powerOfTwoLength, 0);

    //Without multithreading
    // Build and transform padded sequence b_k = exp(I*Pi*k^2/N)
    for (int i = 0; i < len; i++)
    {
        b[i] = sequence[i];
    }

    for (int i = powerOfTwoLength - len + 1; i < powerOfTwoLength; i++)
    {
        b[i] = sequence[powerOfTwoLength - i];
        //b[i] = 0; // Fanny Grosselin 2017/01/12
    }

    MBT_Fourier::radix2(b, -1);

    // Build and transform padded sequence a_k = x_k * exp(-I*Pi*k^2/N)
    for (int i = 0; i < len; i++)
    {
        a[i] = std::conj(sequence[i]) * inputData[i];
    }

    MBT_Fourier::radix2(a, -1);

    for (int i = 0; i < a.size(); i++)
    {
        a[i] *= b[i];
    }

    MBT_Fourier::radix2(a, 1);

    /*
    //Creating the threads
    pthread_t threadsID[2];
    WorkerArgBluesteinConvolutionParallelA argA;
    argA.len = len;
    argA.inputData = &inputData;
    argA.a = &a;
    argA.sequence = &sequence;
    WorkerArgBluesteinConvolutionParallelB argB;
    argB.len = len;
    argB.powerOfTwoLength = powerOfTwoLength;
    argB.b = &b;
    argB.sequence = &sequence;
    MBT_CPPMultithreading::thread_create(&(threadsID[0]), NULL, workerBluesteinConvolutionParallelA, &argA);
    MBT_CPPMultithreading::thread_create(&(threadsID[1]), NULL, workerBluesteinConvolutionParallelB, &argB);

    //Joining the threads
    for (int t = 0; t < 2; t++){
        MBT_CPPMultithreading::thread_join(threadsID[t], NULL);
    }

    if (errno != 0)
    {
        perror("Cannot join workers in bluesteinConvolutionParallel");
    }

    for (int i = 0; i < a.size(); i++)
    {
        a[i] *= b[i];
    }

    MBT_Fourier::radix2Parallel(a, 1);
    */


    double nbInv = 1.0 / powerOfTwoLength;

    for (int i = 0; i < len; i++)
    {
        inputData[i] = nbInv * std::conj(sequence[i]) * a[i];
    }

    // Without multithreading
    //std::cout << "WARNING: FORWARD BLUESTEIN FFT COMPUTATION IS NOT PARALLELIZED. IMPLEMENTATION FOR PARALLEL VERSION IS PENDING." << std::endl;
}


void MBT_FourierBluestein::swapComplexParts(std::vector<std::complex<double> >& inputData)
{
    for (int i = 0; i < inputData.size(); i++)
    {
        inputData[i] = std::complex<double>(inputData[i].imag(), inputData[i].real());
    }
}


void MBT_FourierBluestein::bluestein(std::vector<std::complex<double> >& inputData, const int exponentSign)
{
    int len = (int)inputData.size();
    if (len != 0 && !(len & (len - 1)))
    {
        MBT_Fourier::radix2(inputData, exponentSign);
        return;
    }

    if (exponentSign == 1)
    {
        swapComplexParts(inputData);
    }

    bluesteinConvolutionParallel(inputData);

    if (exponentSign == 1)
    {
        swapComplexParts(inputData);
    }
}
