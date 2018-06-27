 #!/bin/bash
g++ -std=c++11 main.cpp MBT_SNR_Stats.cpp ../../../SignalProcessing.Cpp/Algebra/Cpp/MBT_Operations.cpp ../../../SignalProcessing.Cpp/Transformations/Cpp/MBT_Fourier_fftw3.cpp -O3 -lfftw3 -lm -Wall -Wextra -Wfloat-conversion -g -o exec


