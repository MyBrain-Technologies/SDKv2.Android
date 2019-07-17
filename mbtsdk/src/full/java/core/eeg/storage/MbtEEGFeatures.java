package core.eeg.storage;


public class MbtEEGFeatures {

    private static final int DELTA_RATIO = 45;
    private static final int DELTA_POWER = 46;
    private static final int LOG_DELTA_POWER = 47;
    private static final int NORMALIZED_DELTA_POWER = 48;

    private static final int THETA_RATIO = 49;
    private static final int THETA_POWER = 50;
    private static final int LOG_THETA_POWER = 51;
    private static final int NORMALIZED_THETA_POWER = 52;

    private static final int ALPHA_RATIO = 53;
    private static final int ALPHA_POWER = 54;
    private static final int LOG_ALPHA_POWER = 55;
    private static final int NORMALIZED_ALPHA_POWER = 56;

    private static final int BETA_RATIO = 57;
    private static final int BETA_POWER = 58;
    private static final int LOG_BETA_POWER = 59;
    private static final int NORMALIZED_BETA_POWER = 60;

    private static final int GAMMA_RATIO = 61;
    private static final int GAMMA_POWER = 62;
    private static final int LOG_GAMMA_POWER = 63;
    private static final int NORMALIZED_GAMMA_POWER = 64;

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
}
