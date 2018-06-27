//
// MBT_MainQC.cpp
//
// Created by Fanny GROSSELIN on 2016/08/02
// Copyright (c) 2016 myBrain Technologies. All rights reserved.
//
// Update : Fanny Grosselin 2017/02/16 --> Add the computation of features in MBT_MainQC::MBT_featuresQualityChecker
// 			Fanny Grosselin 2017/02/20 --> Add some code to manage errors of input.
//          Fanny Grosselin 2017/02/20 --> Add codes to replace NaN values of features by 0 and manage signal with NaN values (we don't compute features and quality = 0).
//          Fanny Grosselin 2017/03/14 --> Remove the +1 after the ".end()" when using max_element function to compute some features.
//          Fanny Grosselin 2017/03/16 --> Use of MBT_Fourier_fftw3 instead of using MBT_Fourier.
//          Fanny Grosselin 2017/03/20 --> Initialize all of the vectors with a predefined size and use an assignment instead of using push_back for the use of MBT_Fourier_fftw3 functions
//          Fanny Grosselin 2017/03/24 --> Change float by double to use some functions of SignalProcessing.cpp and DOUBLE_FFTW_R2C_1D to compute some features.
//          Fanny Grosselin 2017/03/27 --> Change "\" into "/".
//          Fanny Grosselin 2017/03/27 --> Fix all the warnings.
//          Fanny Grosselin 2017/03/28 --> Try to interpolate the possible NaN values inside inputData thanks to rawInterpData.
//          Fanny Grosselin 2017/03/28 --> Create a member function MBT_MainQC::MBT_ComputeQuality() to call the others functions.
//          Fanny Grosselin 2017/05/19 --> Correct a typos in MBT_MainQC::MBT_featuresQualityChecker concerning the computation of the second derivative.
//          Fanny Grosselin 2017/05/19 --> Fix all the possible exceptions in MBT_MainQC::MBT_featuresQualityChecker
//          Fanny Grosselin 2017/05/22 --> Change the code to check the Vpp in MBT_MainQC::MBT_qualityChecker().
//          Fanny Grosselin 2017/05/22 --> Fix the memory leaks because of the creation of an object without deleting it.
//          Fanny Grosselin 2017/05/29 --> Fix the possible exceptions in MBT_MainQC in particular in MBT_featuresQualityChecker and MBT_ItakuraDistance
//          Fanny Grosselin 2017/07/25 --> Fix the problem in MBT_MainQC::MBT_interpBTpacketLost that crashes for the first packet. It was because we wanted to access at a memory case not allocated (the case just after length packet).
//          Fanny Grosselin 2017/07/25 --> In MBT_MainQC::MBT_qualityChecker, transform ampvar in Volts instead of microVolts.
//          Fanny Grosselin 2017/07/26 --> In MBT_MainQC::MBT_interpBTpacketLost, fix the case where the number of channels in m_inputData is higher than m_rawInterpData.
//          Fanny Grosselin 2017/09/05 --> Change the paths
//          Fanny Grosselin 2017/09/15 --> In MBT_MainQC::MBT_featuresQualityChecker(), multiply each data by 10^6 in order to convert data in microVolts.
//          Fanny Grosselin 2017/07/25 --> In MBT_MainQC::MBT_qualityChecker, keep ampvar in microVolts.
//          Fanny Grosselin 2017/09/19 --> In MBT_MainQC::MBT_qualityChecker, change the output signal after QualityChecker: keep NaN signal if the quality is 0, or get the original signal (no the interpolated ones).
//          Fanny Grosselin 2017/10/03 --> In MBT_MainQC::MBT_qualityChecker, change the existing thresholds and add one threshold to determined misclassified bad data.
//          Fanny Grosselin 2018/01/10 --> In MBT_MainQC::MBT_featuresQualityChecker(), correct the way we compute nb_max_min.
//          Fanny Grosselin 2018/02/21 --> In MBT_MainQC::MBT_qualityChecker, provide the detection of muscle artifacts.
//          Fanny Grosselin 2018/02/22 --> Add a classifier after the first detection of qualities, in order to distinguish two different types of bad data (bad EEG vs no recorded EEG).
//          Fanny Grosselin 2018/02/27 --> In MBT_MainQC::MBT_featuresQualityChecker(), UNcorrect the way we compute nb_max_min because it classifies better.
//          Aeiocha Li 2018/05/09 --> In MBT_Matrix<float> MBT_MainQC::MBT_compute_data_to_display(MBT_Matrix<float> const &inputData, float firstBound, float secondBound), we compute a processed data for displaying purpose, and add the possibility to add a bandpass to imitate the former firmware filters in MBT_MainQC::MBT_featuresQualityChecker() and MBT_MainQC::MBT_ComputeQuality()

#include "../Headers/MBT_MainQC.h"
#include "../../SignalProcessing.Cpp/Transformations/Headers/MBT_Fourier_fftw3.h"

// Constructor
MBT_MainQC::MBT_MainQC(const float sampRate,MBT_Matrix<float> trainingFeatures, std::vector<float> trainingClasses, std::vector<float> w, std::vector<float> mu, std::vector<float> sigma, unsigned int const& kppv, MBT_Matrix<float> const& costClass, std::vector< std::vector<float> > potTrainingFeatures, std::vector< std::vector<float> > dataClean, std::vector<float> spectrumClean, std::vector<float> cleanItakuraDistance, float accuracy, MBT_Matrix<float> trainingFeaturesBad, std::vector<float> trainingClassesBad, std::vector<float> wBad, std::vector<float> muBad, std::vector<float> sigmaBad, MBT_Matrix<float> const& costClassBad)
{
	if ((trainingFeatures.size().first > 0) & (trainingFeatures.size().second > 0) & (trainingClasses.size() > 0) & (w.size() > 0) & (mu.size() > 0) & (sigma.size() > 0) & (kppv != 0) & (costClass.size().first > 0) & (costClass.size().second > 0) & (spectrumClean.size() > 0) & (cleanItakuraDistance.size() > 0) & (trainingFeaturesBad.size().first > 0) & (trainingFeaturesBad.size().second > 0) & (trainingClassesBad.size() > 0) & (wBad.size() > 0) & (muBad.size() > 0) & (sigmaBad.size() > 0) & (costClassBad.size().first > 0) & (costClassBad.size().second > 0))
    {
		// Initialize the attributes
		m_sampRate = sampRate;
		m_trainingFeatures = trainingFeatures;
		m_trainingClasses = trainingClasses;
		m_w = w;
		m_mu = mu;
		m_sigma = sigma;
		m_kppv = kppv;
		m_costClass = costClass;
		m_potTrainingFeatures = potTrainingFeatures;
		m_dataClean = dataClean;
		m_spectrumClean = spectrumClean;
		m_cleanItakuraDistance = cleanItakuraDistance;
		m_accuracy = accuracy;
		m_trainingFeaturesBad = trainingFeaturesBad;
		m_trainingClassesBad = trainingClassesBad;
		m_wBad = wBad;
		m_muBad = muBad;
		m_sigmaBad = sigmaBad;
		m_costClassBad = costClassBad;
		m_correctInput = true;
	}
	else
	{
		// Return NaN values to be handled in case of problem into MBT_MainQC
		m_correctInput = false;
        errno = EINVAL;
        perror("ERROR: MBT_MainQC CANNOT PROCESS WITH PROBLEM(S) IN INPUTS");
	}

}



void MBT_MainQC::MBT_ComputeQuality(MBT_Matrix<float> const& inputData, bool bandpassProcess, float firstBound, float secondBound)
{
    if ((inputData.size().first > 0) & (inputData.size().second > 0) & (m_correctInput==true))
    {
        m_inputData = inputData;
		m_testFeatures = MBT_Matrix<float>(m_inputData.size().first, m_trainingFeatures.size().second);
		std::vector<float> m_probaClass;
		m_probaClass.assign(m_inputData.size().first, 0);
		std::vector<float> m_predictedClass;
		m_predictedClass.assign(m_inputData.size().first, 0);
		std::vector<float> m_quality;
		m_quality.assign(m_inputData.size().first, 0);

		MBT_interpBTpacketLost();// Try to interpolate the possible NaN values inside inputData thanks to rawInterpData
        MBT_featuresQualityChecker(bandpassProcess, firstBound, secondBound); // change m_testFeatures
        MBT_knn(); // change m_probaClass and m_predictedClass
        //MBT_addTraining(); // change m_potTrainingFeatures and m_dataClean
        MBT_qualityChecker(inputData); // change m_predictedClass and m_quality

    }
    else
    {
		// Return NaN values to be handled in case of problem into MBT_MainQC
		MBT_Matrix<float>  m_inputData;
		m_inputData(0,0) = nan(" ");
		std::vector<float> m_quality;
		m_quality.assign(1, nan(" "));
        errno = EINVAL;
        perror("ERROR: MBT_MainQC CANNOT PROCESS WITHOUT SIGNALS IN INPUT OR WITH PROBLEM(S) IN INPUTS OF MBT_MAINQC OBJECT");
	}
}


