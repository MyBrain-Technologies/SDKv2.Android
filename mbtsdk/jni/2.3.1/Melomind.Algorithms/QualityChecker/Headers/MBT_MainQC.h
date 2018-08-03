//
// MBT_MainQC.h
//
// Created by Fanny GROSSELIN on 2016/08/02
// Copyright (c) 2016 myBrain Technologies. All rights reserved.
//
// Update : Fanny Grosselin 2017/02/16 --> Add the header files which are useful to compute some features in MBT_MainQC::MBT_featuresQualityChecker
// 			Fanny Grosselin 2017/02/20 --> Add an include to manage errors.
//          Fanny Grosselin 2017/03/14 --> Change the inclusion of the bandpass: use MBT_BandPass_fftw3 instead of using MBT_BandPass.
//          Fanny Grosselin 2017/03/16 --> Use MBT_Fourier_fftw3 instead of using MBT_Fourier
//          Fanny Grosselin 2017/03/28 --> Add a member function MBT_interpBTpacketLost to try to interpolate the possible NaN values inside inputData thanks to rawInterpData.
//          Fanny Grosselin 2017/03/28 --> Create a member function MBT_MainQC::MBT_ComputeQuality() to call the others functions.
//          Fanny Grosselin 2017/09/05 --> Change the paths
//          Fanny Grosselin 2017/09/19 --> In MBT_MainQC::MBT_qualityChecker, change the output signal after QualityChecker: keep NaN signal if the quality is 0, or get the original signal (no the interpolated ones).
//          Fanny Grosselin 2018/02/22 --> Add a classifier after the first detection of qualities, in order to distinguish two different types of bad data (bad EEG vs no recorded EEG).
#ifndef MBT_MAINQC_H
#define MBT_MAINQC_H

#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_ReadInputOrWriteOutput.h" // use of the class MBT_ReadInputOrWriteOutput
#include "../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h" // use of the class MBT_Matrix
#include "../../SignalProcessing.Cpp/PreProcessing/Headers/MBT_PreProcessing.h" // use of the class MBT_PreProcess
#include "../../SignalProcessing.Cpp/Transformations/Headers/MBT_PWelchComputer.h" // use of the class MBT_PWelchComputer
#include "../../SignalProcessing.Cpp/Algebra/Headers/MBT_Operations.h" // use of the class MBT_Statistics: mean and standardDeviation
#include "../../SignalProcessing.Cpp/Algebra/Headers/MBT_FindClosest.h"
#include "../../SignalProcessing.Cpp/PreProcessing/Headers/MBT_BandPass_fftw3.h"
//#include "../../SignalProcessing.Cpp/Transformations/Headers/MBT_Fourier.h"
#include "../../SignalProcessing.Cpp/Transformations/Headers/MBT_Fourier_fftw3.h"
#include <iostream> // for std::cout and std::endl
#include <stdio.h>
#include <vector> // for std::vector
#include <limits> // to create inf
#include <algorithm> // for std::unique and std::distance and std::min_element
#include <numeric> // for std::accumulate
#include <time.h>
#include <iterator>  // std::begin, std::end
#include <string>
#include <math.h>
#include <errno.h>



class MBT_MainQC
{
    public:
        // Constructor
        // Method which computes the quality
        MBT_MainQC(const float sampRate,MBT_Matrix<float> trainingFeatures, std::vector<float> trainingClasses, std::vector<float> w, std::vector<float> mu, std::vector<float> sigma, unsigned int const& kppv, MBT_Matrix<float> const& costClass, std::vector< std::vector<float> > potTrainingFeatures, std::vector< std::vector<float> > dataClean, std::vector<float> spectrumClean, std::vector<float> cleanItakuraDistance, float accuracy, MBT_Matrix<float> trainingFeaturesBad, std::vector<float> trainingClassesBad, std::vector<float> wBad, std::vector<float> muBad, std::vector<float> sigmaBad, MBT_Matrix<float> const& costClassBad);

        // Destructor
        ~MBT_MainQC();

        // A member function to call the others functions.
        void MBT_ComputeQuality(MBT_Matrix<float> const& inputData, bool bandpassProcess = false, float firstBound = 2.0, float secondBound = 30.0);

        // Declaration of the prototypes of the methods
        MBT_Matrix<float> MBT_get_m_trainingFeatures(); // Method which returns m_trainingFeatures
        std::vector<float> MBT_get_m_trainingClasses(); // Method which returns m_trainingClasses
        std::vector<float> MBT_get_m_w(); // Method which returns m_w
        std::vector<float> MBT_get_m_mu(); // Method which returns m_mu
        std::vector<float> MBT_get_m_sigma(); // Method which returns m_sigma
        int MBT_get_m_kppv(); // Method which returns m_kppv
        MBT_Matrix<float> MBT_get_m_costClass(); // Method which returns m_costClass
        MBT_Matrix<float> MBT_get_m_trainingFeaturesBad(); // Method which returns m_trainingFeaturesBad
        std::vector<float> MBT_get_m_trainingClassesBad(); // Method which returns m_trainingClassesBad
        std::vector<float> MBT_get_m_wBad(); // Method which returns m_wBad
        std::vector<float> MBT_get_m_muBad(); // Method which returns m_muBad
        std::vector<float> MBT_get_m_sigmaBad(); // Method which returns m_sigmaBad
        MBT_Matrix<float> MBT_get_m_costClassBad(); // Method which returns m_costClassBad
        std::vector< std::vector<float> > MBT_get_m_potTrainingFeatures(); // Method which returns m_potTrainingFeatures
        std::vector< std::vector<float> > MBT_get_m_dataClean(); // Method which returns m_dataClean
        std::vector<float> MBT_get_m_spectrumClean(); // Method which returns m_spectrumClean
        std::vector<float> MBT_get_m_cleanItakuraDistance(); // Method which returns m_cleanItakuraDistance
        float MBT_get_m_accuracy(); // Method which returns m_accuracy
        MBT_Matrix<float> MBT_get_m_inputData(); // Method which returns m_inputData

