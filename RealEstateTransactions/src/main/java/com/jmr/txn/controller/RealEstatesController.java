package com.jmr.txn.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * TODO Put here a description of what this class does.
 * Class RealEstatesController is implemented with @RestController and @RequestMapping annotations which is the  
 * standard way of defining REST endpoints
 * @author Kevin.
 *         Created Jan 16, 2018.
 */

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.jmr.txn.bean.RealEstatesBean;
import com.opencsv.CSVReader;

@RestController
public class RealEstatesController {

	/**
	 * TODO Put here a description of what this method does.
	 * Method createRealEstateTxnList() to expose CSV data via a REST end point in JSON format
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	@RequestMapping(value = "/realEstateTxns", method = RequestMethod.GET, headers = "Accept=application/json")
	public List createRealEstateTxnList() throws Exception {
		
		List realEstateTxnList = new ArrayList();		
		realEstateTxnList = getList();
		
		return realEstateTxnList;
	}

	/**
	 * TODO Put here a description of what this method does.
	 * Method getList() locates the CSV file, parses it and returns file content as an ArrayList in
	 * JSON format. It is made static because we do not an instance of the controller class RealEstatesController 
	 * so as to invoke it
	 * @return
	 * @throws IOException 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ArrayList<RealEstatesBean> getList() throws IOException {

		//Create an ArrayList to hold the CSV data
		ArrayList<RealEstatesBean> realEstateTxnList = new ArrayList();

		//Read the CSV file - added in the resources directory as well as the build path
		InputStream inputStream = RealEstatesController.class.getClassLoader().getResourceAsStream("realestatetransactions.csv");

		//Reference to CSVReader 
		try (CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream))) {

			String[] nextLine;
			csvReader.readNext();

			while ((nextLine = csvReader.readNext()) != null) {
				RealEstatesBean realEstatesBean = new RealEstatesBean(nextLine[0], nextLine[1], Integer.valueOf(nextLine[2]),
						nextLine[3], nextLine[4], nextLine[5], Integer.valueOf(nextLine[6]), nextLine[7], nextLine[8],
						Double.valueOf(nextLine[9]), nextLine[10], nextLine[11]);
				realEstateTxnList.add(realEstatesBean);
			}
		}
		return realEstateTxnList;
	}
}
