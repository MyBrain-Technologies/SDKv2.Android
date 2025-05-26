#ifndef __MBT_READINPUTORWRITEOUTPUT_H__
#define __MBT_READINPUTORWRITEOUTPUT_H__

#include <sp-global.h>

#include "DataManipulation/MBT_Matrix.h"

#include <complex>
#include <fstream>
#include <string>
#include <vector>

/**
 * @brief Read values as a vector from a file
 *        File must be formatted with escapes between values in a single line
 * 
 * @param fileName Path of the file to open
 * @return SP_ComplexFloatVector A vector parsed from the file
 */
SP_ComplexFloatVector MBT_readVector(std::string fileName);

/**
 * @brief Write a vector into a file
 * 
 * @param outputData The vector to output on the file
 * @param fileName The path of the file to write
 */
void MBT_writeVector(SP_ComplexFloatVector& outputData, std::string fileName);

/**
 * @brief Read values as a matrix from a file
 *        File must be formatted with escapes between columns and backspace between matrix lines
 * 
 * @param fileName Path of the file to open
 * @return SP_FloatMatrix A matrix parsed from the file
 */
SP_FloatMatrix MBT_readMatrix(std::string fileName);

/**
 * @brief Write a matrix into a file
 * 
 * @param outputData The vector to output on the file
 * @param fileName The path of the file to write
 */
void MBT_writeMatrix(SP_FloatMatrix& outputEegData,std::string fileName);

#endif // __MBT_READINPUTORWRITEOUTPUT_H__
