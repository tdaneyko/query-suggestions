# Query Suggestions

This is the semester project I did for the course "Information Retrieval" taught
by Daniël de Kok at the University of Tübingen in the winter semester 2016/2017.

The `Main` class asks the user to input partial queries and generates up to
ten meaningful suggestions for complete queries (similar to how Google will start
guessing your intended queries once you start typing in the search bar).

## What is it about?

_(From the Introduction section of my project report, to be found under
report/project_report.pdf)_

When entering a query into a search engine, it is often difficult to formulate
it in such a way that it is informative for the search engine about the user’s
information need. Because of this, many search engines suggest likely query
terms or phrases depending on what the user has entered so far in order to aid
him expressing his information need in a helpful way. Many search engines rely
on a large amount of query logs, i.e. queries that have been submitted before,
and can deduct the most likely queries from this data. However, newer search
engines or information retrieval systems that do not receive as many queries
as web search engines, cannot rely on a sufficiently large query log corpus and
may never be able to collect enough data from their users to provide reasonable
query suggestions.

n-grams extracted from large text corpora can be a good indicator of which
words frequently occur together and can thus be used to supplement or replace
query logs as a basis for query suggestions when there would otherwise not be
enough data to generate proper results. Bhatia, Majumdar, and Mitra 2011
have implemented such a query suggestion system based on n-gram data and
have shown that it can produce high quality results. However, they used very
speciﬁc corpora as a basis and only tested it on queries surrounding the same
topics covered by these corpora.

In this report, I discuss my implementation of an n-gram based query suggestion
system mostly following the probabilistic approach proposed by Bhatia,
Majumdar, and Mitra 2011. The data underlying my system is taken from a
large Wikipedia corpus, thus covering a broad variety of topics. It will be
interesting to see whether it can generate meaningful query suggestions for all kinds
of information needs or whether the specificity of Bhatia, Majumdar, and Mitra
2011’s data is crucial to its success.

## Running the project
The query suggestion program needs n-gram data to run. There are two possibilities
to get this data:

* Download it from [my Dropbox](https://www.dropbox.com/sh/uuoq0x357qu79ji/AACtxRqETVsxncticMHICvNba)
  (~850 MB unzipped), place the files in _src/main/resources_ (on the same level as
  _stop.txt_) and skip the preprocessing part, or
* generate it using the preprocessing classes, as outlined below.

## Preprocessing

If you wish to run the preprocessing yourself, do the following:

1. Download the tagged version of the English Wikicorpus from the
   [Wikicorpus website](http://www.cs.upc.edu/~nlp/wikicorpus/).
2. Run `WikicorpusPreprocessor` with the following arguments:
   `<folder with Wikicorpus files> <folder in which to place processed files>`
3. Run `Trainer` with the following arguments:
   `-in <folder with processed Wikicorpus> -out src/main/resources/
    -from 1 -to 3 -stop src/main/resources/stop.txt`
