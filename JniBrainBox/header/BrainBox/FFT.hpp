#pragma once
#include <vector>
#include <complex>

namespace brainbox {

namespace fft {
	void cleanup();

	// generic function for applying zero padding
	// use type ==  1 for zero padding at the beginning of a vector
	// use type == -1 for zero padding at the end of a vector
	template <typename T>
	inline std::vector<T> zero_padding(std::vector<T> vec_to_be_zero_padded, int length, int type); 
}

template <typename T>
class FFTW_C2C_1D
{
	public:
		// depending on the tmp values, we allocate memory and create a plan
		// either for forward or backward fourier transform
		FFTW_C2C_1D(int size, const std::vector<std::complex<T>>& vec, int tmp);
		// we destroy the plans and free the pointers
		~FFTW_C2C_1D();
		// we apply forward fourier transform
		std::vector<std::complex<T>> fft_execute();
		// we apply backward fourier transform
		std::vector<std::complex<T>> ifft_execute();

	private:
		struct Data;
		Data *data_;
};

template <typename T>
class FFTW_C2R_1D
{
	public:
		FFTW_C2R_1D(int size, const std::vector<std::complex<T>>& vec);
		~FFTW_C2R_1D();

		// we apply inverse fourier transform
		std::vector<T> execute();
	private:
		struct Data;
		Data *data_;
};

} // namespace brainbox