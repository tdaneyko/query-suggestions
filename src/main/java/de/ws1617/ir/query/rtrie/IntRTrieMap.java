/*
 * Author: Thora Daneyko, 3822667
 * Honor Code:  I pledge that this program represents my own work.
 *
 * (Based on my submission for assignment 2)
 */

package de.ws1617.ir.query.rtrie;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * A map from Strings to positive integers, stored in a trie structure
 * to enable prefix searches. The underlying trie is a randomized
 * ternary search trie.
 */
public class IntRTrieMap {
    
    public static final int NO_ENTRY_VALUE = -1;
    private static final int r = Integer.MAX_VALUE; // maximum priority value of nodes
    
    private IntRTrieNode root; // root node
    private int sum; // sum of all values in this trie

    public IntRTrieMap() {
        sum = 0;
        root = null;
    }

    public IntRTrieMap(String s, int val) {
        sum = val;
        root = new IntRTrieNode(s, 0, val);
    }

    public int sum() {
        return sum;
    }

    /**
     * Gets the value stored with string s in this trie.
     * @param s string to check
     * @return the value associated with s, or NO_ENTRY_VALUE if it is not contained
     */
    public int get(String s) {
        if (root == null || s.isEmpty())
            return NO_ENTRY_VALUE;
        return root.get(s, 0);
    }

    /**
     * Inserts a new string into the trie.
     * @param s the string to insert
     * @param val the string's value
     */
    public void insert(String s, int val) {
        sum += val;
        root = (root == null) ? new IntRTrieNode(s, 0, val) : root.insert(s, 0, val);
    }

    /**
     * Searches for all strings in the trie starting with the specified prefix.
     * @param prefix the prefix to search for
     * @return a map from all strings starting with prefix to their value
     */
    public TObjectIntMap<String> prefixSearch(String prefix) {
        TObjectIntMap<String> results = new TObjectIntHashMap<>();
        if (!(root == null || prefix.isEmpty()))
            root.prefixSearch(prefix, 0, results);
        return results;
    }

    public TIntSet getValuesWithPrefix(String prefix) {
        return root.valueSearch(prefix, 0);
    }

    /**
     * Constructs a trie containing a list of words.
     * @param words the list of words
     * @return a trie containing all words
     */
    public static IntRTrieMap constructTrie(TObjectIntMap<String> words) {
        TObjectIntIterator<String> iter = words.iterator();
        if (iter.hasNext()) {
            iter.advance();
            IntRTrieMap trie = new IntRTrieMap(iter.key(), iter.value());
            while (iter.hasNext()) {
                iter.advance();
                trie.insert(iter.key(), iter.value());
            }
            return trie;
        }
        return null;
    }

    @Override
    public String toString() {
        return root.toString();
    }


    /**
     * A node of a randomized ternary search trie.
     */
    private class IntRTrieNode {
        char splitchar; // the char represented by this node
        IntRTrieNode eqkid; // the mid child
        IntRTrieNode lokid; // the left child
        IntRTrieNode hikid; // the right child
        int val; // value of string, NO_ENTRY_VALUE if no string ends here
        int prio; // priority value between 1 and r

        /**
         * Constructs a new subtrie starting at char i in string s with a random priority.
         * @param s string to be inserted
         * @param i index of current character
         * @param val value of string
         */
        public IntRTrieNode(String s, int i, int val) {
            this(s, i, val, (int) (Math.random()*(r-1))+1);
        }

        /**
         * Constructs a new subtrie starting at char i in string s with a specified priority.
         * @param s string to be inserted
         * @param i index of current character
         * @param prio priority of this node
         */
        public IntRTrieNode(String s, int i, int val, int prio) {
            this.splitchar = s.charAt(i);
            this.eqkid = (i < (s.length()-1)) ? new IntRTrieNode(s, ++i, val, prio) : null; // only create eqkid if there are more characters to insert
            this.lokid = null;
            this.hikid = null;
            this.val = (eqkid == null) ? val : NO_ENTRY_VALUE; // accept if this is the end of the string
            this.prio = prio;
        }

