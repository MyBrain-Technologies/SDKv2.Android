//
//  FinalNewMain.cpp
//
//  Created by Fanny Grosselin on 10/10/17.
//  Inspired by NewMain.cpp of the same folder
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
//  The purpose of this file is to test the final new proposed preprocessing just for the best channel directly in MBT_ComputeSNR.
//  Indeed, we don't compute the boundaries of the outliers from the whole signal of calibration but second by second directly in MBT_ComputeSNR.
//  In MBT_ComputeSNR, the steps are:
//                 i) receiving NaN values (for lost or very bad values from the QC),
//                ii) do a linear interpolation of NaN values
//               iii) remove possible remain NaN values from the beginning or the end of the best channel
//                iv) apply DC removal
//                 v) apply a bandpass filter between 2 and 30Hz
//                iv) compute SNR


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
    MBT_Matrix<float> calibrationRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/NewRelease/trueCalibrationRecordings.txt");
    MBT_Matrix<float> calibrationRecordingsQuality = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/NewRelease/trueCalibrationRecordingsQuality.txt");


    // -----------------------------------
    std::cout<<std::endl;
    std::cout<<"CALIBRATION CALIBRATION CALIBRATION CALIBRATION"<<std::endl;

    std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(calibrationRecordings,calibrationRecordingsQuality, sampRate, packetLength, IAFinf, IAFsup);
    std::vector<float> tmp_bestChannel = paramCalib["BestChannel"];
    std::vector<float> tmp_SNRCalib = paramCalib["SNRCalib_ofBestChannel"];


    std::vector<std::complex<float> > bestChannel;
    for (unsigned int ki=0;ki<tmp_bestChannel.size();ki++)
    {
        bestChannel.push_back(std::complex<float>(tmp_bestChannel[ki], 0));
    }
    MBT_writeVector (bestChannel, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/bestChannel.txt");

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
    std::vector<float> Volum;

    MBT_Matrix<float> sessionRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/NewRelease/trueSessionRecordings.txt");

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
        float tmp_Volum = MBT_RelaxIndexToVolum(tmp_SmoothedRelaxIndex, paramCalib);
        Volum.push_back(tmp_Volum);
        std::cout << "Execution time = "<< ((float((clock()-msecs))) / CLOCKS_PER_SEC) << std::endl;
    }

    std::vector<std::complex<float> > SNRSession;
    for (unsigned int ki=0;ki<pastRelaxIndex.size();ki++)
    {
        SNRSession.push_back(std::complex<float>(pastRelaxIndex[ki], 0));
    }
    MBT_writeVector (SNRSession, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/SNRSession.txt");

    std::vector<std::complex<float> > SmoothedSNRSession;
    for (unsigned int ki=0;ki<SmoothedRelaxIndex.size();ki++)
    {
        SmoothedSNRSession.push_back(std::complex<float>(SmoothedRelaxIndex[ki], 0));
    }
    MBT_writeVector (SmoothedSNRSession, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/SmoothedSNRSession.txt");

    std::vector<std::complex<float> > VolumSmoothedSNRSession;
    for (unsigned int ki=0;ki<Volum.size();ki++)
    {
        VolumSmoothedSNRSession.push_back(std::complex<float>(Volum[ki], 0));
    }
    MBT_writeVector (VolumSmoothedSNRSession, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/VolumSmoothedSNRSession.txt");



    return 0;
}

//computes the calibration parameters

//input:
//sampRate : the sampling rate
//packetLenght : the number of eeg data per eegPacket
//calibrationRecordings
//calibrationRecordingsQualities
//
//output : the calibration map 
std::map<string, std::vector<float>> main_calibration(float sampRate, unsigned int packetLength, MBT_Matrix<float> calibrationRecordings, MBT_Matrix<float> calibrationRecordingsQuality)
{
    float IAFinf = 7;
    float IAFsup = 13;

    // Calibration-----------------------------------

    std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(calibrationRecordings,calibrationRecordingsQuality, sampRate, packetLength, IAFinf, IAFsup);

    return paramCalib;
}



float main_relaxIndex(float sampRate, std::map<std::string, std::vector<float> > paramCalib, MBT_Matrix<float> sessionPacket)
{
    float IAFinf = 7;
    float IAFsup = 13;

    // Session-----------------------------------
    float RelaxationIndex = MBT_ComputeRelaxIndex(sessionPacket, paramCalib, sampRate, IAFinf, IAFsup);

    pastRelaxIndex.push_back(RelaxationIndex);
    float tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
    SmoothedRelaxIndex.push_back(tmp_SmoothedRelaxIndex);
    float tmp_Volum = MBT_RelaxIndexToVolum(tmp_SmoothedRelaxIndex, paramCalib);
	

    return tmp_Volum;
}
