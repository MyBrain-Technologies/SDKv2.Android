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
//		Fixed issue with sort_indexes in SNR Stats
#define VERSION "1.1.1"
