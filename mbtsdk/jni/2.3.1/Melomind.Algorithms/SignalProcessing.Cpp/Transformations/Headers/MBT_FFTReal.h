//
//  MBT_FFTReal.h
//  MBT.iOS
//
//  Created by Emma Barme on 12/10/2015.
//  Copyright (c) 2015 Emma Barme. All rights reserved.
//
//  Code inspired by SBS2 project: A. Stopczynski, J. E. Larsen, C. Stahlhut, M. K. Petersen, & L. K. Hansen (2011), A smartphone interface for a wireless EEG headset with real-time 3D reconstruction, Affective Computing and Intelligent Interaction (ACII 2011), Lecture Notes in Computer Science (LNCS) 6357, Springer-Verlag Berlin Heidelberg, pp.317-318.
//
// 	Update: Fanny Grosselin 23/03/2017 --> Change float by double
#ifndef __MBT_iOS__MBT_FFTReal__
#define __MBT_iOS__MBT_FFTReal__

#include <stdio.h>
#include <cmath>
#include <cassert>
#include <errno.h>

//Fourier transformation of real number arrays. Comments in the functions body are severely lacking.
class MBT_FFTReal {
    
public:
    
    /*
     * @brief MBT_FFTReal constructor.
     * @param length The length of the array on which to do an FFT. Must be a strictly positive power of 2.
     * @throws std::bad_alloc, anything.
     */
    MBT_FFTReal(const long length);
    
    /*
     * @brief MBT_FFTReal destructor.
     */
    ~MBT_FFTReal();
    
    /*
     * @brief Compute the FFT of the array
     * @param inputInTime Pointer to the source array (data by time).
     * @param outputInFrequencies Pointer to the destination array (data by frequency). Indexes 0 to length(inputInTime)/2 are real values, indexes from length(inputInTime)/2+1 to length(inputInTime)-1 are imaginary values of coefficents 1 to length(inputInTime)/2-1.
     * @throws anything.
     */
    void do_fft (const double inputInTime[], double outputInFrequencies[]) const;
    
    /*
     * @brief Compute the inverse FFT of the array.
     * Note that IFFT (FFT (x)) = x * length (x). Data must be post-scaled.
     * @param inputInFrequencies Pointer to the source array (data by frequency). Indexes 0 to length(outputInTime)/2 are real values, indexes from length(outputInTime)/2+1 to length(outputInTime)-1 are imaginary values of coefficents 1 to length(outputInTime)/2-1.
     * @param outputInTime Pointer to the destination array (data by time).
     */
    void do_ifft(const double inputInFrequencies[], double outputInTime[]) const;
    
    /*
     * @brief Scale an array by dividing each element by the array's length
     * @param arrayToScale Pointer to the array to rescale.
     */
    void rescale(double arrayToScale[]) const;
    
    
private:
    
    //MBT_FFTReal nested class: Bit-reversed look-up table.
    class MBT_BitReversedLookUpTable {
    public:
        
        explicit MBT_BitReversedLookUpTable(const int nbrBits);
        
        /*
         * @brief MBT_BitReversedLookupTable destructor.
         */
        ~MBT_BitReversedLookUpTable();
        
        /*
         * @brief m_ptr getter.
         */
        const long* get_ptr() const;
        
    private:
        
        long* m_ptr;
    };
    
    //MBT_FFTReal nested class: Trigonometric look-up table.
    class MBT_TrigonometricLookUpTable {
    public:
        
        explicit MBT_TrigonometricLookUpTable(const int nbrBits);
        
        /*
         * @brief MBT_TrigonometricLookUpTable destructor.
         */
        ~MBT_TrigonometricLookUpTable();
        
        /*
         * @brief m_ptr getter.
         */
        const double* get_ptr(const int level) const;
        
    private:
        
        double* m_ptr;
    };
    
    
    const MBT_BitReversedLookUpTable m_bitReversedLookUpTable;
    
    const MBT_TrigonometricLookUpTable m_trigonometricLookUpTable;
    
    const double m_sqrt;
    
    const long m_length;
    
    const int m_nbrBits;
    
    double* m_buffer;
    
    
    /*
     * @brief MBT_FFTReal copy constructor.
     * @param other Reference to the MBT_FFTReal to be copied.
     */
    MBT_FFTReal(const MBT_FFTReal &other);
    
    /* Where are those defined.
    const FFTReal&	operator = (const FFTReal &other);
    int				operator == (const FFTReal &other);
    int				operator != (const FFTReal &other);
     */
};


#endif /* defined(__MBT_iOS__MBT_FFTReal__) */
