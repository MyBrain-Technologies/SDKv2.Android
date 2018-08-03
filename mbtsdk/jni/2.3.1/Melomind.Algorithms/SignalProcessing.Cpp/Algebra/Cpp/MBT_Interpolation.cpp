//
//  MBT_Interpolation.cpp
//
//  Created by Fanny Grosselin on 03/01/2017.
//  Copyright (c) 2017 myBrain Technologies. All rights reserved.
//
//  Update: Fanny Grosselin 2017/03/22 --> Manage boundaries cases of the linear interpolation
// 			Fanny Grosselin 23/03/2017 --> Change float by double
//			Fanny Grosselin 2017/03/27 --> Change '\' by '/' for the paths
//          Fanny Grosselin 2017/03/27 --> Fix all the warnings.
//          Fanny Grosselin 2017/06/09 --> Fix the case when x is empty (interpolation is impossible)
//

#include "../Headers/MBT_Interpolation.h"

std::vector<double> MBT_linearInterp(std::vector<double> &x, std::vector<double> &y, std::vector<double> &xInterp)
{
    std::vector<double> yInterp;
    double dx, dy, a, b;


    if (x.size() == 0)
    {
        yInterp.assign(xInterp.size(),nan(" "));
    }
    else
    {
        yInterp.reserve(xInterp.size());
        for (unsigned int i = 0; i < xInterp.size(); ++i)
        {
            int idx = MBT_FindClosest::findClosestIndex( x, xInterp[i] ); // idx = indice de x le plus proche de xinterp[i] (l'indice dont la valeur est interpoler)
            int X_size = x.size();
            if ((xInterp[i] < x[0]) | (xInterp[i] > x[X_size-1]))
            {
                yInterp.push_back(nan(" "));
            }
            else
            {
                if (x[idx] > xInterp[i]) // si cet indice plus proche est superieur à l'indice à interpoler
                {
                    dx = (x[idx] - x[idx - 1]) ;
                    dy = (y[idx] - y[idx - 1]) ;
                }
                else // si cet indice plus proche est inférieur ou égal à l'indice à interpoler
                {
                    dx = (x[idx + 1] - x[idx]) ;
                    dy = (y[idx + 1] - y[idx]) ;
                }


                a = dy / dx;
                b = y[idx] - a * x[idx];
                yInterp.push_back(a * xInterp[i] + b);
            }
        }
    }
    return yInterp;
}
