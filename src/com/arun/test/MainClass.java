/**
 * 
 */
package com.arun.test;

import java.util.Scanner;

/**
 * @author c38847
 *
 */
public class MainClass {

	static RunSSH ssh;
	public static int NO_OF_COMMANDS=0;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		getEnvironment((args!=null && args.length>0)?args[0]:"");
	}
	
	private static void getEnvironment(String env){
		if(!RunSSHUtil.setEnv(env)){
			System.out.print("<<Enter a valid Environment.>> ");
			System.out.println(RunSSHUtil.getEnvironments());
			Scanner in = new Scanner(System.in);
		    getEnvironment(in.nextLine());
		}else{
			receiveCommand();
		}
	}
	
	
	public static void receiveCommand(){
		Scanner in = new Scanner(System.in);
		System.out.println("<<Enter a Command to run/ check prev commands(help)>>");
	    String command = in.nextLine();
	    if(!command.equalsIgnoreCase("exit")){
	    	runCommand(command);
	    }
	}
	
	public static void runCommand(String command){
		NO_OF_COMMANDS++;
		if(null==ssh)
			ssh =new RunSSH();
		else if("help".equalsIgnoreCase(command)){
			RunSSHUtil.printKnownCommands();
		}
		RunSSH.execute(command);
			
		
	}
	
	

}
