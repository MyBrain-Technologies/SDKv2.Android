#pragma once
#include <vector>
#include <unordered_map>
#include "Matrix.hpp"
#include "Math.hpp"
#include "PWelch.hpp"

namespace brainbox::signal {

/**
 * @brief Format data and intialize a PWelchComputer
 *
 * @param input 
 * @param sampRate 
 * @return PWelchComputer 
 */
template <typename T>
PWelchComputer<T> compute_pwelch(std::vector<T> const& input, const Settings<T>& settings);

/**
 * @brief Take values with frequency < 40Hz
 * 
 * @param f ordered frequency
 * @param pf2 related power signal
 */
template <typename T>
void low_pass_band_hz(std::vector<T>& f, std::vector<T>& pf2, const T max=40.0);

/**
 * @brief Compute Itakura distance
 * 
 * @param spectrum_clean 
 * @param pf2 
 * @return T 
 */
template <typename T>
T compute_itakura_distance(const std::vector<T>& spectrum_clean, const std::vector<T>& pf2);

/**
 * @brief Calculates the Itakura distance
 * between the averaged spectrum of clean data (quality = 1)
 * and the spectrum of each observation of EEG data (between 0 and 40Hz).
 * 
 * @param TODO
 * @return T 
 */
template <typename T>
T itakura_spectrum_distance(const std::vector<T>& data, const std::vector<T>& m_spectrumClean, const Settings<T>& settings, const T low_pass_band_max_hz=40.0);

