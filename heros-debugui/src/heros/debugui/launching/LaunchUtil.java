package heros.debugui.launching;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

public class LaunchUtil {
	
	public static final String ENV_VAR_NAME = "HEROS_DEBUG_PORT";

	public static String[] openSocketAndUpdateEnvironment(
			ILaunchConfiguration configuration, String[] environment) throws CoreException {
		String hostNameAndPort = LaunchUtil.openSocket(configuration);
		if(environment==null) environment = new String[0];
		String[] newEnv = new String[environment.length+1];
		System.arraycopy(environment, 0, newEnv, 1, environment.length);
		newEnv[0] = ENV_VAR_NAME + "="+hostNameAndPort;
		return newEnv;
	}

	private static String openSocket(final ILaunchConfiguration configuration) {
		final ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(0);
			String host = serverSocket.getInetAddress().getHostAddress();
			int port = serverSocket.getLocalPort();

			new Thread("SocketListener") {

				@Override
				public void run() {
					try {
						//wait at most 10 seconds for the socket to open
						//this allows the thread to terminate if no debugging is initiated by the analysis
						serverSocket.setSoTimeout(10000);
						Socket socket = serverSocket.accept();
						InputStream is = socket.getInputStream();
						System.err.println("socket opened");
					} catch(SocketTimeoutException e) {
						//ignore; just terminate thread
					} catch (IOException e) {
						e.printStackTrace();
					}					
				}
			}.start();
			//allow other thread to get to the point where we
			//accept connections on the server socket before we proceed here
			Thread.yield();
			
			return host + ":" +port;
		} catch (IOException e) {
			throw new RuntimeException("Problem creating server socket",e);
		} 
	}
}