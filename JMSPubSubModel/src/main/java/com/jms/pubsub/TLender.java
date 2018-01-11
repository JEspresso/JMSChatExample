package com.jms.pubsub;

/**
 * @author Kevin
 *
 */

import java.io.*;
import javax.jms.*;
import javax.naming.*;

import org.apache.log4j.Logger;

//	TLender class publishes new mortgage rates to a topic

public class TLender {

	private TopicConnection topicConnection = null;
	private TopicSession topicSession = null;
	private Topic topic = null;

	final static Logger logger = Logger.getLogger(TLender.class);

	/*	This constructor handles all the JMS initialization logic
	 * 	It does the following:
	 * 		1. Establishes a connection to the JMS provider
	 * 		2. Creates a TopicSession
	 * 		3. Gets the topic using the JNDI lookup */
	
	public TLender(String topicCF, String topicName) {

		/* 	Notice that the connection factory, connection, and session objects are similar to that of the QBorrower class, except that the 
		 * 	topic-based interfaces are used instead of the queue-based interfaces. 
		 * 	
		 * 	The important thing to note here is that although we are using the topic-based API, the implementation flow is the same as that of the 
		 * 	queue-based API used with the P2P model. 
		 * 		1. Get an initial context to the JMS provider
		 * 		2. Look up the connection factory
		 * 		3. Create JMS connection
		 * 		4. Create JMS session 
		 * 	 	5. Look up the destination
		 * 		6. Start the connection */
		
		try {
			// Connect to the provider and get the connection
			Context context = new InitialContext();
			TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory) context.lookup(topicCF);
			topicConnection = topicConnectionFactory.createTopicConnection();

			// Create the JMS Session
			topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

			topic = (Topic) context.lookup(topicName);

			// Now that the setup is complete, start the connection
			topicConnection.start();
		} catch (JMSException exc) {
			logger.error(exc);
			System.exit(1);
		} catch (NamingException exc) {
			logger.error(exc);
			System.exit(1);
		}
	}

	private	void publishRate(double newRate) {
		
		/*	Once the TLender class is initialized, the rate is entered through the command line. At this point, the publishRate() is invoked from 
		 * 	the main() method and the rate published to the topic. 
		 * 	
		 * 	TLender class will not wait for the response once the message has been published. 
		 * 	TLender class does not know or care about who is subscribing to the rates, what they are doing with the data, or how many subscribers 
		 * 	are receiving the rate information. */
		
		try {
			/*	Create BytesMessage to hold the data - Again, we could have chosen any of the 5 JMS types, but we chose the BytesMessage for 
			 * 	maximum portability. */
			
			BytesMessage bytesMessage = topicSession.createBytesMessage();
			bytesMessage.writeDouble(newRate);

			/*	Create the publisher and publish the message 
			 * 	
			 * 	After the message is created, we then create a TopicPublisher object, specifying the topic we wish to publish the message to, and 
			 * 	then publish the message using the publish() method
			 * 	
			 * 	Just the like send() method in the P2P model, there are several overridden publish() methods available in the TopicSender object. 
			 * 	The one we are using here accepts only the JMS message object as the single argument. 
			 * 	
			 * 	The other overridden methods allow you to specify: 
			 * 		1. Topic
			 * 		2. Delivery mode
			 * 		3. Message priority
			 * 		4. Message expiry
			 * 
			 * 	Since we are not specifying any of the other values in the example, 
 			 *		1. Message priority is set to normal (4) 
			 * 		2. Delivery mode is set to persistent (DeliveryMode.PERSISTENT)
			 * 		3. Message expiry/Time to live is set to 0 - indicating that the message will never expire
			 * 
			 * 	All these parameters can be overridden by using one of the publish() methods */
			TopicPublisher topicPublisher = topicSession.createPublisher(topic);
			topicPublisher.publish(bytesMessage);
		} catch (JMSException exc) {
			logger.error(exc);
		}
	}

	public void exit() {

		try {
			topicConnection.close();
		} catch (JMSException exc) {
			logger.error(exc);
		}
		System.exit(0);
	}
	
	/*	main() method instantiates the Tlender class and, upon receiving a new rate, invokes the publishRate() method to publish the rate to the 
		topic */	
	
	public static void main(String args[]) {

		String topicCF = null;
		String topicName = null;

		/* 	main() method accepts two arguments from the command line:
			 * 		1. JNDI name of the topic connection factory
			 * 		2. JNDI name of the topic used to publish the rates */
		
		if (args.length == 2) {
			topicCF = args[0];
			topicName = args[1];
		} else {
			logger.error("Invalid arguments. Should be: ");
			logger.error("java TLender factory topic");
			System.exit(0);
		}

		TLender lender = new TLender(topicCF, topicName);

		try {
			// Read all standard input and send it as a message
			BufferedReader standardInput = new BufferedReader(new InputStreamReader(System.in));

			logger.info("Topic publisher application started");
			logger.info("Press enter to quit application");
			logger.info("Enter: Rate (e.g. 6.8)");

			while (true) {
				logger.info("> ");
				
				/*	Once the input parameters have been validated, the TLender class is instantiated and a loop is started that reads the new 
				 * 	mortgage rate from the console */

				String rate = standardInput.readLine();

				//	The input loop continues until the user presses enter on the console without entering any data
				if (rate == null || rate.trim().length() <= 0) {
					lender.exit();
				}

				//	Rate is then parsed and then publishRate() method is invoked
				double newRate = Double.valueOf(rate);
				lender.publishRate(newRate);
			}
		} catch (IOException exc) {
			logger.error(exc);
		}
	}
}
