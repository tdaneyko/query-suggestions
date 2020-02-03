/*
 * Author: Thora Daneyko, 3822667
 * Honor Code:  I pledge that this program represents my own work.
 */

package de.ws1617.ir.query.sugg;

import com.google.common.primitives.Ints;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import de.ws1617.ir.query.preproc.DataCompressor;
import de.ws1617.ir.query.rtrie.IntRTrieMap;

import java.util.*;

/**
 * This class generates suggestions for possible queries given a
 * partial query by a user.
 */
public class QuerySuggestor {

    // a mapping from IDs to words
    private List<String> IDToWord;
    // a mapping from unigrams to IDs
    private IntRTrieMap unigramToID;

    // unigrams and their frequencies
    private TIntDoubleMap unigrams;
    // a mapping from unigrams to all n-grams that contain them
    // and their respective frequencies
    private TIntObjectMap<List<NGram>> ngrams;
    // a mapping from unigrams to the set of documents they occur in
    private TIntObjectMap<int[]> docSets;


    public QuerySuggestor(List<String> IDToWord) {
        this.IDToWord = IDToWord;
        this.unigramToID = new IntRTrieMap();

        this.unigrams = new TIntDoubleHashMap();
        this.ngrams = new TIntObjectHashMap<>();
        this.docSets = new TIntObjectHashMap<>();
    }

    /**
     * Adds a collection of unigrams with frequencies to the data pool.
     * @param iter an iterator over unigrams and their frequencies
     */
    public void addUnigrams(DataCompressor.NGramFileIterator iter) {
        while (iter.advance()) {
            int unigram = iter.getNGram()[0];
            unigrams.put(unigram, iter.getFrequency());
            unigramToID.insert(IDToWord.get(unigram), unigram);
        }
    }

    /**
     * Adds a collection of n-grams with frequencies to the data pool.
     * @param iter an iterator over n-grams and their frequencies
     */
    public void addNGrams(DataCompressor.NGramFileIterator iter) {
        while (iter.advance())
            addNGram(iter.getNGram(), iter.getFrequency());
    }

    /**
     * Adds a single n-gram with frequency to the data pool.
     * @param ngram n-gram as an array of word IDs
     * @param freq the n-gram's frequency
     */
    public void addNGram(int[] ngram, double freq) {
        addNGram(new NGram(ngram, freq));
    }

    /**
     * Adds a single n-gram with frequency to the data pool.
     * @param ngram the n-gram with its frequency
     */
    private void addNGram(NGram ngram) {
        for (int word : ngram.ngram) {
            if (unigrams.containsKey(word)) {
                if (!ngrams.containsKey(word))
                    ngrams.put(word, new ArrayList<>());
                ngrams.get(word).add(ngram);
            }
        }
    }

    /**
     * Adds a collection of unigrams with docsets to the data pool.
     * @param iter an iterator over unigrams and their docsets
     */
    public void addDocSets(DataCompressor.DocSetFileIterator iter) {
        while (iter.advance())
            docSets.put(iter.getTerm(), iter.getDocs());
    }


    /**
     * Retrieves suggestions for extending a query with a partial last
     * query term.
     * @param context the word preceding the last query term
     * @param partial the partially entered last query term
     * @param n the number of suggestions to make
     * @return a list with the top n suggestions
     */
    public List<String> queryPartial(String context, String partial, int n) {
        // get id of context (will be -1 if there is no corresponding unigram)
        int contextID = unigramToID.get(context);
        // retrieve completions
        TIntSet completions = getCompletions(contextID, partial);
        if (completions.isEmpty()) return new ArrayList<>();
        // retrieve phrases containing these completions
        TObjectDoubleMap<int[]> phrases = getPhrases(completions, contextID);
        // only keep top phrases; get more than n in case there will be duplicates
        // after appending the context
        int[][] topPhrases = getTopN(phrases, (int)(n*1.5));
        // convert results to strings
        List<String> results = translateNGrams(topPhrases, context);
        // get the most frequent completion to return with the results
        String topCompletion = IDToWord.get(getMostFrequentCompletion(topPhrases, completions));
        // prepend context to completion
        if (contextID >= 0) topCompletion = context + ' ' + topCompletion;
        // add completion to results
        results.add(0, topCompletion);
        // remove duplicates and return top n results
        removeDuplicates(results);
        if (results.size() <= n) return results;
        return removeDuplicates(results).subList(0, n);
    }

