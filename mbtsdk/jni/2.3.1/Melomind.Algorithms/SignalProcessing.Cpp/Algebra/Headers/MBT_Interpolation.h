//
//  MBT_Interpolation.h
//
//  Created by Fanny Grosselin on 03/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
// 	Update: Fanny Grosselin 23/03/2017 --> Change float by double
// 			Fanny Grosselin 2017/03/27 --> Change '\' by '/' for the paths
//  Update: Katerina Pandremmenou 2017/09/20 --> Change a comment to make it clearer
//

#ifndef MBT_INTERPOLATION_H_INCLUDED
#define MBT_INTERPOLATION_H_INCLUDED

#include <stdio.h>
#include <iostream>
#include <math.h>
#include <vector>
#include "../../Algebra/Headers/MBT_FindClosest.h"

//File to interpolate data



/*
 * @brief Linear interpolation in a double vector.
 * @param x A vector of double containing the indexes of the known values.
 * @param y A vector of double containing the known values.
 * @param xInterp A vector of double containing the indexes of the values to ne interpolated.
 * @return A vector of double with the interpolated values.
 */
std::vector<double> MBT_linearInterp(std::vector<double> &x, std::vector<double> &y, std::vector<double> &xInterp);



#endif // MBT_INTERPOLATION_H_INCLUDED
