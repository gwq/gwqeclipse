import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * IndexFiles
 * @author wuqiang.gwq
 *
 */
public class IndexFiles {
	
	private IndexFiles() {}
	
	public static void main(String args[]){
		
		String usage = "java org.apache.lucene.demo.IndexFiles"
						+ " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
				        + "This indexes the documents in DOCS_PATH, creating a Lucene index"
				        + "in INDEX_PATH that can be searched with SearchFiles";
		
		String indexPath = "index";
		String docsPath = null;
		boolean create = true;
		for (int i = 0;i<args.length;i++){
			if ("-index".equals(args[i])){
				indexPath = args[i+1];
				i ++;
			}else if("-docs".equals(args[i])){
				docsPath = args[i+1];
				i ++;
			}else if("-update".equals(args[i])){
				create = false;
			}
		}
		if (docsPath == null){
			System.err.println("Usage: "+usage);
			System.exit(1);
		}
		
		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()){  //文件不存在或不可读
			System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}
		
		
		Date start = new Date();
		try{
			System.out.println("Indexing to directory '" + indexPath + "'...");
			
			Directory dir = FSDirectory.open(new File(indexPath));//将索引文件保存在硬盘上
			//Directory dir = new RAMDirectory()//将索引文件保存在内存中
			
			// :Post-Release-Update-Version.LUCENE_XY:
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_47, analyzer);
			
			if(create){
				//创建新的索引文件，会删除掉之前保存在文件中的所有索引项
				iwc.setOpenMode(OpenMode.CREATE);
			}else{
				//在现有的索引文件中添加索引项
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}
			
          // Optional: for better indexing performance, if you
          // are indexing many documents, increase the RAM
          // buffer.  But if you do this, increase the max heap
          // size to the JVM (eg add -Xmx512m or -Xmx1g):
          //
          // iwc.setRAMBufferSizeMB(256.0);
			
    
			IndexWriter writer = new IndexWriter(dir, iwc);
			indexDocs(writer, docDir);
			
			
			// NOTE: if you want to maximize search performance,
	        // you can optionally call forceMerge here.  This can be
	        // a terribly costly operation, so generally it's only
	        // worth it when your index is relatively static (ie
	        // you're done adding documents to it):
	        //
	        // writer.forceMerge(1);
			
		     writer.close();
		
			 Date end = new Date();
			 System.out.println(end.getTime() - start.getTime() + " total milliseconds");
		}catch (IOException e){
			System.out.println(" caught a " + e.getClass() +"\n with message: " + e.getMessage());
		}
	}

	static void indexDocs(IndexWriter writer, File file) throws IOException {
		// 忽略不可读的文件
		if (file.canRead()){
			if(file.isDirectory()){
				String[] files = file.list();
				//可能发生IO错误
				if (files != null){
					for (int i = 0;i <files.length;i++){
						indexDocs(writer, new File(file,files[i]));
					}
				}
			}
			else{
				FileInputStream fis;
				try{
					fis = new FileInputStream(file);
				}catch(FileNotFoundException fnfe){
					return;
				}
				
				try{
					Document doc = new Document();
					//建立path项，即要检索的文件路径，Field.Store.YES为将文件路径保存到index索引中
					//当要获取path信息时，可以直接从index索引中查找出来
					Field pathField = new StringField("path",file.getPath(),Field.Store.YES);
					doc.add(pathField);
					//建立modified项，即文件最后的修改时间，Field.Store.No不将文件最后修改时间保存到index索引中，
					//当要查找文件的最后修改时间时，会根据path找到文件，再从文件中读取该文件的最后修改时间
					doc.add(new LongField("modified", file.lastModified(), Field.Store.NO));
					// Add the contents of the file to a field named "contents".  Specify a Reader,
					// so that the text of the file is tokenized and indexed, but not stored.
					// Note that FileReader expects the file to be in UTF-8 encoding.
					// If that's not the case searching for special characters will fail.
					doc.add(new TextField("contents",new BufferedReader(new InputStreamReader(fis,"UTF-8"))));
					
					if (writer.getConfig().getOpenMode() == OpenMode.CREATE){
						// New index, so we just add the document (no old document can be there):
						System.out.println("adding "+file);
						writer.addDocument(doc);
					}
					else{
						// Existing index (an old copy of this document may have been indexed) so 
						// we use updateDocument instead to replace the old one matching the exact 
						// path, if present:
						System.out.println("updateing "+ file);
						writer.updateDocument(new Term("path",file.getPath()),doc );
					}
				}
				finally{
					fis.close();
				}
			}
		}
		
	}

}