    /**
     * Retrieves suggestions for extending a query with a complete last
     * query term.
     * @param context the word preceding the last query term
     * @param lastTerm the last query term
     * @param n the number of suggestions to make
     * @return a list with the top n suggestions
     */
    public List<String> queryComplete(String context, String lastTerm, int n) {
        // if the last term is unknown, return nothing
        if (unigramToID.get(lastTerm) == -1) return new ArrayList<>();
        // get phrases containing the last term
        TObjectDoubleMap<int[]> phrases = getPhrasesforCompletion(unigramToID.get(lastTerm), 1, unigramToID.get(context));
        // get top 1.5n phrases, convert to strings, remove duplicates and return top n
        return removeDuplicates(translateNGrams(getTopN(phrases, (int)(n*1.5)), context)).subList(0, n);
    }

    /**
     * Gets the best completions for a partial term given a preceding
     * query term.
     * @param context the preceding query term
     * @param partial the partially entered query term
     * @return the best completions for the partial term
     */
    private TIntSet getCompletions(int context, String partial) {
        // get all possible completions for the partial string
        TIntSet completions = unigramToID.getValuesWithPrefix(partial);
        // remove context in case it is a possible completion
        completions.remove(context);
        // only return sqrt(n_c) (min. 10, max. 100) most probable
        // completions given the context
        int n = (completions.size() < 100) ? 10 : ((completions.size() > 10000) ? 100 : (int) Math.sqrt(completions.size()));
        return getTopNGivenContext(completions, n, context);
    }

    /**
     * Gets all phrases containing the given completions and the context
     * with their respective probabilities.
     * @param completions all completions
     * @param context the context
     * @return a map from phrases to probabilities
     */
    private TObjectDoubleMap<int[]> getPhrases(TIntSet completions, int context) {
        // calculate sum of tf-idfs of all completions and check
        // whether there are more than 10000 candidate phrases
        double csum = 0;
        int numPhrases = 0;
        boolean tooManyPhrases = false;
        for (TIntIterator iter = completions.iterator(); iter.hasNext(); ) {
            int unigram = iter.next();
            csum += unigrams.get(unigram);
            if (!tooManyPhrases && ngrams.containsKey(unigram)) {
                numPhrases += ngrams.get(unigram).size();
                tooManyPhrases = numPhrases > 10000;
            }
        }

        // get phrases
        TObjectDoubleMap<int[]> phrases = new TObjectDoubleHashMap<>();
        for (TIntIterator iter = completions.iterator(); iter.hasNext(); ) {
            int unigram = iter.next();
            // calculate term completion probability (eq. 9 in paper)
            double termCompletionProbability = unigrams.get(unigram) / csum;
            // if there are too many phrases, select completions as results
            if (tooManyPhrases) {
                TIntList complDocs = new TIntArrayList(docSets.get(unigram));
                // calculate completion query correlation (eq. 5 in report);
                // 1 without known context, because else the overall
                // probability for all completions would be 0
                double complQueryCorrelation = (context == -1) ? 1
                        : intersect(complDocs, docSets.get(context)).size() / (double) complDocs.size();
                phrases.put(new int[]{unigram}, termCompletionProbability*complQueryCorrelation);
            }
            // else get phrases for completions
            else
                phrases.putAll(getPhrasesforCompletion(unigram, termCompletionProbability, context));
        }

        return phrases;
    }