    //Static method: Find the index of the closest value to a reference value in a double vector.
template <typename T>
int find_closest_index(std::vector<T> const& inputReference, const T valueToFind);

//Static method: Find the indices of the closest values to two reference values in a double vector.
template <typename T>
std::pair<int, int> find_closest_index(std::vector<T> const& inputReference, const T startValueToFind, const T endValueToFind);
template <typename T>
std::vector<T> linear_interpolation(std::vector<T> &x, std::vector<T> &y, std::vector<T> &xInterp);
template <typename T>
Matrix<T> interpolation_lost_data(const Matrix<T>& input_signal);

/**
 * @brief Convert signal to uV
 * 
 * @param inputDataRow Input data row, corresponding to a signal
 * @return std::vector<T> Signal converted to uV
 */
template <typename T>
std::vector<T> interpolate_signal_to_uv(const std::vector<T>& inputDataRow);

/**
 * @brief Check if signal is constant
 * 
 * @param signal Signal converted to uV
 * @return int Constance of the signal
 */
template <typename T>
int check_signal_constant(const std::vector<T>& signal, const T delta_signal_constant_threshold=0.5);

/**
 * @brief Test amplitude variation of a signal
 * 
 * @param signal Signal converted to uV
 * @param t Current signal index from input data
 */
// template <typename T>
// void testAmplitudeVariation(const std::vector<T>& signal, unsigned int t);
template <typename T>
bool is_amplitude_variation_high(const int m_inputDataSize, const std::vector<T>& signal, const T vpp_threshold=300.0, const T vpp_max_amplitude_threshold=350.0);

// Method which removes the DC offset
template <typename T>
std::vector<T> remove_dc(const std::vector<T>& Data);
// takes float in input and returns double in output
template <typename T>
std::vector<T> remove_dc_f2d(const std::vector<T>& Data);

template <typename T>
std::vector<T> band_pass_filter(const std::vector<T>& RawSignal, const brainbox::Settings<T>& settings, const std::vector<T>& freqBounds);

// this function mirrors the signal
template <typename T>
std::vector<T> mirror_signal(const std::vector<T>&);
// FIR arbitrary shape filter design using the frequency sampling method
template <typename T>
std::vector<T> fir2(int MirroredSize, std::vector<T> Params, std::vector<int> CutOnOff, const Settings<T>& settings);

// it creates a hamming window
template <typename T>
std::vector<T> hamming(int);

// FROM SignalPRocessing/Transformations
/*
 * @brief Find the indexes of the frequencies which are close to IAFinf and IAFsup.
 * @param frequencies A vector of double containing the values of the frequencies.
 * @param IAFinf The lower bound of the range of frequencies we want to study.
 * @param IAFsup The upper bound of the range of frequencies we want to study.
 * @return A pair of int which corresponds to the indexes of the frequency bounds.
 */
template <typename T>
std::pair<int,int> frequency_bounds(std::vector<T> frequencies, const T IAFinf, const T IAFsup);

/*
 * @brief Find the maximum value of the spectrum in a specific frequency range.
 * @param spectre A vector of double containing the values of the spectrum.
 * @param n_finf The index of the lower bound of the range of frequencies we want to study.
 * @param n_fsup The index of the upper bound of the range of frequencies we want to study.
 * @return The value of the peak in the spectrum in a frequency range specified by n_finf and n_fsup.
 */
 // Note: n_finf and n_fsup are the output of frequency_bounds.
template <typename T>
T val_max_peak(std::vector<T> spectre, const int n_finf, const int n_fsup);

/*
 * @brief Find the index of the maximum value of the spectrum in a specific frequency range.
 * @param spectre A vector of double containing the values of the spectrum.
 * @param n_finf The index of the lower bound of the range of frequencies we want to study.
 * @param n_fsup The index of the upper bound of the range of frequencies we want to study.
 * @return The index of the peak in the spectrum in a frequency range specified by n_finf and n_fsup.
 */
 // Note: n_finf and n_fsup are the output of frequency_bounds.
template <typename T>
int ind_max_peak(std::vector<T> spectre, const int n_finf, const int n_fsup);

/*
 * @brief Find the frequency of the peak into the spectrum in a specific frequency range.
 * @param frequencies A vector of double containing the values of the frequencies.
 * @param spectre A vector of double containing the values of the spectrum.
 * @param n_finf The index of the lower bound of the range of frequencies we want to study.
 * @param n_fsup The index of the upper bound of the range of frequencies we want to study.
 * @return The frequency of the peak into the spectrum in a frequency range specified by n_finf and n_fsup.
 */
 // Note: n_finf and n_fsup are the output of frequency_bounds.
 // WARNING : Maybe you need an int for the frequency. In this case, you should round the double value.
template <typename T>
T freq_max_peak(std::vector<T> frequencies, std::vector<T> spectre, const int n_finf, const int n_fsup);

template <typename T>
std::vector<T> calculate_bounds(std::vector<T> DataNoDC);

// Method that removes the outliers
template <typename T>
std::vector<T> remove_outliers(std::vector<T> DataNoDC, std::vector<T> Bounds);

/*
 * Interpolate the outliers detected by Bounds from DataNoDC
 * @params: DataNoDC Vector of double which holds the data
 * @params: Bounds Vector of double which holds the low bound and the up bound to detect outliers
 * @return: the signal with the interpolated outliers
*/
template <typename T>
std::vector<T> interpolate_outliers(std::vector<T> DataNoDC, std::vector<T> Bounds);

// Method that finds the quantiles
template <typename T>
std::vector<T> quantile(std::vector<T>& CalibNoDC);

// quantile's helping function
template <typename T>
T lerp(T v0, T v1, T t);

/**
 * @brief Find the peaks whose frequency is in the range ([mu_peakF-sigma_peakF : mu_peakF+sigma_peakF]) = condition D
 * 
 * @param zero_cross_freq 
 * @param mu_peakF 
 * @param sigma_peakF 
 * @param histFreq Vector containing the previous frequencies.
 * @return std::vector<int> Peaks indexes corresponding to the condition
 */
template <typename T>
std::vector<int> find_peaks_in_range(const std::vector<T>& zero_cross_freq, const T mu_peakF, const T sigma_peakF, std::vector<T> &histFreq, const int minimumFrequencyValuesHistory=10);

/**
 * @brief Compute the difference between the observed spectrum and the estimated one
 * 
 * @param logPSD Observed spectrum
 * @param noisePow Estimate spectrum
 * @return std::vector<T> Difference between both spectrum
 */
template <typename T>
std::vector<T> spectrum_difference(const std::vector<T>& logPSD, const std::vector<T>& noisePow);

/**
 * @brief Look for switch from positive to negative derivative values
 * 
 * @param trunc_frequencies 
 * @param d1 
 * @param difference Difference between observed and estimated spectrum
 * @param IAFinf Lower bound of the frequency range which will be used to compute iaf. For example IAFinf = 7.
 * @param IAFsup Upper bound of the frequency range which will be used to compute iaf. For example IAFsup = 13.
 * @param bin_index 
 * @param zero_cross_freq 
 * @param amp_difference 
 */
template <typename T>
void downward_zero_crossing(const std::vector<T>& trunc_frequencies, const std::vector<T>& d1, const std::vector<T>& difference, const T IAFinf, const T IAFsup,
            std::vector<int>& bin_index, std::vector<T>& zero_cross_freq, std::vector<T>& amp_difference);

} // namespace brainbox::signalprocessing