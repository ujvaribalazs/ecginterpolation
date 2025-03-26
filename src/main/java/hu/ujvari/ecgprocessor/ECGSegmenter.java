package hu.ujvari.ecgprocessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Segédosztály az EKG jel szegmentálására R hullámok alapján
 */
public class ECGSegmenter {
    // Konfigurációs konstans a simítási átmenet szélességére
    private static final int DEFAULT_TRANSITION_WIDTH = 35; // Szélesebb alapértelmezett átmenet
    /**
     * R csúcsokat detektál a jelben ablakozással
     * @param signal Az EKG jel
     * @param threshold Küszöbérték az R csúcsok detektálásához
     * @return A detektált R csúcsok indexeinek listája
     */
    public static List<Integer> detectRPeaks(List<Double> signal, double threshold) {
        List<Integer> peaks = new ArrayList<>();
        System.out.println("[DEBUG] R peak detection called. Threshold: " + threshold);
        // 1000 Hz mintavételezésnél egy 60-100 bpm szívfrekvenciának megfelelő ablak
        int windowSize = 400;  // 400 ms egy ablak
        int stepSize = 200;    // 200 ms lépésköz (50% átfedés)
        
        
        // Menjünk végig a jelen ablakozva
        for (int startIdx = 0; startIdx < signal.size() - windowSize/2; startIdx += stepSize) {
            int endIdx = Math.min(startIdx + windowSize, signal.size());
            
            // Keressük meg a legnagyobb értéket az ablakban
            int maxIdx = startIdx;
            double maxVal = signal.get(startIdx);
            
            for (int i = startIdx + 1; i < endIdx; i++) {
                if (signal.get(i) > maxVal) {
                    maxVal = signal.get(i);
                    maxIdx = i;
                }
            }
            
            // Ha a maximum értéke nagyobb, mint a küszöbérték, akkor ez egy csúcs
            if (maxVal > threshold) {
                // Finomítsuk a csúcs pozícióját, ellenőrizve a lokális környezetet
                int refinedPeakIdx = refineRPeakPosition(signal, maxIdx);
                
                // Ellenőrizzük, hogy ez a csúcs nincs-e már a listában vagy annak közelében
                boolean isNewPeak = true;
                for (int existingPeak : peaks) {
                    if (Math.abs(existingPeak - refinedPeakIdx) < 100) { // 100 ms minimális távolság
                        isNewPeak = false;
                        break;
                    }
                }
                
                if (isNewPeak) {
                    peaks.add(refinedPeakIdx);
                }
            }
        }
        
        // Rendezzük a csúcsokat növekvő sorrendbe
        peaks.sort(Integer::compareTo);
        
        return peaks;
    }

