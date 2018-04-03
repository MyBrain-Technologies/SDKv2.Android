//
//  MBT_Matrix.h
//  MBT.iOS
//
//  Created by Emma Barme on 19/10/2015.
//  Copyright (c) 2015 Emma Barme. All rights reserved.
//
// 	Update: Fanny Grosselin 23/03/2017 --> Create a member function with the possibility to have double in input.
//			Etienne Ganrin 28/03/2017 --> Add a MBT_Matrix constructor with provided dimensions and single dimension array

#ifndef __MBT_iOS__MBT_Matrix__
#define __MBT_iOS__MBT_Matrix__

#include <stdio.h>
#include <vector>

template<class T>
class MBT_Matrix {

private:
    //The number of row
    int m_height;

    //The number of columns
    int m_width;

    //The data, stored in a vector.
    std::vector<T> m_data;

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
     * @param heidht The desired height of the matrix.
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
     * @param heidht The desired height of the matrix.
     * @param data Reference to a 1d vector representation of the data.
     * @return A MBT_Matrix object initialized to represent a default matrix of the provided dimensions.
    */
   MBT_Matrix(unsigned int height, unsigned int width, std::vector<T> const& data){
       m_height = height;
       m_width = width;
       m_data.resize(data.size());
       for (unsigned int j = 0; j < data.size(); j++)
       {
           m_data[j] = data[j];
       }
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
        m_height = height;
        m_width = width;

        m_data.resize(m_height * m_width);
        for (int i = 0; i < m_height; i++)
        {
            for (int j = 0; j < m_width; j++)
            {
                m_data[i * m_width + j] = data[i][j];
            }
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

    /*
     * @brief Extract a row from the matrix.
     * @param rowIndex The index of the desired row.
     * @return A vector corresponding to the desired row.
     */
    std::vector<T> row(const int rowIndex) const
    {
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
        std::vector<T> extractedColumn;
        for (int i = 0; i < m_height; i++)
        {
            extractedColumn.push_back(m_data[i * m_width + columnIndex]);
        }
        return extractedColumn;
    }

};

#endif /* defined(__MBT_iOS__MBT_Matrix__) */
