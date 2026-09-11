package deepfake.ai;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

public class Main {

    private static final List<Float> pitchReadings = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        System.out.println("Current working directory: " + System.getProperty("user.dir"));

File audioFile = new File("D:\\deepfake core(c person)\\voxsentinal\\src\\main\\resources\\voice_real.wav-mono.wav");
        System.out.println("Looking for file at: " + audioFile.getAbsolutePath());
        System.out.println("Does that file exist? " + audioFile.exists());

        AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, 1024, 0);

        float actualSampleRate = dispatcher.getFormat().getSampleRate();
        System.out.println("Actual sample rate of this file: " + actualSampleRate + " Hz");
        System.out.println("Number of audio channels: " + dispatcher.getFormat().getChannels());
        System.out.println("Bits per sample: " + dispatcher.getFormat().getSampleSizeInBits());

        final int[] totalPitchCallbacks = {0};
        final int[] silenceCount = {0};
        final int[] outOfRangeCount = {0};

        PitchDetectionHandler pitchHandler = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult result, AudioEvent event) {
                float pitchInHz = result.getPitch();
                totalPitchCallbacks[0]++;

                if (pitchInHz == -1) {
                    silenceCount[0]++;
                } else if (pitchInHz < 60 || pitchInHz > 500) {
                    outOfRangeCount[0]++;
                    if (outOfRangeCount[0] <= 5) {
                        System.out.println("OUT OF RANGE example: " + pitchInHz + " Hz");
                    }
                } else {
                    pitchReadings.add(pitchInHz);
                    System.out.println("Pitch: " + pitchInHz + " Hz  at time " + event.getTimeStamp() + "s");
                }
            }
        };

        PitchProcessor pitchProcessor = new PitchProcessor(
                PitchEstimationAlgorithm.YIN,
                actualSampleRate,
                1024,
                pitchHandler
        );

        dispatcher.addAudioProcessor(pitchProcessor);
        dispatcher.run();

        System.out.println("Done analyzing the file!");
        System.out.println("Total pitch callbacks: " + totalPitchCallbacks[0]);
        System.out.println("Silence (-1) count: " + silenceCount[0]);
        System.out.println("Out-of-range count: " + outOfRangeCount[0]);

        double jitter = calculateJitter(pitchReadings);
        System.out.println("----------------------------------------");
        System.out.println("Number of pitch readings: " + pitchReadings.size());
        System.out.println("Average Jitter: " + jitter + " Hz");
    }

    private static double calculateJitter(List<Float> pitches) {
        if (pitches.size() < 2) {
            return 0.0;
        }
        double totalDifference = 0.0;
        for (int i = 0; i < pitches.size() - 1; i++) {
            float current = pitches.get(i);
            float next = pitches.get(i + 1);
            totalDifference += Math.abs(next - current);
        }
        return totalDifference / (pitches.size() - 1);
    }
}