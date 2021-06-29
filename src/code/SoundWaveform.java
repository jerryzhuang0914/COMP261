package code;

// DO NOT DISTRIBUTE THIS FILE TO STUDENTS
import ecs100.UI;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/*
  getAudioInputStream
  -> getframelength,
  -> read into byteArray of 2x that many bytes
  -> convert to array of doubles in reversed pairs of bytes (signed)
  -> scale #FFFF to +/- 300

  array of doubles
   -> unscale  +/- 300  to #FFFF (
   -> convert to array of bytes (pairs little endian, signed)
   -> convert to inputStream
   -> convert to AudioInputStream
   -> write to file.
 */

public class SoundWaveform{

    public static final double MAX_VALUE = 300;
    public static final int SAMPLE_RATE = 44100;
    public static final int MAX_SAMPLES = SAMPLE_RATE/100;   // samples in 1/100 sec

    public static final int GRAPH_LEFT = 10;
    public static final int ZERO_LINE = 310;
    public static final int X_STEP = 2;            //pixels between samples
    public static final int GRAPH_WIDTH = MAX_SAMPLES*X_STEP;

    private ArrayList<Double> waveform = new ArrayList<Double>();   // the displayed waveform
    private List<ComplexNumber> spectrum = new ArrayList<ComplexNumber>(); // the spectrum: length/mod of each X(k)
    private static final ComplexNumber ZERO = new ComplexNumber(0, 0);

    /**
     * Displays the waveform.
     */
    public void displayWaveform(){
        if (this.waveform == null){ //there is no data to display
            UI.println("No waveform to display");
            return;
        }
       // UI.clearText();
        UI.println("Printing, please wait...");

        UI.clearGraphics();

        // draw x axis (showing where the value 0 will be)
        UI.setColor(Color.black);
        UI.drawLine(GRAPH_LEFT, ZERO_LINE, GRAPH_LEFT + GRAPH_WIDTH , ZERO_LINE);

        // plot points: blue line between each pair of values
        UI.setColor(Color.blue);

        double x = GRAPH_LEFT;
        for (int i=1; i<this.waveform.size(); i++){
            double y1 = ZERO_LINE - this.waveform.get(i-1);
            double y2 = ZERO_LINE - this.waveform.get(i);
            if (i>MAX_SAMPLES){UI.setColor(Color.red);}
            UI.drawLine(x, y1, x+X_STEP, y2);
            x = x + X_STEP;
        }

        UI.println("Printing completed!");
    }

    /**
     * Displays the spectrum. Scale to the range of +/- 300.
     */
    public void displaySpectrum() {
        if (this.spectrum == null){ //there is no data to display
            UI.println("No spectrum to display");
            return;
        }
       // UI.clearText();
        UI.println("Printing, please wait...");

        UI.clearGraphics();

        // calculate the mode of each element
        ArrayList<Double> spectrumMod = new ArrayList<Double>();
        double max = 0;
        for (int i = 0; i < spectrum.size(); i++) {
            if (i == MAX_SAMPLES)
                break;

            double value = spectrum.get(i).mod();
            max = Math.max(max, value);
            spectrumMod.add(spectrum.get(i).mod());
        }

        double scaling = 300/max;
        for (int i = 0; i < spectrumMod.size(); i++) {
            spectrumMod.set(i, spectrumMod.get(i)*scaling);
        }

        // draw x axis (showing where the value 0 will be)
        UI.setColor(Color.black);
        UI.drawLine(GRAPH_LEFT, ZERO_LINE, GRAPH_LEFT + GRAPH_WIDTH , ZERO_LINE);

        // plot points: blue line between each pair of values
        UI.setColor(Color.blue);

        double x = GRAPH_LEFT;
        for (int i=1; i<spectrumMod.size(); i++){
            double y1 = ZERO_LINE;
            double y2 = ZERO_LINE - spectrumMod.get(i);
            if (i>MAX_SAMPLES){UI.setColor(Color.red);}
            UI.drawLine(x, y1, x+X_STEP, y2);
            x = x + X_STEP;
        }

        UI.println("Printing completed!");
    }

    /**
     * Test case 1
     */
    public void test1(){
        waveform.clear();
        waveform.add(1.0);waveform.add(2.0);waveform.add(3.0);waveform.add(4.0);
        waveform.add(5.0);waveform.add(6.0);waveform.add(7.0);waveform.add(8.0);
    }

    /**
     * Test case 2
     */
    public void test2(){
        waveform.clear();
        waveform.add(1.0);waveform.add(2.0);waveform.add(1.0);waveform.add(2.0);
        waveform.add(1.0);waveform.add(2.0);waveform.add(1.0);waveform.add(2.0);
    }

    /**
     * Test case 3
     */
    public void test3(){
        waveform.clear();
        waveform.add(1.0);waveform.add(2.0);waveform.add(3.0);waveform.add(4.0);
        waveform.add(4.0);waveform.add(3.0);waveform.add(2.0);waveform.add(1.0);
    }

    /**
     * for seeing the output of Fourier Transform.
     */
    public void testSpectrumOutput(){
        for (Object o : spectrum) {
            UI.println(o);
        }
    }

