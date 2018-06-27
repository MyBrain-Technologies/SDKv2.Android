//
//  MBT_FFTReal.cpp
//  MBT.iOS
//
//  Created by Emma Barme on 12/10/2015.
//  Copyright (c) 2015 Emma Barme. All rights reserved.
//
// 	Update: Fanny Grosselin 23/03/2017 --> Change float by double
//			Fanny Grosselin 2017/03/27 --> Change '\' by '/' for the paths

#include "../Headers/MBT_FFTReal.h"

//Public functions
MBT_FFTReal::MBT_FFTReal(const long length): m_length(length), m_nbrBits(int (floor (log (length) / log (2) + 0.5))), m_bitReversedLookUpTable(int (floor (log (length) / log (2) + 0.5))), m_trigonometricLookUpTable(int (floor (log (length) / log (2) + 0.5))), m_sqrt(double((sqrt (2) * 0.5)))
{
    assert ((1L << m_nbrBits) == length);
    
    m_buffer = 0;
    if (m_nbrBits > 2)
    {
        m_buffer = new double [m_length];
    }
}

MBT_FFTReal::~MBT_FFTReal()
{
    delete [] m_buffer;
    m_buffer = 0;
}
    
void MBT_FFTReal::do_fft (const double inputInTime[], double outputInFrequencies[]) const
{
    //General case: More than 4 points
    if (m_nbrBits > 2)
    {
        //MARK: No idea what those are
        double * df;
        double * sf;
        
        //If least significant bit is 1
        if (m_nbrBits & 1)
        {
            df = m_buffer;
            sf = outputInFrequencies;
        }
        else
        {
            df = outputInFrequencies;
            sf = m_buffer;
        }
        
        //The transformation is done in several passes
        {
            long coefIndex;
            
            //Pass 1 and pass 2
            {
                const long* const bitReversedLookUpTablePointer = m_bitReversedLookUpTable.get_ptr();
                coefIndex = 0;
                
                //Treat the input array 4 bits by 4 bits
                do
                {
                    const long reversedIndex0 = bitReversedLookUpTablePointer[coefIndex];
                    const long reversedIndex1 = bitReversedLookUpTablePointer[coefIndex + 1];
                    const long reversedIndex2 = bitReversedLookUpTablePointer[coefIndex + 2];
                    const long reversedIndex3 = bitReversedLookUpTablePointer[coefIndex + 3];
                    
                    double* const dfBis = df + coefIndex;
                    dfBis[1] = inputInTime[reversedIndex0] - inputInTime[reversedIndex1];
                    dfBis[3] = inputInTime[reversedIndex2] - inputInTime[reversedIndex3];
                    
                    const double sfZeroPlusOne = inputInTime[reversedIndex0] + inputInTime[reversedIndex1];
                    const double sfTwoPlusThree = inputInTime[reversedIndex2] + inputInTime[reversedIndex3];
                    
                    dfBis[0] = sfZeroPlusOne + sfTwoPlusThree;
                    dfBis[2] = sfZeroPlusOne - sfTwoPlusThree;
                    
                    coefIndex += 4;
                    
                } while (coefIndex < m_length);
            }
            
            //Pass 3
            {
                coefIndex = 0;
                
                //Treat the input array 8 bits by 8 bits
                do
                {
                    double value;
                    
                    sf[coefIndex] = df[coefIndex] + df[coefIndex + 4];
                    sf[coefIndex + 4] = df[coefIndex] - df[coefIndex + 4];
                    sf[coefIndex + 2] = df[coefIndex + 2];
                    sf[coefIndex + 6] = df[coefIndex + 6];
                    
                    value = (df[coefIndex + 5] - df[coefIndex + 7]) * m_sqrt;
                    sf[coefIndex + 1] = df[coefIndex + 1] + value;
                    sf[coefIndex + 3] = df[coefIndex + 1] - value;
                    
                    value = (df[coefIndex + 5] + df[coefIndex + 7]) * m_sqrt;
                    sf[coefIndex + 5] = value + df[coefIndex + 3];
                    sf[coefIndex + 7] = value - df[coefIndex + 3];
                    
                    coefIndex += 8;
                    
                } while (coefIndex < m_length);
            }
            
            //All following passes
            {
                for (int passIndex = 3; passIndex < m_nbrBits; passIndex++)
                {
                    coefIndex = 0;
                    long nbrCoef = 1 << passIndex; //Equivalent to 1*2^pass
                    long halfNbrCoef = nbrCoef >> 1; //Equivalent to nbrCoef / 2^1
                    long doubleNbrCoef = nbrCoef << 1; //Equivalent to nbrCoef * 2^1
                    const double* const cosinusPointer = m_trigonometricLookUpTable.get_ptr(passIndex);
                    
                    //Treat the input array doubleNbrCoef bits by doubleNbrCoef bits
                    do
                    {
                        const double* const sf1r = sf + coefIndex;
                        const double* const sf2r = sf1r + nbrCoef;
                        double* const dfr = df + coefIndex;
                        double* const dfi = dfr + nbrCoef;
                        
                        /* Extreme coefficients are always real */
                        dfr[0] = sf1r[0] + sf2r[0];
                        dfi[0] = sf1r[0] - sf2r[0];	// dfr [nbrCoef] =
                        dfr[halfNbrCoef] = sf1r[halfNbrCoef];
                        dfi[halfNbrCoef] = sf2r[halfNbrCoef];
                        
                        /* Others are conjugate complex numbers */
                        const double* const	sf1i = sf1r + halfNbrCoef;
                        const double* const	sf2i = sf1i + nbrCoef;
                        for (long i = 1; i < halfNbrCoef; i++)
                        {
                            const double cos = cosinusPointer[i];					// cos (i*PI/nbrCoef);
                            const double sin = cosinusPointer[halfNbrCoef - i];      // sin (i*PI/nbrCoef);
                            double value;
                            
                            value = sf2r[i] * cos - sf2i[i] * sin;
                            dfr[i] = sf1r[i] + value;
                            dfi[-i] = sf1r[i] - value;	// dfr [nbrCoef - i] =
                            
                            value = sf2r[i] * sin + sf2i[i] * cos;
                            dfi[i] = value + sf1i[i];
                            dfi[nbrCoef - i] = value - sf1i[i];
                        }
                        
                        coefIndex += doubleNbrCoef;
                        
                    } while(coefIndex < m_length);
                    
                    //Prepare for the next pass
                    {
                        double* const tempPointer = df;
                        df = sf;
                        sf = tempPointer;
                    }
                }
            }
        }
    }
    
    //Extreme case: 4 points
    else if (m_nbrBits == 2)
    {
        outputInFrequencies[1] = inputInTime[0] - inputInTime[2];
        outputInFrequencies[3] = inputInTime[1] - inputInTime[3];
        
        const double zeroPlusTwo = inputInTime[0] + inputInTime[2];
        const double unPlusThree = inputInTime[1] + inputInTime[3];
        
        outputInFrequencies[0] = zeroPlusTwo + unPlusThree;
        outputInFrequencies[2] = zeroPlusTwo - unPlusThree;
    }
    
    //Extreme case: 2 points
    else if (m_nbrBits == 1)
    {
        outputInFrequencies[0] = inputInTime[0] + inputInTime[1];
        outputInFrequencies[1] = inputInTime[0] - inputInTime[1];
    }
    
    //Extreme case: 1 point
    else if (m_nbrBits == 0)
    {
        outputInFrequencies[0] = inputInTime[0];
    }
    
    //Invalid number of bits.
    else
    {
        assert(m_nbrBits >= 0);
    }
    
}
    
    /*
     * @brief Compute the inverse FFT of the array.
     * Note that IFFT (FFT (x)) = x * length (x). Data must be post-scaled.
     * @param inputInFrequencies Pointer to the source array (data by frequency). Indexes 0 to length(outputInTime)/2 are real values, indexes from length(outputInTime)/2+1 to length(outputInTime)-1 are imaginary values of coefficents 1 to length(outputInTime)/2-1.
     * @param outputInTime Pointer to the destination array (data by time).
     */
