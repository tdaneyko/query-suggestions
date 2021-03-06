/*
 * Author: Thora Daneyko, 3822667
 * Honor Code:  I pledge that this program represents my own work.
 */

package de.ws1617.ir.query.preproc;

import com.google.common.base.Splitter;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class keeps a mapping from single words to integer ids and is
 * able to print the n-gram data stored by NGramExtractors in a condensed,
 * memory-saving byte format. It also provides static methods to load
 * these compressed files back into memory.
 */
public class DataCompressor {

    // a splitter on whitespaces
    private static final Splitter splitter = Splitter.on(' ');
    // a map from strings to integer ids
    private TObjectIntMap<String> wordIDs;


    public DataCompressor() {
        wordIDs = new TObjectIntHashMap<>();
    }

    public DataCompressor(TObjectIntMap<String> wordIDs) {
        this.wordIDs = wordIDs;
    }

    /**
     * Iterates over the n-grams in an NGramExtractor, converts each word in
     * each n-gram to a unique integer id and prints these converted n-grams
     * and their respective frequencies to a file. The format for each n-gram
     * is:
     * - int id of each word in the n-gram, printed one after the other
     * - a -1 int to signal that the n-gram is complete
     * - the frequency of the n-gram as a double
     * Since the frequency is always a single double, no separator is needed
     * between n-grams. Each frequency is always followed by the next n-gram
     * or the end of the file.
     * @param iter an iterator over the n-grams
     * @param out the output stream
     * @throws IOException
     */
    public void printNGramsToFile(NGramExtractor.NGramIterator iter, DataOutputStream out) throws IOException {
        while (iter.hasNext()) {
            iter.advance();
            for (Iterator<String> words = splitter.split(iter.getNGram()).iterator(); words.hasNext(); )
                out.writeInt(convert(words.next()));
            out.writeInt(-1);
            out.writeDouble(iter.getWeightedFrequency());
            out.flush();
        }
    }

    /**
     * Iterates over the docsets in a CompletionExtractor, converts each
     * unigram into a unique integer id, and prints this integer id together
     * with the ids of the documents in which this unigram occurs to a file.
     * The format for each docset is:
     * - int id the unigram as an int
     * - int id of each document in the docset
     * - a -1 int to signal that the docset is complete
     * Since the unigram is always just one int long, no separator is needed
     * between a unigram and its corresponding docset. The first int after a
     * -1 (or at the beginning of the file) is always a unigram id, and every-
     * thing that follows is a document id.
     * @param iter an iterator over the docsets
     * @param out the output stream
     * @throws IOException
     */
    public void printDocSetsToFile(CompletionExtractor.CompletionIterator iter, DataOutputStream out) throws IOException {
        while (iter.hasNext()) {
            iter.advance();
            out.writeInt(convert(iter.getNGram()));
            TIntList docs = iter.getDocSets();
            for (int i = 0; i < docs.size(); i++)
                out.writeInt(docs.get(i));
            out.writeInt(-1);
            out.flush();
        }
    }

    /**
     * Prints the word to id mappings to a file. In order to save disk space,
     * the mappings are first sorted according to ids. Since there are no
     * gaps within the ids, they do not need to be printed, since the id
     * will now correspond to the line number. Thus, the words in the map
     * are just printed alone, one word per line.
     * @param out a writer to the output file
     * @throws IOException
     */
    public void printMappingsToFile(PrintWriter out) throws IOException {
        String[] mappings = new String[wordIDs.size()];
        for (TObjectIntIterator<String> iter = wordIDs.iterator(); iter.hasNext(); ) {
            iter.advance();
            mappings[iter.value()] = iter.key();
        }
        for (int id = 0; id < mappings.length; id++) {
            out.println(mappings[id]);
        }
    }

    /**
     * Retrieves the id for this string, or creates a new one if it was
     * not encountered before.
     * @param word the string
     * @return the string's id
     */
    private int convert(String word) {
        if (wordIDs.containsKey(word))
            return wordIDs.get(word);
        else {
            int id = wordIDs.size();
            wordIDs.put(word, id);
            return id;
        }
    }

    /**
     * Reads a list of words as generated by printMappingsToFile() and stores
     * them in a list, with the list index corresponding to the word's id.
     * @param in a reader on the mappings file
     * @return a list of words
     * @throws IOException
     */
    public static List<String> readWordIDsToList(BufferedReader in) throws IOException {
        List<String> wordIDs = new ArrayList<>();
        for (String word = in.readLine(); word != null; word = in.readLine())
            wordIDs.add(word);
        return wordIDs;
    }

    /**
     * Reads a list of words as generated by printMappingsToFile() and stores
     * them in a map.
     * @param in a reader on the mappings file
     * @return a word-id map
     * @throws IOException
     */
    public static TObjectIntMap<String> readWordIDsToMap(BufferedReader in) throws IOException {
        TObjectIntMap<String> wordIDs = new TObjectIntHashMap<>();
        int id = 0;
        for (String word = in.readLine(); word != null; word = in.readLine(), id++)
            wordIDs.put(word, id);
        return wordIDs;
    }

    /**
     * @param in an n-gram file as generated by printNGramsToFile()
     * @return an iterator over the n-grams stored in this file
     */
    public static NGramFileIterator nGramIterator(DataInputStream in) { return new NGramFileIterator(in); }

    /**
     * @param in an n-gram file as generated by printDocSetsToFile()
     * @return an iterator over the docsets stored in this file
     */
    public static DocSetFileIterator docSetIterator(DataInputStream in) { return new DocSetFileIterator(in); }


    /**
     * An iterator over compressed n-gram data in a file.
     */
    public static class NGramFileIterator {

        private DataInputStream in;
        private int[] currentNgram;
        private double currentFreq;

        private NGramFileIterator(DataInputStream in) {
            this.in = in;
        }

        /**
         * Reads the next n-gram and its frequency.
         * @return true if the reading was successful, false if the end
         * of the file has been reached
         */
        public boolean advance() {
            try {
                TIntList ngram = new TIntArrayList();
                while (true) {
                    int i = in.readInt();
                    if (i == -1) {
                        currentNgram = ngram.toArray();
                        currentFreq = in.readDouble();
                        return true;
                    }
                    else {
                        ngram.add(i);
                    }
                }
            }
            catch (EOFException e) {
                currentNgram = null;
                currentFreq = 0;
                return false;
            }
            catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        /**
         * @return the current n-gram
         */
        public int[] getNGram() {
            return currentNgram;
        }

        /**
         * @return the frequency of the current n-gram
         */
        public double getFrequency() {
            return currentFreq;
        }

    }

    /**
     * An iterator over compressed docset data in a file.
     */
    public static class DocSetFileIterator {

        private DataInputStream in;
        private int currentTerm;
        private int[] currentDocs;

        private DocSetFileIterator(DataInputStream in) {
            this.in = in;
        }

        /**
         * Reads the next unigram and its document set.
         * @return true if the reading was successful, false if the end
         * of the file has been reached
         */
        public boolean advance() {
            try {
                currentTerm = in.readInt();
                TIntList docs = new TIntArrayList();
                while (true) {
                    int i = in.readInt();
                    if (i == -1) {
                        currentDocs = docs.toArray();
                        return true;
                    }
                    else {
                        docs.add(i);
                    }
                }
            }
            catch (EOFException e) {
                return false;
            }
            catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        /**
         * @return the current unigram
         */
        public int getTerm() {
            return currentTerm;
        }

        /**
         * @return the current docset
         */
        public int[] getDocs() {
            return currentDocs;
        }

    }
}
