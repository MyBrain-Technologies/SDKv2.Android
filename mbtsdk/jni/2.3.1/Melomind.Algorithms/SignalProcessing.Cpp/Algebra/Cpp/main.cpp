#include <iostream>
#include <stdio.h>
#include <vector>
#include <map>
#include <algorithm> // std::min_element
#include <iterator>  // std::begin, std::end
#include <string>
#include <numeric>
#include <math.h>
#include "../../DataManipulation/Headers/MBT_ReadInputOrWriteOutput.h"
#include "../../Algebra/Headers/MBT_Operations.h"
#include "../../Algebra/Headers/MBT_FindClosest.h"
#include "../../PreProcessing/Headers/MBT_BandPass.h"
#include "../../Transformations/Headers/MBT_Fourier.h"

using namespace std;

int main()
{

    MBT_Matrix<float> inputData = MBT_readMatrix("C:/Users/Fanny/Documents/SignalProcessing.Cpp/Algebra/TestFiles/inputDataForFeatures.txt");
    MBT_Matrix<float> testFeatures(inputData.size().first, 150);

    for (int ch=0;ch<inputData.size().first;ch++)
    {
        int sampRate = 250;
        int f = 0; // index of the feature in m_testFeatures(ch,.)
        std::vector<float> signal = inputData.row(ch);

        // compute each features for each channel and add 1 to f

        // TIME DOMAIN
        // ===========
        // Median
        float mediane = median(signal);
        testFeatures(ch,f) = mediane;
        f = f+1;

        // Mean
        float moyenne = mean(signal);
        testFeatures(ch,f) = moyenne;
        f = f+1;

        // Variance
        float variance = var(signal);
        testFeatures(ch,f) = variance;
        f = f+1;

        // VRMS
        std::vector<float> PowerOfTwoSignal;
        PowerOfTwoSignal.assign(signal.size(),0);
        for (int i = 0; i<signal.size(); i++)
        {
            PowerOfTwoSignal[i] = signal[i]*signal[i];
        }
        float vrms = sqrt(mean(PowerOfTwoSignal));
        testFeatures(ch,f) = vrms;
        f = f+1;

        // Vpp
        float vpp = 2 * sqrt(2) * vrms;
        testFeatures(ch,f) = vpp;
        f = f+1;

        // Skewness
        float S = skewness(signal);
        testFeatures(ch,f) = S;
        f = f+1;

        // Kurtosis
        float K = kurtosis(signal);
        testFeatures(ch,f) = K;
        f = f+1;

        // Integrated EEG
        float ieeg = 0;
        for (int i = 0; i <signal.size(); i++)
        {
            ieeg = ieeg + abs(signal[i]);
        }
        testFeatures(ch,f) = ieeg;
        f = f+1;

        // Mean absolute value
        float mav = ieeg/signal.size();
        testFeatures(ch,f) = mav;
        f = f+1;

        // Simple square integral
        float ssi = 0;
        for (int i = 0; i <signal.size(); i++)
        {
            ssi = ssi + pow(abs(signal[i]),2);
        }
        testFeatures(ch,f) = ssi;
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
        testFeatures(ch,f) = v2;
        f = f+1;
        float oneThird = float(1)/float(3);
        v3 = pow(v3/signal.size(),oneThird);
        testFeatures(ch,f) = v3;
        f = f+1;

        // Log detector
        float log_detector = 0;
        for (int i = 0; i <signal.size(); i++)
        {
            log_detector = log_detector + log10(abs(signal[i]));
        }
        log_detector = exp(log_detector/signal.size());
        testFeatures(ch,f) = log_detector;
        f = f+1;

        // Average amplitude change
        float aac = 0;
        for (int i = 0; i< signal.size()-1; i++)
        {
            aac = aac + abs(signal[i+1]-signal[i]);
        }
        aac = aac/signal.size();
        testFeatures(ch,f) = aac;
        f = f+1;

        // Difference absolute standard deviation value
        float dasdv = 0;
        for (int i = 0; i< signal.size()-1; i++)
        {
            dasdv = dasdv + pow((signal[i+1]-signal[i]),2);
        }
        dasdv = sqrt(dasdv/(signal.size()-1));
        testFeatures(ch,f) = dasdv;
        f = f+1;

        // Number of maxima and minima
        std::vector<float> time_point, diff_time_point, good_ind, time_point_tmp, X_tmp;
        time_point.assign(signal.size()+1,0);
        diff_time_point.assign(signal.size(),0);

        for (int i = 0; i<signal.size()+1; i++)
        {
            time_point[i] = float(i)/float(sampRate);
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
        testFeatures(ch,f) = nb_max_min;
        f = f+1;

        // 2nd Hjorth parameter = mobility
        float mobility = standardDeviation(deriv_X)/standardDeviation(signal);
        testFeatures(ch,f) = mobility;
        f = f+1;

        // 3rd Hjorth parameter = complexity
        std::vector<float> time_point_two, diff_time_point_two, good_ind_two, time_point_tmp_two, deriv_X_two_tmp;
        time_point_two.assign(signal.size(),0);
        diff_time_point_two.assign(signal.size(),0);

        for (int i = 0; i<signal.size(); i++)
        {
            time_point_two[i] = float(i)/float(sampRate);
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
        testFeatures(ch,f) = complexity;
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

        testFeatures(ch,f) = zrc;
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

        testFeatures(ch,f) = zrc_deriv_one;
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

        testFeatures(ch,f) = zrc_deriv_two;
        f = f+1;


        // Variance of 1st derivative
        float var_deriv_one = var(deriv_X);
        testFeatures(ch,f) = var_deriv_one;
        f = f+1;


        // Variance of 2nd derivative
        float var_deriv_two = var(deriv_X_two);
        testFeatures(ch,f) = var_deriv_two;
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
            testFeatures(ch,f) = var_deriv_two;
            f = f+1;
        end;*/


        // Non linear energy
        std::vector<float> nle;
        nle.assign(signal.size()-2,0);
        for (int i = 1; i<signal.size()-1; i++)
        {
            nle[i-1] = pow(signal[i],2) - signal[i-1]*signal[i+1];
        }
        float nle_mean = mean(nle);
        testFeatures(ch,f) = nle_mean;
        f = f+1;


        //Maximum, kurtosis, std and skewness of the data of each frequency bands
        std::vector<float> freqBounds;
        freqBounds.assign(2,0);
        freqBounds[0] = 0.5;
        freqBounds[1] = 4.0;
        std::vector<float> signalDelta = BandPassFilter(signal,freqBounds);
        freqBounds[0] = 4.0;
        freqBounds[1] = 8.0;
        std::vector<float> signalTheta = BandPassFilter(signal, freqBounds);
        freqBounds[0] = 8.0;
        freqBounds[1] = 13.0;
        std::vector<float> signalAlpha = BandPassFilter(signal, freqBounds);
        freqBounds[0] = 13.0;
        freqBounds[1] = 28.0;
        std::vector<float> signalBeta = BandPassFilter(signal, freqBounds);
        freqBounds[0] = 28.0;
        freqBounds[1] = 110.0;
        std::vector<float> signalGamma = BandPassFilter(signal, freqBounds);
        float ampDelta_sig = *std::max_element(signalDelta.begin(), signalDelta.end()+1);
        float kurtDelta_sig = kurtosis(signalDelta);
        float stdDelta_sig = standardDeviation(signalDelta);
        float skewDelta_sig = skewness(signalDelta);
        testFeatures(ch,f) = ampDelta_sig;
        f = f+1;
        testFeatures(ch,f) = kurtDelta_sig;
        f = f+1;
        testFeatures(ch,f) = stdDelta_sig;
        f = f+1;
        testFeatures(ch,f) = skewDelta_sig;
        f = f+1;
        float ampTheta_sig = *std::max_element(signalTheta.begin(), signalTheta.end()+1);
        float kurtTheta_sig = kurtosis(signalTheta);
        float stdTheta_sig = standardDeviation(signalTheta);
        float skewTheta_sig = skewness(signalTheta);
        testFeatures(ch,f) = ampTheta_sig;
        f = f+1;
        testFeatures(ch,f) = kurtTheta_sig;
        f = f+1;
        testFeatures(ch,f) = stdTheta_sig;
        f = f+1;
        testFeatures(ch,f) = skewTheta_sig;
        f = f+1;
        float ampAlpha_sig = *std::max_element(signalAlpha.begin(), signalAlpha.end()+1);
        float kurtAlpha_sig = kurtosis(signalAlpha);
        float stdAlpha_sig = standardDeviation(signalAlpha);
        float skewAlpha_sig = skewness(signalAlpha);
        testFeatures(ch,f) = ampAlpha_sig;
        f = f+1;
        testFeatures(ch,f) = kurtAlpha_sig;
        f = f+1;
        testFeatures(ch,f) = stdAlpha_sig;
        f = f+1;
        testFeatures(ch,f) = skewAlpha_sig;
        f = f+1;
        float ampBeta_sig = *std::max_element(signalBeta.begin(), signalBeta.end()+1);
        float kurtBeta_sig = kurtosis(signalBeta);
        float stdBeta_sig = standardDeviation(signalBeta);
        float skewBeta_sig = skewness(signalBeta);
        testFeatures(ch,f) = ampBeta_sig;
        f = f+1;
        testFeatures(ch,f) = kurtBeta_sig;
        f = f+1;
        testFeatures(ch,f) = stdBeta_sig;
        f = f+1;
        testFeatures(ch,f) = skewBeta_sig;
        f = f+1;
        float ampGamma_sig = *std::max_element(signalGamma.begin(), signalGamma.end()+1);
        float kurtGamma_sig = kurtosis(signalGamma);
        float stdGamma_sig = standardDeviation(signalGamma);
        float skewGamma_sig = skewness(signalGamma);
        testFeatures(ch,f) = ampGamma_sig;
        f = f+1;
        testFeatures(ch,f) = kurtGamma_sig;
        f = f+1;
        testFeatures(ch,f) = stdGamma_sig;
        f = f+1;
        testFeatures(ch,f) = skewGamma_sig;
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
        N = signal.size();
        // ------------

        // Total power
        std::vector<std::complex<float> > dataToTransform;
        dataToTransform.assign(signal.size(),0);
        for (int i = 0; i < signal.size(); i++)
        {
            dataToTransform[i] = signal[i];
        }
        MBT_Fourier::forwardBluesteinFFT(dataToTransform);
        std::vector<float> tmp_EEG_power;
        tmp_EEG_power.assign(nfft,0);
        std::vector<float> tmp_freqVector;
        tmp_freqVector.assign(nfft,0);
        for (int i = 0; i < nfft; i++)
        {
            tmp_EEG_power[i] = abs(dataToTransform[i]); //Retain Magnitude (imaginary part = phase)
            tmp_freqVector[i] = (float(i)*float(sampRate))/float(nfft);
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
        testFeatures(ch,f) = ttp;
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
        testFeatures(ch,f) = ratio_delta;
        f = f+1;


        // delta power
        float delta_pow = 0;
        for (int i = 0; i < delta_power.size(); i++)
        {
            delta_pow = delta_pow + delta_power[i]*delta_power[i];
        }
        delta_pow = delta_pow/pow(delta_power.size(),2);
        testFeatures(ch,f) = delta_pow;
        f = f+1;


        // log delta power
        float log_delta_pow = log10(delta_pow);
        testFeatures(ch,f) = log_delta_pow;
        f = f+1;


        // Normalized delta power
        float n_delta_pow = delta_pow/ttp; // division of energy
        testFeatures(ch,f) = n_delta_pow;
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
        testFeatures(ch,f) = ratio_theta;
        f = f+1;


        // theta power
        float theta_pow = 0;
        for (int i = 0; i < theta_power.size(); i++)
        {
            theta_pow = theta_pow + theta_power[i]*theta_power[i];
        }
        theta_pow = theta_pow/pow(theta_power.size(),2);
        testFeatures(ch,f) = theta_pow;
        f = f+1;


        // log theta power
        float log_theta_pow = log10(theta_pow);
        testFeatures(ch,f) = log_theta_pow;
        f = f+1;


        // Normalized theta power
        float n_theta_pow = theta_pow/ttp; // division of energy
        testFeatures(ch,f) = n_theta_pow;
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
        testFeatures(ch,f) = ratio_alpha;
        f = f+1;


        // alpha power
        float alpha_pow = 0;
        for (int i = 0; i < alpha_power.size(); i++)
        {
            alpha_pow = alpha_pow + alpha_power[i]*alpha_power[i];
        }
        alpha_pow = alpha_pow/pow(alpha_power.size(),2);
        testFeatures(ch,f) = alpha_pow;
        f = f+1;


        // log alpha power
        float log_alpha_pow = log10(alpha_pow);
        testFeatures(ch,f) = log_alpha_pow;
        f = f+1;


        // Normalized alpha power
        float n_alpha_pow = alpha_pow/ttp; // division of energy
        testFeatures(ch,f) = n_alpha_pow;
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
        testFeatures(ch,f) = ratio_beta;
        f = f+1;


        // beta power
        float beta_pow = 0;
        for (int i = 0; i < beta_power.size(); i++)
        {
            beta_pow = beta_pow + beta_power[i]*beta_power[i];
        }
        beta_pow = beta_pow/pow(beta_power.size(),2);
        testFeatures(ch,f) = beta_pow;
        f = f+1;


        // log beta power
        float log_beta_pow = log10(beta_pow);
        testFeatures(ch,f) = log_beta_pow;
        f = f+1;


        // Normalized beta power
        float n_beta_pow = beta_pow/ttp; // division of energy
        testFeatures(ch,f) = n_beta_pow;
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
        testFeatures(ch,f) = ratio_gamma;
        f = f+1;


        // gamma power
        float gamma_pow = 0;
        for (int i = 0; i < gamma_power.size(); i++)
        {
            gamma_pow = gamma_pow + gamma_power[i]*gamma_power[i];
        }
        gamma_pow = gamma_pow/pow(gamma_power.size(),2);
        testFeatures(ch,f) = gamma_pow;
        f = f+1;


        // log gamma power
        float log_gamma_pow = log10(gamma_pow);
        testFeatures(ch,f) = log_gamma_pow;
        f = f+1;


        // Normalized gamma power
        std::vector<float> n_gamma_power_tmp;
        float n_gamma_pow = gamma_pow/ttp; // division of energy
        testFeatures(ch,f) = n_gamma_pow;
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
        testFeatures(ch,f) = sef80;
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
        testFeatures(ch,f) = sef90;
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
        testFeatures(ch,f) = sef95;
        f = f+1;


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
        float ff_delta = (log_theta_pow - 0);
        testFeatures(ch,f) = ff_delta;
        f = f+1;

        float ff_theta = (log_alpha_pow - log_delta_pow);
        testFeatures(ch,f) = ff_theta;
        f = f+1;

        float ff_alpha = (log_beta_pow - log_theta_pow);
        testFeatures(ch,f) = ff_alpha;
        f = f+1;

        float ff_beta = (log_gamma_pow - log_alpha_pow);
        testFeatures(ch,f) = ff_beta;
        f = f+1;

        float ff_gamma = (0 - log_beta_pow);
        testFeatures(ch,f) = ff_gamma;
        f = f+1;


        // Relative spectral difference
        float rsd_delta = (theta_pow - 0)/(theta_pow + delta_pow + 0);
        testFeatures(ch,f) = rsd_delta;
        f = f+1;

        float rsd_theta = (alpha_pow - delta_pow)/(alpha_pow + theta_pow + delta_pow);
        testFeatures(ch,f) = rsd_theta;
        f = f+1;

        float rsd_alpha = (beta_pow - theta_pow)/(beta_pow + alpha_pow + theta_pow);
        testFeatures(ch,f) = rsd_alpha;
        f = f+1;

        float rsd_beta = (gamma_pow - alpha_pow)/(gamma_pow + beta_pow + alpha_pow);
        testFeatures(ch,f) = rsd_beta;
        f = f+1;

        float rsd_gamma = (0 - beta_pow)/(0 + gamma_pow + beta_pow);
        testFeatures(ch,f) = rsd_gamma;
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
        testFeatures(ch,f) = SN_ratio;
        f = f+1;


        // Power spectrum moments
        float m0 = 0;
        for (int i=0; i<EEG_power.size(); i++)
        {
            m0 = m0 + EEG_power[i]*EEG_power[i];
        }
        testFeatures(ch,f) = m0;
        f = f+1;

        float m1 = 0;
        for (int i=0; i<EEG_power.size(); i++)
        {
            m1 = m1 + EEG_power[i]*EEG_power[i]*freqVector[i];
        }
        testFeatures(ch,f) = m1;
        f = f+1;

        float m2 = 0;
        for (int i=0; i<EEG_power.size(); i++)
        {
            m2 = m2 + EEG_power[i]*EEG_power[i]*pow(freqVector[i],2);
        }
        testFeatures(ch,f) = m2;
        f = f+1;


        // Power spectrum center frequency
        float center_freq = m1/m0;
        testFeatures(ch,f) = center_freq;
        f = f+1;

        // Spectral RMS
        float sp_rms = sqrt(m0/(N-nb_zero_added));
        testFeatures(ch,f) = sp_rms;
        f = f+1;

        // Index of spectral deformation
        float omega_ratio = (sqrt(m2/m0))/center_freq;
        testFeatures(ch,f) = omega_ratio;
        f = f+1;


        // Modified Median Frequency
        std::vector<float> ecart;
        ecart.assign(floor(sampRate/2),0);
        for (int i = 0; i<floor(sampRate/2); i++)
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
        testFeatures(ch,f) = mod_median_freq;
        f = f+1;


        // Modified Mean Frequency
        float mod_mean_freq = 0.0;
        for (int i = 0; i<EEG_power.size(); i++)
        {
            mod_mean_freq = mod_mean_freq + freqVector[i]*EEG_power[i];
        }
        mod_mean_freq = mod_mean_freq/sum_EEG_power;
        testFeatures(ch,f) = mod_mean_freq;
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

    /*//Shannon Entropy
    ePDF = hist(PDF(:), length(f));% Compute N-bin histogram
    ePDF = ePDF / sum(ePDF(:));% Compute probabilities
    i = find(ePDF); % Make mask to eliminate 0's since log(0) = -inf.
    sh_h = -sum(ePDF(i) .* log(ePDF(i))); % Compute entropy*/

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
        testFeatures(ch,f) = s_h;
        f = f+1;


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







/*
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
        N = signal.size();

        std::vector<std::complex<float> > dataToTransform5;
        dataToTransform5.assign(signal.size(),0);
        for (int i = 0; i < signal.size(); i++)
        {
            dataToTransform5[i] = signal[i];
        }
        MBT_Fourier::forwardBluesteinFFT(dataToTransform5);
        std::vector<float> tmp_EEG_power5;
        tmp_EEG_power5.assign(nfft,0);
        for (int i = 0; i < nfft; i++)
        {
            tmp_EEG_power5[i] = abs(dataToTransform5[i]); //Retain Magnitude (imaginary part = phase)
        }

        // Generate the one-sided spectrum
        std::vector<float > EEG_power5;
        EEG_power5.assign(nfft/2,0);
        for (int i=0; i<nfft/2; i++)
        {
            EEG_power5[i] = tmp_EEG_power5[i];
        }


        std::vector<std::complex<float> > fft_EEG_power5;
        for (int ki=0;ki<EEG_power5.size();ki++)
        {
            fft_EEG_power5.push_back(std::complex<float>(EEG_power5[ki], 0));
        }
        MBT_writeVector(fft_EEG_power5, "C:/Users/Fanny/Documents/SignalProcessing.Cpp/Algebra/TestFiles/fft_EEG_power.txt");
*/
        //MBT_Matrix<float> fft_EEG_power8 = MBT_readMatrix("C:/Users/Fanny/Documents/SignalProcessing.Cpp/Algebra/TestFiles/fft_EEG_power.txt");
        //std::vector<float> fft_EEG_power5 = fft_EEG_power8.row(0);

        /*std::vector<std::complex<float> > dataToTransform6;
        dataToTransform6.assign(fft_EEG_power5.size(),0);
        for (int i = 0; i < fft_EEG_power5.size(); i++)
        {
            dataToTransform6[i] = fft_EEG_power5[i];
        }*/
/*
        MBT_Fourier::inverseBluesteinFFT(fft_EEG_power5);
        std::vector<float> tmp_EEG_power6;
        tmp_EEG_power6.assign(fft_EEG_power5.size(),0);
        for (int i = 0; i < fft_EEG_power5.size(); i++)
        {
            tmp_EEG_power6[i] = abs(fft_EEG_power5[i]); //Retain Magnitude (imaginary part = phase)
        }
        // Generate the one-sided spectrum
        std::vector<float> i_EEG_power5;
        i_EEG_power5.assign(nfft/2,0);
        for (int i=0; i<nfft/2; i++)
        {
            i_EEG_power5[i] = tmp_EEG_power6[i];
        }

        std::vector<std::complex<float> > ifft_EEG_power5;
        for (int ki=0;ki<i_EEG_power5.size();ki++)
        {
            ifft_EEG_power5.push_back(std::complex<float>(i_EEG_power5[ki], 0));
        }
        MBT_writeVector(ifft_EEG_power5, "C:/Users/Fanny/Documents/SignalProcessing.Cpp/Algebra/TestFiles/ifft_EEG_power.txt");
*/




    }
    MBT_writeMatrix (testFeatures, "C:/Users/Fanny/Documents/SignalProcessing.Cpp/Algebra/TestFiles/BuildComputationFeatures_Cplusplus.txt");

    return 0;
}
