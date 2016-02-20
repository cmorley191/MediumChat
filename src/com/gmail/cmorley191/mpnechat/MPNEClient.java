/*
 * IMPORTANT Commenting note: MPNEClient is largely contained in one method,
 * so its comments are thorough as to promote readability. See the table of
 * contents below and the section headers (block comments starting with "~")
 */
/*-
 * Table of contents: (order of sections)
 * 
 * public class MPNEClient
 * 	~Protocol flags
 * 	~Working variables
 * 
 * 	public static void main
 * 		~Initialization
 * 		~Running loop
 * 			~If user inputs a command
 * 				~Command: help
 * 				~Command: exit
 * 				~Command: info
 * 				~Command: connect
 * 				~Command: list
 * 				~Command: disconnect
 * 				~Command: name
 * 				~Unknown command
 * 			~If user inputs a message
 * 		~Finalization
 */

package com.gmail.cmorley191.mpnechat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
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

	/*
	 * ~Protocol flags (sequences of bytes used to structure the data packets,
	 * abbreviated PF)
	 */
	/*
	 * Protocol flag terminology: "Level" indicates in which section flags
	 * appear - "L1L2L3L4L5L6..." (a flag in L2 comes directly after L1 with no
	 * bytes in between, for example). "final" indicates this is the last flag
	 * used in a packet (though other information may follow the final flag if
	 * the flag requires it, e.g. MESSAGE_TEXT requires the message in plain
	 * text).
	 */

	/*-
	 * How to read or use the protocol flag commenting scheme:
	 * Each flag lists its location, purpose, and protocol. (lack of a protocol means just use the flag alone)
	 * Start by adding the L1 flag.
	 * Then find the L2 flag applicable to the situation.
	 * Continue adding flags until a final flag is added.
	 * 
	 * Example: 
	 * Structuring a chat message from user "John": Hello, world!
	 * L1: PF_MEDIUMCHAT (because it is mandatory for all packets)
	 * L2: PF_MESSAGE (because it is for messages from peer users)
	 * L3: PF_MESSAGE_OPTION_PROFILE (because it is for messages from peer users that have usernames)
	 * L4, final: PF_MESSAGE_TEXT (because it is for messages from peer users and it is a final flag)
	 * 
	 * The packet would look like:
	 * [PF_MEDIUMCHAT][PF_MESSAGE][PF_MESSAGE_OPTION_PROFILE][4 (the length of "John")]["John"][PF_MESSAGE_TEXT]["Hello, world!"]
	 * The non-flag information is due to the individual packet protocols listed in their comments.
	 * 
	 * Note that the variables are listed in order of relevance, not level.
	 */

	/**
	 * (L1) for all packets in Medium Chat.
	 */
	private static final byte[] PF_MEDIUMCHAT = new byte[] { 1, 77, 25 };

	/**
	 * (L2, after MEDIUMCHAT) for all messages from peer users.
	 */
	private static final byte[] PF_MESSAGE = new byte[] { 2 };
	/**
	 * (L4, final, after MESSAGE_OPTION category) for all messages from peer
	 * users.
	 * <p>
	 * Indicates the start of the peer user's message content.<br>
	 * Protocol: [flag][remaining bytes: message in plain text] (end of packet)
	 * <p>
	 * Example: Message of "Hello, World!"<br>
	 * [PF_MESSAGE] ["Hello, World!"] (end of packet)
	 */
	private static final byte[] PF_MESSAGE_TEXT = new byte[] { 2 };
	/**
	 * (L3, part of MESSAGE_OPTION category, after MESSAGE) for all messages
	 * from peer users with usernames. Can share L3 with others from category.
	 * <p>
	 * Indicates the start of the peer user's username.<br>
	 * Protocol: [header][1 byte: length of the username ({@code len})][
	 * {@code len} bytes: username in plain text]
	 * <p>
	 * Example: Message from "John"<br>
	 * [PF_MESSAGE_TEXT] [4] ["John"]
	 */
	private static final byte[] PF_MESSAGE_OPTION_PROFILE = new byte[] { 17 };

	/*
	 * ~Functional variables
	 */
	/**
	 * This client's username data.
	 */
	private static byte[] currentProfile = null;

	public static void main(String[] args) {
		/*
		 * ~Initialization
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
		 * ~Running loop
		 */
		while (true) {
			String line = scanner.nextLine();

			/*
			 * ~If user inputs a command
			 */
			if (line.startsWith("/")) {
				line = line.substring(1);

				/*
				 * ~Command: help
				 */
				if (line.startsWith("help")) {
					System.out.println("/help - Shows this list");
					System.out.println("/exit - Closes the socket and exits");
					System.out.println("/info - Shows information about this client and session");
					System.out.println("/connect [host] [port] - Opens two-way channel to this peer");
					System.out.println("/list - Lists all open peer channels");
					System.out.println("/disconnect [host] - Closes all channels to the specified host");
					System.out.println("/name [username] - Sets this client's display name");

					/*
					 * ~Command: exit
					 */
				} else if (line.startsWith("exit"))
					break;

				/*
				 * ~Command: info
				 */
				else if (line.startsWith("info")) {
					try {
						System.out.println("Local host IP: " + InetAddress.getLocalHost().getHostAddress());
					} catch (UnknownHostException e) {
					}
					System.out.println("Local host port: " + socket.getPort());
					System.out.println(((currentProfile == null)
							? "No username set - others are shown this machine's IP and port as the username"
							: "Current username: " + new String(currentProfile)));
				}

				/*
				 * ~Command: connect
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
							String host = connection.getAddress().getHostAddress() + ":" + connection.getPort();
							if (peers.contains(connection)) {
								System.out.println("Channel already open to " + host);
							} else {
								ConnectionListener listener = new ConnectionListener() {

									@Override
									public void dataReceived(byte[] data) {
										int pos = 0;
										if (!startsWith(data, PF_MEDIUMCHAT, pos))
											return;
										pos += PF_MEDIUMCHAT.length;
										if (startsWith(data, PF_MESSAGE, pos)) {
											pos += PF_MESSAGE.length;
											String profile = host;
											while (true) {
												if (startsWith(data, PF_MESSAGE_TEXT, pos)) {
													pos += PF_MESSAGE_TEXT.length;
													System.out.println("<" + profile + "> "
															+ new String(Arrays.copyOfRange(data, pos, data.length)));
													break;
												} else if (startsWith(data, PF_MESSAGE_OPTION_PROFILE, pos)) {
													pos += PF_MESSAGE_OPTION_PROFILE.length;
													int len = data[pos];
													pos++;
													profile = new String(Arrays.copyOfRange(data, pos, pos + len));
													pos += len;
												} else
													break;
											}
										}
									}

									private boolean startsWith(byte[] data, byte[] expr, int startPos) {
										if (expr.length > data.length - startPos)
											return false;
										for (int i = 0; i < expr.length; i++)
											if (data[startPos + i] != expr[i])
												return false;
										return true;
									}
								};
								connection.addConnectionListener(listener);
								peerListeners.add(listener);
								peers.add(connection);
								System.out.println("Channel open to " + connection.getAddress().toString() + ":"
										+ connection.getPort());
							}
						} catch (UnknownHostException e) {
							System.out.println("Invalid host");
						} catch (NumberFormatException e) {
							System.out.println("Invalid port");
						}
					}

					/*
					 * ~Command: list
					 */
				} else if (line.startsWith("list")) {
					System.out.println(peers.size() + " open channel" + ((peers.size() != 1) ? "s" : "")
							+ ((peers.size() != 0) ? ":" : "."));
					for (SocketPeerConnection peer : peers)
						System.out.println(peer.getAddress().toString() + ":" + peer.getPort());

					/*
					 * ~Command: disconnect
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
					 * ~Command: name
					 */
				} else if (line.startsWith("name")) {
					line = line.substring(4);
					String[] parts = line.split(" ");
					if (parts.length != 2)
						System.out.println("Bad syntax");
					else {
						currentProfile = parts[1].getBytes();
						System.out.println("Username set to " + parts[1]);
					}

					/*
					 * ~Unknown command
					 */
				} else
					System.out.println("Unknown command");

				/*
				 * ~If user inputs a message
				 */
			} else {
				int errors = 0;
				for (SocketPeerConnection peer : peers)
					try {
						if (currentProfile == null)
							peer.send(MPNEClient.concat(PF_MEDIUMCHAT, PF_MESSAGE, PF_MESSAGE_TEXT, line.getBytes()));
						else
							peer.send(MPNEClient.concat(PF_MEDIUMCHAT, PF_MESSAGE, PF_MESSAGE_OPTION_PROFILE,
									new byte[] { (byte) currentProfile.length }, currentProfile, PF_MESSAGE_TEXT,
									line.getBytes()));
					} catch (IOException e1) {
						errors++;
					}
				if (errors != 0)
					System.out.println("Error sending message to " + errors + "peers");
			}
		}

		/*
		 * ~Finalization
		 */

		socket.close();
		scanner.close();
		System.exit(0);
	}

	/**
	 * Concatenates the specified byte arrays into a single byte array.
	 * 
	 * @param arrays
	 *            the arrays to be concatenated
	 * @return one array containing all the elements in each array specified
	 *         concatenated
	 */
	private static byte[] concat(byte[]... arrays) {
		int len = 0;
		for (byte[] array : arrays)
			len += array.length;
		byte[] full = new byte[len];
		int pos = 0;
		for (byte[] array : arrays) {
			System.arraycopy(array, 0, full, pos, array.length);
			pos += array.length;
		}
		return full;
	}
}
