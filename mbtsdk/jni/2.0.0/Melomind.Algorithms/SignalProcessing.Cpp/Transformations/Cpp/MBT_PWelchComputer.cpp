//
//  MBT_PWelchComputer.cpp
//  MBT.iOS
//
//  Created by Emma Barme on 19/10/2015.
//  Copyright (c) 2015 Emma Barme. All rights reserved.
//
//  Update: Fanny Grosselin 2016/08/26
//          Fanny Grosselin 2016/12/13 --> add a method trendCorrection
//			Fanny Grosselin 2017/01/10 --> correct computeWindow
//          Fanny Grosselin 2017/01/12 --> correct the complexSignal generation: computeWindow should be called with i and not window because "n" of computeWindow should be in [0:windowLength-1] and not in [0:windowNumber];
//          Fanny Grosselin 2017/01/17 --> zero-padding each segment
//          Fanny Grosselin 2017/01/17 --> Generate the one-sided spectrum
//          Fanny Grosselin 2017/01/17 --> Compensate the power of the Hamming window by dividing the power by the power of the window
//          Fanny Grosselin 2017/01/17 --> Divide the power by samplingRate like in Matlab
//          Fanny Grosselin 2017/01/19 --> Add a new method to compute fft and use it instead of MBT_Fourier::forwardBluesteinFFT
//          Fanny Grosselin 2017/01/20 --> Remove the new method to compute fft and re-use MBT_Fourier::forwardBluesteinFFT
//          Fanny Grosselin 2017/02/03 --> rechange floor to ceil in the computation of powerOfTwoLength
//                                         and in the one-sided spectrum,choose 1 point over 2 points because powerOfTwoLength is 2 times
//                                         higher than what we want in Matlab.
//          Fanny Grosselin 2017/03/16 --> Use MBT_Fourier_fftw3 instead of using MBT_Fourier
//          Fanny Grosselin 2017/03/20 --> Initialize all of the vectors with a predefined size and use an assignment in the place of push_back for the use of MBT_Fourier_fftw3 functions
//          Fanny Grosselin 2017/03/23 --> Change float by double and use the double functions of MBT_Fourier_fftw3 because fftwf can't be used for the moment in Andro�d
//          Fanny Grosselin 2017/03/27 --> Fix all the warnings (in particular in MBT_PWelchComputer::computeWindow).
//          Fanny Grosselin 2017/05/22 --> Fix the memory leaks because of the creation of an object without deleting it.
//          Fanny Grosselin 2017/07/19 --> Change the way we complete each window by some zeros.
//          Katerina Pandremmenou 2017/09/19 --> Change all implicit type castings to explicit ones

#include "../Headers/MBT_PWelchComputer.h"

