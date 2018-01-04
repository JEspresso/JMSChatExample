package com.jms.p2p;

import java.io.*;
import javax.jms.*;
import javax.naming.*;

/*	In this example, QLender class is referred to as a "message listener" and, as such, implements the javax.jms.MessageListener interface and 
 * 	overrides the onMessage() method. 
 * 
 * 	QLender class is an asynchronous message listener, meaning that unlike the QBorrower it will not block when waiting for messages. 
 * 
 * 	This is evident from the fact the QLender class implements the MessageListener interface and then overrides the onMessage() method. 
 * */

/**
 * @author Kevin
 *	The role of the QLender class is to listen for loan requests on the loan request queue, determine if the salary meets the necessary business 
 * 	requirements, and finally send back the results to the borrower.
 */
public class QLender implements MessageListener {

	private QueueConnection qConnect = null;
	private QueueSession qSession = null;
	private Queue requestQ = null;

	
	//	The constructor in the QLender class works in the same way as the constructor in the QBorrower class 
	public QLender(String queueCF, String requestQueue) {
		try {
			//	Establish a connection to the provider, do a JNDI lookup to get the queue, creates a QueueSession and then starts the connection
			Context context = new InitialContext();
			QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) context.lookup(queueCF);
			qConnect = queueConnectionFactory.createQueueConnection();

			// create the JMS session
			qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

			// lookup the request queue
			requestQ = (Queue) context.lookup(requestQueue);

			// now that the setup is complete, start the connection
			qConnect.start();

			/*	Once the connection is started, the QLender class can beging to receive messages
			 * 	However, before it can receive messages, it must be registered by the QueueReceiver as a message listener */
			QueueReceiver queueReceiver = qSession.createReceiver(requestQ);
			
			/*	At this point, a separate listener thread is started. That thread will wait until a message is received, and upon receipt of a 
			 * 	message, will invoke the onMessage() method of the listener class. 
			 * 
			 * 	In this case, we set the message to the QLender class using the "this" keyword in the setMessageListner() method. 
			 * 
			 * 	We could have easily delegated the messaging work to another class that implemented the MessageListener interface
			 * 	
			 * 	queueReceiver.setMessageListener(someOtherClass);
			 * 
			 * */
			queueReceiver.setMessageListener(this);
			System.out.println("Waiting for loan requests...");
		} catch (JMSException ex) {
			ex.printStackTrace();
			System.exit(1);
		} catch (NamingException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public void onMessage(Message message) {
		
		/*	When a message is received on the queue specified in the createReceiver() method, the listener thread will asynchronously invoke 
		 * 	the onMessage() method of the listener class (in our case, the QLender class is also the listener class) */
		
		boolean accepted = false;
		
		if (message instanceof MapMessage) {
			try {
				/* 	The onMessage() method first casts the Message to a MapMessage (the message type we are expecting to receive from the borrower 
				 * 	
				 * 	It then extracts the salary and loan amount requested from the message payload, checks the salary to loan amount ratio, then 
				 * 	determines whether to accept or decline the loan request */
				MapMessage mapMessage = (MapMessage) message;
				double salary = mapMessage.getDouble("Salary");
				double loanAmount = mapMessage.getDouble("Loan Amount");

				if (loanAmount < 200000) {
					accepted = (salary / loanAmount) > .25;
				} else {
					accepted = (salary / loanAmount) > .33;
				}
				System.out.println("" + "Percent = " + (salary / loanAmount) + ", loan is " + (accepted ? "Accepted!" : "Declined"));

				/* 	Again, to make this more failsafe, it would be better to check the JMS message type using the instanceof operator in the even that 
				 * 	another message type was being sent to that queue 
				
					if(mapMessage instanceof MapMessage) {
						//	process the request
						} else {
							throw new IllegalArgumentException("Unsupported message type");
						} 	*/
				
				/* 	Once the loan request has been analyzed and the results determined, the QLender class needs to send the response back to the 
				 *  borrower. 
				 *  
				 *  It does this by first creating a JMS message to send. The response message does not need to be the same JMS message type as the 
				 *  loan request message that was received by the QLender. 
				 *  
				 *  To illustrate this point, the QLender returns a TextMessage back to the QBorrower. */
				TextMessage textMessage = qSession.createTextMessage();
				textMessage.setText(accepted ? "Accepted!" : "Declined");
				
				/*	The next statement sets the JMSCorrelationID, which is the JMS header property that is used by the QBorrower class to filter the 
				 * 	incoming response messages 
				 * 		
				 * 	When the message consumer e.g. QLender is ready to send the reply message, it sets the JMSCorrelationID message property to the 
				 * 	message ID from the original message. * */
				
				/* 	QLender application must now get the UUID property from the original message and set the JMSCorrelationID message proprty to
				 * 	this value
				 * 
					textMessage.setJMSCorrelationID(message.getStringProperty("UUID")); */
				
				textMessage.setJMSCorrelationID(mapMessage.getJMSCorrelationID());

				/*	Once the message is created, the onMessage() method then sends the message to the response queue specified by the JMSReplyTo 
				 * 	message header property. 
				 * 
				 * 	In the QBorrower class we set the JMSReplyTo header property when sending the original loan request. 
				 * 
				 * 	The QLender class can now use that header that property as the destination to send the response message to */
				QueueSender queueSender = qSession.createSender((Queue) mapMessage.getJMSReplyTo());
				queueSender.send(textMessage);

				System.out.println("\nWaiting for loan requests...");
			} catch (JMSException ex) {
				ex.printStackTrace();
				System.exit(1);
			} catch (Exception ex) {
				ex.printStackTrace();
				System.exit(1);
			}
		}
	}

	private void exit() {
		try {
			qConnect.close();
		} catch (JMSException ex) {
			ex.printStackTrace();
		}
		System.exit(0);
	}

	/*	Main method validates the command-line arguments and then invokes the constructor by instantiating a new QLender class. 
	 * 
	 * 	It then keeps the primary thread alive until the enter key is pressed on the command line
	 * */
	public static void main(String args[]) {
		String queueCF = null;
		String requestQ = null;

		if (args.length == 2) {
			queueCF = args[0];
			requestQ = args[1];
		} else {
			System.out.println("Invalid arguments. Should be");
			System.out.println("java QLender factory request_queue");
			System.exit(0);
		}

		QLender qLender = new QLender(queueCF, requestQ);

		try {
			// run until enter is pressed
			BufferedReader standardInput = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("QLender application started");
			System.out.println("Press enter to quit application");
			standardInput.readLine();
			qLender.exit();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}

/*	===================================================================================================================================================
 * 	Message Correlation
 * 	===================================================================================================================================================
 * 	
 * 	In the previous example, the borrower sent a loan request on a request queue and waited for a reply from the lender on a response queue.
 * 	Many borrowers maybe making requests at the same time, meaning that the lender application is sending many messages to the response queue.
 * 	Since the response queue may contain many messages, how can you be sure that the response you received from the lender was meant for you and 
 * 	another borrower?
 * 	
 * 	In general, whenever using the request/reply model, you must make sure that the response you are receiving is associated with the original 
 * 	message you sent.
 * 	
 * 	Message correlation is the technique used to ensure that you receive the right message. 
 * 	
 * 	The most popular method of correlating messages is leveraging the JMSCorrelationID message header property in conjuction with the JMSMessageID 
 * 	header property.
 * 	
 * 	The JMSCorrelationID property contains a unique String value that is known by both the sender and the receiver. 
 * 	The JMSMessageID is typically used, since it is unique and is available to the sender and receiver. 
 * 			
 * 	Although JMSMessageID is typically used to identify the unique message, it is certainly not a requirement.
 * 	You can use anything that can correlate the request and reply messages. 
 * 		
 * 	For example, as an alternative you could use the Java UUID class to generate a unique ID. 
 * 	
 * 	Although it is commonly used, you are not required to use the JMSCorrelationID message header property to correlate messages. 
 * 	As a matter of fact, you could set the correlation property to any application property in the message. 
 * 	
 * 	While this is certainly possible, you should leverage the header properties if they exist for full compatibility with messaging servers, 
 * 	third-party brokers, and third-party message bridges. 
 * 
 * 	===================================================================================================================================================
 * 	Dynamic Queues Versus Administered Queues
 * 	===================================================================================================================================================
 * 	Dynamic Queues
 * 	These are queues that are created through the application source code using a vendor-specific API.
 * 		
 *  Creating dynamic queues is useful if you have a large number of queues that may increase over time. 
 * 	
 * 	For example, consider a scenario where a book publisher has relationships with a large number of bookstores. The book publisher regularly sends 
 * 	new book information and order status to the bookstores. Let's assume that there are 1,000 bookstores related to the book publisher. That 
 * 	equates to 1,000 queues - somewhat excessive to administer. The book publisher can dynamically create the bookstore queues based on a 
 * 	numbering scheme, therefore quickly defining the queues necessary for this scenario (e.g. BookStore1, BookStore2, etc) 	
 * 	
 * 	Administered Queues
 * 	These are queue that are defined in the JMS provider configuration files or administered tools. 
 * 	
 * 	Setup and configuration is vendor-specific. 
 * 	A queue maybe used exclusively by one consumer or shared by multiple consumers. 
 * 	It may have a size limit (limiting the number of unconsumed messages held in the queue) with options for in-memory storage versus overflow to disk.
 * 	In addition, a queue maybe configured with a vendor-specific addressing syntax or special routing capabilities. 
 * 	
 * 	JMS does not attempt to define a set of APIs for all the possible options on a queue. It should be possible to set these options administratively, 
 * 	using the vendor-specific administration capabilities. 
 * 	Most vendors supply: 
 * 		1)	A command-line administration tool
 * 		2)	A graphical administration too
 * 		3)	An API for administering queues at runtime
 * 	
 * 	Other vendors supply all the three - however, it is not very portable as the application might always require admin privileges
 * 	
 * 	JMS provides a QueueSession.createQueue(String queuename) method, but this is not intended to define a new queue in the messaging system. 
 * 	It is intended to return a Queue object that represents an existing queue. 
 * 	
 * 	There is also a JMS defined method for creating a temporary queue that can only be consumed by the JMS client that created it using the 
 * 	QueueSession.createTemporaryQueue() method. 
 * 	
 * 	===================================================================================================================================================
 * 	Load Balancing Using Multiple Receivers
 * 	===================================================================================================================================================
 * 	A queue might have have multiple receivers attached to it for the purpose of distributing the workload of message processing. 
 * 	
 * 	JMS specification states that this capability must be implemented by a messaging provider, although it does not define the rules on how the 
 * 	messages are distributed among the consumers.
 * 	
 * 	A sender could use this feature to distribute messages to multiple instances of an application, each of which would provide its own receiver.
 * 	
 * 	When multiple receivers are attached to a queue, each message in the queue is delivered to one receiver. 
 * 	
 * 	The absolute order of messages cannot be guaranteed, since one receiver may process messages faster than the other. 
 * 	
 * 	From the receivers percpective, the messages it consumes should be in relative order - messages delivered to the queue earlier should be 
 * 	consumes first. 
 * 	
 * 	However, if a message need to be redelivered due to an acknowledgement failure, it is possible that it could be delivered to another receiver. 
 * 	
 * 	The other receiver may have already processed more recenlty delivered messages, which would place the redelivered message out of the original 
 * 	order. 
 * 	
 * 	If you would like to see multiple recipients in action, try starting two instances of the QLender class and one instance of the QBorrower class, 
 * 	each in a separate command window.
 * 	
 * 		java ch04.p2p.QLender QueueCF LoanRequestQ
 *		java ch04.p2p.QLender QueueCF LoanRequestQ
 *		java ch04.p2p.QBorrower QueueCF LoanRequestQ LoanResponseQ
 * 		
 * 	Now, when entering salary and loan amount in the command window, you will notice that the message is delivered to one or the other QLender 
 * 	application, but not both.
 * 	
 * 	The exact load balancing will vary between JMS providers. Some may use round-robin load balancing, while others may use first available balancing 
 * 	scheme. */
