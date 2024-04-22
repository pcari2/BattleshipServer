import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;


//import com.sun.security.ntlm.Client;
import javafx.application.Platform;
import javafx.scene.control.ListView;

/*
Paolo Carino CS 342 Project 3
	A messaging application between clients and the server, where clients can
	message each other as a whole group, or to individual clients they choose.
	Additionally, the server has a GUI that keeps track of all the inputs and
	messages being sent
 */

public class Server{

	int count = 1;	
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	TheServer server;
	private Consumer<Message> callback;
	HashMap<String, ClientThread> clientsHashMap = new HashMap<>();
	
	
	Server(Consumer<Message> call){
	
		callback = call;
		server = new TheServer();
		server.start();
	}
	
	
	public class TheServer extends Thread{
		
		public void run() {
		
			try(ServerSocket mysocket = new ServerSocket(5555);){
		    System.out.println("Server is waiting for a client!");
		  
			
		    while(true) {

                Socket clientSocket = mysocket.accept();
                System.out.println("Client has connected"); // outputs if the client connects
                ClientThread c = new ClientThread(clientSocket, count); // creates new clientThread
                clients.add(c); // adds them to the list
				clientsHashMap.put(c.username, c); // adds them to the hashmap
                c.start();
                count++; // counts the user it is

				
			    }
			}//end of try
				catch(IOException e) {
					Message messageException = new Message(Message.MessageType.ERROR, "Server", "Server socket did not launch");
					callback.accept(messageException);
				}
			}//end of while
		}
	

		class ClientThread extends Thread{
			
		
			Socket connection;
			int count;
			private String username;
			ObjectInputStream in;
			ObjectOutputStream out;
			
			ClientThread(Socket s, int count){
				this.connection = s;
				this.count = count;
				username = "user" + count;
			}

			public void setUsername(String username) {
				this.username = username;
			}
			public String getUsername() {
				return username;
			}

			public void updateClients(Message message) {
				for(int i = 0; i < clients.size(); i++) {
					ClientThread t = clients.get(i);
					try {
					 t.out.writeObject(message);
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			public void run(){
					
				try {

					in = new ObjectInputStream(connection.getInputStream());
					out = new ObjectOutputStream(connection.getOutputStream());
					connection.setTcpNoDelay(true);	
				}
				catch(Exception e) {
					System.out.println("Streams not open");
				}

				// Sends to everyone that clientThread joined
				Message ServerJoinMessage = new Message(Message.MessageType.GROUP_MESSAGE, "Server", "user" + count + " has joined");
				updateClients(ServerJoinMessage);

				// Updates the Connected Clients List
				sendUserListToClients();

				// Outputs it in the ServerGUI
				callback.accept(ServerJoinMessage);


				 while(true) {
					    try {
					    	Message data = (Message) in.readObject(); // reads in Message Sent

							// If the message is not a Username then send to the GUI
							if (data.getType() != Message.MessageType.USER_ID_CREATE) {
								callback.accept(data);
							}

							// If the username hasn't been set use the Constructor user# as username
							if (data.getSender() == null) {
								data.setSender(username);
							}


							switch (data.getType()) {

								// Create a username
								case USER_ID_CREATE:
									ClientThread client = clientsHashMap.get(username);
									String newUsername = data.getContent();

									// If it already exists then send back an error Message
									if (clientsHashMap.containsKey(newUsername)) {
										Message usernameTakenMessage = new Message(Message.MessageType.ERROR, "Server", "Username is already taken");
										client.send(usernameTakenMessage);
									}
									// If it doesn't change the username for the client
									else {
										if (client != null) {
											String oldUsername = username;

											// removes the old username's hashmap bucket
											clientsHashMap.remove(oldUsername);
											client.setUsername(newUsername);

											// adds it back with the new username
											clientsHashMap.put(newUsername, client);
											Message usernameChange = new Message(Message.MessageType.USER_ID_CREATE, oldUsername, newUsername);
											sendUserListToClients();

											// Sends message to the server
											callback.accept(usernameChange);
										}
									}
									break;

								// Sends message to all clients connected
								case GROUP_MESSAGE:
									for (ClientThread clientThread : clientsHashMap.values()) {
										clientThread.send(data);
									}
									break;

								// Sends message to those in groupchat only
								case GROUPCHAT_MESSAGE:
									List<String> receivers = data.getReceivers();
									for (String name : receivers) {
										ClientThread receiverThread = clientsHashMap.get(name);
										receiverThread.send(data);
									}

								// Sends message to individual client
								case PRIVATE_MESSAGE:
									String receiver = data.getReceiver();
									ClientThread receiverThread = clientsHashMap.get(receiver);
									receiverThread.send(data);
								}
					    	
					    	}
					    catch(Exception e) {  // if Client exists out

							clients.remove(this); // remove them from the list of clients

							Message messageError = new Message(Message.MessageType.LEAVE, "Server", "OOOOPPs...Something wrong with the socket from " + username + "....closing down!");
					    	callback.accept(messageError); // output an error message to the server

							//Notifies everyone who left
							Message messageLeave = new Message(Message.MessageType.LEAVE, "Server", username + " has left");
							updateClients(messageLeave);

							// removes them from the hashmap of clients
							clientsHashMap.remove(username);

							// updates the connected clients list
							sendUserListToClients();
							closeConnection(); // closes the connection of the lost client
					    	break;
					    }
					}
				}//end of run

			public void send(Message data) { // sends message to client

				try {
					out.writeObject(data);
					out.flush();
				} catch (IOException e) {

					if (e instanceof SocketException && "Connection reset by peer".equals(e.getMessage())) {
						closeConnection();
					}
					else {
						e.printStackTrace();
					}
				}
			}

			private void sendUserListToClients() {
				List<String> usernames = new ArrayList<>(); // creates a list of usernames who are connected

				// adds each clientThreads username
				for (ClientThread clientThread : clientsHashMap.values()) {
					usernames.add(clientThread.getUsername());
				}

				// sends the list of usernames to everyone
				Message usernamesListSend = new Message(Message.MessageType.LIST_OF_NAMES, usernames);
				for (ClientThread clientThread : clientsHashMap.values()) {
					clientThread.send(usernamesListSend);
				}
			}

			public void closeConnection() { // securely closes the connection
				try {
					if (out != null) {
						out.close();
					}
					if (connection != null && !connection.isClosed()) {
						connection.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			
		}//end of client thread
}


	
	

	
