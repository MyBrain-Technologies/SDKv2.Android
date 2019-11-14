package core.eeg.storage;

import static core.eeg.storage.Feature.*;

public enum FrequencyBand {

    DELTA(DELTA_RATIO, DELTA_POWER, LOG_DELTA_POWER, NORMALIZED_DELTA_POWER,
            MAXIMUM_DELTA_FREQUENCY_BAND, KURTOSIS_DELTA_FREQUENCY_BAND, STANDARD_DEVIATION_DELTA_FREQUENCY_BAND, SKEWNESS_DELTA_FREQUENCY_BAND),

    THETA(THETA_RATIO, THETA_POWER, LOG_THETA_POWER, NORMALIZED_THETA_POWER,
            MAXIMUM_THETA_FREQUENCY_BAND, KURTOSIS_THETA_FREQUENCY_BAND, STANDARD_DEVIATION_THETA_FREQUENCY_BAND, SKEWNESS_THETA_FREQUENCY_BAND),

    ALPHA(ALPHA_RATIO, ALPHA_POWER, LOG_ALPHA_POWER, NORMALIZED_ALPHA_POWER,
            MAXIMUM_ALPHA_FREQUENCY_BAND, KURTOSIS_ALPHA_FREQUENCY_BAND, STANDARD_DEVIATION_ALPHA_FREQUENCY_BAND, SKEWNESS_ALPHA_FREQUENCY_BAND),

    BETA(BETA_RATIO, BETA_POWER, LOG_BETA_POWER, NORMALIZED_BETA_POWER,
            MAXIMUM_BETA_FREQUENCY_BAND, KURTOSIS_BETA_FREQUENCY_BAND, STANDARD_DEVIATION_BETA_FREQUENCY_BAND, SKEWNESS_BETA_FREQUENCY_BAND),

    GAMMA(GAMMA_RATIO, GAMMA_POWER, LOG_GAMMA_POWER, NORMALIZED_GAMMA_POWER,
            MAXIMUM_GAMMA_FREQUENCY_BAND, KURTOSIS_GAMMA_FREQUENCY_BAND, STANDARD_DEVIATION_GAMMA_FREQUENCY_BAND, SKEWNESS_GAMMA_FREQUENCY_BAND);

    private Feature ratio;
    private Feature power;
    private Feature logPower;
    private Feature normalizedPower;
    private Feature maximum;
    private Feature kurtosis;
    private Feature standardDeviation;
    private Feature skewness;

    FrequencyBand(Feature ratio, Feature power, Feature logPower, Feature normalizedPower,
                  Feature maximum, Feature kurtosis, Feature standardDeviation, Feature skewness) {
        this.ratio = ratio;
        this.power = power;
        this.logPower = logPower;
        this.normalizedPower = normalizedPower;
        this.maximum = maximum;
        this.kurtosis = kurtosis;
        this.standardDeviation = standardDeviation;
        this.skewness = skewness;
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

    public Feature getMaximum() {
        return maximum;
    }

    public Feature getKurtosis() {
        return kurtosis;
    }

    public Feature getStandardDeviation() {
        return standardDeviation;
    }

    public Feature getSkewness() {
        return skewness;
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

    public int getMaximumIndex() {
        return maximum.ordinal();
    }

    public int getKurtosisIndex() {
        return kurtosis.ordinal();
    }

    public int getStandardDeviationIndex() {
        return standardDeviation.ordinal();
    }

    public int getSkewnessIndex() {
        return skewness.ordinal();
    }
}