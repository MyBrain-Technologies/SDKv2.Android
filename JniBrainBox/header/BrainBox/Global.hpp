#pragma once
#include <vector>
#include <complex>
#include <limits>
#include <string>

template <typename T>
static const T SP_INF_T() {
    static const T n = std::numeric_limits<T>::infinity();
    return n;
}    

template <typename T>
static const T SP_NAN_T() {
    static const T n = std::numeric_limits<T>::quiet_NaN();
    return n;
}    

template <typename T>
static const T SP_PI_T() {
    static const T pi = std::atan(static_cast<double>(1)) * 4;
    return pi;
}