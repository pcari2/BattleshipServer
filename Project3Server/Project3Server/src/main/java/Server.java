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
Paolo Carino CS 342 Project 4
	A BattleShip Game that can
 */

public class Server{

	int count = 1;

	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	ArrayList<String> Lobby = new ArrayList<String>();

	ArrayList<ArrayList<String>> ActiveGameSessions = new ArrayList<ArrayList<String>>();
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


				 while(true) {
					    try {
							Message data = (Message) in.readObject(); // reads in Message Sent

//							// If the message is not a Username then send to the GUI
//							if (data.getType() != Message.MessageType.USER_ID_CREATE) {
//								callback.accept(data);
//							}

							// If the username hasn't been set use the Constructor user# as username
							if (data.getSender() == null) {
								data.setSender(username);
							}


							switch (data.getType()) {

								case PLAYER_LOOKING_FOR_GAME:
									// if there is another player looking for a game (Lobby size is >= 2), add them to ArrayList "ActiveGameSessions"
									// send message back to both Clients (type: GAME_FOUND)
									ArrayList<String> activeGameSession = null;
									if (!Lobby.isEmpty()) {
										//add the first player in lobby to ActiveGameSession
										String player1 = Lobby.get(0); // Get player from front of list
										activeGameSession = new ArrayList<>();
										activeGameSession.add(player1);
										activeGameSession.add(data.getUsername());

										// Add the activeGameSession to the ActiveGameSessions list
										ActiveGameSessions.add(activeGameSession);

										Message gameFoundMessagePlayer1 = new Message();
										gameFoundMessagePlayer1.setMessageType(Message.MessageType.GAME_FOUND);
										clientsHashMap.get(player1).send(gameFoundMessagePlayer1);

										Message gameFoundMessagePlayer2 = new Message();
										gameFoundMessagePlayer1.setMessageType(Message.MessageType.GAME_FOUND);
										clientsHashMap.get(data.getUsername()).send(gameFoundMessagePlayer2);

										Lobby.remove(0);
									} else {
										Lobby.add(data.getUsername());
									}
									break;

								case REGULAR_MOVE:
									// Receive move from client. This message should have the client's username.
									// Use this client's username to find the username of complement client in ActiveGameSessions
									// Send message with move to complement client
									String currentUser = data.getUsername();

									ArrayList<String> currentGameSession = null;
									for (ArrayList<String> gameSession : ActiveGameSessions) {
										if (gameSession.contains(currentUser)) {
											currentGameSession = gameSession;
											break;
										}
									}

									if (currentGameSession != null) {
										// Get the complement user's username
										String complementUser = currentGameSession.get(0).equals(currentUser) ? currentGameSession.get(1) : currentGameSession.get(0);
										Message moveMessage = new Message();
										moveMessage.setMessageType(Message.MessageType.REGULAR_MOVE);
										moveMessage.setUsername(currentUser); // Set the username of the player making the move
										moveMessage.setContent(data.getContent());
										clientsHashMap.get(complementUser).send(moveMessage); // Send the message to the complement user
									} else {
										System.out.println("Error: No active game session found for user " + currentUser);
									}
									break;




							}
						} catch(Exception e) {  // if Client exists out

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


	
	

	
