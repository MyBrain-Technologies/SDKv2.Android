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
//            FG : Introduction of a new function for mapping the Smoothed SNR values online to [0,1]  
//            KP : - Calculation of the area below a threshold
//                 - New way for calculating the percentages in exercise Journey to increase the achieved results of the users
//                 - Fixing bugs in the calculation of the area above a threshold
//                 - Better estimation of the results for the exercise Stability
//                 - Change the type of the input vector in sort_indexes in SP repo in MBT_Operations.h
//V2.1.0 :    KP : - In file "MBT_SNT_Stats.cpp"
//                 1) Fix a bug for Ycross_down
//				   2) Change the place in the code where we calculate Performance
// 				   3) Sort KeepXcross_up and KeepYcross_up based on the sorted KeepXcross_up
//				   4) Change the way we calculate the overall area and go back to the implementation of the sum of the areas above the threshold to the overall area
#define VERSION "2.1.0"
