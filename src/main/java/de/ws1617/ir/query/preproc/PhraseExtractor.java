/*
 * Author: Thora Daneyko, 3822667
 * Honor Code:  I pledge that this program represents my own work.
 */

package de.ws1617.ir.query.preproc;

import com.google.common.base.Splitter;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.io.LineIterator;

import java.io.Reader;
import java.util.Iterator;
import java.util.Set;

/**
 * This class extracts n-grams from corpus data files.
 */
public class PhraseExtractor implements NGramExtractor {

    // the amount by which the cleanup threshold increases if necessary
    private static final int CLEANUP_INCREASE = 1;
    // the current cleanup frequency threshold
    private int c;

    // a splitter on whitespaces
    private static final Splitter split = Splitter.on(' ');

    // the length of the n-grams to be extracted
    private final int n;
    // the set of stopwords
    private final Set<String> stopwords;
    // the n-grams and their frequencies
    private TObjectIntMap<String> freqs;
    // the sum of all frequencies
    private long fsum;


    public PhraseExtractor(int n, Set<String> stopwords) {
        this.n = n;
        this.freqs = new TObjectIntHashMap<>();
        this.stopwords = stopwords;
        this.fsum = 0;

        this.c = CLEANUP_INCREASE;
    }

    /**
     * Extracts n-grams from a file.
     * @param input a reader over a corpus file
     */
    @Override
    public void extract(Reader input) {
        LineIterator iter = new LineIterator(input);
        extract(iter);
        iter.close();
    }

    /**
     * Extracts n-grams from a file. Only iterates the file once,
     * building n n-grams at a time.
     * @param input an iterator over the lines of a corpus file
     */
    @Override
    public void extract(Iterator<String> input) {
        // counts how many words have already been appended to the n-grams
        int[] idx = new int[n];
        // the n-grams
        StringBuilder[] s = new StringBuilder[n];
        // initialize
        for (int i = 0; i < n; i++) {
            // initialize to 0-i so that they start recording one after the other
            // so that every n-gram along the way is recorded
            idx[i] = 0-i;
            s[i] = new StringBuilder();
        }
        // runtime to check memory usage
        Runtime r = Runtime.getRuntime();

        while (input.hasNext()) {
            String line = input.next();

            for (String word : split.split(line)) {
                if (!word.equals("<newdoc>")) {
                    for (int i = 0; i < n; i++) {
                        // if word is a stop word, number or single character and
                        // not the first in the n-gram, only append, do not count
                        if (stopwords.contains(word) || isNumber(word) || word.length() == 1) {
                            if (idx[i] > 0) {
                                s[i].append(word).append(' ');
                            }
                        }
                        // else count
                        else {
                            // if count is negative (after initialization), do not record
                            if (idx[i] < 0) {
                                idx[i]++;
                            }
                            // if it is the last word, store the complete n-gram in the map,
                            // reset counter and StringBuilder and update frequency sum
                            else if (idx[i] == n - 1) {
                                freqs.adjustOrPutValue(s[i].append(word).toString(), 1, 1);
                                idx[i] = 0;
                                s[i].setLength(0);
                                fsum++;
                            }
                            // else just append and count
                            else {
                                s[i].append(word).append(' ');
                                idx[i]++;
                            }
                        }
                    }
                }

                // if 75% of available memory are used, perform a cleanup
                if (r.totalMemory() - r.freeMemory() > 0.75 * r.maxMemory())
                    cleanup();
            }

            // at the end of a sentence, reset counters and StringBuilders
            for (int i = 0; i < n; i++) {
                idx[i] = 0-i;
                s[i].setLength(0);
                s[i].trimToSize();
            }
        }
    }

    /**
     * Checks if a string is a number, i.e. if all of its characters
     * are digits.
     * @param s the string to check
     * @return true if it is a number, false if not
     */
    private boolean isNumber(String s) {
        for (char c : s.toCharArray())
            if (!Character.isDigit(c)) return false;
        return true;
    }

    /**
     * Performs a regular cleanup.
     */
    private void cleanup() {
        int prevSize = freqs.size();
        cleanup(c);

        // increase threshold for next regular cleanup if less than
        // half of the material was deleted
        if (freqs.size() > prevSize/2) c += CLEANUP_INCREASE;
    }

    /**
     * Performs a cleanup, i.e. removes all unigrams with a frequency
     * lower or equal to a given threshold and suggests garbage collection
     * afterwards.
     * @param threshold frequency threshold
     */
    private void cleanup(int threshold) {
        for (TObjectIntIterator iter = freqs.iterator(); iter.hasNext(); ) {
            iter.advance();
            if (iter.value() <= threshold) {
                fsum -= iter.value();
                iter.remove();
            }
        }
        System.gc();
    }

    /**
     * @return an iterator over the extracted n-grams
     */
    @Override
    public PhraseIterator iterator() { return new PhraseIterator(); }

    /**
     * Returns an iterator over the extracted n-grams after performing
     * a cleanup with the given threshold.
     * @param threshold frequency threshold below which entries should be removed
     * @return an iterator over the extracted n-grams
     */
    @Override
    public PhraseIterator iterator(int threshold) {
        cleanup(threshold);
        return new PhraseIterator();
    }


    /**
     * An iterator over the extracted n-grams.
     */
    public class PhraseIterator implements NGramIterator {

        TObjectIntIterator<String> iter;
        double logAvg;

        private PhraseIterator() {
            this.iter = freqs.iterator();
            this.logAvg = Math.log(((double) fsum) / freqs.size());
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public void advance() {
            iter.advance();
        }

        @Override
        public String getNGram() {
            return iter.key();
        }

        /**
         * @return the normalized frequency of the current n-gram
         */
        @Override
        public double getWeightedFrequency() {
            return iter.value() / logAvg;
        }
    }

}
