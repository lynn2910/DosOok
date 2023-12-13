import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


public class DosSend {
    final int FECH = 44100; // fréquence d'échantillonnage
    final int FP = 1000;    // fréquence de la porteuse
    final int BAUDS = 100;  // débit en symboles par seconde
    final int FMT = 8;    // format des données
    final int MAX_AMP = (1<<(FMT-1))-1; // amplitude max en entier
    final int CHANNELS = 1; // nombre de voies audio (1 = mono)
    final int[] START_SEQ = {1,0,1,0,1,0,1,0}; // séquence de synchro au début
    final Scanner input = new Scanner(System.in); // pour lire le fichier texte

    long taille;                // nombre d'octets de données à transmettre
    double duree ;              // durée de l'audio
    double[] dataMod;           // données modulées
    char[] dataChar;            // données en char

    FileOutputStream outStream; // flux de sortie pour le fichier .wav

    /**
     * Constructor
     * @param path  the path of the wav file to create
     */
    public DosSend(String path){
        File file = new File(path);
        try{
            outStream = new FileOutputStream(file);
        } catch (Exception e) {
            System.out.println("Erreur de création du fichier");
        }
    }

    /**
     * Write a raw 4-byte integer in little endian
     * @param octets    the integer to write
     * @param destStream  the stream to write in
     */
    public void writeLittleEndian(int octets, int taille, FileOutputStream destStream){
        char poidsFaible;
        while(taille > 0){
            poidsFaible = (char) (octets & 0xFF);
            try {
                destStream.write(poidsFaible);
            } catch (Exception e) {
                System.out.println("Erreur d'écriture");
            }
            octets = octets >> 8;
            taille--;
        }
    }


    /**
     * Create and write the header of a wav file
     * TODO
     */
    public void writeWavHeader() {
        try {
            int subChunk1Size = 16;
            int format = 1;
            int byteRate = FECH * CHANNELS * FMT / 8;
            short blockAlign = (short) (CHANNELS * FMT / 8);

            // RIFF chunk
            outStream.write("RIFF".getBytes());
            writeLittleEndian(36 + dataChar.length, 4, outStream);
            outStream.write("WAVE".getBytes());

            // fmt sub-chunk
            outStream.write("fmt ".getBytes());
            writeLittleEndian(subChunk1Size, 4, outStream);
            writeLittleEndian(format, 2, outStream);
            writeLittleEndian(CHANNELS, 2, outStream);
            writeLittleEndian(FECH, 4, outStream);
            writeLittleEndian(byteRate, 4, outStream);
            writeLittleEndian(blockAlign, 2, outStream);
            writeLittleEndian(FMT, 2, outStream);

            // data sub-chunk
            outStream.write("data".getBytes());
            writeLittleEndian(dataMod.length, 4, outStream);
        } catch (IOException e) {
            System.out.println("Could not write wave file - " + e.getMessage());
        }
    }

    /**
     * Write the data in the wav file
     * after normalizing its amplitude to the maximum value of the format (8 bits signed)
     */
    public void writeNormalizeWavData() {
        try {
            double maxVal = dataMod[0];
            for (double v : dataMod) {
                if (v > maxVal) maxVal = v;
            }

            double minVal = dataMod[0];
            for (double v : dataMod) {
                if (v < minVal) minVal = v;
            }

            double absMax = Math.max(Math.abs(maxVal), Math.abs(minVal));

            double normFactor = ((1 << (FMT - 1)) - 1) / absMax;

            // Normalize data
            for (double v : dataMod) {
                short val = (short) (v * normFactor);
                writeLittleEndian(val, 2, outStream);
            }


        } catch (Exception e) {
            System.out.println("Erreur d'écriture : " + e.getMessage());
        }
    }

    /**
     * Read the text data to encode and store them into dataChar
     * @return the number of characters read
     */
    public int readTextData(){
        if (this.dataChar == null) {
            this.dataChar = new char[0];
        }

        int charCount = 0;
        while (input.hasNext()) {
            String l = input.nextLine();
            int lLength = l.length();
            charCount += lLength;

            // Count the newline character if it's not the last line
            if (input.hasNext()) {
                charCount++;
            }

            char[] dataCharNew = new char[dataChar.length + lLength + 1];
            System.arraycopy(dataChar, 0, dataCharNew, 0, dataChar.length);
            l.getChars(0, lLength, dataCharNew, dataChar.length);

            // Add a newline character if it's not the last line
            if (input.hasNext()) {
                dataCharNew[dataChar.length + lLength] = '\n';
            }

            dataChar = dataCharNew;
        }

        return charCount;
    }

