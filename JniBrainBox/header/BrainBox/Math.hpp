#pragma once
#include "Global.hpp"
#include "Settings.hpp"
#include <numeric>
#include <type_traits>

namespace brainbox::math {

/**
 * @brief Check wether a signal contains only NaN values
 * 
 * @param input 
 * @return true 
 * @return false 
 */

template <typename T>
bool has_only_nan(const std::vector<T>& input);

/**
 * @brief Check wether a signal contains NaN values
 * 
 * @param input 
 * @return true 
 * @return false 
 */
template <typename T>
bool has_nan(const std::vector<T>& input);

/**
 * @brief Prepare a signal for features computations by powing it to 10^6
 * 
 * @param inputSignal 
 * @param settings
 * @return std::vector<T> 
 */
template <typename T>
std::vector<T> prepare_signal_for_features_computations(const std::vector<T>& inputSignal, const Settings<T>& settings);

/**
 * @brief Compute the power of two of a signal, without DC
 * 
 * @param input
 * @return std::vector<T>
 */
template <typename T>
std::vector<T> power_of_two_without_dc(const std::vector<T>& input);

/**
 * @brief Compute the sum of absolute values of a vector of double
 * 
 * @param input
 * @return T
 */
template <typename T>
T absolute_sum(const std::vector<T>& input);

/**
 * @brief Compute simple square integral of a signal
 * 
 * @param input 
 * @return T 
 */
template <typename T>
T simple_square_integral(const std::vector<T>& input);

/**
 * @brief Compute V2-order of a signal
 * 
 * @param input 
 * @return T 
 */
template <typename T>
T compute_v2(const std::vector<T>& input);

/**
 * @brief Compute V3-order of a signal
 * 
 * @param input 
 * @return T 
 */
template <typename T>
T compute_v3(const std::vector<T>& input);

/**
 * @brief Compute log detector of a signal
 * 
 * @param input 
 * @return T 
 */
template <typename T>
T compute_log_detector(const std::vector<T>& input);

/**
 * @brief Compute average amplitude change of a signal
 * 
 * @param input 
 * @return T 
 */
template <typename T>
T average_amplitude_change(const std::vector<T>& input);

/**
 * @brief Compute the difference absolute standard deviation of a signal
 * 
 * @param input 
 * @return T 
 */
template <typename T>
T difference_absolute_standard_deviation(const std::vector<T>& input);

/**
 * @brief Compute first derivative of a signal
 * @note To compute the derivate of (e.g.) sin(x):
 * auto step = 0.01
 * for (auto i = 0; i < data.size(); ++i)
 *    data[i] = sin(i*step)
 * time_derivative(data, 1/step);
 * 
 * To compute the derivate of a signal in Hz (e.g) sampRate = 250.0:
 * time_derivative(data, sampRate)
 * 
 * @param input signal
 * @param sampRate in Hz
 * @return std::vector<T> 
 */
template <typename T>
std::vector<T> time_derivative(const std::vector<T>& input, T samp_rate);

/**
 * @brief Compute occurences of min/max values from derivative
 * A min/max counter is added everytime the derivative is < 0.01
 * 
 * @param derivativeInput 
 * @return int 
 */
template <typename T>
int nb_max_min_from_time_derivative(const std::vector<T>& derivativeInput, const T approximate_gradient_solution_threshold=0.01);

/**
 * @brief Compute mobility of a signal from it's first derivative
 * 
 * @param input 
 * @param derivativeInput 
 * @return T 
 */
template <typename T>
T compute_mobility_from_time_derivative(const std::vector<T>& input, const std::vector<T>& derivativeInput);

/**
 * @brief Compute complexity of a signal
 * 
 * @param derivativeInput 
 * @param secondDerivativeInput 
 * @param mobility 
 * @return T 
 */
template <typename T>
T compute_complexity(const std::vector<T>& derivativeInput, const std::vector<T>& secondDerivativeInput, T mobility);

/**
 * @brief Compute zero crossing rate of a signal
 * 
 * @param input 
 * @return T 
 */
template <typename T>
T zero_crossing_rate(const std::vector<T>& input);

/**
 * @brief Compute non linear energy of a signal
 * 
 * @param input 
 * @return std::vector<T> 
 */
template <typename T>
std::vector<T> non_linear_energy(const std::vector<T>& input);

template <typename T> std::vector<T> get_theta_band(const std::vector<T>& input, const Settings<T>& settings, const std::vector<T>& freqBounds={ 4.0,    8.0});
template <typename T> std::vector<T> get_delta_band(const std::vector<T>& input, const Settings<T>& settings, const std::vector<T>& freqBounds={ 0.5,    4.0});
template <typename T> std::vector<T> get_alpha_band(const std::vector<T>& input, const Settings<T>& settings, const std::vector<T>& freqBounds={ 8.0,   13.0});
template <typename T> std::vector<T> get_beta_band (const std::vector<T>& input, const Settings<T>& settings, const std::vector<T>& freqBounds={ 13.0,  28.0});
template <typename T> std::vector<T> get_gamma_band(const std::vector<T>& input, const Settings<T>& settings, const std::vector<T>& freqBounds={ 28.0, 110.0});

/**
 * @brief Perform zero padding on a signal
 * 
 * @param input Input signal
 * @param int New size of the signal
 * @param int Computed number of zero to add to the signal
 * @param int Number of zero added to the signal
 */
template <typename T>
void zero_padding(std::vector<T>& input, unsigned int& N, unsigned int& nfft, unsigned int& nb_zero_added);

/**
 * @brief Compute one sided spectrum
 * 
 * @param input input signal
 * @param nfft Computed number of zero to add to the signal
 * @param sampRate Sample rate of the signal
 * @param EEG_power 
 * @param freqVector
 */
template <typename T>
T one_sided_spectrum(const std::vector<T>& input, unsigned int nfft, T sampRate, std::vector<T>& EEG_power, std::vector<T>& freqVector);

/**
 * @brief Compute delta ratio of a signal
 * 
 * @param freqVector 
 * @param EEG_power 
 * @param AUC_EEG_power 
 * @param delta_power 
 * @return T 
 */
template <typename T>
T delta_ratio(std::vector<T> const& freqVector, std::vector<T> const& EEG_power, T AUC_EEG_power, std::vector<T>& power, const std::vector<T> freqBounds={0.5, 4.0});

/**
 * @brief Compute theta ratio of a signal
 * 
 * @param freqVector 
 * @param EEG_power 
 * @param AUC_EEG_power 
 * @param theta_power 
 * @return T 
 */
template <typename T>
T theta_ratio(std::vector<T> const& freqVector, std::vector<T> const& EEG_power, T AUC_EEG_power, std::vector<T>& power, const std::vector<T> freqBounds={0.5, 4.0});

/**
 * @brief Compute alpha ratio of a signal
 * 
 * @param freqVector 
 * @param EEG_power 
 * @param AUC_EEG_power 
 * @param alpha_power 
 * @return T 
 */
template <typename T>
T alpha_ratio(std::vector<T> const& freqVector, std::vector<T> const& EEG_power, T AUC_EEG_power, std::vector<T>& power, const std::vector<T> freqBounds={0.5, 4.0});

/**
 * @brief Compute beta ratio of a signal
 * 
 * @param freqVector 
 * @param EEG_power 
 * @param AUC_EEG_power 
 * @param beta_power 
 * @return T 
 */
template <typename T>
T beta_ratio(std::vector<T> const& freqVector, std::vector<T> const& EEG_power, T AUC_EEG_power, std::vector<T>& power, const std::vector<T> freqBounds={0.5, 4.0});

/**
 * @brief Compute gamma ratio of a signal
 * 
 * @param freqVector 
 * @param EEG_power 
 * @param AUC_EEG_power 
 * @param gamma_power 
 * @return T 
 */
template <typename T>
T gamma_ratio(std::vector<T> const& freqVector, std::vector<T> const& EEG_power, T AUC_EEG_power, std::vector<T>& power, const std::vector<T> freqBounds={0.5, 4.0});

/**
 * @brief Compute band power of a signal
 * 
 * @param band_power 
 * @return T 
 */
template <typename T>
T band_pow(const std::vector<T>& band_power);

/**
 * @brief Compute logarithmic band power of a signal
 * 
 * @param band_pow 
 * @return T 
 */
template <typename T>
T logband_pow(T const &band_pow);

/**
 * @brief Compute normalized band power of a signal
 * 
 * @param band_pow 
 * @param ttp 
 * @return T 
 */
template <typename T>
T normband_pow(T const& band_pow, T const& ttp);

/**
 * @brief Compute normalize EEG power
 * 
 * @param EEG_power 
 * @param sum_EEG_power 
 * @return std::vector<T> 
 */
template <typename T>
std::vector<T> compute_eeg_power_norm(std::vector<T> const& EEG_power, T sum_EEG_power);

/**
 * @brief Compute cumulative EEG power ?
 * 
 * @param EEG_power_norm 
 * @return std::vector<T> 
 */
template <typename T>
std::vector<T> compute_eeg_power_cum(std::vector<T> const& EEG_power_norm);

/**
 * @brief Compute spectral edge frequency
 * 
 * @param freqVector 
 * @param EEG_power 
 * @param EEG_power_cum 
 * @param sum_EEG_power 
 * @param percentage 
 * @return T 
 */
template <typename T>
T spectral_edge_frequency(std::vector<T> const& freqVector, std::vector<T> const& EEG_power, std::vector<T> const& EEG_power_cum, T sum_EEG_power, T percentage);

/**
 * @brief Compute relative spectral difference between a signal's band and its neighbours
 * 
 * @param left_pow Left band power
 * @param current_pow Current band power
 * @param right_pow Right band power
 * @return T 
 */
template <typename T>
T relative_spectral_difference(T left_pow, T current_pow, T right_pow);

/**
 * @brief Compute signal to noise ratio (SNR) of a signal
 * 
 * @param freqVector 
 * @param EEG_power 
 * @param ttp 
 * @return T 
 */
template <typename T>
T signal_to_noise_ratio(std::vector<T> const& freqVector, std::vector<T> const& EEG_power, T const& ttp, T spectrum_frequency_noisy_threshold=30.0);

/**
 * @brief Compute power spectrum moment
 * 
 * @param EEG_power 
 * @return T 
 */
template <typename T>
T power_spectrum_moment(std::vector<T> const& EEG_power, std::vector<T> const& freqVector, int power);

/**
 * @brief Compute power spectrum center frequency
 * 
 * @param m0 
 * @param m1 
 * @return T 
 */
template <typename T>
T power_spectrum_center_freq(T m0, T m1);

/**
 * @brief Compute spectral rms
 * 
 * @param m0 
 * @param int 
 * @param int 
 * @return T 
 */
template <typename T>
T spectral_rms(T m0, unsigned int& N, unsigned int& nb_zero_added);

/**
 * @brief Compute index of spectral deformation
 * 
 * @param m0 
 * @param m1 
 * @param m2 
 * @param center_freq 
 * @return T 
 */
template <typename T>
T spectral_deformation_index(T m0, T m1, T m2, T center_freq);

/**
 * @brief Compute modified median frequency
 * 
 * @param freqVector 
 * @param EEG_power 
 * @param sampRate 
 * @return T The value of the smallest found index
 */
template <typename T>
T modified_median_frequency(std::vector<T> const& freqVector, std::vector<T> const& EEG_power, T sampRate);

/**
 * @brief Compute modified mean frequency
 * 
 * @param freqVector 
 * @param EEG_power 
 * @param sum_EEG_power 
 * @return T 
 */
template <typename T>
T modified_mean_frequency(std::vector<T> const& freqVector, std::vector<T> const& EEG_power, T sum_EEG_power);

/**
 * @brief Compute spectral entropy
 * 
 * @param freqVector 
 * @param EEG_power 
 * @return T 
 */
template <typename T>
T spectral_entropy(std::vector<T> const& freqVector, std::vector<T> const& EEG_power);

/**
 * @brief Compute the derivative of a vector
 * @param The input vector
 * @return The derivative vector
 */
template <typename T>
std::vector<T> derivative(std::vector<T> const& input);

/**
 * @brief Compute the median of a vector of double.
 * @param The input vector of double.
 * @return The median value.
 */
template <typename T>
T median(std::vector<T> const& input);

/**
 * @brief Compute the mean of a vector of double.
 * @param The input vector of double.
 * @return The mean value.
 */
template <typename T>
T mean(std::vector<T> const& input);

/**
 * @brief Compute the mean of a vector of double with removed NaN values.
 * @param The input vector of double.
 * @return The mean value without considering NaN values.
 */
template <typename T>
T nanmean(std::vector<T> const& input);

/**
 * @brief Compute the variance of a vector of double.
 * @param The input vector of double.
 * @return The variance value.
 */
template <typename T>
T var(std::vector<T> const& input);

/**
 * @brief Compute the variance of a vector of double.
 * @param The input vector of double.
 * @param The mean value of the input vector.
 * @return The variance value.
 */
template <typename T>
T var(std::vector<T> const& input, const T average);

/**
 * @brief Compute the standard deviation of a vector of double.
 * @param The input vector of double.
 * @return The standard deviation value.
 */
template <typename T>
T standard_deviation(std::vector<T> const& input);

/**
 * @brief Compute the standard deviation of a vector of double.
 * @param The input vector of double.
 * @param The mean value of the input vector.
 * @return The standard deviation value.
 */
template <typename T>
T standard_deviation(std::vector<T> const& input, const T average);


/**
 * @brief Compute the skewness of a vector of double.
 * @param The input vector of double.
 * @return The skewness value.
 */
template <typename T>
T skewness(std::vector<T> const& input);

/**
 * @brief Compute the kutosis of a vector of double.
 * @param The input vector of double.
 * @return The kurtosis value.
 */
template <typename T>
T kurtosis(std::vector<T> const& input);


/**
 * @brief Compute the approximate integral of inputY via the trapezoidal method with unit spacing determine by the size of inputX.
 * @param inputX The vector of double which allows to compute the integral of inputY.
 * @param inputY The input vector of double on which the integral will be computed.
 * @return The approximate integral of inputY.
 */
template <typename T>
T trapz(std::vector<T> const& inputX, std::vector<T> const& inputY);


