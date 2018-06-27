#include <iostream>
#include <stdio.h>
#include <vector>
#include <map>
#include <algorithm> // std::min_element
#include <iterator>  // std::begin, std::end
#include <string>
#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_ReadInputOrWriteOutput.h"
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
    //////////////////////////
    // PIPELINE OF MELOMIND //
    //////////////////////////

    // DURING THE CALIBRATION
    // ----------------------
    // If we don't receive the packet : quality = 0 (in qualityCalibration) and data packet is set to 250 NaN values (in rawCalibrationData)
    // For each channel x data segment (2 x 250):
    // - linear interpolation of possible missing values
    // - remove the mean
    // - filter the powerline noise
    // - compute quality with quality checker which can replace data by NaN values if quality is bad
    // - Add the new values in qualityCalibration and rawCalibrationData

    // AT THE END OF CALIBRATION
    // -------------------------
    // For the FULL EEG signal of calibration (2 x ....):
    // - remove NaN values (bad quality or not received data)
    // - remove mean
    // - filter the powerline noise
    // - bandpass between 2 and 30Hz
    // - set the thresholds for the outliers
    // For each second with 4-second packet : remove NaN values, remove mean, filter the powerline noise, bandpass between 2 and 30 Hz and interpolate the outliers based on calibration data
    // - apply MBT_ComputeCalibration with the preprocessed signal: get the bestChannel and Smoothed SNR of 1-second EEG packet in calibration


    // DURING THE SESSION
    // ------------------
    // If we don't receive the packet : quality = 0 (in qualitySession) and data packet is set to 250 NaN values (in rawSessionData)
    // Compute quality with quality checker which can replace data by NaN values if quality is bad
    // Linear interpolation of possible missing values
    // Add the new values in qualityCalibration and rawCalibrationData
    // Create pastRelaxIndex (a vector of float), which will hold the relaxation indexes of the session
    // For each channel x data segment of 4s (with sliding window) (2 x 1000):
    // - remove NaN values (bad quality or not received data)
    // - remove the mean
    // - filter the powerline noise
    // - compute quality with quality checker
    // - bandpass between 2 and 30Hz
    // - interpolate outliers based on calibration data
    // - apply MBT_ComputeRelaxIndex with the preprocessed signal of the session and the parameters from the calibration: get the SNR of the best channel defined in the calibration
    // - store the SNR of session in pastRelaxIndex
    // - apply MBT_SmoothRelaxIndex to smooth the last relaxation index with the relaxation indexes of the past: get the smoothed relaxation index
    // - apply MBT_NormalizeRelaxIndex to normalize the smoothed SNR of session with the Smoothed SNR from calibration.
    // - apply MBT_RelaxIndexToVolum to transform the smoothed relaxation index into volum: get the corresponding volum value.
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // Test pipeline
    // ----------------------------------------------------------------------------------------------------------------------------------------------

    float sampRate = 250;
    unsigned int packetLength = 250;
    float IAFinf = 7;
    float IAFsup = 13;
    //MBT_Matrix<float> calibrationRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/trueCalibrationRecordings.txt");
    MBT_Matrix<float> calibrationRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/calibPb.txt");
    //MBT_Matrix<float> calibrationRecordingsQuality = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/trueCalibrationRecordingsQuality.txt");
    MBT_Matrix<float> calibrationRecordingsQuality = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/calibPbQuality.txt");


    // Set the thresholds for the outliers
    // -----------------------------------
    MBT_Matrix<float> Bounds(calibrationRecordings.size().first,2);

    for (int ch=0; ch<calibrationRecordings.size().first; ch++)
    {
        std::vector<float> signal_ch = calibrationRecordings.row(ch);

		if (std::all_of(signal_ch.begin(), signal_ch.end(), [](double testNaN){return std::isnan(testNaN);}) )
		{
			errno = EINVAL;
			perror("ERROR: BAD CALIBRATION - WE HAVE ONLY NAN VALUES");
		}

        signal_ch.erase(std::remove_if(signal_ch.begin(), signal_ch.end(),[](float testNaN){return std::isnan(testNaN);}),signal_ch.end()); // remove NaN values
        std::vector<float> calibrationRecordings_withoutDC = RemoveDC(signal_ch); // Remove DC
        // Notch
        std::vector<float> freqBounds;
        freqBounds.assign(2,0);
        freqBounds[0] = 2.0;
        freqBounds[1] = 30.0;
        std::vector<float> tmp_outputBandpass = BandPassFilter(calibrationRecordings_withoutDC,freqBounds); // Bandpass
        std::vector<float> tmp_Bounds = CalculateBounds(tmp_outputBandpass); // Set the thresholds for outliers
        Bounds(ch,0) = tmp_Bounds[0];
        Bounds(ch,1) = tmp_Bounds[1];
    }
    // -----------------------------------
    std::cout<<std::endl;
    std::cout<<"CALIBRATION CALIBRATION CALIBRATION CALIBRATION"<<std::endl;

    std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(calibrationRecordings,calibrationRecordingsQuality, sampRate, packetLength, IAFinf, IAFsup, Bounds);
    std::vector<float> tmp_bestChannel = paramCalib["BestChannel"];
    std::vector<float> tmp_SNRCalib = paramCalib["SNRCalib_ofBestChannel"];
    std::vector<float> tmp_BoundsCalib = paramCalib["Bounds_For_Outliers"];

    std::vector<std::complex<float> > BoundsCalib;
    for (unsigned int ki=0;ki<tmp_BoundsCalib.size();ki++)
    {
        BoundsCalib.push_back(std::complex<float>(tmp_BoundsCalib[ki], 0));
    }
    MBT_writeVector (BoundsCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/BoundsCalib.txt");

    std::vector<std::complex<float> > bestChannel;
    for (unsigned int ki=0;ki<tmp_bestChannel.size();ki++)
    {
        bestChannel.push_back(std::complex<float>(tmp_bestChannel[ki], 0));
    }
    MBT_writeVector (bestChannel, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/bestChannel.txt");

    std::vector<std::complex<float> > SNRCalib;
    for (unsigned int ki=0;ki<tmp_SNRCalib.size();ki++)
    {
        SNRCalib.push_back(std::complex<float>(tmp_SNRCalib[ki], 0));
    }
    MBT_writeVector (SNRCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/SNRCalib.txt");

    std::cout<<std::endl;
    std::cout<<"SESSION SESSION SESSION SESSION SESSION SESSION"<<std::endl;

    std::vector<float> pastRelaxIndex;
    std::vector<float> SmoothedRelaxIndex;
    std::vector<float> NormalizedRelaxIndex;
    std::vector<float> Volum;

    //MBT_Matrix<float> sessionRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/trueSessionRecordings.txt");
    MBT_Matrix<float> sessionRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/sessionPb.txt");

    unsigned int nbPacket = sessionRecordings.size().second/(sampRate)-3;
    for (unsigned int indPacket = 0; indPacket<nbPacket; indPacket++)
    {
        clock_t msecs;
        msecs = clock();

        MBT_Matrix<float> sessionPacket(2, 4*sampRate);;
        for (unsigned int sample=0; sample<4*sampRate; sample++)
        {
            sessionPacket(0,sample) = sessionRecordings(0,indPacket*sampRate+sample);
            sessionPacket(1,sample) = sessionRecordings(1,indPacket*sampRate+sample);
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

    /*MBT_Matrix<float> sessionPacket = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputData.txt");
    float RelaxationIndex = MBT_ComputeRelaxIndex(sessionPacket, paramCalib, sampRate, IAFinf, IAFsup);
    std::cout<<"RelaxationIndex (session) = "<<RelaxationIndex<<std::endl;
    pastRelaxIndex.push_back(RelaxationIndex);
    float tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
    SmoothedRelaxIndex.push_back(tmp_SmoothedRelaxIndex);
    float tmp_NormalizedRelaxIndex = MBT_NormalizeRelaxIndex(tmp_SmoothedRelaxIndex, tmp_SNRCalib);
    NormalizedRelaxIndex.push_back(tmp_NormalizedRelaxIndex);
    float tmp_Volum = MBT_RelaxIndexToVolum(tmp_NormalizedRelaxIndex);
    Volum.push_back(tmp_Volum);

    sessionPacket = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputData.txt");
    RelaxationIndex = MBT_ComputeRelaxIndex(sessionPacket, paramCalib, sampRate, IAFinf, IAFsup);
    std::cout<<"RelaxationIndex (session) = "<<RelaxationIndex<<std::endl;
    pastRelaxIndex.push_back(RelaxationIndex);
    tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
    SmoothedRelaxIndex.push_back(tmp_SmoothedRelaxIndex);
    tmp_NormalizedRelaxIndex = MBT_NormalizeRelaxIndex(tmp_SmoothedRelaxIndex, tmp_SNRCalib);
    NormalizedRelaxIndex.push_back(tmp_NormalizedRelaxIndex);
    tmp_Volum = MBT_RelaxIndexToVolum(tmp_NormalizedRelaxIndex);
    Volum.push_back(tmp_Volum);

    sessionPacket = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputData.txt");
    RelaxationIndex = MBT_ComputeRelaxIndex(sessionPacket, paramCalib, sampRate, IAFinf, IAFsup);
    std::cout<<"RelaxationIndex (session) = "<<RelaxationIndex<<std::endl;
    pastRelaxIndex.push_back(RelaxationIndex);
    tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
    SmoothedRelaxIndex.push_back(tmp_SmoothedRelaxIndex);
    tmp_NormalizedRelaxIndex = MBT_NormalizeRelaxIndex(tmp_SmoothedRelaxIndex, tmp_SNRCalib);
    NormalizedRelaxIndex.push_back(tmp_NormalizedRelaxIndex);
    tmp_Volum = MBT_RelaxIndexToVolum(tmp_NormalizedRelaxIndex);
    Volum.push_back(tmp_Volum);

    sessionPacket = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputData.txt");
    RelaxationIndex = MBT_ComputeRelaxIndex(sessionPacket, paramCalib, sampRate, IAFinf, IAFsup);
    std::cout<<"RelaxationIndex (session) = "<<RelaxationIndex<<std::endl;
    pastRelaxIndex.push_back(RelaxationIndex);
    tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
    SmoothedRelaxIndex.push_back(tmp_SmoothedRelaxIndex);
    tmp_NormalizedRelaxIndex = MBT_NormalizeRelaxIndex(tmp_SmoothedRelaxIndex, tmp_SNRCalib);
    NormalizedRelaxIndex.push_back(tmp_NormalizedRelaxIndex);
    tmp_Volum = MBT_RelaxIndexToVolum(tmp_NormalizedRelaxIndex);
    Volum.push_back(tmp_Volum);

    sessionPacket = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputData.txt");
    RelaxationIndex = MBT_ComputeRelaxIndex(sessionPacket, paramCalib, sampRate, IAFinf, IAFsup);
    std::cout<<"RelaxationIndex (session) = "<<RelaxationIndex<<std::endl;
    pastRelaxIndex.push_back(RelaxationIndex);
    tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
    SmoothedRelaxIndex.push_back(tmp_SmoothedRelaxIndex);
    tmp_NormalizedRelaxIndex = MBT_NormalizeRelaxIndex(tmp_SmoothedRelaxIndex, tmp_SNRCalib);
    NormalizedRelaxIndex.push_back(tmp_NormalizedRelaxIndex);
    tmp_Volum = MBT_RelaxIndexToVolum(tmp_NormalizedRelaxIndex);
    Volum.push_back(tmp_Volum);

    sessionPacket = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputData.txt");
    RelaxationIndex = MBT_ComputeRelaxIndex(sessionPacket, paramCalib, sampRate, IAFinf, IAFsup);
    std::cout<<"RelaxationIndex (session) = "<<RelaxationIndex<<std::endl;
    pastRelaxIndex.push_back(RelaxationIndex);
    tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
    SmoothedRelaxIndex.push_back(tmp_SmoothedRelaxIndex);
    tmp_NormalizedRelaxIndex = MBT_NormalizeRelaxIndex(tmp_SmoothedRelaxIndex, tmp_SNRCalib);
    NormalizedRelaxIndex.push_back(tmp_NormalizedRelaxIndex);
    tmp_Volum = MBT_RelaxIndexToVolum(tmp_NormalizedRelaxIndex);
    Volum.push_back(tmp_Volum);

    sessionPacket = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputData.txt");
    RelaxationIndex = MBT_ComputeRelaxIndex(sessionPacket, paramCalib, sampRate, IAFinf, IAFsup);
    std::cout<<"RelaxationIndex (session) = "<<RelaxationIndex<<std::endl;
    pastRelaxIndex.push_back(RelaxationIndex);
    tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
    SmoothedRelaxIndex.push_back(tmp_SmoothedRelaxIndex);
    tmp_NormalizedRelaxIndex = MBT_NormalizeRelaxIndex(tmp_SmoothedRelaxIndex, tmp_SNRCalib);
    NormalizedRelaxIndex.push_back(tmp_NormalizedRelaxIndex);
    tmp_Volum = MBT_RelaxIndexToVolum(tmp_NormalizedRelaxIndex);
    Volum.push_back(tmp_Volum);

    sessionPacket = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputData.txt");
    RelaxationIndex = MBT_ComputeRelaxIndex(sessionPacket, paramCalib, sampRate, IAFinf, IAFsup);
    std::cout<<"RelaxationIndex (session) = "<<RelaxationIndex<<std::endl;
    pastRelaxIndex.push_back(RelaxationIndex);
    tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
    SmoothedRelaxIndex.push_back(tmp_SmoothedRelaxIndex);
    tmp_NormalizedRelaxIndex = MBT_NormalizeRelaxIndex(tmp_SmoothedRelaxIndex, tmp_SNRCalib);
    NormalizedRelaxIndex.push_back(tmp_NormalizedRelaxIndex);
    tmp_Volum = MBT_RelaxIndexToVolum(tmp_NormalizedRelaxIndex);
    Volum.push_back(tmp_Volum);

    sessionPacket = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputData.txt");
    RelaxationIndex = MBT_ComputeRelaxIndex(sessionPacket, paramCalib, sampRate, IAFinf, IAFsup);
    std::cout<<"RelaxationIndex (session) = "<<RelaxationIndex<<std::endl;
    pastRelaxIndex.push_back(RelaxationIndex);
    tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
    SmoothedRelaxIndex.push_back(tmp_SmoothedRelaxIndex);
    tmp_NormalizedRelaxIndex = MBT_NormalizeRelaxIndex(tmp_SmoothedRelaxIndex, tmp_SNRCalib);
    NormalizedRelaxIndex.push_back(tmp_NormalizedRelaxIndex);
    tmp_Volum = MBT_RelaxIndexToVolum(tmp_NormalizedRelaxIndex);
    Volum.push_back(tmp_Volum);

    sessionPacket = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputData.txt");
    RelaxationIndex = MBT_ComputeRelaxIndex(sessionPacket, paramCalib, sampRate, IAFinf, IAFsup);
    std::cout<<"RelaxationIndex (session) = "<<RelaxationIndex<<std::endl;
    pastRelaxIndex.push_back(RelaxationIndex);
    tmp_SmoothedRelaxIndex = MBT_SmoothRelaxIndex(pastRelaxIndex);
    SmoothedRelaxIndex.push_back(tmp_SmoothedRelaxIndex);
    tmp_NormalizedRelaxIndex = MBT_NormalizeRelaxIndex(tmp_SmoothedRelaxIndex, tmp_SNRCalib);
    NormalizedRelaxIndex.push_back(tmp_NormalizedRelaxIndex);
    tmp_Volum = MBT_RelaxIndexToVolum(tmp_NormalizedRelaxIndex);
    Volum.push_back(tmp_Volum);*/

    std::vector<std::complex<float> > SNRSession;
    for (unsigned int ki=0;ki<SmoothedRelaxIndex.size();ki++)
    {
        SNRSession.push_back(std::complex<float>(pastRelaxIndex[ki], 0));
    }
    MBT_writeVector (SNRSession, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/SNRSession.txt");

    std::vector<std::complex<float> > SmoothedSNRSession;
    for (unsigned int ki=0;ki<SmoothedRelaxIndex.size();ki++)
    {
        SmoothedSNRSession.push_back(std::complex<float>(SmoothedRelaxIndex[ki], 0));
    }
    MBT_writeVector (SmoothedSNRSession, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/SmoothedSNRSession.txt");

    std::vector<std::complex<float> > NormalizedSmoothedSNRSession;
    for (unsigned int ki=0;ki<NormalizedRelaxIndex.size();ki++)
    {
        NormalizedSmoothedSNRSession.push_back(std::complex<float>(NormalizedRelaxIndex[ki], 0));
    }
    MBT_writeVector (NormalizedSmoothedSNRSession, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/NormalizedSmoothedSNRSession.txt");


    std::vector<std::complex<float> > VolumNormalizedSmoothedSNRSession;
    for (unsigned int ki=0;ki<Volum.size();ki++)
    {
        VolumNormalizedSmoothedSNRSession.push_back(std::complex<float>(Volum[ki], 0));
    }
    MBT_writeVector (VolumNormalizedSmoothedSNRSession, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/VolumNormalizedSmoothedSNRSession.txt");


/*
    MBT_Matrix<float> testfft = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/trueCalibrationRecordings.txt");
    std::vector<float> signal = testfft.row(0);

    std::vector<std::complex<double>> signal_H;
    for (unsigned int ki=0;ki<signal.size();ki++)
    {
        signal_H.push_back(std::complex<double>(signal[ki], 0));
    }
    int signal_HSize = signal_H.size();
    DOUBLE_FFTW_C2C_1D *trans1 = new DOUBLE_FFTW_C2C_1D(signal_HSize, signal_H,-1);
    std::vector<complex<double> > Vec1(signal_HSize);
    Vec1 = trans1 -> fft_execute();
    delete trans1;

    std::vector<double> tmp_fftTransformed;
    for (int q = 0; q < Vec1.size(); q++)
    {
        tmp_fftTransformed.push_back(real(Vec1[q]));
    }
    std::vector<std::complex<float> > fftTransformed;
    for (unsigned int ki=0;ki<tmp_fftTransformed.size();ki++)
    {
        fftTransformed.push_back(std::complex<float>(tmp_fftTransformed[ki], 0));
    }
    MBT_writeVector (fftTransformed, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/fftTransformed.txt");


    DOUBLE_FFTW_C2C_1D *obj = new DOUBLE_FFTW_C2C_1D(Vec1.size(), Vec1, 1);
    std::vector<complex<double> > IFFTH(Vec1.size());
    IFFTH = obj->ifft_execute();
    delete obj;

    std::vector<double> tmp_ifftTransformed;
    for (int q = 0; q <  IFFTH.size(); q++)
    {
        tmp_ifftTransformed.push_back(imag(IFFTH[q]));
    }
    std::vector<std::complex<float> > ifftTransformed;
    for (unsigned int ki=0;ki<tmp_ifftTransformed.size();ki++)
    {
        ifftTransformed.push_back(std::complex<float>(tmp_ifftTransformed[ki], 0));
    }
    MBT_writeVector (ifftTransformed, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/ifftTransformed.txt");




    MBT_Matrix<float> tmp_inputForPwelch = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputForPwelch.txt");
    //std::vector<float> inputForPwelch = tmp_inputForPwelch.row(0);
    MBT_Matrix<double> inputForPwelch(tmp_inputForPwelch.size().first,tmp_inputForPwelch.size().second);
    for (int i=0;i<tmp_inputForPwelch.size().first;i++)
    {
        for (int j=0;j<tmp_inputForPwelch.size().second;j++)
        {
            inputForPwelch(i,j) = tmp_inputForPwelch(i,j);
        }
    }

    MBT_PWelchComputer psd = MBT_PWelchComputer(inputForPwelch, (double)250, "HAMMING"); // Compute spectrum
    std::vector<double> frequencies = psd.get_PSD(0); //Extract the frequencies for the psd
    std::vector<double> chpsd = psd.get_PSD(1);

    std::vector<std::complex<float> > writechpsd;
    for (unsigned int ki=0;ki<chpsd.size();ki++)
    {
        writechpsd.push_back(std::complex<float>(chpsd[ki], 0));
    }
    MBT_writeVector (writechpsd, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/writechpsd.txt");

*/
    // ----------------------------------------------------------------------------------------------------------------------------------------------






    // Test of MBT_ComputeSNR
    // ----------------------
    /*float sampRate = 250;
    float IAFinf = 7;
    float IAFsup = 13;
    MBT_Matrix<float> signal = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputData.txt");
    std::vector<float> tmp_SNR = MBT_ComputeSNR(signal, sampRate, IAFinf, IAFsup);

    std::vector<std::complex<float> > SNR;
    for (int ki=0;ki<tmp_SNR.size();ki++)
    {
        SNR.push_back(std::complex<float>(tmp_SNR[ki], 0));
    }
    MBT_writeVector (SNR, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/SNR.txt");*/
    // ------------------------



    // Test of MBT_ComputeCalibration
    // ------------------------------
    /*float sampRate = 250;
    int packetLength = 250;
    float IAFinf = 7;
    float IAFsup = 13;
    MBT_Matrix<float> signal = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputData.txt");
    MBT_Matrix<float> calibrationRecordingsQuality = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/calibrationRecordingsQuality.txt");
    std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(signal,calibrationRecordingsQuality, sampRate, packetLength, IAFinf, IAFsup);
    std::vector<float> tmp_bestChannel = paramCalib["BestChannel"];
    std::vector<float> tmp_SNRCalib = paramCalib["SNRCalib_ofBestChannel"];

    std::vector<std::complex<float> > bestChannel;
    for (int ki=0;ki<tmp_bestChannel.size();ki++)
    {
        bestChannel.push_back(std::complex<float>(tmp_bestChannel[ki], 0));
    }
    MBT_writeVector (bestChannel, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/bestChannel.txt");

    std::vector<std::complex<float> > SNRCalib;
    for (int ki=0;ki<tmp_SNRCalib.size();ki++)
    {
        SNRCalib.push_back(std::complex<float>(tmp_SNRCalib[ki], 0));
    }
    MBT_writeVector (SNRCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/SNRCalib.txt");*/


    /*float sampRate = 250;
    int packetLength = 250;
    float IAFinf = 7;
    float IAFsup = 13;
    MBT_Matrix<float> calibrationRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/calibrationRecordings.txt");

    // Both Best
    MBT_Matrix<float> calibrationRecordingsQuality = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/calibrationRecordingsQuality_BOTHBest.txt");
    std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(calibrationRecordings,calibrationRecordingsQuality, sampRate, packetLength, IAFinf, IAFsup);
    std::vector<float> tmp_bestChannel = paramCalib["BestChannel"];
    std::vector<float> tmp_SNRCalib = paramCalib["SNRCalib_ofBestChannel"];

    std::vector<std::complex<float> > bestChannel;
    for (int ki=0;ki<tmp_bestChannel.size();ki++)
    {
        bestChannel.push_back(std::complex<float>(tmp_bestChannel[ki], 0));
    }
    MBT_writeVector (bestChannel, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/bestChannel_BOTHBest.txt");

    std::vector<std::complex<float> > SNRCalib;
    for (int ki=0;ki<tmp_SNRCalib.size();ki++)
    {
        SNRCalib.push_back(std::complex<float>(tmp_SNRCalib[ki], 0));
    }
    MBT_writeVector (SNRCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/SNRCalib_BOTHBest.txt");

    // Both Bad
    MBT_Matrix<float> calibrationRecordingsQuality = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/calibrationRecordingsQuality_BOTHBad.txt");
    std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(calibrationRecordings,calibrationRecordingsQuality, sampRate, packetLength, IAFinf, IAFsup);
    std::vector<float> tmp_bestChannel = paramCalib["BestChannel"];
    std::vector<float> tmp_SNRCalib = paramCalib["SNRCalib_ofBestChannel"];

    std::vector<std::complex<float> > bestChannel;
    for (int ki=0;ki<tmp_bestChannel.size();ki++)
    {
        bestChannel.push_back(std::complex<float>(tmp_bestChannel[ki], 0));
    }
    MBT_writeVector (bestChannel, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/bestChannel_BOTHBad.txt");

    std::vector<std::complex<float> > SNRCalib;
    for (int ki=0;ki<tmp_SNRCalib.size();ki++)
    {
        SNRCalib.push_back(std::complex<float>(tmp_SNRCalib[ki], 0));
    }
    MBT_writeVector (SNRCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/SNRCalib_BOTHBad.txt");


    // 1st Good
    MBT_Matrix<float> calibrationRecordingsQuality = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/calibrationRecordingsQuality_1stGood.txt");
    std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(calibrationRecordings,calibrationRecordingsQuality, sampRate, packetLength, IAFinf, IAFsup);
    std::vector<float> tmp_bestChannel = paramCalib["BestChannel"];
    std::vector<float> tmp_SNRCalib = paramCalib["SNRCalib_ofBestChannel"];

    std::vector<std::complex<float> > bestChannel;
    for (int ki=0;ki<tmp_bestChannel.size();ki++)
    {
        bestChannel.push_back(std::complex<float>(tmp_bestChannel[ki], 0));
    }
    MBT_writeVector (bestChannel, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/bestChannel_1stGood.txt");

    std::vector<std::complex<float> > SNRCalib;
    for (int ki=0;ki<tmp_SNRCalib.size();ki++)
    {
        SNRCalib.push_back(std::complex<float>(tmp_SNRCalib[ki], 0));
    }
    MBT_writeVector (SNRCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/SNRCalib_1stGood.txt");


    // 2nd Good
    MBT_Matrix<float> calibrationRecordingsQuality = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/calibrationRecordingsQuality_2ndGood.txt");
    std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(calibrationRecordings,calibrationRecordingsQuality, sampRate, packetLength, IAFinf, IAFsup);
    std::vector<float> tmp_bestChannel = paramCalib["BestChannel"];
    std::vector<float> tmp_SNRCalib = paramCalib["SNRCalib_ofBestChannel"];

    std::vector<std::complex<float> > bestChannel;
    for (int ki=0;ki<tmp_bestChannel.size();ki++)
    {
        bestChannel.push_back(std::complex<float>(tmp_bestChannel[ki], 0));
    }
    MBT_writeVector (bestChannel, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/bestChannel_2ndGood.txt");

    std::vector<std::complex<float> > SNRCalib;
    for (int ki=0;ki<tmp_SNRCalib.size();ki++)
    {
        SNRCalib.push_back(std::complex<float>(tmp_SNRCalib[ki], 0));
    }
    MBT_writeVector (SNRCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/SNRCalib_2ndGood.txt");*/
    // ------------------------------

    // Test of MBT_ComputeRelaxIndex
    // -----------------------------
    /*float sampRate = 250;
    int packetLength = 250;
    float IAFinf = 7;
    float IAFsup = 13;
    MBT_Matrix<float> signal = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/calibrationRecordings.txt");
    MBT_Matrix<float> calibrationRecordingsQuality = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFilescalibrationRecordingsQuality.txt");
    std::map<std::string, std::vector<float> > paramCalib = MBT_ComputeCalibration(signal,calibrationRecordingsQuality, sampRate, packetLength, IAFinf, IAFsup);
    std::vector<float> tmp_bestChannel = paramCalib["BestChannel"];
    std::vector<float> tmp_SNRCalib = paramCalib["SNRCalib_ofBestChannel"];

    std::vector<std::complex<float> > bestChannel;
    for (int ki=0;ki<tmp_bestChannel.size();ki++)
    {
        bestChannel.push_back(std::complex<float>(tmp_bestChannel[ki], 0));
    }
    MBT_writeVector (bestChannel, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/bestChannel.txt");

    std::vector<std::complex<float> > SNRCalib;
    for (int ki=0;ki<tmp_SNRCalib.size();ki++)
    {
        SNRCalib.push_back(std::complex<float>(tmp_SNRCalib[ki], 0));
    }
    MBT_writeVector (SNRCalib, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/SNRCalib.txt");




    MBT_Matrix<float> inputNAN(2,250);
    for (int i = 0;i<2;i++)
    {
        for (int j=0;j<250;j++)
        {
            inputNAN(i,j) = nan(" ");
        }
    }


    MBT_Matrix<float> sessionPacket = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/inputData.txt");
    float RelaxationIndex = MBT_ComputeRelaxIndex(sessionPacket, paramCalib, sampRate, IAFinf, IAFsup);
    std::cout<<"RelaxationIndex (session) = "<<RelaxationIndex<<std::endl;*/
    // -----------------------------

    // Test of MBT_SmoothRelaxIndex
    // ----------------------------
    /*std::vector<float> pastRelaxIndexes;

    pastRelaxIndexes.push_back(0.1);
    pastRelaxIndexes.push_back(0.5);
    pastRelaxIndexes.push_back(nan(" "));
    pastRelaxIndexes.push_back(0.1);
    pastRelaxIndexes.push_back(0.5);
    pastRelaxIndexes.push_back(nan(" "));
    pastRelaxIndexes.push_back(std::numeric_limits<float>::infinity());
    float SmoothedRelaxationIndex = MBT_SmoothRelaxIndex(pastRelaxIndexes);
    std::cout<<"SmoothedRelaxationIndex = "<<SmoothedRelaxationIndex<<std::endl;*/
    // ----------------------------

    // Test of MBT_RelaxIndexToVolum
    // -----------------------------
    /*float volum = MBT_RelaxIndexToVolum(SmoothedRelaxationIndex);
    std::cout<<"volum = "<<volum<<std::endl;*/
    // -----------------------------



    // Test MBT_PWelchComputer
    // -----------------------
    /*float sampRate = 250;
    MBT_Matrix<float> signal = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/Calib_4s.txt");*/

    /*std::vector<float> signal_ch = signal.row(0);
    signal_ch.erase(std::remove_if(signal_ch.begin(), signal_ch.end(),[](float testNaN){return std::isnan(testNaN);}),signal_ch.end()); // remove NaN values
    //std::cout<<"signal_ch.size() = "<<signal_ch.size()<<std::endl;
    MBT_Matrix<float> signal_ch_mat(1,signal_ch.size());
    for (int m = 0; m<signal_ch_mat.size().second;m++)
    {
        signal_ch_mat(0,m) = signal_ch[m];
    }
    MBT_Matrix<float> correctedSignal = MBT_trendCorrection(signal_ch_mat,sampRate);*/

    /*MBT_PWelchComputer psd = MBT_PWelchComputer(signal, sampRate, "HAMMING"); // Compute spectrum
    std::vector<float> frequencies = psd.get_PSD(0); //Extract the frequencies for the psd
    std::vector<float> tmp_channelPSD1 = psd.get_PSD(1);
    //std::vector<float> tmp_channelPSD2 = psd.get_PSD(2);


    std::vector<std::complex<float> > frequenciesPSD;
    for (int ki=0;ki<frequencies.size();ki++)
    {
        frequenciesPSD.push_back(std::complex<float>(frequencies[ki], 0));
    }
    MBT_writeVector (frequenciesPSD, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/frequenciesPSD.txt");

    std::vector<std::complex<float> > channelPSD1;
    for (int ki=0;ki<tmp_channelPSD1.size();ki++)
    {
        channelPSD1.push_back(std::complex<float>(tmp_channelPSD1[ki], 0));
    }
    MBT_writeVector (channelPSD1, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/channelPSD1.txt");*/

    /*std::vector<std::complex<float> > channelPSD2;
    for (int ki=0;ki<tmp_channelPSD2.size();ki++)
    {
        channelPSD2.push_back(std::complex<float>(tmp_channelPSD2[ki], 0));
    }
    MBT_writeVector (channelPSD2, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/channelPSD2.txt");*/




    // Test preprocessing
    // ------------------
    /*calibrationRecordings = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/trueCalibrationRecordings.txt");
    std::vector<float> CalibVector = calibrationRecordings.row(0);
    std::vector<float> tmp_testDCremoval = RemoveDC(CalibVector);
    std::vector<std::complex<float> > testDCremoval;
    for (int ki=0;ki<tmp_testDCremoval.size();ki++)
    {
        testDCremoval.push_back(std::complex<float>(tmp_testDCremoval[ki], 0));
    }
    MBT_writeVector (testDCremoval, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/testDCremoval.txt");


    std::vector<float> tmp_Bounds = CalculateBounds(tmp_testDCremoval);
    std::vector<std::complex<float> > Bounds;
    for (int ki=0;ki<tmp_Bounds.size();ki++)
    {
        Bounds.push_back(std::complex<float>(tmp_Bounds[ki], 0));
    }
    MBT_writeVector (Bounds, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/Bounds.txt");

    MBT_Matrix<float> tmp_testDCremoval2 = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/testDCremoval2.txt");
    std::vector<float> tmp_testDCremoval3 = tmp_testDCremoval2.row(0);
    std::vector<float> tmp_dataWithoutOutliers = InterpolateOutliers(tmp_testDCremoval3, tmp_Bounds);
    std::vector<std::complex<float> > dataWithoutOutliers;
    for (int ki=0;ki<tmp_dataWithoutOutliers.size();ki++)
    {
        dataWithoutOutliers.push_back(std::complex<float>(tmp_dataWithoutOutliers[ki], 0));
    }
    MBT_writeVector (dataWithoutOutliers, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/dataWithoutOutliers.txt");
    */

    // Test bandpass
    // -------------
    /*MBT_Matrix<float> tmp_inputBandpass = MBT_readMatrix("C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Files/TestFiles/InputBandpass.txt");
    std::vector<float> inputBandpass = tmp_inputBandpass.row(0);

    std::vector<float> tmp_outputBandpass = BandPassFilter(inputBandpass);

    std::vector<std::complex<float> > outputBandpass;
    for (int ki=0;ki<tmp_outputBandpass.size();ki++)
    {
        outputBandpass.push_back(std::complex<float>(tmp_outputBandpass[ki], 0));
    }
    MBT_writeVector (outputBandpass, "C:/Users/Fanny/Documents/Melomind.Algorithms/NF_Melomind/Results/TestFiles/outputBandpass.txt");
*/
    return 0;
}
