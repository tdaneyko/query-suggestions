/*
 * Author: Thora Daneyko, 3822667
 * Honor Code:  I pledge that this program represents my own work.
 */

package de.ws1617.ir.query.preproc;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

/**
 * This class generates n-grams of arbitrary length from preformatted
 * corpus data. It accepts input of the form:
 *
 * -in <infile(s)> -out <outfolder> -from <n> -to <n> -stop <optional stopword list>
 *
 * The -to parameter is inclusive, i.e. -from 1 -to 3 generates n-grams with
 * n = 1, 2 and 3.
 */
public class Trainer {

    // Minimum frequency of unigrams
    private static final int UNI_THRESHOLD = 5;
    // Minimum frequency of >1-grams
    private static final int GENERAL_THRESHOLD = 5;


    public static void main(String[] args) {
        String in = null;
        String out = null;
        int from = 0;
        int to = 0;
        String stopfile = null;
        // read provided arguments
        for (int i = 0; i < args.length-1; i++) {
            switch (args[i]) {
                case "-in": in = args[++i]; break;
                case "-out": out = args[++i]; break;
                case "-from": from = Integer.parseInt(args[++i]); break;
                case "-to": to = Integer.parseInt(args[++i]); break;
                case "-stop": stopfile = args[++i]; break;
                default:
                    System.err.println("Unknown parameter " + args[i] + ".");
                    printParameters();
                    System.exit(1);
            }
        }

        // check if all obligatory information was provided
        if (in == null || out == null || !new File(out).isDirectory() || from == 0 || to == 0) {
            System.err.println("Please specify all obligatory parameters.");
            printParameters();
        }

        // get input file(s)
        File infile = new File(in);
        File[] infiles = (infile.isDirectory()) ? infile.listFiles() : new File[]{infile};

        // read stopwords if existent
        Set<String> stopwords = new HashSet<>();
        if (stopfile != null) {
            try (BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(stopfile), "utf-8"))) {
                for (String stopword = read.readLine(); stopword != null; stopword = read.readLine())
                    stopwords.add(stopword);
            }
            catch (IOException e) {
                System.err.println("Could not read stopword file " + stopfile + ". Will train without stopwords.");
            }
        }

        // DataCompressor to write the generated data
        DataCompressor conv = new DataCompressor();

        // extract unigrams if needed and document sets for unigrams if required
        if (from == 1) {
            CompletionExtractor ex = new CompletionExtractor(stopwords);

            // unigrams
            extractAll(ex, infiles);
            printNGrams(ex.iterator(UNI_THRESHOLD), out + "ngrams-1", conv);

            // docsets
            try (DataOutputStream writ = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(out + "docsets")))) {
                conv.printDocSetsToFile(ex.iterator(UNI_THRESHOLD), writ);
            }
            catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }

            from++;
        }

        // extract all other n-grams
        for (int n = from; n <= to; n++) {
            PhraseExtractor ex = new PhraseExtractor(n, stopwords);
            extractAll(ex, infiles);
            printNGrams(ex.iterator(GENERAL_THRESHOLD), out + "ngrams-" + n, conv);
        }

        // save word IDs generated by DataCompressor
        try (PrintWriter writ = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out + "wordIDs"), Charset.forName("UTF-8")))) {
            conv.printMappingsToFile(writ);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Repeatedly feeds corpus files to the n-gram extractor until all data
     * has been processed.
     * @param ex the n-gram extractor
     * @param infiles the corpus files
     */
    private static void extractAll(NGramExtractor ex, File[] infiles) {
        for (File file : infiles) {
            try (BufferedReader read = new BufferedReader(new FileReader(file))) {
                ex.extract(read);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Makes the DataCompressor print the extracted n-grams in compressed format.
     * @param iter an iterator over the n-grams
     * @param outfile the output file
     * @param conv the DataCompressor
     */
    private static void printNGrams(NGramExtractor.NGramIterator iter, String outfile, DataCompressor conv) {
        try (DataOutputStream writ = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outfile)))) {
            conv.printNGramsToFile(iter, writ);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Prints information about this program.
     */
    private static void printParameters() {
        System.err.println("Applicable parameters:");
        System.err.println("\t-in: Training data file (obligatory)");
        System.err.println("\t-out: Output file (obligatory)");
        System.err.println("\t-from: Smallest n (>0) (obligatory)");
        System.err.println("\t-to: Largest n (>0) (inclusive!) (obligatory)");
        System.err.println("\t-stop: Stopword file (optional)");
    }

}
