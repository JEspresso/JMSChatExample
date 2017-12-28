package com.jms.p2p;

import java.util.Enumeration;
import javax.jms.ConnectionMetaData;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;

//This information can logged on application startup, indicating the JMS provider and version numbers
//It is particularly useful for products or applications that may use multiple providers

public class MetaData {
	
	public static void main(String args[]) {
		try {
			Context context = new InitialContext();
			QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory)context.lookup("QueueCF");
			QueueConnection queueConnection = queueConnectionFactory.createQueueConnection();
			ConnectionMetaData connectionMetaData = queueConnection.getMetaData();
			
			System.out.println("JMS Version: "  + connectionMetaData.getJMSMajorVersion() + "." + connectionMetaData.getJMSMinorVersion());
			System.out.println("JMS Provider: " + connectionMetaData.getJMSProviderName());
			System.out.println("JMSX Properties Supported: ");
			
			@SuppressWarnings("rawtypes")
			Enumeration enumeration = connectionMetaData.getJMSXPropertyNames();
			
			while(enumeration.hasMoreElements()) {
				System.out.println("\t-" + enumeration.nextElement());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
}
