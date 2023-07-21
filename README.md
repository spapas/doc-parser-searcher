# doc-parser-searcher

## A tool to help you index and search your documents.

This tool combines two great java libraries to help you index and then very fast search your documents:

* apache lucene (https://lucene.apache.org/) for searching text
* apache tika (https://tika.apache.org/) for extracting text from various types of files

The app 

## Usage


## Example

Indexing the docs of my organization(> 100k pdf/doc/xls etc files) takes a couple of hours and creates an index of ~ 1 GB. 
Then depending on the query results are returned in 100 ms to 1 second.  


## Changes to fat.jar

- org.apache.lucene.codecs.lucene94.Lucene94Codec >> fat-jar/META-INF/services/org.apache.lucene.codecs.Codec
- org.apache.lucene.codecs.lucene90.Lucene90PostingsFormat >> fat-jar/META-INF/services/org.apache.lucene.codecs.PostingsFormat