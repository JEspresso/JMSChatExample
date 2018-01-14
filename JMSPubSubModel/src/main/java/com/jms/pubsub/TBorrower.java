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
 * 	As above, we have created a durable subscriber namesed Borrower1	
 * 		
 * 		TopicSubscriber topicSubscriber = topicSession.createDurableSubscriber(topic, "Borrower1");
 * 	
 * 	Some JMS providers allow you to statically define the durable subscriber in the configuration file or the admin interface. 
 * 	In this case, the durable subscriber is said to be an "administered durable subscriber". 
 * 	
 * 	This means that the durable subscriber is statically defined and known by the JMS provider. 
 * 	
 * 	However, suppose you needed to produce a temporary durable subscriber, say to gather mortgage rates for the next one or two days to do some 
 * 	trend analysis. It would be silly to have to modify the JMS provider to have to modify the JMS configuration files for this simple request.
 * 	
 * 	The JMS specification allows for durable subscribers to be defined dynamically at run-time, without having to statically define them in your 
 * 	JMS configuration files. 
 * 	
 * 	These types of durable subscribers are known as "dynamic durable subscribers" - for example, if we were to define a new durable subscriber 
 * 	called BorrowerA, we write
 * 		
 * 		TopicSubscriber topicSubscriber = topicSession.createDurableSubscriber(topic, "BorrowerA");
 * 	
 * 	In this case, BorrowerA durable subscriber is not defined in the JMS provider, and, therefore, is not an administered durable subscriber. 
 * 	
 * 	However, once the above line of code executes, a new durable subscriber called BorrowerA is created is created in the JMS provider, and, 
 * 	therefore, will receive all rates published to the topic, whether the subscriber is active or not. 
 * 	
 * 	The subscriber will remain a durable subscriber unti it is unsubscribed. 
 * 	
 * 	Although this feature provides a great deal of flexibility, it also comes with a price - Each durable subscriber, whether it is administered or 
 * 	dynamic, will receive a copy of the message published to the topic. 
 * 	
 * 	This means that when the subscriber is not active, those messages are being stored for each durable subscriber. From a capacity planning 
 * 	perspective, dynamic durable subscribers are somewhat dangerous in that it is difficult to control the number of durable subscribers using the 
 * 	system (although this can be monitored through an admin console, depending on the JMS provider and monitoring software that you are using). 
 * 	
 * 	Imagine, for a moment that 100 new dynamic durable subscribers were suddenly created to start receiving every mortgage rate or stock price to 
 * 	perform trend analysis. Then, once that analysis was complete, those 100 subscribers were retired but not unsubscribed. This means that every 
 * 	mortgage rate and every stock price update would be stored for those retired dynamic durable subscribers forever or until the machine hosting 
 * 	the JMS datastore ran out of storage/memory.
 * 	
 * 	There are a few methods a middleware administrator can use for addressing this issue in production environments to help control machine 
 * 	resources and capacity. 
 * 		
 * 	You can prohibit dynamic durable subscribers in your mortgage system by frequently (once a minute, or once an hour, etc) running a control 
 * 	program or database script that compares the known durable subscribers with those registered with the JMS provider. 
 * 	
 * 	Each JMS provider will store the messages in either a database or filesystem. 
 * 	
 * 	For example, OpenJMS - an open source JMS provider useful for testing and training purposes uses a JDBC 2.0 compliant database to store 
 * 	messages - refer to OpenJMS example
 * 	
 * 	For pub/sub messaging, the CONSUMERS table is used to hold durable subscribers and the MESSAGE_HANDLES table is used to link the messages to 
 * 	the consumers. 
 * 	
 * 	Given this schema, a middleware administrator can write a simple database script or program to query for any durable subscribers in the 
 * 	CONSUMER table that are not in the administered list of subscribers, and simply delete them from the JMS provider database (along with the 
 * 	corresponding messages in the MESSAGE_HANDLE table). 
 * 	
 * 	Another solution is to provide for the creation of dynamic durable subscribers, but only have them active for a limited period of time e.g. 
 * 	two days, one week, etc. 
 * 	
 * 	If you notice in the previous MySQL database schema definitions for OpenJMS, the CONSUMERS table has a created column containing the TIMESTAMP 
 * 	(represented as LONG in MILLISECONDS) of when that durable subscriber was first created. 
 * 	
 * 	You can easily create a database script or control program that executes each evening, removing any dynamic durable subscribers that were 
 * 	created a specified number of days ago. With this method, you can allow for flexibility of dynamic durable subscribers if the business rules or 
 * 	use cases call for them, but limit the lifespan of those dynamic durable subscribers to avoid filling up the storage capacity of the database. 
 * 	
 * 	A less aggressive approach would be to leverage the database schema of the JMS provider to create a report of the number of dynamic durable 
 * 	subscribers and current message count using the tables described earlier. 
 * 	
 * 	This report would show any significantly large message count for a particular subscriber, indicating that the durable subscriber is possibly 
 * 	retired or nolonger interested in the data. 
 * 	
 * 	The dynamic subscriber would then be flagged as a possible candidate for removal/message cleanup. 
 * 	
 *=====================================================================================================================================================
 *	Unsubscribing Dynamic Durable Subscriber
 *=====================================================================================================================================================
 *	
 *	private void exit() {
 *		try {
 *			subscriber.close();
 *			topicSession.unsubscribe("BorrowerA");
 *			topicConnection.close();
 *		} catch(javax.jms.JMSException exc) {
 *			exc.printStackTrace();
 *		} 
 *		System.exit(0);
 *	}
 *	
 *	For nondurable subscribers, calling the close() method on the TopicSubscriber class is sufficient. 
 *	
 *	For durable subscriptions, there is an unsubscribe(String name) method on the TopicSession object, which takes the subscription name as its 
 *	parameter. 
 *	
 *	This informs the JMS provider that it should nolonger store messages on behalf of this client. 
 *	
 *	You cannot call the unsubscribe() method without first closing the subscription (you will get an exception if you do this). 
 *	
 *	Hence, both methods need to be called for durable subscriptions.
 *	
 *=====================================================================================================================================================
 *	Temporary Topics
 *=====================================================================================================================================================
 *	A temporary topic is a topic that is dynamicallt created by a JMS provider, using the createTemporaryTopic() method of the TopicSession object.
 *	
 * 	A temporary topic is associated with the connection that belongs to the TopicSession that created it. 
 * 	
 * 	It is only active for the duration of the connection and it is guaranteed to be unique across all connections. 
 * 	
 * 	Since it is temporary, it cannot be durable - it lasts only as long as its associated client connection is active. 
 * 	
 * 	Since a temporary topic is unique across all client connections (it is obtained dynamically through a method call on a client's session object),  
 * 	it is unavailable to other JMS clients unless the topic identity is transferred using the JMSReplyTo header. 
 * 	
 * 	While any client may publish messages on another client's temporary topic, only the sessions that are associated with the JMS client connection 
 * 	that created the temporary topic may subscribe to it. JMS clients can also publish messages to their own temporary topics. 
 * 	
 * 	A temporary topic allows a consumer to respond directly to a producer. In larger real-world applications, there may be many publishers and 
 * 	subscribers exchanging messages across many topics. 
 * 	
 * 	A message may represent a workflow, which may take multiple hops through various stages of a business process. In that type of scenario, the 
 * 	consumer of a message may never respond directly to the producer that originated the message.
 * 	
 * 	It is more likely that the respond to the message will be forwarded to some other process. 
 * 	
 * 	Thus, the JMSReplyTo header can be used as a place to specify a forwarding address, rather than the destination address of the original sender. */
