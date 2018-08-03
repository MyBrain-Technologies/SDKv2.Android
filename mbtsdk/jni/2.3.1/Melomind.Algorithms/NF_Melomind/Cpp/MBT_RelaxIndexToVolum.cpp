//
//  MBT_SmoothRelaxIndex.cpp
//
//  Created by Fanny Grosselin on 06/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
//  Update : Katerina Pandremmenou on 20/09/2017 --> Change all implicit type castings to explicit ones
//           Fanny Grosselin on 27/09/2017 --> Change the volum to 1 when smoothRelaxIndex is nan instead of 0.5.
//           Fanny Grosselin on 10/10/2017 --> Change the way we map the values between 0 and 1.
//           Fanny Grosselin on 11/10/2017 --> Change the value of the volum to 1 when smoothedRelaxIndex=infinity.
//           Fanny Grosselin on 14/12/2017 --> Change one of the call of the map parametersFromCalibration.
//           Fanny Grosselin on 18/12/2017 --> Change one input by another (the map of parameters from calibration to the vector of relax index from calibration).

#include "../Headers/MBT_RelaxIndexToVolum.h"

float MBT_RelaxIndexToVolum(const float smoothedRelaxIndex, std::vector<float> SNRCalib)
{
    float volum;
    if (smoothedRelaxIndex == std::numeric_limits<float>::infinity())
    {
        // Store values to be handled in case of problem into MBT_RelaxIndexToVolum
        volum = 1.0;
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
        vector<double> Copy_SNRCalib(SNRCalib.begin(), SNRCalib.end()); // convert vector of float in vector of double
        std::vector<double> quants = Quantile(Copy_SNRCalib);
        double range = quants.back() - 1;
        double slope = quants.back() + 1.5 * range - 1;
        slope = slope/4;
        slope = 1/slope;
        double center = 1 + (quants.back() + 1.5 * range - 1)/2;
        float rescale = 0;
        if (smoothedRelaxIndex <= 1)
        {
            rescale = 0;
        }
        else if (smoothedRelaxIndex <= (float) center )
        {
            rescale = (0.5/((float) center-1))*smoothedRelaxIndex-(0.5/((float) center-1));
        }
        else
        {
            rescale = 1/(1 + exp(-(float)slope*(smoothedRelaxIndex- (float)center)));
        }
        volum = 1 - rescale;
    }
    return volum;
}