    /**
     * convert a char array to a bit array
     * @param chars The chars to convert
     * @return byte array containing only 0 & 1
     */
    public byte[] charToBits(char[] chars) {
        if(chars == null) {
            throw new IllegalArgumentException("Input character array should not be null.");
        }

        StringBuilder binaryStr = new StringBuilder();
        for (char c : chars) {
            StringBuilder binaryChar = new StringBuilder(Integer.toBinaryString(c));
            while (binaryChar.length() < FMT) {
                binaryChar.insert(0, "0");
            }
            binaryStr.append(binaryChar);
        }

        // Converting binary string to byte array
        byte[] byteArr = new byte[binaryStr.length()];
        for (int i = 0; i < binaryStr.length(); i++) {
            byteArr[i] = (byte) (binaryStr.charAt(i) == '1' ? 1 : 0);
        }
        return byteArr;
    }


    /**
     * Modulate the data to send and apply the symbol throughput via BAUDS and FECH.
     * @param bits the data to modulate
     */
    public void modulateData(byte[] bits) {
        if (bits == null) {
            throw new IllegalArgumentException("Input bits should not be null");
        }

        int bitCount = bits.length;
        duree = (double) bitCount / BAUDS; // ajuste la durée en fonction de la fréquence de bauds

        dataMod = new double[(int) (duree * FECH)];

        int idx = 0;
        for (int i = 0; i < bitCount; i++) {
            double bitDuration = 1.0 / BAUDS; // durée d'un bit (symbole)
            double timeStart = i * bitDuration;
            double timeEnd = (i + 1) * bitDuration;

            for (double t = timeStart; t < timeEnd && idx < dataMod.length; t += 1.0 / FECH) {
                double phase = 2 * Math.PI * t * FP;
                double amplitude = bits[i] == 1 ? 0.5 : 0.0;
                dataMod[idx++] = amplitude * Math.sin(phase);
            }
        }
    }

    public static final double Y_AXIS_PADDING = 0.75;

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
     * Display signals in a window
     * @param listOfSigs A list of the signals to display
     * @param start The first sample to display
     * @param stop The last sample to display
     * @param mode "line" or "point"
     * @param title The title of the window
     */
    public static void displaySig(List<double[]> listOfSigs, int start, int stop, String mode, String title){
        int N = stop - start;

        if (N <= 0) {
            return; // No need to display an empty signal range
        }

        double maxVal = listOfSigs.stream()
                .flatMapToDouble(sig -> Arrays.stream(sig, start, stop))
                .max()
                .orElse(1.0);
        double minVal = listOfSigs.stream()
                .flatMapToDouble(sig -> Arrays.stream(sig, start, stop))
                .min()
                .orElse(0.0);

        double yRange = maxVal - minVal;
        double padding = Y_AXIS_PADDING * yRange;

        StdDraw.setCanvasSize(700, 500);
        StdDraw.setXscale(start, stop - 1);
        StdDraw.setYscale(minVal - padding, maxVal + padding);

        int numOfSigs = listOfSigs.size();

        StdDraw.setPenColor(StdDraw.BLUE);
        for (int s = 0; s < numOfSigs; s++) {
            double[] sig = listOfSigs.get(s);

            if ("line".equals(mode)) {
                // Draw using lines
                for (int i = start + 1; i < stop && i < sig.length; i++) {
                    StdDraw.line(i - 1, sig[i - 1], i, sig[i]);
                }
            } else {
                // Draw using points
                for (int i = start; i < stop && i < sig.length; i++) {
                    StdDraw.point(i, sig[i]);
                }
            }
        }
    }

    public static void main(String[] args) {
        // créé un objet DosSend
        DosSend dosSend = new DosSend("DosOok_message.wav");

        // lit le texte à envoyer depuis l'entrée standard
        // et calcule la durée de l'audio correspondant
        dosSend.duree = (double) (dosSend.readTextData() + dosSend.START_SEQ.length / 8) * 8.0 / dosSend.BAUDS;

        // génère le signal modulé après avoir converti les données en bits
        dosSend.modulateData(dosSend.charToBits(dosSend.dataChar));

        // écrit l'entête du fichier wav
        dosSend.writeWavHeader();

        // écrit les données audio dans le fichier wav
        dosSend.writeNormalizeWavData();

        // affiche les caractéristiques du signal dans la console
        System.out.println("Message : "+String.valueOf(dosSend.dataChar));
        System.out.println("\tNombre de symboles : "+dosSend.dataChar.length);
        System.out.println("\tNombre d'échantillons : "+dosSend.dataMod.length);
        System.out.println("\tDurée : "+dosSend.duree+" s");
        System.out.println();


        // exemple d'affichage du signal modulé dans une fenêtre graphique
        displaySig(dosSend.dataMod, 1000, 3000, "line", "Signal modulé");
    }

}
