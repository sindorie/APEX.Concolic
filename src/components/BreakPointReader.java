package components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import apex.staticFamily.StaticApp;
import support.CommandLine;
import support.Utility;

public class BreakPointReader {
	private String serial, packName;
	private int localPort = 15975;
	private Process process; 
	private OutputStream out;
	private BufferedReader stdReader, errReader;
	private InputStream stdin, stderr;
	private List<String> lines;
	
	public BreakPointReader(String serial, String packName, List<String> lines){
		this.serial = serial; this.packName = packName;
		this.lines = new ArrayList<String>(lines);
	}

	
	public void setup(){
		
	}
	
	
}
