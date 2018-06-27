//
//  MBT_ComputeNoise.cpp
//
//  Created by Fanny Grosselin on 01/12/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//


#include <numeric>
#include "../Headers/MBT_ComputeNoise.h"


std::vector<double> MBT_ComputeNoise(std::vector<double> trunc_frequencies, std::vector<double> trunc_channelPSD)
{

    // BACKGROUND SPECTRAL NOISE
    std::vector<double> noisePow = trunc_frequencies; // copy trunc_frequencies in noisePow : initialization of the background spectral noise

    // 1st fitting
    std::vector<double> t = trunc_frequencies; // copy trunc_frequencies in t
    std::vector<double> y = trunc_channelPSD; // copy trunc_channelPSD in y
    std::transform(t.begin(), t.end(), t.begin(), [](double f){ return 10*std::log10(f); });// apply 10*log10 on the vector containing the frequencies
    std::transform(y.begin(), y.end(), y.begin(), [](double p){ return 10*std::log10(p); });// apply 10*log10 on the vector containing the power values
    int T = t.size();
    std::vector<double> b;
    b.assign(2,0); // parameters of the fitting
    std::vector<double> t_y, t_2;
    t_y.assign(t.size(),0);
    t_2.assign(t.size(),0);
    for (unsigned int va=0;va<t.size();va++)
    {
        t_y[va] = t[va]*y[va];
        t_2[va] = pow(t[va],2);
    }
    double sum_t_y =  std::accumulate (t_y.begin(), t_y.end(), 0.0);
    double sum_t =  std::accumulate (t.begin(), t.end(), 0.0);
    double sum_y =  std::accumulate (y.begin(), y.end(), 0.0);
    double sum_t_2 =  std::accumulate (t_2.begin(), t_2.end(), 0.0);
    b[1] = (T*sum_t_y-sum_t*sum_y)/(T*sum_t_2-pow(sum_t,2)); // fit the parameters
    b[0] = (sum_y-b[1]*sum_t)/T; // fit the parameters
    std::vector<double> tmp_noisePow1 = t; // copy t in tmp_noisePow1
    std::transform(tmp_noisePow1.begin(), tmp_noisePow1.end(), tmp_noisePow1.begin(), [b](double f){ return b[0]+b[1]*f; }); // estimate the fitting curve

    int counter = 0;
    double errFit1 = 0.0;
    double errFit2 = 0.0;
    while (counter<49)
    {
        // 2) Replace values above the estimated background by the background values
        for (unsigned int idxAbove=0;idxAbove<y.size();idxAbove++)
        {
            if (y[idxAbove]>tmp_noisePow1[idxAbove])
            {
                y[idxAbove] = tmp_noisePow1[idxAbove];
            }
        }

        // 3) 2nd fitting
        for (unsigned int va=0;va<t.size();va++)
        {
            t_y[va] = t[va]*y[va];
            t_2[va] = pow(t[va],2);
        }
        sum_t_y =  std::accumulate (t_y.begin(), t_y.end(), 0.0);
        sum_t =  std::accumulate (t.begin(), t.end(), 0.0);
        sum_y =  std::accumulate (y.begin(), y.end(), 0.0);
        sum_t_2 =  std::accumulate (t_2.begin(), t_2.end(), 0.0);
        b[1] = (T*sum_t_y-sum_t*sum_y)/(T*sum_t_2-pow(sum_t,2)); // fit the parameters
        b[0] = (sum_y-b[1]*sum_t)/T; // fit the parameters
        std::vector<double> tmp_noisePow2 = t; // copy t in tmp_noisePow2
        std::transform(tmp_noisePow2.begin(), tmp_noisePow2.end(), tmp_noisePow2.begin(), [b](double f){ return b[0]+b[1]*f; }); // estimate the fitting curve

        std::vector<double> difft_2 = t;
        for (int st=0; st<T; st++)
        {
            difft_2[st] = pow(tmp_noisePow2[st]-tmp_noisePow1[st],2);
        }
        double sum_difft_2 =  std::accumulate (difft_2.begin(), difft_2.end(), 0.0);
        errFit2 = sqrt(sum_difft_2/(double)T);
        if ((counter>1) && (std::abs(errFit1 - errFit2) <= 0.05))
        {
            //std::cout<<"errFit1 = "<<errFit1<<std::endl;
            //std::cout<<"errFit2 = "<<errFit2<<std::endl;
            //std::cout<<"std::abs(errFit1 - errFit2) = "<<std::abs(errFit1 - errFit2)<<std::endl;
            noisePow = tmp_noisePow1;
            counter = 49;
        }
        else
        {
            counter = counter + 1;
            //std::cout<<"counter = "<<counter<<std::endl;
            tmp_noisePow1 = tmp_noisePow2;
            errFit1 = errFit2;
            if (counter == 49)
            {
                noisePow = tmp_noisePow2;
            }
        }
    }
    return noisePow;
}
