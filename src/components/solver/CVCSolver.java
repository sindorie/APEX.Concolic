package components.solver;

import java.io.File; 
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import apex.Configuration;

public class CVCSolver {
	static String command = Configuration.getValue(Configuration.attCVC4);		
	static int index = 0;
	
//"/home/zhenxu/Tools/Solver/cvc4/cvc4-1.4-x86_64-linux-opt --lang smt --strings-exp --incremental ";

	/**
	 * 
	 * @param statements
	 * @return an array of string list. Such array is only of length 2. 
	 * 		   the first part is from stdout while the second is from stderr
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static List<String>[] solve(List<String> statements) throws IOException, InterruptedException{
		File tmpScript = File.createTempFile("Script#"+(index++), null);
		PrintWriter pw = new PrintWriter(tmpScript);
		for(String smt :statements){
			pw.println(smt);
		}
		pw.close();
		tmpScript.deleteOnExit();
		
		return solve(tmpScript);
	}
	
	@SuppressWarnings("unchecked")
	public static List<String>[] solve(File script) throws IOException, InterruptedException{
		Process solving = Runtime.getRuntime().exec(command+" "+script.getAbsolutePath());
		solving.waitFor();

		List<String> 
			stdoutLines = new ArrayList<String>(), 
			stderrLines = new ArrayList<String>();
		
		Scanner sc_stdin = new Scanner(solving.getInputStream());
		while(sc_stdin.hasNextLine()){stdoutLines.add(sc_stdin.nextLine());}
		sc_stdin.close();
		Scanner sc_err = new Scanner(solving.getErrorStream());
		while(sc_err.hasNextLine()){stderrLines.add(sc_err.nextLine());}
		sc_err.close();
		return new List[]{stdoutLines, stderrLines};
	}
}
