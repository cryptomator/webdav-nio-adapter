package org.cryptomator.frontend.webdav.mount;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

class ProcessUtil {

	/**
	 * Fails with a CommandFailedException, if the process did not finish with the expected exit code.
	 *
	 * @param proc              A finished process
	 * @param expectedExitValue Exit code returned by the process
	 * @throws IOException Thrown in case of unexpected exit values
	 */
	public static void assertExitValue(Process proc, int expectedExitValue) throws IOException {
		int actualExitValue = proc.exitValue();
		if (actualExitValue != expectedExitValue) {
			try {
				@SuppressWarnings("resource") String error = proc.errorReader(StandardCharsets.UTF_8).lines().collect(Collectors.joining("\n"));
				throw new IOException("Stderr output: " + error);
			} catch (IOException e) {
				throw new IOException("Command failed with exit code " + actualExitValue + ". Expected " + expectedExitValue + ".", e);
			}
		}
	}

	/**
	 * Starts a new process and invokes {@link #waitFor(Process, long, TimeUnit)}.
	 *
	 * @param processBuilder The process builder used to start the new process
	 * @param timeout        Maximum time to wait
	 * @param unit           Time unit of <code>timeout</code>
	 * @return The finished process.
	 * @throws IOException      If an I/O error occurs when starting the process.
	 * @throws TimeoutException Thrown in case of a timeout
	 */
	public static Process startAndWaitFor(ProcessBuilder processBuilder, long timeout, TimeUnit unit) throws IOException, TimeoutException {
		Process proc = processBuilder.start();
		waitFor(proc, timeout, unit);
		return proc;
	}

	/**
	 * Waits for the process to terminate or throws an exception if it fails to do so within the given timeout.
	 *
	 * @param proc    A started process
	 * @param timeout Maximum time to wait
	 * @param unit    Time unit of <code>timeout</code>
	 * @throws TimeoutException Thrown in case of a timeout
	 */
	public static void waitFor(Process proc, long timeout, TimeUnit unit) throws TimeoutException {
		try {
			boolean finishedInTime = proc.waitFor(timeout, unit);
			if (!finishedInTime) {
				proc.destroyForcibly();
				throw new TimeoutException();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