    // Segédmetódus a csúcs pozíciójának finomításához
    private static int refineRPeakPosition(List<Double> signal, int approximatePeakIdx) {
        // Kis ablak a pontos csúcs meghatározásához
        int refineWindow = 30; // ±30 ms
        int startIdx = Math.max(0, approximatePeakIdx - refineWindow);
        int endIdx = Math.min(signal.size() - 1, approximatePeakIdx + refineWindow);
        
        int maxIdx = approximatePeakIdx;
        double maxVal = signal.get(approximatePeakIdx);
        
        // Keressük meg a valódi lokális maximumot
        for (int i = startIdx; i <= endIdx; i++) {
            if (signal.get(i) > maxVal) {
                maxVal = signal.get(i);
                maxIdx = i;
            }
        }
        
        // Ellenőrizzük, hogy valóban lokális maximum-e
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
     * Egy szűrőt alkalmaz szegmentáltan az EKG jelre, megőrizve az R csúcsokat
     * @param signal Az eredeti EKG jel
     * @param filter A használandó szűrő funkció
     * @param rPeakThreshold Küszöbérték az R csúcsok detektálásához
     * @return A szegmentáltan szűrt jel
     */
    public static SegmentationResult applyFilterBySegments(List<Double> signal, FilterFunction filter, double rPeakThreshold) {
        // R csúcsok detektálása
        List<Integer> rPeakIndices = detectRPeaks(signal, rPeakThreshold);
        
        // Eredmény előkészítése
        List<Double> result = new ArrayList<>();
        
        // Ha nincsenek detektált R csúcsok, alkalmazzuk a szűrőt a teljes jelre
        if (rPeakIndices.isEmpty()) {
            return new SegmentationResult(filter.apply(signal), rPeakIndices);
        }
        
              
        
        // Első szakasz: 0-tól az első csúcsig
        int prevIdx = 0;
        for (int i = 0; i < rPeakIndices.size(); i++) {
            int currIdx = rPeakIndices.get(i);
            
            

            // Kivágjuk az adott szakaszt
            List<Double> segment = signal.subList(prevIdx, currIdx);
            System.out.println("[DEBUG] Segment " + i + ": " + segment.size() + " samples, from " + prevIdx + " to " + currIdx);


            // Alkalmazzuk a szűrőt
            List<Double> filteredSegment = filter.apply(segment);

            // Hozzáadjuk a szűrt szakaszt az eredményhez
            result.addAll(filteredSegment);

            prevIdx = currIdx;
        }

        // Utolsó szakasz: az utolsó csúcstól a végéig
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
        // Ha nincsenek detektált R csúcsok, alkalmazzuk a szűrőt a teljes jelre
        if (rPeakIndices == null || rPeakIndices.isEmpty()) {
            return new SegmentationResult(filter.apply(signal), new ArrayList<>());
        }
        
        List<Double> result = new ArrayList<>(signal.size());
        List<Integer> newPeakIndices = new ArrayList<>();
        
        System.out.println("[DEBUG] Alkalmazunk szegmentált szűrést " + rPeakIndices.size() + " R csúccsal");
        
        // Az átmeneti zóna szélessége (minták száma)
        int transitionZone = DEFAULT_TRANSITION_WIDTH;
        
        // Rendezzük a csúcsokat, hogy biztosan növekvő sorrendben legyenek
        List<Integer> sortedPeaks = new ArrayList<>(rPeakIndices);
        Collections.sort(sortedPeaks);
        
        // Ellenőrzés: minden csúcs a jel tartományán belül van?
        sortedPeaks.removeIf(idx -> idx < 0 || idx >= signal.size());
        
        // Előre betöltjük a jelet egy új listába, amit módosítani fogunk
        List<Double> modifiedSignal = new ArrayList<>(signal);
        
        // Szegmensenként szűrjük a jelet
        int prevIdx = 0;
        for (int i = 0; i < sortedPeaks.size(); i++) {
            int peakIdx = sortedPeaks.get(i);
            
            // Ellenőrizzük, hogy az index érvényes-e
            if (peakIdx >= signal.size()) {
                System.out.println("[WARNING] R csúcs index (" + peakIdx + ") nagyobb, mint a jel hossza (" + signal.size() + ")");
                continue;
            }
            
            // Szűrjük a szakaszt a csúcs előtt
            if (peakIdx > prevIdx) {
                List<Double> segment = signal.subList(prevIdx, peakIdx);
                List<Double> filteredSegment = filter.apply(segment);
                
                // Másoljuk a szűrt eredményt a módosított jelbe
                for (int j = 0; j < filteredSegment.size(); j++) {
                    int signalIdx = prevIdx + j;
                    modifiedSignal.set(signalIdx, filteredSegment.get(j));
                }
            }
            
            // A csúcs értékét érintetlenül hagyjuk
            
            // A következő szakasz a csúcs utáni indextől kezdődik
            prevIdx = peakIdx + 1;
        }
        
        // Utolsó szakasz: az utolsó csúcstól a végéig
        if (prevIdx < signal.size()) {
            List<Double> segment = signal.subList(prevIdx, signal.size());
            List<Double> filteredSegment = filter.apply(segment);
            
            // Másoljuk a szűrt eredményt a módosított jelbe
            for (int j = 0; j < filteredSegment.size(); j++) {
                int signalIdx = prevIdx + j;
                modifiedSignal.set(signalIdx, filteredSegment.get(j));
            }
        }
        
        // Most létrehozzuk a sima átmeneteket az R csúcsok körül
        for (int peakIdx : sortedPeaks) {
            // A csúcs előtti átmeneti zóna
            int beforeStart = Math.max(0, peakIdx - transitionZone);
            for (int i = beforeStart; i < peakIdx; i++) {
                // Lineáris interpoláció az átmeneti zónában
                double weight = (double)(i - beforeStart) / (peakIdx - beforeStart);
                double filteredValue = modifiedSignal.get(i);
                double peakValue = signal.get(peakIdx);
                // Simább átmenet a szűrt érték és a csúcsérték között
                modifiedSignal.set(i, filteredValue * (1 - weight) + peakValue * weight);
            }
            
            // A csúcs utáni átmeneti zóna
            int afterEnd = Math.min(signal.size() - 1, peakIdx + transitionZone);
            for (int i = peakIdx + 1; i <= afterEnd; i++) {
                // Lineáris interpoláció az átmeneti zónában
                double weight = (double)(afterEnd - i) / (afterEnd - peakIdx);
                double filteredValue = modifiedSignal.get(i);
                double peakValue = signal.get(peakIdx);
                // Simább átmenet a csúcsérték és a szűrt érték között
                modifiedSignal.set(i, filteredValue * (1 - weight) + peakValue * weight);
            }
        }
        
        // Az eredmény a módosított jel
        result = modifiedSignal;
        
        // A csúcsok indexei ugyanazok maradnak
        newPeakIndices = sortedPeaks;
        
        System.out.println("[DEBUG] Eredeti jel hossza: " + signal.size() + ", Szűrt jel hossza: " + result.size());
        
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
     * Funkcionális interfész a különböző szűrők használatához
     */
    @FunctionalInterface
    public interface FilterFunction {
        List<Double> apply(List<Double> signal);
    }
}