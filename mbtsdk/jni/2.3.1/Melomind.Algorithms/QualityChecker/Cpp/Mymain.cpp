#include <iostream>
#include <stdio.h>
#include <vector>
#include <algorithm> // std::min_element
#include <iterator>  // std::begin, std::end


#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h" // use of the class MBT_Matrix
#include "../Headers/MBT_MainQC.h" // use of the class MBT_Matrix
#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_ReadInputOrWriteOutput.h" // use of the class MBT_ReadInputOrWriteOutput

#include "../../SignalProcessing.Cpp/Transformations/Headers/MBT_PWelchComputer.h" // use of the class MBT_PWelchComputer
#include "../../SignalProcessing.Cpp/Algebra/Headers/MBT_Operations.h"

#include "../../SignalProcessing.Cpp/PreProcessing/Headers/MBT_BandPass_fftw3.h"


int main()
{

    /////////////////////////////////// Complete test without MBT_featuresQualityCheck //////////////////////////////////////
    float sampRate = 250;

    // Construction de trainingFeatures
    //MBT_Matrix<float> trainingFeatures = MBT_readMatrix("../Files/trainingFeatures4.txt");
    //MBT_Matrix<float> trainingFeatures = MBT_readMatrix("../Files/trainingFeatures_BrainAmp.txt");
    //MBT_Matrix<float> trainingFeatures = MBT_readMatrix("../Files/trainingFeatures_Data480trainingQC.txt");
    //MBT_Matrix<float> trainingFeatures = MBT_readMatrix("../Files/trainingFeatures_Data420trainingQC.txt");
    //MBT_Matrix<float> trainingFeatures = MBT_readMatrix("../Files/trainingFeatures_Data420UnguidedtrainingQC.txt");
    MBT_Matrix<float> trainingFeatures = MBT_readMatrix("../Files/trainingFeatures_Data570UnguidedtrainingQCNEW4.txt");
    //MBT_Matrix<float> trainingFeatures = MBT_readMatrix("../Files/trainingFeatures_Data900trainingQC.txt");

    // Construction de trainingClasses
    //std::vector<std::complex<float> > tmp_trainingClasses = MBT_readVector("../Files/trainingClasses.txt");
    //std::vector<std::complex<float> > tmp_trainingClasses = MBT_readVector("../Files/trainingClasses_BrainAmp.txt");
    //std::vector<std::complex<float> > tmp_trainingClasses = MBT_readVector("../Files/trainingClasses _Data480trainingQC.txt");
    //std::vector<std::complex<float> > tmp_trainingClasses = MBT_readVector("../Files/trainingClasses_Data420trainingQC.txt");
    //std::vector<std::complex<float> > tmp_trainingClasses = MBT_readVector("../Files/trainingClasses_Data420UnguidedtrainingQC.txt");
    std::vector<std::complex<float> > tmp_trainingClasses = MBT_readVector("../Files/trainingClasses _Data570UnguidedtrainingQCNEW4.txt");
    //std::vector<std::complex<float> > tmp_trainingClasses = MBT_readVector("../Files/trainingClasses _Data900trainingQC.txt");
    std::vector<float> trainingClasses;
    trainingClasses.assign(tmp_trainingClasses.size(),0);
    for (unsigned int t=0;t<tmp_trainingClasses.size();t++)
    {
        trainingClasses[t] = tmp_trainingClasses[t].real();
    }

    // Construction de w
    //std::vector<std::complex<float> > tmp_w = MBT_readVector("../Files/w.txt");
    //std::vector<std::complex<float> > tmp_w = MBT_readVector("../Files/trainingW_BrainAmp.txt");
    //std::vector<std::complex<float> > tmp_w = MBT_readVector("../Files/trainingW_Data480trainingQC.txt");
    //std::vector<std::complex<float> > tmp_w = MBT_readVector("../Files/trainingW_Data420UnguidedtrainingQC.txt");
    //std::vector<std::complex<float> > tmp_w = MBT_readVector("../Files/trainingW_Data420trainingQC.txt");
    std::vector<std::complex<float> > tmp_w = MBT_readVector("../Files/trainingW_Data570UnguidedtrainingQCNEW4.txt");
    //std::vector<std::complex<float> > tmp_w = MBT_readVector("../Files/trainingW_Data900trainingQC.txt");
    std::vector<float> w;
    w.assign(tmp_w.size(),0);
    for (unsigned int t=0;t<tmp_w.size();t++)
    {
        w[t] = tmp_w[t].real();
    }

    // Construction de mu
    //std::vector<std::complex<float> > tmp_mu = MBT_readVector("../Files/mu4.txt");
    //std::vector<std::complex<float> > tmp_mu = MBT_readVector("../Files/trainingMu_BrainAmp.txt");
    //std::vector<std::complex<float> > tmp_mu = MBT_readVector("../Files/trainingMu_Data480trainingQC.txt");
    //std::vector<std::complex<float> > tmp_mu = MBT_readVector("../Files/trainingMu_Data420trainingQC.txt");
    //std::vector<std::complex<float> > tmp_mu = MBT_readVector("../Files/trainingMu_Data420UnguidedtrainingQC.txt");
    std::vector<std::complex<float> > tmp_mu = MBT_readVector("../Files/trainingMu_Data570UnguidedtrainingQCNEW4.txt");
    //std::vector<std::complex<float> > tmp_mu = MBT_readVector("../Files/trainingMu_Data900trainingQC.txt");
    std::vector<float> mu;
    mu.assign(tmp_mu.size(),0);
    for (unsigned int t=0;t<tmp_mu.size();t++)
    {
        mu[t] = tmp_mu[t].real();
    }

    // Construction de sigma
    //std::vector<std::complex<float> > tmp_sigma = MBT_readVector("../Files/sigma4.txt");
    //std::vector<std::complex<float> > tmp_sigma = MBT_readVector("../Files/trainingSigma_BrainAmp.txt");
    //std::vector<std::complex<float> > tmp_sigma = MBT_readVector("../Files/trainingSigma_Data480trainingQC.txt");
    //std::vector<std::complex<float> > tmp_sigma = MBT_readVector("../Files/trainingSigma_Data420trainingQC.txt");
    //std::vector<std::complex<float> > tmp_sigma = MBT_readVector("../Files/trainingSigma_Data420UnguidedtrainingQC.txt");
    std::vector<std::complex<float> > tmp_sigma = MBT_readVector("../Files/trainingSigma_Data570UnguidedtrainingQCNEW4.txt");
    //std::vector<std::complex<float> > tmp_sigma = MBT_readVector("../Files/trainingSigma_Data900trainingQC.txt");
    std::vector<float> sigma;
    sigma.assign(tmp_sigma.size(),0);
    for (unsigned int t=0;t<tmp_sigma.size();t++)
    {
        sigma[t] = tmp_sigma[t].real();
    }

    // Construction de kppv
    unsigned int kppv = 19;

    // Construction de costClass
    MBT_Matrix<float> costClass(3,3);
    for (int t=0;t<costClass.size().first;t++)
    {
        for (int t1=0;t1<costClass.size().second;t1++)
        {
            if (t == t1)
            {
                costClass(t,t1) = 0;
            }
            else
            {
                costClass(t,t1) = 1;
            }
        }
    }

    // Construction de potTrainingFeatures
    std::vector< std::vector<float> > potTrainingFeatures;

    // Construction de dataClean
    std::vector< std::vector<float> > dataClean;

    // Construction de spectrumClean
//    std::vector<std::complex<float> > tmp_spectrumClean = MBT_readVector("../Files/spectrumClean.txt");
    std::vector<std::complex<float> > tmp_spectrumClean = MBT_readVector("../Files/spectrumClean_Data570UnguidedtrainingQCNEW4.txt");
    std::vector<float> spectrumClean;
    spectrumClean.assign(tmp_spectrumClean.size(),0);
    for (unsigned int t=0;t<tmp_spectrumClean.size();t++)
    {
        spectrumClean[t] = tmp_spectrumClean[t].real();
    }

    // Construction de cleanItakuraDistance
//    std::vector<std::complex<float> > tmp_cleanItakuraDistance = MBT_readVector("../Files/cleanItakuraDistance.txt");
    std::vector<std::complex<float> > tmp_cleanItakuraDistance = MBT_readVector("../Files/cleanItakuraDistance_Data570UnguidedtrainingQCNEW4.txt");
    std::vector<float> cleanItakuraDistance;
    cleanItakuraDistance.assign(tmp_cleanItakuraDistance.size(),0);
    for (unsigned int t=0;t<tmp_cleanItakuraDistance.size();t++)
    {
        cleanItakuraDistance[t] = tmp_cleanItakuraDistance[t].real();
    }


    // Construction de accuracy
    float accuracy = (float)0.85;





    // ###############################
    // # TRAINING DETECTION BAD DATA #
    // ###############################
    // Construction de trainingFeaturesBad
    MBT_Matrix<float> trainingFeaturesBad = MBT_readMatrix("../Files/trainingFeatures_Data300BadQCNEW4.txt");

    // Construction de trainingClassesBad
    std::vector<std::complex<float> > tmp_trainingClassesBad = MBT_readVector("../Files/trainingClasses _Data300BadQCNEW4.txt");
    std::vector<float> trainingClassesBad;
    trainingClassesBad.assign(tmp_trainingClassesBad.size(),0);
    for (unsigned int t=0;t<tmp_trainingClassesBad.size();t++)
    {
        trainingClassesBad[t] = tmp_trainingClassesBad[t].real();
    }

    // Construction de wBad
    std::vector<std::complex<float> > tmp_wBad = MBT_readVector("../Files/trainingW_Data300BadQCNEW4.txt");
    std::vector<float> wBad;
    wBad.assign(tmp_wBad.size(),0);
    for (unsigned int t=0;t<tmp_wBad.size();t++)
    {
        wBad[t] = tmp_wBad[t].real();
    }

    // Construction de muBad
    std::vector<std::complex<float> > tmp_muBad = MBT_readVector("../Files/trainingMu_Data300BadQCNEW4.txt");
    std::vector<float> muBad;
    muBad.assign(tmp_muBad.size(),0);
    for (unsigned int t=0;t<tmp_muBad.size();t++)
    {
        muBad[t] = tmp_muBad[t].real();
    }

    // Construction de sigmaBad
    std::vector<std::complex<float> > tmp_sigmaBad = MBT_readVector("../Files/trainingSigma_Data300BadQCNEW4.txt");
    std::vector<float> sigmaBad;
    sigmaBad.assign(tmp_sigmaBad.size(),0);
    for (unsigned int t=0;t<tmp_sigmaBad.size();t++)
    {
        sigmaBad[t] = tmp_sigmaBad[t].real();
    }

    // Construction de costClassBad
    MBT_Matrix<float> costClassBad(2,2);
    for (int t=0;t<costClassBad.size().first;t++)
    {
        for (int t1=0;t1<costClassBad.size().second;t1++)
        {
            if (t == t1)
            {
                costClassBad(t,t1) = 0;
            }
            else
            {
                costClassBad(t,t1) = 1;
            }
        }
    }






    // Construction de inputData
    //MBT_Matrix<float> inputData = MBT_readMatrix("inputData.txt");
    //MBT_Matrix<float> inputData = MBT_readMatrix("../Files/inputData2.txt");
    //MBT_Matrix<float> inputData = MBT_readMatrix("../Files/trainingInputData_BrainAmpVolt.txt");
    //MBT_Matrix<float> inputData = MBT_readMatrix("../Files/testBad_MM_NOEL_KEV2_P3.txt");
    //MBT_Matrix<float> inputData = MBT_readMatrix("inputData3.txt");
    //MBT_Matrix<float> inputData = MBT_readMatrix("../Files/inputData422MMHeadsetOnTable.txt");
    //MBT_Matrix<float> inputData = MBT_readMatrix("../Files/inputData150TestingQCbis.txt");
    //MBT_Matrix<float> inputData = MBT_readMatrix("../Files/inputDataTestChangeQC19092017.txt");
//    MBT_Matrix<float> inputData = MBT_readMatrix("../Files/inputData_1200RSEC_BrainAmpTesting.txt");
    //MBT_Matrix<float> inputData = MBT_readMatrix("../Files/inputData300TestingQC.txt");
//    MBT_Matrix<float> inputData = MBT_readMatrix("../Data/testEEG.txt"); // for the call from Matlab
//    MBT_Matrix<float> inputData = MBT_readMatrix("../Files/inputData570UnguidedTestingQC.txt");
    MBT_Matrix<float> inputData = MBT_readMatrix("../Files/test.txt");



    clock_t msecs;
    msecs = clock();
    MBT_MainQC mainQC(sampRate,trainingFeatures,trainingClasses,w,mu,sigma,kppv,costClass,potTrainingFeatures,dataClean,spectrumClean,cleanItakuraDistance,accuracy,trainingFeaturesBad,trainingClassesBad,wBad,muBad,sigmaBad,costClassBad);
    mainQC.MBT_ComputeQuality(inputData);
    std::cout << "Execution time of the quality checker = "<< ((float((clock()-msecs))) / CLOCKS_PER_SEC) << std::endl;

    std::vector<float> tmp_quality_Cplusplus = mainQC.MBT_get_m_quality();
    std::vector<std::complex<float> > quality_Cplusplus;
    for (unsigned int ki=0;ki<tmp_quality_Cplusplus.size();ki++)
    {
        quality_Cplusplus.push_back(std::complex<float>(tmp_quality_Cplusplus[ki], 0));
    }
    //MBT_writeVector(quality_Cplusplus, "quality_Cplusplus.txt");
    //MBT_writeVector(quality_Cplusplus, "../Results/quality2_Cplusplus.txt");
    //MBT_writeVector(quality_Cplusplus, "../Results/quality_MM_NOEL_KEV2_P3.txt");
    //MBT_writeVector(quality_Cplusplus, "quality3_Cplusplus.txt");
    //MBT_writeVector(quality_Cplusplus, "../Results/quality_Data422MMHeadsetOnTable.txt");
    //MBT_writeVector(quality_Cplusplus, "../Results/quality_Data150TestingQCbis.txt");
    //MBT_writeVector(quality_Cplusplus, "../Results/quality_AfterChangeQC19092017.txt");
//    MBT_writeVector(quality_Cplusplus, "../Results/quality_Data_1200RSEC_BrainAmpTesting.txt");
    MBT_writeVector(quality_Cplusplus, "../Results/qualityTesting.txt");// for the call from Matlab

    MBT_Matrix<float> outputData_Cplusplus = mainQC.MBT_get_m_inputData();
    //MBT_writeMatrix (outputData_Cplusplus, "outputData_Cplusplus.txt");
    //MBT_writeMatrix (outputData_Cplusplus, "../Results/outputData2_Cplusplus.txt");
    //MBT_writeMatrix (outputData_Cplusplus, "../Results/outputData_MM_NOEL_KEV2_P3.txt");
    //MBT_writeMatrix (outputData_Cplusplus, "outputData3_Cplusplus.txt");
    //MBT_writeMatrix (outputData_Cplusplus, "../Results/outputData_Data422MMHeadsetOnTable.txt");
    //MBT_writeMatrix (outputData_Cplusplus, "../Results/outputData_Data150TestingQCbis.txt");
    //MBT_writeMatrix (outputData_Cplusplus, "../Results/outputData_AfterChangeQC19092017.txt");
    /*MBT_writeMatrix (outputData_Cplusplus, "../Results/outputData_Data630GuidedTestingQC.txt");

    std::vector<float> tmp_probaClass_Cplusplus = mainQC.MBT_get_m_probaClass();
    std::vector<std::complex<float> > probaClass_Cplusplus;
    for (unsigned int ki=0;ki<tmp_probaClass_Cplusplus.size();ki++)
    {
        probaClass_Cplusplus.push_back(std::complex<float>(tmp_probaClass_Cplusplus[ki], 0));
    }*/
    //MBT_writeVector(probaClass_Cplusplus, "probaClass_Cplusplus.txt");
    //MBT_writeVector(probaClass_Cplusplus, "../Results/probaClass2_Cplusplus.txt");
    //MBT_writeVector(probaClass_Cplusplus, "../Results/probaClass_MM_NOEL_KEV2_P3.txt");
    //MBT_writeVector(probaClass_Cplusplus, "probaClass3_Cplusplus.txt");
    //MBT_writeVector(probaClass_Cplusplus, "../Results/probaClass_Data422MMHeadsetOnTable.txt");
    //MBT_writeVector(probaClass_Cplusplus, "../Results/probaClass_Data150TestingQCbis.txt");
    //MBT_writeVector(probaClass_Cplusplus, "../Results/probaClass_AfterChangeQC19092017.txt");
    //MBT_writeVector(probaClass_Cplusplus, "../Results/probaClass_Data630GuidedTestingQC.txt");


    MBT_Matrix<float> Copy_testFeatures = mainQC.MBT_get_m_testFeatures();
    MBT_writeMatrix (Copy_testFeatures, "../Results/Copy_testFeatures.txt");

    MBT_Matrix<float> filteredData = mainQC.MBT_compute_data_to_display(inputData);
    MBT_writeMatrix (filteredData, "../Results/filteredData.txt");


/*
    MBT_Matrix<float> inputData5 = MBT_readMatrix("../Files/inputData5.txt");
    mainQC.MBT_ComputeQuality(inputData5);

    MBT_Matrix<float> inputData6 = MBT_readMatrix("../Files/inputData6.txt");
    mainQC.MBT_ComputeQuality(inputData6);

    MBT_Matrix<float> inputData7 = MBT_readMatrix("../Files/inputData7.txt");
    mainQC.MBT_ComputeQuality(inputData7);

    mainQC.MBT_ComputeQuality(inputData5);*/
/*
    // -------------------------------------------------------------------------------------------------------------------------------
    ////////////////// testBad_MM_NOEL_KEV2_P4 ///////////////////////
    MBT_Matrix<float> inputData2 = MBT_readMatrix("../Files/testBad_MM_NOEL_KEV2_P4.txt");
    mainQC.MBT_ComputeQuality(inputData2);

    std::vector<float> tmp_quality_Cplusplus2 = mainQC.MBT_get_m_quality();
    std::vector<std::complex<float> > quality_Cplusplus2;
    for (unsigned int ki=0;ki<tmp_quality_Cplusplus2.size();ki++)
    {
        quality_Cplusplus2.push_back(std::complex<float>(tmp_quality_Cplusplus2[ki], 0));
    }
    MBT_writeVector(quality_Cplusplus2, "../Results/quality_MM_NOEL_KEV2_P4.txt");


    MBT_Matrix<float> outputData_Cplusplus2 = mainQC.MBT_get_m_inputData();
    MBT_writeMatrix (outputData_Cplusplus2, "../Results/outputData_MM_NOEL_KEV2_P4.txt");

    std::vector<float> tmp_probaClass_Cplusplus2 = mainQC.MBT_get_m_probaClass();
    std::vector<std::complex<float> > probaClass_Cplusplus2;
    for (unsigned int ki=0;ki<tmp_probaClass_Cplusplus2.size();ki++)
    {
        probaClass_Cplusplus2.push_back(std::complex<float>(tmp_probaClass_Cplusplus2[ki], 0));
    }
    MBT_writeVector(probaClass_Cplusplus2, "../Results/probaClass_MM_NOEL_KEV2_P4.txt");
    // -------------------------------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------------------------------
    ////////////////// testBad_MM_NOEL_KIN2_P3 ///////////////////////
    MBT_Matrix<float> inputData3 = MBT_readMatrix("../Files/testBad_MM_NOEL_KIN2_P3.txt");
    mainQC.MBT_ComputeQuality(inputData3);

    std::vector<float> tmp_quality_Cplusplus3 = mainQC.MBT_get_m_quality();
    std::vector<std::complex<float> > quality_Cplusplus3;
    for (unsigned int ki=0;ki<tmp_quality_Cplusplus3.size();ki++)
    {
        quality_Cplusplus3.push_back(std::complex<float>(tmp_quality_Cplusplus3[ki], 0));
    }
    MBT_writeVector(quality_Cplusplus3, "../Results/quality_MM_NOEL_KIN2_P3.txt");


    MBT_Matrix<float> outputData_Cplusplus3 = mainQC.MBT_get_m_inputData();
    MBT_writeMatrix (outputData_Cplusplus3, "../Results/outputData_MM_NOEL_KIN2_P3.txt");

    std::vector<float> tmp_probaClass_Cplusplus3 = mainQC.MBT_get_m_probaClass();
    std::vector<std::complex<float> > probaClass_Cplusplus3;
    for (unsigned int ki=0;ki<tmp_probaClass_Cplusplus3.size();ki++)
    {
        probaClass_Cplusplus3.push_back(std::complex<float>(tmp_probaClass_Cplusplus3[ki], 0));
    }
    MBT_writeVector(probaClass_Cplusplus3, "../Results/probaClass_MM_NOEL_KIN2_P3.txt");
    // -------------------------------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------------------------------
    ////////////////// testBad_MM_NOEL_KIN2_P4 ///////////////////////
    MBT_Matrix<float> inputData4 = MBT_readMatrix("../Files/testBad_MM_NOEL_KIN2_P4.txt");
    mainQC.MBT_ComputeQuality(inputData4);

    std::vector<float> tmp_quality_Cplusplus4 = mainQC.MBT_get_m_quality();
    std::vector<std::complex<float> > quality_Cplusplus4;
    for (unsigned int ki=0;ki<tmp_quality_Cplusplus4.size();ki++)
    {
        quality_Cplusplus4.push_back(std::complex<float>(tmp_quality_Cplusplus4[ki], 0));
    }
    MBT_writeVector(quality_Cplusplus4, "../Results/quality_MM_NOEL_KIN2_P4.txt");


    MBT_Matrix<float> outputData_Cplusplus4 = mainQC.MBT_get_m_inputData();
    MBT_writeMatrix (outputData_Cplusplus4, "../Results/outputData_MM_NOEL_KIN2_P4.txt");

    std::vector<float> tmp_probaClass_Cplusplus4 = mainQC.MBT_get_m_probaClass();
    std::vector<std::complex<float> > probaClass_Cplusplus4;
    for (unsigned int ki=0;ki<tmp_probaClass_Cplusplus4.size();ki++)
    {
        probaClass_Cplusplus4.push_back(std::complex<float>(tmp_probaClass_Cplusplus4[ki], 0));
    }
    MBT_writeVector(probaClass_Cplusplus4, "../Results/probaClass_MM_NOEL_KIN2_P4.txt");
    // -------------------------------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------------------------------
    ////////////////// testBad_MM_NOEL_MAR4_P3 ///////////////////////
    MBT_Matrix<float> inputData5 = MBT_readMatrix("../Files/testBad_MM_NOEL_MAR4_P3.txt");
    mainQC.MBT_ComputeQuality(inputData5);

    std::vector<float> tmp_quality_Cplusplus5 = mainQC.MBT_get_m_quality();
    std::vector<std::complex<float> > quality_Cplusplus5;
    for (unsigned int ki=0;ki<tmp_quality_Cplusplus5.size();ki++)
    {
        quality_Cplusplus5.push_back(std::complex<float>(tmp_quality_Cplusplus5[ki], 0));
    }
    MBT_writeVector(quality_Cplusplus5, "../Results/quality_MM_NOEL_MAR4_P3.txt");


    MBT_Matrix<float> outputData_Cplusplus5 = mainQC.MBT_get_m_inputData();
    MBT_writeMatrix (outputData_Cplusplus5, "../Results/outputData_MM_NOEL_MAR4_P3.txt");

    std::vector<float> tmp_probaClass_Cplusplus5 = mainQC.MBT_get_m_probaClass();
    std::vector<std::complex<float> > probaClass_Cplusplus5;
    for (unsigned int ki=0;ki<tmp_probaClass_Cplusplus5.size();ki++)
    {
        probaClass_Cplusplus5.push_back(std::complex<float>(tmp_probaClass_Cplusplus5[ki], 0));
    }
    MBT_writeVector(probaClass_Cplusplus5, "../Results/probaClass_MM_NOEL_MAR4_P3.txt");
    // -------------------------------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------------------------------
    ////////////////// testBad_MM_NOEL_MAR4_P4 ///////////////////////
    MBT_Matrix<float> inputData6 = MBT_readMatrix("../Files/testBad_MM_NOEL_MAR4_P4.txt");
    mainQC.MBT_ComputeQuality(inputData6);

    std::vector<float> tmp_quality_Cplusplus6 = mainQC.MBT_get_m_quality();
    std::vector<std::complex<float> > quality_Cplusplus6;
    for (unsigned int ki=0;ki<tmp_quality_Cplusplus6.size();ki++)
    {
        quality_Cplusplus6.push_back(std::complex<float>(tmp_quality_Cplusplus6[ki], 0));
    }
    MBT_writeVector(quality_Cplusplus6, "../Results/quality_MM_NOEL_MAR4_P4.txt");


    MBT_Matrix<float> outputData_Cplusplus6 = mainQC.MBT_get_m_inputData();
    MBT_writeMatrix (outputData_Cplusplus6, "../Results/outputData_MM_NOEL_MAR4_P4.txt");

    std::vector<float> tmp_probaClass_Cplusplus6 = mainQC.MBT_get_m_probaClass();
    std::vector<std::complex<float> > probaClass_Cplusplus6;
    for (unsigned int ki=0;ki<tmp_probaClass_Cplusplus6.size();ki++)
    {
        probaClass_Cplusplus6.push_back(std::complex<float>(tmp_probaClass_Cplusplus6[ki], 0));
    }
    MBT_writeVector(probaClass_Cplusplus6, "../Results/probaClass_MM_NOEL_MAR4_P4.txt");
    // -------------------------------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------------------------------
    ////////////////// testBad_MM_NOEL_VIN_P3 ///////////////////////
    MBT_Matrix<float> inputData7 = MBT_readMatrix("../Files/testBad_MM_NOEL_VIN_P3.txt");
    mainQC.MBT_ComputeQuality(inputData7);

    std::vector<float> tmp_quality_Cplusplus7 = mainQC.MBT_get_m_quality();
    std::vector<std::complex<float> > quality_Cplusplus7;
    for (unsigned int ki=0;ki<tmp_quality_Cplusplus7.size();ki++)
    {
        quality_Cplusplus7.push_back(std::complex<float>(tmp_quality_Cplusplus7[ki], 0));
    }
    MBT_writeVector(quality_Cplusplus7, "../Results/quality_MM_NOEL_VIN_P3.txt");


    MBT_Matrix<float> outputData_Cplusplus7 = mainQC.MBT_get_m_inputData();
    MBT_writeMatrix (outputData_Cplusplus7, "../Results/outputData_MM_NOEL_VIN_P3.txt");

    std::vector<float> tmp_probaClass_Cplusplus7 = mainQC.MBT_get_m_probaClass();
    std::vector<std::complex<float> > probaClass_Cplusplus7;
    for (unsigned int ki=0;ki<tmp_probaClass_Cplusplus7.size();ki++)
    {
        probaClass_Cplusplus7.push_back(std::complex<float>(tmp_probaClass_Cplusplus7[ki], 0));
    }
    MBT_writeVector(probaClass_Cplusplus7, "../Results/probaClass_MM_NOEL_VIN_P3.txt");
    // -------------------------------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------------------------------
    ////////////////// testBad_MM_NOEL_VIN_P4 ///////////////////////
    MBT_Matrix<float> inputData8 = MBT_readMatrix("../Files/testBad_MM_NOEL_VIN_P4.txt");
    mainQC.MBT_ComputeQuality(inputData8);

    std::vector<float> tmp_quality_Cplusplus8 = mainQC.MBT_get_m_quality();
    std::vector<std::complex<float> > quality_Cplusplus8;
    for (unsigned int ki=0;ki<tmp_quality_Cplusplus8.size();ki++)
    {
        quality_Cplusplus8.push_back(std::complex<float>(tmp_quality_Cplusplus8[ki], 0));
    }
    MBT_writeVector(quality_Cplusplus8, "../Results/quality_MM_NOEL_VIN_P4.txt");


    MBT_Matrix<float> outputData_Cplusplus8 = mainQC.MBT_get_m_inputData();
    MBT_writeMatrix (outputData_Cplusplus8, "../Results/outputData_MM_NOEL_VIN_P4.txt");

    std::vector<float> tmp_probaClass_Cplusplus8 = mainQC.MBT_get_m_probaClass();
    std::vector<std::complex<float> > probaClass_Cplusplus8;
    for (unsigned int ki=0;ki<tmp_probaClass_Cplusplus8.size();ki++)
    {
        probaClass_Cplusplus8.push_back(std::complex<float>(tmp_probaClass_Cplusplus8[ki], 0));
    }
    MBT_writeVector(probaClass_Cplusplus8, "../Results/probaClass_MM_NOEL_VIN_P4.txt");
    // -------------------------------------------------------------------------------------------------------------------------------

    // -------------------------------------------------------------------------------------------------------------------------------
    ////////////////// testGood_MM ///////////////////////
    MBT_Matrix<float> inputData9 = MBT_readMatrix("../Files/testGood_MM.txt");
    mainQC.MBT_ComputeQuality(inputData9);
*/
    /*std::vector<float> tmp_quality_Cplusplus9 = mainQC.MBT_get_m_quality();
    std::vector<std::complex<float> > quality_Cplusplus9;
    for (unsigned int ki=0;ki<tmp_quality_Cplusplus9.size();ki++)
    {
        quality_Cplusplus9.push_back(std::complex<float>(tmp_quality_Cplusplus9[ki], 0));
    }
    MBT_writeVector(quality_Cplusplus9, "../Results/quality_testGood_MM.txt");


    MBT_Matrix<float> outputData_Cplusplus9 = mainQC.MBT_get_m_inputData();
    MBT_writeMatrix (outputData_Cplusplus9, "../Results/outputData_testGood_MM.txt");

    std::vector<float> tmp_probaClass_Cplusplus9 = mainQC.MBT_get_m_probaClass();
    std::vector<std::complex<float> > probaClass_Cplusplus9;
    for (unsigned int ki=0;ki<tmp_probaClass_Cplusplus9.size();ki++)
    {
        probaClass_Cplusplus9.push_back(std::complex<float>(tmp_probaClass_Cplusplus9[ki], 0));
    }
    MBT_writeVector(probaClass_Cplusplus9, "../Results/probaClass_testGood_MM.txt");/*
    // -------------------------------------------------------------------------------------------------------------------------------
/*

    float m_sampRate = sampRate;
    MBT_Matrix<float> m_trainingFeatures = trainingFeatures;
	std::vector<float> m_trainingClasses = trainingClasses;
	std::vector<float> m_w = w;
	std::vector<float> m_mu = mu;
	std::vector<float> m_sigma = sigma;
	unsigned int m_kppv = kppv;
	MBT_Matrix<float> m_costClass = costClass;
	std::vector< std::vector<float> > m_potTrainingFeatures = potTrainingFeatures;
	std::vector< std::vector<float> > m_dataClean = dataClean;
	std::vector<float> m_spectrumClean = spectrumClean;
	std::vector<float> m_cleanItakuraDistance = cleanItakuraDistance;
	float m_accuracy = accuracy;
	MBT_Matrix<float> m_inputData = inputData;

	MBT_Matrix<float> m_testFeatures = MBT_Matrix<float>(m_inputData.size().first, m_trainingFeatures.size().second);
	std::vector<float> m_probaClass;
	m_probaClass.assign(m_inputData.size().first, 0);
	std::vector<float> m_predictedClass;
	m_predictedClass.assign(m_inputData.size().first, 0);
	std::vector<float> m_quality;
	m_quality.assign(m_inputData.size().first, 0);


    // Construction de m_testFeatures
    for (int ch=0;ch<m_inputData.size().first;ch++)
    //for (int ch=0;ch<3;ch++)
    {
        int f = 0; // index of the feature in m_testFeatures(ch,.)
        std::vector <float> tmp_signal = m_inputData.row(ch);

        if (std::all_of(tmp_signal.begin(), tmp_signal.end(), [](float testNaN){return std::isnan(testNaN);}) )
        {
            for (int i = 0; i<m_testFeatures.size().second; i++)
            {
                m_testFeatures(ch,i) = nan(" ");
            }
            errno = EINVAL;
            perror("ERROR: MBT_MainQC HAS IN INPUT A SIGNAL WITH ONLY NaN VALUES");
        }
        else
        {
            // TODO:
            std::vector<float> signal = RemoveDC(tmp_signal); // Remove DC
            // notch

            // compute each features for each channel and add 1 to f

            // TIME DOMAIN
            // ===========
            // Median
            float mediane = median(signal);
            m_testFeatures(ch,f) = mediane;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Mean
            float moyenne = mean(signal);
            m_testFeatures(ch,f) = moyenne;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Variance
            float variance = var(signal);
            m_testFeatures(ch,f) = variance;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // VRMS
            std::vector<float> PowerOfTwoSignal;
            PowerOfTwoSignal.assign(signal.size(),0);
            for (int i = 0; i<signal.size(); i++)
            {
                PowerOfTwoSignal[i] = signal[i]*signal[i];
            }
            float vrms = sqrt(mean(PowerOfTwoSignal));
            m_testFeatures(ch,f) = vrms;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Vpp
            float vpp = 2 * sqrt(2) * vrms;
            m_testFeatures(ch,f) = vpp;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Skewness
            float S = skewness(signal);
            m_testFeatures(ch,f) = S;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Kurtosis
            float K = kurtosis(signal);
            m_testFeatures(ch,f) = K;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Integrated EEG
            float ieeg = 0;
            for (int i = 0; i <signal.size(); i++)
            {
                ieeg = ieeg + abs(signal[i]);
            }
            m_testFeatures(ch,f) = ieeg;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Mean absolute value
            float mav = ieeg/signal.size();
            m_testFeatures(ch,f) = mav;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Simple square integral
            float ssi = 0;
            for (int i = 0; i <signal.size(); i++)
            {
                ssi = ssi + pow(abs(signal[i]),2);
            }
            m_testFeatures(ch,f) = ssi;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            //v-order 2 and 3
            float v2 = 0;
            float v3 = 0;
            for (int i = 0; i <signal.size(); i++)
            {
                v2 = v2 + pow(signal[i],2);
                v3 = v3 + pow(abs(signal[i]),3);
            }
            v2 = sqrt(v2/signal.size());
            m_testFeatures(ch,f) = v2;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            float oneThird = float(1)/float(3);
            v3 = pow(v3/signal.size(),oneThird);
            m_testFeatures(ch,f) = v3;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Log detector
            float log_detector = 0;
            for (int i = 0; i <signal.size(); i++)
            {
                log_detector = log_detector + log10(abs(signal[i]));
            }
            log_detector = exp(log_detector/signal.size());
            m_testFeatures(ch,f) = log_detector;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Average amplitude change
            float aac = 0;
            for (int i = 0; i< signal.size()-1; i++)
            {
                aac = aac + abs(signal[i+1]-signal[i]);
            }
            aac = aac/signal.size();
            m_testFeatures(ch,f) = aac;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Difference absolute standard deviation value
            float dasdv = 0;
            for (int i = 0; i< signal.size()-1; i++)
            {
                dasdv = dasdv + pow((signal[i+1]-signal[i]),2);
            }
            dasdv = sqrt(dasdv/(signal.size()-1));
            m_testFeatures(ch,f) = dasdv;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Number of maxima and minima
            std::vector<float> time_point, diff_time_point, good_ind, time_point_tmp, X_tmp;
            time_point.assign(signal.size()+1,0);
            diff_time_point.assign(signal.size(),0);

            for (int i = 0; i<signal.size()+1; i++)
            {
                time_point[i] = float(i)/float(m_sampRate);
            }
            for (int i = 0; i<signal.size(); i++)
            {
                diff_time_point[i] = time_point[i+1] - time_point[i];
                if (diff_time_point[i] != 0)
                {
                    good_ind.push_back(i);
                    time_point_tmp.push_back(time_point[i]);
                    X_tmp.push_back(signal[i]);
                }
            }
            std::vector<float> deriv_X;
            deriv_X.assign(X_tmp.size()-1,0);
            int nb_max_min = 0;
            for (int i = 0; i < X_tmp.size()-1; i++)
            {
                deriv_X[i] = float(X_tmp[i+1] - X_tmp[i]) / float(time_point_tmp[i+1] - time_point_tmp[i]); // first derivative of the signal
                if (deriv_X[i] < 0.01)
                {
                    nb_max_min = nb_max_min + 1;
                }
            }
            m_testFeatures(ch,f) = nb_max_min;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // 2nd Hjorth parameter = mobility
            float mobility = standardDeviation(deriv_X)/standardDeviation(signal);
            m_testFeatures(ch,f) = mobility;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // 3rd Hjorth parameter = complexity
            std::vector<float> time_point_two, diff_time_point_two, good_ind_two, time_point_tmp_two, deriv_X_two_tmp;
            time_point_two.assign(signal.size(),0);
            diff_time_point_two.assign(signal.size(),0);

            for (int i = 0; i<signal.size(); i++)
            {
                time_point_two[i] = float(i)/float(m_sampRate);
            }
            for (int i = 0; i<signal.size()-1; i++)
            {
                diff_time_point_two[i] = time_point_two[i+1] - time_point_two[i];
                if (diff_time_point[i] != 0)
                {
                    good_ind_two.push_back(i);
                    time_point_tmp_two.push_back(time_point_two[i]);
                    deriv_X_two_tmp.push_back(deriv_X[i]);
                }
            }
            std::vector<float> deriv_X_two;
            deriv_X_two.assign(deriv_X_two_tmp.size()-1,0);
            for (int i = 0; i < deriv_X_two_tmp.size()-1; i++)
            {
                deriv_X_two[i] = float(deriv_X_two_tmp[i+1] - deriv_X_two_tmp[i]) / float(time_point_tmp_two[i+1] - time_point_tmp_two[i]); // second derivative of the signal
            }
            float complexity = (standardDeviation(deriv_X_two)/standardDeviation(deriv_X)) / mobility;
            m_testFeatures(ch,f) = complexity;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Zero-crossing rate
            float zrc = 0;
            float indPlusUn, ind;
            for (int i = 0; i< signal.size()-1; i++)
            {
                indPlusUn = 0;
                ind = 0;
                if (signal[i+1]>0)
                {
                    indPlusUn = 1;
                }
                if (signal[i]>0)
                {
                    ind = 1;
                }

                zrc = zrc + abs(indPlusUn - ind);
            }
            zrc = zrc/signal.size();

            m_testFeatures(ch,f) = zrc;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Zero-crossing rate of 1st derivative
            float zrc_deriv_one = 0;
            float deriv_indPlusUn, deriv_ind;
            for (int i = 0; i< deriv_X.size()-1; i++)
            {
                deriv_indPlusUn = 0;
                deriv_ind = 0;
                if (deriv_X[i+1]>0)
                {
                    deriv_indPlusUn = 1;
                }
                if (deriv_X[i]>0)
                {
                    deriv_ind = 1;
                }
                zrc_deriv_one = zrc_deriv_one + abs(deriv_indPlusUn - deriv_ind);
            }
            zrc_deriv_one = zrc_deriv_one/deriv_X.size();

            m_testFeatures(ch,f) = zrc_deriv_one;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Zero-crossing rate of 2nd derivative
            float zrc_deriv_two = 0;
            float deriv_two_indPlusUn, deriv_two_ind;
            for (int i = 0; i< deriv_X_two.size()-1; i++)
            {
                deriv_two_indPlusUn = 0;
                deriv_two_ind = 0;
                if (deriv_X_two[i+1]>0)
                {
                    deriv_two_indPlusUn = 1;
                }
                if (deriv_X_two[i]>0)
                {
                    deriv_two_ind = 1;
                }

                zrc_deriv_two = zrc_deriv_two + abs(deriv_two_indPlusUn - deriv_two_ind);
            }
            zrc_deriv_two = zrc_deriv_two/deriv_X_two.size();

            m_testFeatures(ch,f) = zrc_deriv_two;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Variance of 1st derivative
            float var_deriv_one = var(deriv_X);
            m_testFeatures(ch,f) = var_deriv_one;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Variance of 2nd derivative
            float var_deriv_two = var(deriv_X_two);
            m_testFeatures(ch,f) = var_deriv_two;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
*/
            // Autoregressive modelling error
            /*%     %simulate the system
            %     [y t]=step(g,10);
            %
            %     %add some random noise
            %     n=0.001*randn(size(t));
            %     y1=y+n
            %     x=ones(size(t)); %input step
            %     Rmat=[y1(1:end-1) x(1:end-1)];
            %     Yout=[y1(2:end)];
            %     P=Rmat\Yout;
            %     %simulated output
            %     Yout1=Rmat*P;
            %
            %     %Mean Square Error
            %     e=sqrt(mean((Yout-Yout1).^2));
            %     disp(e);
            for i = 1:9
                [m_ar e_ar(i)] = aryule(X,i);
                m_testFeatures(ch,f) = var_deriv_two;
                f = f+1;
                if (isnan(m_testFeatures(ch,f)))
                {
                    m_testFeatures(ch,f) = 0;
                }
            end;*/
/*

            // Non linear energy
            std::vector<float> nle;
            nle.assign(signal.size()-2,0);
            for (int i = 1; i<signal.size()-1; i++)
            {
                nle[i-1] = pow(signal[i],2) - signal[i-1]*signal[i+1];
            }
            float nle_mean = mean(nle);
            m_testFeatures(ch,f) = nle_mean;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            //Maximum, kurtosis, std and skewness of the data of each frequency bands
            std::vector<float> freqBounds;
            freqBounds.assign(2,0);
            freqBounds[0] = 0.5;
            freqBounds[1] = 4.0;
            std::vector<float> signalDelta = BandPassFilter(signal,freqBounds);
            std::cout<<"delta of channel "<<ch<<std::endl;
            freqBounds[0] = 4.0;
            freqBounds[1] = 8.0;
            std::vector<float> signalTheta = BandPassFilter(signal, freqBounds);
            std::cout<<"theta of channel "<<ch<<std::endl;
            freqBounds[0] = 8.0;
            freqBounds[1] = 13.0;
            std::vector<float> signalAlpha = BandPassFilter(signal, freqBounds);
            std::cout<<"alpha of channel "<<ch<<std::endl;
            freqBounds[0] = 13.0;
            freqBounds[1] = 28.0;
            std::vector<float> signalBeta = BandPassFilter(signal, freqBounds);
            std::cout<<"beta of channel "<<ch<<std::endl;
            freqBounds[0] = 28.0;
            freqBounds[1] = 110.0;
            std::vector<float> signalGamma = BandPassFilter(signal, freqBounds);
            std::cout<<"gamma of channel "<<ch<<std::endl;
            float ampDelta_sig = *std::max_element(signalDelta.begin(), signalDelta.end());
            float kurtDelta_sig = kurtosis(signalDelta);
            float stdDelta_sig = standardDeviation(signalDelta);
            float skewDelta_sig = skewness(signalDelta);
            m_testFeatures(ch,f) = ampDelta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = kurtDelta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = stdDelta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = skewDelta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            float ampTheta_sig = *std::max_element(signalTheta.begin(), signalTheta.end());
            float kurtTheta_sig = kurtosis(signalTheta);
            float stdTheta_sig = standardDeviation(signalTheta);
            float skewTheta_sig = skewness(signalTheta);
            m_testFeatures(ch,f) = ampTheta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = kurtTheta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = stdTheta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = skewTheta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            float ampAlpha_sig = *std::max_element(signalAlpha.begin(), signalAlpha.end());
            float kurtAlpha_sig = kurtosis(signalAlpha);
            float stdAlpha_sig = standardDeviation(signalAlpha);
            float skewAlpha_sig = skewness(signalAlpha);
            m_testFeatures(ch,f) = ampAlpha_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = kurtAlpha_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = stdAlpha_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = skewAlpha_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            float ampBeta_sig = *std::max_element(signalBeta.begin(), signalBeta.end());
            float kurtBeta_sig = kurtosis(signalBeta);
            float stdBeta_sig = standardDeviation(signalBeta);
            float skewBeta_sig = skewness(signalBeta);
            m_testFeatures(ch,f) = ampBeta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = kurtBeta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = stdBeta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = skewBeta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            float ampGamma_sig = *std::max_element(signalGamma.begin(), signalGamma.end());
            float kurtGamma_sig = kurtosis(signalGamma);
            float stdGamma_sig = standardDeviation(signalGamma);
            float skewGamma_sig = skewness(signalGamma);
            m_testFeatures(ch,f) = ampGamma_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = kurtGamma_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = stdGamma_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = skewGamma_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;



            // FREQUENY DOMAIN
            // ===============

            // Zero-padding
            // ------------
            int N = signal.size();
            int tmp_nfft = pow(2, ceil(log(N)/log(2)));
            int nfft = std::max(256,tmp_nfft);
            int nb_zero_added = nfft-N;
            if (nfft>N)
            {
                for (int i = 0; i < nfft - N; i++)
                {
                    signal.push_back(0);
                }
            }
            N = signal.size();*/
            // ------------

            // Total power
            /*std::vector<std::complex<float> > dataToTransform;
            dataToTransform.assign(signal.size(),0);
            for (int i = 0; i < signal.size(); i++)
            {
                dataToTransform[i] = signal[i];
            }
            MBT_Fourier::forwardBluesteinFFT(dataToTransform)*/
            /*if (ch==0)
            {
                std::vector<std::complex<float> > DebugSignal;
                for (int ki=0;ki<signal.size();ki++)
                {
                    DebugSignal.push_back(std::complex<float>(signal[ki], 0));
                }
                MBT_writeVector(DebugSignal, "../Results/DebugSignal.txt");
            }*/
/*
            std::cout<<" "<<std::endl;
            std::cout<<"start r2c"<<std::endl;
            FLOAT_FFTW_R2C_1D *obj = new FLOAT_FFTW_R2C_1D(signal.size(), signal);
            std::vector<complex<float> > dataToTransform(signal.size());
            dataToTransform = obj->execute();
            std::cout<<"end r2c"<<std::endl;
            std::cout<<" "<<std::endl;


            std::vector<float> tmp_EEG_power;
            tmp_EEG_power.assign(nfft,0);
            std::vector<float> tmp_freqVector;
            tmp_freqVector.assign(nfft,0);
            for (int i = 0; i < nfft; i++)
            {
                tmp_EEG_power[i] = abs(dataToTransform[i]); //Retain Magnitude (imaginary part = phase)
                tmp_freqVector[i] = (float(i)*float(m_sampRate))/float(nfft);
            }
            // Generate the one-sided spectrum
            std::vector<float> EEG_power;
            std::vector<float> freqVector;
            EEG_power.assign(nfft/2,0);
            freqVector.assign(nfft/2,0);
            for (int i=0; i<nfft/2; i++)
            {
                freqVector[i] = tmp_freqVector[i];
                EEG_power[i] = tmp_EEG_power[i];
            }
            float ttp = 0;
            for (int i = 0; i < EEG_power.size(); i++)
            {
                ttp = ttp + pow(EEG_power[i],2);
            }
            ttp = ttp/pow(EEG_power.size(),2);
            m_testFeatures(ch,f) = ttp;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            float AUC_EEG_power = trapz(freqVector,EEG_power);


            // Ratio delta
            std::vector<float> delta_power, freqVector_delta;
            for (int tmp = 0 ; tmp < freqVector.size(); tmp++)
            {
                if ( (freqVector[tmp] >= 0.5) && (freqVector[tmp] <= 4.0) )
                {
                    freqVector_delta.push_back(freqVector[tmp]);
                    delta_power.push_back(EEG_power[tmp]);
                }
            }
            float AUC_delta_power = trapz(freqVector_delta,delta_power);
            float ratio_delta = AUC_delta_power/AUC_EEG_power;
            m_testFeatures(ch,f) = ratio_delta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // delta power
            float delta_pow = 0;
            for (int i = 0; i < delta_power.size(); i++)
            {
                delta_pow = delta_pow + delta_power[i]*delta_power[i];
            }
            delta_pow = delta_pow/pow(delta_power.size(),2);
            m_testFeatures(ch,f) = delta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // log delta power
            float log_delta_pow = log10(delta_pow);
            m_testFeatures(ch,f) = log_delta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Normalized delta power
            float n_delta_pow = delta_pow/ttp; // division of energy
            m_testFeatures(ch,f) = n_delta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Ratio theta
            std::vector<float> theta_power, freqVector_theta;
            for (int tmp = 0 ; tmp < freqVector.size(); tmp++)
            {
                if ( (freqVector[tmp] >= 4.0) && (freqVector[tmp] <= 8.0) )
                {
                    freqVector_theta.push_back(freqVector[tmp]);
                    theta_power.push_back(EEG_power[tmp]);
                }
            }
            float AUC_theta_power = trapz(freqVector_theta,theta_power);
            float ratio_theta = AUC_theta_power/AUC_EEG_power;
            m_testFeatures(ch,f) = ratio_theta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // theta power
            float theta_pow = 0;
            for (int i = 0; i < theta_power.size(); i++)
            {
                theta_pow = theta_pow + theta_power[i]*theta_power[i];
            }
            theta_pow = theta_pow/pow(theta_power.size(),2);
            m_testFeatures(ch,f) = theta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // log theta power
            float log_theta_pow = log10(theta_pow);
            m_testFeatures(ch,f) = log_theta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Normalized theta power
            float n_theta_pow = theta_pow/ttp; // division of energy
            m_testFeatures(ch,f) = n_theta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Ratio alpha
            std::vector<float> alpha_power, freqVector_alpha;
            for (int tmp = 0 ; tmp < freqVector.size(); tmp++)
            {
                if ( (freqVector[tmp] >= 8.0) && (freqVector[tmp] <= 13.0) )
                {
                    freqVector_alpha.push_back(freqVector[tmp]);
                    alpha_power.push_back(EEG_power[tmp]);
                }
            }
            float AUC_alpha_power = trapz(freqVector_alpha,alpha_power);
            float ratio_alpha = AUC_alpha_power/AUC_EEG_power;
            m_testFeatures(ch,f) = ratio_alpha;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // alpha power
            float alpha_pow = 0;
            for (int i = 0; i < alpha_power.size(); i++)
            {
                alpha_pow = alpha_pow + alpha_power[i]*alpha_power[i];
            }
            alpha_pow = alpha_pow/pow(alpha_power.size(),2);
            m_testFeatures(ch,f) = alpha_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // log alpha power
            float log_alpha_pow = log10(alpha_pow);
            m_testFeatures(ch,f) = log_alpha_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Normalized alpha power
            float n_alpha_pow = alpha_pow/ttp; // division of energy
            m_testFeatures(ch,f) = n_alpha_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Ratio beta
            std::vector<float> beta_power, freqVector_beta;
            for (int tmp = 0 ; tmp < freqVector.size(); tmp++)
            {
                if ( (freqVector[tmp] >= 13.0) && (freqVector[tmp] <= 28.0) )
                {
                    freqVector_beta.push_back(freqVector[tmp]);
                    beta_power.push_back(EEG_power[tmp]);
                }
            }
            float AUC_beta_power = trapz(freqVector_beta,beta_power);
            float ratio_beta = AUC_beta_power/AUC_EEG_power;
            m_testFeatures(ch,f) = ratio_beta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // beta power
            float beta_pow = 0;
            for (int i = 0; i < beta_power.size(); i++)
            {
                beta_pow = beta_pow + beta_power[i]*beta_power[i];
            }
            beta_pow = beta_pow/pow(beta_power.size(),2);
            m_testFeatures(ch,f) = beta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // log beta power
            float log_beta_pow = log10(beta_pow);
            m_testFeatures(ch,f) = log_beta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Normalized beta power
            float n_beta_pow = beta_pow/ttp; // division of energy
            m_testFeatures(ch,f) = n_beta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Ratio gamma
            std::vector<float> gamma_power, freqVector_gamma;
            for (int tmp = 0 ; tmp < freqVector.size(); tmp++)
            {
                if ( freqVector[tmp] >= 28.0)
                {
                    freqVector_gamma.push_back(freqVector[tmp]);
                    gamma_power.push_back(EEG_power[tmp]);
                }
            }
            float AUC_gamma_power = trapz(freqVector_gamma,gamma_power);
            float ratio_gamma = AUC_gamma_power/AUC_EEG_power;
            m_testFeatures(ch,f) = ratio_gamma;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // gamma power
            float gamma_pow = 0;
            for (int i = 0; i < gamma_power.size(); i++)
            {
                gamma_pow = gamma_pow + gamma_power[i]*gamma_power[i];
            }
            gamma_pow = gamma_pow/pow(gamma_power.size(),2);
            m_testFeatures(ch,f) = gamma_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // log gamma power
            float log_gamma_pow = log10(gamma_pow);
            m_testFeatures(ch,f) = log_gamma_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Normalized gamma power
            std::vector<float> n_gamma_power_tmp;
            float n_gamma_pow = gamma_pow/ttp; // division of energy
            m_testFeatures(ch,f) = n_gamma_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Spectral Edge Frequency 80%
            float sum_EEG_power = std::accumulate(EEG_power.begin(), EEG_power.begin() + EEG_power.size(), 0.0);
            std::vector<float> EEG_power_norm;
            EEG_power_norm.assign(EEG_power.size(),0);
            for (int i=0; i<EEG_power.size(); i++)
            {
                EEG_power_norm[i] = EEG_power[i]/sum_EEG_power;
            }
            std::vector<float> EEG_power_cum;
            EEG_power_cum.assign(EEG_power_norm.size(),0);
            std::partial_sum (EEG_power_norm.begin(), EEG_power_norm.end(), EEG_power_cum.begin());
            std::vector<float> EEG_power_cum_80;
            EEG_power_cum_80.assign(EEG_power_cum.size(),0);
            for (int i = 0; i < EEG_power_cum_80.size(); i++)
            {
                EEG_power_cum_80[i] = abs(EEG_power_cum[i] - 0.2);
            }
            int idx_sef_80 = min_element(EEG_power_cum_80.begin(), EEG_power_cum_80.end()) - EEG_power_cum_80.begin();
            float sef80 = freqVector[idx_sef_80];
            m_testFeatures(ch,f) = sef80;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Spectral Edge Frequency 90%
            std::vector<float> EEG_power_cum_90;
            EEG_power_cum_90.assign(EEG_power_cum.size(),0);
            for (int i = 0; i < EEG_power_cum_90.size(); i++)
            {
                EEG_power_cum_90[i] = abs(EEG_power_cum[i] - 0.1);
            }
            int idx_sef_90 = min_element(EEG_power_cum_90.begin(), EEG_power_cum_90.end()) - EEG_power_cum_90.begin();
            float sef90 = freqVector[idx_sef_90];
            m_testFeatures(ch,f) = sef90;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Spectral Edge Frequency 95%
            std::vector<float> EEG_power_cum_95;
            EEG_power_cum_95.assign(EEG_power_cum.size(),0);
            for (int i = 0; i < EEG_power_cum_95.size(); i++)
            {
                EEG_power_cum_95[i] = abs(EEG_power_cum[i] - 0.05);
            }
            int idx_sef_95 = min_element(EEG_power_cum_95.begin(), EEG_power_cum_95.end()) - EEG_power_cum_95.begin();
            float sef95 = freqVector[idx_sef_95];
            m_testFeatures(ch,f) = sef95;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
*/

        /*%Calculation of the Wavelet Coefficients of every Band (EEG Signal Description with Spectral-Envelope-Based Speech Recognition Features for Detection of Neonatal Seizures by Andriy Temko et al.)
        % ---------------------------------------------------- (http://fr.mathworks.com/matlabcentral/answers/31413-computing-delta-power-of-eeg-signal)
        [C,L] = wavedec(X,5,'db8'); %################### NEED WAVELET TOOLBOX
        cD1 = detcoef(C,L,1); %NOISY
        cD2 = detcoef(C,L,2); %Gamma (31,25-62,5Hz)
        cD2 = var(cD2);
        cD3 = detcoef(C,L,3); %Beta (15.625-31.25Hz)
        cD3 = var(cD3);
        cD4 = detcoef(C,L,4); %Alpha (7.8-15.625Hz)
        cD4 = var(cD4);
        cD5 = detcoef(C,L,5); %Theta (3.9-7.8Hz)
        cD5 = var(cD5);
        cA5 = appcoef(C,L,'db8',5); %Delta (0-3.9Hz)
        cA5 =  var(cA5);
        WAV_ENERGY_GAMMA = [WAV_ENERGY_GAMMA;cD2];
        WAV_ENERGY_BETA = [WAV_ENERGY_BETA;cD3];
        WAV_ENERGY_ALPHA = [WAV_ENERGY_ALPHA;cD4];
        WAV_ENERGY_THETA = [WAV_ENERGY_THETA;cD5];
        WAV_ENERGY_DELTA = [WAV_ENERGY_DELTA;cA5];

        % Cepstral coefficients (EEG Signal Description with Spectral-Envelope-Based Speech Recognition Features for Detection of Neonatal Seizures by Andriy Temko et al.)
        % --------------------- (https://en.wikipedia.org/wiki/Mel-frequency_cepstrum)
        % Calcul de la transform�e de Fourier de la trame � analyser
        y = abs(fft(X));      %Retain Magnitude (imaginary part = phase)
        EEG_power = y(1:N/2);     %Discard Half of Points
        f = Fs*(0:N/2-1)/N;

        % Create a mel frequency filterbank
        nb_banks = 30; % the number of filter banks to construct
        Filter = melfilter(nb_banks,f); % ####################### required Signal Processing Toolbox

        % Pond�ration du spectre d'amplitude (ou de puissance selon les cas) par un banc de filtres triangulaires espac�s selon l'�chelle de Mel
        F_EEG_power = Filter*double(EEG_power');

        % Calcul de la transform�e en cosinus discr�te du log-mel-spectre % The expansion coefficients in vector X measure how much energy is stored in each of the components.
        %ij = find(F_EEG_power);  % Make mask to eliminate 0's since log(0) = -inf.
        %list_mel_log_powers = log10(F_EEG_power(ij));
        list_mel_log_powers = log10(F_EEG_power);
        cc = dct(list_mel_log_powers); % ####################### required Signal Processing Toolbox
        cc = cc(2:end); % remove the zeroth CC
        [cc_sort_abs,ind] = sort(abs(cc),'descend');
        cc = cc(ind(1:10)); % take the tenth first coefficient (highest energy)

        CC1 = [CC1;cc(1)];
        CC2 = [CC2;cc(2)];
        CC3 = [CC3;cc(3)];
        CC4 = [CC4;cc(4)];
        CC5 = [CC5;cc(5)];
        CC6 = [CC6;cc(6)];
        CC7 = [CC7;cc(7)];
        CC8 = [CC8;cc(8)];
        CC9 = [CC9;cc(9)];
        CC10 = [CC10;cc(10)];*/
/*
            // Frequency-filtered band energies
            float ff_delta = (log_theta_pow - 0);
            m_testFeatures(ch,f) = ff_delta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            float ff_theta = (log_alpha_pow - log_delta_pow);
            m_testFeatures(ch,f) = ff_theta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            float ff_alpha = (log_beta_pow - log_theta_pow);
            m_testFeatures(ch,f) = ff_alpha;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            float ff_beta = (log_gamma_pow - log_alpha_pow);
            m_testFeatures(ch,f) = ff_beta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            float ff_gamma = (0 - log_beta_pow);
            m_testFeatures(ch,f) = ff_gamma;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Relative spectral difference
            float rsd_delta = (theta_pow - 0)/(theta_pow + delta_pow + 0);
            m_testFeatures(ch,f) = rsd_delta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            float rsd_theta = (alpha_pow - delta_pow)/(alpha_pow + theta_pow + delta_pow);
            m_testFeatures(ch,f) = rsd_theta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            float rsd_alpha = (beta_pow - theta_pow)/(beta_pow + alpha_pow + theta_pow);
            m_testFeatures(ch,f) = rsd_alpha;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            float rsd_beta = (gamma_pow - alpha_pow)/(gamma_pow + beta_pow + alpha_pow);
            m_testFeatures(ch,f) = rsd_beta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            float rsd_gamma = (0 - beta_pow)/(0 + gamma_pow + beta_pow);
            m_testFeatures(ch,f) = rsd_gamma;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // SN ratio
            std::vector<float> EEG_noise;
            for (int tmp = 0 ; tmp < freqVector.size(); tmp++)
            {
                if (freqVector[tmp] > 30.0)
                {
                    EEG_noise.push_back(EEG_power[tmp]);
                }
            }
            float tot_EEG_noise = 0;
            for (int i = 0; i < EEG_noise.size(); i++)
            {
                tot_EEG_noise = tot_EEG_noise + EEG_noise[i]*EEG_noise[i];
            }
            tot_EEG_noise = tot_EEG_noise/pow(EEG_noise.size(),2);
            float SN_ratio = ttp/tot_EEG_noise;
            m_testFeatures(ch,f) = SN_ratio;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Power spectrum moments
            float m0 = 0;
            for (int i=0; i<EEG_power.size(); i++)
            {
                m0 = m0 + EEG_power[i]*EEG_power[i];
            }
            m_testFeatures(ch,f) = m0;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            float m1 = 0;
            for (int i=0; i<EEG_power.size(); i++)
            {
                m1 = m1 + EEG_power[i]*EEG_power[i]*freqVector[i];
            }
            m_testFeatures(ch,f) = m1;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            float m2 = 0;
            for (int i=0; i<EEG_power.size(); i++)
            {
                m2 = m2 + EEG_power[i]*EEG_power[i]*pow(freqVector[i],2);
            }
            m_testFeatures(ch,f) = m2;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Power spectrum center frequency
            float center_freq = m1/m0;
            m_testFeatures(ch,f) = center_freq;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Spectral RMS
            float sp_rms = sqrt(m0/(N-nb_zero_added));
            m_testFeatures(ch,f) = sp_rms;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Index of spectral deformation
            float omega_ratio = (sqrt(m2/m0))/center_freq;
            m_testFeatures(ch,f) = omega_ratio;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Modified Median Frequency
            std::vector<float> ecart;
            ecart.assign(floor(m_sampRate/2),0);
            for (int i = 0; i<floor(m_sampRate/2); i++)
            {
                int f_dist = 0;
                for (int j = 0; j<freqVector.size(); j++)
                {
                    if (freqVector[j]<=i && j>f_dist)
                    {
                        f_dist = j;
                    }
                }
                float ASD1 = std::accumulate(EEG_power.begin(), EEG_power.begin() + f_dist, 0.0);
                float ASD2 = std::accumulate(EEG_power.begin() + f_dist, EEG_power.end(), 0.0);
                ecart[i] = abs(ASD1 - ASD2);
            }
            float low_ecart = *std::min_element(ecart.begin(), ecart.end());
            std::vector<float> mod_median_freq_tmp;
            for (int i = 0; i<ecart.size(); i++)
            {
                if (ecart[i] == low_ecart)
                {
                    mod_median_freq_tmp.push_back(i);
                }
            }
            float mod_median_freq = mod_median_freq_tmp[0]; // we take the smallest index
            m_testFeatures(ch,f) = mod_median_freq;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Modified Mean Frequency
            float mod_mean_freq = 0.0;
            for (int i = 0; i<EEG_power.size(); i++)
            {
                mod_mean_freq = mod_mean_freq + freqVector[i]*EEG_power[i];
            }
            mod_mean_freq = mod_mean_freq/sum_EEG_power;
            m_testFeatures(ch,f) = mod_mean_freq;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;



            // ENTROPY DOMAIN
            // ==============
            std::vector<float> PSD;
            PSD.assign(EEG_power.size(),0);
            for (int i = 0; i<EEG_power.size(); i++)
            {
                PSD[i] = pow(EEG_power[i],2);
            }
            float sum_PSD = std::accumulate(PSD.begin(), PSD.begin() + PSD.size(), 0.0);
            std::vector<float> PDF;
            PDF.assign(EEG_power.size(),0);
            for (int i = 0; i<EEG_power.size(); i++)
            {
                PDF[i] = PSD[i]/sum_PSD; // estimate of the probability density function

            }
*/
        /*//Shannon Entropy
        ePDF = hist(PDF(:), length(f));% Compute N-bin histogram
        ePDF = ePDF / sum(ePDF(:));% Compute probabilities
        i = find(ePDF); % Make mask to eliminate 0's since log(0) = -inf.
        sh_h = -sum(ePDF(i) .* log(ePDF(i))); % Compute entropy*/
/*
            // Spectral Entropy
            float s_h = 0;
            for (int i = 0; i<PDF.size(); i++)
            {
                if (PDF[i] != NULL) // eliminate 0's since log(0) = -inf.
                {
                    s_h = s_h + PDF[i]*log(PDF[i]);
                }
            }
            s_h = -s_h/log10(freqVector.size()); // normalized spectral entropy
            m_testFeatures(ch,f) = s_h;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }

*/
        /*// Singular value decomposition entropy
        m_svd = N/2;
        nb_svd = N-m_svd+1;
        for i = 1:m_svd
            X_svd(i,:) = X(i:i-1+nb_svd);
        end;

        [U,S,V] = svd(X_svd);
        n_svd = S/(sum(S(:)));
        i = find(n_svd); % Make mask to eliminate 0's since log(0) = -inf.
        svde = -sum(n_svd(i).*log(n_svd(i)));
    */

        //}
    //}
    /*for (int ch=0;ch<2;ch++)
    {
        std::vector<float> freqBounds;
        freqBounds.assign(2,0);
        freqBounds[0] = 0.5;
        freqBounds[1] = 4.0;
        MBT_Matrix<float> DebugSignal = MBT_readMatrix("../Files/DebugSignal.txt");
        std::vector<float> signal = DebugSignal.row(0);
        std::vector<float> signalDelta = BandPassFilter(signal,freqBounds);
        FLOAT_FFTW_R2C_1D *obj = new FLOAT_FFTW_R2C_1D(signal.size(), signal);
        std::vector<complex<float> > dataToTransform = obj->execute();
    }*/
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////// TEST PWELCH ////////////////////////////////////////////////////

    //std::vector<std::complex<float> > sinusoide = MBT_readVector("C:\Users\Fanny\Documents\Melomind.Algorithms/QualityChecker\Files\dataEEG.txt");
    //MBT_Matrix<float> sinusoide = MBT_readMatrix("sinusoide.txt");

    /*int power = 0;
    int nfft = 0;
    while (nfft<sinusoide.size().second)
    {
        nfft = pow(2,power);
        power = power + 1;
    }

    MBT_Matrix<float> signal = MBT_Matrix<float>(1, nfft);
    for (int i = 0; i < nfft; i++)
    {
        if (i<sinusoide.size().second)
        {
            signal(0, i) = sinusoide(0,i);
        }
        else
        {
            signal(0, i) = 0;
        }


    }*/

    /*MBT_Matrix<float> signal = MBT_Matrix<float>(1, sinusoide.size());
    for (int i = 0; i < sinusoide.size(); i++)
        {
            signal(0, i) = sinusoide[i].real();
        }


    MBT_PWelchComputer pWelch = MBT_PWelchComputer(signal, 250, "HAMMING");
    //MBT_PWelchComputer pWelch = MBT_PWelchComputer(sinusoide, 250, "HAMMING");


    std::vector<float> f = pWelch.get_PSD(0);
    std::vector<float> pf2 = pWelch.get_PSD(1);


    std::vector<std::complex<float> > testf;
    for (int ki=0;ki<f.size();ki++)
    {
        testf.push_back(std::complex<float>(f[ki], 0));
    }
    MBT_writeVector(testf, "C:\Users\Fanny\Documents\Melomind.Algorithms\QualityChecker\Results\testf.txt");

    std::vector<std::complex<float> > testPwelchNfft;
    for (int ki=0;ki<pf2.size();ki++)
    {
        testPwelchNfft.push_back(std::complex<float>(pf2[ki], 0));
    }
    MBT_writeVector(testPwelchNfft, "C:\Users\Fanny\Documents\Melomind.Algorithms\QualityChecker\Results\testPwelchNfft.txt");*/
    ///////////////////////////////////////////////////////////////////////////////////

    return 0;
}





