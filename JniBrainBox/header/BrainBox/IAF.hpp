#pragma once
#include "Matrix.hpp"
#include "Settings.hpp"

namespace brainbox {

template <typename T>
struct IAFResult {
    int nb_peak;
    std::vector<T> iaf;
    std::vector<T> qf;
    std::vector<T> snr;
    std::vector<T> noisepow;
    std::vector<T> alphapow;
    std::vector<T> hist_freq;
};

template <typename T>
IAFResult<T> compute_iaf(const Matrix<T>& signal, const Settings<T>& settings, const std::vector<T> &histFreq = std::vector<T>{});

}  // namespace brainbox