void MBT_PWelchComputer::computePSD()
{
    const int signalLength = m_inputData.size().second;

    // ~~~ I. compute FFT on overlapped/windowed data ~~~

    // Get windowing size
    const int windowNumber = m_nbrWindows;
    const double tempWindowLength = signalLength / (windowNumber * (1 - m_overlap) + m_overlap);
    const int windowLength = (int) floor(tempWindowLength);

    std::vector<MBT_Matrix<double> > segmentedSignal;
    segmentedSignal.assign(m_nbChannel, MBT_Matrix<double>(windowNumber, windowLength));
    // Padding to maximum between 256 and power of two
    int tmp_powerOfTwoLength = (int) pow(2, ceil(log(2 * windowLength - 1)/log(2))); // Fanny Grosselin 2017/01/17
    int powerOfTwoLength = std::max(256,tmp_powerOfTwoLength); // Fanny Grosselin 2017/01/17

    std::vector<MBT_Matrix<std::complex<double> > > complexSignal;
    //complexSignal.assign(m_nbChannel, MBT_Matrix<std::complex<double> >(windowNumber, windowLength));
    complexSignal.assign(m_nbChannel, MBT_Matrix<std::complex<double> >(windowNumber, powerOfTwoLength)); // Fanny Grosselin 2017/01/17

    std::vector<double> WindowSignal; // Fanny Grosselin 2017/01/17
    WindowSignal.assign(windowLength,0); // Fanny Grosselin 2017/01/17
    // Fanny Grosselin 2017/01/17
    double U = 0;
    for (int i = 0; i < windowLength; i++)
    {
        WindowSignal[i] = computeWindow(windowLength, i);
        U = U + WindowSignal[i]*WindowSignal[i]; // compute the power of the window
    }

    //Cut the input data into m_nbrWindow segments
    for (int channel = 0; channel < m_nbChannel; channel++)
    {
        for (int window = 0; window < windowNumber; window++)
        {
            const int starting_index = (int) floor(window * windowLength * (1.0 - m_overlap));

            for (int i = 0; i < windowLength; i++)
            {
                segmentedSignal[channel](window, i) = m_inputData(channel, starting_index + i);
                //complexSignal[channel](window, i) = segmentedSignal[channel](window, i) * computeWindow(windowLength, window);
                complexSignal[channel](window, i) = segmentedSignal[channel](window, i) * computeWindow(windowLength, i); // Fanny Grosselin 2017/01/12 because "n" of computeWindow should be in [0:windowLength-1];
            }
            // Fanny Grosselin 2017/01/17
            //for (int i = powerOfTwoLength - windowLength + 1; i < powerOfTwoLength; i++)
            for (int i = windowLength; i < powerOfTwoLength; i++) // Fanny Grosselin 2017/07/19
            {
                complexSignal[channel](window, i) = 0;
            }
        }
    }

    // compute FFT in place
    for (int window = 0; window < windowNumber; window++)
    {
        for (int channel = 0; channel < m_nbChannel; channel++)
        {
            std::vector<std::complex<double> > tmp_dataToTransform = complexSignal[channel].row(window);

            //MBT_Fourier::forwardBluesteinFFT(dataToTransform);
            DOUBLE_FFTW_C2C_1D *obj = new DOUBLE_FFTW_C2C_1D(tmp_dataToTransform.size(), tmp_dataToTransform, -1);
            std::vector<complex<double> > dataToTransform(tmp_dataToTransform.size());
            dataToTransform = obj->fft_execute();
            delete obj;


            for (unsigned int i = 0; i < dataToTransform.size(); i++)
            {
                complexSignal[channel](window, i) = dataToTransform[i];
            }
        }
    }

    // ~~~ II. compute PSD ~~~
    //m_psd = MBT_Matrix<double>(m_nbChannel + 1, windowLength); //Freq, Pchannel1, Pchannel2...
    m_psd = MBT_Matrix<double>(m_nbChannel + 1, powerOfTwoLength); //Freq, Pchannel1, Pchannel2... // Fanny Grosselin 2017/01/17

    //for (int i = 0; i < windowLength; i++)
    for (int i = 0; i < powerOfTwoLength; i++) // Fanny Grosselin 2017/01/17
    {
        //m_psd(0, i) = (double)(i) * (m_sampRate / windowLength);
        m_psd(0, i) = (double)(i) * (m_sampRate / powerOfTwoLength); // Fanny Grosselin 2017/01/17


        for (int channel = 1; channel <= m_nbChannel; channel++)
        {
            m_psd(channel, i) = 0;

            for (int window = 0; window < windowNumber; window++)
            {
                //m_psd(channel, i) += pow(std::abs(complexSignal[channel - 1](window, i)), 2.0);
                m_psd(channel, i) += (pow(std::abs(complexSignal[channel - 1](window, i)), 2.0))/U; // Fanny Grosselin 2017/01/17 // Compensate for the power of the window
            }

            // Average the sum of the periodograms
            //m_psd(channel, i) = 10 * log10(m_psd(channel, i) / windowNumber);
            m_psd(channel, i) = (m_psd(channel, i) / windowNumber);// Fanny Grosselin 2016/08/26
        }
    }

    // Fanny Grosselin 2017/01/17
    // Generate the one-sided spectrum
    /*if (fmod(powerOfTwoLength,2) == 1) // if odd (impair)
    {
        std::cout<<"impair"<<std::endl;
        m_psd_Scaled = MBT_Matrix<double>(m_nbChannel + 1, (powerOfTwoLength+1)/2); //Freq, Pchannel1, Pchannel2...

        for (int i=0; i<(powerOfTwoLength+1)/2; i++)
        {
            m_psd_Scaled(0,i) = m_psd(0,i);
            for (int channel = 1; channel <= m_nbChannel; channel++)
            {
                if (i!=0)
                {
                    //m_psd_Scaled(channel,i) = 2*m_psd(channel, i);
                    m_psd_Scaled(channel,i) = 2*m_psd(channel, i)/m_sampRate;
                }
                else // only DC is a unique point and doesn't get doubled
                {
                    //m_psd_Scaled(channel,i) = m_psd(channel, i);
                    m_psd_Scaled(channel,i) = m_psd(channel, i)/m_sampRate;
                }
            }
        }
    }
    else // if even (pair)
    {*/
        // Fanny Grosselin 2017/02/03 : Choose 1 point over 2 points because powerOfTwoLength is 2 times higher than what we want in Matlab except if powerOfTwoLentth = 256
    if (powerOfTwoLength != 256)
    {
        m_psd_Scaled = MBT_Matrix<double>(m_nbChannel + 1, ((powerOfTwoLength/2 +1)-1)/2+1); //Freq, Pchannel1, Pchannel2...
        for (int i=0; i<((powerOfTwoLength/2 +1)-1)/2+1; i++)
        {
            m_psd_Scaled(0,i) = m_psd(0,i*2);
            for (int channel = 1; channel <= m_nbChannel; channel++)
            {
                if ((i*2!=0) & (i*2!=powerOfTwoLength/2))
                {
                    //m_psd_Scaled(channel,i) = 2*m_psd(channel, i);
                    m_psd_Scaled(channel,i) = 2*m_psd(channel, i*2)/m_sampRate;
                }
                else // don't double unique Nyquist point
                {
                    //m_psd_Scaled(channel,i) = m_psd(channel, i);
                    m_psd_Scaled(channel,i) = m_psd(channel, i*2)/m_sampRate;
                }
            }
        }
    }
    else
    {
        m_psd_Scaled = MBT_Matrix<double>(m_nbChannel + 1, powerOfTwoLength/2 +1); //Freq, Pchannel1, Pchannel2...
        for (int i=0; i<powerOfTwoLength/2 +1; i++)
        {
            m_psd_Scaled(0,i) = m_psd(0,i);
            for (int channel = 1; channel <= m_nbChannel; channel++)
            {
                if ((i!=0) & (i!=powerOfTwoLength/2))
                {
                    //m_psd_Scaled(channel,i) = 2*m_psd(channel, i);
                    m_psd_Scaled(channel,i) = 2*m_psd(channel, i)/m_sampRate;
                }
                else // don't double unique Nyquist point
                {
                    //m_psd_Scaled(channel,i) = m_psd(channel, i);
                    m_psd_Scaled(channel,i) = m_psd(channel, i)/m_sampRate;
                }
            }
        }
    }

    //}
}


