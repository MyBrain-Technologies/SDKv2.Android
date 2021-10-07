#pragma once
#include "Matrix.hpp"
#include "Settings.hpp"
#include "Calibration.hpp"

namespace brainbox {

template <typename T>
struct RelaxIndexOutputData {
    T volum;
    T alpha_power;
    T relative_alpha_power;
    std::vector<T> qualities;
};

template <typename T>
struct RelaxIndexSessionOutputData {
    T mean_alpha_power;
    T mean_relative_alpha_power;
    T confidence;
    std::vector<T> volums;
    std::vector<T> alpha_powers;
    std::vector<T> relative_alpha_powers;
    std::vector<T> qualities;
    std::vector<T> past_relax_index;
    std::vector<T> smoothed_relax_index;
};

template <typename T>
class RelaxIndexSession {
    struct Data;
    Data* data_;
    public:

    RelaxIndexSession(typename Settings<T>::CSPtr settings,
                      const CalibrationOutputData<T> calibration);

    RelaxIndexSession(typename Settings<T>::CSPtr settings,
                      const std::pair<T, T>& min_max,
                      const T iaf_median_inf,
                      const T iaf_median_sup);

    RelaxIndexSession(typename Settings<T>::CSPtr settings,
                      const std::vector<T>& smoothed_rms,
                      const T iaf_median_inf,
                      const T iaf_median_sup);

    RelaxIndexSession() = delete;
    RelaxIndexSession(RelaxIndexSession&&) = delete;
    RelaxIndexSession(const RelaxIndexSession&) = delete;

    ~RelaxIndexSession();

    RelaxIndexOutputData<T> compute(const Matrix<T>& signals, const std::vector<T>& qualities) noexcept(false);
    RelaxIndexSessionOutputData<T> end_session();
};

}  // namespace brainbox

