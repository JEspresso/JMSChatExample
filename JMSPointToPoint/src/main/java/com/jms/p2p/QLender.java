package com.jms.p2p;

import java.io.*;
import javax.jms.*;
import javax.naming.*;

/*	The role of the QLender class is to listen for loan requests on the loan request queue, determine if the salary meets the necessary business 
 * 	requirements, and finally send back the results to the borrower. 
 * 
 * 	In this example, QLender class is referred to as a "message listener" and, as such, implements the javax.jms.MessageListener interface and 
 * 	overrides the onMessage() method. 
 * 
 * 	QLender class is an asynchronous message listener, meaning that unlike the QBorrower it will not block when waiting for messages. 
 * 
 * 	This is evident from the fact the QLender class implements the MessageListener interface and then overrides the onMessage() method. 
 * */

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
				 * 	incoming response messages */
				
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
