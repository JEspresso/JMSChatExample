package com.jms.pubsub;

/**
 * @author Kevin
 *
 */

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
			
			//	Create the message listener
			TopicSubscriber topicSubscriber = topicSession.createSubscriber(topic);
			topicSubscriber.setMessageListener(this);
			
			//	Now that the setup is complete, start the Connection
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
			//	Get the data from the message
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
