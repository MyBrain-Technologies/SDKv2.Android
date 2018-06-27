#include <iostream>
#include <stdio.h>
#include <vector>
#include <algorithm> // std::min_element
#include <iterator>  // std::begin, std::end
#include <string>


#include "..\..\DataManipulation\Headers\MBT_Matrix.h" // use of the class MBT_Matrix
#include "..\..\DataManipulation\Headers\MBT_ReadInputOrWriteOutput.h" // use of the class MBT_ReadInputOrWriteOutput
#include "..\Headers\MBT_PWelchComputer.h" // use of the class MBT_PWelchComputer


int main()
{
    // Test trendCorrection
    float sampRate = 250;
    MBT_Matrix<float> inputData = MBT_readMatrix("C:/Users/Fanny/Documents/SignalProcessing.Cpp/Transformations/TestFiles/inputData.txt");


    MBT_Matrix<float> correctedSignal = MBT_trendCorrection(inputData, sampRate);
    MBT_writeMatrix (correctedSignal, "C:/Users/Fanny/Documents/SignalProcessing.Cpp/Transformations/TestFiles/correctedSignalCPlusPlus.txt");

    return 0;
}
