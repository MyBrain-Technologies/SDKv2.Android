//
//  MBT_ComputeCalibration.cpp
//
//  Created by Fanny Grosselin on 04/01/17.
//  Inspired by MBT_ComputeCalibration.cpp of Emma Barme on 20/10/2015.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
// Update: Fanny Grosselin : 2017/01/16 Smooth the SNR of calibration.
//         Fanny Grosselin : 2017/01/26 Compute SNR on segments of 4s but with a sliding window of 1s.
//         Fanny Grosselin : 2017/02/03 Add in inputs and for MBT_ComputeSNR, the Bounds to detect the outliers of the signal.
//		   Fanny Grosselin : 2017/02/14 Add the values of Bounds when there is error.
// 		   Fanny Grosselin : 2017/03/23 Change float by double for the functions not directly used by Andro�d. For the others, keep inputs and outputs in double, but do the steps with double.
//         Fanny Grosselin : 2017/03/27 Fix all the warnings.
//         Fanny Grosselin : 2017/09/18 Remove BoundsRow from the input of MBT_ComputeSNR
//         Katerina Pandremmenou : 2017/09/20 Change all implicit type castings to explicit ones
//         Fanny Grosselin : 2017/09/27 Instead of dividing meanQualities by the number of packets kept, we divide by the number of second during calibration
//		   Fanny Grosselin : 2017/09/27 Change the decision rule to know if the calibration is good.
//         Katerina Pandremmenou : 2017/09/28 Consider the bounds for the outliers for both channels of the calibration
//                                            Put infinite outliers to all 4 bounds in cases of a bad calibration 
//         Fanny Grosselin : 2017/09/28 Change the decision rule to know if the calibration is good.

#include "../Headers/MBT_ComputeCalibration.h"

#define MIN_QUALITY_PER_PACKET 0.5

#define GENERAL_QUALITY_THRESHOLD 0.5
#define CHANNEL_QUALITY_THRESHOLD 0.75

