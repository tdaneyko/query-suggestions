/*
 * Author: Thora Daneyko, 3822667
 * Honor Code:  I pledge that this program represents my own work.
 */

package de.ws1617.ir.query.preproc;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import org.apache.commons.io.LineIterator;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * This class strips unnecessary information from the Wikicorpus files
 * and converts them into a format easily processable for Trainer.
 */
public class WikicorpusPreprocessor {

    // Matches the most common punctuation
    private static CharMatcher PUNCT = CharMatcher.anyOf(".,:;?¿!¡+-±×·_'\"«»/\\$£¢¥%@<>=~()[]|#&*§{}°º¹²³½¼ª´`^¨¸©®¬¦¤");
    // Matches characters marking word boundaries
    private static Splitter SPLIT = Splitter.on(CharMatcher.anyOf("_-"));

    /**
     * Converts all files in a directory and places the formatted equivalents
     * of these file in another directory.
     * @param indir the directory containing the originals
     * @param outdir the directory in which to place the processed files
     */
    public static void extractTokensInDir(File indir, File outdir) {
        for (File infile : indir.listFiles()) {
            File outfile = new File(outdir.getPath()+"/"+infile.getName());
            extractTokens(infile, outfile);
        }
    }

    /**
     * Converts a single file.
     * @param in the original file
     * @param out the file to write the results to
     */
    public static void extractTokens(File in, File out) {
        try (PrintWriter writ = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), Charset.forName("UTF-8")))) {
            LineIterator iter = new LineIterator(new BufferedReader(new InputStreamReader(new FileInputStream(in), "Cp1252")));
            extractTokens(iter, writ);
            iter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts a single file.
     * @param in an iterator over the lines in the original
     * @param out a writer to the output file
     */
    public static void extractTokens(Iterator<String> in, PrintWriter out) {
        // marks whether the end of a document has been reached
        boolean end = true;
        // stores the current sentence
        StringBuilder sentence = new StringBuilder();

        while (in.hasNext()) {
            String line = in.next();
            if (end) {
                // if a new document starts, print a tag to the output and set end flag to false
                if (line.startsWith("<doc")) {
                    out.println("<newdoc>");
                    end = false;
                }
            }
            else {
                // if the line is empty, a sentence has ended
                if (line.isEmpty()) {
                    // print sentence to file and empty StringBuilder
                    if (sentence.length() > 0) {
                        out.println(sentence.deleteCharAt(sentence.length() - 1).toString());
                        sentence.setLength(0);
                    }
                }
                // if the end of a document or its reference part has been reached, set end flag to true
                else if (line.startsWith("</doc") || line.startsWith("References") || line.startsWith("ENDOFARTICLE")) {
                    end = true;
                }
                // if a token if not a URL or an HTML tag, store it
                else if (!(line.startsWith("http://") || line.startsWith("www.") || line.charAt(0) == '<')) {
                    // end index of token field
                    int s = line.indexOf(' ');
                    if (s >= 0) {
                        // split multi-word tokens
                        for (Iterator<String> words = SPLIT.split(line.substring(0, s)).iterator(); words.hasNext(); ) {
                            // convert to lower case
                            String word = words.next().toLowerCase();
                            if (!word.isEmpty()) {
                                char firstChar = word.charAt(0);
                                // if word does not start with punctuation except for '
                                if (firstChar == '\'' || !PUNCT.matches(firstChar)) {
                                    // remove ' and concatenate tokens separated by '
                                    if (firstChar == '\'' && sentence.length() > 0)
                                        sentence.deleteCharAt(sentence.length() - 1);
                                    // append token
                                    sentence.append(PUNCT.removeFrom(word)).append(' ');
                                }
                            }
                        }
                    }
                }
            }
        }
        // print final sentence to file
        if (sentence.length() > 0) {
            out.println(sentence.deleteCharAt(sentence.length() - 1).toString());
        }
    }

    /**
     * Takes an input and an output file or directory and converts that file
     * or all files in that directory.
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java WikicorpusPreprocessor <infile/dir> <outfile/dir>");
            System.err.println("Input and output must either be both fils or both directories!");
        }
        else {
            File in = new File(args[0]);
            File out = new File(args[1]);
            if (in.isDirectory() && out.isDirectory()) {
                extractTokensInDir(in, out);
            }
            else if (in.isFile() && out.isFile()) {
                extractTokens(in, out);
            }
            else {
                System.err.println("Input and output must either be both fils or both directories!");
            }
        }
    }
}
