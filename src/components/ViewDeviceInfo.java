package components;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

import javax.swing.tree.TreeNode;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import apex.Configuration;
import support.TreeUtility;
import support.TreeUtility.Searcher;
 

public class ViewDeviceInfo { 
	String serial;
	String adb;
	LayoutNode mRootNode;
	
	String xmlTargtLoc = "/sdcard/uidump";
//			"/data/uidump";
	
	private static String classBlackList = 
			"android.webkit.WebView"
			;
	
	
	public ViewDeviceInfo(String serial){
		this.serial = serial;
		adb = Configuration.getValue(Configuration.attADB);
	}
	
	public String getSerial(){
		return this.serial;
	}
	
	/**
	 * Use UIAutomator to retrieve information
	 * @return
	 */
	private int index = 0;
	public LayoutNode loadWindowData() throws Exception{
		mRootNode = null;
		ProcRunner procRunner;
		int retCode, waitTime = 10*1000;
		int iter = 0;
		Exception lastException = null;
		while(iter < 5){
			try {
				index += 1;
				File xmlDumpFile = File.createTempFile("dump"+index+"_"+serial, ".xml");
				
				procRunner = getAdbRunner(serial,"shell", "rm",xmlTargtLoc+"*");
				retCode = procRunner.run(waitTime);
				
				String remoteFileName = xmlTargtLoc+index+".xml";
		        procRunner = getAdbRunner(serial,
		                "shell", "/system/bin/uiautomator", "dump", remoteFileName);
		        retCode = procRunner.run(waitTime);
		        
		        procRunner = getAdbRunner(serial,
		                "pull", remoteFileName, xmlDumpFile.getAbsolutePath());
		        retCode = procRunner.run(waitTime);
//		        System.out.println(procRunner.mOutput);
		        
		        LayoutNode result =  buildTree(xmlDumpFile);
		        xmlDumpFile.delete();
		        
		        return result;
			} catch (IOException e) { }
			try { Thread.sleep(1000); } catch (InterruptedException e) { lastException = e;}
			iter += 1;
		}
		throw new AssertionError(lastException.getLocalizedMessage());
	}
	
	public LayoutNode buildTree(File xmlFile) throws SAXException, IOException{
 
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = null;
        try {
            parser = factory.newSAXParser();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        }
        // handler class for SAX parser to receiver standard parsing events:
        // e.g. on reading "<foo>", startElement is called, on reading "</foo>",
        // endElement is called
        DefaultHandler handler = new DefaultHandler(){
            LayoutNode mParentNode;
            LayoutNode mWorkingNode;
            @Override
            public void startElement(String uri, String localName, String qName,
                    Attributes attributes) throws SAXException {
                boolean nodeCreated = false;
                // starting an element implies that the element that has not yet been closed
                // will be the parent of the element that is being started here
                mParentNode = mWorkingNode;
                if ("hierarchy".equals(qName)) {
                	String windowName = attributes.getValue("windowName");
                	if(windowName == null) windowName = "focused";
                    mWorkingNode = new LayoutNode(windowName);
                    for (int i = 0; i < attributes.getLength(); i++) {
                    	mWorkingNode.addAtrribute(attributes.getQName(i), attributes.getValue(i));
//                    	System.out.println(attributes.getQName(i)+" - "+attributes.getValue(i));
                    }
                    nodeCreated = true;
                } else if ("node".equals(qName)) {
                	LayoutNode tmpNode = new LayoutNode();
                	tmpNode.setUserObject(attributes.getValue("", "class")+" "
                			+attributes.getValue("", "bounds")+" "
                			+attributes.getValue("", "text")
                			);
                	
                	
                    for (int i = 0; i < attributes.getLength(); i++) {
                        tmpNode.addAtrribute(attributes.getQName(i), attributes.getValue(i));
                    }
                    mWorkingNode = tmpNode;
                    nodeCreated = true;
                }
                // nodeCreated will be false if the element started is neither
                // "hierarchy" nor "node"
                if (nodeCreated) {
                    if (mRootNode == null) {
                        // this will only happen once
                        mRootNode = mWorkingNode;
                    }
                    if (mParentNode != null) {
                        mParentNode.addChild(mWorkingNode);
                    }
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                //mParentNode should never be null here in a well formed XML
                if (mParentNode != null) {
                    // closing an element implies that we are back to working on
                    // the parent node of the element just closed, i.e. continue to
                    // parse more child nodes
                    mWorkingNode = mParentNode;
                    mParentNode = mParentNode.getParent();
                }
            }
        };
        try{
            parser.parse(xmlFile, handler);
        }catch(Exception e){
        	new File("error").mkdirs();
        	Scanner sc = new Scanner(xmlFile);
        	PrintWriter writer = new PrintWriter("error/"+Calendar.getInstance().getTime()+".xml");
        	while(sc.hasNextLine()){
        		String line = sc.nextLine();
        		writer.println(line);
        	}
        	sc.close();
        	writer.close();
        	throw e;
        }
        //filter unnecessary Node
        
        TreeUtility.breathFristSearch(this.mRootNode, new Searcher(){
			@Override
			public int check(TreeNode node) {
				if(node != null){
					LayoutNode lay = (LayoutNode)node;
					if(lay.className != null){
						if(classBlackList.contains(lay.className)){
							lay.removeAllChildren();
							return Searcher.SKIP;
						}
					}
				}
				return Searcher.NORMAL;
			}
        });
        
        
        return mRootNode;
	}
	
    private ProcRunner getAdbRunner(String serial, String... command) {
        List<String> cmd = new ArrayList<String>();
        cmd.add(adb);
        if (serial != null) {
            cmd.add("-s");
            cmd.add(serial);
        }
        for (String s : command) {
            cmd.add(s);
        }
        return new ProcRunner(cmd);
    }
	
    private static class ProcRunner {

        ProcessBuilder mProcessBuilder;
        List<String> mOutput = new ArrayList<String>();
        public ProcRunner(List<String> command) {
            mProcessBuilder = new ProcessBuilder(command).redirectErrorStream(true);
        }
        public int run(long timeout) throws IOException {
            final Process p = mProcessBuilder.start();
            Thread t = new Thread() {
                @Override
                public void run() {
                    String line;
                    mOutput.clear();
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(
                                p.getInputStream()));
                        while ((line = br.readLine()) != null) {
                            mOutput.add(line);
                        }
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                };
            };
            t.start();
            try {
                t.join(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (t.isAlive()) {
                throw new IOException("external process not terminating.");
            }
            try {
                return p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new IOException(e);
            }
        }

        public String getOutputBlob() {
            StringBuilder sb = new StringBuilder();
            for (String line : mOutput) {
                sb.append(line);
                sb.append(System.getProperty("line.separator"));
            }
            return sb.toString();
        }
    }
}
