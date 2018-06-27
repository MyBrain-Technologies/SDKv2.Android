//V1.0.0 : EG : initial version used in the mobile applications
//V1.1.0 : KP : Change of the preprocessing steps and change all implicit type castings to explicit ones
//              - The data from the quality checker may be nan in the cases of i) lost packets, ii) quality = 0
//              - a)Apply an IQR and find the bounds for the outliers on the Calibration data. Put all outliers to nan.
//              - b)Interpolate the channels between themselves.
//              - c)Interpolate separately each channel. Remove nan (if present) from the begining/end of the matrix
//              - d)Apply DC removal and bandpass on calibration data.
//              - Send the bounds from calibration to session, put all outliers to nan and repeat steps b)-d) for each 4 seconds of data on sliding windows
//              - Change all implicit type castings to explicit ones
//		   FG : In the QualityChecker
//				- Change the output of the Quality Checker in terms of EEG : either raw EEG (without interpolation) or NaN if the EEG segment has bad quality.
//				- Change all implicit type castings to explicit ones
//
//
//V1.1.1 : EG : Updated signal processing library to version 1.1.1
//		        Fixed issue with sort_indexes in SNR Stats
//V2.0.0 : FG&KP : Change of the preprocessing steps
//               - For the calibration and session the steps are the same
//                  - For the best channel* (or a mix if NaN in 4 consecutive seconds) and at each second, for 4 seconds of data
//                  - a) Interpolate linearly nan values across each channel (nan values can result from i) packet losses, ii) bad data)
//                  - b) Remove possible remaining nan values (in the beginning and/or end of the signal)
//                  - c) DC removal
//                  - d) Bandpass (2 Hz-30 Hz)
//                  - e) Calculate the bounds for the outliers and put outliers to nan
//                  - f) Interpolate the nan values across the channel
//                  - g) Remove possible remaining nan values (in the beginning and/or end of the signal)
//                  - h) Compute SNR
//                  - i) Smooth SNR
//                  - j) Map the values of step i) to [0,1] using a newly introduced function
//         FG : Introduction of a new function for mapping the Smoothed SNR values online to [0,1]
//         KP : - Calculation of the area below a threshold
//                 - New way for calculating the percentages in exercise Journey to increase the achieved results of the users
//                 - Fixing bugs in the calculation of the area above a threshold
//                 - Better estimation of the results for the exercise Stability
//                 - Change the type of the input vector in sort_indexes in SP repo in MBT_Operations.h
//V2.1.0 : KP : - In file "MBT_SNT_Stats.cpp"
//                 1) Fix a bug for Ycross_down
//				   2) Change the place in the code where we calculate Performance
// 				   3) Sort KeepXcross_up and KeepYcross_up based on the sorted KeepXcross_up
//				   4) Change the way we calculate the overall area and go back to the implementation of the sum of the areas above the threshold to the overall area
//V2.2.0 : FG : - Change the way we compute the SNR in NF_Melomind:
//				   i) new way to compute the spectral background noise --> new function: MBT_ComputeNoise.cpp and MBT_ComputeNoise.h
//				   ii) new way to detect the alpha peak --> modifications of MBT_ComputeSNR
//				   iii) new way to compute SNR: based on both channels and not on the best channel --> modification of the inputs and outputs
//						of MBT_ComputeCalibration and MBT_ComputeRelaxIndex
//				  Note: this version used the preprocessing of the v2.1.0 versionning but ignoring the step concerning the choice of values from
//						the other channel when the EEG values of the best channel are NaN. Indeed, we have not anymore the notion of best channel
//						because we compute SNR on both channel and not only on the best channel.
//				- Updated signal processing library to version 1.1.3
//V2.2.1 : GF&EG : Optimize the way we use histFreq.  Optimized the way we compute calibration and relax index. Added references in input to avoid
//				   too many collections manipulations
//V2.2.2 : FG : Fix a problem of none kept packet even if good calibration when the rule "at least one channel has an averaged quality >= 0.5" is satisfied.
//V2.3.0 : FG: The quality checker is able to discriminate muscle artifacts from other artifacts.
//             It is also able to discriminate bad EEG signal from no EEG signal (headset on the table or headset on the head without electrodes).
//             This release also remodified the feature that decreases the performance of the Quality Checker (nb_max_min).
//V2.3.1 : FG & AL : 1) Add the smoothing length of the relax index in input in order to be able to choose the time length.
//					 2) Manage a possible exception in the detection of the bounds of the alpha peak in MBT_ComputeSNR.cpp
//					 3) Manage the case where the spectrum is NaN (may happen when the signal is constant) in MBT_ComputeSNR.cpp
//					 4) Add a possibility to filter the data (bandpass) in the Quality Checker process.
#include "SignalProcessing.Cpp/version.h"  // version.h of the submodule SignalProcessing.cpp
#define VERSION_SP SP_VERSION
#define VERSION "2.3.1"

