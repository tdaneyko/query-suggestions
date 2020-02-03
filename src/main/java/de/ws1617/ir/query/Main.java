/*
 * Author: Thora Daneyko, 3822667
 * Honor Code:  I pledge that this program represents my own work.
 */

package de.ws1617.ir.query;

import de.ws1617.ir.query.preproc.DataCompressor;
import de.ws1617.ir.query.sugg.QuerySuggestor;

import java.io.*;
import java.util.List;
import java.util.Scanner;

/**
 * This class accepts incomplete user queries and suggests possible
 * completions and extensions to the user.
 */
public class Main {

    // the number of suggestions
    private static final int NO_OF_RESULTS = 10;
    // the directory of the data files
    private static final String DATA_DIR = "src/main/resources/";


    public static void main(String[] args) {
        // load all data files
        QuerySuggestor q = load();

        // start dialogue with user
        Scanner user = new Scanner(System.in);
        System.out.print("Enter your query: ");
        // get query and convert to lower case
        String query = user.nextLine().toLowerCase();

        while (!query.equals("-exit")) {
            // checks whether the last word is a partial or complete term
            boolean complete = query.charAt(query.length()-1) == ' ';
            // end index of the query
            int end = (complete) ? query.length()-1 : query.length();
            // start index of last term
            int split1 = query.lastIndexOf(' ', end-1);
            // the last term
            String qWord = query.substring(split1+1, end);
            // the query term preceding the last term
            String context = "";
            // all other query terms entered so far
            String greaterContext = "";
            // get context if available
            if (split1 > 0) {
                int split2 = query.lastIndexOf(' ', split1-1);
                context = query.substring(split2+1, split1);
                // get greater context if available
                if (split2 > 0) {
                    greaterContext = query.substring(0, split2);
                }
            }

            // get query suggestions
            long start = System.currentTimeMillis();
            List<String> c = (complete)
                    ? q.queryComplete(context, qWord, NO_OF_RESULTS)
                    : q.queryPartial(context, qWord, NO_OF_RESULTS);
            long finish = System.currentTimeMillis() - start;

            // print suggestions
            for (String result : c) {
                System.out.print("> ");
                if (!greaterContext.isEmpty())
                    System.out.print(greaterContext + ' ');
                System.out.println(result);
            }
            System.out.println("Retrieved in " + finish + "ms.");

            // continue
            System.out.print("Enter your query: ");
            // get next query and convert to lower case
            query = user.nextLine().toLowerCase();
        }
    }


    private static QuerySuggestor load() {

        try {
            long start = System.currentTimeMillis();
            
            System.out.println("Loading word to id mappings...");
            BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(DATA_DIR+"wordIDs"), "UTF-8"));
            List<String> IDToWord = DataCompressor.readWordIDsToList(read);
            read.close();

            QuerySuggestor q = new QuerySuggestor(IDToWord);

            System.out.println("Loading docsets...");
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(DATA_DIR+"docsets")));
            q.addDocSets(DataCompressor.docSetIterator(in));
            in.close();

            System.out.println("Loading unigrams...");
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(DATA_DIR+"ngrams-1")));
            q.addUnigrams(DataCompressor.nGramIterator(in));
            in.close();

            System.out.println("Loading bigrams...");
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(DATA_DIR+"ngrams-2")));
            q.addNGrams(DataCompressor.nGramIterator(in));
            in.close();

            System.out.println("Loading trigrams...");
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(DATA_DIR+"ngrams-3")));
            q.addNGrams(DataCompressor.nGramIterator(in));
            in.close();
            
            System.out.println("Launched in " + (System.currentTimeMillis() - start) + " ms.");

            return q;
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

}
