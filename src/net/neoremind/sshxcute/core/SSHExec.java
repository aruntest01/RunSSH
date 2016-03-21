package net.neoremind.sshxcute.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import net.neoremind.sshxcute.exception.TaskExecFailException;
import net.neoremind.sshxcute.exception.UploadFileNotSuccessException;
import net.neoremind.sshxcute.task.CustomTask;

import com.arun.test.Constants;
import com.arun.test.MainClass;
import com.arun.test.RunSSHUtil;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class SSHExec {
	static Logger logger = Logger.getLogger();
	private Session session;
	private Channel channel;
	private ConnBean conn;
	private static SSHExec ssh;
	private JSch jsch;

	private SSHExec(ConnBean conn) {
		try {
			logger.putMsg(Logger.INFO, "SSHExec initializing ...");
			this.conn = conn;
			jsch = new JSch();
		} catch (Exception e) {
			logger.putMsg(Logger.ERROR,
					"Init SSHExec fails with the following exception: " + e);
		}
	}

	public static SSHExec getInstance(ConnBean conn) {
		if (ssh == null) {
			ssh = new SSHExec(conn);
		}
		return ssh;
	}

	public Boolean connect() {
		try {
			session = jsch.getSession(conn.getUser(), conn.getHost(),
					SysConfigOption.SSH_PORT_NUMBER);
			UserInfo ui = new ConnCredential(conn.getPassword());
			logger.putMsg(Logger.INFO,
					"Session initialized and associated with user credential "
							+ conn.getPassword());
			session.setUserInfo(ui);
			logger.putMsg(Logger.INFO, "SSHExec initialized successfully");
			logger.putMsg(
					Logger.INFO,
					"SSHExec trying to connect " + conn.getUser() + "@"
							+ conn.getHost());
			session.connect(3600000);
			logger.putMsg(Logger.INFO, "SSH connection established");
		} catch (Exception e) {
			logger.putMsg(Logger.ERROR,
					"Connect fails with the following exception: " + e);
			return false;
		}
		return true;
	}

	public Boolean disconnect() {
		try {
			session.disconnect();
			session = null;
			logger.putMsg(Logger.INFO, "SSH connection shutdown");
			System.out.println("SSH connection shutdown");
		} catch (Exception e) {
			logger.putMsg(Logger.ERROR,
					"Disconnect fails with the following exception: " + e);
			return false;
		}
		return true;
	}

	public synchronized Result exec(CustomTask task)
			throws TaskExecFailException {
		Result r = new Result();
		try {
			channel = session.openChannel("exec");
			String command = task.getCommand();
			logger.putMsg(Logger.INFO, "Command is " + command);
			((ChannelExec) channel).setCommand(command);
			channel.setInputStream(null);
			channel.setOutputStream(System.out);
			FileOutputStream fos = new FileOutputStream(
					SysConfigOption.ERROR_MSG_BUFFER_TEMP_FILE_PATH);
			((ChannelExec) channel).setErrStream(fos);
			InputStream in = channel.getInputStream();
			channel.connect();
			logger.putMsg(Logger.INFO,
					"Connection channel established succesfully");
			logger.putMsg(Logger.INFO, "Start to run command");
			
			StringBuilder sb = new StringBuilder();
			byte[] tmp = new byte[1024];
			while (true) {
				boolean success= false;
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					String str = new String(tmp, 0, i);
					sb.append(str);
					logger.putMsg(Logger.INFO, str);
					System.out.println(str);
					success=true;
				}
				String currCommand = command.replace(command.split(";")[0]+";","");
				if(!success){
					String s =SSHExecUtil.getErrorMsg(SysConfigOption.ERROR_MSG_BUFFER_TEMP_FILE_PATH);
					if(!s.equals("") && !"help".equalsIgnoreCase(currCommand)){
						System.out.println("***ERROR***");
						System.out.println(s);
					}
					RunSSHUtil.removeCommand(currCommand);
					if("help".equalsIgnoreCase(currCommand)){
						RunSSHUtil.printKnownCommands();
					}
				}else{
					RunSSHUtil.addCommand(currCommand);
				}
				
				if (channel.isClosed()) {
					logger.putMsg(Logger.INFO, "Connection channel closed");
					logger.putMsg(Logger.INFO,
							"Check if exec success or not ... ");
					r.rc = channel.getExitStatus();
					r.sysout = sb.toString();
					if (task.isSuccess(sb.toString(), channel.getExitStatus())) {
						logger.putMsg(
								Logger.INFO,
								"Execute successfully for command: "
										+ task.getCommand());
						r.error_msg = "";
						r.isSuccess = true;
					} else {
						r.error_msg = SSHExecUtil
								.getErrorMsg(SysConfigOption.ERROR_MSG_BUFFER_TEMP_FILE_PATH);
						r.isSuccess = false;
						logger.putMsg(Logger.INFO,
								"Execution failed while executing command: "
										+ task.getCommand());
						logger.putMsg(Logger.INFO, "Error message: "
								+ r.error_msg);
						if (SysConfigOption.HALT_ON_FAILURE) {
							logger.putMsg(
									Logger.ERROR,
									"The task has failed to execute :"
											+ task.getInfo()
											+ ". So program exit.");
							throw new TaskExecFailException(task.getInfo());
						}
					}
					break;
				}
			}
			try {
				/*logger.putMsg(Logger.INFO, "Now wait "
						+ SysConfigOption.INTEVAL_TIME_BETWEEN_TASKS / 1000
						+ " seconds to begin next task ...");
				Thread.sleep(SysConfigOption.INTEVAL_TIME_BETWEEN_TASKS);*/
				System.out.println("<<Enter next Command or enter exit / check prev commands(help)>>");
				Scanner newScan = new Scanner(System.in);
				String newcommand = newScan.nextLine();
				if(MainClass.NO_OF_COMMANDS>Integer.parseInt(RunSSHUtil.getProp(Constants.MAX_NO_ON_COMMANDS))
						|| newcommand.equalsIgnoreCase("exit")){
					channel.disconnect();
					logger.putMsg(Logger.INFO, "Connection channel disconnect");
					System.out.println("Connection channel disconnect");
					disconnect();
					if(MainClass.NO_OF_COMMANDS>Integer.parseInt(RunSSHUtil.getProp(Constants.MAX_NO_ON_COMMANDS))){
						System.out.println("Maximum commands for a session is --> "+RunSSHUtil.getProp(Constants.MAX_NO_ON_COMMANDS));
						System.out.println("Please refer Host.properties to change, if needed ");
					}
					return r;
				}
				else{
			    	MainClass.runCommand(newcommand);
			    }
			} catch (Exception ee) {
			}
			try{channel.disconnect();}catch(Exception e){}
			logger.putMsg(Logger.INFO, "Connection channel disconnect");
		} catch (JSchException e) {
			logger.putMsg(Logger.ERROR, e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			logger.putMsg(Logger.ERROR, e.getMessage());
			e.printStackTrace();
		}
		return r;
	}

	public void uploadAllDataToServer(String fromLocalDir, String toServerDir)
			throws Exception {
		if (!new File(fromLocalDir).isDirectory()) {
			throw new UploadFileNotSuccessException(fromLocalDir);
		} else {
			dataList = new LinkedHashMap<String, String>();
			String staticRootDir = "";
			if (fromLocalDir.lastIndexOf('/') > 0) {
				staticRootDir = fromLocalDir.substring(0,
						fromLocalDir.lastIndexOf('/'));
			} else if (fromLocalDir.lastIndexOf('\\') > 0) {
				staticRootDir = fromLocalDir.substring(0,
						fromLocalDir.lastIndexOf('\\'));
			} else {
				staticRootDir = fromLocalDir;
			}
			staticRootDir = staticRootDir.replace('\\', '/');
			traverseDataDir(new File(fromLocalDir), staticRootDir);
			Iterator<Entry<String,String>> it = dataList.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String,String> entry = it.next();
				uploadSingleDataUnderDirToServer(entry.getKey(),
						toServerDir + "/" + entry.getValue());
			}
		}
	}

	private void uploadSingleDataUnderDirToServer(String fromLocalFile,
			String toServerFile) throws Exception {
		FileInputStream fis = null;
		logger.putMsg(Logger.INFO, "Ready to transfer local file '"
				+ fromLocalFile + "' to server directory '" + toServerFile
				+ "'");
		String command = "mkdir -p " + toServerFile + "; scp -p -t "
				+ toServerFile;
		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);
		OutputStream out = channel.getOutputStream();
		InputStream in = channel.getInputStream();
		channel.connect();
		logger.putMsg(Logger.INFO, "Connection channel established succesfully");
		logger.putMsg(Logger.INFO, "Start to upload");
		if (SSHExecUtil.checkAck(in) != 0) {
			System.exit(0);
		}
		long filesize = (new File(fromLocalFile)).length();
		command = "C0644 " + filesize + " ";
		if (fromLocalFile.lastIndexOf('/') > 0) {
			command += fromLocalFile
					.substring(fromLocalFile.lastIndexOf('/') + 1);
		} else {
			command += fromLocalFile;
		}
		command += "\n";
		out.write(command.getBytes());
		out.flush();
		if (SSHExecUtil.checkAck(in) != 0) {
			logger.putMsg(Logger.ERROR, fromLocalFile + "check fails");
			return;
		}
		fis = new FileInputStream(fromLocalFile);
		byte[] buf = new byte[1024];
		while (true) {
			int len = fis.read(buf, 0, buf.length);
			if (len <= 0)
				break;
			out.write(buf, 0, len);
		}
		fis.close();
		fis = null;
		buf[0] = 0;
		out.write(buf, 0, 1);
		out.flush();
		if (SSHExecUtil.checkAck(in) != 0) {
			logger.putMsg(Logger.ERROR, toServerFile + "check fails");
			return;
		}
		out.close();
		logger.putMsg(Logger.INFO, "Upload success");
		channel.disconnect();
		logger.putMsg(Logger.INFO, "channel disconnect");
	}

	public void uploadSingleDataToServer(String fromLocalFile,
			String toServerFile) throws Exception {
		if (new File(fromLocalFile).isDirectory()) {
			throw new UploadFileNotSuccessException(fromLocalFile);
		}
		FileInputStream fis = null;
		logger.putMsg(Logger.INFO, "Ready to transfer local file '"
				+ fromLocalFile + "' to server directory '" + toServerFile
				+ "'");
		String command = "scp -p -t " + toServerFile;
		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);
		OutputStream out = channel.getOutputStream();
		InputStream in = channel.getInputStream();
		channel.connect();
		logger.putMsg(Logger.INFO, "Connection channel established succesfully");
		logger.putMsg(Logger.INFO, "Start to upload");
		if (SSHExecUtil.checkAck(in) != 0) {
			System.exit(0);
		}
		long filesize = (new File(fromLocalFile)).length();
		command = "C0644 " + filesize + " ";
		if (fromLocalFile.lastIndexOf('/') > 0) {
			command += fromLocalFile
					.substring(fromLocalFile.lastIndexOf('/') + 1);
		} else if (fromLocalFile.lastIndexOf('\\') > 0) {
			command += fromLocalFile
					.substring(fromLocalFile.lastIndexOf('\\') + 1);
		} else {
			command += fromLocalFile;
		}
		command += "\n";
		out.write(command.getBytes());
		out.flush();
		if (SSHExecUtil.checkAck(in) != 0) {
			logger.putMsg(Logger.INFO, fromLocalFile + "check fails");
			return;
		}
		fis = new FileInputStream(fromLocalFile);
		byte[] buf = new byte[1024];
		while (true) {
			int len = fis.read(buf, 0, buf.length);
			if (len <= 0)
				break;
			out.write(buf, 0, len);
		}
		fis.close();
		fis = null;
		buf[0] = 0;
		out.write(buf, 0, 1);
		out.flush();
		if (SSHExecUtil.checkAck(in) != 0) {
			logger.putMsg(Logger.ERROR, toServerFile + "check fails");
			return;
		}
		out.close();
		logger.putMsg(Logger.INFO, "Upload success");
		channel.disconnect();
		logger.putMsg(Logger.INFO, "channel disconnect");
	}

	public static void setOption(String option, String value) {
		Class<SysConfigOption> optionClass = SysConfigOption.class;
		Field[] field = optionClass.getDeclaredFields();
		for (int i = 0; i < field.length; i++) {
			if (field[i].getName().equals(option)
					&& field[i].getType().getName().equals("java.lang.String")) {
				try {
					logger.putMsg(Logger.INFO,
							"Set system configuration parameter '" + option
									+ "' to new value '" + value + "'");
					field[i].set(option, value);
					break;
				} catch (IllegalAccessException e) {
					logger.putMsg(Logger.ERROR,
							"Unable to set global configuration param "
									+ option + " to value " + value);
				}
			}
		}
	}

	public static void setOption(String option, int value) {
		Class<SysConfigOption> optionClass = SysConfigOption.class;
		Field[] field = optionClass.getDeclaredFields();
		for (int i = 0; i < field.length; i++) {
			if (field[i].getName().equals(option)
					&& field[i].getType().getName().equals("int")) {
				try {
					logger.putMsg(Logger.INFO,
							"Set system configuration parameter '" + option
									+ "' to new value '" + value + "'");
					field[i].set(option, value);
					break;
				} catch (IllegalAccessException e) {
					logger.putMsg(Logger.ERROR,
							"Unable to set global configuration param "
									+ option + " to value " + value);
				}
			}
		}
	}

	public static void setOption(String option, long value) {
		Class<SysConfigOption> optionClass = SysConfigOption.class;
		Field[] field = optionClass.getDeclaredFields();
		for (int i = 0; i < field.length; i++) {
			if (field[i].getName().equals(option)
					&& field[i].getType().getName().equals("long")) {
				try {
					logger.putMsg(Logger.INFO,
							"Set system configuration parameter '" + option
									+ "' to new value '" + value + "'");
					field[i].set(option, value);
					break;
				} catch (IllegalAccessException e) {
					logger.putMsg(Logger.ERROR,
							"Unable to set global configuration param "
									+ option + " to value " + value);
				}
			}
		}
	}

	public static void setOption(String option, boolean value) {
		Class<SysConfigOption> optionClass = SysConfigOption.class;
		Field[] field = optionClass.getDeclaredFields();
		for (int i = 0; i < field.length; i++) {
			if (field[i].getName().equals(option)
					&& field[i].getType().getName().equals("boolean")) {
				try {
					logger.putMsg(Logger.INFO,
							"Set system configuration parameter '" + option
									+ "' to new value '" + value + "'");
					field[i].set(option, value);
					break;
				} catch (IllegalAccessException e) {
					logger.putMsg(Logger.ERROR,
							"Unable to set global configuration param "
									+ option + " to value " + value);
				}
			}
		}
	}

	public static void showEnvConfig() throws Exception {
		Class<SysConfigOption> optionClass = SysConfigOption.class;
		Field[] field = optionClass.getDeclaredFields();
		logger.putMsg(Logger.INFO,
				"******************************************************");
		logger.putMsg(Logger.INFO,
				"The list below shows sshxcute configuration parameter");
		logger.putMsg(Logger.INFO,
				"******************************************************");
		for (int i = 0; i < field.length; i++) {
			logger.putMsg(Logger.INFO,
					field[i].getName() + " => " + field[i].get(optionClass));
		}
	}

	protected Map<String, String> dataList = null;
	protected static String PARENT_DIR = "";

	protected Map<String, String> traverseDataDir(File parentDir,
			String parentRootPath) throws Exception {
		if (parentDir.isDirectory()) {
			String[] subComponents = SSHExecUtil.getFiles(parentDir);
			for (int j = 0; j < subComponents.length; j++) {
				PARENT_DIR = PARENT_DIR + File.separator + parentDir.getName();
				traverseDataDir(new File(parentDir + "/" + subComponents[j]),
						parentRootPath);
			}
		} else if (parentDir.isFile()) {
			logger.putMsg(Logger.INFO, "Find " + parentDir.getPath());
			dataList.put(
					parentDir.getPath().toString().replace('\\', '/'),
					parentDir.getParent().toString().replace('\\', '/')
							.split(parentRootPath)[1]);
		}
		return dataList;
	}

}