    /**
     * Gets all phrases containing the given completion and the context
     * with their respective probabilities.
     * @param completion the completion
     * @param termCompletionProbability the probability of this completion
     * @param context the context
     * @return a map from phrases to probabilities
     */
    private TObjectDoubleMap<int[]> getPhrasesforCompletion(int completion, double termCompletionProbability, int context) {
        TObjectDoubleMap<int[]> phrases = new TObjectDoubleHashMap<>();
        if (ngrams.containsKey(completion)) {
            // get phrases containing unigram
            List<NGram> extensions = ngrams.get(completion);
            // get sum of the normalized frequencies of all phrases
            double psum = 0;
            for (NGram ngram : extensions)
                    psum += ngram.freq;

            for (NGram ngram : extensions) {
                // calculate term to phrase probability (eq. 11 in paper)
                double termToPhraseProbability = ngram.freq / psum;
                // calculate phrase selection probability (eq. 8 in paper)
                double phraseSelectionProbability = termCompletionProbability * termToPhraseProbability;
                // calculate phrase query correlation (eq. 13 in paper);
                // 1 without known context, because else the overall
                // probability for all phrases would be 0
                TIntList phraseDocs = getDocsWithPhrase(ngram);
                double phraseQueryCorrelation = (context == -1) ? 1
                        : intersect(phraseDocs, docSets.get(context)).size() / (double) phraseDocs.size();
                // calculate overall probabilty (eq. 7 in paper) and add phrase
                // and frequency to results
                phrases.put(ngram.ngram, phraseSelectionProbability * phraseQueryCorrelation);
            }
        }

        return phrases;
    }

    /**
     * Gets all documents that contain a given phrase, or rather all
     * documents that contain all words in the query that we have a
     * docset for (as suggested in eq. 14 in the paper).
     * @param phrase the phrase
     * @return the documents containing that phrase
     */
    private TIntList getDocsWithPhrase(NGram phrase) {
        // Sort query terms according to frequency
        List<int[]> candidates = new ArrayList<>();
        for (int word : phrase.ngram) {
            if (docSets.containsKey(word))
                candidates.add(docSets.get(word));
        }
        candidates.sort(new ArrayLengthComparator());

        // Repeatedly intersect the terms from lowest to highest frequency
        TIntList result = new TIntArrayList(candidates.get(0));
        for (int i = 1; i < candidates.size(); i++)
            if (candidates.get(i).length > 0)
                result = intersect(result, candidates.get(i));

        return result;
    }

    /**
     * Intersect a sorted list and a sorted array of ints.
     * @param l1 sorted integer list
     * @param l2 sorted integer array
     * @return intersection
     */
    private TIntList intersect(TIntList l1, int[] l2) {
        if (l2 == null)
            return l1;

        TIntList intersection = new TIntArrayList();

        int i = 0;
        int j = 0;
        while (i != l1.size() && j != l2.length) {
            if (l1.get(i) == l2[j]) {
                intersection.add(l1.get(i));
                i++;
                j++;
            }
            else if (l1.get(i) < l2[j])
                i++;
            else
                j++;
        }

        return intersection;
    }

    /**
     * Extracts the n phrases with the highest probabilities, sorted
     * in descending order.
     * @param orig the phrases with their probabilities
     * @param n number of phrases to return
     * @return the top n phrases
     */
    private int[][] getTopN(TObjectDoubleMap<int[]> orig, int n) {
        // sort phrases according to their probabilities in descending order
        List<int[]> ranked = new ArrayList<>(orig.keySet());
        Comparator<int[]> phraseRanker = (s1, s2) -> Double.compare(orig.get(s2), orig.get(s1));
        ranked.sort(phraseRanker);
        // return n topmost phrases
        int[][] topN = new int[Math.min(n, ranked.size())][];
        for (int i = 0; i < topN.length; i++)
            topN[i] = ranked.get(i);
        return topN;
    }

