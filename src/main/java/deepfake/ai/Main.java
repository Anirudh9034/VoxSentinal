package deepfake.ai;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static class LivenessResult {
        public double jitter;
        public double shimmer;
        public int silenceGapCount;
        public double gapConsistency;
        public int score;
        public String verdict;

        @Override
        public String toString() {
            return "Score: " + score + "/100 (" + verdict + ") | "
                    + "Jitter: " + jitter + " Hz | Shimmer: " + shimmer
                    + " | Gaps: " + silenceGapCount;
        }
    }

    public static void main(String[] args) throws Exception {

        File audioFile = new File("D:\\deepfake core(c person)\\voxsentinal\\src\\main\\resources\\voice_real.wav-mono.wav");

        LivenessResult result = analyzeAudioFile(audioFile);

        System.out.println("==========================================");
        System.out.println("FINAL RESULT: " + result);
    }

    public static LivenessResult analyzeAudioFile(File audioFile) throws Exception {

        System.out.println("Analyzing: " + audioFile.getName());
        System.out.println("File exists? " + audioFile.exists());

        final List<Float> pitchReadings = new ArrayList<>();
        final List<Double> loudnessReadings = new ArrayList<>();
        final List<Double> silenceGapLengths = new ArrayList<>();
        final boolean[] currentlySilent = {false};
        final double[] silenceStartTime = {0.0};

        AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, 1024, 0);
        float actualSampleRate = dispatcher.getFormat().getSampleRate();
        System.out.println("Sample rate: " + actualSampleRate + " Hz, Channels: " + dispatcher.getFormat().getChannels());

        PitchDetectionHandler pitchHandler = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult pitchResult, AudioEvent event) {
                float pitchInHz = pitchResult.getPitch();
                if (pitchInHz != -1 && pitchInHz >= 60 && pitchInHz <= 500) {
                    pitchReadings.add(pitchInHz);
                }
            }
        };
        PitchProcessor pitchProcessor = new PitchProcessor(
                PitchEstimationAlgorithm.YIN, actualSampleRate, 1024, pitchHandler
        );

        final double SILENCE_THRESHOLD = 0.01;

        AudioProcessor combinedProcessor = new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                double rms = audioEvent.getRMS();
                double time = audioEvent.getTimeStamp();

                if (rms > SILENCE_THRESHOLD) {
                    loudnessReadings.add(rms);
                    if (currentlySilent[0]) {
                        silenceGapLengths.add(time - silenceStartTime[0]);
                        currentlySilent[0] = false;
                    }
                } else {
                    if (!currentlySilent[0]) {
                        currentlySilent[0] = true;
                        silenceStartTime[0] = time;
                    }
                }
                return true;
            }

            @Override
            public void processingFinished() {
            }
        };

        dispatcher.addAudioProcessor(pitchProcessor);
        dispatcher.addAudioProcessor(combinedProcessor);
        dispatcher.run();

        LivenessResult result = new LivenessResult();
        result.jitter = averageDifferenceFloat(pitchReadings);
        result.shimmer = averageDifferenceDouble(loudnessReadings);
        result.silenceGapCount = silenceGapLengths.size();
        result.gapConsistency = standardDeviation(silenceGapLengths);

        int score = 100;
        if (result.jitter < 12) score -= 35;
        if (result.shimmer < 0.002 || result.shimmer > 0.05) score -= 15;
        if (result.silenceGapCount < 1 || result.silenceGapCount > 6) score -= 25;
        if (result.gapConsistency < 0.03 && result.silenceGapCount >= 2) score -= 25;

        result.score = score;
        if (score >= 75) result.verdict = "LIKELY REAL";
        else if (score >= 50) result.verdict = "UNCERTAIN";
        else result.verdict = "LIKELY SYNTHETIC / SUSPICIOUS";

        return result;
    }

    private static double averageDifferenceFloat(List<Float> values) {
        if (values.size() < 2) return 0.0;
        double total = 0.0;
        for (int i = 0; i < values.size() - 1; i++) total += Math.abs(values.get(i + 1) - values.get(i));
        return total / (values.size() - 1);
    }

    private static double averageDifferenceDouble(List<Double> values) {
        if (values.size() < 2) return 0.0;
        double total = 0.0;
        for (int i = 0; i < values.size() - 1; i++) total += Math.abs(values.get(i + 1) - values.get(i));
        return total / (values.size() - 1);
    }

    private static double standardDeviation(List<Double> values) {
        if (values.size() < 2) return 0.0;
        double sum = 0.0;
        for (double v : values) sum += v;
        double mean = sum / values.size();
        double sumSquaredDiff = 0.0;
        for (double v : values) sumSquaredDiff += (v - mean) * (v - mean);
        return Math.sqrt(sumSquaredDiff / values.size());
    }
}