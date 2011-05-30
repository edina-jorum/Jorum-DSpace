/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : JettyKiller.java
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

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * @author gwaller
 *
 */
public class JettyKiller {

	public static void usage(){
		System.out.println("Usage: " + JettyKiller.class.getCanonicalName() + " <stop port> <stop key>");
		System.exit(1);
	}


  public static void main(String[] args)
          throws Exception
      {
          if (args.length != 2){
          	usage();
          }
          
         stop(Integer.parseInt(args[0]), args[1]);
         
      }
	
	
	/**
     * Stop a running jetty instance.
     */
    public static void stop(int port, String key)
    {
        try
        {
            Socket s=new Socket(InetAddress.getByName("127.0.0.1"),port);
            OutputStream out=s.getOutputStream();
            out.write((key+"\r\nstop\r\n").getBytes());
            out.flush();
            s.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }
	
}