    /**
     * Extracts the n completions with the highest correlation with
     * the given context.
     * @param orig the completions
     * @param n number of completions to return
     * @param context
     * @return
     */
    private TIntSet getTopNGivenContext(TIntSet orig, int n, int context) {
        // return original if it has n or less elements
        if (orig.size() <= n) return orig;

        // else start filtering
        List<Integer> ranked = new ArrayList<>();
        ranked.addAll(Ints.asList(orig.toArray()));

        // sort completions according to completion query correlation or,
        // if the context is unknown, after their tf-idfs in descending order
        if (context == -1) {
            ranked.sort((s1, s2) -> Double.compare(unigrams.get(s2), unigrams.get(s1)));
        }
        else {
            TIntList contextDocs = new TIntArrayList(docSets.get(context));
            TIntDoubleMap completionQueryCorrelation = new TIntDoubleHashMap();
            for (int c : ranked) {
                int[] docs = docSets.get(c);
                completionQueryCorrelation.put(c, intersect(contextDocs, docs).size() / (double) docs.length);
            }
            ranked.sort((s1, s2) -> Double.compare(completionQueryCorrelation.get(s2), completionQueryCorrelation.get(s1)));
        }

        // return n topmost completions
        TIntSet topN = new TIntHashSet();
        for (int i = 0; i < n; i++)
            topN.add(ranked.get(i));
        return topN;
    }

    /**
     * Get string representations of the given n-grams and append
     * the context.
     * @param ngrams the n-grams
     * @param context the context
     * @return the n-grams with context as strings
     */
    private List<String> translateNGrams(int[][] ngrams, String context) {
        List<String> results = new ArrayList<>();
        for (int i = 0; i < ngrams.length; i++)
            results.add(translateNGram(ngrams[i], context));
        return results;
    }

    /**
     * Get string representation of the given n-gram and append
     * the context.
     * @param ngram the n-gram
     * @param context the context
     * @return the n-gram with context as a string
     */
    private String translateNGram(int[] ngram, String context) {
        StringBuilder s = new StringBuilder();
        // stores whether the context is already included in the phrase
        boolean hasContext = false;
        for (int id : ngram) {
            String word = IDToWord.get(id);
            if (word.equals(context))
                hasContext = true;
            s.append(' ').append(word);
        }
        // only append context if it is not already contained in the phrase
        if (hasContext || context.isEmpty())
            return s.deleteCharAt(0).toString();
        else
            return s.insert(0, context).toString();
    }

    /**
     * Get the completion that occurs most frequently in the given phrases.
     * @param phrases the phrases
     * @param completions the completions
     * @return the most frequent completion
     */
    private int getMostFrequentCompletion(int[][] phrases, TIntSet completions) {
        // get words in phrases with frequencies
        TIntIntMap freqs = new TIntIntHashMap();
        for (int[] phrase : phrases) {
            for (int word : phrase) {
                freqs.adjustOrPutValue(word, 1, 1);
            }
        }
        // get most frequent completion
        int fav = -1;
        for (TIntIterator iter = completions.iterator(); iter.hasNext(); ) {
            int compl = iter.next();
            if (fav == -1 || freqs.get(compl) > freqs.get(fav))
                fav = compl;
        }
        return fav;
    }

    /**
     * Removes duplicate entries from a list. Modifies the given list.
     * @param l the list
     * @return the list without duplicate entries
     */
    private List<String> removeDuplicates(List<String> l) {
        Set<String> noDuplicates = new LinkedHashSet<>(l);
        l.clear();
        l.addAll(noDuplicates);
        return l;
    }


    /**
     * A representation of an n-gram with its normalized frequency.
     */
    private class NGram {
        private int[] ngram;
        private double freq;

        public NGram(int[] ngram, double freq) {
            this.ngram = ngram;
            this.freq = freq;
        }

        public boolean contains(int word) {
            for (int w : ngram)
                if (w == word) return true;
            return false;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof NGram) {
                NGram otherNGram = (NGram) other;
                return this.freq == otherNGram.freq && Arrays.equals(this.ngram, otherNGram.ngram);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (13 + Arrays.hashCode(ngram)) * (31 + Double.hashCode(freq)) + 11;
        }
    }
}
