// //
//  NewMain.cpp
//
//  Created by Katerina Pandremmenou on 18/09/17.
//  Inspired by main.cpp of the same folder
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//  
//  The purpose of this file is to test the new proposed preprocessing :
//                 i) receiving NaN values (for lost or very bad values from the QC),
//                ii) apply an IQR and set the outliers to NaN
//               iii) interpolate the nan values between the channels
//                iv) interpolate each channel across itself 
//                 v) remove possible NaN values from the beginning or the end of an MBT_Matrix
// Update : Fanny Grosselin 2017/09/27 : remove the step where we remove nan if there still exist in the beginning or the end of the MBT_Matrix

#include <iostream>
#include <stdio.h>
#include <vector>
#include <map>
#include <algorithm> // std::min_element
#include <iterator>  // std::begin, std::end
#include <string>
#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_ReadInputOrWriteOutput.h"
#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_ReadInputOrWriteOutput.h"
#include "../../SignalProcessing.Cpp/Algebra/Headers/MBT_FindClosest.h"
#include "../Headers/MBT_ComputeSNR.h"
#include "../Headers/MBT_ComputeCalibration.h"
#include "../Headers/MBT_ComputeRelaxIndex.h"
#include "../Headers/MBT_SmoothRelaxIndex.h"
#include "../Headers/MBT_NormalizeRelaxIndex.h"
#include "../Headers/MBT_RelaxIndexToVolum.h"
#include "../../SignalProcessing.Cpp/PreProcessing/Headers/MBT_PreProcessing.h"
#include "../../SignalProcessing.Cpp/PreProcessing/Headers/MBT_BandPass_fftw3.h"

using namespace std;