double MBT_PWelchComputer::computeWindow(int windowLength, int n) const
{
    const double  PI_F=3.14159265358979f;
    switch (m_windowType) {
        case RECT:
            return 1;
            break;

        case HANN:
			//return (1.0 - cos(2.0 * PI_F * n / windowLength)) / 2.0;
            return (1.0 - cos(2.0 * PI_F * n / (windowLength-1))) / 2.0; // Fanny Grosselin 2017/01/10 (windowLength-1) instead of windowLength
            break;

        case HAMMING:
			//return 0.54 - 0.46 * cos(2.0 * PI_F * n / windowLength);
            return 0.54 - 0.46 * cos(2.0 * PI_F * n / (windowLength-1));  // Fanny Grosselin 2017/01/10 (windowLength-1) instead of windowLength
            break;

        default:
            return nan(" ");
            break;
    }
}



MBT_PWelchComputer::MBT_PWelchComputer(MBT_Matrix<double> const& inputData, const double sampRate, std::string windowType)
{
    //Initialize input parameters.
    m_sampRate = sampRate;
    if (windowType == "RECT")
    {
        m_windowType = RECT;
    }
    else if (windowType == "HANN")
    {
        m_windowType = HANN;
    }
    else if (windowType == "HAMMING")
    {
        m_windowType = HAMMING;
    }
    else
    {
        std::cerr << "WARNING: UNKNOWN WINDOW TYPE, USING DEFAULT" << std::endl;
    }
    m_inputData = inputData;
    m_nbChannel = m_inputData.size().first;

    //Compute the PSD
    computePSD();
}


MBT_PWelchComputer::~MBT_PWelchComputer()
{

}


MBT_Matrix<double> MBT_PWelchComputer::get_PSD() const
{
    //return m_psd;
    return m_psd_Scaled; // Fanny Grosselin 2017/01/17
}


std::vector<double> MBT_PWelchComputer::get_PSD(int channelIndex) const
{
    //return m_psd.row(channelIndex);
    return m_psd_Scaled.row(channelIndex); // Fanny Grosselin 2017/01/17
}


// Fanny Grosselin 2016/12/13
MBT_Matrix<double> MBT_trendCorrection(MBT_Matrix<double> inputData, const double sampRate)
{
    MBT_Matrix<double> correctedSignal;
    correctedSignal = MBT_Matrix<double>(inputData.size().first, inputData.size().second-1);

    for (int channel = 0; channel < inputData.size().first; channel++)
    {
        for (int i = 1; i < inputData.size().second; i++)
        {
            correctedSignal(channel, i-1) = (inputData(channel,i)-inputData(channel,i-1))/(1/sampRate);
        }
    }
    return correctedSignal;
}
