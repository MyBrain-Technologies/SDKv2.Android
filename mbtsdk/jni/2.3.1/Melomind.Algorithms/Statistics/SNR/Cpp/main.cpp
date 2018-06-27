//
// main.cpp
// Created by Katerina Pandremmenou on 31/10/2017 (inspired by Emma's code for reading from input/writing to output)
// Copyright (c) 2017 Katerina Pandremmenou. All rights reserved
// 
//
// Description: A main function to test the functionality of the statistics of the exercises Journey, Switch, Equilibrium
//              

#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <iterator>
#include <iomanip>
#include <string>
#include <algorithm>
#include <ctime>
#include <chrono>
#include <math.h>
#include <stdio.h>
#include <complex>

#include "../Headers/MBT_SNR_Stats.h"

using namespace std;

// function to read from a file
vector<float> readInput(string fileName)
{
    ifstream file (fileName.c_str());
    vector<float> data;

    if (file.is_open())
    {  
        string number;
        while (file >> number)
        {
            istringstream numberStream(number);
            float n;
            numberStream >> n;
            data.push_back(n);
        }
        
        file.close();
    }
    else
    {
        errno = ENOENT;
        perror("ERROR: Cannot open the input file");
    }
    
    return data;
}

int main()
{  
    //============================ CHECK THE SNR STATISTICS ===============================
    vector<float> SNR = readInput("../Files/Carnac/VinRec2.txt");

    SNR_Statistics obj(SNR);

    int u = 0;
    float ExerciseThreshold = 0.4f; 
    
    while(u<=10)
    {
       ExerciseThreshold = (float) u / 10.0f; 

       map<string, float> SNR_Statistics = obj.CalculateSNRStatistics(SNR, ExerciseThreshold);
    

        for (const auto &p : SNR_Statistics) 
        {
            cout << p.first << " = " << p.second << '\n';
        }
    
        cout << endl;
        u = u + 1;
    }
    return 0;
}

map<string, float> main_release(vector<float> SNR, float ExerciseThreshold)
{  
    //============================ CHECK THE SNR STATISTICS ===============================
    SNR_Statistics obj(SNR);
    
    map<string, float> SNR_Statistics = obj.CalculateSNRStatistics(SNR, ExerciseThreshold);

    return SNR_Statistics;
}


