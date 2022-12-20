package gr.serafeim

import gr.serafeim.web.SearchParams
import gr.serafeim.web.dateToMillis
import gr.serafeim.web.fromDateString
import org.apache.commons.logging.Log
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.el.GreekAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.LongPoint
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.*
import org.apache.lucene.search.highlight.Highlighter
import org.apache.lucene.search.highlight.QueryScorer
import org.apache.lucene.search.highlight.SimpleHTMLFormatter
import org.apache.lucene.search.highlight.SimpleSpanFragmenter
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.util.*


data class Results(val results: List<Result>, val total: Int)

data class Result(
    val id: String,
    val text: String,
    val hfragments: List<String>,
    val name: String,
    val path: List<String>,
    val created: Date,
    val modified: Date,
    val accessed: Date
)

fun addDateQuery(bqb: BooleanQuery.Builder, dateFrom: Date?, dateTo: Date?, what: String) {
    if (dateFrom != null || dateTo != null) {
        val fromMillis = dateToMillis(dateFrom, 0L)
        val toMillis = dateToMillis(dateTo, Long.MAX_VALUE)
        val query3: Query = LongPoint.newRangeQuery(what, fromMillis, toMillis)
        bqb.add(query3, BooleanClause.Occur.FILTER)
    }

}

object SearchHolder {
    private val logger: Logger = LoggerFactory.getLogger("Search")
    private val directory: Directory = FSDirectory.open(Paths.get("lucene_index"))
    private val reader: DirectoryReader = DirectoryReader.open(directory)
    private val indexSearcher = IndexSearcher(reader)
    private val analyzer: Analyzer = GlobalsHolder.getAnalyzerInstance()

    init {
        logger.info("Search Singleton class invoked.")
    }
    fun search(sp: SearchParams): Results {

        // https://stackoverflow.com/questions/2005084/how-to-specify-two-fields-in-lucene-queryparser
        val query1 = QueryParser("text", analyzer).parse(sp.q)
        val bqb = BooleanQuery.Builder()
        bqb.add(query1, BooleanClause.Occur.SHOULD)
        val query2: Query = WildcardQuery(Term("name", sp.q))
        bqb.add(query2, BooleanClause.Occur.SHOULD)
        bqb.setMinimumNumberShouldMatch(1)

        val query2a = QueryParser("name_t", analyzer).parse(sp.q)
        bqb.add(query2a, BooleanClause.Occur.SHOULD)
        bqb.setMinimumNumberShouldMatch(1)

        addDateQuery(bqb, sp.createdFrom, sp.createdTo, "created_point")
        addDateQuery(bqb, sp.modifiedFrom, sp.modifiedTo, "modified_point")
        addDateQuery(bqb, sp.accessedFrom, sp.accessedTo, "accessed_point")

        if (sp.path != null && sp.path != "") {
            val query4: Query = WildcardQuery(Term("path", sp.path))
            bqb.add(query4, BooleanClause.Occur.FILTER)
        }

        val booleanQuery = bqb.build()
        val collector = TopScoreDocCollector.create(99999, 100)

        indexSearcher.search(booleanQuery, collector)

        val start = (sp.p - 1) * sp.n
        val howmany = sp.n

        // Highlight
        val formatter = SimpleHTMLFormatter("<span class='highlight'>", "</span>");
        val queryScorer = QueryScorer(booleanQuery);
        val highlighter = Highlighter(formatter, queryScorer);
        highlighter.textFragmenter = SimpleSpanFragmenter(queryScorer, Int.MAX_VALUE)
        highlighter.maxDocCharsToAnalyze = Int.MAX_VALUE

        val fragmentHighlighter = Highlighter(formatter, queryScorer);
        fragmentHighlighter.textFragmenter = SimpleSpanFragmenter(queryScorer, 30)
        fragmentHighlighter.maxDocCharsToAnalyze = Int.MAX_VALUE

        val results = collector.topDocs(start, howmany).scoreDocs.map {

            val doc: Document = indexSearcher.doc(it.doc)
            val id = doc.get("id")
            val path = doc.getValues("path").asList()
            val name = doc.get("name")
            val text = doc.get("text")

            val created = fromDateString(doc.get("created"))
            val accessed = fromDateString(doc.get("accessed"))
            val modified = fromDateString(doc.get("modified"))

            val fragments = fragmentHighlighter.getBestFragments(analyzer.tokenStream("text", text), text, 10)

            val htext = highlighter.getBestFragment(analyzer, "text", text)?:text;
            Result(
                id = id,
                text = htext,
                hfragments = fragments.toList(),
                name = name,
                path = path,
                accessed = accessed,
                modified = modified,
                created = created
            )
        }

        return Results(results = results, total = collector.totalHits)
    }
}


