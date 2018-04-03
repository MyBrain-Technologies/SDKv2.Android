//
//  MBT_SmoothRelaxIndex.cpp
//
//  Created by Fanny Grosselin on 06/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//

#ifndef MBT_RELAXINDEXTOVOLUM_H_INCLUDED
#define MBT_RELAXINDEXTOVOLUM_H_INCLUDED

#include <errno.h>
#include <stdio.h>
#include <math.h>
#include <iostream>
#include <limits>


/*
 * @brief Transform the normalized smoothed relaxation index of the signal into volum value. For now, only takes only one value by one value into account.
 *        Only one volum value by second but this is computed on a signal of 4s with a sliding window of 1s.
 * @param smoothedRelaxIndex Float holding the normalized smoothed relaxation index.
 * @return The volum value which corresponds to the normalized smoothed relaxation index value.
 */
float MBT_RelaxIndexToVolum(const float smoothedRelaxIndex);


#endif // MBT_RELAXINDEXTOVOLUM_H_INCLUDED
