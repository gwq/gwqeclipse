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
 * 
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
		if (!docDir.exists() || !docDir.canRead()){  //�ļ������ڻ򲻿ɶ�
			System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}
		
		
		Date start = new Date();
		try{
			System.out.println("Indexing to directory '" + indexPath + "'...");
			
			Directory dir = FSDirectory.open(new File(indexPath));//�������ļ�������Ӳ����
			//Directory dir = new RAMDirectory()//�������ļ��������ڴ���
			
			// :Post-Release-Update-Version.LUCENE_XY:
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_47, analyzer);
			
			if(create){
				//�����µ������ļ�����ɾ����֮ǰ�������ļ��е�����������
				iwc.setOpenMode(OpenMode.CREATE);
			}else{
				//�����е������ļ�������������
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
		// ���Բ��ɶ����ļ�
		if (file.canRead()){
			if(file.isDirectory()){
				String[] files = file.list();
				//���ܷ���IO����
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
					//����path���Ҫ�������ļ�·����Field.Store.YESΪ���ļ�·�����浽index������
					//��Ҫ��ȡpath��Ϣʱ������ֱ�Ӵ�index�����в��ҳ���
					Field pathField = new StringField("path",file.getPath(),Field.Store.YES);
					doc.add(pathField);
					//����modified����ļ������޸�ʱ�䣬Field.Store.No�����ļ�����޸�ʱ�䱣�浽index�����У�
					//��Ҫ�����ļ�������޸�ʱ��ʱ�������path�ҵ��ļ����ٴ��ļ��ж�ȡ���ļ�������޸�ʱ��
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