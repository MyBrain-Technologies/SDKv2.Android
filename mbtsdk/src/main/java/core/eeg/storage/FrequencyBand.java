package core.eeg.storage;

import android.support.annotation.Keep;

import static core.eeg.storage.Feature.*;

@Keep
public enum FrequencyBand {

    DELTA(DELTA_RATIO, DELTA_POWER, LOG_DELTA_POWER, NORMALIZED_DELTA_POWER,
            DELTA_MAXIMUM, DELTA_KURTOSIS, DELTA_STANDARD_DEVIATION, DELTA_SKEWNESS),

    THETA(THETA_RATIO, THETA_POWER, LOG_THETA_POWER, NORMALIZED_THETA_POWER,
            THETA_MAXIMUM, THETA_KURTOSIS, THETA_STANDARD_DEVIATION, THETA_SKEWNESS),

    ALPHA(ALPHA_RATIO, ALPHA_POWER, LOG_ALPHA_POWER, NORMALIZED_ALPHA_POWER,
            ALPHA_MAXIMUM, ALPHA_KURTOSIS, ALPHA_STANDARD_DEVIATION, ALPHA_SKEWNESS),

    BETA(BETA_RATIO, BETA_POWER, LOG_BETA_POWER, NORMALIZED_BETA_POWER,
            BETA_MAXIMUM, BETA_KURTOSIS, BETA_STANDARD_DEVIATION, BETA_SKEWNESS),

    GAMMA(GAMMA_RATIO, GAMMA_POWER, LOG_GAMMA_POWER, NORMALIZED_GAMMA_POWER,
            GAMMA_MAXIMUM, GAMMA_KURTOSIS, GAMMA_STANDARD_DEVIATION, GAMMA_SKEWNESS);

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