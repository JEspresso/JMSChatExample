package com.jms.p2p;

import java.io.*;
import javax.jms.*;
import javax.naming.*;

public class QLender implements MessageListener{
	
	private QueueConnection qConnect = null;
	private QueueSession qSession = null;
	private Queue requestQ = null;
	
	public QLender(String queueCF, String requestQueue) {
		try {
			//connect to the provider and get the JMS connection
			Context context = new InitialContext();
			QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory)context.lookup(queueCF);
			qConnect = queueConnectionFactory.createQueueConnection();
			
			//create the JMS session
			qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			
			//lookup the request queue
			requestQ = (Queue)context.lookup(requestQueue);
			
			//now that the setup is complete, start the connection
			qConnect.start();
			
			//create the message listener
			QueueReceiver queueReceiver = qSession.createReceiver(requestQ);
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
		try {
			boolean accepted = false;
			
			//get the data from the message
			MapMessage mapMessage = (MapMessage)message;
			double salary = mapMessage.getDouble("Salary");
			double loanAmount =  mapMessage.getDouble("Loan Amount");
			
			//determine whether to accept or decline the loan request
			if(loanAmount < 200000) {
				accepted = (salary / loanAmount) > .25;
			} else {
				accepted = (salary / loanAmount) > .33;
			}
			System.out.println("" + "Percent = " + (salary / loanAmount) + ", loan is " + (accepted ? "Accepted!" : "Declined"));
			
			//send the result back to the borrower
			TextMessage textMessage = qSession.createTextMessage();
			textMessage.setText(accepted ? "Accepted!" : "Declined");
			textMessage.setJMSCorrelationID(mapMessage.getJMSCorrelationID());
			
			//create the sender and send the message
			QueueSender queueSender = qSession.createSender((Queue)mapMessage.getJMSReplyTo());
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
	
	private void exit() {
		try {
			qConnect.close();
		} catch (JMSException ex) {
			ex.printStackTrace();
		}
		System.exit(0);
	}
	
	public static void main(String args[]) {
		String queueCF = null;
		String requestQ = null;
		
		if(args.length == 2) {
			queueCF = args[0];
			requestQ = args[1];
		} else {
			System.out.println("Invalid arguments. Should be");
			System.out.println("java QLender factory request_queue");
			System.exit(0);			
		}
		
		QLender qLender = new QLender(queueCF, requestQ);
		
		try
		{
			//run until enter is pressed
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
