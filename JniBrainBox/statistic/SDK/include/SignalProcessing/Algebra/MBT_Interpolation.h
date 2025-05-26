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

#include <sp-global.h>

#include "Algebra/MBT_FindClosest.h"

#include <stdio.h>
#include <iostream>
#include <math.h>
#include <vector>

//File to interpolate data



/*
 * @brief Linear interpolation in a double vector.
 * @param x A vector of double containing the indexes of the known values.
 * @param y A vector of double containing the known values.
 * @param xInterp A vector of double containing the indexes of the values to ne interpolated.
 * @return A vector of double with the interpolated values.
 */
SP_Vector MBT_linearInterp(SP_Vector &x, SP_Vector &y, SP_Vector &xInterp);



#endif // MBT_INTERPOLATION_H_INCLUDED
