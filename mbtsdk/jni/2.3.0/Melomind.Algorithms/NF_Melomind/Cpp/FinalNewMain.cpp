//
//  FinalNewMain.cpp
//
//  Created by Fanny Grosselin on 10/10/17.
//  Inspired by NewMain.cpp of the same folder
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
//  The purpose of this file is to test the final pipeline that integrate the new way i) to detect the alpha peak, ii) to compute the signal background noise
//  and iii) so to compute a combined SNR from both channels.
//  This pipeline used the preprocessing of the v2.1.0 versionning but ignoring the step concerning the choice of values from the other channel when the EEG values of the
//  best channel are NaN. Indeed, we have not anymore the notion of best channel because we compute SNR on both channel and not only on the best channel.


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
#include "../../version.h" // version.h of NF_Melomind

using namespace std;

//Here are defined the alpha band limits
#define IAFinf 6 // warning: it's 6 and not 7
#define IAFsup 13


// COMPUTE THE CALIBRATION PARAMETERS

// INPUTS:
// ------
// sampRate: [float] the sampling rate
// packetLength: [int] the number of eeg data per eegPacket
// calibrationRecordings: [MBT_Matrix<float>] the EEG signals as it's returned by the QualityChecker (number of rows = number of channels)
// calibrationRecordingsQualities: [MBT_Matrix<float>] the quality of each EEG signal as it's returned by the QualityChecker (number of rows = number of channels)
//
// OUTPUTS:
// -------
// the calibration map

std::map<string, std::vector<float>> main_calibration(float sampRate, unsigned int packetLength, MBT_Matrix<float> calibrationRecordings, MBT_Matrix<float> calibrationRecordingsQuality)
{
    std::vector<float> histFreq;

    // Calibration-----------------------------------
    std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(calibrationRecordings,calibrationRecordingsQuality, sampRate, packetLength, IAFinf, IAFsup);

    return paramCalib;
}


// PLAY THE SESSION

// INPUTS:
// ------
// sampRate: [float] the sampling rate
// paramCalib: the calibration map
// sessionPacket: [MBT_Matrix<float>] the EEG signals on real-time during session as it's returned by the QualityChecker (number of rows = number of channels)
// histFreq: [vector of float] containing the frequency of the alpha peak detected previously (previously means during calibration and the previous packets of the session)
//            If it's the first packet of the session: get histFreq from paramCalib --> histFreq = paramCalib["HistFrequencies"];
//            else: get histFreq from session -->  histFreq = paramSession["HistFrequencies"];
// pastRelaxIndex: [vector of float] containing the previous relax index computed during the session (not smoothed).
//
// OUTPUT:
// ------
// a map containing some measures during the session (snr, smoothed snr, history of frequency, volum)
//TODO maybe pass a map in input that is filled each time this is called instead of recreating temp vectors
//TODO paramCalib could also be a reference
float main_relaxIndex(const float sampRate, std::map<std::string, std::vector<float> > paramCalib,
                                                     const MBT_Matrix<float> &sessionPacket, std::vector<float> &histFreq, std::vector<float> &pastRelaxIndex, std::vector<float> &resultSmoothedSNR, std::vector<float> &resultVolum)
{

    std::vector<float> errorMsg = paramCalib["errorMsg"];
    std::vector<float> snrCalib = paramCalib["snrCalib"];

    // Session-----------------------------------
    float snrValue = MBT_ComputeRelaxIndex(sessionPacket, errorMsg, sampRate, IAFinf, IAFsup, histFreq);

    //TODO pastRelaxIndex could also be passed as reference in MBT_ComputeRelaxIndex. See if it is relevant to do so
    pastRelaxIndex.push_back(snrValue); // incrementation of pastRelaxIndex
    float smoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
    float volum = MBT_RelaxIndexToVolum(smoothedRelaxIndex, snrCalib); // warning it's not the same inputs than previously

    //resultSmoothedSNR.assign(1,smoothedRelaxIndex);
    resultSmoothedSNR.push_back(smoothedRelaxIndex);

    //resultVolum.assign(1,volum);
    resultVolum.push_back(volum);
    // WARNING: If it's possible, I would like to save in the .json file, pastRelaxIndex and smoothedRelaxIndex (both)

    return volum;
}


