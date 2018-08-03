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

    /////////////////////////////////// Complete pipeline//////////////////////////////////////
    float sampRate = 250;

	// Just to let you know that the EEG data of the training are in the inputData570UnguidedTestingQC.txt file (570x250).

    // Construction de trainingFeatures (570x88)
    MBT_Matrix<float> trainingFeatures = MBT_readMatrix("../Files/trainingFeatures_Data570UnguidedtrainingQCNEW.txt");

    // Construction de trainingClasses (1x570)
    std::vector<std::complex<float> > tmp_trainingClasses = MBT_readVector("../Files/trainingClasses _Data570UnguidedtrainingQCNEW.txt");
    std::vector<float> trainingClasses;
    trainingClasses.assign(tmp_trainingClasses.size(),0);
    for (unsigned int t=0;t<tmp_trainingClasses.size();t++)
    {
        trainingClasses[t] = tmp_trainingClasses[t].real();
    }

    // Construction de w (1x570)
    std::vector<std::complex<float> > tmp_w = MBT_readVector("../Files/trainingW_Data570UnguidedtrainingQCNEW.txt");
    std::vector<float> w;
    w.assign(tmp_w.size(),0);
    for (unsigned int t=0;t<tmp_w.size();t++)
    {
        w[t] = tmp_w[t].real();
    }

    // Construction de mu (1x88)
    std::vector<std::complex<float> > tmp_mu = MBT_readVector("../Files/trainingMu_Data570UnguidedtrainingQCNEW.txt");
    std::vector<float> mu;
    mu.assign(tmp_mu.size(),0);
    for (unsigned int t=0;t<tmp_mu.size();t++)
    {
        mu[t] = tmp_mu[t].real();
    }

    // Construction de sigma (1x88)
    std::vector<std::complex<float> > tmp_sigma = MBT_readVector("../Files/trainingSigma_Data570UnguidedtrainingQCNEW.txt");
    std::vector<float> sigma;
    sigma.assign(tmp_sigma.size(),0);
    for (unsigned int t=0;t<tmp_sigma.size();t++)
    {
        sigma[t] = tmp_sigma[t].real();
    }

    // Construction de kppv (1x1)
    unsigned int kppv = 19;

    // Construction de costClass (3x3)
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

    // Construction de potTrainingFeatures (inchang�)
    std::vector< std::vector<float> > potTrainingFeatures;

    // Construction de dataClean
    std::vector< std::vector<float> > dataClean;

    // Construction de spectrumClean (inchang�)
    std::vector<std::complex<float> > tmp_spectrumClean = MBT_readVector("../Files/spectrumClean.txt");
    std::vector<float> spectrumClean;
    spectrumClean.assign(tmp_spectrumClean.size(),0);
    for (unsigned int t=0;t<tmp_spectrumClean.size();t++)
    {
        spectrumClean[t] = tmp_spectrumClean[t].real();
    }

    // Construction de cleanItakuraDistance (inchang�)
    std::vector<std::complex<float> > tmp_cleanItakuraDistance = MBT_readVector("../Files/cleanItakuraDistance.txt");
    std::vector<float> cleanItakuraDistance;
    cleanItakuraDistance.assign(tmp_cleanItakuraDistance.size(),0);
    for (unsigned int t=0;t<tmp_cleanItakuraDistance.size();t++)
    {
        cleanItakuraDistance[t] = tmp_cleanItakuraDistance[t].real();
    }


    // Construction de accuracy (inchang�)
    float accuracy = (float)0.85;

    // Construction de inputData to test data (630x250)
    MBT_Matrix<float> inputData = MBT_readMatrix("../Files/inputData630GuidedTestingQC.txt");

	// Call of QualityChecker
    MBT_MainQC mainQC(sampRate,trainingFeatures,trainingClasses,w,mu,sigma,kppv,costClass,potTrainingFeatures,dataClean,spectrumClean,cleanItakuraDistance,accuracy); // call at the begining of the recording
    mainQC.MBT_ComputeQuality(inputData); // it should be called each second



	// Just to see what are the results of QualityChecker
	// --------------------------------------------------
    std::vector<float> tmp_quality_Cplusplus = mainQC.MBT_get_m_quality();
    std::vector<std::complex<float> > quality_Cplusplus;
    for (unsigned int ki=0;ki<tmp_quality_Cplusplus.size();ki++)
    {
        quality_Cplusplus.push_back(std::complex<float>(tmp_quality_Cplusplus[ki], 0));
    }
    MBT_writeVector(quality_Cplusplus, "../Results/quality_Data630GuidedTestingQC.txt");

    MBT_Matrix<float> outputData_Cplusplus = mainQC.MBT_get_m_inputData();
    MBT_writeMatrix (outputData_Cplusplus, "../Results/outputData_Data630GuidedTestingQC.txt");

    std::vector<float> tmp_probaClass_Cplusplus = mainQC.MBT_get_m_probaClass();
    std::vector<std::complex<float> > probaClass_Cplusplus;
    for (unsigned int ki=0;ki<tmp_probaClass_Cplusplus.size();ki++)
    {
        probaClass_Cplusplus.push_back(std::complex<float>(tmp_probaClass_Cplusplus[ki], 0));
    }
    MBT_writeVector(probaClass_Cplusplus, "../Results/probaClass_Data630GuidedTestingQC.txt");


    return 0;
}





