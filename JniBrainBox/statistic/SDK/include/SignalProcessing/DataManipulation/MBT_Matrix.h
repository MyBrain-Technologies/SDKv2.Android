#ifndef __MBT_MATRIX_H__
#define __MBT_MATRIX_H__

#include <sp-global.h>

#include <stdio.h>
#include <vector>
#include <stdexcept>

template<class T>
class MBT_Matrix {

public:
    /*
     * @brief Empty MBT_Matrix constructor.
     * @return A MBT_Matrix object initialized to represent an empty matrix.
     */
    MBT_Matrix()
    {
        m_height = 0;
        m_width = 0;
    }

    /*
     * @brief MBT_Matrix copy constructor.
     * @return A MBT_Matrix object identitical to the original one.
     */
    MBT_Matrix(const MBT_Matrix &originalMatrix)
    {
        m_height = originalMatrix.m_height;
        m_width = originalMatrix.m_width;

        m_data.resize(m_height * m_width);
        for (int i = 0; i < m_height; i++)
        {
            for (int j = 0; j < m_width; j++)
            {
                m_data[i * m_width + j] = originalMatrix.m_data[i * m_width + j];
            }
        }
    }

    /*
     * @brief MBT_Matrix constructor with provided dimensions.
     * @param width The desired width of the matrix.
     * @param height The desired height of the matrix.
     * @return A MBT_Matrix object initialized to represent a default matrix of the provided dimensions.
     */
    MBT_Matrix(unsigned int height, unsigned int width)
    {
        m_height = height;
        m_width = width;
        m_data.resize(m_height * m_width);
    }
	
	/*
	 * @brief MBT_Matrix constructor with provided dimensions and single dimension array.
     * @param width The desired width of the matrix.
     * @param height The desired height of the matrix.
     * @param data Reference to a 1d vector representation of the data.
     * @return A MBT_Matrix object initialized to represent a default matrix of the provided dimensions.
    */
    MBT_Matrix(unsigned int height, unsigned int width, std::vector<T> const& data)
    {
        if (height * width != data.size()) {
            throw std::invalid_argument("Illegal construction parameters");
        }

        m_height = height;
        m_width = width;
        m_data = data;
    }

    /*
     * @brief MBT_Matrix constructor with provided dimensions.
     * @param width The desired width of the matrix.
     * @param heidht The desired height of the matrix.
     * @param data Reference to a 2d vector representation of the data.
     * @return A MBT_Matrix object initialized to represent a default matrix of the provided dimensions.
     */
    MBT_Matrix(unsigned int height, unsigned int width, std::vector< std::vector<T> > const& data)
    {
        if (height != data.size()) {
            throw std::invalid_argument("Illegal construction parameters");
        } else {
            for (const auto subVector : data) {
                if (width != subVector.size()) {
                    throw std::invalid_argument("Illegal construction parameters");
                }
            }
        }

        m_height = height;
        m_width = width;

        m_data.reserve(m_height * m_width);
        for (const auto& subVector : data)
        {
            std::move(subVector.begin(), subVector.end(), std::back_inserter(m_data));
        }
    }

    /*
     * @brief MBT_Matrix destructor.
     */
    ~MBT_Matrix()
    {
    }

    /*
     * @brief Subscript operator.
     * @param row The row index.
     * @param col The column index.
     * @return The value corresponding to the specified indexes.
     */
    T& operator() (unsigned int row, unsigned int col)
    {
        if (row >= m_height || col >= m_width) {
            throw std::out_of_range("Out of range accessor");
        }
        return m_data[row * m_width + col];
    }

    /*
     * @brief Get the size of the matrix.
     * @return A pair of the height of the width of the matrix.
     */
    std::pair<int, int> size() const
    {
        return std::pair<int, int>(m_height, m_width);
    }

    /**
     * @brief Checks wether a MBT_Matrix is empty
     * 
     * @return true The matrix is empty
     * @return false The matrix is not empty
     */
    bool empty() const
    {
        return m_height == 0 || m_width == 0;
    }

    /*
     * @brief Extract a row from the matrix.
     * @param rowIndex The index of the desired row.
     * @return A vector corresponding to the desired row.
     */
    std::vector<T> row(const int rowIndex) const
    {
        if (rowIndex >= m_height) {
            throw std::out_of_range("Out of range accessor");
        }

        std::vector<T> extractedRow;
        for (int i = 0; i < m_width; i++)
        {
            extractedRow.push_back(m_data[rowIndex * m_width + i]);
        }
        return extractedRow;
    }

    /*
     * @brief Extract a column from the matrix.
     * @param rowIndex The index of the desired column.
     * @return A vector corresponding to the desired column.
     */
    std::vector<T> column(const int columnIndex) const
    {
        if (columnIndex >= m_width) {
            throw std::out_of_range("Out of range accessor");
        }
        
        std::vector<T> extractedColumn;
        for (int i = 0; i < m_height; i++)
        {
            extractedColumn.push_back(m_data[i * m_width + columnIndex]);
        }
        return extractedColumn;
    }

private:
    /** @brief The number of row */
    int m_height;
    /**  @brief The number of columns */
    int m_width;
    /** @brief The data of the matrix, stored in a vector */
    std::vector<T> m_data;
};

typedef MBT_Matrix<SP_RealType> SP_Matrix;
typedef MBT_Matrix<SP_Complex> SP_ComplexMatrix;

// Retrocompatibility type definition, remove when finishing full refactoring
typedef MBT_Matrix<SP_FloatType> SP_FloatMatrix;

#endif // __MBT_MATRIX_H__
