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
//          Fanny Grosselin : 2017/10/10 Change the preprocessing for MBT_ComputeSNR:
//                                          i) receiving NaN values (for lost or very bad values from the QC),
//                                         ii) do a linear interpolation of NaN values
//                                        iii) remove possible remain NaN values from the beginning or the end of the best channel
//                                         iv) apply DC removal
//                                          v) apply a bandpass filter between 2 and 30Hz
//                                         iv) compute SNR

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
                // Do a linear interpolation of NaN values
                std::vector<double> n_x_notnan; // indexes of known values (n_x_notnan) (values != nan)
                std::vector<double> n_v_notnan; // values of known values (n_v_notnan) (values != nan)
                std::vector<double> n_xq_nan; // indexes of unknown values (n_xq_nan) (values == nan)
                for (int sig=0; sig<signal_ch.size(); sig++)
                {
                    if (std::isnan(signal_ch[sig]))
                    {
                        n_xq_nan.push_back(sig);
                    }
                    else
                    {
                        n_x_notnan.push_back(sig);
                        n_v_notnan.push_back(signal_ch[sig]);
                    }
                }
                std::vector<double> n_vq_nan = MBT_linearInterp(n_x_notnan, n_v_notnan, n_xq_nan);  // find the interpolated values (n_vq_nan)
                for (int signan=0; signan<n_xq_nan.size(); signan++)
                {
                    signal_ch[n_xq_nan[signan]] = n_vq_nan[signan];
                }

                signal_ch.erase(std::remove_if(signal_ch.begin(), signal_ch.end(),[](double testNaN){return std::isnan(testNaN);}),signal_ch.end()); // remove NaN values

                std::vector<double> signal_ch_withoutDC = RemoveDC(signal_ch); // Remove DC

                std::vector<double> freqBoundsBandPass;
                freqBoundsBandPass.assign(2,0);
                freqBoundsBandPass[0] = 2.0;
                freqBoundsBandPass[1] = 30.0;
                std::vector<double> tmp_outputBandpass = BandPassFilter(signal_ch_withoutDC,freqBoundsBandPass); // BandPass

                std::vector<double> tmp_Bounds = CalculateBounds(tmp_outputBandpass); // Find the bounds for outliers
                std::vector<double> dataWithoutOutliers = InterpolateOutliers(tmp_outputBandpass, tmp_Bounds); // Interpolate outliers

                if (std::all_of(dataWithoutOutliers.begin(), dataWithoutOutliers.end(), [](double testNaN){return std::isnan(testNaN);}) )
                {
                    SNR[channel] = nan(" ");
                }
                else
                {

                    dataWithoutOutliers.erase(std::remove_if(dataWithoutOutliers.begin(), dataWithoutOutliers.end(),[](double testNaN){return std::isnan(testNaN);}),dataWithoutOutliers.end()); // remove NaN values
                    // -----------------------------------

                    MBT_Matrix<double> signal_ch_mat(1,dataWithoutOutliers.size());
                    for (int m = 0; m<signal_ch_mat.size().second;m++)
                    {
                        signal_ch_mat(0,m) = dataWithoutOutliers[m];
                    }
                    MBT_PWelchComputer psd = MBT_PWelchComputer(signal_ch_mat, sampRate, "HAMMING"); // Compute spectrum
                    std::vector<double> frequencies = psd.get_PSD(0); //Extract the frequencies for the psd

                    // Find the first index of the frequencies which is higher or equal to IAFinf
                    // and find the last index of the frequencies which is lower or equal to IAFsup
                    std::pair<int,int> freqBounds = MBT_frequencyBounds(frequencies, IAFinf, IAFsup);
                    int n_f7 = freqBounds.first;
                    int n_f13 = freqBounds.second;

                    // Find the value and the index of EEG peak into the range [IAFinf:IAFsup]
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
