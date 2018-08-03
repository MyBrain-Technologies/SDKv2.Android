//
//  MBT_FindClosest.h
//  MBT.iOS
//
//  Created by Emma Barme on 19/10/2015.
//  Copyright (c) 2015 Emma Barme. All rights reserved.
//
// 	Update: Fanny Grosselin 23/03/2017 --> Change float by double
//

#ifndef __MBT_iOS__MBT_FindClosest__
#define __MBT_iOS__MBT_FindClosest__

#include <stdio.h>
#include <utility>
#include <vector>
#include <algorithm>
#include <errno.h>

//Find values in double vectors (the vectors are supposed to be dense)
class MBT_FindClosest {
    
public:
    //Static method: Find the index of the closest value to a reference value in a double vector.
    static int findClosestIndex(std::vector<double> const& inputReference, const double valueToFind);
    
    //Static method: Find the indices of the closest values to two reference values in a double vector.
    static std::pair<int, int> findClosetIndices(std::vector<double> const& inputReference, const double startValueToFind, const double endValueToFind);
};

#endif /* defined(__MBT_iOS__MBT_FindClosest__) */
