/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : JettyStopListener.java
 *  Author              : gwaller
 *  Approver            : Gareth Waller 
 * 
 *  Notes               :
 *
 *
 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 */
package uk.ac.jorum;


import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.mortbay.jetty.Server;

/**
 * @author gwaller
 * 
 */
public class JettyStopListener extends Thread{

	private String _key;
	private Server _server;

	ServerSocket _serverSocket;
	boolean _kill;

	public JettyStopListener(int port, String key, Server server, boolean kill) throws UnknownHostException, IOException {
		if (port <= 0)
			throw new IllegalStateException("Bad stop port");
		if (key == null)
			throw new IllegalStateException("Bad stop key");

		_key = key;
		_server = server;
		_kill = kill;

		_serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
		_serverSocket.setReuseAddress(true);
	}

	public void run() {
		while (_serverSocket != null) {
			Socket socket = null;
			try {
				socket = _serverSocket.accept();
				socket.setSoLinger(false, 0);
				LineNumberReader lin = new LineNumberReader(new InputStreamReader(socket.getInputStream()));

				String key = lin.readLine();
				if (!_key.equals(key))
					continue;
				String cmd = lin.readLine();
				if ("stop".equals(cmd)) {
					try {
						socket.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						socket.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						_serverSocket.close();
					} catch (Exception e) {
						e.printStackTrace();
					}

					_serverSocket = null;

					if (_kill) {
						System.exit(0);
					} else {

						try {
							_server.stop();
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				socket = null;
			}
		}
	}

}
