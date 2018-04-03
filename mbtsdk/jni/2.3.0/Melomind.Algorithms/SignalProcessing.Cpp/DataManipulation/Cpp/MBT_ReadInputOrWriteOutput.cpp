//
// MBT_ReadInputOrWriteOutput.cpp
//
// Created by Fanny GROSSELIN on 2016/08/26 (inspired from Emma Barme's code on 2015)
// Copyright (c) 2016 myBrain Technologies. All rights reserved.
//
// Update : Fanny Grosselin 2017/01/26 Read correctly NaN values.
//		    Fanny Grosselin 23/03/2017 --> Change std::nan by nanf
//			Fanny Grosselin 2017/03/27 --> Change '\' by '/' for the paths
//          Fanny Grosselin 2017/03/27 --> Fix all the warnings.
// Update : Katerina Pandremmenou 2017/04/04 --> Include two OR conditions in MBT_readVector for nan values
#include "../Headers/MBT_ReadInputOrWriteOutput.h"


// From Emma Barme on 2015
std::vector<std::complex<float> > MBT_readVector(std::string fileName)
{
    std::fstream (file);
    file.open (fileName.c_str());

    std::vector<std::complex<float> > data;

    if (file.is_open())
    {
        if ( file.peek() != std::ifstream::traits_type::eof() )
        {
            std::string number;
            while (file >> number) // read "word" by "word"
            {
                if ( (number == "NaN") || (number == "nan") || (number == "Nan") )
                {
                    float nanValue = nanf(" ");
                    data.push_back(std::complex<float>(nanValue, 0));
                }
                else
                {
                    std::istringstream numberStream(number);
                    float n;
                    numberStream >> n;
                    data.push_back(std::complex<float>(n, 0));
                }
            }

            file.close();
        }
        else
        {
            errno = ENOENT;
            perror("Read Vector ERROR: The input file is empty");
        }
    }
    else
    {
        errno = ENOENT;
        perror("Read Vector ERROR: Cannot open the input file");
    }

    return data;
}

// From Emma Barme on 2015
void MBT_writeVector(std::vector<std::complex<float> >& outputData, std::string fileName)
{
    std::ofstream file (fileName.c_str());

    if (file.is_open())
    {
        for (unsigned int t=0;t<outputData.size();t++)
        {
            std::complex<float> complexNumber = outputData[t];
            file << std::fixed << std::setprecision(20) << complexNumber.real() << " ";
        }

        file.close();
    }
    else
    {
        errno = ENOENT;
        perror("Write Vector ERROR: Cannot open the output file");
    }
}


MBT_Matrix<float> MBT_readMatrix(std::string fileName)
{
    std::fstream (file);
    file.open (fileName.c_str());

    MBT_Matrix<float> EegData;

    if (file.is_open())
    {
        if ( file.peek() != std::ifstream::traits_type::eof() )
        {
            std::string number;
            unsigned int numberOfEegValues = 0;
            while (file >> number) ++numberOfEegValues;// read "EEG value" by "EEG value"

            file.clear(); // because we have already reach the end of the file
            file.seekg(0,file.beg); // go back to the beginning of the file

            // Number of EEG signals
            std::string ligne;
            unsigned int numberOfLines = 0;
            while(std::getline(file,ligne)) ++numberOfLines;

            // Recover the length of 1 EEG signal
            unsigned int lenSignal = numberOfEegValues/numberOfLines;
            //std::cout<<"lenSignal = "<<lenSignal<<std::endl;

            MBT_Matrix<float> initEegData(numberOfLines,lenSignal);
            EegData = initEegData;

            file.clear(); // because we have already reach the end of the file
            file.seekg(0,file.beg); // go back to the beginning of the file

            std::string EegValue;
            unsigned int countLenSignal = 0;
            unsigned int numLine = 0;
            float n;
            std::vector<std::complex<float> > signal;

            while (file >> EegValue)
            {
                if (countLenSignal<lenSignal)
                {
                    if (EegValue == "NaN")
                    {
                        float nanValue = nanf(" ");
                        signal.push_back(std::complex<float>(nanValue, 0));
                    }
                    else
                    {
                        std::istringstream numberStream(EegValue);
                        numberStream >> n;
                        signal.push_back(std::complex<float>(n, 0));
                    }

                    countLenSignal = countLenSignal + 1;
                }
                else
                {
                    for (unsigned int t=0;t<signal.size();t++)
                    {
                        EegData(numLine,t) = signal[t].real();
                    }

                    numLine = numLine + 1;
                    countLenSignal = 0;
                    signal.clear();

                    if (EegValue == "NaN")
                    {
                        float nanValue = nanf(" ");
                        signal.push_back(std::complex<float>(nanValue, 0));
                    }
                    else
                    {
                        std::istringstream numberStream(EegValue);
                        numberStream >> n;
                        signal.push_back(std::complex<float>(n, 0));
                    }

                    countLenSignal = countLenSignal + 1;
                }

                if (numLine == numberOfLines-1)
                {
                    for (unsigned int t=0;t<signal.size();t++)
                    {
                        EegData(numLine,t) = signal[t].real();
                    }
                }
            }
            file.close();
        }
        else
        {
            errno = ENOENT;
            perror("Read matrix ERROR: The input file is empty");
        }

    }
    else
    {
        errno = ENOENT;
        perror("Read matrix ERROR: Cannot open the input file");
    }

    return EegData;
}


void MBT_writeMatrix(MBT_Matrix<float>& outputEegData,std::string fileName)
{
    std::ofstream file (fileName.c_str());

    if (file.is_open())
    {
        for (int i=0;i<outputEegData.size().first;i++)
        {
            for (int j=0;j<outputEegData.size().second;j++)
            {
                std::complex<float> complexNumber = outputEegData(i,j);
                file << std::fixed << std::setprecision(20) << complexNumber.real() << " ";
            }
            file << '\n';
        }

        file.close();
    }
    else
    {
        errno = ENOENT;
        perror("Write matrix ERROR: Cannot open the output file");
    }
}
