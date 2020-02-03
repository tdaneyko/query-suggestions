/*
 * Author: Thora Daneyko, 3822667
 * Honor Code:  I pledge that this program represents my own work.
 */

package de.ws1617.ir.query.preproc;

import java.io.Reader;
import java.util.Iterator;

public interface NGramExtractor {
    void extract(Reader input);
    void extract(Iterator<String> input);
    NGramIterator iterator();
    NGramIterator iterator(int threshold);

    interface NGramIterator {
        boolean hasNext();
        void advance();
        String getNGram();
        double getWeightedFrequency();
    }
}
