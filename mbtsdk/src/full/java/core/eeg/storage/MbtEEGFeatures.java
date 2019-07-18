package core.eeg.storage;

import android.support.annotation.Keep;

@Keep
public class MbtEEGFeatures {
    @Keep
    public enum Feature {

        MEDIAN,
        MEAN,
        VARIANCE,
        VRMS,
        VPP,
        SKEWNESS,
        KURTOSIS,
        INTEGRATED_EEG,
        MEAN_ABSOLUTE_VALUE,
        SIMPLE_SQUARE_INTEGRAL,
        V_ORDER_2_AND_3,
        LOG_DETECTOR,
        AVERAGE_AMPLITUDE_CHANGE,
        DIFFERENCE_ABSOLUTE_STANDARD_DEVIATION,
        NUMBER_OF_MAXIMA_AND_MINIMA,
        MOBILITY_HJORTH_PARAMETER,
        COMPLEXITY_HJORTH_PARAMETER,
        ZERO_CROSSING_RATE,
        ZERO_CROSSING_RATE_1ST_DERIVATIVE,
        ZERO_CROSSING_RATE_2ND_DERIVATIVE,
        VARIANCE_1ST_DERIVATIVE,
        VARIANCE_2ND_DERIVATIVE,
        NON_LINEAR_ENERGY,

        MAXIMUM_DELTA_FREQUENCY_BAND,
        KURTOSIS_DELTA_FREQUENCY_BAND,
        STANDARD_DEVIATION_DELTA_FREQUENCY_BAND,
        SKEWNESS_DELTA_FREQUENCY_BAND,
        MAXIMUM_THETA_FREQUENCY_BAND,
        KURTOSIS_THETA_FREQUENCY_BAND,
        STANDARD_DEVIATION_THETA_FREQUENCY_BAND,
        SKEWNESS_THETA_FREQUENCY_BAND,
        MAXIMUM_ALPHA_FREQUENCY_BAND,
        KURTOSIS_ALPHA_FREQUENCY_BAND,
        STANDARD_DEVIATION_ALPHA_FREQUENCY_BAND,
        SKEWNESS_ALPHA_FREQUENCY_BAND,
        MAXIMUM_BETA_FREQUENCY_BAND,
        KURTOSIS_BETA_FREQUENCY_BAND,
        STANDARD_DEVIATION_BETA_FREQUENCY_BAND,
        SKEWNESS_BETA_FREQUENCY_BAND,
        MAXIMUM_GAMMA_FREQUENCY_BAND,
        KURTOSIS_GAMMA_FREQUENCY_BAND,
        STANDARD_DEVIATION_GAMMA_FREQUENCY_BAND,
        SKEWNESS_GAMMA_FREQUENCY_BAND,

        ZERO_PADDING,
        ONE_SIDED_SPECTRUM,

        DELTA_RATIO,
        DELTA_POWER,
        LOG_DELTA_POWER,
        NORMALIZED_DELTA_POWER,

         THETA_RATIO,
         THETA_POWER,
         LOG_THETA_POWER,
         NORMALIZED_THETA_POWER,

         ALPHA_RATIO,
         ALPHA_POWER,
         LOG_ALPHA_POWER,
         NORMALIZED_ALPHA_POWER,

         BETA_RATIO,
         BETA_POWER,
         LOG_BETA_POWER,
         NORMALIZED_BETA_POWER,

        GAMMA_RATIO,
        GAMMA_POWER,
        LOG_GAMMA_POWER,
        NORMALIZED_GAMMA_POWER,

        SPECTRAL_EDGE_FREQUENCY_80,
        SPECTRAL_EDGE_FREQUENCY_90,
        SPECTRAL_EDGE_FREQUENCY_95,

        FREQUENCY_FILTERED_BAND_ENERGIES,
        RELATIVE_SPECTRAL_DIFFERENCE,
        SN_RATIO,
        POWER_SPECTRUM_MOMENTS,
        MODIFIED_MEDIAN_FREQUENCY,
        MODIFIED_MEAN_FREQUENCY,
        SPECTRAL_ENTROPY
    }


    public enum Frequency {

        DELTA(Feature.DELTA_RATIO, Feature.DELTA_POWER, Feature.LOG_DELTA_POWER, Feature.NORMALIZED_DELTA_POWER),
        THETA(Feature.THETA_RATIO, Feature.THETA_POWER, Feature.LOG_THETA_POWER, Feature.NORMALIZED_THETA_POWER),
        ALPHA(Feature.ALPHA_RATIO, Feature.ALPHA_POWER, Feature.LOG_ALPHA_POWER, Feature.NORMALIZED_ALPHA_POWER),
        BETA(Feature.BETA_RATIO, Feature.BETA_POWER, Feature.LOG_BETA_POWER, Feature.NORMALIZED_BETA_POWER),
        GAMMA(Feature.GAMMA_RATIO, Feature.GAMMA_POWER, Feature.LOG_GAMMA_POWER, Feature.NORMALIZED_GAMMA_POWER);

        private Feature ratio;
        private Feature power;
        private Feature logPower;
        private Feature normalizedPower;

        Frequency(Feature ratio, Feature power, Feature logPower, Feature normalizedPower) {
            this.ratio = ratio;
            this.power = power;
            this.logPower = logPower;
            this.normalizedPower = normalizedPower;
        }

        public Feature getRatio() {
            return ratio;
        }

        public Feature getPower() {
            return power;
        }

        public Feature getLogPower() {
            return logPower;
        }

        public Feature getNormalizedPower() {
            return normalizedPower;
        }

        public int getRatioIndex() {
            return ratio.ordinal();
        }

        public int getPowerIndex() {
            return power.ordinal();
        }

        public int getLogPowerIndex() {
            return logPower.ordinal();
        }

        public int getNormalizedPowerIndex() {
            return normalizedPower.ordinal();
        }
    }
}
