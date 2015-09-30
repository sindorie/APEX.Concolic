package apex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.xml.sax.*;
import org.w3c.dom.*;

public class Configuration {
	public final static String TAG = "Configuration";
	
	private static Map<String,String> att;
	private static Element doc;
	private static String fileName = "configuraion.txt";

	private final static String rootTag = "cong";
	public final static String 
	attADB = "adb", attYics = "yices", attCVC4 = "cvc4";
	
	static{
		att = new HashMap<String,String>();
		File f = new File(fileName);
		if(!f.exists()){
			att.put(attADB, "adb");
			att.put(attCVC4, "libs/cvc4-1.4-x86_64-linux-opt --lang smt --strings-exp --incremental ");
			writeXML();
		}
		readXML();
	}
	
	public static String getValue(String key){
		String result = att.get(key);
		if(result == null){
			String val = getTextValue(null, doc, key);          
			if(val != null){
				att.put(key, val);
			}
			return val;
		}else{
			return result;
		}
	}
	
	private static boolean readXML(){
        Document dom;
        // Make an  instance of the DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use the factory to take an instance of the document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            // parse using the builder to get the DOM mapping of the    
            // XML file
            dom = db.parse(fileName);
            doc = dom.getDocumentElement();
            String adbVal = getTextValue(null, doc, attADB);            
            if(adbVal == null) return false;
            att.put(attADB, adbVal);
            
            String yicesVal = getTextValue(null, doc, attYics);    
            if(yicesVal == null) return false;
            att.put(attYics, yicesVal);
            
            return true;
        } catch (ParserConfigurationException pce) {
            System.out.println(pce.getMessage());
        } catch (SAXException se) {
            System.out.println(se.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        return false;
	}
	
	private static void writeXML(){
		Document dom;
	    Element e = null;

	    // instance of a DocumentBuilderFactory
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    try {
	        // use factory to get an instance of document builder
	        DocumentBuilder db = dbf.newDocumentBuilder();
	        // create instance of DOM
	        dom = db.newDocument();

	        // create the root element
	        Element rootEle = dom.createElement(rootTag);

	        // create data elements and place them under root
	        for( Entry<String, String> entry : att.entrySet()){
	        	e = dom.createElement(entry.getKey());
	        	e.appendChild(dom.createTextNode(entry.getValue()));
	        	rootEle.appendChild(e);
	        }
	        
	        dom.appendChild(rootEle);

	        try {
	            Transformer tr = TransformerFactory.newInstance().newTransformer();
	            tr.setOutputProperty(OutputKeys.INDENT, "yes");
	            tr.setOutputProperty(OutputKeys.METHOD, "xml");
	            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//	            tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "roles.dtd");
	            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

	            // send DOM to file
	            FileOutputStream out = new FileOutputStream(fileName);
	            tr.transform(new DOMSource(dom),  new StreamResult(new FileOutputStream(fileName)));
	            
	            out.close();
	        } catch (TransformerException te) {
	            System.out.println(te.getMessage());
	        } catch (IOException ioe) {
	            System.out.println(ioe.getMessage());
	        }
	    } catch (ParserConfigurationException pce) {
	        System.out.println("UsersXML: Error trying to instantiate DocumentBuilder " + pce);
	    }
	}
	
	private static String getTextValue(String def, Element doc, String tag) {
	    String value = def;
	    NodeList nl;
	    nl = doc.getElementsByTagName(tag);
	    if (nl.getLength() > 0 && nl.item(0).hasChildNodes()) {
	        value = nl.item(0).getFirstChild().getNodeValue();
	    }
	    return value;
	}
}