void MBT_MainQC::MBT_interpBTpacketLost()
{
    // gérer erreur rawInterPdata.size().second <= 2*packetLength

    // Fanny Grosselin 2017/07/26
    // --------------------------
    if (m_inputData.size().first > m_rawInterpData.size().first)
    {
        MBT_Matrix<float> new_m_rawInterpData;
        m_rawInterpData = new_m_rawInterpData;
    }
    // ----------------------------

    // Update m_rawInterpData with the new data m_inputData
    // --------------------------------------------------
    if (m_rawInterpData.size().second==0) // If this is the first second of recording
    {
        MBT_Matrix<float> tmp_rawInterpData(m_inputData.size().first,m_inputData.size().second);
        for (int ch = 0; ch<m_inputData.size().first; ch++)
        {
            for (int i = 0; i<m_inputData.size().second; i++)
            {
                tmp_rawInterpData(ch,i) = m_inputData(ch,i);
            }
        }
        m_rawInterpData = tmp_rawInterpData;
    }
    else if (m_rawInterpData.size().second==m_sampRate) // If we have 1s of history data
    {
        MBT_Matrix<float> tmp_rawInterpData(m_inputData.size().first,2*m_inputData.size().second);
        for (int ch = 0; ch<m_inputData.size().first; ch++)
        {
            for (int i = 0; i<m_rawInterpData.size().second; i++)
            {
                tmp_rawInterpData(ch,i) = m_rawInterpData(ch,i);
            }
            for (int i = 0; i<m_inputData.size().second; i++)
            {
                tmp_rawInterpData(ch,m_inputData.size().second + i) = m_inputData(ch,i);
            }
        }
        m_rawInterpData = tmp_rawInterpData;
    }
    else if (m_rawInterpData.size().second==2*m_sampRate)// If we have 2s of history data
    {
        MBT_Matrix<float> tmp_rawInterpData(m_inputData.size().first,2*m_inputData.size().second);
        for (int ch = 0; ch<m_inputData.size().first; ch++)
        {
            for (int i = 0; i<m_inputData.size().second; i++)
            {
                tmp_rawInterpData(ch,i) = m_rawInterpData(ch,m_inputData.size().second + i);
            }
            for (int i = 0; i<m_inputData.size().second; i++)
            {
                tmp_rawInterpData(ch,m_inputData.size().second + i) = m_inputData(ch,i);
            }
        }
        m_rawInterpData = tmp_rawInterpData;
    }
    else
    {
        errno = EINVAL;
        perror("ERROR: MBT_MainQC::MBT_interpBTpacketLost CANNOT PROCESS WITH AN HISTORY OF DATA UPPER THAN 2 SECONDS");
    }
    // --------------------------------------------------

    // Interpolate possible NaN values of m_inputData with m_rawInterpData
    // -----------------------------------------------------------------
    for (int ch=0;ch<m_inputData.size().first;ch++)
    {
        std::vector<float> tmp_rawInterpData_row= m_rawInterpData.row(ch); // get m_rawInterpData for a specific channel
        std::vector<double> rawInterpData_row(tmp_rawInterpData_row.begin(),tmp_rawInterpData_row.end()); // transform in double

        std::vector<double> x, y, xInterp;
        std::vector<double> tmp_InterpolatedData;
        for (unsigned int tmp = 0 ; tmp < rawInterpData_row.size(); tmp++)
        {
            if (std::isnan(rawInterpData_row[tmp]) )
            {
                xInterp.push_back(tmp);
            }
            else
            {
                x.push_back(tmp);
                y.push_back(rawInterpData_row[tmp]);
            }
        }
        std::vector<double> InterpolatedData = MBT_linearInterp(x, y, xInterp);
        for (unsigned int d = 0; d<InterpolatedData.size(); d++)
        {
            rawInterpData_row[(unsigned int)xInterp[d]] = InterpolatedData[d];
        }

        // Update of m_rawInterpData after interpolation
        for (unsigned int e = 0; e<rawInterpData_row.size(); e++)
        {
            m_rawInterpData(ch,e) = (float)rawInterpData_row[e];
        }
        // Test if there are still NaN values
        unsigned int counterNaN = 0;

        // Fanny Grosselin 2017/07/25
        // -----------------------------
        /*int t = 0;
        while ((t<m_inputData.size().second) & (counterNaN==0))
        {
            if (isnan(rawInterpData_row[m_inputData.size().second + t]))
            {
                counterNaN = counterNaN + 1;
            }
            t = t + 1;
        }*/
        unsigned long t = rawInterpData_row.size() - m_inputData.size().second;
        while ((t<rawInterpData_row.size()) & (counterNaN==0))
        {
            if (isnan(rawInterpData_row[t]))
            {
                counterNaN = counterNaN + 1;
            }
            t = t + 1;
        }
        // -------------------------------
        // Update of m_inputData after interpolation --> if there still are NaN Values, all the values of m_inputData are put to NaN
        for (int e = 0; e<m_inputData.size().second; e++)
        {
            if (counterNaN ==0)
            {
                if (m_rawInterpData.size().second == m_inputData.size().second)
                {
                    m_inputData(ch,e) = (float)rawInterpData_row[e];
                }
                else
                {
                    m_inputData(ch,e) = (float)rawInterpData_row[m_inputData.size().second + e];
                }
            }
            else
            {
                m_inputData(ch,e) = nan(" ");
            }
        }
    }
    // -----------------------------------------------------------------
}

