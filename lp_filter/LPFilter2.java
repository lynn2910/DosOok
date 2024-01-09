package lp_filter;

public class LPFilter2 {
    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq) {
        double rc = 1.0 / (cutoffFreq * 2 * Math.PI);
        double dt = 1.0 / sampleFreq;
        double alpha = dt / (rc + dt);
        double[] output = new double[inputSignal.length];

        output[0] = inputSignal[0];
        for (int i = 1; i < inputSignal.length; i++) {
            output[i] = alpha * inputSignal[i] + (1 - alpha) * output[i - 1];
        }
        return output;
    }
}