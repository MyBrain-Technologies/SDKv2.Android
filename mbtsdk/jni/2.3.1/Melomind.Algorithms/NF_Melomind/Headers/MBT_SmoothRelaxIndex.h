//
//  MBT_SmoothRelaxIndex.cpp
//
//  Created by Fanny Grosselin on 09/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
//  Update: Fanny Grosselin : 2017/09/05 Change the pathes.


#ifndef MBT_SMOOTHRELAXINDEX_H_INCLUDED
#define MBT_SMOOTHRELAXINDEX_H_INCLUDED


#include <stdio.h>
#include <string>
#include <iostream>
#include <limits>
#include <errno.h>
#include <algorithm>
#include <vector>
#include "../../SignalProcessing.Cpp/Algebra/Headers/MBT_Operations.h"


/*
 * @brief Smooth the last relaxation index which is holded by pastRelaxIndexes. Only one smoothed relaxation
 *        index by second but this is computed on a signal of 4s with a sliding window of 1s.
 * @param tmp_pastRelaxIndexes Vector holding the relaxation indexes.
 * @param smoothingDuration Integer that gives the number of relaxation indexes we have to take into account to
          smooth the current one. For instance smoothingDuration=2 means we average the current relaxationIndex
          with the previous one.
 * @return The smoothed last relaxation index value.
 */
float MBT_SmoothRelaxIndex(std::vector<float> tmp_pastRelaxIndexes, int smoothingDuration);


#endif // MBT_SMOOTHRELAXINDEX_H_INCLUDED
