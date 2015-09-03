package ocaml.exec;

import java.io.File;
import java.io.InputStream;

import ocaml.OcamlPlugin;

import org.eclipse.debug.core.DebugPlugin;

/**
 * Execute a system process, and allows to get its standard and error outputs, but doesn't allow interactive
 * communication with the process.
 */
public class CommandRunner {
	private StreamGobbler errorGobbler = null;

	private StreamGobbler outputGobbler = null;

	private String StartError = null;

	private Process process = null;

	private int exitValue;

	/**
	 * Start a process
	 * 
	 * @param command
	 *            the command to execute, without parameters
	 * @param fileFolderPath
	 *            the path in which the process will start
	 */
	@Deprecated
	public CommandRunner(String command, String fileFolderPath) {
		this(DebugPlugin.parseArguments(command), fileFolderPath);
	}

	/**
	 * Start a process
	 * 
	 * @param command
	 *            the command to execute, with its parameters
	 * @param folderPath
	 *            the working directory of the subprocess, or <code>null</code>
	 *            if the subprocess should inherit the working directory of the
	 *            current process.
	 */
	public CommandRunner(String[] command, String folderPath) {
		if (command.length == 0) {
			return;
		}

		Runtime runtime = Runtime.getRuntime();

//		System.err.print("command runner: ");
//		for (String word : command)
//			System.err.print("'" + word + "' ");
//		System.err.print("\n");

		try {
			// execute the command
			this.process = runtime.exec(command, null, folderPath == null ? null : new File(folderPath));

			// get the standard and error outputs
			InputStream stderr = this.process.getErrorStream();
			InputStream stdout = this.process.getInputStream();

			// start the two threads that will get the outputs from stderr and stdout
			this.errorGobbler = new StreamGobbler(stderr);
			this.outputGobbler = new StreamGobbler(stdout);
			this.errorGobbler.start();
			this.outputGobbler.start();

			// wait for the process to end
			this.exitValue = this.process.waitFor();
		} catch (Exception e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			this.StartError = e.toString();
		}
	}

	/** Get the exit value of the command (returned by exit()) */
	public synchronized int getExitValue() {
		if(this.process == null) {
			return -1;
		}
		try {
			this.process.waitFor();
		} catch (InterruptedException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
		return this.exitValue;
	}

	/** Get the error output */
	public synchronized String getStderr() {
		if(this.StartError != null) {
			return this.StartError;
		}
		if(this.errorGobbler == null) {
			return null;
		}
		return this.errorGobbler.waitAndGetResult();
	}

	/** Get the standard output */
	public synchronized String getStdout() {
		if(this.outputGobbler == null) {
			return null;
		}
		return this.outputGobbler.waitAndGetResult();
	}

	/** Kill the process */
	public synchronized void kill() {
		if (process != null) {
			process.destroy();
			process = null;
		}
	}
}