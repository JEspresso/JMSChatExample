package com.jms.pubsub;

/**
 * @author Kevin
 *
 */

//	TBorrower class acts as a subscriber to the rate topic and, as such, is a an asynchronous message listener (similar to QBorrower class)

import java.io.*;
import javax.jms.*;
import javax.naming.*;

import org.apache.log4j.Logger;

public class TBorrower implements MessageListener {
	
	private TopicConnection topicConnection = null;
	private TopicSession topicSession = null;
	private Topic topic = null;
	
	private double currentRate;
	
	final static Logger logger = Logger.getLogger(TBorrower.class);
	
	//	TBorrower constructor works in the same way as TLender constructor 
	public TBorrower(String topicCF, String topicName, String rate) {
		
		try {
			currentRate = Double.valueOf(rate);
			
			//	Connect to the provider and get the JMS connection
			Context context = new InitialContext();
			TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory)context.lookup(topicCF);
			topicConnection = topicConnectionFactory.createTopicConnection();
			
			// Create the JMS Session
			topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
			
			topic = (Topic)context.lookup(topicName);
			
			/*	Once the connection is started, the TBorrower class can begin to receive messages. 
			 * 	However, before it can receive messages, it must be registered by the TopicSubscriber as a message listener (in this case, a 
			 * 	subscriber) 
			 * 	
			 * 	At this point, a separate listener thread is started. 
			 * 	That thread will wait until a message is received, and upon receipt of a message will invoke the onMessage() method of the 
			 * 	listener class. 
			 * 	
			 *  In this case, we set the message listener to the TBorrower object using the "this" keyword in the setMessageListener() method */
			
			TopicSubscriber topicSubscriber = topicSession.createSubscriber(topic);
			topicSubscriber.setMessageListener(this);

			topicConnection.start();
			
			logger.info("Waiting for loan rates...");
		} catch (JMSException exc) {
			logger.error(exc);
			System.exit(1);
		} catch (NamingException exc) {
			logger.error(exc);
			System.exit(1);
		}
	}
	
	public void onMessage(Message message) {

		try {
			/*	When a message is received on the topic specified in the createSubscriber() method, the listener thread will asynchronously invoke the 
			 * 	onMessage() method of the listener class (in our case, TBorrower class is also the listener class) 
			 *	
			 * 	The onMessage() method first casts the message to a BytesMessage (the message type we are expecting to receive from the lender)
			 * 	
			 * 	It then extracts the new rate and determines whether to refinance or not
			 * 	
			 * 	In practise, it would be better to make this method failsafe by checking the JMS message type using the "instanceof" keyword in the 
			 * 	event that another message type was being sent to that queue. 
			 *		if(message instanceof BytesMessage) {
			 * 			// process request
			 * 		} else {
			 * 			throw new IllegalArgumentException("Unsupported message type") */
			
			BytesMessage bytesMessage = (BytesMessage)message;
			double newRate = bytesMessage.readDouble();
			
			//	If the rate is at least 1 point lower than the current rate, then recommend financing 
			if((currentRate - newRate) >= 1.0) {
				logger.info("New rate = " + newRate + " :- Consider refinancing loan");
			} else {
				logger.info("New rate = " + newRate + " :- Keep existing loan");
			}
			logger.info("Waiting for rate updates...");
		} catch (JMSException exc) {
			logger.error(exc);
			System.exit(1);
		} catch (Exception exc) {
			logger.error(exc);
			System.exit(1);
		}
	}
	
	private void exit() {
		
		try {
			topicConnection.close();
		} catch (JMSException exc) {
			logger.error(exc);
		}
		System.exit(0);
	}
	
	public static void main(String args[]) {
		
		/*	main() method validates the command-line arguments and invokes the constructor by instantiating a new TBorrower class
		 * 	It then keeps the primary thread alive until the enter key is pressed on the command line. */
		
		String topicCF = null;
		String topicName = null;
		String rate = null;
		
		if(args.length == 3) {
			topicCF = args[0];
			topicName = args[1];
			rate = args[2];
		} else {
			logger.error("Invalid arguments. Should be: ");
			logger.error("java TBorrower factory topic rate");
			System.exit(0);
		}
		
		TBorrower borrower = new TBorrower(topicCF, topicName, rate);
		
		try {
			//	Run until enter is pressed
			BufferedReader standardInput = new BufferedReader(new InputStreamReader(System.in));
			logger.info("Topic subscriber application started");
			logger.info("Press enter to quit the application");
			standardInput.readLine();
			borrower.exit();
		} catch (IOException exc) {
			logger.error(exc);
		}
	}
}


/*=====================================================================================================================================================
 * 	Durable vs Nondurable Subscribers
 *=====================================================================================================================================================
 * 	
 * 	Nondurable Subscriber
 * 	
 * 	If you were to run the TBorrower class and then publish several rates, the TBorrower class would pick up the new rate and make a determination 
 * 	as to whether it was a good rate or not. 
 * 	
 * 	However, if you were to terminate the TBorrower class, publish some new rates, then restart the TBorrower class, you would not have picked up 
 * 	the rates that were published to the topic while the TBorrower class was not running. Why? 
 * 	
 * 	- 	Because the TBorrower was created as a nondurable subscriber. 
 * 		TopicSubscriber topicSubscriber = topicSession.createSubscriber(topic);
 * 	
 * 	Nondurable subscribers receive messages only when they are actively listening on that topic. Otherwise, the message is gone. 
 * 	
 * 	In Pub/Sub model, there is no real concept of a topic holding of all the messages, rather, when a message is received by the JMS provider, the 
 * 	provider makes a copy of that message for each subscriber.  
 * 		
 * 	If the subscriber is not active, it does not receive a copy of that message. 
 * 	
 * 	Durable Subscribers
 * 	
 * 	They are created by specifying a subscriber name in the JMS provider through configuration or through an admin interface and then using the 
 * 	method createDurableSubscriber(), which accepts a subscription name as one of the parameters.
 * 		
 * 		TopicSubscriber topicSubscriber = topicSession.createDurableSubscriber(topic, "Borrower1");
 * 	
 *================================================================================================================================================ 	
 * 	Dynamic vs Administered Subscribers
 *================================================================================================================================================ 	
 * 	
 * 	
 * 		
 * 	
 * 	
 * 	
 * */
