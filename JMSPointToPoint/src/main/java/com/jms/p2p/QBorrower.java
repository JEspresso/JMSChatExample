package com.jms.p2p;

import java.io.*;
import java.util.StringTokenizer;
import javax.jms.*;
import javax.naming.*;

public class QBorrower {

	private QueueConnection qConnect = null;
	private QueueSession qSession = null;
	private Queue responseQ = null;
	private Queue requestQ = null;
	
	public QBorrower(String queueCF, String requestQueue, String responseQueue) {
		try {
			//connect to the provider and get the JMS connection
			Context context = new InitialContext();
			QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory)context.lookup(queueCF);
			qConnect = queueConnectionFactory.createQueueConnection();
			
			//create the JMS session
			qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			
			//lookup the request and response queues
			requestQ = (Queue)context.lookup(requestQueue);
			responseQ = (Queue)context.lookup(responseQueue);
			
			//now that the setup is complete, start the connection
			qConnect.start();
		} catch (JMSException ex) {
			ex.printStackTrace();
			System.exit(1);
		} catch (NamingException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	private void sendLoanRequest(double salary, double loanAmount) {
		try {
			//create JMS message
			MapMessage mapMessage = qSession.createMapMessage();
			mapMessage.setDouble("Salary", salary);
			mapMessage.setDouble("Loan Amount", loanAmount);
			mapMessage.setJMSReplyTo(responseQ);
			
			//create the sender and send the message
			QueueSender queueSender = qSession.createSender(requestQ);
			queueSender.send(mapMessage);
			
			//wait to see if the loan request was accepted or denied
			String filter = "JMSCorrelationID = '" + mapMessage.getJMSCorrelationID() + "'";
			QueueReceiver queueReceiver = qSession.createReceiver(responseQ, filter);
			TextMessage textMessage = (TextMessage)queueReceiver.receive(30000);
			
			if(textMessage == null) {
				System.out.println("QLender not responding");
			} else {
				System.out.println("Loan request was " + textMessage.getText());
			}
		} catch (JMSException ex) {
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
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String queueCF = null;
		String requestQ = null;
		String responseQ = null;
		
		if(args.length == 3) {
			queueCF = args[0];
			requestQ = args[1];
			responseQ = args[2];
		} else {
			System.out.println("Invalid arguments. Should be: ");
			System.out.println("java QBorrower factory requestQueue responseQueue");
			System.exit(0);
		}
		
		QBorrower qBorrower = new QBorrower(queueCF, requestQ, responseQ);
		
		try {
			//read all standard input and send it as a message
			BufferedReader standardInput = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("QBorrower application started");
			System.out.println("Press Enter to quit the application");
			System.out.println("Enter: Salary, Loan Amount");
			System.out.println("\ne.g. 50000, 120000");
			
			while(true) {
				System.out.println("> ");
				String loanRequest = standardInput.readLine();
				
				if(loanRequest == null || loanRequest.trim().length() <= 0) {
					qBorrower.exit();					
				}
				
				//pass the deal description
				StringTokenizer tokenizer = new StringTokenizer(loanRequest, ",");
				double salary = Double.valueOf(tokenizer.nextToken().trim()).doubleValue();
				double loanAmount = Double.valueOf(tokenizer.nextToken().trim()).doubleValue();
				qBorrower.sendLoanRequest(salary, loanAmount);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

}
