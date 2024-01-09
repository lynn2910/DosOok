import java.awt.*;
import java.util.Arrays;

class LPFilter1 {
    public static void main(String[] args) {
        // Création d'un signal audio de test (par exemple, un signal sinusoïdal)
        int numSamples = 1000;
        double[] inputSignal = generateSineWave(numSamples, 20, 440, 44100);

        for (int i = 0; i < inputSignal.length; i++) {
            if (inputSignal[i] < 0) {
                inputSignal[i] = -(inputSignal[i] / 2);
            }
        }

        LPFilter1 lpFilter = new LPFilter1();

        // Application du filtre passe-bas
        double[] filteredSignal = lpFilter.lpFilter(inputSignal, 750, 200);

        displaySig(filteredSignal, 0, inputSignal.length, "line", "Signal audio filtré");
    }


    /**
     * Display a signal in a window
     * @param sig The signal to display
     * @param start The first sample to display
     * @param stop The last sample to display
     * @param mode "line" or "point"
     * @param title The title of the window
     */
    public static void displaySig(double[] sig, int start, int stop, String mode, String title){
        StdDraw.setTitle(title);

        int length = sig.length;
        if (length == 0) {
            return; // No need to display an empty signal
        }

        double max = sig[0];
        for (double s: sig)
            if (s > max) max = s;

        int height = 500;
        int width  = 1000;

        StdDraw.setCanvasSize(width, height);
        StdDraw.setXscale(0.0, width);
        StdDraw.setYscale(-max, max);

        // draw min & max
        StdDraw.text(50, max - (max / 1000 * 50), String.valueOf((int) max));
        StdDraw.text(50, -max + (max / 1000 * 50),         String.valueOf((int) -max));

        // draw middle line with numbers
        StdDraw.line(0, 0.0, width, 0.0);
        double seqPeriod = (double) stop - (double) start;
        for (int i = 0; i < 10; i++) {
            int x = (width / 10) * i;
            String n = String.valueOf((int) ((seqPeriod / 10) * i));

            StdDraw.line(x, (max / 1000 * 5), x, (max / 1000 * -5));
            StdDraw.text(x, (max / 1000 * -60), n);
        }

        // draw lines
        double paddingRatio = 0.8;
        if ("line".equals(mode)) {
            StdDraw.setPenColor(Color.BLUE);
            for (int i = start; i < stop - 1; i++) {
                double seq1 = sig[i];
                double x1 = ((double) width / (stop - start)) * (i - start);

                double seq2 = sig[i + 1];
                double x2 = ((double) width / (stop - start)) * (i + 1 - start);

                StdDraw.line(x1, seq1 * paddingRatio, x2, seq2 *paddingRatio);
            }
        } else {
            for (int i = start; i < stop; i++) {
                double seq = sig[i];
                double x = ((double) width / (stop - start)) * (i - start);

                StdDraw.point(x, seq * paddingRatio);
            }
        }
    }


    // Fonction utilitaire pour générer un signal sinusoïdal
    private static double[] generateSineWave(int numSamples, int n, double frequency, double sampleRate) {
        double[] sineWave = new double[numSamples * n];
        for (int i = 0; i < numSamples; i++) {
            int bit = (int) (Math.floor(Math.random() * 2) % 2);

            for (int j = 0; j < n; j++) {
                sineWave[i * n + j] = bit * 1000 * Math.sin(2 * Math.PI * frequency * i / sampleRate);
            }
        }
        return sineWave;
    }

    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq) {
        if (inputSignal == null || inputSignal.length == 0)
            throw new IllegalArgumentException("Input signal must be non-empty");

        double[] filteredAudio = new double[inputSignal.length];

        for (int i = 0; i < inputSignal.length; i++) {
            double sum = 0;
            int startIdx = Math.max(0, i - (int) sampleFreq + 1);
            for (int j = startIdx; j <= i; j++)
                sum += inputSignal[j];

            filteredAudio[i] = sum / sampleFreq;
        }

        return filteredAudio;
    }

}