        /**
         * Gets the value stored with a substring s starting at index i if it
         * is contained in the subtrie starting with the current node.
         * @param s string to be matched
         * @param i index of current character
         * @return the value associated with s, or NO_ENTRY_VALUE if it is not contained
         */
        public int get(String s, int i) {
            // false if i is out of bounds
            if (i == s.length())
                return NO_ENTRY_VALUE;

            // check in left child if s(i) is smaller than splitchar
            if (s.charAt(i) < splitchar) {
                if (lokid == null)
                    return NO_ENTRY_VALUE;
                else
                    return lokid.get(s, i);
            }

            // check in right child if s(i) is larger than splitchar
            if (s.charAt(i) > splitchar) {
                if (hikid == null)
                    return NO_ENTRY_VALUE;
                else
                    return hikid.get(s, i);
            }

            if (s.charAt(i) == splitchar) {
                // check if node is accepting if s(i) is the end of the string
                if (i == (s.length()-1))
                    return val;
                // if there are more characters, continue searching in eqkid
                else {
                    if (eqkid == null)
                        return NO_ENTRY_VALUE;
                    else
                        return eqkid.get(s, ++i);
                }
            }

            // unreachable, just to make the compiler happy
            return NO_ENTRY_VALUE;
        }

        public IntRTrieNode insert(String s, int i) {
            return insert(s, i, 1);
        }

        /**
         * Inserts string s starting at i into this subtrie.
         * @param s the string to insert
         * @param i index of current character
         * @return the potentially altered version of this node
         */
        public IntRTrieNode insert(String s, int i, int val) {

            // insert in left child if s(i) is smaller than splitchar
            if (s.charAt(i) < splitchar) {
                if (lokid == null)
                    lokid = new IntRTrieNode(s, i, val);
                else
                    lokid = lokid.insert(s, i, val);
            }

            // insert in right child if s(i) is larger than splitchar
            if (s.charAt(i) > splitchar) {
                if (hikid == null)
                    hikid = new IntRTrieNode(s, i, val);
                else
                    hikid = hikid.insert(s, i, val);
            }

            if (s.charAt(i) == splitchar) {
                // if s(i) matches splitchar and there are more characters, continue inserting in eqkid
                if (i < (s.length()-1)) {
                    if (eqkid == null)
                        eqkid = new IntRTrieNode(s, ++i, val, this.prio);
                    else {
                        eqkid = eqkid.insert(s, ++i, val);
                        prio = eqkid.prio;
                    }
                }
                // if this is the end of the string, make node accepting
                else
                    this.val = val;
            }

            IntRTrieNode newNode = this;
            // if lokid's priority is now higher than this node's and hikid's, rotate
            if ((lokid != null && lokid.prio > this.prio)
                    && (hikid == null || lokid.prio >= hikid.prio))
                newNode = this.rotateWithLo();
            // if hikid's priority is now higher than this node's and lokid's, rotate
            if ((hikid != null && hikid.prio > this.prio)
                    && (lokid == null || hikid.prio >= lokid.prio))
                newNode = this.rotateWithHi();

            return newNode;
        }

        /**
         * Rotates with node with lokid.
         * @return the rotated node
         */
        private IntRTrieNode rotateWithLo() {
            IntRTrieNode oldLo = lokid;
            lokid = oldLo.hikid;
            oldLo.hikid = this;
            return oldLo;
        }

        /**
         * Rotates with node with hikid.
         * @return the rotated node
         */
        private IntRTrieNode rotateWithHi() {
            IntRTrieNode oldHi = hikid;
            hikid = oldHi.lokid;
            oldHi.lokid = this;
            return oldHi;
        }

