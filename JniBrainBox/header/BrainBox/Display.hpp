#pragma once
#include <algorithm>
#include <vector>
#include <unordered_map>
#include "Global.hpp"
#include "TrainingData.hpp"
#include "Settings.hpp"

namespace brainbox::display {

template <typename T>
std::vector<T> process_input_data_row(const std::vector<T>& tmpInputDataRow, const std::vector<T>& freqBounds, std::vector<T>& x, std::vector<T>& y, std::vector<T>& xInterp, const Settings<T>& settings);
template <typename T>
std::vector<T> process_data_for_display(const std::vector<T>& inputDataRow, const std::vector<T>& xInterp);
template <typename T>
Matrix<T> compute_data_to_display(Matrix<T> const &inputData, const Settings<T>& settings, T firstBound=2.0, T secondBound=30.0);

}  // namespace brainbox::display

