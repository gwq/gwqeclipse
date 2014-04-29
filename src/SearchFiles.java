import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


public class SearchFiles {

	private SearchFiles(){}
	
	public static void main(String args[]) throws Exception{
		String usage = "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] "
				+ "[-field f] [-repeat n] [-queries file] [-query string] [-raw] "
				+ "[-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
		if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))){
			System.out.println(usage);
			System.exit(0);
		}
		
		String index = "index";
		String field = "contents";
		String queries = null;
		int repeat = 0;
		boolean raw = false;
		String queryString = null;
		int hitsPerPage = 10;
		
		for (int i = 0;i < args.length;i ++){
			if("-index".equals(args[i])){
				index = args[i+1];
				i ++;
			}
			else if ("-field".equals(args[i])){
				field = args[i+1];
				i ++;
			}
			else if ("-queries".equals(args[i])){
				queries = args[i+1];
				i ++;
			}
			else if ("-query".equals(args[i])){
				queryString = args[i];
				i ++;
			}
			else if ("-repeat".equals(args[i])){
				repeat = Integer.valueOf(args[i+1]);
				i ++;
			}
			else if ("-raw".equals(args[i])){
				raw = true;
			}
			else if ("-paging".equals(args[i])){
				hitsPerPage = Integer.valueOf(args[i+1]);
				if (hitsPerPage <= 0){
					System.out.println("There must be at least 1 hit per page.");
					System.exit(1);
				}
				i ++;
			}
		}
		
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index)));
		IndexSearcher searcher = new IndexSearcher(reader);
		// :Post-Release-Update-Version.LUCENE_XY:
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
		
		BufferedReader in = null;
		if (queries != null){
			in = new BufferedReader(new InputStreamReader(new FileInputStream(queries),"UTF-8"));
		}
		else{
			in = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
		}
		 // :Post-Release-Update-Version.LUCENE_XY:
		QueryParser parser = new QueryParser(Version.LUCENE_47,field,analyzer);
		while (true){
			if (queries ==null && queryString == null){
				System.out.println("Enter query:");
			}
			String line = queryString != null ? queryString : in.readLine();
			
			if (line == null || line.length() == -1){
				break;
			}
			
			line = line.trim();
			if (line.length() == 0){
				break;
			}
			
			Query query = parser.parse(line);
			System.out.println("Searching for: "+query.toString(field));
			
			if (repeat > 0){              // repeat & time as benchmark
				Date start = new Date();
				for (int i = 0;i < repeat;i ++){
					searcher.search(query, null,100);
				}
				Date end = new Date();
				System.out.println("Time: "+(end.getTime() - start.getTime())+"ms");
			}
			doPagingSearch(in,searcher,query,hitsPerPage,raw,queries == null && queryString == null);
			if (queryString != null){
				break;
			}
		}
		reader.close();
	}

	/**
	      * This demonstrates a typical paging search scenario, where the search engine presents 
	      * pages of size n to the user. The user can then go to the next page if interested in
          * the next hits.
	      * 
	      * When the query is executed for the first time, then only enough results are collected
	      * to fill 5 result pages. If the user wants to page beyond this limit, then the query
	      * is executed another time and all hits are collected.
	 * @throws IOException 
	      * 
	      */
	private static void doPagingSearch(BufferedReader in,
			IndexSearcher searcher, Query query, int hitsPerPage, boolean raw,
			boolean interactive) throws IOException {
		
		// Collect enough docs to show 5 pages
		TopDocs results = searcher.search(query,5 * hitsPerPage);
		ScoreDoc[] hits = results.scoreDocs;
		
		int numTotalHits = results.totalHits;
		
	}
}