void MBT_MainQC::MBT_featuresQualityChecker(bool bandpassProcess, float firstBound, float secondBound)
{
    //this->m_testFeatures = m_testFeatures;

    // Construction de m_testFeatures
    for (int ch=0;ch<m_inputData.size().first;ch++)
    //for (int ch=0;ch<1;ch++)
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
            std::vector<double> tmp_tmp_signal(tmp_signal.begin(),tmp_signal.end());
            std::vector<double> signal = RemoveDC(tmp_tmp_signal); // Remove DC
            // Add the possibility to do a bandpass filter
            if(bandpassProcess){
                std::vector<double> freqBounds;
                freqBounds.push_back(firstBound);
                freqBounds.push_back(secondBound);
                signal = BandPassFilter(signal, freqBounds);
            }
            for (unsigned int v=0;v<signal.size();v++)
            {
                signal[v] = signal[v]*pow(10,6);
            }
            //std::vector<double> signal = tmp_tmp_signal;
            // TODO:
            // notch

            // compute each features for each channel and add 1 to f

            // TIME DOMAIN
            // ===========
            // Median
            double mediane = median(signal);
            m_testFeatures(ch,f) = (float)mediane;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Mean
            double moyenne = mean(signal);
            m_testFeatures(ch,f) = (float)moyenne;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Variance
            double variance = var(signal);
            m_testFeatures(ch,f) = (float)variance;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // VRMS
            std::vector<double> PowerOfTwoSignal;
            PowerOfTwoSignal.assign(signal.size(),0);
            for (unsigned int i = 0; i<signal.size(); i++)
            {
                PowerOfTwoSignal[i] = pow(signal[i] - mean(signal),(double)2);
            }

            double vrms = sqrt(mean(PowerOfTwoSignal));
            m_testFeatures(ch,f) = (float)vrms;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Vpp
            double vpp = 2.0 * sqrt(2.0) * vrms;
            m_testFeatures(ch,f) = (float)vpp;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Skewness
            double S = skewness(signal);
            m_testFeatures(ch,f) = (float)S;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Kurtosis
            double K = kurtosis(signal);
            m_testFeatures(ch,f) = (float)K;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Integrated EEG
            double ieeg = 0;
            for (unsigned int i = 0; i <signal.size(); i++)
            {
                ieeg = ieeg + abs(signal[i]);
            }
            m_testFeatures(ch,f) = (float)ieeg;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Mean absolute value
            double mav = ieeg/(double)signal.size();
            m_testFeatures(ch,f) = (float)mav;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Simple square integral
            double ssi = 0;
            for (unsigned int i = 0; i <signal.size(); i++)
            {
                ssi = ssi + pow(abs(signal[i]),2);
            }
            m_testFeatures(ch,f) = (float)ssi;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            //v-order 2 and 3
            double v2 = 0;
            double v3 = 0;
            for (unsigned int i = 0; i <signal.size(); i++)
            {
                v2 = v2 + pow(signal[i],2);
                v3 = v3 + pow(abs(signal[i]),3);
            }
            v2 = sqrt(v2/(double)signal.size());
            m_testFeatures(ch,f) = (float)v2;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            double oneThird = double(1)/double(3);
            v3 = pow(v3/(double)signal.size(),oneThird);
            m_testFeatures(ch,f) = (float)v3;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Log detector
            double log_detector = 0;
            for (unsigned int i = 0; i <signal.size(); i++)
            {
                if (abs(signal[i]) != 0)
                {
                    log_detector = log_detector + log10(abs(signal[i]));
                }
            }
            log_detector = exp(log_detector/(double)signal.size());
            m_testFeatures(ch,f) = (float)log_detector;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Average amplitude change
            double aac = 0;
            for (unsigned int i = 0; i< signal.size()-1; i++)
            {
                aac = aac + abs(signal[i+1]-signal[i]);
            }
            aac = aac/(double)signal.size();
            m_testFeatures(ch,f) = (float)aac;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Difference absolute standard deviation value
            double dasdv = 0;
            for (unsigned int i = 0; i< signal.size()-1; i++)
            {
                dasdv = dasdv + pow((signal[i+1]-signal[i]),2);
            }
            dasdv = sqrt(dasdv/((double)signal.size()-1));
            m_testFeatures(ch,f) = (float)dasdv;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Number of maxima and minima
            std::vector<double> time_point, diff_time_point, good_ind, time_point_tmp, X_tmp;
            time_point.assign(signal.size()+1,0);
            diff_time_point.assign(signal.size(),0);

            for (unsigned int i = 0; i<signal.size()+1; i++)
            {
                time_point[i] = double(i)/double(m_sampRate);
            }
            for (unsigned int i = 0; i<signal.size(); i++)
            {
                diff_time_point[i] = time_point[i+1] - time_point[i];
                if (diff_time_point[i] != 0)
                {
                    good_ind.push_back(i);
                    time_point_tmp.push_back(time_point[i]);
                    X_tmp.push_back(signal[i]);
                }
            }
            std::vector<double> deriv_X;
            deriv_X.assign(X_tmp.size()-1,0);
            int nb_max_min = 0;
            for (unsigned int i = 0; i < X_tmp.size()-1; i++)
            {
                deriv_X[i] = double(X_tmp[i+1] - X_tmp[i]) / double(time_point_tmp[i+1] - time_point_tmp[i]); // first derivative of the signal
                if (deriv_X[i] < 0.01)
                {
                    nb_max_min = nb_max_min + 1;
                }
            }
            m_testFeatures(ch,f) = (float)nb_max_min;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // 2nd Hjorth parameter = mobility
            double mobility = 0;
            if (standardDeviation(signal) != 0)
            {
                mobility = standardDeviation(deriv_X)/standardDeviation(signal);
            }
            m_testFeatures(ch,f) = (float)mobility;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // 3rd Hjorth parameter = complexity
            std::vector<double> time_point_two, diff_time_point_two, good_ind_two, time_point_tmp_two, deriv_X_two_tmp;
            time_point_two.assign(signal.size(),0);
            diff_time_point_two.assign(signal.size(),0);

            for (unsigned int i = 0; i<signal.size(); i++)
            {
                time_point_two[i] = double(i)/double(m_sampRate);
            }
            for (unsigned int i = 0; i<signal.size()-1; i++)
            {
                diff_time_point_two[i] = time_point_two[i+1] - time_point_two[i];
                if (diff_time_point_two[i] != 0)
                {
                    good_ind_two.push_back(i);
                    time_point_tmp_two.push_back(time_point_two[i]);
                    deriv_X_two_tmp.push_back(deriv_X[i]);
                }
            }
            std::vector<double> deriv_X_two;
            deriv_X_two.assign(deriv_X_two_tmp.size()-1,0);
            for (unsigned int i = 0; i < deriv_X_two_tmp.size()-1; i++)
            {
                deriv_X_two[i] = double(deriv_X_two_tmp[i+1] - deriv_X_two_tmp[i]) / double(time_point_tmp_two[i+1] - time_point_tmp_two[i]); // second derivative of the signal
            }
            double complexity = 0;
            if ((standardDeviation(deriv_X) != 0) & (mobility != 0))
            {
                complexity = (standardDeviation(deriv_X_two)/standardDeviation(deriv_X)) / mobility;
            }
            m_testFeatures(ch,f) = (float)complexity;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Zero-crossing rate
            double zrc = 0;
            double indPlusUn, ind;
            for (unsigned int i = 0; i< signal.size()-1; i++)
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
            zrc = zrc/(double)signal.size();

            m_testFeatures(ch,f) = (float)zrc;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Zero-crossing rate of 1st derivative
            double zrc_deriv_one = 0;
            double deriv_indPlusUn, deriv_ind;
            for (unsigned int i = 0; i< deriv_X.size()-1; i++)
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
            zrc_deriv_one = zrc_deriv_one/(double)deriv_X.size();

            m_testFeatures(ch,f) = (float)zrc_deriv_one;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Zero-crossing rate of 2nd derivative
            double zrc_deriv_two = 0;
            double deriv_two_indPlusUn, deriv_two_ind;
            for (unsigned int i = 0; i< deriv_X_two.size()-1; i++)
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
            zrc_deriv_two = zrc_deriv_two/(double)deriv_X_two.size();

            m_testFeatures(ch,f) = (float)zrc_deriv_two;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Variance of 1st derivative
            double var_deriv_one = var(deriv_X);
            m_testFeatures(ch,f) = (float)var_deriv_one;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Variance of 2nd derivative
            double var_deriv_two = var(deriv_X_two);
            m_testFeatures(ch,f) = (float)var_deriv_two;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

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


            // Non linear energy
            std::vector<double> nle;
            nle.assign(signal.size()-2,0);
            for (unsigned int i = 1; i<signal.size()-1; i++)
            {
                nle[i-1] = pow(signal[i],2) - signal[i-1]*signal[i+1];
            }
            double nle_mean = mean(nle);
            m_testFeatures(ch,f) = (float)nle_mean;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            //Maximum, kurtosis, std and skewness of the data of each frequency bands
            std::vector<double> freqBounds;
            freqBounds.assign(2,0);
            freqBounds[0] = 0.5;
            freqBounds[1] = 4.0;
            std::vector<double> signalDelta = BandPassFilter(signal,freqBounds);
            freqBounds[0] = 4.0;
            freqBounds[1] = 8.0;
            std::vector<double> signalTheta = BandPassFilter(signal, freqBounds);
            freqBounds[0] = 8.0;
            freqBounds[1] = 13.0;
            std::vector<double> signalAlpha = BandPassFilter(signal, freqBounds);
            freqBounds[0] = 13.0;
            freqBounds[1] = 28.0;
            std::vector<double> signalBeta = BandPassFilter(signal, freqBounds);
            freqBounds[0] = 28.0;
            freqBounds[1] = 110.0;
            std::vector<double> signalGamma = BandPassFilter(signal, freqBounds);
            double ampDelta_sig = *std::max_element(signalDelta.begin(), signalDelta.end());
            double kurtDelta_sig = kurtosis(signalDelta);
            double stdDelta_sig = standardDeviation(signalDelta);
            double skewDelta_sig = skewness(signalDelta);
            m_testFeatures(ch,f) = (float)ampDelta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)kurtDelta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)stdDelta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)skewDelta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            double ampTheta_sig = *std::max_element(signalTheta.begin(), signalTheta.end());
            double kurtTheta_sig = kurtosis(signalTheta);
            double stdTheta_sig = standardDeviation(signalTheta);
            double skewTheta_sig = skewness(signalTheta);
            m_testFeatures(ch,f) = (float)ampTheta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)kurtTheta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)stdTheta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)skewTheta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            double ampAlpha_sig = *std::max_element(signalAlpha.begin(), signalAlpha.end());
            double kurtAlpha_sig = kurtosis(signalAlpha);
            double stdAlpha_sig = standardDeviation(signalAlpha);
            double skewAlpha_sig = skewness(signalAlpha);
            m_testFeatures(ch,f) = (float)ampAlpha_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)kurtAlpha_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)stdAlpha_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)skewAlpha_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            double ampBeta_sig = *std::max_element(signalBeta.begin(), signalBeta.end());
            double kurtBeta_sig = kurtosis(signalBeta);
            double stdBeta_sig = standardDeviation(signalBeta);
            double skewBeta_sig = skewness(signalBeta);
            m_testFeatures(ch,f) = (float)ampBeta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)kurtBeta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)stdBeta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)skewBeta_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            double ampGamma_sig = *std::max_element(signalGamma.begin(), signalGamma.end());
            double kurtGamma_sig = kurtosis(signalGamma);
            double stdGamma_sig = standardDeviation(signalGamma);
            double skewGamma_sig = skewness(signalGamma);
            m_testFeatures(ch,f) = (float)ampGamma_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)kurtGamma_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)stdGamma_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;
            m_testFeatures(ch,f) = (float)skewGamma_sig;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // FREQUENY DOMAIN
            // ===============

            // Zero-padding
            // ------------
            unsigned int N = signal.size();
            int tmp_nfft = (int)pow(2, ceil(log(N)/log(2)));
            unsigned int nfft = std::max(256,tmp_nfft);
            unsigned int nb_zero_added = nfft-N;
            if (nfft>N)
            {
                for (unsigned int i = 0; i < nfft - N; i++)
                {
                    signal.push_back(0);
                }
            }
            N = signal.size();
            // ------------

            // Total power
            /*std::vector<std::complex<float> > dataToTransform;
            dataToTransform.assign(signal.size(),0);
            for (unsigned int i = 0; i < signal.size(); i++)
            {
                dataToTransform[i] = signal[i];
            }
            MBT_Fourier::forwardBluesteinFFT(dataToTransform)*/


            std::vector<std::complex<double> > complex_signal;
            for (unsigned int ki=0;ki<signal.size();ki++)
            {
                complex_signal.push_back(std::complex<double>(signal[ki], 0));
            }
            DOUBLE_FFTW_C2C_1D *obj = new DOUBLE_FFTW_C2C_1D(complex_signal.size(), complex_signal,-1);
            std::vector<complex<double> > dataToTransform(complex_signal.size());
            dataToTransform = obj -> fft_execute();
            delete obj;

            std::vector<double> tmp_EEG_power;
            tmp_EEG_power.assign(nfft,0);
            std::vector<double> tmp_freqVector;
            tmp_freqVector.assign(nfft,0);
            for (unsigned int i = 0; i < nfft; i++)
            {
                tmp_EEG_power[i] = abs(dataToTransform[i]); //Retain Magnitude (imaginary part = phase)
                tmp_freqVector[i] = (double(i)*double(m_sampRate))/double(nfft);
            }
            // Generate the one-sided spectrum
            std::vector<double> EEG_power;
            std::vector<double> freqVector;
            EEG_power.assign(nfft/2,0);
            freqVector.assign(nfft/2,0);
            for (unsigned int i=0; i<nfft/2; i++)
            {
                freqVector[i] = tmp_freqVector[i];
                EEG_power[i] = tmp_EEG_power[i];
            }
            double ttp = 0;
            for (unsigned int i = 0; i < EEG_power.size(); i++)
            {
                ttp = ttp + pow(EEG_power[i],(double)2);
            }
            ttp = ttp/pow((double)EEG_power.size(),(double)2);
            m_testFeatures(ch,f) = (float)ttp;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            double AUC_EEG_power = trapz(freqVector,EEG_power);


            // Ratio delta
            std::vector<double> delta_power, freqVector_delta;
            for (unsigned int tmp = 0 ; tmp < freqVector.size(); tmp++)
            {
                if ( (freqVector[tmp] >= 0.5) && (freqVector[tmp] <= 4.0) )
                {
                    freqVector_delta.push_back(freqVector[tmp]);
                    delta_power.push_back(EEG_power[tmp]);
                }
            }
            double AUC_delta_power = trapz(freqVector_delta,delta_power);
            double ratio_delta = 0;
            if (AUC_EEG_power != 0)
            {
                ratio_delta = AUC_delta_power/AUC_EEG_power;
            }
            m_testFeatures(ch,f) = (float)ratio_delta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // delta power
            double delta_pow = 0;
            for (unsigned int i = 0; i < delta_power.size(); i++)
            {
                delta_pow = delta_pow + delta_power[i]*delta_power[i];
            }
            delta_pow = delta_pow/pow((double)delta_power.size(),(double)2);
            m_testFeatures(ch,f) = (float)delta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // log delta power
            double log_delta_pow = 0;
            if (delta_pow != 0)
            {
                log_delta_pow = log10(delta_pow);
            }
            m_testFeatures(ch,f) = (float)log_delta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Normalized delta power
            double n_delta_pow = 0;
            if (ttp != 0)
            {
                n_delta_pow = delta_pow/ttp; // division of energy
            }
            m_testFeatures(ch,f) = (float)n_delta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Ratio theta
            std::vector<double> theta_power, freqVector_theta;
            for (unsigned int tmp = 0 ; tmp < freqVector.size(); tmp++)
            {
                if ( (freqVector[tmp] >= 4.0) && (freqVector[tmp] <= 8.0) )
                {
                    freqVector_theta.push_back(freqVector[tmp]);
                    theta_power.push_back(EEG_power[tmp]);
                }
            }
            double AUC_theta_power = trapz(freqVector_theta,theta_power);
            double ratio_theta = 0;
            if (AUC_EEG_power != 0)
            {
                ratio_theta = AUC_theta_power/AUC_EEG_power;
            }
            m_testFeatures(ch,f) = (float)ratio_theta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // theta power
            double theta_pow = 0;
            for (unsigned int i = 0; i < theta_power.size(); i++)
            {
                theta_pow = theta_pow + theta_power[i]*theta_power[i];
            }
            theta_pow = theta_pow/pow((double)theta_power.size(),(double)2);
            m_testFeatures(ch,f) = (float)theta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // log theta power
            double log_theta_pow = 0;
            if (theta_pow != 0)
            {
                log_theta_pow = log10(theta_pow);
            }
            m_testFeatures(ch,f) = (float)log_theta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Normalized theta power
            double n_theta_pow = 0;
            if (ttp != 0)
            {
                n_theta_pow = theta_pow/ttp; // division of energy
            }
            m_testFeatures(ch,f) = (float)n_theta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Ratio alpha
            std::vector<double> alpha_power, freqVector_alpha;
            for (unsigned int tmp = 0 ; tmp < freqVector.size(); tmp++)
            {
                if ( (freqVector[tmp] >= 8.0) && (freqVector[tmp] <= 13.0) )
                {
                    freqVector_alpha.push_back(freqVector[tmp]);
                    alpha_power.push_back(EEG_power[tmp]);
                }
            }
            double AUC_alpha_power = trapz(freqVector_alpha,alpha_power);
            double ratio_alpha = 0;
            if (AUC_EEG_power != 0)
            {
                ratio_alpha = AUC_alpha_power/AUC_EEG_power;
            }
            m_testFeatures(ch,f) = (float)ratio_alpha;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // alpha power
            double alpha_pow = 0;
            for (unsigned int i = 0; i < alpha_power.size(); i++)
            {
                alpha_pow = alpha_pow + alpha_power[i]*alpha_power[i];
            }
            alpha_pow = alpha_pow/pow((double)alpha_power.size(),(double)2);
            m_testFeatures(ch,f) = (float)alpha_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // log alpha power
            double log_alpha_pow = 0;
            if (alpha_pow != 0)
            {
                log_alpha_pow = log10(alpha_pow);
            }
            m_testFeatures(ch,f) = (float)log_alpha_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Normalized alpha power
            double n_alpha_pow = 0;
            if (ttp != 0)
            {
                n_alpha_pow = alpha_pow/ttp; // division of energy
            }
            m_testFeatures(ch,f) = (float)n_alpha_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Ratio beta
            std::vector<double> beta_power, freqVector_beta;
            for (unsigned int tmp = 0 ; tmp < freqVector.size(); tmp++)
            {
                if ( (freqVector[tmp] >= 13.0) && (freqVector[tmp] <= 28.0) )
                {
                    freqVector_beta.push_back(freqVector[tmp]);
                    beta_power.push_back(EEG_power[tmp]);
                }
            }
            double AUC_beta_power = trapz(freqVector_beta,beta_power);
            double ratio_beta = 0;
            if (AUC_EEG_power != 0)
            {
                ratio_beta = AUC_beta_power/AUC_EEG_power;
            }
            m_testFeatures(ch,f) = (float)ratio_beta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // beta power
            double beta_pow = 0;
            for (unsigned int i = 0; i < beta_power.size(); i++)
            {
                beta_pow = beta_pow + beta_power[i]*beta_power[i];
            }
            beta_pow = beta_pow/pow((double)beta_power.size(),(double)2);
            m_testFeatures(ch,f) = (float)beta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // log beta power
            double log_beta_pow = 0;
            if (beta_pow != 0)
            {
                log_beta_pow = log10(beta_pow);
            }
            m_testFeatures(ch,f) = (float)log_beta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Normalized beta power
            double n_beta_pow = 0;
            if (ttp != 0)
            {
                n_beta_pow = beta_pow/ttp; // division of energy
            }
            m_testFeatures(ch,f) = (float)n_beta_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Ratio gamma
            std::vector<double> gamma_power, freqVector_gamma;
            for (unsigned int tmp = 0 ; tmp < freqVector.size(); tmp++)
            {
                if ( freqVector[tmp] >= 28.0)
                {
                    freqVector_gamma.push_back(freqVector[tmp]);
                    gamma_power.push_back(EEG_power[tmp]);
                }
            }
            double AUC_gamma_power = trapz(freqVector_gamma,gamma_power);
            double ratio_gamma = 0;
            if (AUC_EEG_power != 0)
            {
                ratio_gamma = AUC_gamma_power/AUC_EEG_power;
            }
            m_testFeatures(ch,f) = (float)ratio_gamma;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // gamma power
            double gamma_pow = 0;
            for (unsigned int i = 0; i < gamma_power.size(); i++)
            {
                gamma_pow = gamma_pow + gamma_power[i]*gamma_power[i];
            }
            gamma_pow = gamma_pow/pow((double)gamma_power.size(),(double)2);
            m_testFeatures(ch,f) = (float)gamma_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // log gamma power
            double log_gamma_pow = 0;
            if (gamma_pow != 0)
            {
                log_gamma_pow = log10(gamma_pow);
            }
            m_testFeatures(ch,f) = (float)log_gamma_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Normalized gamma power
            std::vector<double> n_gamma_power_tmp;
            double n_gamma_pow = 0;
            if (ttp != 0)
            {
                n_gamma_pow = gamma_pow/ttp; // division of energy
            }
            m_testFeatures(ch,f) = (float)n_gamma_pow;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Spectral Edge Frequency 80%
            double sum_EEG_power = std::accumulate(EEG_power.begin(), EEG_power.begin() + EEG_power.size(), 0.0);
            std::vector<double> EEG_power_norm;
            EEG_power_norm.assign(EEG_power.size(),0);
            if (sum_EEG_power != 0)
            {
                for (unsigned int i=0; i<EEG_power.size(); i++)
                {
                    EEG_power_norm[i] = EEG_power[i]/sum_EEG_power;
                }
                std::vector<double> EEG_power_cum;
                EEG_power_cum.assign(EEG_power_norm.size(),0);
                std::partial_sum (EEG_power_norm.begin(), EEG_power_norm.end(), EEG_power_cum.begin());
                std::vector<double> EEG_power_cum_80;
                EEG_power_cum_80.assign(EEG_power_cum.size(),0);
                for (unsigned int i = 0; i < EEG_power_cum_80.size(); i++)
                {
                    EEG_power_cum_80[i] = abs(EEG_power_cum[i] - (double)0.2);
                }
                int idx_sef_80 = min_element(EEG_power_cum_80.begin(), EEG_power_cum_80.end()) - EEG_power_cum_80.begin();
                double sef80 = freqVector[idx_sef_80];
                m_testFeatures(ch,f) = (float)sef80;
                if (isnan(m_testFeatures(ch,f)))
                {
                    m_testFeatures(ch,f) = 0;
                }
                f = f+1;


                // Spectral Edge Frequency 90%
                std::vector<double> EEG_power_cum_90;
                EEG_power_cum_90.assign(EEG_power_cum.size(),0);
                for (unsigned int i = 0; i < EEG_power_cum_90.size(); i++)
                {
                    EEG_power_cum_90[i] = abs(EEG_power_cum[i] - (double)0.1);
                }
                int idx_sef_90 = min_element(EEG_power_cum_90.begin(), EEG_power_cum_90.end()) - EEG_power_cum_90.begin();
                double sef90 = freqVector[idx_sef_90];
                m_testFeatures(ch,f) = (float)sef90;
                if (isnan(m_testFeatures(ch,f)))
                {
                    m_testFeatures(ch,f) = 0;
                }
                f = f+1;


                // Spectral Edge Frequency 95%
                std::vector<double> EEG_power_cum_95;
                EEG_power_cum_95.assign(EEG_power_cum.size(),0);
                for (unsigned int i = 0; i < EEG_power_cum_95.size(); i++)
                {
                    EEG_power_cum_95[i] = abs(EEG_power_cum[i] - (double)0.05);
                }
                int idx_sef_95 = min_element(EEG_power_cum_95.begin(), EEG_power_cum_95.end()) - EEG_power_cum_95.begin();
                double sef95 = freqVector[idx_sef_95];
                m_testFeatures(ch,f) = (float)sef95;
                if (isnan(m_testFeatures(ch,f)))
                {
                    m_testFeatures(ch,f) = 0;
                }
                f = f+1;
            }
            else
            {
                m_testFeatures(ch,f) = 0; // Spectral Edge Frequency 80%
                f = f+1;
                m_testFeatures(ch,f) = 0; // Spectral Edge Frequency 90%
                f = f+1;
                m_testFeatures(ch,f) = 0; // Spectral Edge Frequency 95%
                f=f+1;
            }

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
        % Calcul de la transformée de Fourier de la trame à analyser
        y = abs(fft(X));      %Retain Magnitude (imaginary part = phase)
        EEG_power = y(1:N/2);     %Discard Half of Points
        f = Fs*(0:N/2-1)/N;

        % Create a mel frequency filterbank
        nb_banks = 30; % the number of filter banks to construct
        Filter = melfilter(nb_banks,f); % ####################### required Signal Processing Toolbox

        % Pondération du spectre d'amplitude (ou de puissance selon les cas) par un banc de filtres triangulaires espacés selon l'échelle de Mel
        F_EEG_power = Filter*double(EEG_power');

        % Calcul de la transformée en cosinus discrète du log-mel-spectre % The expansion coefficients in vector X measure how much energy is stored in each of the components.
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

            // Frequency-filtered band energies
            double ff_delta = (log_theta_pow - (double)0);
            m_testFeatures(ch,f) = (float)ff_delta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            double ff_theta = (log_alpha_pow - log_delta_pow);
            m_testFeatures(ch,f) = (float)ff_theta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            double ff_alpha = (log_beta_pow - log_theta_pow);
            m_testFeatures(ch,f) = (float)ff_alpha;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            double ff_beta = (log_gamma_pow - log_alpha_pow);
            m_testFeatures(ch,f) = (float)ff_beta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            double ff_gamma = ((double)0 - log_beta_pow);
            m_testFeatures(ch,f) = (float)ff_gamma;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Relative spectral difference
            double rsd_delta = 0;
            if (theta_pow + delta_pow + (double)0 != 0)
            {
                rsd_delta = (theta_pow - (double)0)/(theta_pow + delta_pow + (double)0);
            }
            m_testFeatures(ch,f) = (float)rsd_delta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            double rsd_theta = 0;
            if (alpha_pow + theta_pow + delta_pow != 0)
            {
                rsd_theta = (alpha_pow - delta_pow)/(alpha_pow + theta_pow + delta_pow);
            }
            m_testFeatures(ch,f) = (float)rsd_theta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            double rsd_alpha = 0;
            if (beta_pow + alpha_pow + theta_pow != 0)
            {
                rsd_alpha = (beta_pow - theta_pow)/(beta_pow + alpha_pow + theta_pow);
            }
            m_testFeatures(ch,f) = (float)rsd_alpha;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            double rsd_beta = 0;
            if (gamma_pow + beta_pow + alpha_pow != 0)
            {
                rsd_beta = (gamma_pow - alpha_pow)/(gamma_pow + beta_pow + alpha_pow);
            }
            m_testFeatures(ch,f) = (float)rsd_beta;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            double rsd_gamma = 0;
            if ((double)0 + gamma_pow + beta_pow != 0)
            {
                rsd_gamma = ((double)0 - beta_pow)/((double)0 + gamma_pow + beta_pow);
            };
            m_testFeatures(ch,f) = (float)rsd_gamma;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // SN ratio
            std::vector<double> EEG_noise;
            for (unsigned int tmp = 0 ; tmp < freqVector.size(); tmp++)
            {
                if (freqVector[tmp] > (double)30.0)
                {
                    EEG_noise.push_back(EEG_power[tmp]);
                }
            }
            double tot_EEG_noise = 0;
            for (unsigned int i = 0; i < EEG_noise.size(); i++)
            {
                tot_EEG_noise = tot_EEG_noise + EEG_noise[i]*EEG_noise[i];
            }
            tot_EEG_noise = tot_EEG_noise/pow((double)EEG_noise.size(),(double)2);
            double SN_ratio = 0;
            if (tot_EEG_noise != 0)
            {
                SN_ratio = ttp/tot_EEG_noise;
            }
            m_testFeatures(ch,f) = (float)SN_ratio;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Power spectrum moments
            double m0 = 0;
            for (unsigned int i=0; i<EEG_power.size(); i++)
            {
                m0 = m0 + EEG_power[i]*EEG_power[i];
            }
            m_testFeatures(ch,f) = (float)m0;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            double m1 = 0;
            for (unsigned int i=0; i<EEG_power.size(); i++)
            {
                m1 = m1 + EEG_power[i]*EEG_power[i]*freqVector[i];
            }
            m_testFeatures(ch,f) = (float)m1;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            double m2 = 0;
            for (unsigned int i=0; i<EEG_power.size(); i++)
            {
                m2 = m2 + EEG_power[i]*EEG_power[i]*pow(freqVector[i],(double)2);
            }
            m_testFeatures(ch,f) = (float)m2;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Power spectrum center frequency
            double center_freq = 0;
            if (m0 != 0)
            {
                center_freq = m1/m0;
            }
            m_testFeatures(ch,f) = (float)center_freq;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Spectral RMS
            double sp_rms = 0;
            if (N-nb_zero_added != 0)
            {
                sp_rms = sqrt(m0/(N-nb_zero_added));
            }
            m_testFeatures(ch,f) = (float)sp_rms;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;

            // Index of spectral deformation
            double omega_ratio = 0;
            if ((m0 != 0) & (m2/m0 >=0) & (center_freq != 0))
            {
                omega_ratio = (sqrt(m2/m0))/center_freq;
            }
            m_testFeatures(ch,f) = (float)omega_ratio;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Modified Median Frequency
            std::vector<double> ecart;
            ecart.assign((unsigned int) floor((double)m_sampRate/(double)2),0);
            for (unsigned int i = 0; i<floor(m_sampRate/2); i++)
            {
                unsigned int f_dist = 0;
                for (unsigned int j = 0; j<freqVector.size(); j++)
                {
                    if (freqVector[j]<=i && j>f_dist)
                    {
                        f_dist = j;
                    }
                }
                double ASD1 = std::accumulate(EEG_power.begin(), EEG_power.begin() + f_dist, 0.0);
                double ASD2 = std::accumulate(EEG_power.begin() + f_dist, EEG_power.end(), 0.0);
                ecart[i] = abs(ASD1 - ASD2);
            }
            double low_ecart = *std::min_element(ecart.begin(), ecart.end());
            std::vector<double> mod_median_freq_tmp;
            for (unsigned int i = 0; i<ecart.size(); i++)
            {
                if (ecart[i] == low_ecart)
                {
                    mod_median_freq_tmp.push_back(i);
                }
            }
            double mod_median_freq = mod_median_freq_tmp[0]; // we take the smallest index
            m_testFeatures(ch,f) = (float)mod_median_freq;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;


            // Modified Mean Frequency
            double mod_mean_freq = 0.0;
            for (unsigned int i = 0; i<EEG_power.size(); i++)
            {
                mod_mean_freq = mod_mean_freq + freqVector[i]*EEG_power[i];
            }
            if (sum_EEG_power != 0)
            {
                mod_mean_freq = mod_mean_freq/sum_EEG_power;
            }
            else
            {
                mod_mean_freq = 0;
            }
            m_testFeatures(ch,f) = (float)mod_mean_freq;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }
            f = f+1;



            // ENTROPY DOMAIN
            // ==============
            std::vector<double> PSD;
            PSD.assign(EEG_power.size(),0);
            for (unsigned int i = 0; i<EEG_power.size(); i++)
            {
                PSD[i] = pow(EEG_power[i],(double)2);
            }
            double sum_PSD = std::accumulate(PSD.begin(), PSD.begin() + PSD.size(), 0.0);
            std::vector<double> PDF;
            PDF.assign(EEG_power.size(),0);
            if (sum_PSD != 0)
            {
                for (unsigned int i = 0; i<EEG_power.size(); i++)
                {
                    PDF[i] = PSD[i]/sum_PSD; // estimate of the probability density function

                }
            }


        /*//Shannon Entropy
        ePDF = hist(PDF(:), length(f));% Compute N-bin histogram
        ePDF = ePDF / sum(ePDF(:));% Compute probabilities
        i = find(ePDF); % Make mask to eliminate 0's since log(0) = -inf.
        sh_h = -sum(ePDF(i) .* log(ePDF(i))); % Compute entropy*/

            // Spectral Entropy
            double s_h = 0;
            for (unsigned int i = 0; i<PDF.size(); i++)
            {
                if (PDF[i] != 0) // eliminate 0's since log(0) = -inf.
                {
                    s_h = s_h + PDF[i]*log(PDF[i]);
                }
            }
            if (log10(freqVector.size()) != 0)
            {
                s_h = -s_h/log10(freqVector.size()); // normalized spectral entropy
            }
            else
            {
                s_h = 0;
            }
            m_testFeatures(ch,f) = (float)s_h;
            if (isnan(m_testFeatures(ch,f)))
            {
                m_testFeatures(ch,f) = 0;
            }


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

        }
    }
}