    /**
     * for seeing the output of Inverse Fourier Transform.
     */
    public void testWaveformOutput(){
        for (Object o : waveform) {
            UI.println(o);
        }
    }

    /**
     * It is called when transforming the discrete signal from the time domain to
     * the frequency domain by using the Discrete Fourier Transform technique.
     */
    public void dft() {

        double startTime = System.currentTimeMillis();
        UI.clearText();
        UI.println("DFT in process, please wait...");

        //test1();
        //test2();
        //test3();

        // sample size
        int N = waveform.size();

        // if N = 1, X(k) = x(n).
        if (N == 1) {
            ComplexNumber xn = new ComplexNumber(waveform.get(0), 0);
            spectrum.add(xn);
        }
        // if N > 1.
        for (int k = 0; k < N; k++) {
            double sumReal = 0;
            double sumImag = 0;
            for (int n = 0; n < N; n++) {

                double angle = -n * k * (2 * Math.PI / N);
                // Euler's formula: e^it = cos(t) + i * sin(t)
                // So x(n) * e^(-i * n * k * 2PI/N) = x(n) * [cos(-n * k * 2PI/N) + i * sin(-n * k * 2PI/N)]
                sumReal += waveform.get(n) * Math.cos(angle);
                sumImag += waveform.get(n) * Math.sin(angle);
            }
            ComplexNumber Xk = new ComplexNumber(sumReal, sumImag);
            spectrum.add(Xk);
        }

        // calculate the DFT runtime.
        double endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        if (duration >= 60.0) {
            double minutes = duration / 60;
            String doubleAsString = String.valueOf(minutes);
            int indexOfDecimal = doubleAsString.indexOf(".");
            double seconds = Double.parseDouble(doubleAsString.substring(indexOfDecimal)) * 60.0;
            UI.println("DFT completed in " + doubleAsString.substring(0, indexOfDecimal) + "m," + String.format("%.5gs%n", seconds));
            UI.println("Please click the 'Display Spectrum' button.\n");
        } else {
            UI.println("DFT completed in " + String.format("%.3gs%n", duration));
            UI.println("Please click the 'Display Spectrum' button.\n");
        }

        //testSpectrumOutput();
        waveform.clear();
    }

    /**
     * It is called when transforming the discrete signal from the frequency domain
     * to the time domain by using the Inverse Discrete Fourier Transform technique.
     */
    public void idft() {
        double startTime = System.currentTimeMillis();
        UI.clearText();
        UI.println("IDFT in process, please wait...");

        // sample size
        int N = spectrum.size();

        // if N = 1, x(n) = |X(k)|
        if (N == 1) {
            double xn = Math.sqrt(Math.pow(spectrum.get(0).getRe(), 2) + Math.pow(spectrum.get(0).getIm(), 2));
            waveform.add(xn);
        }
        // if N > 1,
        for (int n = 0; n < N; n++) {
            double sumReal = 0;
            //double sumImag = 0;

            for (int k = 0; k < N; k++) {

                double angle = n * k * (2 * Math.PI / N);
                // Euler's formula: e^it = cos(t) + i * sin(t)
                // So X(k) * e^(i * n * k * 2PI/N) = X(k) * [cos(n * k * 2PI/N) + i * sin(n * k * 2PI/N)]
                // Use Multiplication rule, (a + b * i) * (c + d * i) = (ac - bd) + (bc + ad) * i
                // only need to calculate real part since the imaginary part is supposed to be (almost) zero.
                sumReal += spectrum.get(k).getRe() * Math.cos(angle) - spectrum.get(k).getIm() * Math.sin(angle);
                //sumImag += spectrum.get(k).getIm() * Math.cos(angle) + spectrum.get(k).getRe() * Math.sin(angle);

            }

            // simply gets real part.
            double xn = sumReal / N;
            waveform.add(xn);
        }

        double endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        if (duration >= 60.0) {
            double minutes = duration / 60;
            String doubleAsString = String.valueOf(minutes);
            int indexOfDecimal = doubleAsString.indexOf(".");
            double seconds = Double.parseDouble(doubleAsString.substring(indexOfDecimal)) * 60.0;
            UI.println("IDFT completed in " + doubleAsString.substring(0, indexOfDecimal) + "m," + String.format("%.5gs%n", seconds));
            UI.println("Please click the 'Display Waveform' button.\n");
        } else {
            UI.println("IDFT completed in " + String.format("%.3gs%n", duration));
            UI.println("Please click the 'Display Waveform' button.\n");
        }

        //testWaveformOutput();
        spectrum.clear();
    }

    /**
     * It is called when transforming the discrete signal from the time domain
     * to the frequency domain by using the Fast Fourier Transform technique.
     */
    public void fft() {

        double startTime = System.currentTimeMillis();
        UI.clearText();
        UI.println("FFT in process, please wait...");

        //test1();
        //test2();
        //test3();

        ArrayList<ComplexNumber> input = new ArrayList<>();
        for (Double real : waveform) {
            ComplexNumber xn = new ComplexNumber(real, 0);
            input.add(xn);
        }

        // cut the tail of the waveform, while its size isn't equal to the power of 2.
        while (isNotThePowerOfTwo(input.size())) {
            input.remove(input.size() - 1);
        }

        spectrum = FFT(input);

        double endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        UI.println("FFT completed in  " + String.format("%.3gs%n", duration));
        UI.println("Please click the 'Display Spectrum' button.\n");

        //testSpectrumOutput();
        waveform.clear();
    }