std::map<std::string, std::vector<float> > MBT_ComputeCalibration(MBT_Matrix<float> calibrationRecordings, MBT_Matrix<float> calibrationRecordingsQuality, const float sampRate, const int packetLength, const float IAFinf, const float IAFsup, MBT_Matrix<float> Bounds)
{
    if ((calibrationRecordings.size().first > 0) & (calibrationRecordings.size().second > 0) & (calibrationRecordingsQuality.size().first > 0) & (calibrationRecordingsQuality.size().second > 0) & (calibrationRecordings.size().first == calibrationRecordingsQuality.size().first) & (calibrationRecordings.size().second == calibrationRecordingsQuality.size().second*packetLength))
    {
        std::map<std::string, std::vector<float> > calibrationParameters;
        vector<float> BoundsForBothChannels;

        //Selecting the packets with a good enough quality value.
        //The minimum quality value for a packet to be taken into account. Hard coded to 0.5.
        int channelNb = calibrationRecordings.size().first;
        std::vector<std::vector<int> > packetsToKeepIndex; //A two-dimensional vector holding for each channel (row) the indices of the good packets.
        packetsToKeepIndex.resize(channelNb);
        std::vector<double> meanQualities;
        meanQualities.assign(channelNb, 0);
        //For each channel
        for (int channelIndex = 0; channelIndex < channelNb; channelIndex++)
        {
            //For each packet
            for (int packetIndex = 0; packetIndex < calibrationRecordingsQuality.size().second; packetIndex++)
            {
                double quality = calibrationRecordingsQuality(channelIndex, packetIndex);
                //If the signal is good enough
                if (quality >= MIN_QUALITY_PER_PACKET)
                {
                    meanQualities[channelIndex] += quality;
                    packetsToKeepIndex[channelIndex].push_back(packetIndex);
                }
            }
            //meanQualities[channelIndex] /= packetsToKeepIndex[channelIndex].size();
			meanQualities[channelIndex] /= calibrationRecordingsQuality.size().second;
        }
		//double AverageMeanQuality = std::accumulate(meanQualities.begin(), meanQualities.end(), 0.0);
		//AverageMeanQuality = AverageMeanQuality/meanQualities.size();
        unsigned int counter = 0;
        for (unsigned int co = 0; co<meanQualities.size(); co++)
        {
            if(meanQualities[co] >= CHANNEL_QUALITY_THRESHOLD){
                counter = (unsigned int) meanQualities.size();
                break;
            }
            if (meanQualities[co] >= GENERAL_QUALITY_THRESHOLD)
            {
                counter = counter + 1;
            }
        }          
        
        //If all the channels have an average qualty higher or equal to 0.5, the calibration is good. Otherwise the calibration fails.
        // Return value to be handled in case of problem / false and if needed to start calibration over and to use different values than default
        if (counter<meanQualities.size())
        {
            // Store values to be handled in case of problem into MBT_ComputeCalibration
            std::vector<float> SmoothedSNRCalib;
            SmoothedSNRCalib.push_back(std::numeric_limits<float>::infinity());
            std::vector<float> bestChannel;
            bestChannel.push_back(-2);
			std::vector<float> BoundsCalib;
			BoundsCalib.push_back(std::numeric_limits<float>::infinity());
            BoundsCalib.push_back(std::numeric_limits<float>::infinity());
            BoundsCalib.push_back(std::numeric_limits<float>::infinity());
			BoundsCalib.push_back(std::numeric_limits<float>::infinity());
            calibrationParameters["BestChannel"] = bestChannel;
            calibrationParameters["SNRCalib_ofBestChannel"] = SmoothedSNRCalib;
            calibrationParameters["Bounds_For_Outliers"] = BoundsCalib;
            errno = EINVAL;
            perror("ERROR: MBT_COMPUTECALIBRATION CANNOT PROCESS WITH ONLY BAD QUALITY SIGNALS");

            return calibrationParameters;
        }
		
		//Selecting the channel with the highest quality value.
        //TODO: Make it possible to use several channels at the same time.
        int bestChannelIndex = -1;
        double bestMeanQuality = -1;
        for (int channelIndex = 0; channelIndex < channelNb; channelIndex++)
        {
            if (bestMeanQuality < meanQualities[channelIndex]) // by this way if quality are equal in both channel, we keep the first one that is to say the left channel
            {
                bestMeanQuality = meanQualities[channelIndex];
                bestChannelIndex = channelIndex;
            }
        }

        std::vector<float> SNRCalib;
        std::vector<float> SmoothedSNRCalib;
        //Creating a new matrix with only the data values for the packets with a good quality value of the channel with the best mean quality value.
        int Buffer = 4 * (int) sampRate;
        int SlidWin = Buffer/4;
        MBT_Matrix<double> goodCalibrationRecordings(1, Buffer);

        for (int k = 0;k< packetsToKeepIndex[bestChannelIndex].size()*sampRate - Buffer + SlidWin; k= k +SlidWin)
        //for (int k = 0;k< packetsToKeepIndex[bestChannelIndex].size()*sampRate - Buffer + SlidWin; k= k +SlidWin*4)
        {
            int tmp1 = k;
            int tmp2 = Buffer + k - 1;
            for (int dataPointInPacketIndex = tmp1; dataPointInPacketIndex < tmp2 + 1; dataPointInPacketIndex++)
            {
                goodCalibrationRecordings(0, dataPointInPacketIndex-tmp1) = calibrationRecordings(bestChannelIndex, dataPointInPacketIndex);
            }
            //std::vector<float> tmp_BoundsRow = Bounds.row(bestChannelIndex);
            //std::vector<double> BoundsRow(tmp_BoundsRow.begin(), tmp_BoundsRow.end());
            std::vector<double> SNRCalibPacket = MBT_ComputeSNR(goodCalibrationRecordings, double(sampRate), double(IAFinf), double(IAFsup)); // there is only one value into the vector SNRCalib because packetOfGoodCalibrationRecordings contains one segment of 4 seconds of values of only the best channel and we apply a sliding window of 1s.
            SNRCalib.push_back((float)SNRCalibPacket[0]);
            SmoothedSNRCalib.push_back(MBT_SmoothRelaxIndex(SNRCalib)); // we smooth the SNR from calibration
        }

        /*for (int packetIndex = 0; packetIndex < packetsToKeepIndex[bestChannelIndex].size(); packetIndex++)
        {
            for (int dataPointInPacketIndex = 0; dataPointInPacketIndex < packetLength; dataPointInPacketIndex++)
            {
                goodCalibrationRecordings(0, dataPointInPacketIndex) = calibrationRecordings(bestChannelIndex, dataPointInPacketIndex + packetIndex * packetLength);
            }
            std::vector<double> SNRCalibPacket = MBT_ComputeSNR(goodCalibrationRecordings, sampRate, IAFinf, IAFsup); // there is only one value into the vector SNRCalib because packetOfGoodCalibrationRecordings contains one packet of values of only the best channel.
            //std::cout<<"SNRCalibPacket = "<<SNRCalibPacket[0]<<std::endl;
            SNRCalib.push_back(SNRCalibPacket[0]);
            SmoothedSNRCalib.push_back(MBT_SmoothRelaxIndex(SNRCalib)); // we smooth the SNR from calibration
        }*/

        std::vector<float> bestChannel;
        bestChannel.push_back(bestChannelIndex);

        // Store the parameters into calibrationParameters
        calibrationParameters["BestChannel"] = bestChannel;
        calibrationParameters["SNRCalib_ofBestChannel"] = SmoothedSNRCalib;
        for (int t1 = 0 ; t1 < Bounds.size().first; t1++)
        {
            for (int t2 = 0 ; t2 < Bounds.size().second; t2++)
                // the order of the bounds is: LowCh1, HighCh1, LowCh2, HighCh2
                BoundsForBothChannels.push_back(Bounds(t1,t2));
        }

        //calibrationParameters["Bounds_For_Outliers"] = Bounds.row(bestChannelIndex);
        calibrationParameters["Bounds_For_Outliers"] = BoundsForBothChannels;

        return calibrationParameters;
    }

    else
    {
        // Store values to be handled in case of problem into MBT_ComputeCalibration
        std::map<std::string, std::vector<float> > calibrationParameters;
        std::vector<float> SmoothedSNRCalib;
        SmoothedSNRCalib.push_back(std::numeric_limits<float>::infinity());
        std::vector<float> bestChannel;
        bestChannel.push_back(-1);
		std::vector<float> BoundsCalib;
        BoundsCalib.push_back(std::numeric_limits<float>::infinity());
        BoundsCalib.push_back(std::numeric_limits<float>::infinity());
        BoundsCalib.push_back(std::numeric_limits<float>::infinity());
        BoundsCalib.push_back(std::numeric_limits<float>::infinity());
        calibrationParameters["BestChannel"] = bestChannel;
        calibrationParameters["SNRCalib_ofBestChannel"] = SmoothedSNRCalib;
		calibrationParameters["Bounds_For_Outliers"] = BoundsCalib;
        errno = EINVAL;
        perror("ERROR: MBT_COMPUTECALIBRATION CANNOT PROCESS WITHOUT GOOD INPUTS");

        return calibrationParameters;
    }

}
