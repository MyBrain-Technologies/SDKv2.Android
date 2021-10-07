/**
 * @brief Pondered-Knn classifier and related helpers
 * @copyright Copyright (c) 2021 myBrain Technologies. All rights reserved.
 *
 */

#pragma once
#include "Global.hpp"
#include "Matrix.hpp"
#include "TrainingData.hpp"
#include <unordered_map>
#include <tuple>

namespace brainbox::classifier {

template <typename T>
using Prediction_Type = std::unordered_map<T, std::vector<T>>;

/**
 * @brief Normalization of training dataset
 * 
 * @return Matrix<T> Normalized training dataset
 */
template <typename T>
Matrix<T> normalize_training_dataset(const Matrix<T>& m_trainingFeatures, const std::vector<T>& m_sigma, const std::vector<T>& m_mu);

/**
 * @brief Find distances between features
 * 
 * @param t Current feature row
 * @return std::vector<T> Distances between features and their normalizations
 */
template <typename T>
std::vector<T> find_distance(const Matrix<T>& test_features, const Matrix<T>& m_trainingFeatures, const std::vector<T>& m_sigma, const std::vector<T>& m_mu, unsigned int t);

/**
 * @brief Sort distances and find indexes
 * 
 * @param distanceNeighbor Distances between features and their normalizations
 * @param sortDistanceNeighbor Sorted distance without duplicate value
 * @param indiceNeighbor Indexes of the neighbors
 */
template <typename T>
void sort_distance_and_find_indexes(const std::vector<T>& distanceNeighbor, std::vector<T>& sortDistanceNeighbor, std::vector<int>& indiceNeighbor, const int m_kppv);

/**
 * @brief Compute probability class
 * 
 * @param typeClasses Type of classes
 * @param sortDistanceNeighbor Sorted distance without duplicate value
 * @param indiceNeighbor Indexes of the neighbors
 * @param minDist Normalization of distances
 * @return std::vector<T> Probability classes
 */
template <typename T>
std::vector<T> compute_proba_class(const std::vector<T>& typeClasses, const std::vector<T>& sortDistanceNeighbor, const std::vector<int>& indiceNeighbor, T minDist, const std::vector<T>& m_trainingClasses, const std::vector<T>& m_w);

/**
 * @brief Predicting class labels and affecting results to members
 * 
 * @param typeClasses Type of classes
 * @param tmpProbaClass Probability classes
 * @param t Current feature row
 */
template <typename T>
std::pair<T, T> predict_class_label(const Matrix<T>& m_costClass, const std::vector<T>& typeClasses, const std::vector<T>& tmpProbaClass);

/**
 * @brief K-nearest neighbors classifier 
 */
template <typename T>
std::tuple<std::vector<T>, std::vector<T>, Prediction_Type<T>> knn(Matrix<T> const& testFeature, const TrainingData<T>& training, unsigned int kppv);

}