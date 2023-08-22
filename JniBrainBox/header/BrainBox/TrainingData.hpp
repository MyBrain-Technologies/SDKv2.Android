#pragma once
#include <memory>
#include <string>
#include "Global.hpp"
#include "Matrix.hpp"

namespace brainbox {
/**
 * @brief Compute average features values across training features data
 * 
 * @param featuresMatrix A matrix containing features (columns) for a dataset of signal (rows)
 * @return std::vector<T> Average features values accross training data
 */
template <typename T>
std::vector<T> averageFeatures(const Matrix<T>& featuresMatrix);

/**
 * @brief Compute standard deviation features values across training features data
 * 
 * @param featuresMatrix A matrix containing features (columns) for a dataset of signal (rows)
 * @return std::vector<T> Standard deviation of features values
 */
template <typename T>
std::vector<T> standard_deviationFeatures(const Matrix<T>& featuresMatrix);

/**
 * @brief Get the number Of unique values from a vector
 * 
 * @param vec The vector to analyze
 * @return unsigned int Number of unique occurences
 */
template <typename T>
unsigned int getNumberOfUniqueValues(const std::vector<T>& vec);

/**
 * @brief Build a costClass matrix from a trainingClasses vector
 * 
 * @param trainingClasses The vector of training classes
 * @return std::vector<T> The result costClass matrix
 */
template <typename T>
Matrix<T> buildCostClass(const std::vector<T>& trainingClasses);

/**
 * @brief Quality Checker training data container
 */
template <typename T>
class TrainingData
{
    public:
    using CSPtr = std::shared_ptr<const TrainingData>;
    /**
     * @brief Construct a new TrainingData object
     * @deprecated
     * 
     * @param trainingFeatures 
     * @param trainingClasses 
     * @param w 
     * @param mu 
     * @param sigma 
     * @param costClassSize 
     */
    TrainingData(const Matrix<T>& trainingFeatures, const std::vector<T>& trainingClasses,
                        const std::vector<T>& w, const std::vector<T>& mu,
                        const std::vector<T>& sigma, const unsigned int costClassSize);
    
    /**
     * @brief Construct a new TrainingData object
     * 
     * @param trainingFeatures 
     * @param trainingClasses 
     * @param wFile 
     */
    TrainingData(const Matrix<T>& trainingFeatures, const std::vector<T>& trainingClasses,
                        const std::vector<T>& wFile);


    const static CSPtr GetFrontalTraining();
    const static CSPtr GetFrontoparietalTraining();
    const static CSPtr GetGoodTraining();
    const static CSPtr GetBadTraining();
    const static std::vector<T>& GetSpectrumClean();
    const static std::vector<T>& GetItakuraDistance();
    /**
     * @brief Training features
     */
    Matrix<T> m_trainingFeatures;
    /**
     * @brief Quality labels of each features
     */
    std::vector<T> m_trainingClasses;
    /**
     * @brief Weights
     */
    std::vector<T> m_w;
    /**
     * @brief Average features values accross training data
     */
    std::vector<T> m_mu;
    /**
     * @brief Standard deviation of features values
     */
    std::vector<T> m_sigma;
    /**
     * @brief Cost class matrix, size is equals to trainingClasses unique values count
     * 
     */
    Matrix<T> m_costClass;
};

/**
 * @brief Read a real vector from a complex vector file
 * 
 * @param filename 
 * @return std::vector<T> 
 */
template <typename T>
std::vector<T> read_complex_vector_to_real_vector(const std::string &filename);

/**
 * @brief Quality Checker training data reader
 */
template <typename T>
class TrainingDataReader
{
    public:
    /**
     * @brief Read a training data from all needed training files
     * 
     * @param trainingFeaturesFile 
     * @param trainingClassesFile 
     * @param wFile 
     * @param muFile 
     * @param sigmaFile 
     * @param costClassSize 
     * @return TrainingData 
     */
    static TrainingData<T> read(const std::string& trainingFeaturesFile, const std::string& trainingClassesFile,
                        const std::string& wFile, const std::string& muFile,
                        const std::string& sigmaFile, const unsigned int costClassSize);
};


} // namespace brainbox