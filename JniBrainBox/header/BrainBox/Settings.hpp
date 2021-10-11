#pragma once
#include <memory>
#include <unordered_map>
#include <optional>
#include <utility>
#include <thread>
#include "Global.hpp"

namespace brainbox {

template <typename T>
struct Settings {
    using CSPtr = std::shared_ptr<const Settings>;

    Settings() = default;
    Settings(Settings&&) = default;
    Settings(const Settings&) = default;

    static CSPtr GetCSharedPtr() {
        return std::make_shared<const Settings>();
    }

    // @brief define the signal in Hz frequency
    T sampling_rate{250.0};
    
    // @brief Number of closest nearest neighbours to get
    unsigned int kppv{19};
    
    // QualityChecker.hpp:
    bool use_bandpass_process{false};
    T bandpass_first_bound{2.0};
    T bandpass_second_bound{30.0};
    bool use_microvolts{true};

    // Math.hpp:
    T approximate_gradient_solution_threshold{0.01};
    std::vector<T> delta_freq_bounds{0.5,    4.0};
    std::vector<T> theta_freq_bounds{4.0,    8.0};
    std::vector<T> alpha_freq_bounds{8.0,   13.0};
    std::vector<T> beta_freq_bounds{13.0,   28.0};
    std::vector<T> gamma_freq_bounds{28.0, 110.0};
    T spectrum_frequency_noisy_threshold{30.0};

    // SignalProcessing.hpp:
    T low_pass_band_max_hz{40.0};
    T delta_signal_constant_threshold{0.5};
    T signal_constant_threshold{35.0};
    T vpp_threshold{300.0};
    T vpp_max_amplitude_threshold{350.0};

    // SignalProcessing::BandPass
    T sample_rate_norm{250.0/2.0};
    T highpass_min{2.0};
    T highpass_factor{2.0};
    T lowpass_min{10.0};
    T lowpass_factor{0.2};
    
    // SignalProcessing::fir2
    int fir_threshold{1024};
    int fir_size{512};
    int fir_lap_factor{25};

    // iaf
    T iaf_inf{6};
    T iaf_sup{13};
    T iaf_sliding_window_sec{8.0};
    int iaf_sliding_windows_in_second{8};
    T iaf_min_quality_per_packet{0.5};
    T iaf_general_quality_threshold{0.5};
    T iaf_channel_quality_threshold{0.5};

    // PWelch
    T pwelch_overlap{0.5};
    int pwelch_nbr_windows{8};
    int pwelch_max_power_of_two{256};
    std::string window_type{"HAMMING"};
    T pwelch_hamming_multi_factor{0.46};
    T pwelch_hamming_minus_factor{0.54};
    int pwelch_no_outliers_nbr_windows{128};
    T pwelch_no_outliers_overlap{64};
    int pwelch_no_outliers_padding{512};

    // rms
    T rms_lower_guard_bound_lower_peakband{3.0};
    T rms_upper_guard_bound_lower_peakband{6.5};
    T rms_lower_guard_bound_upper_peakband{13.5};
    T rms_upper_guard_bound_upper_peakband{18.5};
    T rms_min_factor{0.9};
    T rms_max_factor{1.5};

    // NFConfig
    int smoothing_duration{2};
    T buffer_size{1};

    // Noise
    int max_compute_noise_loop{49};

};

} // namespace brainbox