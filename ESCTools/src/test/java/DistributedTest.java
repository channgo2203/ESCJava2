import java.util.Iterator;

import junit.framework.*;
import junitutils.*;

public class DistributedTest extends junitutils.TestFilesTestSuite {

	static public junit.framework.Test suite() {

	String hostName = "";
	try
	{
		java.net.InetAddress localMachine =
			java.net.InetAddress.getLocalHost();	
		hostName = localMachine.getHostName();
	}
	catch(java.net.UnknownHostException uhe)
	{
		throw new RuntimeException(uhe.toString());
	}    
	
	String defaultSettings = "-nowarn Deadlock -verboseTrace -testMode -Loop5";
	String listOfFiles = "src/test/list.txt";
	String listOfServers = "src/test/servers.txt";
	String testSuiteName = "Distributed-Test-" + hostName;
	int numberOfServers = 0;
	int serverIndex = -1; // run no tests unless a server is found 
    	
    	try { 
    	    Iterator i = new LineIterator(listOfServers);
    	    while (i.hasNext()) {
    	    	
    	    	String s = (String)i.next();
    	    	if (s.equalsIgnoreCase(hostName)) {
    	    		serverIndex = numberOfServers;
    	    	}
    	    	numberOfServers++;
    	    }
    	} catch (java.io.IOException e) {
    	    throw new RuntimeException(e.toString());
    	}

	 

        junit.framework.TestSuite suite =
	    new AbstractDistributedTestSuite(
		testSuiteName,
		listOfFiles,
		defaultSettings,
		escjava.Main.class,
		serverIndex,
		numberOfServers) {
		    public int expectedIntegerStatus(String f, String o) {
			if (javafe.util.ErrorSet.errors > 0) return 2;

			return 0;
		    }
                    public String expectedStatusReport(String fileToTest,
                        int ecode, String expectedOutput) {
                        int ret = expectedIntegerStatus(fileToTest,expectedOutput);
                        if ((ecode > 0) == (ret > 0)) return null;
                        return super.expectedStatusReport(fileToTest,ecode,expectedOutput);
                    }
		};
	return suite;
    }
}