void MBT_MainQC::MBT_knn()
{

    // Recover the different type of classes
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    std::vector<double> typeClasses;
    typeClasses.assign(m_trainingClasses.size(),0);
    for (unsigned int t=0;t<typeClasses.size();t++)
    {
        typeClasses[t] = m_trainingClasses[t];
    }
    std::sort( typeClasses.begin(), typeClasses.end() );
    typeClasses.erase( std::unique( typeClasses.begin(), typeClasses.end() ), typeClasses.end() );
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // Initialization of some variables
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    m_predictedClass.assign(m_testFeatures.size().first,0);
    m_probaClass.assign(m_testFeatures.size().first,0);
    std::vector<double> distanceNeighbor;
    distanceNeighbor.assign(m_trainingFeatures.size().first, 0);
    std::vector<double> yPredict;
    yPredict.assign(typeClasses.size(), 0);
    std::vector<double> norm_m_testFeatures(m_testFeatures.size().second,0);
    std::vector<double> tmp_distanceNeighbor(norm_m_testFeatures.size(),0);
    std::vector<double> sortDistanceNeighbor;
    std::vector<int> indiceNeighbor;
    indiceNeighbor.assign(m_trainingFeatures.size().first,0);

    /*int n;
    int n1;
    int t;
    int t1;
    unsigned int t2;*/

    double minDist;
    std::vector<double> tmpProbaClass;
    tmpProbaClass.assign(typeClasses.size(), 0);
    //int redondance;
    std::vector<double> distanceNeighborNormalized;
    distanceNeighborNormalized.assign(m_trainingFeatures.size().first,0);
    std::vector<double> distanceNeighborWeight;
    distanceNeighborWeight.assign(m_trainingFeatures.size().first,0);
    std::vector<double> observedWeight;
    observedWeight.assign(m_trainingFeatures.size().first,0);
    std::vector<double> weight;
    weight.assign(m_trainingFeatures.size().first,0);
    std::vector<double> classNeighbor;
    classNeighbor.assign(m_trainingFeatures.size().first,0);
    std::vector<double> tmp_yPredict;
    tmp_yPredict.assign(m_costClass.size().first,0);
    float inf = std::numeric_limits<double>::infinity();
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


    // Normalization of Training dataset
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //MBT_Matrix<double> copy_m_trainingFeatures(m_trainingFeatures);
    MBT_Matrix<double> copy_m_trainingFeatures(m_trainingFeatures.size().first,m_trainingFeatures.size().second);
    for (int n =0; n<copy_m_trainingFeatures.size().first; n++)
    {
        for (int n1 = 0; n1<copy_m_trainingFeatures.size().second;n1++)
        {
            copy_m_trainingFeatures(n,n1)= ((double)m_trainingFeatures(n,n1) - (double)m_mu[n1])/(double)m_sigma[n1];
        }
    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    for (int t =0;t<m_testFeatures.size().first;t++)
    {
        std::vector<float> m_testFeatures_row = m_testFeatures.row(t);
        std::vector<double> testNaNFeatures(m_testFeatures_row.begin(),m_testFeatures_row.end());
        if (std::all_of(testNaNFeatures.begin(), testNaNFeatures.end(), [](double testNaN){return std::isnan(testNaN);}) )
        {
            m_predictedClass[t] = 0;
            m_probaClass[t] = inf;
        }
        else
        {
            // Don't modify m_testFeatures
            // Normalize each vector (new observation) in m_testFeatures
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            for (int t1 =0;t1<m_testFeatures.size().second;t1++)
            {
                norm_m_testFeatures[t1] = ((double)m_testFeatures(t,t1) - (double)m_mu[t1])/(double)m_sigma[t1];
            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

            // Find the k nearest neighbors of the new observation on the training dataset
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // Find distances
            for (int t1=0;t1<m_trainingFeatures.size().first;t1++)
            {
                distanceNeighbor[t1] = 0;
                for (unsigned int t2 =0;t2<norm_m_testFeatures.size();t2++)
                {
                    tmp_distanceNeighbor[t2] = norm_m_testFeatures[t2] - copy_m_trainingFeatures(t1,t2);
                    tmp_distanceNeighbor[t2] = pow(tmp_distanceNeighbor[t2],(double)2);
                    distanceNeighbor[t1] = distanceNeighbor[t1] + tmp_distanceNeighbor[t2];
                }
                distanceNeighbor[t1] = sqrt(distanceNeighbor[t1]);
            }
            //std::cout<<"distanceNeighbor "<<std::endl;
            //std::cout<<distanceNeighbor[0]<<" "<<distanceNeighbor[1]<<" "<<distanceNeighbor[2]<<" "<<distanceNeighbor[3]<<" "<<distanceNeighbor[4]<<" "<<distanceNeighbor[5]<<" "<<distanceNeighbor[6]<<" "<<distanceNeighbor[7]<<" "<<distanceNeighbor[8]<<" "<<distanceNeighbor[9]<<std::endl;

            // Sort distances from neighbors (sort and remove the doublon distance; in case of doublon we keep the smallest indice neigbor)
            sortDistanceNeighbor = distanceNeighbor;
            std::sort( sortDistanceNeighbor.begin(), sortDistanceNeighbor.end() );
            sortDistanceNeighbor.erase( std::unique( sortDistanceNeighbor.begin(), sortDistanceNeighbor.end() ), sortDistanceNeighbor.end() );

            // Find indexes of the neighbors
            indiceNeighbor.resize(sortDistanceNeighbor.size(),0);
            for (unsigned int t1= 0; t1<sortDistanceNeighbor.size();t1++)
            {
                int results = std::find( distanceNeighbor.begin(), distanceNeighbor.end(), sortDistanceNeighbor[t1] )- distanceNeighbor.begin();
                indiceNeighbor[t1]= results;
            }

            // Keep the kppv smallest distances of the k nearest neighbors
            sortDistanceNeighbor.erase (sortDistanceNeighbor.begin()+m_kppv,sortDistanceNeighbor.end());
            indiceNeighbor.erase (indiceNeighbor.begin()+m_kppv,indiceNeighbor.end());
            //std::cout<<"sortDistanceNeighbor "<<std::endl;
            //std::cout<<sortDistanceNeighbor[0]<<" "<<sortDistanceNeighbor[1]<<" "<<sortDistanceNeighbor[2]<<" "<<sortDistanceNeighbor[3]<<" "<<sortDistanceNeighbor[4]<<" "<<sortDistanceNeighbor[5]<<" "<<sortDistanceNeighbor[6]<<std::endl;
            //std::cout<<"indiceNeighbor "<<std::endl;
            //std::cout<<indiceNeighbor[0]<<" "<<indiceNeighbor[1]<<" "<<indiceNeighbor[2]<<" "<<indiceNeighbor[3]<<" "<<indiceNeighbor[4]<<" "<<indiceNeighbor[5]<<" "<<indiceNeighbor[6]<<std::endl;
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

            // Normalization of distances
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            minDist = *std::min_element(sortDistanceNeighbor.begin(), sortDistanceNeighbor.begin()+ sortDistanceNeighbor.size());
            //redondance = 0;

            //std::cout<<"minDist = "<<minDist<<std::endl;
            //std::cout<<std::endl;

            if (minDist!=0)
            {
                // Normalize each distance in sortDistanceNeighbor
                distanceNeighborNormalized.resize(sortDistanceNeighbor.size(),0);
                for (unsigned int t1 =0;t1<sortDistanceNeighbor.size();t1++)
                {
                    distanceNeighborNormalized[t1] = (sortDistanceNeighbor[t1]/minDist);
                }

                // Weight transformation of the distance
                distanceNeighborWeight.resize(distanceNeighborNormalized.size(),0);
                for (unsigned int t1 =0;t1<distanceNeighborNormalized.size();t1++)
                {
                    distanceNeighborWeight[t1] = 1/(pow(distanceNeighborNormalized[t1],2));
                }

                // Observed weight
                observedWeight.resize(indiceNeighbor.size(),0);
                for (unsigned int t1 =0;t1<indiceNeighbor.size();t1++)
                {
                    observedWeight[t1] = (double)m_w[indiceNeighbor[t1]];
                }

                // Final weight
                weight.resize(distanceNeighborWeight.size(),0);
                for (unsigned int t1 =0;t1<distanceNeighborWeight.size();t1++)
                {
                    weight[t1] = distanceNeighborWeight[t1]*observedWeight[t1];
                }

                // Calculation of the probability to be in each class
                classNeighbor.resize(indiceNeighbor.size(),0);
                for (unsigned int t1 =0;t1<distanceNeighborWeight.size();t1++)
                {
                    classNeighbor[t1] = (double)m_trainingClasses[indiceNeighbor[t1]];
                }

                for (unsigned int t1=0;t1<tmpProbaClass.size();t1++)
                {
                    tmpProbaClass[t1]=0;
                }
                for (unsigned int t1=0;t1<classNeighbor.size();t1++)
                {
                    int indTypeClasses = std::find( typeClasses.begin(), typeClasses.end(), classNeighbor[t1] )- typeClasses.begin();
                    tmpProbaClass[indTypeClasses] = tmpProbaClass[indTypeClasses] + weight[t1];
                }

                double sum_of_tmpProbaClass = std::accumulate(tmpProbaClass.begin(),tmpProbaClass.end(),0.0);

                for (unsigned int t1=0;t1<tmpProbaClass.size();t1++)
                {
                    tmpProbaClass[t1] = tmpProbaClass[t1]/sum_of_tmpProbaClass;
                }
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

                // Predicted class label
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                for (unsigned int t1 = 0; t1<typeClasses.size();t1++)
                {
                    yPredict[t1] = 0;
                    for (int t2=0;t2<m_costClass.size().first;t2++)
                    {
                        tmp_yPredict[t2] = tmpProbaClass[t2]*(double)m_costClass(t1,t2);
                    }
                    yPredict[t1] = std::accumulate(tmp_yPredict.begin(),tmp_yPredict.end(),0.0);
                }

                double bestYPredict = *std::min_element(yPredict.begin(), yPredict.begin()+ yPredict.size());
                int predictedClass_tmp = std::find( yPredict.begin(), yPredict.end(), bestYPredict )- yPredict.begin();
                m_predictedClass[t] = (float)typeClasses[predictedClass_tmp];
                m_probaClass[t] = (float)tmpProbaClass[predictedClass_tmp];
            }
            else
            {
                double classNeighbor_0 = (double)m_trainingClasses[indiceNeighbor[0]];
                /*int indTypeClasses = std::find( typeClasses.begin(), typeClasses.end(), classNeighbor_0 )- typeClasses.begin();
                for (unsigned int t1=0;t1<tmpProbaClass.size();t1++)
                {
                    tmpProbaClass[t1]=0;
                }
                tmpProbaClass[indTypeClasses] = 1;*/
                m_probaClass[t] = (float)inf;
                m_predictedClass[t] = (float)classNeighbor_0;
                //redondance = 1;
            }
            /*if (redondance == 0)
            {
                m_probaClass[t] = (float)tmpProbaClass[predictedClass_tmp];
            }
            else if (redondance == 1)
            {
                m_probaClass[t] = (float)inf;
            }*/
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
    }
}


void MBT_MainQC::MBT_addTraining()
{
    float seuilProba = 1; // it can be changed
    unsigned int sizeX_m_testFeatures = m_testFeatures.size().first;
    float inf = std::numeric_limits<float>::infinity();
    unsigned int sizeX_m_potTrainingFeatures;
    std::vector<float> vecteur_m_potTrainingFeatures;
    unsigned int sizeY_m_testFeatures;
    unsigned int sizeX_m_dataClean;
    std::vector<float> vecteur_m_inputData;
    unsigned int t;
    unsigned int t1;
    for (t = 0;t<sizeX_m_testFeatures;t++)
    {
        if (m_probaClass[t] != inf && m_probaClass[t] >= seuilProba)
        {
            if (m_predictedClass[t] == (float)0.25) // on the training set, we have only 3 classes (0 0.5 1)
            {
                m_predictedClass[t] = (float)0.5;
            }
            sizeX_m_potTrainingFeatures = m_potTrainingFeatures.size();
            m_potTrainingFeatures.push_back(vecteur_m_potTrainingFeatures);
            m_potTrainingFeatures[sizeX_m_potTrainingFeatures].push_back(m_probaClass[t]);// add probability of being in the class for this potentially adding segment
            m_potTrainingFeatures[sizeX_m_potTrainingFeatures].push_back(m_predictedClass[t]);// add the class for this potentially adding segment

            sizeY_m_testFeatures = m_testFeatures.size().second;
            for (t1=0;t1<sizeY_m_testFeatures;t1++)
            {
                m_potTrainingFeatures[sizeX_m_potTrainingFeatures].push_back(m_testFeatures(t,t1)); // add the features for this potentially adding segment
            }

            if (m_predictedClass[t] == (float)1)
            {
                sizeX_m_dataClean = m_dataClean.size();
                m_dataClean.push_back(vecteur_m_inputData);
                m_dataClean[sizeX_m_dataClean] = m_inputData.row(t);// add potentially clean data
            }
        }
    }
}


void MBT_MainQC::MBT_qualityChecker(MBT_Matrix<float> inputDataInit)
{
    // seuil of Itakura distance to detect muscular artifacts --> sid can be changed
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    std::vector<double> double_m_cleanItakuraDistance(m_cleanItakuraDistance.begin(),m_cleanItakuraDistance.end());
    double sid = mean(double_m_cleanItakuraDistance) + 2.5*standardDeviation(double_m_cleanItakuraDistance);
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


    // initialization of some variables
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    unsigned int t;
    int t1;
    std::vector<double> signal;
    signal.assign(m_inputData.size().second,0);
    std::vector<double> signalInit;
    signalInit.assign(inputDataInit.size().second,0);
    int cstce;
    double ampvar;
    std::vector<double> pow_signal;
    pow_signal.assign(m_inputData.size().second,0);
    double itak = 0;
    double inf = std::numeric_limits<double>::infinity();

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    for (t=0;t<(unsigned int)m_inputData.size().first;t++)
    {
        // Original signal
        std::vector<float> inputDataInit_row = inputDataInit.row(t);
        std::vector<double> double_inputDataInit_row(inputDataInit_row.begin(),inputDataInit_row.end());
        signalInit = double_inputDataInit_row;

        // Interpolated signal
        std::vector<float> m_inputData_row = m_inputData.row(t);
        std::vector<double> double_m_inputData_row(m_inputData_row.begin(),m_inputData_row.end());
        signal = double_m_inputData_row;

        // Multiply by 10^6 (to be in uV)
        for (unsigned int v=0;v<signal.size();v++)
        {
            signal[v] = signal[v]*pow(10,6);
        }

        int len = signal.size();


        // Check if signal is constant
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        int counter = 0;
        for (t1=0;t1<len-1;t1++)
        {
            if (abs((double)signal[t1+1]- (double)signal[t1])<(double)0.5) // Fanny Grosselin 2017/10/13
            {
                counter = counter + 1;
            }
        }
        cstce = (100*counter)/(len-1);
        if (cstce > 35) // Fanny Grosselin 2017/10/13
        {
            m_predictedClass[t] = (float)0;
            m_probaClass[t] = (float)inf;
            //std::cout<<"The signal no"<<t<<" is considered as constant."<<std::endl;
        }
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        else
        {
            // Test of the amplitude of variation of the signal
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            for (t1=0;t1<len;t1++)
            {
                pow_signal[t1] = pow(signal[t1]-mean(signal),(double)2);
            }
            ampvar = (double)2*sqrt((double)2)*sqrt(mean(pow_signal));
            if (ampvar > (double)300)
            {
                m_predictedClass[t] = (float)0;
                m_probaClass[t] = (float)inf;
                //std::cout<<"The signal no"<<t<<" has a too high amplitude."<<std::endl;
            }
            else
            {
                // Test of the amplitude between max and min of the signal
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                double min_signal = *std::min_element(signal.begin(),signal.end());
                double max_signal = *std::max_element(signal.begin(),signal.end());
                double dist_min_max = sqrt(pow((max_signal - min_signal),2));
                if (dist_min_max > (double)350)
                {
                    m_predictedClass[t] = (float)0;
                    m_probaClass[t] = (float)inf;
                    //std::cout<<"The signal no"<<t<<" has a too high amplitude."<<std::endl;
                }
            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }

        // Preprocess signal according to the value of m_predictedClass
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        if (m_predictedClass[t] == (float)0)
        {
            // Copy attributes in order to don't erase them
            // --------------------------------------------
            std::vector<float> copy_m_predictedClass = m_predictedClass;
            std::vector<float> copy_m_probaClass = m_probaClass;
            MBT_Matrix<float> copy_m_trainingFeaturesFirst(m_trainingFeatures.size().first, m_trainingFeatures.size().second);
            for (int n =0; n<copy_m_trainingFeaturesFirst.size().first; n++)
            {
                for (int n1 = 0; n1<copy_m_trainingFeaturesFirst.size().second;n1++)
                {
                    copy_m_trainingFeaturesFirst(n,n1)= m_trainingFeatures(n,n1);
                }
            }
            std::vector<float> copy_m_trainingClasses = m_trainingClasses;
            std::vector<float> copy_m_w = m_w;
            std::vector<float> copy_m_mu = m_mu;
            std::vector<float> copy_m_sigma = m_sigma;
            MBT_Matrix<float> copy_m_costClass(m_costClass.size().first, m_costClass.size().second);
            for (int n =0; n<copy_m_costClass.size().first; n++)
            {
                for (int n1 = 0; n1<copy_m_costClass.size().second;n1++)
                {
                    copy_m_costClass(n,n1)= m_costClass(n,n1);
                }
            }
            MBT_Matrix<float> copy_m_testFeatures(m_testFeatures.size().first, m_testFeatures.size().second);
            for (int n =0; n<copy_m_testFeatures.size().first; n++)
            {
                for (int n1 = 0; n1<copy_m_testFeatures.size().second;n1++)
                {
                    copy_m_testFeatures(n,n1)= m_testFeatures(n,n1);
                }
            }
            // ------------------------------------------------------------------------

            // Change the attributes to classify into 2 bad classes (bad EEG or no EEG signal)
            // -------------------------------------------------------------------------------
            m_trainingFeatures = m_trainingFeaturesBad;
            m_trainingClasses = m_trainingClassesBad;
            m_w = m_wBad;
            m_mu = m_muBad;
            m_sigma = m_sigmaBad;
            m_costClass = m_costClassBad;
            MBT_Matrix<float> tmp_m_testFeatures(1,m_testFeatures.size().second);
            for (int n1 = 0; n1<m_testFeatures.size().second;n1++)
            {
                tmp_m_testFeatures(0,n1)= m_testFeatures(t,n1);
            }
            m_testFeatures = tmp_m_testFeatures;
            // --------------------------------------------------------------------------------

            // Classify into 2 bad classes
            // ---------------------------
            MBT_knn(); // change m_probaClass and m_predictedClass
            copy_m_predictedClass[t] = m_predictedClass[0];
            copy_m_probaClass[t] = m_probaClass[0];
            // ---------------------------------------------------

            // Get the initial attributes
            // --------------------------
            m_predictedClass = copy_m_predictedClass;
            m_probaClass = copy_m_probaClass;
            m_trainingFeatures = copy_m_trainingFeaturesFirst;
            m_trainingClasses = copy_m_trainingClasses;
            m_w = copy_m_w;
            m_mu = copy_m_mu;
            m_sigma = copy_m_sigma;
            m_costClass = copy_m_costClass;
            m_testFeatures = copy_m_testFeatures;
            // --------------------------

            std::vector<double> signalRem = MBT_remove(signal);
            signal = signalRem;
            for (t1=0;t1<m_inputData.size().second;t1++) // we keep NaN values because the signal is bad
            {
                m_inputData(t,t1)=(float)signal[t1];
            }
            //std::cout<<"The signal no"<<t<<" has been removed."<<std::endl;
        }
        else if (m_predictedClass[t] == (float)0.5)
        {
            std::vector<float> float_signal(signal.begin(),signal.end());
            itak = MBT_itakuraDistance(float_signal);
            //std::cout<<"The itakura distance of signal no"<<t<<" is : "<<itak<<std::endl;
            if ((double)itak >= sid)
            {
                m_predictedClass[t] = (float)0.25; //<-- do that when we will use muscular artifacts
                //m_predictedClass[t] = (float)0.5;
                // Puis detrend et/ou bandpass % ########################
                //std::cout<<"The signal no"<<t<<" is a muscular artifact."<<std::endl;
            }

            /*else
            {
                //std::vector<float> signalCorr = MBT_correctArtifact(signal);
                //signal = signalCorr;
                //std::cout<<"The signal no"<<t<<" has been corrected."<<std::endl;
            }*/
            for (t1=0;t1<m_inputData.size().second;t1++) // we get the original signal, not the interpolated one
            {
                m_inputData(t,t1)=(float)signalInit[t1];
            }
        }
        else if (m_predictedClass[t] == (float)1)
        {
            // Puis detrend et/ou bandpass % ########################
            //std::cout<<"The signal no"<<t<<" is correct."<<std::endl;
            for (t1=0;t1<m_inputData.size().second;t1++) // we get the original signal, not the interpolated one
            {
                m_inputData(t,t1)=(float)signalInit[t1];
            }
        }
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        /*for (t1=0;t1<m_inputData.size().second;t1++)
        {
            m_inputData(t,t1)=(float)signal[t1];
        }*/
    }

    m_quality = m_predictedClass;

    /*for (int qua=0;qua<m_quality.size();qua++)
    {
        std::cout<<"The quality of signal no"<<qua<<" is : "<<m_quality[qua]<<std::endl;
        std::cout<<"The probability of being in such class is : "<<m_probaClass[qua]<<std::endl;
    }*/

}


float MBT_MainQC::MBT_itakuraDistance(std::vector<float> data)
{
    unsigned int i;

    // Calcul of pwelch of the signal
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // TO CHECK <------------------------------
    MBT_Matrix<double> signal = MBT_Matrix<double>(1, data.size());
    for (i = 0; i < data.size(); i++)
    {
        signal(0, i) = (double)data[i];
    }
    MBT_PWelchComputer pWelch = MBT_PWelchComputer(signal, (double)m_sampRate, "HAMMING");

    std::vector<double> f = pWelch.get_PSD(0);
    std::vector<double> pf2 = pWelch.get_PSD(1);
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /*for (int sp=0;sp<pf2.size();sp++)
    {
        std::cout<<pf2[sp]<<std::endl;
    }*/

    // Take values with frequency < 40Hz
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    std::vector<double> compare_40;
    compare_40.assign(1,40);

    int index40=0;

    for (i =0;i<f.size();i++)
    {
        if(f[i]<compare_40[0])
        {
            index40 = i;
        }
    }

    f.erase(f.begin()+index40+1,f.end());
    pf2.erase(pf2.begin()+index40+1,pf2.end());
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


    // Recovery of some variables useful for the Itakura distance calculation
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    std::vector<double> pf1(m_spectrumClean.begin(),m_spectrumClean.end());
    int p2 = pf1.size();
    int p1 = p2 - 1;
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


    // Calculation of Itakura distance
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    std::vector<double> r;
    r.assign(pf1.size(),0);
    std::vector<double> q;
    q.assign(pf1.size(),0);
    for (i=0; i<q.size();i++)
    {
        if (pf2[i] != 0)
        {
            r[i] = pf1[i]/pf2[i];
        }
        if (r[i] != 0)
        {
            q[i] = log(r[i]);
        }
    }

    double i1 = std::accumulate(r.begin()+1,r.begin()+p1,0.0);// (sum(r(:,2:p1),2)
    double i2 = (double)0.5*(r[0]+r[p2-1]); // 0.5*(r(:,1)+r(:,p2))
    double i3 = std::accumulate(q.begin()+1,q.begin()+p1,0.0); //sum(q(:,2:p1),2)
    double i4 = (double)0.5*(q[0]+q[p2-1]); // 0.5*(q(:,1)+q(:,p2))
    double itakuraDistance = 0;
    if ((i1+i2) != 0)
    {
        itakuraDistance = log((i1+i2)/(double)p1) - (i3+i4)/(double)p1;
    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    return (float)itakuraDistance;
}

// Method to compute a processed data for display purpose
// The given process is:
// Get the index where we have NaN
// Interpolate linearly the NaN
// Delete the NaN that can't be interpolated
// Do the processing: RemoveDC and BandPassFilter
// Put back the NaN to the processed data
MBT_Matrix<float> MBT_MainQC::MBT_compute_data_to_display(MBT_Matrix<float> const &inputData, float firstBound,
                                                          float secondBound) {
    std::vector<double> freqBounds;
    freqBounds.push_back((double)firstBound);
    freqBounds.push_back((double)secondBound);
    std::vector<std::vector<float>> dataToDisplay;

    // Interpolate the NaN
    for (int ch = 0; ch<inputData.size().first; ch++)
    {
        std::vector<float> tmpInputDataRow= inputData.row(ch); // get inputData for a specific channel
        std::vector<double> inputDataRow(tmpInputDataRow.begin(),tmpInputDataRow.end()); // transform in double

        std::vector<double> x, y, xInterp;
        std::vector<int> nanIndex;

        for (unsigned int tmp = 0 ; tmp < inputDataRow.size(); tmp++)
        {
            if (std::isnan(inputDataRow[tmp]) )
            {
                xInterp.push_back(tmp);
                nanIndex.push_back(tmp);
            }
            else
            {
                x.push_back(tmp);
                y.push_back(inputDataRow[tmp]);
            }
        }
        std::vector<double> InterpolatedData = MBT_linearInterp(x, y, xInterp);
        for (unsigned int d = 0; d<InterpolatedData.size(); d++)
        {
            inputDataRow[(unsigned int)xInterp[d]] = InterpolatedData[d];
        }

        // Remove the NaN that are not interpolated
        inputDataRow.erase(std::remove_if(inputDataRow.begin(), inputDataRow.end(),[](double testNaN){return std::isnan(testNaN);}),inputDataRow.end()); // remove NaN values

        // Process the data
        inputDataRow = RemoveDC(inputDataRow);
        inputDataRow = BandPassFilter(inputDataRow, freqBounds);

        // Put back the NaN in the processed data
        std::vector<float> processedData;
        std::vector<double>::iterator iteratorInputData;
        iteratorInputData = inputDataRow.begin();
        for (int tmp = 0; tmp<tmpInputDataRow.size(); tmp++){
            if(std::find(nanIndex.begin(), nanIndex.end(), tmp) != nanIndex.end()) {
                processedData.push_back(NAN);
            } else {

                processedData.push_back((float)*iteratorInputData);
                iteratorInputData++;
            }
        }
        dataToDisplay.push_back(processedData);
    }
    MBT_Matrix<float> matrixToDisplay = MBT_Matrix<float>(inputData.size().first, inputData.size().second, dataToDisplay);

    return matrixToDisplay;
}


// Destructor
MBT_MainQC::~MBT_MainQC()
{

}



// Methods to get attributes
MBT_Matrix<float> MBT_MainQC::MBT_get_m_trainingFeatures()
{
    return m_trainingFeatures;
}

std::vector<float> MBT_MainQC::MBT_get_m_trainingClasses()
{
    return m_trainingClasses;
}

std::vector<float> MBT_MainQC::MBT_get_m_w()
{
    return m_w;
}

std::vector<float> MBT_MainQC::MBT_get_m_mu()
{
    return m_mu;
}

std::vector<float> MBT_MainQC::MBT_get_m_sigma()
{
    return m_sigma;
}

int MBT_MainQC::MBT_get_m_kppv()
{
    return m_kppv;
}

MBT_Matrix<float> MBT_MainQC::MBT_get_m_costClass()
{
    return m_costClass;
}

MBT_Matrix<float> MBT_MainQC::MBT_get_m_trainingFeaturesBad()
{
    return m_trainingFeaturesBad;
}

std::vector<float> MBT_MainQC::MBT_get_m_trainingClassesBad()
{
    return m_trainingClassesBad;
}

std::vector<float> MBT_MainQC::MBT_get_m_wBad()
{
    return m_wBad;
}

std::vector<float> MBT_MainQC::MBT_get_m_muBad()
{
    return m_muBad;
}

std::vector<float> MBT_MainQC::MBT_get_m_sigmaBad()
{
    return m_sigmaBad;
}

MBT_Matrix<float> MBT_MainQC::MBT_get_m_costClassBad()
{
    return m_costClassBad;
}

std::vector< std::vector<float> > MBT_MainQC::MBT_get_m_potTrainingFeatures()
{
    return m_potTrainingFeatures;
}

std::vector< std::vector<float> > MBT_MainQC::MBT_get_m_dataClean()
{
    return m_dataClean;
}

std::vector<float> MBT_MainQC::MBT_get_m_spectrumClean()
{
    return m_spectrumClean;
}

std::vector<float> MBT_MainQC::MBT_get_m_cleanItakuraDistance()
{
    return m_cleanItakuraDistance;
}

float MBT_MainQC::MBT_get_m_accuracy()
{
    return m_accuracy;
}

MBT_Matrix<float> MBT_MainQC::MBT_get_m_inputData()
{
    return m_inputData;
}

MBT_Matrix<float> MBT_MainQC::MBT_get_m_testFeatures()
{
    return m_testFeatures;
}

std::vector<float> MBT_MainQC::MBT_get_m_probaClass()
{
    return m_probaClass;
}

std::vector<float> MBT_MainQC::MBT_get_m_predictedClass()
{
    return m_predictedClass;
}

std::vector<float> MBT_MainQC::MBT_get_m_quality()
{
    return m_quality;
}
