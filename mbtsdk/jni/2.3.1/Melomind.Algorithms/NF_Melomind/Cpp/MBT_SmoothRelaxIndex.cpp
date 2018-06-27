//
//  MBT_SmoothRelaxIndex.cpp
//
//  Created by Fanny Grosselin on 09/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
// 	Update: Fanny Grosselin on 2017/03/23 --> Change float by double for the functions not directly used by Androïd. For the others, keep inputs and outputs in double, but do the steps with double


#include "../Headers/MBT_SmoothRelaxIndex.h"

float MBT_SmoothRelaxIndex(std::vector<float> tmp_pastRelaxIndexes, int smoothingDuration)
{
    double smoothedRelaxIndex;
	std::vector<double> pastRelaxIndexes(tmp_pastRelaxIndexes.begin(),tmp_pastRelaxIndexes.end());

    int sizePastRelaxIndexes = pastRelaxIndexes.size();

    if (sizePastRelaxIndexes > 0)
    {
        int N = smoothingDuration;
        if (sizePastRelaxIndexes<smoothingDuration)
        {
            N = sizePastRelaxIndexes;
        }

        // count the number of inf and nan
        int counterInf = 0;
        int counterNaN = 0;
        std::vector<double> pastRelaxIndexesWithoutInfNaN;
        for (int t=0; t<N; t++)
        {
            if (isinf(pastRelaxIndexes[sizePastRelaxIndexes-1 -t]))
            {
                counterInf = counterInf +1;
            }
            else if (isnan(pastRelaxIndexes[sizePastRelaxIndexes-1 -t]))
            {
                counterNaN = counterNaN +1;
            }
            else
            {
                pastRelaxIndexesWithoutInfNaN.push_back(pastRelaxIndexes[sizePastRelaxIndexes-1 -t]);
            }
        }

        // if only Inf data
        if (counterInf == N)
        {
            // Store values to be handled in case of problem into MBT_SmoothRelaxIndex
            smoothedRelaxIndex = std::numeric_limits<double>::infinity();
            errno = EINVAL;
            perror("ERROR: MBT_SMOOTHRELAXINDEX CANNOT PROCESS WITHOUT GOOD PASTRELAXINDEX IN INPUT");
        }
        // if only NaN data
        else if (counterNaN == N)
        {
            // Store values to be handled in case of problem into MBT_SmoothRelaxIndex
            smoothedRelaxIndex = nan(" ");
        }

        // if there is only NaN and Inf data
        else if (counterInf + counterNaN == N)
        {
            // Store values to be handled in case of problem into MBT_SmoothRelaxIndex
            smoothedRelaxIndex = nan(" ");
        }

        // if there are some "good" values, we compute smoothedRelaxIndex with the average of these "good" values
        else
        {
            smoothedRelaxIndex = mean(pastRelaxIndexesWithoutInfNaN);
        }

    }
    else
    {
         // Store values to be handled in case of problem into MBT_SmoothRelaxIndex
        smoothedRelaxIndex = std::numeric_limits<double>::infinity();
        errno = EINVAL;
        perror("ERROR: MBT_SMOOTHRELAXINDEX CANNOT PROCESS WITHOUT GOOD INPUT");
    }

    float returnSmmothedRelaxIndex = (float)smoothedRelaxIndex;
    return returnSmmothedRelaxIndex;
}
