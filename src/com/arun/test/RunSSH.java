/**
 * 
 */
package com.arun.test;

import net.neoremind.sshxcute.core.ConnBean;
import net.neoremind.sshxcute.core.IOptionName;
import net.neoremind.sshxcute.core.Result;
import net.neoremind.sshxcute.core.SSHExec;
import net.neoremind.sshxcute.exception.TaskExecFailException;
import net.neoremind.sshxcute.task.CustomTask;
import net.neoremind.sshxcute.task.impl.ExecCommand;

/**
 * @author c38847
 *
 */
public class RunSSH {

	static SSHExec instancessh;
	
	{
		connect();
	}
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		
		try{instancessh.disconnect();}catch(Exception e){}
		super.finalize();
	}

	private static void connect() {
		// Initialize a ConnBean object, parameter list is ip, username,
		// password
		/*System.out.println(""+RunSSHUtil.getProp(Constants.HOST)+ RunSSHUtil.getProp(Constants.USERNAME)
							+ RunSSHUtil.getProp(Constants.PASSWORD));*/
		ConnBean cb = new ConnBean(RunSSHUtil.getProp(Constants.HOST),RunSSHUtil.getProp(Constants.USERNAME),
							RunSSHUtil.getProp(Constants.PASSWORD));

		// Put the ConnBean instance as parameter for SSHExec static method
		// getInstance(ConnBean) to retrieve a singleton
		instancessh = SSHExec.getInstance(cb);
		SSHExec.setOption(IOptionName.INTEVAL_TIME_BETWEEN_TASKS, 25);
		// Connect to server
		instancessh.connect();
	}
	
	static boolean execute(String command){
		command = RunSSHUtil.getProp(Constants.LOGPATH) +","+command;
		CustomTask sampleTask = new ExecCommand(command.split(","));
		try {
			Result res = instancessh.exec(sampleTask);
			if (res.isSuccess){ 
				/*System.out.println("========================");
				System.out.println(res.sysout);
				System.out.println("========================");*/
				return true;
			}else{    
				System.out.println("Return code: " + res.rc);    
				System.out.println("error message: " + res.error_msg);
				return false;
			}
		} catch (TaskExecFailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public static void disConnect(){
		instancessh.disconnect();
	}
	
}
