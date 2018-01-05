package com.jms.p2p;

import java.util.Enumeration;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.log4j.Logger;


/**
 * @author Kevin
 *	LoanRequestQueueBrowser to browse LoanResponseQ
 */
public class LoanRequestQueueBrowser {
	
	final static Logger logger = Logger.getLogger(LoanRequestQueueBrowser.class);
	
	public static void main(String args[]) {
		try {
			//	Establish a connection
			Context context = new InitialContext();
			QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory)context.lookup("QueueCF");
			QueueConnection queueConnection = queueConnectionFactory.createQueueConnection();
			queueConnection.start();
			
			//	Establish session
			Queue queue = (Queue)context.lookup("LoanResponseQ");
			QueueSession queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			QueueBrowser queueBrowser = queueSession.createBrowser(queue);
			
			@SuppressWarnings("rawtypes")
			Enumeration enumeration = queueBrowser.getEnumeration();
			
			while(enumeration.hasMoreElements()) {
				TextMessage textMessage = (TextMessage)enumeration.nextElement();
				logger.info("Browsing:\t" + textMessage.getText().toString());
			}
			queueBrowser.close();
			queueConnection.close();
			System.exit(0);
		} catch (Exception exc) {
			logger.error(exc);			
		}
	}
}

/* 	===================================================================================================================================================
* 	Examining a Queue
* 	===================================================================================================================================================
* 	A QueueBrowser is a specialized object that allow you to peek ahead at pending messages on a Queue without actually consuming them. 
* 	
* 	This feature is unique to P2P messaging. 
* 	
* 	Queue browsing can be useful for monitoring the contents of a queue from an administration tool or for browsing through multiple messages to 
* 	locate a message that is more important than the one at the head of the queue. 	
* 	
* 	It is useful for monitoring tasks, such as determining the current queue depth. 
* 	
* 	Messages obtained from a QueueBrowser are copies of messages contained in the queue and are not considered to be consumed - they are merely for 
* 	browsing. 
* 	
* 	QueueBrowser is not guaranteed to have a definitive list of messages in the queue. 
* 	
* 	QueueBrowser contains only a snapshot/copy of the queue as it appears at the time the QueueBrowser is creaed. 
* 	
* 	The contents of the queue may change between the time the browser is created and the time you examine its contents. However, no matter how small 
* 	that window of time is, new messages may arrive and other messages may be consumed by other JMS clients. 
* 	
* 	A QueueBrowser is created from the Session object using the createBrowser() method. 
* 	
* 	This method takes as an argument the queue from which you would like to view the messages. 
* 	
* 	It is during the createBrowser() method call that the snapshot is taken from the queue. 
* 	
* 	You can then get a list of the messages by using the method getEnumeration() from the QueueBrowser. 
* 	
* 	QueueBrowser queueBrowser = queueSession.createBrowser(queue);
* 	Enumeration enumeration = queueBrowser.getEnumeration();
* 	while (enumeration.hasElements()){
* 		//display messages
* 	} */
