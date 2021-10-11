#pragma once
#include <algorithm>
#include <vector>
#include <memory>
#include <unordered_map>
#include "TrainingData.hpp"
#include "Settings.hpp"

namespace brainbox {

template <typename T>
using Prediction_Type = std::unordered_map<T, std::vector<T>>;

template <typename T>
struct QualityData {
    TrainingData<T> good_training;
    TrainingData<T> bad_training;
    std::vector<T> spectrum_clean;
    std::vector<T> clean_itakura_distance;
};

template <typename T>
struct QualityResult {
    Matrix<T> input_signal;
    Matrix<T> test_features;
    std::vector<T> proba_class;
    std::vector<T> predicted_class;
    Prediction_Type<T> y_prediction;
    std::vector<T> quality;
    Matrix<T> output_signal;
};

namespace quality {
template <typename T>
std::vector<T> time_features(const std::vector<T>& signal, const Settings<T>& settings);
template <typename T>
std::vector<T> frequency_features(const std::vector<T>& signal, const Settings<T>& settings);
template <typename T>
Matrix<T> checker_features(const Matrix<T>& input_signal, const int m_testFeaturesSize, const Settings<T>& settings);

} // namespace quality

template <class Base>
struct CSPtrType {
    using CSPtr = std::shared_ptr<const Base>;
};

template <class Base, class Derived>
struct CSPtrMake : CSPtrType<Base> {
    template <typename ...T>
    static typename CSPtrType<Base>::CSPtr GetCSharedPtr(T&&...t) {
        return std::make_shared<Derived>(std::forward<T>(t)...);
    }
};

template <typename T>
class ArtifactDetectionInterface : public CSPtrType<ArtifactDetectionInterface<T>> {
public:
    virtual T compute(const std::vector<T>& signal) const = 0;
    virtual ~ArtifactDetectionInterface() {}
};

template <typename T>
class Itakura : public ArtifactDetectionInterface<T>, public CSPtrMake<ArtifactDetectionInterface<T>, Itakura<T>> {
public:    
    Itakura (const std::vector<T>& clean_itakura_distance 
            ,const std::vector<T>& spectrum_clean
            ,typename Settings<T>::CSPtr settings)
    : clean_itakura_distance_(clean_itakura_distance)
    , spectrum_clean_(spectrum_clean)
    , settings_(settings)
    {}

    Itakura(const Itakura& other) = default;
    Itakura(Itakura&& other) = default;

    T compute(const std::vector<T>& signal) const override;

private:
    std::vector<T> clean_itakura_distance_; 
    std::vector<T> spectrum_clean_;
    typename Settings<T>::CSPtr settings_;    
};

template <typename T>
class ClassifierInterface : public CSPtrType<ClassifierInterface<T>> {
public:
    using ClassifierRet = std::tuple<std::vector<T>,std::vector<T>, Prediction_Type<T>>;
    virtual ClassifierRet compute(Matrix<T> const& features, const TrainingData<T>& training) const = 0;
    virtual ~ClassifierInterface() {}
};

template <typename T>
class Knn : public ClassifierInterface<T>, public CSPtrMake<ClassifierInterface<T>, Knn<T>> {
public:
    Knn(unsigned int kppv)
    :kppv_(kppv) 
    {}
    
    Knn(const Knn& other) = default;
    Knn(Knn&& other)= default;

    typename ClassifierInterface<T>::ClassifierRet compute(Matrix<T> const& features, const TrainingData<T>& training) const override;

private:
    unsigned int kppv_;
};

template <typename T>
class QualityChecker {
    
    struct DefaultArtifact : public ArtifactDetectionInterface<T>, public CSPtrMake<ArtifactDetectionInterface<T>, DefaultArtifact> {
        T compute(const std::vector<T>& signal) const override;
    };

    std::pair<std::vector<T>, Matrix<T>> 
    quality_checker(const Matrix<T>& input_signal, const Matrix<T>& inputDataInit, const std::vector<T>& probaClass, const std::vector<T>& predictedClass);
    QualityResult<T> bad_training_filter(QualityResult<T>&& results);

public:

    QualityChecker(typename TrainingData<T>::CSPtr good_training);
    QualityChecker(const QualityChecker&) = default;
    QualityChecker(QualityChecker&&) = default;
    
    QualityChecker& WithArtifactDetection(typename ArtifactDetectionInterface<T>::CSPtr artifact);
    QualityChecker& WithClassifier(typename ClassifierInterface<T>::CSPtr classifier);
    QualityChecker& WithBadTraining(typename TrainingData<T>::CSPtr bad_training);
    QualityChecker& WithSettings(typename Settings<T>::CSPtr settings);

    QualityResult<T> compute(const Matrix<T>& inputData);

private:
    typename TrainingData<T>::CSPtr good_training_;
    typename ArtifactDetectionInterface<T>::CSPtr artifact_;
    typename ClassifierInterface<T>::CSPtr classifier_;
    typename TrainingData<T>::CSPtr bad_training_;
    typename Settings<T>::CSPtr settings_;
};

}  // namespace quality

