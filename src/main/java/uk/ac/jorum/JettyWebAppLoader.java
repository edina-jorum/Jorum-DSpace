/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : JettyWebAppLoader.java
 *  Author              : gwaller
 *  Approver            : Gareth Waller 
 * 
 *  Notes               :
 *
 *
 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 */
package uk.ac.jorum;

import java.io.File;
import org.mortbay.jetty.Server;
import org.mortbay.xml.XmlConfiguration;


public class JettyWebAppLoader
{
	public static void usage(){
		System.out.println("Usage: " + JettyWebAppLoader.class.getCanonicalName() + " <path to config file> <stop port> <stop key>");
		System.exit(1);
	}


  public static void main(String[] args)
          throws Exception
      {
          if (args.length != 3){
          	usage();
          }
          
          try{
        	  Server server = new Server();
        	  XmlConfiguration configuration = new XmlConfiguration(new File(args[0]).toURL()); 
        	  configuration.configure(server);
          
        	  server.start();
          
        	  JettyStopListener monitor = new JettyStopListener(Integer.parseInt(args[1]), args[2], server, true);
        	  monitor.start();
          
        	  server.join();
          } catch (Exception e){
        	  e.printStackTrace();
        	  System.exit(1);
          }
      }
  
    
}