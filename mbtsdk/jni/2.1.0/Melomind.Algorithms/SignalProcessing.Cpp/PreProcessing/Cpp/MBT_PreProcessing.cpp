//
// MBT_PreProcess.cpp
//
// Created by Katerina Pandremmenou on 08/12/2016
// Copyright (c) 2016 myBrain Technologies. All rights reserved.
//
// Update on 09/12/2016 by Katerina Pandremmenou : double-->double
// Update on 03/02/2017 by Fanny Grosselin : Add a method that interpolates the outliers
// Update on 23/03/2017 by Fanny Grosselin : Change float by double for the functions not directly used by Andro�d. For the others, keep inputs and outputs in float, but do the steps with double or create two functions : one with only float, another one with only double
// Update on 27/03/2017 by Fanny Grosselin : Fix all the warnings.
// Update on 31/03/2017 by Katerina Pandremmenou : Inclusion of RemoveDCF2D function
// Update on 03/04/2017 by Katerina Pandremmenou : Inclusion of CalculateBounds function for doubles
// Update on 14/09/2017 by Katerina Pandremmenou : Inclusion of OutliersToNan function
// Update on 18/09/2017 by Katerina Pandremmenou : Inclusion of MBT_InterpolateBetweenChannels and MBT_InterpolateAcrossChannels functions
// Update on 19/09/2017 by Katerina Pandremmenou : Change the implicit type casting to an explicit one in line 109
// Update on 21/09/2017 by Katerina Pandremmenou : Inclusion of i)a function to skip NaN values and ii)remove possible NaN values in the beggining or the end of an MBT_Matrix
#include <android/log.h>

#include "../Headers/MBT_PreProcessing.h"
#include "../../Algebra/Headers/MBT_Operations.h"
#include <algorithm>
#include <functional>
#include <iostream>
#include <vector>
#include <iomanip>
#include <math.h>
#include <numeric>

using namespace std;

vector<float> RemoveDC(vector<float> Data)
{
    vector<double> tmp_Data(Data.begin(), Data.end());
	double meanData = mean(tmp_Data);
    transform(tmp_Data.begin(), tmp_Data.end(), tmp_Data.begin(), bind2nd(minus<double>(), meanData));
    vector<float> returnData(tmp_Data.begin(), tmp_Data.end());
    return returnData;
}

vector<double> RemoveDC(vector<double> Data)
{
	double meanData = mean(Data);
    transform(Data.begin(), Data.end(), Data.begin(), bind2nd(minus<double>(), meanData));
    return Data;
}

vector<double> RemoveDCF2D(vector<float> Data)
{
	vector<double> tmp_Data(Data.begin(), Data.end());
	double meanData = mean(tmp_Data);
	transform(tmp_Data.begin(), tmp_Data.end(), tmp_Data.begin(), bind2nd(minus<double>(), meanData));
    return tmp_Data;
}

vector<float> CalculateBounds(vector<float> DataNoDC)
{
    vector<double> tmp_DataNoDC(DataNoDC.begin(), DataNoDC.end());
	vector<double> quants = Quantile(tmp_DataNoDC);
	double range = quants.back() - quants.front();
	double LowBound = quants.front() - 1.5 * range;
	double UpBound = quants.back() + 1.5 * range;

	vector<float> Bounds = {(float)LowBound, (float)UpBound};
	return Bounds;
}

vector<double> CalculateBounds(vector<double> DataNoDC)
{
    vector<double> quants = Quantile(DataNoDC);
    double range = quants.back() - quants.front();
    double LowBound = quants.front() - 1.5 * range;
    double UpBound = quants.back() + 1.5 * range;

    vector<double> Bounds = {LowBound, UpBound};
    return Bounds;
}

vector<double> RemoveOutliers(vector<double> DataNoDC, vector<double> Bounds)
{
	vector<double> GetBack;

    for (unsigned int tmp = 0 ; tmp < DataNoDC.size(); tmp++)
    {
    	if ( (DataNoDC[tmp] >= Bounds[0]) && (DataNoDC[tmp] <= Bounds[1]) )
    		GetBack.push_back(DataNoDC[tmp]);
    }

	return GetBack;
}

