package hu.ujvari.ecgprocessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for segmentation of ECG signal based on R waves
 */
public class ECGSegmenter {
    // Configuration constant for the width of the smoothing transition
    private static final int DEFAULT_TRANSITION_WIDTH = 35; // Wider default transition

    public static List<Integer> detectRPeaks(List<Double> signal, double threshold) {
        List<Integer> peaks = new ArrayList<>();
        System.out.println("[DEBUG] R peak detection called. Threshold: " + threshold);
        
        // At 1000 Hz sampling rate, this corresponds to 60-100 bpm heart rate window
        int windowSize = 400;  // 400 ms window
        int stepSize = 200;    // 200 ms step size (50% overlap)
        
        // Sliding window through the signal
        for (int startIdx = 0; startIdx < signal.size() - windowSize/2; startIdx += stepSize) {
            int endIdx = Math.min(startIdx + windowSize, signal.size());
            
            // Find the maximum value in the current window
            int maxIdx = startIdx;
            double maxVal = signal.get(startIdx);
            
            for (int i = startIdx + 1; i < endIdx; i++) {
                if (signal.get(i) > maxVal) {
                    maxVal = signal.get(i);
                    maxIdx = i;
                }
            }
            
            // If the max value exceeds the threshold, consider it a peak
            if (maxVal > threshold) {
                // Refine the peak position by checking local neighborhood
                int refinedPeakIdx = refineRPeakPosition(signal, maxIdx);
                
                // Check if this peak is already present or too close to an existing one
                boolean isNewPeak = true;
                for (int existingPeak : peaks) {
                    if (Math.abs(existingPeak - refinedPeakIdx) < 100) { // minimum 100 ms apart
                        isNewPeak = false;
                        break;
                    }
                }
                
                if (isNewPeak) {
                    peaks.add(refinedPeakIdx);
                }
            }
        }
        
        // Sort the peaks in ascending order
        peaks.sort(Integer::compareTo);
        
        return peaks;
    }

    // Helper method for refining the detected peak position
    private static int refineRPeakPosition(List<Double> signal, int approximatePeakIdx) {
        // Small window around the detected peak
        int refineWindow = 30; // ±30 ms
        int startIdx = Math.max(0, approximatePeakIdx - refineWindow);
        int endIdx = Math.min(signal.size() - 1, approximatePeakIdx + refineWindow);
        
        int maxIdx = approximatePeakIdx;
        double maxVal = signal.get(approximatePeakIdx);
        
        // Search for local maximum
        for (int i = startIdx; i <= endIdx; i++) {
            if (signal.get(i) > maxVal) {
                maxVal = signal.get(i);
                maxIdx = i;
            }
        }
        
        // Verify that it is truly a local maximum
        boolean isLocalMax = true;
        if (maxIdx > 0 && signal.get(maxIdx) <= signal.get(maxIdx - 1)) {
            isLocalMax = false;
        }
        if (maxIdx < signal.size() - 1 && signal.get(maxIdx) <= signal.get(maxIdx + 1)) {
            isLocalMax = false;
        }
        
        return isLocalMax ? maxIdx : approximatePeakIdx;
    }
    
    /**
     * Applies a filter segment-wise to the ECG signal while preserving the R peaks
     * @param signal The original ECG signal
     * @param filter The filter function to be applied
     * @param rPeakThreshold Threshold for R peak detection
     * @return The filtered signal and list of detected R peak indices
     */
    public static SegmentationResult applyFilterBySegments(List<Double> signal, FilterFunction filter, double rPeakThreshold) {
        // Detect R peaks
        List<Integer> rPeakIndices = detectRPeaks(signal, rPeakThreshold);
        
        // Prepare result container
        List<Double> result = new ArrayList<>();
        
        // If no peaks detected, apply the filter to the entire signal
        if (rPeakIndices.isEmpty()) {
            return new SegmentationResult(filter.apply(signal), rPeakIndices);
        }

        // First segment: from 0 to the first peak
        int prevIdx = 0;
        for (int i = 0; i < rPeakIndices.size(); i++) {
            int currIdx = rPeakIndices.get(i);

            // Cut out the current segment
            List<Double> segment = signal.subList(prevIdx, currIdx);
            System.out.println("[DEBUG] Segment " + i + ": " + segment.size() + " samples, from " + prevIdx + " to " + currIdx);

            // Apply the filter
            List<Double> filteredSegment = filter.apply(segment);

            // Add the filtered segment to the result
            result.addAll(filteredSegment);

            prevIdx = currIdx;
        }

        // Last segment: from last peak to the end
        if (prevIdx < signal.size()) {
            List<Double> segment = signal.subList(prevIdx, signal.size());
            List<Double> filteredSegment = filter.apply(segment);
            result.addAll(filteredSegment);
        }
        
        return new SegmentationResult(result, rPeakIndices);
    }

