package com.jms.p2p;

import java.util.Enumeration;
import javax.jms.ConnectionMetaData;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.log4j.Logger;

//This information can logged on application startup, indicating the JMS provider and version numbers
//It is particularly useful for products or applications that may use multiple providers

public class MetaData {
	
	final static Logger logger = Logger.getLogger(MetaData.class);
	
	public static void main(String args[]) {
		try {
			Context context = new InitialContext();
			QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory)context.lookup("QueueCF");
			QueueConnection queueConnection = queueConnectionFactory.createQueueConnection();
			ConnectionMetaData connectionMetaData = queueConnection.getMetaData();
			
			logger.info("JMS Version\t:\t"  + connectionMetaData.getJMSMajorVersion() + "." + connectionMetaData.getJMSMinorVersion());
			logger.info("JMS Provider\t:\t" + connectionMetaData.getJMSProviderName());
			logger.info("JMSX Properties Supported: ");
			
			@SuppressWarnings("rawtypes")
			Enumeration enumeration = connectionMetaData.getJMSXPropertyNames();
			
			while(enumeration.hasMoreElements()) {
				logger.info("\t-" + enumeration.nextElement());
			}
			System.exit(1);
		} catch (Exception exc) {
			logger.error(exc);
			System.exit(1);
		}
	}
}