void MBT_FFTReal::do_ifft(const double inputInFrequencies[], double outputInTime[]) const
{
    //General case: More than 4 points
    if (m_nbrBits > 2)
    {
        double* sf = const_cast<double*>(inputInFrequencies);
        double* df;
        double* dfTemp;
        
        //If least significant bit is 1
        if (m_nbrBits & 1)
        {
            df = m_buffer;
            dfTemp = outputInTime;
        }
        else
        {
            df = outputInTime;
            dfTemp = m_buffer;
        }
        
        //The transformation is done in several passes
        {
            long coefIndex;
            
            //All passes up to the last three
            for (int passIndex = m_nbrBits - 1; passIndex >= 3; passIndex--)
            {
                coefIndex = 0;
                long nbrCoef = 1 << passIndex; //Equivalent to 1*2^pass
                long halfNbrCoef = nbrCoef >> 1; //Equivalent to nbrCoef / 2^1
                long doubleNbrCoef = nbrCoef << 1; //Equivalent to nbrCoef * 2^1
                const double* const cosinusPointer = m_trigonometricLookUpTable.get_ptr(passIndex);
                
                //Treat the input array doubleNbrCoef bits by doubleNbrCoef bits
                do
                {
                    const double* const sfr = sf + coefIndex;
                    const double* const sfi = sfr + nbrCoef;
                    double*	const df1r = df + coefIndex;
                    double*	const df2r = df1r + nbrCoef;
                    
                    /* Extreme coefficients are always real */
                    df1r[0] = sfr[0] + sfi[0];		// + sfr [nbrCoef]
                    df2r[0] = sfr[0] - sfi[0];		// - sfr [nbrCoef]
                    df1r[halfNbrCoef] = sfr[halfNbrCoef] * 2;
                    df2r[halfNbrCoef] = sfi[halfNbrCoef] * 2;
                    
                    /* Others are conjugate complex numbers */
                    double* const df1i = df1r + halfNbrCoef;
                    double* const df2i = df1i + nbrCoef;
                    for (long i = 1; i < halfNbrCoef; i++)
                    {
                        df1r[i] = sfr[i] + sfi[-i];		// + sfr [nbrCoef - i]
                        df1i[i] = sfi[i] - sfi[nbrCoef - i];
                        
                        const double cos = cosinusPointer[i];					// cos (i*PI/nbrCoef);
                        const double sin = cosinusPointer[halfNbrCoef - i];	// sin (i*PI/nbrCoef);
                        const double valueReal = sfr[i] - sfi[-i];		// - sfr [nbrCoef - i]
                        const double valueImaginary = sfi[i] + sfi[nbrCoef - i];
                        
                        df2r [i] = valueReal * cos + valueImaginary * sin;
                        df2i [i] = valueImaginary * cos - valueReal * sin;
                    }
                    
                    coefIndex += doubleNbrCoef;
                } while (coefIndex < m_length);
                
                /* Prepare for the next pass */
                if (passIndex < m_nbrBits - 1)
                {
                    double* const tempPointer = df;
                    df = sf;
                    sf = tempPointer;
                }
                else
                {
                    sf = df;
                    df = dfTemp;
                }
            }
            
            //Antepenultimate pass
            {
                coefIndex = 0;
                
                //Treat the input array 8 bits by 8 bits
                do
                {
                    df[coefIndex] = sf[coefIndex] + sf[coefIndex + 4];
                    df[coefIndex + 4] = sf[coefIndex] - sf[coefIndex + 4];
                    df[coefIndex + 2] = sf[coefIndex + 2] * 2;
                    df[coefIndex + 6] = sf[coefIndex + 6] * 2;
                    
                    df[coefIndex + 1] = sf[coefIndex + 1] + sf[coefIndex + 3];
                    df[coefIndex + 3] = sf[coefIndex + 5] - sf[coefIndex + 7];
                    
                    const double valueReal = sf[coefIndex + 1] - sf[coefIndex + 3];
                    const double valueImaginary = sf[coefIndex + 5] + sf[coefIndex + 7];
                    
                    df[coefIndex + 5] = (valueReal + valueImaginary) * m_sqrt;
                    df[coefIndex + 7] = (valueImaginary - valueReal) * m_sqrt;
                    
                    coefIndex += 8;
                }
                while (coefIndex < m_length);
            }
            
            //Penultimate and last pass
            {
                coefIndex = 0;
                const long*	bitReversedLookUpTablePointer = m_bitReversedLookUpTable.get_ptr ();
                const double* sf2 = df;
                
                //Treat the input array 4 bits by 4 bits
                do
                {
                    {
                        const double b_0 = sf2[0] + sf2[2];
                        const double b_2 = sf2[0] - sf2[2];
                        const double b_1 = sf2[1] * 2;
                        const double b_3 = sf2[3] * 2;
                        
                        outputInTime[bitReversedLookUpTablePointer[0]] = b_0 + b_1;
                        outputInTime[bitReversedLookUpTablePointer[1]] = b_0 - b_1;
                        outputInTime[bitReversedLookUpTablePointer[2]] = b_2 + b_3;
                        outputInTime[bitReversedLookUpTablePointer[3]] = b_2 - b_3;
                    }
                    
                    {
                        const double b_0 = sf2[4] + sf2[6];
                        const double b_2 = sf2[4] - sf2[6];
                        const double b_1 = sf2[5] * 2;
                        const double b_3 = sf2[7] * 2;
                        
                        outputInTime[bitReversedLookUpTablePointer[4]] = b_0 + b_1;
                        outputInTime[bitReversedLookUpTablePointer[5]] = b_0 - b_1;
                        outputInTime[bitReversedLookUpTablePointer[6]] = b_2 + b_3;
                        outputInTime[bitReversedLookUpTablePointer[7]] = b_2 - b_3;
                    }
                    
                    sf2 += 8;
                    coefIndex += 8;
                    bitReversedLookUpTablePointer += 8;
                }
                while (coefIndex < m_length);
            }
        }
    }
    
    //Extreme case: 4 points
    else if (m_nbrBits == 2)
    {
        const double zeroPlusTwo = inputInFrequencies[0] + inputInFrequencies[2];
        const double zeroMinusTwo = inputInFrequencies[0] - inputInFrequencies[2];
        
        outputInTime[0] = zeroPlusTwo + inputInFrequencies[1] * 2;
        outputInTime[1] = zeroPlusTwo - inputInFrequencies[1] * 2;
        outputInTime[2] = zeroMinusTwo + inputInFrequencies[3] * 2;
        outputInTime[3] = zeroMinusTwo - inputInFrequencies[3] * 2;
    }
    
    //Extreme case: 2 points
    else if (m_nbrBits == 1)
    {
        outputInTime[0] = inputInFrequencies[0] + inputInFrequencies[1];
        outputInTime[0] = inputInFrequencies[0] - inputInFrequencies[1];
    }
    
    //Extreme case: 1 point
    else if (m_nbrBits == 0)
    {
        outputInTime[0] = inputInFrequencies[0];
    }
    
    //Invalid number of bits.
    else
    {
        assert(m_nbrBits >= 0);
    }
}
    

