//V1.0.0 : EG : initial version used in the mobile applications
//V1.1.0 : KP : Inclusion of new functions in the MBT_Preprocessing file
//              - a) function which puts the outliers to nan
//              - b) function which interpolates values between channels
//              - c) function which interpolates values across channels
//              - d) function which skips the possible NaN values that are present in a file
//              - e) function which removes possible remaining nan values in the beginning or the end of the MBT_Matrix
//              - Change all implicit type castings to explicit ones
//V1.1.1 : EG : Updated MBT_Matrix constructors to fully use Template model.
//		Redefined sort_indexes function to take template in input and unsigned long as output
//		Fixed a bug in nan interpolations where matrix size wasn't checked
//V1.1.2 : Change the type of the input in one function in MBT_Operations.h
//V1.1.3 : FG : Add a new function in MBT_Operations to compute derivative and add some parameters in MBT_PWelchComputer.
// 				Also, fix an error in MBT_frequencyBounds.
#define VERSION "1.1.3"