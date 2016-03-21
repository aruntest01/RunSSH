/**
 * 
 */
package com.arun.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import net.neoremind.sshxcute.core.Logger;

/**
 * @author c38847
 * 
 */
public class RunSSHUtil {

	private static final String fileName = "Host.properties";
	private static String environment;
	private static Map<String, HashMap<String, String>> propMap;
	
	public static boolean setEnv(String env) {
		// TODO Auto-generated constructor stub
		try {
			loadProps();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(propMap.containsKey(env))
			environment=env;
		else
			return false;
		return true;
	}
	
	public static Set<String> getEnvironments(){
		return propMap.keySet();
	}

	public static String getProp(String key) {
		if (propMap != null && propMap.containsKey(environment))
			return (String) propMap.get(environment).get(key);
		else
			return null;
	}

	private static void loadProps()
			throws Exception {
		Logger.getLogger().clearLogger();
		BufferedReader fs = null;
		propMap = new HashMap<String, HashMap<String, String>>();
		try {
			String section = null;
			HashMap<String, String> map = null;

			InputStream input = new FileInputStream(
					new File("").getAbsolutePath() + "/" + fileName);
			if (input != null) {
				fs = new BufferedReader(new InputStreamReader(input));
				String line;
				while ((line = fs.readLine()) != null) {
					if (line.indexOf("[") == -1) {
						if (line.trim().length() > 0) {
							line = line.trim();
							String value;
							String name;
							if (line.indexOf("=") > -1) {
								name = line.substring(0, line.indexOf("="))
										.trim();
								value = line.substring(line.indexOf("=") + 1,
										line.length()).trim();
							} else {
								name = line.trim();
								value = "";
							}
							map.put(name, value);
						}
					} else {
						if (map != null && map.size() > 0) {
							propMap.put(section, map);
						}
						line = line.trim();
						section = line.substring(1, line.length() - 1);
						map = new HashMap<String, String>();
					}
				}
				if (map.size() != 0) {
					propMap.put(section, map);
				}
			}

		} catch (FileNotFoundException f) {
			throw new Exception("Property File Not Found.", f);
		} catch (IOException i) {
			throw new Exception("Error Reading property file.", i);
		} catch (NullPointerException e) {
			throw new Exception("Section(s) was not found in property file.", e);
		} finally {
			try {
				if (fs != null) {
					fs.close();
				}
			} catch (Exception e) {
				throw new Exception("Error closing property file. ", e);
			}
		}
	}
	
	public static void addCommand(String command){
		try {
			File commandsFile = new File(getProp("COMMANDS_REF"));
			List<String> commands = FileUtils.readLines(commandsFile);
			if(!commands.contains(command)){
				commands.add(command);
				FileUtils.writeLines(commandsFile, commands);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void removeCommand(String command){
		try {
			File commandsFile = new File(getProp("COMMANDS_REF"));
			List<String> commands = FileUtils.readLines(commandsFile);
			if(commands.contains(command)){
				commands.remove(command);
				FileUtils.writeLines(commandsFile, commands);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void printKnownCommands(){
		try {
			File commandsFile = new File(getProp("COMMANDS_REF"));
			List<String> commands = FileUtils.readLines(commandsFile);
			for(String s:commands){
				System.out.println(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