void MBT_FFTReal::rescale(double arrayToScale[]) const
{
    const double multiplicator = double (1.0 / m_length);
    long i = m_length - 1;
    
    do
    {
        arrayToScale[i] *= multiplicator;
        i--;
    }
    while (i >= 0);
}
    
    

MBT_FFTReal::MBT_BitReversedLookUpTable::MBT_BitReversedLookUpTable(const int nbrBits)
{
    long length = 1 << nbrBits; //2^nbrBits
    m_ptr = new long[length];
    
    long br_index = 0;
    m_ptr[0] = 0;
    
    for (long count = 1; count < length; count++)
    {
        long bit = length >> 1; //length/2
        
        while (((br_index ^= bit) & bit) == 0) {
            bit >>= 1; //bit/2
        }
        
        m_ptr[count] = br_index;
    }
    
}
        

MBT_FFTReal::MBT_BitReversedLookUpTable::~MBT_BitReversedLookUpTable()
{
    delete [] m_ptr;
    m_ptr = 0;
}
        

const long* MBT_FFTReal::MBT_BitReversedLookUpTable::get_ptr() const
{
    return m_ptr;
}
        

MBT_FFTReal::MBT_TrigonometricLookUpTable::MBT_TrigonometricLookUpTable(const int nbrBits)
{
    long totalLength;
    
    m_ptr = 0;
    
    if (nbrBits > 3)
    {
        totalLength = (1L << (nbrBits - 1)) - 4;
        m_ptr = new double [totalLength];
        
        const double PI = atan(1) * 4;
        for (int level = 3; level < nbrBits; level++)
        {
            const long level_len = 1L << (level - 1);
            double* const level_ptr = const_cast<double*> (get_ptr (level));
            const double mul = PI / (level_len << 1);
            
            for (long i = 0; i < level_len; i++)
            {
                level_ptr[i] = (double) cos(i * mul);
            }
        }
    }
}


MBT_FFTReal::MBT_TrigonometricLookUpTable::~MBT_TrigonometricLookUpTable()
{
    delete [] m_ptr;
    m_ptr = 0;
}
        

const double* MBT_FFTReal::MBT_TrigonometricLookUpTable::get_ptr(const int level) const
{
    return (m_ptr + (1L << (level - 1)) - 4);
}