        /**
         * Searches for all strings in the trie starting with the specified prefix.
         * @param prefix the prefix to search for
         * @param start index of current character
         * @param results the set to place the results in
         */
        public void prefixSearch(String prefix, int start, TObjectIntMap<String> results) {
            // if the whole prefix has been matched, collect suffixes of this node,
            // concatenate them with the prefix and place them in the set
            if (start == (prefix.length()-1) && prefix.charAt(start) == splitchar) {
                if (eqkid != null) {
                    TObjectIntMap<String> suffixes = eqkid.getSuffixes();
                    for (String suffix : suffixes.keys(new String[suffixes.size()]))
                            results.put(prefix + suffix, suffixes.get(suffix));
                }
                // add prefix itself if this node is accepting
                if (val != NO_ENTRY_VALUE)
                    results.put(prefix, val);
            }
            // else continue matching the prefix
            else {
                if (prefix.charAt(start) < splitchar && lokid != null)
                    lokid.prefixSearch(prefix, start, results);

                if (prefix.charAt(start) > splitchar && hikid != null)
                    hikid.prefixSearch(prefix, start, results);

                if (prefix.charAt(start) == splitchar && eqkid != null)
                    eqkid.prefixSearch(prefix, ++start, results);
                }
            // if none of the above conditions holds, the string is not in the trie and the set remains empty
            }

        /**
         * Collects all suffixes of the current node
         * @return a set of suffixes
         */
        private TObjectIntMap<String> getSuffixes() {
            TObjectIntMap<String> suffixes = new TObjectIntHashMap<>();

            // if this node is accepting, add its splitchar
            if (val != NO_ENTRY_VALUE)
                suffixes.put(Character.toString(splitchar), val);

            // get suffixes of eqkid and concatenate with splitchar
            if (eqkid != null) {
                TObjectIntMap<String> eqsuf = eqkid.getSuffixes();
                for (String suffix : eqsuf.keys(new String[eqsuf.size()]))
                    suffixes.put(splitchar + suffix, eqsuf.get(suffix));
            }

            // get suffixes of lokid
            if (lokid != null)
                suffixes.putAll(lokid.getSuffixes());

            // get suffixes of hikid
            if (hikid != null)
                suffixes.putAll(hikid.getSuffixes());

            return suffixes;
        }

        /**
         * Searches for all values in the trie of strings starting with the specified prefix.
         * @param prefix the prefix to search for
         * @param start index of current character
         * @return a set of values
         */
        public TIntSet valueSearch(String prefix, int start) {
            // if the whole prefix has been matched, collect suffixes of this node,
            // concatenate them with the prefix and place them in the set
            if (start == (prefix.length()-1) && prefix.charAt(start) == splitchar) {
                if (eqkid != null) {
                    TIntSet values = eqkid.getValues();
                    // add value of this node if it is accepting
                    if (val != NO_ENTRY_VALUE)
                        values.add(val);
                    return values;
                }
            }
            // else continue matching the prefix
            else {
                if (prefix.charAt(start) < splitchar && lokid != null)
                    return lokid.valueSearch(prefix, start);

                if (prefix.charAt(start) > splitchar && hikid != null)
                    return hikid.valueSearch(prefix, start);

                if (prefix.charAt(start) == splitchar && eqkid != null)
                    return eqkid.valueSearch(prefix, ++start);
            }
            // if none of the above conditions holds, the string is not in the trie and the set is empty
            return new TIntHashSet();
        }

        /**
         * Collects all values below the current node
         * @return a set of values
         */
        private TIntSet getValues() {
            TIntSet values = new TIntHashSet();

            // if this node is accepting, add its value
            if (val != NO_ENTRY_VALUE)
                values.add(val);

            // get values of eqkid
            if (eqkid != null)
                values.addAll(eqkid.getValues());

            // get values of lokid
            if (lokid != null)
                values.addAll(lokid.getValues());

            // get values of hikid
            if (hikid != null)
                values.addAll(hikid.getValues());

            return values;
        }
    }
}
