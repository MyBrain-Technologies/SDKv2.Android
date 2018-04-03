// MBT_TF_map.h
// Created by Katerina Pandremmenou on 06/06/2016.
// Copyright (c) 2016 Katerina Pandremmenou. All rights reserved.
//
// Update on 09/12/2016 by Katerina Pandremmenou (no Armadillo, double-->float)
// Update on 03/04/2017 by Katerina Pandremmenou (convert everything from float to double)
// Update on 05/04/2017 by Katerina Pandremmenou (Fix all the warnings)

#ifndef _MBT_TF_map_
#define _MBT_TF_map_

#include <vector>
#include <complex>
#include <iomanip>
#include "../../../SignalProcessing.Cpp/DataManipulation/Headers/MBT_Matrix.h"
#include "../Headers/MBT_TF_map.h"
#include "../../../SignalProcessing.Cpp/Algebra/Headers/MBT_Operations.h"

using namespace std;

typedef complex <double> ComplexDouble;
typedef vector <ComplexDouble> CDVector;

// Edge points at the beginning that correspond to frequencies 5 Hz - 15 Hz (7Hz-2, 13Hz+2)
const vector<int> AllEdgePoints = {76, 63, 54, 47, 42, 38, 35, 32, 29, 27, 26}; 
	
// this function calculates the TF map
MBT_Matrix<double> TF(vector<double> signal, const double fs, vector<double> powers, vector<double> frequencies);
// this function calculates the TF mask
MBT_Matrix<int> TF_mask(int, int);
// this function calculates the frequency peak based on the maximum power in the range of [7Hz ~ 13Hz]
int get_freqPeak(vector<double>, vector<double>);
// this function calculates the minimum (after zero) and maximum values of the TF map
vector<double> MinMaxTFMap(MBT_Matrix<double>);
bool great(double value);

class TF_map {
public:
    TF_map();
    ~TF_map();
    
protected:
    const double fc = 1;
    const double FWHM_tc = 3;
    const double sigma_tc = FWHM_tc / sqrt(8*log(2));
    int precision = 3;
    ComplexDouble i = -1;
};

#endif
