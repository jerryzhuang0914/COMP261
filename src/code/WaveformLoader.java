package code;

import ecs100.*;
import java.awt.Color;
import java.util.*;
import java.io.*;
import javax.sound.sampled.*;
import javax.swing.JOptionPane;

/** Waveform loads and saves wav files into arraylists of double */

public class WaveformLoader{



    public static final int SAMPLE_RATE = 44100; 
    public static final int MAX_SAMPLES = SAMPLE_RATE*30;   // samples in 30 sec

    // wav file data is scaled to +/-300 in waveform;
    // must be reverse scaled when waveform is saved.
    public static double scalingForSavingFile = 1;

    /**
     * Load up to 30 seconds of a WAV file into an arraylist
     * Scales all the values so that the maximum (+ or -) is 300, and puts the scale factor
     * into the scalingForSavingFile field, which can be used when saving to reverse the scaling.
     * Only loads 1 channel, and only uses the first two bytes in a frame. Assumes littleEndian
     *
     */
    public static ArrayList<Double> doLoad(){
        String filename = UIFileChooser.open("wav file");
        if (filename==null) {return null;}
        if (!filename.endsWith(".wav") && !filename.endsWith(".WAV")){
            JOptionPane.showMessageDialog(null, "Not a  wav file"); return null;
        }
        ArrayList<Double> waveform = new ArrayList<Double>();
        try{
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(filename));

            AudioFormat format = audioStream.getFormat();
            if (format.isBigEndian()) {
                JOptionPane.showMessageDialog(null, "Wrong format"); return null;
            }

            int fsize = format.getFrameSize();
            int numSamples = Math.min(MAX_SAMPLES,(int)audioStream.getFrameLength());

            double max = 0;
            byte[] frame = new byte[fsize];
            for (int i=0; i<numSamples; i++){
                audioStream.read(frame);
                double value = 0;
                if (fsize==1){ value = ((double)frame[0])*0xFFFF/0xFF; }    // scale 8 bit to 16 bit
                else { value = (double)((frame[0] & 0xFF) | frame[1]<<8); } // read 16 bit
                max = Math.max(max, value);
                waveform.add(value);
            }
            audioStream.close();
            scalingForSavingFile = 300/max;
            for (int i=0; i<waveform.size(); i++){
                waveform.set(i, waveform.get(i)*scalingForSavingFile);
            }
        }
        catch(UnsupportedAudioFileException e){JOptionPane.showMessageDialog(null, "Not a valid audio file: " + e);} 
        catch(IOException e){JOptionPane.showMessageDialog(null, "Loading the wav file failed: " + e);} 

        return waveform;
    }


    /**
     * Save the waveform to a WAV file, 1 channel, 16 bits per frame
     * scales the values by dividing each value by scale.
     */
    public static void doSave(ArrayList<Double> waveform, double scale){

        short[] samples = new short[waveform.size()];  //the samples, scaled to a max value of 15000
        int index = 0;
        for (double val : waveform){
            samples[index++] = (short) (val/scale);
        }
        byte[] bytes = new byte[samples.length*2];
        index = 0;
        for (short val : samples){
            byte littleend = (byte) (val & 0xFF);
            byte bigend = (byte) (val >> 8);
            bytes[index++] = littleend;
            bytes[index++] = bigend;
        }
        AudioFormat  wavFormat = new AudioFormat(44100f, 16, 1, true, false);

        InputStream byteStream = new ByteArrayInputStream(bytes);
        AudioInputStream audioStream = new AudioInputStream(byteStream, wavFormat, samples.length);
        String filename = UIFileChooser.save("Output wav file");
        if (filename==null) {return;}
        if (!filename.endsWith(".wav") && !filename.endsWith(".WAV")){
            filename = filename+".wav";
        }
        try{
            AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, new File(filename));
        } catch(IOException e){JOptionPane.showMessageDialog(null, "Writing the wav file failed: " + e);}
    }

}
