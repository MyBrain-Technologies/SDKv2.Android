#ifndef __UTILS_H__
#define __UTILS_H__

#include <sp-global.h>

#include <complex>
#include <fstream>
#include <string>
#include <vector>

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
std::ifstream openFileWithExceptions(const std::string& fileName);

/**
 * @brief Checker wether a file is empty or not
 *        An empty file contains no value (except ' ', '\n' and '\t')
 * 
 * @param stream The file to check
 * @return true The file is empty
 * @return false The file is not empty
 */
bool fileIsEmpty(std::ifstream& stream);

/**
 * @brief Clear error flags of a file and set it's cursor position
 * 
 * @param stream The file to clear and set
 * @param position The cursor position to set on the file
 * @return std::streampos The previous cursor position
 */
std::streampos clearAndSetPosition(std::ifstream& stream, std::streampos position);

/**
 * @brief Convert a string to a float
 * 
 * @param s String to convert
 * @return SP_FloatType A SP_FloatType value corresponding to the parsed string
 */
SP_FloatType string_to_float(const std::string& s);

/**
 * @brief Add a string value representing a SP_FloatType to a vector of complex
 * 
 * @param vec The vector to push in
 * @param value The string value
 */
void pushEegComplexValue(SP_ComplexFloatVector& vec, const std::string& value);

/**
 * @brief Add a string value representing a SP_FloatType to a vector of float
 * 
 * @param vec The vector to push in
 * @param value The string value
 */
void pushEegFloatValue(SP_FloatVector& vec, const std::string& value);

/**
 * @brief Case insensitive comparison of two strings
 * 
 * @param str1 First string to compare
 * @param str2 Second string to compare
 * @return true The two strings are equals
 * @return false The two strings are different
 */
bool caseInSensStringCompare(const std::string& str1, const std::string& str2);

/**
 * @brief Output a float into a file with scientific notation
 * Precision of outputed float is set to the number of base-10 digits of float type + 1
 * This incrementation enables to control floating point errors
 *
 * @param value The value to output
 * @param file The file to put the value in
 */
void outputScientificFloatingPointToFile(const SP_FloatType value, std::ostream& file);

#endif // __UTILS_H__
