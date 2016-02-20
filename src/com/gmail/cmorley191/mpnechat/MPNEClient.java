package com.gmail.cmorley191.mpnechat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

import com.gmail.cmorley191.mpne.ConnectionListener;
import com.gmail.cmorley191.mpne.MPNESocket;
import com.gmail.cmorley191.mpne.MPNESocket.SocketPeerConnection;

/**
 * Demonstration of MPNE UDP framework ({@link MPNESocket} and
 * {@link MPNESocket.SocketPeerConnection}) in the form of a console
 * peer-to-peer chat application.
 * 
 * @author Charlie Morley
 *
 */
public class MPNEClient {

	public static void main(String[] args) {
		/*
		 * Initialization
		 */
		Scanner scanner = new Scanner(System.in);

		MPNESocket socket;
		while (true) {
			System.out.print("Enter local port: ");
			try {
				socket = new MPNESocket(Integer.parseInt(scanner.nextLine().trim()));
				break;
			} catch (Exception e) {
				System.out.println("Invalid port");
			}
		}
		System.out.println("Opened socket.");

		ArrayList<SocketPeerConnection> peers = new ArrayList<SocketPeerConnection>();
		ArrayList<ConnectionListener> peerListeners = new ArrayList<ConnectionListener>();
		System.out.println("Enter to send data to connected peers. Use \"/help\" for a list of commands.");

		/*
		 * Running loop
		 */
		while (true) {
			String line = scanner.nextLine();

			/*
			 * Input is command
			 */
			if (line.startsWith("/")) {
				line = line.substring(1);

				/*
				 * Command: help
				 */
				if (line.startsWith("help")) {
					System.out.println("/help - Shows this list");
					System.out.println("/exit - Closes the socket and exits");
					System.out.println("/connect [host] [port] - Opens two-way channel to this peer");
					System.out.println("/list - Lists all open peer channels");
					System.out.println("/disconnect [host] - Closes all channels to the specified host");

					/*
					 * Command: exit
					 */
				} else if (line.startsWith("exit"))
					break;

				/*
				 * Command: connect
				 */
				else if (line.startsWith("connect")) {
					line = line.substring(7);
					String[] parts = line.split(" ");
					if (parts.length != 3)
						System.out.println("Bad syntax");
					else {
						try {
							SocketPeerConnection connection = socket.new SocketPeerConnection(
									InetAddress.getByName(parts[1]), Integer.parseInt(parts[2]));
							ConnectionListener listener = new ConnectionListener() {

								@Override
								public void dataReceived(byte[] data) {
									System.out.println("Received: " + new String(data));
								}
							};
							connection.addConnectionListener(listener);
							peerListeners.add(listener);
							peers.add(connection);
							System.out.println("Channel open to " + connection.getAddress().toString() + ":"
									+ connection.getPort());
						} catch (UnknownHostException e) {
							System.out.println("Invalid host");
						} catch (NumberFormatException e) {
							System.out.println("Invalid port");
						}
					}

					/*
					 * Command: list
					 */
				} else if (line.startsWith("list")) {
					System.out.println(peers.size() + " open channel" + ((peers.size() != 1) ? "s" : "")
							+ ((peers.size() != 0) ? ":" : "."));
					for (SocketPeerConnection peer : peers)
						System.out.println(peer.getAddress().toString() + ":" + peer.getPort());

					/*
					 * Command: disconnect
					 */
				} else if (line.startsWith("disconnect")) {
					line = line.substring(10);
					String[] parts = line.split(" ");
					if (parts.length != 2)
						System.out.println("Bad syntax");
					else {
						try {
							InetAddress address = InetAddress.getByName(parts[1]);
							ArrayList<SocketPeerConnection> removeList = new ArrayList<SocketPeerConnection>();
							for (SocketPeerConnection peer : peers)
								if (peer.getAddress().equals(address))
									removeList.add(peer);
							for (SocketPeerConnection peer : removeList) {
								ConnectionListener listener = peerListeners.get(peers.indexOf(peer));
								peer.removeConnectionListener(listener);
								peerListeners.remove(listener);
								peers.remove(peer);
								System.out.println("Closed channel to " + address.toString() + ":" + peer.getPort());
							}
							System.out.println("Closed " + removeList.size() + " channel"
									+ ((removeList.size() != 1) ? "s" : "") + ".");
						} catch (UnknownHostException e) {
							System.out.println("Invalid host");
						}
					}

					/*
					 * Unknown command
					 */
				} else
					System.out.println("Unknown command");

				/*
				 * Input is text to send
				 */
			} else {
				int errors = 0;
				for (SocketPeerConnection peer : peers)
					try {
						peer.send(line.getBytes());
					} catch (IOException e1) {
						errors++;
					}
				if (errors != 0)
					System.out.println("Error sending message to " + errors + "peers");
			}
		}

		socket.close();
		scanner.close();
	}
}
