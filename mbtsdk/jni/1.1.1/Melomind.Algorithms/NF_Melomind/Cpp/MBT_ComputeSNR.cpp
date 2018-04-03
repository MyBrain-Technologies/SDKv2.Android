//
//  MBT_ComputeSNR.cpp
//
//  Created by Fanny Grosselin on 03/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
//  Update: Fanny Grosselin : 2017/01/26 Remove the possible NaN values before computing SNR.
//          Fanny Grosselin : 2017/02/03 Add in inputs, the Bounds to detect the outliers of the signal.
//          Fanny Grosselin : 2017/03/23 Change float by double.
//			Fanny Grosselin : 2017/03/27 Change '\' by '/' for the paths
//          Fanny Grosselin : 2017/03/27 Fix all the warnings.
//          Fanny Grosselin : 2017/05/16 Fix the case when we have only NaN in the signal.
//          Fanny Grosselin : 2017/06/09 Fix the case when the outliers were not interpolated and put to NaN (SNR is impossible to be computed if we have only NaN).
//          Fanny Grosselin : 2017/07/26 Change the index that we get for the estimatedNoise.
//          Fanny Grosselin : 2017/07/27 Fix the exception when estimatedNoise=0.
//          Fanny Grosselin : 2017/09/18 Remove std::vector<double> Bounds from the input of MBT_ComputeSNR
//          Fanny Grosselin : 2017/09/28 Change the name of freqBoundsOutliers to a more relevant name, that is freqBOundsBandPass

#include "../Headers/MBT_ComputeSNR.h"


std::vector<double> MBT_ComputeSNR(MBT_Matrix<double> signal, const double sampRate, const double IAFinf, const double IAFsup)
{
    if ((signal.size().first > 0) & (signal.size().second > 0))
    {
        std::vector<double> SNR;
        SNR.assign(signal.size().first,0);
        for (int channel=0; channel<signal.size().first; channel++)
        {
            std::vector<double> signal_ch = signal.row(channel);

            // Interpolate the outliers
            // -----------------------------------
            if (std::all_of(signal_ch.begin(), signal_ch.end(), [](double testNaN){return std::isnan(testNaN);}) )
            {
                SNR[channel] = nan(" ");
            }
            else
            {
                signal_ch.erase(std::remove_if(signal_ch.begin(), signal_ch.end(),[](double testNaN){return std::isnan(testNaN);}),signal_ch.end()); // remove NaN values
                std::vector<double> signal_ch_withoutDC = RemoveDC(signal_ch); // Remove DC
                std::vector<double> freqBoundsBandPass;
                freqBoundsBandPass.assign(2,0);
                freqBoundsBandPass[0] = 2.0;
                freqBoundsBandPass[1] = 30.0;
                std::vector<double> tmp_outputBandpass = BandPassFilter(signal_ch_withoutDC,freqBoundsBandPass); // BandPass
               
                if (std::all_of(tmp_outputBandpass.begin(), tmp_outputBandpass.end(), [](double testNaN){return std::isnan(testNaN);}) )
                {
                    SNR[channel] = nan(" ");
                }
                else
                {
                    tmp_outputBandpass.erase(std::remove_if(tmp_outputBandpass.begin(), tmp_outputBandpass.end(),[](double testNaN){return std::isnan(testNaN);}),tmp_outputBandpass.end()); // remove NaN values
                    // -----------------------------------

                    MBT_Matrix<double> signal_ch_mat(1,tmp_outputBandpass.size());
                    for (int m = 0; m<signal_ch_mat.size().second;m++)
                    {
                        signal_ch_mat(0,m) = tmp_outputBandpass[m];
                    }
                    //MBT_Matrix<double> correctedSignal = MBT_trendCorrection(signal_ch_mat,sampRate); // Correct the signal to avoid 1/f trend of the spectrum
                    MBT_PWelchComputer psd = MBT_PWelchComputer(signal_ch_mat, sampRate, "HAMMING"); // Compute spectrum
                    std::vector<double> frequencies = psd.get_PSD(0); //Extract the frequencies for the psd
                    /*MBT_Matrix<double> psd = MBT_readMatrix("C:/Users/Fanny/Documents/SignalProcessing.Cpp/Melomind/TestFiles/Pxx_testComputeSNR.txt");
                    std::vector<double> frequencies = psd.row (0); //Extract the frequencies for the psd*/

                    // Find the first index of the frequencies which is higher or equal to IAFinf
                    // and find the last index of the frequencies which is lower or equal to IAFsup
                    std::pair<int,int> freqBounds = MBT_frequencyBounds(frequencies, IAFinf, IAFsup);
                    int n_f7 = freqBounds.first;
                    int n_f13 = freqBounds.second;

                    // Find the value and the index of EEG peak into the range [IAFinf:IAFsup]
                    //std::vector<double> channelPSD = psd.row(channel + 1);
                    std::vector<double> channelPSD = psd.get_PSD(channel + 1);

                    double alphaMax = MBT_valMaxPeak(channelPSD, n_f7, n_f13);
                    int peakIndex = MBT_indMaxPeak(channelPSD, n_f7, n_f13);

                    // Estimate the EEG noise by linear interpolation
                    int iFlow = peakIndex - 4;
                    int iFup = peakIndex + 4;
                    std::vector<double> n_x; // indexes of known values (n_x)
                    std::vector<double> n_v; // values of known values (n_v)
                    for (int i = iFlow-3; i<iFlow +1; i++) // assign the index of known values (n_x)
                    {
                        n_x.push_back(i);
                        n_v.push_back(channelPSD[i]);
                    }
                    for (int i = iFup; i<iFup+3 +1; i++) // assign the values of known values (n_v)
                    {
                        n_x.push_back(i);
                        n_v.push_back(channelPSD[i]);
                    }
                    std::vector<double> n_xq; // assign the index of interpolated values (n_xq)
                    for (int i = iFlow+1; i<iFup-1 +1; i++)
                    {
                        n_xq.push_back(i);
                    }
                    std::vector<double> n_vq = MBT_linearInterp(n_x, n_v, n_xq);  // find the interpolated values (n_vq)
                    //double estimatedNoise = n_vq[floor(n_vq.size()/2)+1];
        
                    int tmp = (int) floor(n_vq.size()/2);
                    double estimatedNoise = n_vq[tmp];// Fanny Grosselin 2017/07/26
                    // Compute SNR
                    double snr = nan(" ");
                    if (estimatedNoise!=0) // Fanny Grosselin 2017/07/27
                    {
                        snr = alphaMax/estimatedNoise;
                        if (snr<1)
                        {
                            snr = 1;
                        }
                    }
                    SNR[channel] = snr;
                }
            }
        }
        return SNR;
    }
    else
    {
        // Return NaN values to be handled in case of problem into MBT_ComputeSNR
        std::vector<double> SNR;
        SNR.push_back(std::numeric_limits<double>::infinity());
        errno = EINVAL;
        perror("ERROR: MBT_COMPUTESNR CANNOT PROCESS WITHOUT SIGNALS IN INPUT");

        return SNR;
    }
}
