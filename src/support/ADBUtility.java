package support;


public class ADBUtility {
	public static String getPID(String packageName, String serial) {
		CommandLine.executeADBCommand(" shell ps | grep " + packageName, serial);
		String message = CommandLine.getLatestStdoutMessage();
		if(message!= null){
			String[] lines = message.split("\n");
			for(String line : lines){
				line = line.trim();
				if (!line.endsWith(packageName)){continue;}
				String[] parts = line.split(" ");
				for (int i = 1; i < parts.length; i++) {
					if (parts[i].equals("")){continue;}
					return parts[i].trim();
				}
			}
		}
		return null;
	}
}
