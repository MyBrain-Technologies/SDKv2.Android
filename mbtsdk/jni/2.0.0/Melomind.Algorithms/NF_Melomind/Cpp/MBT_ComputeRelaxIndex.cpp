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
//          Fanny Grosselin 2017/10/10 --> Remove the preprocessing of the outliers because it will be done in MBT_ComputeSNR.
//          Fanny Grosselin 2017/10/16 --> Compute SNR on the other channel each time we have 4 consecutive seconds with a quality of 0 in the best channel.

#include "../Headers/MBT_ComputeRelaxIndex.h"

float MBT_ComputeRelaxIndex(MBT_Matrix<float> sessionPacket, std::map<std::string, std::vector<float> > parametersFromCalibration, const float sampRate, const float IAFinf, const float IAFsup)
{
    // Get BestChannel from the calibration
    std::vector<float> bestChannelVector = parametersFromCalibration["BestChannel"];

    int bestChannel = round(bestChannelVector[0]);

    double relaxIndex;

    if ((sessionPacket.size().first>0) & (sessionPacket.size().second>0) & (bestChannel != -2) & (bestChannel != -1))
    {
        MBT_Matrix<double> packetChannel(sessionPacket.size().first,sessionPacket.size().second);
        std::vector<int> otherChannel;
        for (int sample = 0; sample < sessionPacket.size().second; sample++)
        {
            for (int ch = 0;ch < sessionPacket.size().first; ch++)
            {
                packetChannel(ch,sample) = nan(" ");
                if (!std::isnan(sessionPacket(ch,sample)))
                {
                    packetChannel(ch,sample) = sessionPacket(ch,sample);
                }
                if (ch!=bestChannel)
                {
                    otherChannel.push_back(ch);
                }
            }
        }

        // Get the data of the best channel
        // --------------------------------
        std::vector<double> packetBestChannelVector = packetChannel.row(bestChannel); // get the vector inside the matrix to test if all the element are NaN
        // --------------------------------

        // Get the data of the other channel
        // ---------------------------------
        std::vector<double> packetOtherChannelVector = packetChannel.row(otherChannel[0]); // just in case we have more than 2 channels, to avoid bug
        // ---------------------------------

        // Put in matrix to go to MBT_ComputeSNR.cpp
        // -----------------------------------------
        MBT_Matrix<double> packetBestChannel(1,sessionPacket.size().second);
        MBT_Matrix<double> packetOtherChannel(1,sessionPacket.size().second);
        for (int sample = 0; sample < sessionPacket.size().second; sample++)
        {
            packetBestChannel(0,sample) = packetChannel(bestChannel,sample);
            packetOtherChannel(0,sample) = packetChannel(otherChannel[0],sample);
        }
        // -----------------------------------------


        std::vector<double> SNRSession;

        if (std::all_of(packetBestChannelVector.begin(), packetBestChannelVector.end(), [](double testNaN){return std::isnan(testNaN);}) & std::all_of(packetOtherChannelVector.begin(), packetOtherChannelVector.end(), [](double testNaN){return std::isnan(testNaN);}) )
        {
            SNRSession.push_back(nan(" "));
        }
        else
        {
            if (std::all_of(packetBestChannelVector.begin(), packetBestChannelVector.end(), [](double testNaN){return std::isnan(testNaN);}) )
            {
                // Compute SNR with the other channel
                SNRSession = MBT_ComputeSNR(packetOtherChannel, double(sampRate), double(IAFinf), double(IAFsup)); // there is only one value but this is a vector
            }
            else
            {
                // Compute SNR with the best channel
                SNRSession = MBT_ComputeSNR(packetBestChannel, double(sampRate), double(IAFinf), double(IAFsup)); // there is only one value but this is a vector
            }
        }
        relaxIndex = SNRSession[0];
    }
    else
    {
        // Store values to be handled in case of problem into MBT_ComputeRelaxIndex
        relaxIndex = std::numeric_limits<double>::infinity();
        errno = EINVAL;
        perror("ERROR: MBT_COMPUTERELAXINDEX CANNOT PROCESS WITHOUT GOOD INPUTS");
    }
    return relaxIndex;
}