int main()
{
    // Test pipeline
    // ----------------------------------------------------------------------------------------------------------------------------------------------

    float sampRate = 250;
    unsigned int packetLength = 250;
    float IAFinf = 7;
    float IAFsup = 13;
    MBT_Matrix<float> calibrationRecordings = MBT_readMatrix("../Files/TestFiles/calibrationRecordingswithNaN.txt");
    MBT_Matrix<float> calibrationRecordingsQuality = MBT_readMatrix("../Files/TestFiles/calibrationRecordingsQuality.txt");

    // Set the thresholds for the outliers
    // -----------------------------------
    MBT_Matrix<float> Bounds(calibrationRecordings.size().first,2);
    MBT_Matrix<float> Test(calibrationRecordings.size().first, calibrationRecordings.size().second);

    for (int ch=0; ch<calibrationRecordings.size().first; ch++)
    {
        vector<float> signal_ch = calibrationRecordings.row(ch);

		if (all_of(signal_ch.begin(), signal_ch.end(), [](double testNaN){return isnan(testNaN);}) )
		{
			errno = EINVAL;
			perror("ERROR: BAD CALIBRATION - WE HAVE ONLY NAN VALUES");
        }
        
        // skip the NaN values in order to calculate the Bounds
        vector<float>tmp_signal_ch = SkipNaN(signal_ch);

        // find the bounds 
        vector<float> tmp_Bounds = CalculateBounds(tmp_signal_ch); // Set the thresholds for outliers
        Bounds(ch,0) = tmp_Bounds[0];
        Bounds(ch,1) = tmp_Bounds[1];

        // basically, we convert from vector<float> to vector<double>
        vector<double> CopySignal_ch(signal_ch.begin(), signal_ch.end());
        vector<double> Copytmp_Bounds(tmp_Bounds.begin(), tmp_Bounds.end());

        // set outliers to nan
        vector<double> InterCopySignal_ch = MBT_OutliersToNan(CopySignal_ch, Copytmp_Bounds);
        for (unsigned int t = 0 ; t < InterCopySignal_ch.size(); t++)
            Test(ch,t) = (float) InterCopySignal_ch[t];
    }

    // interpolate the nan values between the channels
    MBT_Matrix<float> FullyInterpolatedTest = MBT_InterpolateBetweenChannels(Test);

    // interpolate the nan values across each channel
    MBT_Matrix<float> InterpolatedAcrossChannels = MBT_InterpolateAcrossChannels(FullyInterpolatedTest);
    
    // remove the nan if there still exist in the beginning or the end of the MBT_Matrix
    //MBT_Matrix<float> DataWithoutNaN = RemoveNaNIfAny(InterpolatedAcrossChannels);

    // this code is just to test the results
    /*for (int r1 = 0; r1 < DataWithoutNaN.size().first; r1++)
    {
        for (int r2 = 0; r2 < DataWithoutNaN.size().second; r2++)
        {
            cout << DataWithoutNaN(r1,r2) << ",";
        }
        cout << endl;
    }*/

    // -----------------------------------
    std::cout<<std::endl;
    std::cout<<"CALIBRATION CALIBRATION CALIBRATION CALIBRATION"<<std::endl;

    std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(InterpolatedAcrossChannels,calibrationRecordingsQuality, sampRate, packetLength, IAFinf, IAFsup, Bounds);
    std::vector<float> tmp_bestChannel = paramCalib["BestChannel"];
    std::vector<float> tmp_SNRCalib = paramCalib["SNRCalib_ofBestChannel"];
    std::vector<float> tmp_BoundsCalib = paramCalib["Bounds_For_Outliers"];

    std::vector<std::complex<float> > BoundsCalib;
    for (unsigned int ki=0;ki<tmp_BoundsCalib.size();ki++)
    {
        BoundsCalib.push_back(std::complex<float>(tmp_BoundsCalib[ki], 0));
    }
    //MBT_writeVector (BoundsCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/BoundsCalib.txt");

    std::vector<std::complex<float> > bestChannel;
    for (unsigned int ki=0;ki<tmp_bestChannel.size();ki++)
    {
        bestChannel.push_back(std::complex<float>(tmp_bestChannel[ki], 0));
    }
    //MBT_writeVector (bestChannel, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/bestChannel.txt");

    std::vector<std::complex<float> > SNRCalib;
    for (unsigned int ki=0;ki<tmp_SNRCalib.size();ki++)
    {
        SNRCalib.push_back(std::complex<float>(tmp_SNRCalib[ki], 0));
    }
    //MBT_writeVector (SNRCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/SNRCalib.txt");

    std::cout<<std::endl;
    std::cout<<"SESSION SESSION SESSION SESSION SESSION SESSION"<<std::endl;

    std::vector<float> pastRelaxIndex;
    std::vector<float> SmoothedRelaxIndex;
    std::vector<float> NormalizedRelaxIndex;
    std::vector<float> Volum;

    //MBT_Matrix<float> sessionRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/trueSessionRecordings.txt");
    //MBT_Matrix<float> sessionRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/sessionRecordingswithNaN.txt");
    MBT_Matrix<float> sessionRecordings = MBT_readMatrix("../Files/TestFiles/sessionRecordingswithNaN.txt");

    unsigned int nbPacket = (int) (sessionRecordings.size().second/(sampRate)-3);
    for (unsigned int indPacket = 0; indPacket<nbPacket; indPacket++)
    {
        clock_t msecs;
        msecs = clock();

        MBT_Matrix<float> sessionPacket(2, 4*(int)sampRate);;
        for (unsigned int sample=0; sample<4*sampRate; sample++)
        {
            sessionPacket(0,sample) = sessionRecordings(0,indPacket*(int)sampRate+sample);
            sessionPacket(1,sample) = sessionRecordings(1,indPacket*(int)sampRate+sample);
        }
        float RelaxationIndex = MBT_ComputeRelaxIndex(sessionPacket, paramCalib, sampRate, IAFinf, IAFsup);
        pastRelaxIndex.push_back(RelaxationIndex);
        float tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
        SmoothedRelaxIndex.push_back(tmp_SmoothedRelaxIndex);
        float tmp_NormalizedRelaxIndex = MBT_NormalizeRelaxIndex(tmp_SmoothedRelaxIndex, tmp_SNRCalib);
        NormalizedRelaxIndex.push_back(tmp_NormalizedRelaxIndex);
        float tmp_Volum = MBT_RelaxIndexToVolum(tmp_NormalizedRelaxIndex);
        Volum.push_back(tmp_Volum);
        std::cout << "Execution time = "<< ((float((clock()-msecs))) / CLOCKS_PER_SEC) << std::endl;
    }

    return 0;
}
