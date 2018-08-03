//
//  MBT_FindClosest.cpp
//  MBT.iOS
//
//  Created by Emma Barme on 19/10/2015.
//  Copyright (c) 2015 Emma Barme. All rights reserved.
//
// Updated by Fanny Grosselin on 23/03/2017 --> Change float by double
// 			  Fanny Grosselin 2017/03/27 --> Change '\' by '/' for the paths
//            Fanny Grosselin 2017/03/27 --> Fix all the warnings.

#include "../Headers/MBT_FindClosest.h"

int MBT_FindClosest::findClosestIndex(std::vector<double> const& inputReference, const double valueToFind)
{
    unsigned int length = (int)inputReference.size();
    std::vector<double> dist;
    dist.assign(length, valueToFind);
    for (unsigned int i = 0; i < length; i++)
    {
        dist[i] -= inputReference[i];
        dist[i] *= dist[i];
    }

    return (int)(std::min_element(dist.begin(), dist.begin() + length) - dist.begin());
}

std::pair<int, int> MBT_FindClosest::findClosetIndices(std::vector<double> const& inputReference, const double startValueToFind, const double endValueToFind)
{
    return std::pair<int, int> (MBT_FindClosest::findClosestIndex(inputReference, startValueToFind), MBT_FindClosest::findClosestIndex(inputReference, endValueToFind));
}
