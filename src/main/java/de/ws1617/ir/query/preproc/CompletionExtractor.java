/*
 * Author: Thora Daneyko, 3822667
 * Honor Code:  I pledge that this program represents my own work.
 */

package de.ws1617.ir.query.preproc;

import com.google.common.base.Splitter;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.io.LineIterator;

import java.io.*;
import java.util.*;

/**
 * This class extracts unigrams and docsets from corpus data files.
 */
public class CompletionExtractor implements NGramExtractor {

    // the amount by which the cleanup threshold increases if necessary
    private static final int CLEANUP_INCREASE = 1;
    // the current cleanup frequency threshold
    private int c;

    // a splitter on whitespaces
    private static final Splitter split = Splitter.on(' ');

    // the set of stopwords
    private final Set<String> stopwords;
    // the unigrams with frequencies
    private HashMap<String, Frequency> freqs;
    // the current number of documents recorded
    private int D;


    public CompletionExtractor(Set<String> stopwords) {
        this.freqs = new HashMap<>();
        this.stopwords = stopwords;
        D = 0;

        this.c = CLEANUP_INCREASE;
    }

    /**
     * Extracts unigrams and docsets from a file.
     * @param input a reader over a corpus file
     */
    @Override
    public void extract(Reader input) {
        LineIterator iter = new LineIterator(input);
        extract(iter);
        iter.close();
    }

    /**
     * Extracts unigrams and docsets from a file.
     * @param input an iterator over the lines of a corpus file
     */
    @Override
    public void extract(Iterator<String> input) {
        // runtime to check memory usage
        Runtime r = Runtime.getRuntime();

        while (input.hasNext()) {
            String line = input.next();

            for (String word : split.split(line)) {
                // increase doc counter at document boundaries
                if (word.equals("<newdoc>"))
                    D++;
                // record unigram only if it is not a stop word, number or single character
                else if (!stopwords.contains(word) && !isNumber(word) && word.length() > 1) {
                    if (freqs.containsKey(word))
                        freqs.get(word).count(D);
                    else
                        freqs.put(word, new Frequency(D));
                }

                // if 75% of available memory are used, perform a cleanup
                if (r.totalMemory() - r.freeMemory() > 0.75 * r.maxMemory())
                    cleanup();
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
        freqs.entrySet().removeIf(term -> term.getValue().tf <= threshold);
        System.gc();
    }

    /**
     * @return an iterator over the extracted unigrams and docsets
     */
    @Override
    public CompletionIterator iterator() { return new CompletionIterator(); }

    /**
     * Returns an iterator over the extracted unigrams and docsets after performing
     * a cleanup with the given threshold.
     * @param threshold frequency threshold below which entries should be removed
     * @return an iterator over the extracted unigrams
     */
    @Override
    public CompletionIterator iterator(int threshold) {
        cleanup(threshold);
        return new CompletionIterator();
    }


    /**
     * Stores a set of documents in which a term occurs and its term frequency.
     */
    private class Frequency {

        private TIntList docs;
        private int tf;

        public Frequency(int docID) {
            docs = new TIntArrayList();
            docs.add(docID);
            tf = 1;
        }

        public void count(int docID) {
            tf++;
            if (docID != docs.get(docs.size()-1))
                docs.add(docID);
        }
    }


    /**
     * An iterator over the extracted unigrams and docsets.
     */
    public class CompletionIterator implements NGramIterator {

        Iterator<Map.Entry<String, Frequency>> iter;
        Map.Entry<String, Frequency> current;

        private CompletionIterator() {
            this.iter = freqs.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public void advance() {
            current = iter.next();
        }

        @Override
        public String getNGram() {
            return current.getKey();
        }

        /**
         * @return the tf-idf of the current unigram
         */
        @Override
        public double getWeightedFrequency() {
            Frequency freq = current.getValue();
            return freq.tf*Math.log(D/freq.docs.size());
        }

        public TIntList getDocSets() {
            return current.getValue().docs;
        }
    }
}
