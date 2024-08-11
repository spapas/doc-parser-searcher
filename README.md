# doc-parser-searcher

## A tool to help you index and search your documents.

This tool combines two great java libraries to help you index and then very fast search your documents:

* apache lucene (https://lucene.apache.org/) for searching text
* apache tika (https://tika.apache.org/) for extracting text from various types of files

With this tool you can select a folder which contains all your documents.
These documents will then be parsed, and you'll be able to search their contents. 

## Screenshot

![image](https://github.com/spapas/doc-parser-searcher/assets/3911074/c5a18c9e-3ab5-4a7a-8c93-1ad37cd26353)

## Requirements

You need java 18 to run this program.

## Usage

To run this download the `docparser-all.jar` from the 
[github releases](https://github.com/spapas/doc-parser-searcher/releases)
and run it with java using something like 

```
java -jar docparser-all.jar 
```

You need to pass a parameter to the program to indicate its mode of operation:

* server: Runs as a server, this is the main way to use this program. When running as a server it will first index all your documents and re-index them after a configurable interval.
* search: You can pass a query to search the documents
* parse: Parses/indexes your documents
* info: Prints info on your document index
* clear: Deletes the index so you can index everything again

The main mode of operation is the `server` so you can visit the configured host/port (by default 127.0.0.1:8080) and, after your documents have been indexed, search them. 

## How to search

You should search using the lucene query parser syntax:

https://lucene.apache.org/core/9_11_1/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package.description

Quick searching tutorial:
<ul>
<li><b>Simple:</b> Enter a word and it will search for it using stemming rules for the configured language(i.e if you search for "apple" it will also return documents containing "apples")</li>
<li><b>Phrase:</b> If you want to search for an exact phrase, f.e "hello, world" you need to enter it between quotes. If you enter two words without the quotes it will search for documents containing one of these words. So searching for hello, world (without quotes) will return documents containing hello and documents containing word (see boolean search for more explanation)</li>
<li><b>Wildcard:</b> You can do wildcard search: Searching for app* will return documents containing apple, applying or application. Use ? for a single letter, * for any number of characters and + for at least one character. The wildcard character cannot be on the start of your query, i.e *ppl will not work.</li>
<li><b>Boolean:</b> You can use boolean operators like AND OR and NOT to create more complex queries. Things like (apple AND orange) OR (not strawberry) should work. </li>
<li><b>Always include/exclude:</b> You can use the + or - operators before a word (or phrase) to include or exclude documents containing it. For example +apple +orange -strawberry will return documents containing apple and orange but not strawberry.</li>
<li><b>Distance:</b> You can search by distance using the ~ operator. For example, "commit local"~3 will search for documents that have the words commit and local on a distance less than 3. That means that a document containing the phrase "commit all changes to local dev" will be returned but a document with the phrase "commit all changes to production and local dev" will not work.</li>
<li><b>Filtering:</b> You can use the extra search choices to filter based on the name of the folder that contains the document or its created/modified/accessed date. For example if you write appl* to the folder it will only return documents that are contained within a folder named apples or applications (this includes all ancestor folders).</li>
<li><b>Combinations:</b> You can use all the above in whatever combinations: For example +"commit local"~3 +download -conf* will search documents containing the word commit near the word local and also contain the word download but do not contain any words starting with conf</li>
</ul>

## Configuration

The default configuration is this:

```
parser.parseDirectory=. # starts parsing from the directory your start the program from 
parser.dataDirectory=. # saves index data to the directory your start the program from
parser.interval=60 # re-indexes docs every 60 mins
parser.pageSize=10 # result page size
parser.analyzerClazzString=org.apache.lucene.analysis.el.GreekAnalyzer # use the correspoding analyzer for your language
parser.parseExtensions=doc,docx,xls,xlsx,ppt,pptx,odt,fodt,ods,fods,odp,fodp,txt,html,md,rst,rtf,pdf # parse allowed extensions 

server.port=8080 # Which port to listen to
server.host=127.0.0.1 # IP to bind to. Use 0.0.0.0 to allow remote connections 
server.userUsername= # Enables HTTP basic auth for users if set
server.userPassword= # Enables HTTP basic auth for users if set
server.adminUsername= # Enables HTTP basic auth for admins if set
server.adminPassword= # Enables HTTP basic auth for admins if set
```

Right now the admin doesn't have much functionality. Beyond
searching, it allows connecting to:

* /status to see the status of the server and 
* /docs to be able to see all the files that are indexed. 

To override the configuration you can copy over the
[application.local.props.template](https://github.com/spapas/doc-parser-searcher/blob/master/application.local.props.template)
to the same folder as the jar as `application.props` and edit it according to your needs.

Then pass it to the app using `-c`, for example:

`java -jar docparser-all.jar -c application.props server`

If you use the default configuration it will parse from the directory
you start the program from and keep its data to that directory.

### The analyzer

Lucene uses an "analyzer" to parse your documents and your search queries. The 
analyzer will proeprly transform the docs for each language (case sensitivity,
stemming, accents etc). By default, I'm using the analyzer for the greek
language, but you should use the correct on for your own language. See 
here for the existing available languages: 

https://lucene.apache.org/core/9_7_0/analysis/common/index.html

i.e for english use  `org.apache.lucene.analysis.en.EnglishAnalyzer`

### How this works

When the file parser sees a file it keeps its pathname and last modified date on a (persistant)
hashmap. This way the file won't need to be re-indexed if it hasn't been changed. You can observe this behavior
by running `parse`: The first time it will take a lot of time to index everything but if you re-run 
it should be much faster (notice it will need *some* time because it has to walk all files, if there are
a lot of files it will need a lot of time even if nothing has changed).

The application generates a `lucene_index` directory where the search 
index is saved and a `map.db` (and map.db.wal.*) file to keep the 
persistent hashmap (it uses the [mapdb](https://mapdb.org/) library for this).

The parser indexes the text, title, path and created/modified/accessed dates
for each document.

## Example

Indexing the docs of my organization(> 100k pdf/doc/xls etc files) takes a couple of hours and creates an index of ~ 1 GB. 
Then depending on the query results are returned in 100 ms to 1 second.  

## The OCR situation

The apache tika library allows you to use [tesseract OCR](https://github.com/tesseract-ocr/tesseract)
to read some files. You need to install tesseract to your server and then use a 
custom tika.config.xml by passing the `parser.externalTikaConfig` setting to 
your application.props. For example `parser.externalTikaConfig=c:\\progr\\kotlin\\doc-parser-searcher\\tika-config.xml`.
One sample tika config that uses tesseract can be found [here](https://github.com/spapas/doc-parser-searcher/blob/master/tika-config-ocr.xml).

*Warning*: Using the OCR is very slow. I have mainly included to test the functionality, I don't think that this is useful
in a lot of situations. Of course, you are free to use it if you need it. Finally, you need to properly set the language of the
documents you'll OCR or else your results will be very bad (using `<param name="language" type="string">ell</param>`).

## Development

I'm using Intellij Idea for development. You should be able to run it directly from Intellij if you wish. For deployment check the fatjar.bat file or .github/workflows/workflow.yml for how to
create a "fat" jar (will be built on `build\libs\docparser-all.jar`).

### Changes to fat.jar

This shouldn't be needed anymore.

- org.apache.lucene.codecs.lucene94.Lucene94Codec >> fat-jar/META-INF/services/org.apache.lucene.codecs.Codec
- org.apache.lucene.codecs.lucene90.Lucene90PostingsFormat >> fat-jar/META-INF/services/org.apache.lucene.codecs.PostingsFormat


## Changelog

- v1.3: Improve project docs and styling
- v1.2: Update dependencies
- v1.1: First public version
 
## About
If you find this project useful, consider
<a href="https://buymeacoffee.com/spapas">buying me a coffee</a>!