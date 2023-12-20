import java.awt.*;
import java.io.*;
import java.util.Arrays;


public class DosRead {
    static final int FP = 1000;
    static final int BAUDS = 100;
    static final int[] START_SEQ = {1,0,1,0,1,0,1,0};
    FileInputStream fileInputStream;
    int sampleRate = 44100;
    int bitsPerSample;
    int dataSize;
    double[] audio;
    int[] outputBits;
    char[] decodedChars;

    /**
     * Constructor that opens the FIlEInputStream
     * and reads sampleRate, bitsPerSample and dataSize
     * from the header of the wav file
     * @param path the path of the wav file to read
     */
    public void readWavHeader(String path){
        byte[] header = new byte[44]; // The header is 44 bytes long
        try {
            fileInputStream= new FileInputStream(path);
            fileInputStream.read(header);

            // Read the sample rate (offset 24, 4 bytes)
            sampleRate = byteArrayToInt(header, 24, 32);
            // Read the number of bits per sample (offset 34, 2 bytes)
            bitsPerSample = byteArrayToInt(header, 34, 16);
            // Read the size of the data (offset 40, 4 bytes)
            dataSize = byteArrayToInt(header, 40, 32);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method to convert a little-endian byte array to an integer
     * @param bytes the byte array to convert
     * @param offset    the offset in the byte array
     * @param fmt   the format of the integer (16 or 32 bits)
     * @return  the integer value
     */
    private static int byteArrayToInt(byte[] bytes, int offset, int fmt) {
        if (fmt == 16)
            return ((bytes[offset + 1] & 0xFF) << 8) | (bytes[offset] & 0xFF);
        else if (fmt == 32)
            return ((bytes[offset + 3] & 0xFF) << 24) |
                    ((bytes[offset + 2] & 0xFF) << 16) |
                    ((bytes[offset + 1] & 0xFF) << 8) |
                    (bytes[offset] & 0xFF);
        else return (bytes[offset] & 0xFF);
    }

    /**
     * Read the audio data from the wav file
     * and convert it to an array of doubles
     * that becomes the audio attribute
     */
    public void readAudioDouble() {
        byte[] audioData = new byte[dataSize];
        try {
            fileInputStream.read(audioData);

            // Crée un tableau de doubles appelé audio pour stocker les valeurs normalisées des échantillons audio.
            // La taille du tableau est déterminée en fonction du nombre d'octets par échantillon (bitsPerSample / 8)
            // et de la taille totale des données audio dans audioData.
            audio = new double[audioData.length / (bitsPerSample / 8)];

            for (int i = 0; i < audio.length; i++) {
                int byteFaible = audioData[2 * i];
                int byteFort = audioData[2 * i + 1];

                int echantillon = (byteFort << 8) | (byteFaible & 255); // Composition un échantillon sur 16 bits
                audio[i] = echantillon / 32768.0; // Normalise l'échantillon sur [-1, 1]
            }
        } catch (IOException e) {
            System.out.println("Erreur de lecture des données audio: " + e.getMessage());
        }
    }

    /**
     * Reverse the negative values of the audio array
     */
    public void audioRectifier(){
        for (int i = 0; i < audio.length; i++) {
            if (audio[i] < 0) {
                audio[i] = -audio[i];
            }
        }
    }

    /**
     * Apply a low pass filter to the audio array
     * Fc = (1/2n)*FECH
     * @param n the number of samples to average
     */
    public void audioLPFilter(int n) {
        /*
            À compléter
        */
    }

    /**
     * Resample the audio array and apply a threshold
     * @param period the number of audio samples by symbol
     * @param threshold the threshold that separates 0 and 1
     */
    public void audioResampleAndThreshold(int period, int threshold){
      /*
        À compléter
      */
    }

    /**
     * Decode the outputBits array to a char array
     * The decoding is done by comparing the START_SEQ with the actual beginning of outputBits.
     * The next first symbol is the first bit of the first char.
     */
    public void decodeBitsToChar(){
      /*
        À compléter
      */
    }

    /**
     * Print the elements of an array
     * @param data the array to print
     */
    public static void printIntArray(char[] data) {
      /*
        À compléter
      */
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
        int length = sig.length;
        if (length == 0) {
            return; // No need to display an empty signal
        }

        double max = Arrays.stream(sig, start, stop).max().orElse(1.0);

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
        double seqPeriod = stop - start;
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


    /**
     *  Un exemple de main qui doit pourvoir être exécuté avec les méthodes
     * que vous aurez conçues.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java DosRead <input_wav_file>");
            return;
        }
        String wavFilePath = args[0];

        // Open the WAV file and read its header
        DosRead dosRead = new DosRead();
        dosRead.readWavHeader(wavFilePath);

        // Print the audio data properties
        System.out.println("Fichier audio: " + wavFilePath);
        System.out.println("\tSample Rate: " + dosRead.sampleRate + " Hz");
        System.out.println("\tBits per Sample: " + dosRead.bitsPerSample + " bits");
        System.out.println("\tData Size: " + dosRead.dataSize + " bytes");

        // Read the audio data
        dosRead.readAudioDouble();
        // reverse the negative values
        dosRead.audioRectifier();
        // apply a low pass filter
        dosRead.audioLPFilter(44);
        // Resample audio data and apply a threshold to output only 0 & 1
        dosRead.audioResampleAndThreshold(dosRead.sampleRate/BAUDS, 12000 );

        dosRead.decodeBitsToChar();
        if (dosRead.decodedChars != null){
            System.out.print("Message décodé : ");
            printIntArray(dosRead.decodedChars);
        }

        displaySig(dosRead.audio, 0, dosRead.audio.length-1, "line", "Signal audio");

        // Close the file input stream
        try {
            dosRead.fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}