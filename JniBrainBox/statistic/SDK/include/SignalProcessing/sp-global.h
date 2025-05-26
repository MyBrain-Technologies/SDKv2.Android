#ifndef __SIGNAL_PROCESSING_GLOBAL_H__
#define __SIGNAL_PROCESSING_GLOBAL_H__

#include <sp-config.h>

#include <vector>
#include <complex>
#include <limits>

/* Maybe other global definitions… */
#if defined(SP_ENABLE_FLOAT)
typedef float SP_RealType;
#elif defined(SP_ENABLE_LONG_DOUBLE)
typedef long double SP_RealType;
#elif defined(SP_ENABLE_QUAD_PRECISION)
typedef __float128 SP_RealType;
#else
typedef double SP_RealType;
#endif

/* Legacy mode, enabling compatibility with old SignalProcessing usage */
#if defined(SP_LEGACY)
    typedef float SP_FloatType;
#else
    typedef SP_RealType SP_FloatType;
#endif

#if defined(SP_ENABLE_FLOAT) || !defined(SP_LEGACY)
    #define SP_FLOAT_OR_NOT_LEGACY 1
#endif

typedef std::complex<SP_RealType> SP_Complex;

typedef std::vector<SP_RealType> SP_Vector;
typedef std::vector<SP_Complex> SP_ComplexVector;

// Retrocompatibility type definitions, remove when finishing full refactoring
typedef std::complex<SP_FloatType> SP_ComplexFloat;

typedef std::vector<SP_FloatType> SP_FloatVector;
typedef std::vector<SP_ComplexFloat> SP_ComplexFloatVector;

// Declare NAN and INF values
const std::string SP_INF_STR = "inf";
const std::string SP_MINF_STR = "-inf";
const SP_RealType SP_INF = std::numeric_limits<SP_RealType>::infinity();
const SP_FloatType SP_INFFLOAT = std::numeric_limits<SP_FloatType>::infinity();

const std::string SP_NAN_STR = "nan";
const SP_RealType SP_NAN = std::numeric_limits<SP_RealType>::quiet_NaN();
const std::string SP_NANFLOAT_STR = "nanf";
const SP_FloatType SP_NANFLOAT = std::numeric_limits<SP_FloatType>::quiet_NaN();

// Compute and define PI depending on type definitions
static SP_RealType const_pi() { return std::atan(static_cast<SP_RealType>(1)) * 4; }
static const SP_RealType SP_PI = const_pi();
static const SP_FloatType SP_PIFLOAT = SP_PI;
static const SP_RealType SP_PIFROMFLOAT= SP_PIFLOAT; // TODO : replace this wrong usage when refactoring


#endif // __SIGNAL_PROCESSING_GLOBAL_H__