    public static SegmentationResult applyFilterBySegments(
        List<Double> signal,
        FilterFunction filter,
        List<Integer> rPeakIndices
    ) {
        // If no peaks are given, apply the filter to the entire signal
        if (rPeakIndices == null || rPeakIndices.isEmpty()) {
            return new SegmentationResult(filter.apply(signal), new ArrayList<>());
        }
        
        List<Double> result = new ArrayList<>(signal.size());
        List<Integer> newPeakIndices = new ArrayList<>();
        
        System.out.println("[DEBUG] Performing segmented filtering with " + rPeakIndices.size() + " R peaks");

        int transitionZone = DEFAULT_TRANSITION_WIDTH;

        List<Integer> sortedPeaks = new ArrayList<>(rPeakIndices);
        Collections.sort(sortedPeaks);

        // Ensure all peaks are within the signal boundaries
        sortedPeaks.removeIf(idx -> idx < 0 || idx >= signal.size());

        // Create a copy of the signal to be modified
        List<Double> modifiedSignal = new ArrayList<>(signal);

        // Filter the signal segment by segment
        int prevIdx = 0;
        for (int i = 0; i < sortedPeaks.size(); i++) {
            int peakIdx = sortedPeaks.get(i);
            
            if (peakIdx >= signal.size()) {
                System.out.println("[WARNING] R peak index (" + peakIdx + ") exceeds signal length (" + signal.size() + ")");
                continue;
            }

            // Filter the segment before the peak
            if (peakIdx > prevIdx) {
                List<Double> segment = signal.subList(prevIdx, peakIdx);
                List<Double> filteredSegment = filter.apply(segment);

                for (int j = 0; j < filteredSegment.size(); j++) {
                    int signalIdx = prevIdx + j;
                    modifiedSignal.set(signalIdx, filteredSegment.get(j));
                }
            }

            // The peak value itself remains unchanged
            prevIdx = peakIdx + 1;
        }

        // Last segment: from last peak to the end
        if (prevIdx < signal.size()) {
            List<Double> segment = signal.subList(prevIdx, signal.size());
            List<Double> filteredSegment = filter.apply(segment);

            for (int j = 0; j < filteredSegment.size(); j++) {
                int signalIdx = prevIdx + j;
                modifiedSignal.set(signalIdx, filteredSegment.get(j));
            }
        }

        // Smooth transitions around R peaks
        for (int peakIdx : sortedPeaks) {
            // Transition zone before the peak
            int beforeStart = Math.max(0, peakIdx - transitionZone);
            for (int i = beforeStart; i < peakIdx; i++) {
                double weight = (double)(i - beforeStart) / (peakIdx - beforeStart);
                double filteredValue = modifiedSignal.get(i);
                double peakValue = signal.get(peakIdx);
                modifiedSignal.set(i, filteredValue * (1 - weight) + peakValue * weight);
            }

            // Transition zone after the peak
            int afterEnd = Math.min(signal.size() - 1, peakIdx + transitionZone);
            for (int i = peakIdx + 1; i <= afterEnd; i++) {
                double weight = (double)(afterEnd - i) / (afterEnd - peakIdx);
                double filteredValue = modifiedSignal.get(i);
                double peakValue = signal.get(peakIdx);
                modifiedSignal.set(i, filteredValue * (1 - weight) + peakValue * weight);
            }
        }

        result = modifiedSignal;
        newPeakIndices = sortedPeaks;

        System.out.println("[DEBUG] Original signal length: " + signal.size() + ", Filtered signal length: " + result.size());

        return new SegmentationResult(result, newPeakIndices);
    }

    public static class SegmentationResult {
        private List<Double> filteredSignal;
        private List<Integer> rPeakIndices;

        public SegmentationResult(List<Double> filteredSignal, List<Integer> rPeakIndices) {
            this.filteredSignal = filteredSignal;
            this.rPeakIndices = rPeakIndices;
        }

        public List<Double> getFilteredSignal() { return filteredSignal; }
        public List<Integer> getRPeakIndices() { return rPeakIndices; }
    }

    /**
     * Functional interface for applying custom filters to a signal
     */
    @FunctionalInterface
    public interface FilterFunction {
        List<Double> apply(List<Double> signal);
    }
}
