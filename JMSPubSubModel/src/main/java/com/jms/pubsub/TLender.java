package com.jms.pubsub;

/**
 * @author Kevin
 *
 */

import java.io.*;
import javax.jms.*;
import javax.naming.*;

import org.apache.log4j.Logger;

public class TLender {

	private TopicConnection topicConnection = null;
	private TopicSession topicSession = null;
	private Topic topic = null;

	final static Logger logger = Logger.getLogger(TLender.class);

	public TLender(String topicCF, String topicName) {

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

	private void publishRate(double newRate) {

		try {
			// Create JMS message
			BytesMessage bytesMessage = topicSession.createBytesMessage();
			bytesMessage.writeDouble(newRate);

			// Create the publisher and publish the message
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

	public static void main(String args[]) {

		String topicCF = null;
		String topicName = null;

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
			logger.info("Enter: Rate");
			logger.info("\nE.g. 6.8");

			while (true) {
				logger.info("> ");
				String rate = standardInput.readLine();

				if (rate == null || rate.trim().length() <= 0) {
					lender.exit();
				}

				// Parse the deal description
				double newRate = Double.valueOf(rate);
				lender.publishRate(newRate);
			}
		} catch (IOException exc) {
			logger.error(exc);
		}
	}
}