vector<double> InterpolateOutliers(vector<double> DataNoDC, vector<double> Bounds)
{
    vector<double> DataInterpolatedOutliers;
    vector<double> x, y, xInterp;
    for (unsigned int tmp = 0 ; tmp < DataNoDC.size(); tmp++)
    {
        DataInterpolatedOutliers.push_back(DataNoDC[tmp]);
    	if ( (DataNoDC[tmp] >= Bounds[0]) && (DataNoDC[tmp] <= Bounds[1]) )
        {
            x.push_back(tmp);
    		y.push_back(DataNoDC[tmp]);
        }
        else
        {
            xInterp.push_back(tmp);
        }
    }
    vector<double> InterpolatedOutliers = MBT_linearInterp(x, y, xInterp);
    for (unsigned int d = 0; d<InterpolatedOutliers.size(); d++)
    {
        DataInterpolatedOutliers[(int)xInterp[d]] = InterpolatedOutliers[d];
    }
	return DataInterpolatedOutliers;
}

vector<double> Quantile(vector<double>& CalibNoDC)
{
	const vector<double> probs = {0.25, 0.5, 0.75};

    vector<double> data = CalibNoDC;
    sort(data.begin(), data.end());
    vector<double> quantiles;

    for (size_t i = 0; i < probs.size(); ++i)
    {
        double center = Lerp(-0.5, data.size() - 0.5, probs[i]);

        size_t left = max(int64_t(floor(center)), int64_t(0));
        size_t right = min(int64_t(ceil(center)), int64_t(data.size() - 1));

        double datLeft = data.at(left);
        double datRight = data.at(right);

        double quantile = Lerp(datLeft, datRight, center - left);

        quantiles.push_back(quantile);
    }

    return quantiles;
}

double Lerp(double v0, double v1, double t)
{
    return (1 - t)*v0 + t*v1;
}

vector<double> detrend(vector<double> original, const double fs)
{
    int i;
    int n = original.size();
	vector<double> helper;
	vector<double> differences(n);

	for (i = 0; i < n; i++)
		helper.push_back(i/fs);

    const auto s_x  = accumulate(helper.begin(), helper.end(), 0.0);
    const auto s_y  = accumulate(original.begin(), original.end(), 0.0);
    const auto s_xx = inner_product(helper.begin(), helper.end(), helper.begin(), 0.0);
    const auto s_xy = inner_product(helper.begin(), helper.end(), original.begin(), 0.0);

    //slope
    const auto a    = (n * s_xy - s_x * s_y) / (n * s_xx - s_x * s_x);

	//intercept
	const auto b = (s_xx * s_y - s_x * s_xy) / (s_xx * n - s_x * s_x);

    // ax
    transform(helper.begin(), helper.end(), helper.begin(), bind1st(multiplies<double>(), a));
    // +b
    transform(helper.begin(), helper.end(), helper.begin(), bind1st(plus<double>(), b));
    // measured - estimated
    transform (original.begin(), original.end(), helper.begin(), differences.begin(), minus<double>());

    return differences;
}

double calculateGain(vector<double> const& original, vector<double> const& amplified)
{
	vector<double> results(original.size());

    vector<double> sub_mean_orig(original.size());
    vector<double> sub_mean_ampl(amplified.size());

    // mean of the first signal (original)
    double sum_orig = accumulate(original.begin(), original.end(), 0.0);
	double mean_orig = sum_orig / original.size();
    //cout << "mean_orig = " << fixed << setprecision(6) << mean_orig << endl;
    // subtract from the first signal its mean
    transform(original.begin(), original.end(), sub_mean_orig.begin(), bind2nd(minus<double>(), mean_orig));

    //standard deviation of the first signal (original)
    //double sq_sum_orig = std::inner_product(sub_mean_orig.begin(), sub_mean_orig.end(), sub_mean_orig.begin(), 0.0);
	//double stdev_orig = sqrt(sq_sum_orig / sub_mean_orig.size());
    //cout << "stdev_orig = " << fixed << setprecision(6) << stdev_orig << endl;

    // mean of the second signal (amplified)
    double sum_ampl = accumulate(amplified.begin(), amplified.end(), 0.0);
	double mean_ampl = sum_ampl / amplified.size();
    // cout << "mean_ampl = " << fixed << setprecision(6) << mean_ampl << endl;
	// subtract from the second signal its mean
    transform(amplified.begin(), amplified.end(), sub_mean_ampl.begin(), bind2nd(minus<double>(), mean_ampl));

    // divide the two signals
    transform (sub_mean_orig.begin(), sub_mean_orig.end(), sub_mean_ampl.begin(), results.begin(), divides <double>());
    // take the mean of the resulted signal
    double sum_results = accumulate(results.begin(), results.end(), 0.0);
	double mean_results = sum_results / results.size();

    return mean_results;
}