    /**
     * Recursive FFT
     * @param x
     *      the waveform signal input
     * @return
     *      return a list of element which has been through the Fourier Transform.
     */
    public List<ComplexNumber> FFT(List<ComplexNumber> x) {

        // sample size
        int N = x.size();

        // if N = 1, X(k) = x(n).
        if (N == 1) {return x;}

        // if N is not the power of 2.
        if (isNotThePowerOfTwo(N)) {throw new IllegalArgumentException("N is not power of 2.");}

        // if N > 1,
        // even parts of x(n)
        List<ComplexNumber> xeven = new ArrayList<>();
        for (int n = 0; n < N - 1; n+=2) {
            xeven.add(x.get(n));
        }
        List<ComplexNumber> Xeven = FFT(xeven);

        // odd parts of x(n)
        List<ComplexNumber> xodd = new ArrayList<>();
        for (int n = 1; n < N; n+=2) {
            xodd.add(x.get(n));
        }
        List<ComplexNumber> Xodd = FFT(xodd);

        // W(k, N) and W(k+N/2, N),
        ComplexNumber[] W = new ComplexNumber[N];
        for (int k = 0; k < N; k++) {
            double angle = -k * (2 * Math.PI / N);
            // Euler's formula: e^it = cos(t) + i * sin(t)
            W[k] = new ComplexNumber(Math.cos(angle), Math.sin(angle));
        }

        ComplexNumber[] X = new ComplexNumber[N];
        for (int k = 0; k < N/2; k++) {

            // X(k) = Xeven(k) + Xodd(k) * W(k, N)
            ComplexNumber Xk1 = Xeven.get(k).plus(Xodd.get(k).multiplyBy(W[k]));
            X[k] = Xk1;

            // X(k+N/2) = Xeven(k) + Xodd(k) * W(k+N/2, N)
            ComplexNumber Xk2 = Xeven.get(k).plus(Xodd.get(k).multiplyBy(W[k + N/2]));
            X[k + N/2] = Xk2;
        }
        return Arrays.asList(X);
    }

    /**
     *  It is called when checking if n is the power of 2
     *
     * @param n
     *      size of waveform.
     * @return
     *      return true if n is the power of 2, otherwise return false.
     */
    public static boolean isNotThePowerOfTwo(int n)
    {
        if (n == 0)
            return true;

        while (n != 1) {
            if (n % 2 != 0)
                return true;
            n = n / 2;
        }
        return false;
    }

    /**
     * It is called when transforming the discrete signal from the frequency domain
     * to the time domain by using the Inverse Fast Fourier Transform technique.
     */
    public void ifft() {
        double startTime = System.currentTimeMillis();
        UI.clearText();
        UI.println("IFFT in process, please wait...");

        int N = spectrum.size();

        // take the spectrum's complex conjugate.
        List<ComplexNumber> conjugateNum = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            conjugateNum.add(spectrum.get(i).conjugate());
        }

        // take the FFT of the result above.
        List<ComplexNumber> fftConjugate = FFT(conjugateNum);

        // take the complex conjugate again.
        for (int i = 0; i < fftConjugate.size(); i++) {
            conjugateNum.set(i, fftConjugate.get(i).conjugate());
        }

        // get the real part of complex then divided by N, add into waveform.
        for (int i = 0; i < N; i++) {
            waveform.add(conjugateNum.get(i).getRe() / N);
        }

        double endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        UI.println("IFFT completed in  " + String.format("%.3gs%n", duration));
        UI.println("Please click the 'Display Waveform' button.\n");

        //testWaveformOutput();
        //spectrum.clear();
    }

    /**
     * Save the wave form to a WAV file
     */
    public void doSave() {
        WaveformLoader.doSave(waveform, WaveformLoader.scalingForSavingFile);
    }

    /**
     * Load the WAV file.
     */
    public void doLoad() {
        UI.clearText();
        UI.println("Loading...");

        waveform = WaveformLoader.doLoad();

        this.displayWaveform();

        UI.println("Loading completed!");
    }

    public static void main(String[] args){
        SoundWaveform wfm = new SoundWaveform();
        //core
        UI.addButton("Display Waveform", wfm::displayWaveform);
        UI.addButton("Display Spectrum", wfm::displaySpectrum);
        UI.addButton("DFT", wfm::dft);
        UI.addButton("IDFT", wfm::idft);
        UI.addButton("FFT", wfm::fft);
        UI.addButton("IFFT", wfm::ifft);
        UI.addButton("Save", wfm::doSave);
        UI.addButton("Load", wfm::doLoad);
        UI.addButton("Quit", UI::quit);
        //UI.setMouseMotionListener(wfm::doMouse);
        UI.setWindowSize(950, 630);
    }
}
