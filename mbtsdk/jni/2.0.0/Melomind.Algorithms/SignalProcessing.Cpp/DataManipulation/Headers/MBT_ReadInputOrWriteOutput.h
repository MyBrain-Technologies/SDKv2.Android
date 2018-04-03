
//
// MBT_ReadInputOrWriteOutput.h
//
// Created by Fanny GROSSELIN on 2016/08/26 (inspired from Emma Barme's code on 2015)
// Copyright (c) 2016 myBrain Technologies. All rights reserved.
//
// Update : Fanny Grosselin 2017/01/26 Read correctly NaN values.


#ifndef MBT_READINPUTORWRITEOUTPUT_H
#define MBT_READINPUTORWRITEOUTPUT_H

#include <stdio.h>
#include <string.h>
#include <iostream> // for std::cout and std::endl
#include <vector> // for std::vector
#include <complex>
#include <iomanip> // std::setprecision
#include <errno.h>
#include <fstream>
#include <cmath>
#include "MBT_Matrix.h" // use of the class MBT_Matrix


// Method which read values as a vector from a file (From Emma Barme on 2015)
std::vector<std::complex<float> > MBT_readVector(std::string fileName);

// Method which write a vector into a file (From Emma Barme on 2015)
void MBT_writeVector(std::vector<std::complex<float> >& outputData, std::string fileName);

// Method which read values as a matrix from a file
MBT_Matrix<float> MBT_readMatrix(std::string fileName);

// Method which write a matrix into a file
void MBT_writeMatrix(MBT_Matrix<float>& outputEegData,std::string fileName);




#endif // MBT_READINPUTORWRITEOUTPUT_H