    //Find the index of the closest value to a reference value in a double vector.
template <typename T>
int find_closest_index(std::vector<T> const& inputReference, const T valueToFind);
    
    //Find the indices of the closest values to two reference values in a double vector.
template <typename T>
std::pair<int, int> find_closest_indices(std::vector<T> const& inputReference, const T startValueToFind, const T endValueToFind);

/**
 * @brief Linear interpolation in a double vector.
 * @param x A vector of double containing the indexes of the known values.
 * @param y A vector of double containing the known values.
 * @param xInterp A vector of double containing the indexes of the values to ne interpolated.
 * @return A vector of double with the interpolated values.
 */
template <typename T>
std::vector<T> linear_interp(std::vector<T> &x, std::vector<T> &y, std::vector<T> &xInterp);

/**
 * @brief Linear interpolation of NaN values of a vector
 * @param signal_ch The vector to replace NaN values by interpolated ones
 */
template <typename T>
void linear_interpolation_of_nan(std::vector<T>& signal_ch);

/**
 * @brief Compute squared sum of a signal
 * 
 * @param signal A signal
 * @return std::pair<T, T> Squared sum of the signal samples and number of NaN samples
 */
template <typename T>
std::pair<T, T> squared_sum(const std::vector<T>& signal);

/**
 * @brief Compute rms of a signal, if freqBounds are specified, compute it after filtering
 * 
 * @param signal A signal
 * @param freqBounds Frequency bounds to apply for filtering
 * @return T rms value of the signal
 */
template <typename T>
T root_mean_square(const std::vector<T>& signal, const Settings<T>& settings, const std::vector<T>& freqBounds = std::vector<T>{});

template <typename T>
T apen(const std::vector<T> &x, unsigned int m, T r, T std);
template <typename T>
T smen(const std::vector<T> &x, unsigned int m, T r, T std);



} // namespace brainbox::math