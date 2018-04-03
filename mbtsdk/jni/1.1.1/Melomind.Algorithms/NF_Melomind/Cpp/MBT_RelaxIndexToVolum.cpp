//
//  MBT_SmoothRelaxIndex.cpp
//
//  Created by Fanny Grosselin on 06/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
//  Update : Katerina Pandremmenou on 20/09/2017 --> Change all implicit type castings to explicit ones
//           Fanny Grosselin on 27/09/2017 --> Change the volum to 1 when smoothRelaxIndex is nan instead of 0.5.

#include "../Headers/MBT_RelaxIndexToVolum.h"

float MBT_RelaxIndexToVolum(const float smoothedRelaxIndex)
{
    float volum;
    if (smoothedRelaxIndex == std::numeric_limits<float>::infinity())
    {
        // Store values to be handled in case of problem into MBT_RelaxIndexToVolum
        volum = -1.0;
        errno = EINVAL;
        perror("ERROR: MBT_RELAXINDEXTOVOLUM CANNOT PROCESS WITHOUT SMOOTHEDRELAXINDEX IN INPUT");
    }
    else if (isnan(smoothedRelaxIndex))
    {
        // if 4 eeg packets have a bad quality, its eeg values are set to NaN. So we put a volum which is the middle of the scale that is to say 0.5.
        volum = 1.0;
        std::cout<<"At least four consecutive EEG packets have bad quality."<<std::endl;
    }
    else
    {
        float rescale = (float) tanh(smoothedRelaxIndex); // rescale between -1 and 1
        rescale = rescale + 1; // rescale between 0 and 2
        rescale = rescale/2; // rescale between 0 and 1
        volum = 1 - rescale;
    }
    return volum;
}