vector<double> MBT_remove(vector<double> signalToRemove)
{
    vector<double> signalRem;
    signalRem.assign(signalToRemove.size(),nan(" "));
    return signalRem;
}

vector<double> MBT_correctArtifact(vector<double> signalToCorrect)
{
    vector<double> signalCorr = signalToCorrect; // a modifier
    return signalCorr;
}

vector<double> MBT_OutliersToNan (vector<double> SignalWithOutliers, vector<double> Bounds)
{
    for (unsigned int tmp = 0 ; tmp < SignalWithOutliers.size(); tmp++)
    {
        if ( (SignalWithOutliers[tmp] < Bounds[0]) || (SignalWithOutliers[tmp] > Bounds[1]) )
            SignalWithOutliers[tmp] = nan(" ");
    }
    return SignalWithOutliers;
}

MBT_Matrix<float> MBT_InterpolateBetweenChannels(MBT_Matrix<float> MatricesToInterpolate)
{
    if(MatricesToInterpolate.size().first >= 2){
        for (unsigned int i = 0 ; i < (unsigned) MatricesToInterpolate.size().second; i++)
        {
            if (isnan(MatricesToInterpolate(0,i)) && !isnan(MatricesToInterpolate(1,i)))
                MatricesToInterpolate(0,i) = MatricesToInterpolate(1,i);
            else if (!isnan(MatricesToInterpolate(0,i)) && isnan(MatricesToInterpolate(1,i)))
                MatricesToInterpolate(1,i) = MatricesToInterpolate(0,i);
        }
    }

    return MatricesToInterpolate;
}

MBT_Matrix<float> MBT_InterpolateAcrossChannels(MBT_Matrix<float> ChannelsToInterpolate)
{
    vector<double> posNaN, x, posx; 
    
    for (int r1 = 0; r1 < ChannelsToInterpolate.size().first; r1++)
    {
        int cnt = 0;
        for (int r2 = 0; r2 < ChannelsToInterpolate.size().second; r2++)
        {
            if (isnan(ChannelsToInterpolate(r1,r2)))
            {
                posNaN.push_back(r2);
            }
            else
            {
                x.push_back(ChannelsToInterpolate(r1,r2));
                posx.push_back(r2);
            }
        }
        vector<double> InterpolatedOutliers = MBT_linearInterp(posx, x, posNaN);

        for (int r2 = 0; r2 < ChannelsToInterpolate.size().second; r2++)
        {
            if (isnan(ChannelsToInterpolate(r1,r2)))
            {
                ChannelsToInterpolate(r1,r2) = (float) InterpolatedOutliers[cnt];
                cnt = cnt + 1;
            }
        }
    }
    return ChannelsToInterpolate;
}

vector<float> SkipNaN(vector<float> Input)
{
  vector<float> InputNoNaN;
  for (unsigned int a = 0 ; a < Input.size(); a++)
  {
    if (!isnan(Input[a]))
      InputNoNaN.push_back(real(Input[a]));
  }

  return InputNoNaN;
}

MBT_Matrix<float> RemoveNaNIfAny(MBT_Matrix<float> InterpolatedAcrossChannels) 
{
    vector<int> sizes(InterpolatedAcrossChannels.size().first);
    for (int r1 = 0 ; r1 < InterpolatedAcrossChannels.size().first; r1++)
    {
       vector<float> separateRow = SkipNaN(InterpolatedAcrossChannels.row(r1));
       sizes[r1] = separateRow.size();
    }
    
    MBT_Matrix<float> DataWithoutNaN(InterpolatedAcrossChannels.size().first, *min_element(sizes.begin(), sizes.end()));

    for (int r1 = 0 ; r1 < DataWithoutNaN.size().first; r1++)
    {
        vector<float> separateRow = SkipNaN(InterpolatedAcrossChannels.row(r1));
        for (int r2 = 0 ; r2 < DataWithoutNaN.size().second; r2++)
        {
            DataWithoutNaN(r1,r2) = separateRow[r2];
        }
    }
    
    return DataWithoutNaN;
}