#pragma once
#include "Global.hpp"

#include <stdio.h>
#include <vector>
#include <stdexcept>

namespace brainbox {

template<class T>
class Matrix {

public:
    /*
     * @brief Empty Matrix constructor.
     * @return A Matrix object initialized to represent an empty matrix.
     */
    Matrix();

    /*
     * @brief Matrix copy constructor.
     * @return A Matrix object identitical to the original one.
     */
    Matrix(const Matrix &originalMatrix);

    /*
     * @brief Matrix constructor with provided dimensions.
     * @param width The desired width of the matrix.
     * @param height The desired height of the matrix.
     * @return A Matrix object initialized to represent a default matrix of the provided dimensions.
     */
    Matrix(unsigned int height, unsigned int width);

	/*
	 * @brief Matrix constructor with provided dimensions and single dimension array.
     * @param width The desired width of the matrix.
     * @param height The desired height of the matrix.
     * @param data Reference to a 1d vector representation of the data.
     * @return A Matrix object initialized to represent a default matrix of the provided dimensions.
    */
    Matrix(unsigned int height, unsigned int width, std::vector<T> const& data);
    
    /*
     * @brief Matrix constructor with provided dimensions.
     * @param width The desired width of the matrix.
     * @param heidht The desired height of the matrix.
     * @param data Reference to a 2d vector representation of the data.
     * @return A Matrix object initialized to represent a default matrix of the provided dimensions.
     */
    Matrix(unsigned int height, unsigned int width, std::vector< std::vector<T> > const& data);
    
    /*
     * @brief Matrix destructor.
     */
    ~Matrix();

    void set_row(unsigned int row, std::vector<T>&& v);
    void set_row(unsigned int row, const std::vector<T>& v);

    /*
     * @brief Subscript operator.
     * @param row The row index.
     * @param col The column index.
     * @return The value corresponding to the specified indexes.
     */
    T& operator() (unsigned int row, unsigned int col);

    T operator() (unsigned int row, unsigned int col) const;

    /*
     * @brief Get the size of the matrix.
     * @return A pair of the height of the width of the matrix.
     */
    std::pair<int, int> size() const;
    
    /**
     * @brief Checks wether a Matrix is empty
     * 
     * @return true The matrix is empty
     * @return false The matrix is not empty
     */
    bool empty() const;

    /*
     * @brief Extract a row from the matrix.
     * @param rowIndex The index of the desired row.
     * @return A vector corresponding to the desired row.
     */
    std::vector<T> row(const int rowIndex) const;

    /*
     * @brief Extract a column from the matrix.
     * @param rowIndex The index of the desired column.
     * @return A vector corresponding to the desired column.
     */
    std::vector<T> column(const int columnIndex) const;
    
private:
    /** @brief The number of row */
    int m_height;
    /**  @brief The number of columns */
    int m_width;
    /** @brief The data of the matrix, stored in a vector */
    std::vector<T> m_data;
};

namespace matrix {

/**
 * @brief Read values as a vector from a file
 *        File must be formatted with escapes between values in a single line
 * 
 * @param fileName Path of the file to open
 * @return std::vector<std::complex<T>> A vector parsed from the file
 */
template <typename T>
std::vector<std::complex<T>> read_vector(std::string fileName);

/**
 * @brief Write a vector into a file
 * 
 * @param output_signal The vector to output on the file
 * @param fileName The path of the file to write
 */
template <typename T>
void write_vector(std::vector<std::complex<T>>& output_signal, std::string fileName);

/**
 * @brief Read values as a matrix from a file
 *        File must be formatted with escapes between columns and backspace between matrix lines
 * 
 * @param fileName Path of the file to open
 * @return std::vector<T> A matrix parsed from the file
 */
template <typename T>
Matrix<T> read_matrix(std::string fileName);

/**
 * @brief Write a matrix into a file
 * 
 * @param output_signal The vector to output on the file
 * @param fileName The path of the file to write
 */
template <typename T>
void write_matrix(Matrix<T>& outputEegData,std::string fileName);


/**
 * @brief Count lines of an open file
 *        Counting only lines containing at least a legit character (different than ' ', '\t' and '\n')
 * 
 * @param stream File stream to count lines
 * @return size_t Number of lines of the file
 */
size_t count_lines(std::ifstream& stream);

/**
 * @brief Count values of an open file
 *        Counting only values different than ' ', '\t' and '\n'
 * 
 * @param stream File stream to count lines
 * @return size_t Number of values of the file
 */
size_t count_values(std::ifstream& stream);

/**
 * @brief Open a file with throwing exception mode enabled
 * 
 * @param fileName Name of the file to open
 * @return std::ifstream The opened file
 */
std::ifstream open_file(const std::string& fileName);

/**
 * @brief Checker wether a file is empty or not
 *        An empty file contains no value (except ' ', '\n' and '\t')
 * 
 * @param stream The file to check
 * @return true The file is empty
 * @return false The file is not empty
 */
bool is_file_empty(std::ifstream& stream);

/**
 * @brief Clear error flags of a file and set it's cursor position
 * 
 * @param stream The file to clear and set
 * @param position The cursor position to set on the file
 * @return std::streampos The previous cursor position
 */
std::streampos clear_and_set_position(std::ifstream& stream, std::streampos position);

/**
 * @brief Convert a string to a float
 * 
 * @param s String to convert
 * @return T A T value corresponding to the parsed string
 */
template <typename T>
T string_to_float(const std::string& s);

/**
 * @brief Add a string value representing a T to a vector of complex
 * 
 * @param vec The vector to push in
 * @param value The string value
 */
template <typename T>
void push_eeg_complex_value(std::vector<std::complex<T>>& vec, const std::string& value);

/**
 * @brief Add a string value representing a T to a vector of float
 * 
 * @param vec The vector to push in
 * @param value The string value
 */
template <typename T>
void push_eeg_value(std::vector<T>& vec, const std::string& value);

/**
 * @brief Case insensitive comparison of two strings
 * 
 * @param str1 First string to compare
 * @param str2 Second string to compare
 * @return true The two strings are equals
 * @return false The two strings are different
 */
bool case_insens_string_compare(const std::string& str1, const std::string& str2);

/**
 * @brief Output a float into a file with scientific notation
 * Precision of outputed float is set to the number of base-10 digits of float type + 1
 * This incrementation enables to control floating point errors
 *
 * @param value The value to output
 * @param file The file to put the value in
 */
template <typename T>
void output_scientific_floating_point_to_file(const T value, std::ostream& file);

/**
 * @brief Remove NaN from a vector and affect values to a matrix 
 * 
 * @param dataWithoutOutliers 
 * @return Matrix<T> 
 */
template <typename T>
Matrix<T> any_vector_to_matrix_no_nan(std::vector<T>& dataWithoutOutliers);
}  // namespace matrix
}  // namespace brainbox