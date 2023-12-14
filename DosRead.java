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

            // Read the sample rate (offset: 24, format: 32 bits)
            sampleRate = byteArrayToInt(header, 24, 32);

            // Read the bits per sample (offset: 34, format: 16 bits)
            bitsPerSample = byteArrayToInt(header, 34, 16);

            // Read the data size (offset: 40, format: 32 bits)
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

            // Assuming 16 bits per sample, convert audioData to an array of doubles
            audio = new double[dataSize / 2]; // 2 bytes per sample for 16 bits

            for (int i = 0, j = 0; i < dataSize; i += 2, j++) {
                // Convert two bytes to a 16-bit signed integer
                int sample = byteArrayToInt(audioData, i, 16);

                // Normalize the 16-bit integer to a double in the range [-1.0, 1.0]
                audio[j] = sample / 32768.0;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Reverse the negative values of the audio array
     */
    public void audioRectifier() {
        if (audio != null) {
            for (int i = 0; i < audio.length; i++) {
                // Reverse the sign of the audio signal for negative values
                if (audio[i] < 0) {
                    audio[i] = -audio[i];
                }
            }
        } else {
            // Handle the case where the 'audio' array is not initialized
            System.err.println("Audio array is not initialized. Call readAudioDouble() first.");
        }
    }


    /**
     * Apply a low pass filter to the audio array
     * Fc = (1/2n)*FECH
     * @param n the number of samples to average
     */
    public void audioLPFilter(int n) {
        if (audio != null) {
            double[] filteredAudio = new double[audio.length];

            for (int i = 0; i < audio.length; i++) {
                double sum = 0.0;

                // Calculate the average of the previous 'n' samples
                for (int j = Math.max(0, i - n + 1); j <= i; j++) {
                    sum += audio[j];
                }

                // Apply the low pass filter to the current sample
                filteredAudio[i] = sum / Math.min(n, i + 1);
            }

            // Copy the filtered audio back to the original array
            System.arraycopy(filteredAudio, 0, audio, 0, audio.length);
        } else {
            // Handle the case where the 'audio' array is not initialized
            System.err.println("Audio array is not initialized. Call readAudioDouble() first.");
        }
    }


    /**
     * Resample the audio array and apply a threshold
     * @param period the number of audio samples by symbol
     * @param threshold the threshold that separates 0 and 1
     */
    public void audioResampleAndThreshold(int period, int threshold){
        if (audio != null) {
            int outputSize = audio.length / period;
            outputBits = new int[outputSize];

            for (int i = 0; i < outputSize; i++) {
                double sum = 0.0;

                // Calculate the average of 'period' samples
                for (int j = i * period; j < (i + 1) * period; j++) {
                    sum += audio[j];
                }

                // Calculate the average value for the current symbol
                double average = sum / period;

                // Apply threshold and determine the output bit
                outputBits[i] = (average > (threshold * FP)) ? 1 : 0;
            }
        } else {
            // Handle the case where the 'audio' array is not initialized
            System.err.println("Audio array is not initialized. Call readAudioDouble() first.");
        }
    }


    /**
     * Decode the outputBits array to a char array
     * The decoding is done by comparing the START_SEQ with the actual beginning of outputBits.
     * The next first symbol is the first bit of the first char.
     */
    public void decodeBitsToChar(){
        if (outputBits != null) {
            // Find the index where the START_SEQ begins
            System.out.println("Output Bits: " + Arrays.toString(outputBits));
            int startIndex = findStartSequence(outputBits, START_SEQ);

            if (startIndex != -1) {
                // Calculate the number of symbols to decode
                int symbolsToDecode = (outputBits.length - startIndex) / 8;

                decodedChars = new char[symbolsToDecode];

                for (int i = 0; i < symbolsToDecode; i++) {
                    // Initialize the decoded char with the first bit of the symbol
                    char decodedChar = (char) (outputBits[startIndex + i * 8] + '0');

                    // Add the remaining 7 bits to the decoded char
                    for (int j = 1; j < 8; j++) {
                        decodedChar = (char) ((decodedChar << 1) | outputBits[startIndex + i * 8 + j]);
                    }

                    // Store the decoded char in the array
                    decodedChars[i] = decodedChar;
                }
            } else {
                System.err.println("Start sequence not found.");
            }
        } else {
            // Handle the case where the 'outputBits' array is not initialized
            System.err.println("Output bits array is not initialized. Call audioResampleAndThreshold first.");
        }
    }


    /**
     * Check if the specified subarray of the array starts with the given sequence
     *
     * @param array    the array to check
     * @param start    the starting index of the subarray
     * @param sequence the sequence to check
     * @return true if the subarray starts with the sequence, false otherwise
     */
    private boolean startsWithSequence(int[] array, int start, int[] sequence) {
        for (int i = 0; i < sequence.length; i++) {
            if (array[start + i] != sequence[i]) {
                return false;
            }
        }
        return true;
    }


    /**
     * Find the index where the specified sequence begins in the array
     *
     * @param array    the array to search
     * @param sequence the sequence to find
     * @return the index where the sequence begins, or -1 if not found
     */
    private int findStartSequence(int[] array, int[] sequence) {
        for (int i = 0; i < array.length - sequence.length + 1; i++) {
            if (startsWithSequence(array, i, sequence)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Print the elements of an array
     * @param data the array to print
     */
    public static void printIntArray(char[] data) {
        for (char c : data) {
            System.out.print(c);
        }
        System.out.println(); // Ajoutez une nouvelle ligne à la fin pour plus de lisibilité
    }

    public static final double Y_AXIS_PADDING = 0.75;

    /**
     * Display a signal in a window
     * @param sig  the signal to display
     * @param start the first sample to display
     * @param stop the last sample to display
     * @param mode "line" or "point"
     * @param title the title of the window
     */
    public static void displaySig(double[] sig, int start, int stop, String mode, String title){
        int length = sig.length;
        if (length == 0) {
            return; // No need to display an empty signal
        }

        double maxVal = Arrays.stream(sig, start, stop).max().orElse(1.0);
        double minVal = Arrays.stream(sig, start, stop).min().orElse(0.0);

        double yRange = maxVal - minVal;
        double padding = Y_AXIS_PADDING * yRange;

        StdDraw.setCanvasSize(700, 500);
        StdDraw.setXscale(start, stop - 1);
        StdDraw.setYscale(minVal - padding, maxVal + padding);

        if ("line".equals(mode)) {
            // Draw using lines
            for (int i = start + 1; i < stop && i < length; i++) {
                StdDraw.line(i - 1, sig[i - 1], i, sig[i]);
            }
        } else {
            // Draw using points
            for (int i = start; i < stop && i < length; i++) {
                StdDraw.point(i, sig[i]);
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

        // TODO
        System.out.println("Audio avant traitement : " + Arrays.toString(audio));

        // Print the audio data properties
        System.out.println("Fichier audio: " + wavFilePath);
        System.out.println("\tSample Rate: " + dosRead.sampleRate + " Hz");
        System.out.println("\tBits per Sample: " + dosRead.bitsPerSample + " bits");
        System.out.println("\tData Size: " + dosRead.dataSize + " bytes");

        // Read the audio data
        dosRead.readAudioDouble();
        // reverse the negative values
        dosRead.audioRectifier();

        // TODO
        System.out.println("Audio après redressement : " + Arrays.toString(audio));

        // apply a low pass filter
        dosRead.audioLPFilter(44);

        // TODO
        System.out.println("Audio après filtrage passe-bas : " + Arrays.toString(audio));

        // Resample audio data and apply a threshold to output only 0 & 1
        dosRead.audioResampleAndThreshold(dosRead.sampleRate/BAUDS, 10000 );

        // TODO
        System.out.println("Output Bits : " + Arrays.toString(outputBits));

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