int main()
{
    // Calibration
    // ----------------------------------------------------------------------------------------------------------------------------------------------
    std::cout<<"CALIBRATION"<<std::endl;
    float sampRate = 250;
    unsigned int packetLength = 250;
    std::cout<<"Reading file..."<<std::endl;
//    MBT_Matrix<float> calibrationRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/NewRelease/trueCalibrationRecordings.txt");
//    MBT_Matrix<float> calibrationRecordingsQuality = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/NewRelease/trueCalibrationRecordingsQuality.txt");
//    MBT_Matrix<float> calibrationRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/NewRelease/PbCalibrationRecordings.txt");
//    MBT_Matrix<float> calibrationRecordingsQuality = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/NewRelease/PbCalibrationRecordingsQuality.txt");
    MBT_Matrix<float> calibrationRecordings = MBT_readMatrix("../Data/CalibrationRecordings.txt"); // absolute path for calling the executable with matlab
    MBT_Matrix<float> calibrationRecordingsQuality = MBT_readMatrix("../Data/CalibrationRecordingsQuality.txt"); // absolute path for calling the executable with matlab
    std::cout<<"End of reading"<<std::endl;

    std::map<string, std::vector<float>> paramCalib = main_calibration(sampRate, packetLength, calibrationRecordings, calibrationRecordingsQuality);



    std::vector<float> histFreq = paramCalib["histFrequencies"]; // update histFreq
    std::vector<float> tmp_errorMsg = paramCalib["errorMsg"];
    std::vector<float> tmp_SNRCalib = paramCalib["snrCalib"];
    std::vector<float> tmp_RawSNRCalib = paramCalib["rawSnrCalib"];

    // just to get them in text files
    std::vector<std::complex<float> > w_histFreq;
    for (unsigned int ki=0;ki<histFreq.size();ki++)
    {
        w_histFreq.push_back(std::complex<float>(histFreq[ki], 0));
    }
//    MBT_writeVector (w_histFreq, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/histFreqCalib.txt");
//    MBT_writeVector (w_histFreq, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/PbhistFreqCalib.txt");
    MBT_writeVector (w_histFreq, "../Results/histFreqCalib.txt"); // absolute path for calling the executable with matlab

    std::vector<std::complex<float> > errorMsg;
    for (unsigned int ki=0;ki<tmp_errorMsg.size();ki++)
    {
        errorMsg.push_back(std::complex<float>(tmp_errorMsg[ki], 0));
    }
//    MBT_writeVector (errorMsg, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/errorMsg.txt");
//    MBT_writeVector (errorMsg, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/PberrorMsg.txt");
    MBT_writeVector (errorMsg, "../Results/errorMsg.txt"); // absolute path for calling the executable with matlab

    std::vector<std::complex<float> > SNRCalib;
    for (unsigned int ki=0;ki<tmp_SNRCalib.size();ki++)
    {
        SNRCalib.push_back(std::complex<float>(tmp_SNRCalib[ki], 0));
    }
//    MBT_writeVector (SNRCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/SNRCalib.txt");
//    MBT_writeVector (SNRCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/PbSNRCalib.txt");
    MBT_writeVector (SNRCalib, "../Results/snrCalib.txt"); // absolute path for calling the executable with matlab


    std::vector<std::complex<float> > RawSNRCalib;
    for (unsigned int ki=0;ki<tmp_RawSNRCalib.size();ki++)
    {
        RawSNRCalib.push_back(std::complex<float>(tmp_RawSNRCalib[ki], 0));
    }
//    MBT_writeVector (RawSNRCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/RawSNRCalib.txt");
//    MBT_writeVector (RawSNRCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/PbRawSNRCalib.txt");
    MBT_writeVector (RawSNRCalib, "../Results/rawSnrCalib.txt"); // absolute path for calling the executable with matlab


    // Session
    // ----------------------------------------------------------------------------------------------------------------------------------------------
    std::cout<<"SESSION"<<std::endl;

    //TODO these vectors might be transformed into one single map to reduce the number of input. Better readability but less understandability
    std::vector<float> pastRelaxIndex;
    std::vector<float> smoothedRelaxIndex;
    std::vector<float> volum;

    std::cout<<"Reading file..."<<std::endl;
//    MBT_Matrix<float> sessionRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/NewRelease/trueSessionRecordings814.txt");
//    MBT_Matrix<float> sessionRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/NewRelease/PbSessionRecordings.txt");
    MBT_Matrix<float> sessionRecordings = MBT_readMatrix("../Data/SessionRecordings.txt"); // absolute path for calling the executable with matlab
    std::cout<<"End of reading"<<std::endl;

    unsigned int nbPacket = (unsigned int) (sessionRecordings.size().second/(sampRate)-3);
    for (unsigned int indPacket = 0; indPacket < nbPacket; indPacket++)
    {
        clock_t msecs;
        msecs = clock();

        MBT_Matrix<float> sessionPacket(2, 4*(int)sampRate);;
        for (unsigned int sample=0; sample<4*sampRate; sample++)
        {
            sessionPacket(0,sample) = sessionRecordings(0,indPacket*(int)sampRate+sample);
            sessionPacket(1,sample) = sessionRecordings(1,indPacket*(int)sampRate+sample);
        }
        //Volum value to return to the application
        float newVolum = main_relaxIndex(sampRate, paramCalib, sessionPacket, histFreq, pastRelaxIndex, smoothedRelaxIndex, volum);


        // get histFreq please! (in.json)
//
//        pastRelaxIndex = resultSession["rawSnrSession"];
//        float currentSNR = pastRelaxIndex[pastRelaxIndex.size()-1]; // get the last
//
//        std::vector<float> sessionSmoothedSNR = resultSession["smoothedSnrSession"];
//        float currentSmoothedSNR = sessionSmoothedSNR[sessionSmoothedSNR.size()-1]; // get the last
//
//        std::vector<float> sessionVolum = resultSession["volum"];
//        float currentVolum = sessionVolum[sessionVolum.size()-1]; // get the last
//
//
//        // Just to get them in a text file
//        smoothedRelaxIndex.push_back(currentSmoothedSNR);
//        volum.push_back(currentVolum);
        std::cout << "Execution time = "<< ((float((clock()-msecs))) / CLOCKS_PER_SEC) << std::endl;
    }

    std::vector<std::complex<float> > w2_histFreq;
    for (unsigned int ki=0;ki<histFreq.size();ki++)
    {
        w2_histFreq.push_back(std::complex<float>(histFreq[ki], 0));
    }
//    MBT_writeVector (w2_histFreq, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/histFreqSession.txt");
//    MBT_writeVector (w2_histFreq, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/PbhistFreqSession.txt");
    MBT_writeVector (w2_histFreq, "../Results/histFreqSession.txt");// absolute path for calling the executable with matlab

    std::vector<std::complex<float> > RawSNRSession;
    for (unsigned int ki=0;ki<pastRelaxIndex.size();ki++)
    {
        RawSNRSession.push_back(std::complex<float>(pastRelaxIndex[ki], 0));
    }
//    MBT_writeVector (RawSNRSession, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/RawSNRSession.txt");
//    MBT_writeVector (RawSNRSession, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/PbRawSNRSession.txt");
    MBT_writeVector (RawSNRSession, "../Results/rawSnrSession.txt");// absolute path for calling the executable with matlab

    std::vector<std::complex<float> > SmoothedSNRSession;
    for (unsigned int ki=0;ki<smoothedRelaxIndex.size();ki++)
    {
        SmoothedSNRSession.push_back(std::complex<float>(smoothedRelaxIndex[ki], 0));
    }
//    MBT_writeVector (SmoothedSNRSession, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/SmoothedSNRSession.txt");
//    MBT_writeVector (SmoothedSNRSession, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/PbSmoothedSNRSession.txt");
    MBT_writeVector (SmoothedSNRSession, "../Results/smoothedSnrSession.txt"); // absolute path for calling the executable with matlab

    std::vector<std::complex<float> > VolumSmoothedSNRSession;
    for (unsigned int ki=0;ki<volum.size();ki++)
    {
        VolumSmoothedSNRSession.push_back(std::complex<float>(volum[ki], 0));
    }
//    MBT_writeVector (VolumSmoothedSNRSession, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/VolumSmoothedSNRSession.txt");
//    MBT_writeVector (VolumSmoothedSNRSession, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/NewRelease/PbVolumSmoothedSNRSession.txt");
    MBT_writeVector (VolumSmoothedSNRSession, "../Results/volumSmoothedSnrSession.txt"); // absolute path for calling the executable with matlab

    std::cout<<"VERSION_SP = "<<VERSION_SP<<std::endl;
    std::cout<<"VERSION = "<<VERSION<<std::endl;


    return 0;
}

