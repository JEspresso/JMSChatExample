package com.jms.p2p;

import java.io.*;
import java.util.StringTokenizer;

import javax.jms.*;
import javax.naming.*;

import org.apache.log4j.Logger;

/**
 * @author Kevin
 *	QBorrower class is responsible for sending a loan request message to a queue containing a salary and amount
 */
public class QBorrower {

	private QueueConnection qConnect = null;
	private QueueSession qSession = null;
	private Queue responseQ = null;
	private Queue requestQ = null;
	
	final static Logger logger = Logger.getLogger(QBorrower.class);
	
	//	JMS initialization: 
	//	All JMS initialization is done in the QBorrower class constructor
	/**
	 * @param queueCF
	 * @param requestQueue
	 * @param responseQueue
	 */
	public QBorrower(String queueCF, String requestQueue, String responseQueue) {
		try {
			//	Connect to the provider and get the JMS connection (create an InitialContext)
			//	The JMS connection information needed to connect to the JMS provider is specified in the jndi.properties file located in the classpath
			Context context = new InitialContext();
			
			/*	Once we have the JNDI context, we can get the QueueConnectionFactory using the JNDI connection factory name passed into the 
			 * 	constructor arguments */
			QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory)context.lookup(queueCF);
			
			//	The QueueConnectionFactory is then used to create the QueueConnection using a factory method on the QueueConnectionFactory			
			qConnect = queueConnectionFactory.createQueueConnection();
			
			/*	Alternatively, you can pass a user name and password into the createQueueConnection() method as String arguments to perform basic
			 * 	authentication on the connection. A JMSSecurityException will be thrown if the user fails to authenticate
			 * 
			 * 	qConnect = queueConnectionFactory.createQueueConnection("sysemt", "manager");
			 * 
			 * 	At this point, a connection has been created to the JMS provider 
			 * 	When QueueConnection is created, the connection is initially in "stopped mode". This means that you can send messages to the queue,
			 * 	but no message consumers (including the QBorrower class - also a message consumer) may receive messages from this connection until 
			 * 	it is started
			 * 
			 * 	The QueueConnection object is used to create a JMS session object (specifically a QueueSession), which is the working thread and 
			 *  transactional unit of work in JMS. 
			 *  
			 *  Unlike JDBC which requires a connection for each transactional unit of work, JMS uses a single connection and multiple Session objects 
			 *  Typically, applications will create a single JMS Connection on application startup and maintain a pool of Session objects for use 
			 *  whenever a message needs to be produced or consumed 
			 *  
			 *  The QueueSession object is created through a factory object on the QueueConnection object 
			 *  QueueConnection variable is declared outside of the constructor so that the connection can be closed in the exit method of QBorrower 
			 *  class 
			 *  Close connection when not being used to free up resources
			 *  Closing the Connection object also closes any open Session objects associated with the connection */
			
			//	Create the QueueSession
			qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			
			/*	The first parameter of the createQueueSession() indicated whether the QueueSession is transacted or not. 
			 *  A value of "true" indicates that the session is transacted, meaning that messages sent to the queues during the lifespan of the 
			 *  QueueSession will not be delivered to the receivers until the commit() method is invoked on the QueueSession 
			 *  
			 *  Likewise, invoking the rollback() method on the QueueSession will remove any messages sent during the transacted session. 
			 *  
			 *  The second parameter indicated the acknowledgement mode. The 3 possible values are: 
			 *  	1. Session.AUTO_ACKNOWLEDGE 
			 *  	2. Session.CLIENT_ACKNOWLEDGE
			 *  	3. Session.DUPS_OK_ACKNOWLEDGE 
			 *  	
			 *  	The acknowledgement mode is ignored if the session is transacted. 
			 *  
			 *  	Perform a JNDI lookup to the JMS provider to obtain the JNDI name of the queues being used */
			requestQ = (Queue)context.lookup(requestQueue);
			responseQ = (Queue)context.lookup(responseQueue);
			
			//	Start the connection
			//	This allows messages to be received on this connection
			//	It is generally best practise to perform all of your initialization logic before starting the connection
			qConnect.start();
			
			/* 	You do need to start the conenction if all you are doing is to send messages 
			 * 	However, it is generally advisable to start the connection to avoid future issues if there is a chance that the connection may be 
			 * 	shared or request/reply processing added to the sender class 
			 * 
			 * 	Another useful thing that you can obtain from the JMS Connection is the metadata about the connection. 
			 * 	Invoking the getMetaData method on the Connection object gives you a ConnectionMetaData object that provides useful information: 
			 * 
			 * 		-	JMS version 
			 * 		-	JMS provider name 
			 * 		-	JMS provider version 
			 * 		-	JMSX property name extensions supported by the JMS provider */
			
		} catch (JMSException exc) {
			logger.error(exc);
			System.exit(1);
		} catch (NamingException exc) {
			logger.error(exc);			
			System.exit(1);
		}
	}
	
	private void sendLoanRequest(double salary, double loanAmount) {
		try {
			//	create JMS message. We chose to create a MapMessage but we could have used any of the JMS message types
			//	JMS message is created from the Session object, via a factory method matching the message type
			
			//	Instantiating a JMS object using new keyword will not work - it must be created from the Session object
			MapMessage mapMessage = qSession.createMapMessage();
			mapMessage.setDouble("Salary", salary);
			mapMessage.setDouble("Loan Amount", loanAmount);
			
			/*	After creating and loading the message object, we are also setting the JMSReplyTo header property to the response queue, which 
			 * 	further decouples the producer from the consumer */
			
			/*	The practise of setting the JMSReplyTo header property in the message producer as opposed to specifying the reply-to-queue in the 
			 * 	message consumer is a standard practise when using the request/reply model */
			mapMessage.setJMSReplyTo(responseQ);
			
			/* 	After the message is created, create the QueueSender object, specifying the queue we wish to send messages to and then send the 
			 * 	message using the send() method */
			QueueSender queueSender = qSession.createSender(requestQ);
			
			/*	There are several overridden send() methods available in the QueueSender object. 
			 * 	The one we are using here accepts only the JMS message object as the single argument 
			 * 	The other overridden methods allow you to specify:
			 * 		-	Queue
			 * 		-	Delivery mode
			 * 		-	Message priority 
			 * 		-	Message expiry 
			 * 
			 * 	Since we are not specifying any of the other values in the example shown, 
			 * 		-	Message priority is set to normal (4) 
			 * 		-	Delivery mode is set to persistent messages (DeliveryMode.PERSISTENT)
			 * 		-	Message expiry/time to live is set to 0 (zero) indicating that the message will never expire 
			 * 
			 * 	All these parameters can be overridden by using one of the other send() methods */	
			queueSender.send(mapMessage);
			
			/*	Once the message has been sent, the QueueBorrower class will block and wait for a response from the QLender on whether the loan was 	
			 * 	approved or denied. 
			 * 	The first step in this process is to set up a message selector so that we can correlate the response message with the one we sent. 
			 * 	This is necessary because there maybe many other loan requests being sent to and from the loan request queues while we are making our 
			 * 	loan request. 
			 * 	To make sure we get the proper response back, we use a technique called "message correlation".
			 * 	
			 * 	Message correlation is required when using the request/reply model of P2P messaging where the queue is being shared by multiple 
			 * 	producers and consumers. 
			 * 
			 * 	We specify the filter when creating the QueueReceiver indicating that we only want to receive messages when the JMSCorrelationID is 
			 * 	equal to the original JMSMessageID. 
			 * 	
			 * 	Message producer, QBorrower expects the response about whether or not the loan was approved by creating a message selector based on 
			 * 	the JMSCorrelationID message property * */
			
			/*	Although JMSMessageID is typically used to identify the unique message, it is certainly not a requirement.
			 * 	You can use anything that can correlate the request and reply messages. 
			 * 		
			 * 	For example, as an alternative you could use the Java UUID class to generate a unique ID 
			
				mapMessage.setDouble("Salary", salary);
				mapMessage.setDouble("Loan Amount", loanAmount);
				mapMessage.setJMSReplyTo(responseQ);
				
				UUID uuid = UUID.randomUUID();
				String uniqueId = uuid.toString();
				mapMessage.setStringProperty("UUID", uniqueId); */
			
			String filter = "JMSCorrelationID = '" + mapMessage.getJMSCorrelationID() + "'";
			
			//filter = "JMSCorrelationID = '" + uniqueId + "'";
			
			QueueReceiver queueReceiver = qSession.createReceiver(responseQ, filter);

			/*	Now that we have QueueReceiver, we can invoke the receive() method to do a blocking wait until the response message is received. 
			 * 	In this case, we are using the overridden receive() method that accepts a timeout value in milliseconds */
			TextMessage textMessage = (TextMessage)queueReceiver.receive(30000);
			
			/*	It is a good idea to always specify a reasonable timeout value on the receive method. Otherwise, it will sit there and wait forever. 
			 * 	In effect, the application would "hang".
			 * 
			 * 	Specifying a reasonable timeout value allows the request/reply sender (in this case, QBorrower) to take action in the event the 
			 * 	message has not been delivered in a timely fashion or there is a problem on the receiving side (in this case, QLender).
			 * 
			 * 	If a timeout condition does occur, the message returned from the receive method will be null. (It is the entire message object that 
			 * 	is null, not just the message payload).
			 * 
			 * 	The receive() method returns a Message object. If the message type is known, then you can cast the return the message as we did in the 
			 * 	preceding example. 
			 * 
			 * 	However, a more fail safe technique would be to check the return Message type using the instanceof operator/keyword as indicated here:	

				Message receiveMessage = queueReceiver.receive(30000);
				if(receiveMessage == null) {
					System.out.println("QLender not responding");
				} else {
					if(receiveMessage instanceof TextMessage) {
						TextMessage txtMsg = (TextMessage)receiveMessage;
						System.out.println("Loan request was " + txtMsg.getText());
					} else {
						throw new IllegalStateException("Invalid message type");
					}
				}

			*	Notice that the message received does not need to be of the same message type as the one sent. 
			*	In the example just shown we sent the loan request using a MapMessage, yet we received the response from the receiver as a TextMessage. 
			*	
			*	While you could potentially increase the level of decoupling between the sender and receiver by including the message type as part of 
			*	the application properties of the message, you would still need to know how to interrupt the payload in the message. 
			*
			*	For example, with a StreamMessage or BytesMessage you would still need to know the order of data being sent so that you could in turn 
			*	read it in the proper order and data type.
			*	
			*	As long as you can guess, because of the "contract" of the data between the sender and the receiver, there is still a fair amount 
			*	of coupling in the P2P model, at least from the payload perspective */
			
			if(textMessage == null) {
				logger.info("Receiver not responding. There is no response message to return to producer" );
			} else {
				logger.info("Loan request was " + textMessage.getText());
			}
		} catch (JMSException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	private void exit() {
		try {
			qConnect.close();
		} catch (JMSException exc) {		
			logger.error(exc);
		}
		System.exit(0);		
	}
	
	/*	Main method accepts 3 arguments from the command line: 
			1. The JNDI name of the queue connection factory
			2. The JNDI name of the loan request queue
			3. The JNDI name of the loan response queue where the response from the QLender class will be received */
	public static void main(String[] args) {
		
		String queueCF = null;
		String requestQ = null;
		String responseQ = null;
		
		//Validate the input parameters
		if(args.length == 3) {
			queueCF = args[0];
			requestQ = args[1];
			responseQ = args[2];
		} else {
			logger.info("Invalid arguments. Should be: ");
			logger.info("java QBorrower factory requestQueue responseQueue");
			System.exit(0);
		}
		
		//Instantiate and initialize QBorrower class' constructor 
		QBorrower qBorrower = new QBorrower(queueCF, requestQ, responseQ);
		
		try {
			//read all standard input and send it as a message
			BufferedReader standardInput = new BufferedReader(new InputStreamReader(System.in));
			logger.info("Message producer application started");
			logger.info("Press Enter to quit the application");
			logger.info("Enter: Salary, Loan Amount");
			logger.info("\ne.g. 50000, 120000");
			
			//Start a loop that reads the salary and loan amount into the class from the console
			while(true) {
				logger.info("> ");
				String loanRequest = standardInput.readLine();
				
				//Parse the deal description (salary and loan amount input data)
				if(loanRequest == null || loanRequest.trim().length() <= 0) {
					//The input loop continues until the user presses enter on the console without entering any data
					qBorrower.exit();					
				}
								
				StringTokenizer tokenizer = new StringTokenizer(loanRequest, ",");
				double salary = Double.valueOf(tokenizer.nextToken().trim()).doubleValue();
				double loanAmount = Double.valueOf(tokenizer.nextToken().trim()).doubleValue();
				
				//Invoke the sendLoanRequest() method to send the loan requests to the queue and wait for the response from the QLender class
				qBorrower.sendLoanRequest(salary, loanAmount);
			}
		} catch (IOException exc) {
			logger.error(exc);
		}
	}
}