        MBT_Matrix<float> MBT_get_m_testFeatures(); // Method which returns m_testFeatures
        std::vector<float> MBT_get_m_probaClass(); // Method which returns m_probaClass
        std::vector<float> MBT_get_m_predictedClass(); // Method which returns m_predictedClass
        std::vector<float> MBT_get_m_quality(); // Method which returns m_quality
        MBT_Matrix<float> MBT_compute_data_to_display(MBT_Matrix<float> const& inputData, float firstBound = 2.0, float secondBound = 30.0); // Method to compute a processed data for display purpose

    private:

        // Declaration of the prototypes of the methods
        void MBT_interpBTpacketLost(); // method to try to interpolate the possible NaN values inside inputData thanks to rawInterpData
        void MBT_featuresQualityChecker(bool bandpassProcess = false, float firstBound= 2.0, float secondBound = 30.0); // Method which calculates features of each EEG observation
        void MBT_knn(); // Method which is a k-nearest neighbors classifier
        void MBT_addTraining(); // Method which prepares variables to add eventual data in the cloud storage
        void MBT_qualityChecker(MBT_Matrix<float> inputData); // Method which gives the final quality of each observation and prepare each observation
                              //  (in function of its quality) to the relaxation index module
        float MBT_itakuraDistance(std::vector<float> data); // Method which calculates the Itakura distance between the averaged
                                                              // spectrum of clean data (quality = 1) and the spectrum of each observation
                                                              // of EEG data (between 0 and 40Hz).

        // Declaration of the attributes
        MBT_Matrix<float> m_rawInterpData; // history of at most 2s of data possibly interpolated
        bool m_correctInput;
        float m_sampRate; // the sampling rate
        MBT_Matrix<float> m_trainingFeatures; // array which contains the values of each features for the training dataset
        std::vector<float> m_trainingClasses; // vector which contains the classes of each observation (of the training dataset)
        std::vector<float> m_w; // vector which contains prior probabilities for each observation in the training dataset.
                                 // In general, the prior probability for each observation = 1
        std::vector<float> m_mu; // vector which contains mean of each feature (of the training dataset)
        std::vector<float> m_sigma; // vector which contains standard deviation of each feature (of the training dataset)
        unsigned int m_kppv; // number of nearest neighbors
        MBT_Matrix<float> m_costClass; // square matrix with y rows and k columns (number of y = number of k)
                                        // It is the cost of classifying an observation as y when its true class is k.
        std::vector< std::vector<float> > m_potTrainingFeatures; // array which contains the probability of classification (1st colomn), the
                                                  // predicted class (2nd column) and the (non-normalized) features (3 to m+3th columns)
                                                  // of all the observations with a probability of being in such a class is upper
                                                  // or equal to SeuilProba (which is defined as 1 that is to say a probability of 100%)
        std::vector< std::vector<float> > m_dataClean; // array which contains the segments of EEG values of the observations with a probability of being
                                        // in the class 1 is upper or equal SeuilProba (which is defined as 1 that is to say a probability of 100%)
        std::vector<float> m_spectrumClean; // row vector which is the averaged spectrum of
                                                             // clean data (quality = 1) between 0 and 40Hz.
        std::vector<float> m_cleanItakuraDistance; // which contains Itakura distances between each "clean" observations (quality = 1)
                                                    // and the averaged spectrum of "clean" observations (quality = 1).
        float m_accuracy; // Performance of the classification with the stored Training set (after 10-fold cross validation)
        MBT_Matrix<float> m_trainingFeaturesBad; // array which contains the values of each features for the training dataset of bad data
        std::vector<float> m_trainingClassesBad; // vector which contains the classes of each observation (of the training dataset of bad data)
        std::vector<float> m_wBad; // vector which contains prior probabilities for each observation in the training dataset of bad data.
                                 // In general, the prior probability for each observation = 1
        std::vector<float> m_muBad; // vector which contains mean of each feature (of the training dataset of bad data)
        std::vector<float> m_sigmaBad; // vector which contains standard deviation of each feature (of the training dataset of bad data)
        MBT_Matrix<float> m_costClassBad; // square matrix with y rows and k columns (number of y = number of k) for the training set of bad data.
                                        // It is the cost of classifying an observation as y when its true class is k.
        MBT_Matrix<float> m_inputData; // array which contains the EEG values of each observation.

        MBT_Matrix<float> m_testFeatures; // array which contains the values of each feature for the test dataset
        std::vector<float> m_probaClass; // vector which contains for each tested observations the probability of the PredictedClass.
        std::vector<float> m_predictedClass; // vector which contains the predicted classes of the (test) data to classify
        std::vector<float> m_quality; // vector which contains the quality (0 for bad, 0.25 for
                                       // muscular artifacts, 0.5 for other artifacts, 1 for clean) of each observation

};

#endif // MBT_MAINQC_H
