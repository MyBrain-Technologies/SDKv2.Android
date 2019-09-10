package core.eeg.signalprocessing;

import static core.eeg.signalprocessing.MbtEegFeatures.*;

public enum Frequency {

        DELTA(DELTA_RATIO, DELTA_POWER, LOG_DELTA_POWER, NORMALIZED_DELTA_POWER),
        THETA(THETA_RATIO, THETA_POWER, LOG_THETA_POWER, NORMALIZED_THETA_POWER),
        ALPHA(ALPHA_RATIO, ALPHA_POWER, LOG_ALPHA_POWER, NORMALIZED_ALPHA_POWER),
        BETA(BETA_RATIO, BETA_POWER, LOG_BETA_POWER, NORMALIZED_BETA_POWER),
        GAMMA(GAMMA_RATIO, GAMMA_POWER, LOG_GAMMA_POWER, NORMALIZED_GAMMA_POWER);

        private int ratio;
        private int power;
        private int logPower;
        private int normalizedPower;

        Frequency(int ratio, int power, int logPower, int normalizedPower) {
            this.ratio = ratio;
            this.power = power;
            this.logPower = logPower;
            this.normalizedPower = normalizedPower;
        }

        public int getRatio() {
            return ratio;
        }

        public int getPower() {
            return power;
        }

        public int getLogPower() {
            return logPower;
        }

        public int getNormalizedPower() {
            return normalizedPower;
        }
}


