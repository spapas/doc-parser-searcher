# doc-parser-searcher

## A tool to help you index and search your documents.

This tool combines two great java libraries to help you index and then very fast search your documents:

* apache lucene (https://lucene.apache.org/) for searching text
* apache tika (https://tika.apache.org/) for extracting text from various types of files

With this tool you can select a folder which contains all your documents.
These documents will then be parsed and you'll be able to search their contents. 

## Screenshot

![image](https://github.com/spapas/doc-parser-searcher/assets/3911074/c5a18c9e-3ab5-4a7a-8c93-1ad37cd26353)

## Requirements

You need java 14 to run this program.

## Usage

To run this download the `docparser-all.jar` from the 
[github releases](https://github.com/spapas/doc-parser-searcher/releases).
You can then run it with java using something like 

```
java -jar docparser-all.jar
```

You need to pass a parameter to the program to indicate its mode of operation:

* server: Runs as a server, this is the main way to use this program. When running as a server it will first index all your documents and re-index them after a configurable interval.
* search: You can pass a query to search the documents
* parse: Parses/indexes your documents
* info: Prints info on your document index
* clear: Deletes the index so you can index everything again

When run as server you can visit the configured host/port (by default 127.0.0.1:8080)
and, after your documents have been indexed, search them. 

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

Right now the admin doesn't have much functionality, it just allows connecting to
to /status to see the status of the server and 
/keys to be able to see all the files that are indexed. 

To override the configuration you can create an 
`application.props` file with the settings you need to override
and then pass it to the app using `-c` for 
example:

`java -jar docparser-all.jar -c application.local.props server`

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
by running `parse`: The first time it will take a lot of time to index everything but if you re-run it 
it should be much faster (notice it will need *some* time because it has to walk all files).

The application generates a `lucene_index` directory where there search 
index is saved and a map.db (and map.db.wal.*) to keep the persistant hasmap (it
uses [mapdb](https://mapdb.org/) for this).

## Example

Indexing the docs of my organization(> 100k pdf/doc/xls etc files) takes a couple of hours and creates an index of ~ 1 GB. 
Then depending on the query results are returned in 100 ms to 1 second.  


## Changes to fat.jar

This shouldn't be needed anymore.

- org.apache.lucene.codecs.lucene94.Lucene94Codec >> fat-jar/META-INF/services/org.apache.lucene.codecs.Codec
- org.apache.lucene.codecs.lucene90.Lucene90PostingsFormat >> fat-jar/META-INF/services/org.apache.lucene.codecs.PostingsFormat


## Changelog

- v1.2: Update dependencies
- v1.1: First public version
- 