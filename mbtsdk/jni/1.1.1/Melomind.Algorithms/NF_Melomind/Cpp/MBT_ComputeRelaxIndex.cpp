//
//  MBT_ComputeRelaxIndex.cpp
//
//  Created by Fanny Grosselin on 06/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
//  Update: Fanny Grosselin 2017/01/16 --> Remove the normalization of SNR because we do that after smoothing and not before.
//          Fanny Grosselin 2017/02/03 --> Get the Bounds (from parametersFromCalibration) to detect the outliers of the signal.
// 		    Fanny Grosselin 2017/03/23 --> Change float by double for the functions not directly used by Andro�d. For the others, keep inputs and outputs in double, but do the steps with double.
//          Fanny Grosselin 2017/03/27 --> Fix all the warnings.
//          Fanny Grosselin 2017/09/18 --> Remove Bounds from the input of MBT_ComputeSNR
//          Katerina Pandremmenou 2017/09/20 --> Change the preprocessing (use the bounds from calibration and set the outliers to NaN
//                                               interpolate the nan values between the channels, interpolate each channel across itself,
//                                               remove possible remaining nan values in the beginning or the end of an MBT_Matrix.
//                                               Change all implicit type castings to explicit ones
//          Katerina Pandremmenou 2017/09/28 --> Put all the block with outliers to nan, interpolatation between and across channels, 
//                                               in the case where the calibration is good.
//                                           --> Put outliers of BOTH channels to NaN. Put in comments the function for ignoring remaining nan values. (This is done in MBT_ComputeSNR file).

#include "../Headers/MBT_ComputeRelaxIndex.h"

float MBT_ComputeRelaxIndex(MBT_Matrix<float> sessionPacket, std::map<std::string, std::vector<float> > parametersFromCalibration, const float sampRate, const float IAFinf, const float IAFsup)
{
    // Get BestChannel from the calibration
    std::vector<float> bestChannelVector = parametersFromCalibration["BestChannel"];
    std::vector<float> tmp_Bounds = parametersFromCalibration["Bounds_For_Outliers"]; // Fanny Grosselin 2017/02/03
    std::vector<double> Bounds(tmp_Bounds.begin(), tmp_Bounds.end());
    std::vector<double> BoundsPerChan;

    int bestChannel = (int) round(bestChannelVector[0]);

    double relaxIndex;
    
    if ((sessionPacket.size().first>0) & (sessionPacket.size().second>0) & (bestChannel != -2) & (bestChannel != -1))
    {
        for (int r1 = 0 ; r1 < sessionPacket.size().first; r1++)
        {
            vector<float> NewsessionPacket = sessionPacket.row(r1);
            vector<double> CopysessionPacket(NewsessionPacket.begin(), NewsessionPacket.end());
    
            // set the outliers to NaN based on the bounds found from calibration
            // bounds for the specific channel
            BoundsPerChan.push_back(Bounds[2*r1]);
            BoundsPerChan.push_back(Bounds[2*r1+1]);

            vector<double> InterpolatedsessionPacket = MBT_OutliersToNan(CopysessionPacket, BoundsPerChan);
            for (unsigned int t = 0 ; t < InterpolatedsessionPacket.size(); t++)
                sessionPacket(r1,t) = (float) InterpolatedsessionPacket[t];
        }
    
        // interpolate the nan values between the channels
        MBT_Matrix<float> InterpolatedsessionPacket = MBT_InterpolateBetweenChannels(sessionPacket);
        // interpolate the nan values across each channel
        MBT_Matrix<float> InterpolatedAcrossChannelssessionPackets = MBT_InterpolateAcrossChannels(InterpolatedsessionPacket);
        // remove the nan if there still exist in the beginning or the end of the MBT_Matrix
        //MBT_Matrix<float> DataWithoutNaN = RemoveNaNIfAny(InterpolatedAcrossChannelssessionPackets);
    
        // Get  SNR vector from the calibration
        //std::vector<double> SNRCalib = parametersFromCalibration["SNRCalib_ofBestChannel"];
        MBT_Matrix<double> packetBestChannel(1,InterpolatedAcrossChannelssessionPackets.size().second);
        for (int sample = 0; sample < InterpolatedAcrossChannelssessionPackets.size().second; sample++)
        {
            packetBestChannel(0,sample) = nan(" ");
            if (!std::isnan(InterpolatedAcrossChannelssessionPackets(bestChannel,sample)))
            {
                packetBestChannel(0,sample) = InterpolatedAcrossChannelssessionPackets(bestChannel,sample);
            }
        }

        std::vector<double> SNRSession;

        std::vector<double> packetBestChannelVector = packetBestChannel.row(0); // get the vector inside the matrix to test if all the element are NaN
        if (std::all_of(packetBestChannelVector.begin(), packetBestChannelVector.end(), [](double testNaN){return std::isnan(testNaN);}) )
        {
            SNRSession.push_back(nan(" "));
        }
        else
        {
            // Compute SNR
            SNRSession = MBT_ComputeSNR(packetBestChannel, double(sampRate), double(IAFinf), double(IAFsup)); // there is only one value but this is a vector
        }

        // Normalize SNR
        /*double meanSNRCalib = mean(SNRCalib);
        double stdSNRCalib = standardDeviation(SNRCalib);
        relaxIndex = SNRSession[0] - meanSNRCalib;
        if (stdSNRCalib != 0)
        {
            relaxIndex = relaxIndex/stdSNRCalib;
        }*/
        relaxIndex = SNRSession[0];
    }
    else
    {
        // Store values to be handled in case of problem into MBT_ComputeRelaxIndex
        relaxIndex = std::numeric_limits<double>::infinity();
        errno = EINVAL;
        perror("ERROR: MBT_COMPUTERELAXINDEX CANNOT PROCESS WITHOUT GOOD INPUTS");
    }
    return (float) relaxIndex;
}
