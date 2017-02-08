package org.cryptomator.frontend.webdav.mount;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.cryptomator.frontend.webdav.mount.Mounter.CommandFailedException;

import com.google.common.io.CharStreams;

class ProcessUtil {

	/**
	 * Fails with a CommandFailedException, if the process did not finish with the expected exit code.
	 * 
	 * @param proc A finished process
	 * @param expectedExitValue Exit code returned by the process
	 * @throws CommandFailedException Thrown in case of unexpected exit values
	 */
	public static void assertExitValue(Process proc, int expectedExitValue) throws CommandFailedException {
		int actualExitValue = proc.exitValue();
		if (actualExitValue != expectedExitValue) {
			try {
				String error = toString(proc.getErrorStream(), StandardCharsets.UTF_8);
				throw new CommandFailedException("Command failed with exit code " + actualExitValue + ". Expected " + expectedExitValue + ". Stderr: " + error);
			} catch (IOException e) {
				throw new CommandFailedException("Command failed with exit code " + actualExitValue + ". Expected " + expectedExitValue + ".");
			}
		}
	}

	/**
	 * Waits for the process to terminate or throws an exception if it fails to do so within the given timeout.
	 * 
	 * @param proc A started process
	 * @param timeout Maximum time to wait
	 * @param unit Time unit of <code>timeout</code>
	 * @throws CommandFailedException Thrown in case of a timeout
	 */
	public static void waitFor(Process proc, long timeout, TimeUnit unit) throws CommandFailedException {
		try {
			boolean finishedInTime = proc.waitFor(timeout, unit);
			if (!finishedInTime) {
				proc.destroyForcibly();
				throw new CommandFailedException("Command timed out.");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static String toString(InputStream in, Charset charset) throws IOException {
		return CharStreams.toString(new InputStreamReader(in, charset));
	}

}
