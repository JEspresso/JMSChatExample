package com.jms.chatexample;

import java.io.*;

import javax.jms.*;
import javax.naming.*;

//Program to illustrate JMS pub/sub messaging model. 
//Producer/Publisher can send a message to many consumers/subscribers by delivering the message
//to a single topic*/
public class Chat implements MessageListener {

	//second commit
	private TopicSession pubSession;
	private TopicPublisher publisher;
	private TopicConnection connection;
	private String userName;
	
	//Constructor to initialize Chat
	public Chat(String topicFactory, String topicName, String userName) throws Exception{
		
		//Obtain a JNDI connection using the jndi.properties file
		InitialContext ctx = new InitialContext();
		
		//Lookup a JMS connection factory and create the connection
		TopicConnectionFactory conFactory = (TopicConnectionFactory)ctx.lookup(topicFactory);
		TopicConnection connection = conFactory.createTopicConnection();
		
		//Create two JMS session objects
		TopicSession pubSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		TopicSession subSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		
		//Lookup a JMS topic
		Topic chatTopic = (Topic)ctx.lookup(topicName);
		
		/*Create a JMS publisher and subscriber. The additional parameters on the createSubscriber 
		 * are a message selector (null) and a true value for the noLocal flag indicating that the 
		 * messages produced from this publisher should not be consumed by this publisher*/
		TopicPublisher publisher = pubSession.createPublisher(chatTopic);
		TopicSubscriber subscriber = subSession.createSubscriber(chatTopic, null, true);
		
		//Set a JMS message listener
		subscriber.setMessageListener(this);
		
		//Initialize the chat application variables
		this.connection = connection;
		this.pubSession = pubSession;
		this.publisher = publisher;
		this.userName = userName;
		
		//Start the JMS connection. This allow message to be delivered 
		connection.start();
	}
	
	//Receive messages from the Topic subscriber
	public void onMessage(Message message) {
		try {
			TextMessage textMessage = (TextMessage)message;
			System.out.println(textMessage.getText());
		} catch(JMSException ex) {
			ex.printStackTrace();
		}
	}
	
	//Create and send message using publisher
	protected void writeMessage(String text) throws JMSException{
		TextMessage message = pubSession.createTextMessage();
		message.setText(userName + "\t:\t" + text);
		publisher.publish(message);
	}
	
	//Clone the JMS connection
	public void close() throws JMSException{
		connection.close();
	}
	
	//Run the chat client
	public static void main(String[] args) {
		try {
			if(args.length != 3)
				System.out.println("Factory, Topic or user name missing");

			//args[0] = topicFactory; args[1] = topicName; args[2] = userName;
			Chat chat = new Chat(args[0], args[1], args[2]);
			
			//Read from command line
			BufferedReader commandLine = new BufferedReader(new InputStreamReader(System.in));
			
			//Loop until the word "exit" is typed
			while(true) {
				String string = commandLine.readLine();
				if(string.equalsIgnoreCase("exit")) {
					chat.close();
					System.exit(0);					
				} else {
					chat.writeMessage(string);					
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
