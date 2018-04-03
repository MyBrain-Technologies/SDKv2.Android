//
//  MBT_Operations.h
//  MBT.iOS
//
//  Created by Katerina Pandremmenou on 13/12/2016.
//  Copyright (c) 2016 Katerina Pandremmenou. All rights reserved.
//
//  Update: Fanny Grosselin on 10/02/2017 --> // Add methods to compute median, variance, nanmean, skewness, kurtosis
//          Fanny Grosselin on 14/02/2017 --> // Add a method to approximate integral of a vector via the trapezoidal method (trapz of Matlab)
//  Update: Katerina Pandremmenou on 10/03/2017
//          a) Addition of a method for multiplication in frequency domain instead of convolution in time domain
//          b) Change on the protocol for the naive convolution method
//  Updated: Fanny Grosselin on 23/03/2017 --> Change float by double
//  Updated: Katerina Pandremmenou on 28/04/2017 --> Inclusion of the function "sort_indexes"

#ifndef __MBT_iOS__MBT_Operations__
#define __MBT_iOS__MBT_Operations__

#include <complex>
#include <iostream>
#include <vector>
#include <stdio.h>
#include <cmath>
#include <numeric>
#include "../../DataManipulation/Headers/MBT_Matrix.h"

using namespace std;

typedef complex <float> ComplexFloat;
typedef vector <ComplexFloat> CFVector;
typedef complex <double> ComplexDouble;
typedef vector <ComplexDouble> CDVector;

/*
 * @brief Compute the median of a vector of double.
 * @param The input vector of double.
 * @return The median value.
 */
double median(std::vector<double> const& input);

/*
 * @brief Compute the mean of a vector of double.
 * @param The input vector of double.
 * @return The mean value.
 */
double mean(std::vector<double> const& input);

/*
 * @brief Compute the mean of a vector of double with removed NaN values.
 * @param The input vector of double.
 * @return The mean value without considering NaN values.
 */
double nanmean(std::vector<double> const& input);

/*
 * @brief Compute the variance of a vector of double.
 * @param The input vector of double.
 * @return The variance value.
 */
double var(std::vector<double> const& input);

/*
 * @brief Compute the variance of a vector of double.
 * @param The input vector of double.
 * @param The mean value of the input vector.
 * @return The variance value.
 */
double var(std::vector<double> const& input, const double average);

/*
 * @brief Compute the standard deviation of a vector of double.
 * @param The input vector of double.
 * @return The standard deviation value.
 */
double standardDeviation(std::vector<double> const& input);

/*
 * @brief Compute the standard deviation of a vector of double.
 * @param The input vector of double.
 * @param The mean value of the input vector.
 * @return The standard deviation value.
 */
double standardDeviation(std::vector<double> const& input, const double average);

/*
 * @brief Compute the skewness of a vector of double.
 * @param The input vector of double.
 * @return The skewness value.
 */
double skewness(std::vector<double> const& input);

/*
 * @brief Compute the kutosis of a vector of double.
 * @param The input vector of double.
 * @return The kurtosis value.
 */
double kurtosis(std::vector<double> const& input);

/*
 * @brief Compute the approximate integral of inputY via the trapezoidal method with unit spacing determine by the size of inputX.
 * @param inputX The vector of double which allows to compute the integral of inputY.
 * @param inputY The input vector of double on which the integral will be computed.
 * @return The approximate integral of inputY.
 */
double trapz(std::vector<double> inputX, std::vector<double> inputY);

// It shifts a position to the right the values of the vector and assigns a zero to the first position
//CFVector ShiftVector(CFVector VecToShift);
CDVector ShiftVector(CDVector VecToShift);

// applies naive convolution in time domain
//CFVector naive_convolve(CFVector signal, CFVector channel);
CDVector naive_convolve(CDVector signal, CDVector channel);

// applies multiplication in frequency domain (advanced method for much faster convolution)
//CFVector fft_convolve(CFVector signal, CFVector channel);
CDVector fft_convolve(CDVector signal, CDVector channel);

// keep the central part of the convolution
//CFVector ConvCentralPart(int convsize, int signalsize, CFVector y);
CDVector ConvCentralPart(int convsize, int signalsize, CDVector y);

// is used to print the outcome of the convolution
//void output(CFVector outputVector);
void output(CDVector outputVector);

// this function is used to calculate the area of the "image" - rectangle
int get_ImageArea(MBT_Matrix<double> TFMap);

// this function changes the size of the matrix
MBT_Matrix<double> keepPart(MBT_Matrix<double> matrix, int rows, int columns);

 // this function is used
MBT_Matrix<double> mergeArrays(MBT_Matrix<double> contourc, MBT_Matrix<double> this_contour);

// this function is used to find the order of the indexes after having sorted the values
template <typename T>
vector<unsigned long> sort_indexes(const vector<T> &v)
{
  // initialize original index locations
  vector<unsigned long> idx(v.size());
  iota(idx.begin(), idx.end(), 0);

  // sort indexes based on comparing values in v
  sort(idx.begin(), idx.end(),
       [&v](T i1, T i2) {return v[i1] < v[i2];});

  return idx;
}

#endif /* defined(__MBT_iOS__MBT_Operations__) */
