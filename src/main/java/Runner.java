package main.java;

import java.io.File;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

import javabrowser.JavaBrowser;

public class Runner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Runner r = new Runner();
		try {
			r.runWithJetty();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//		JavaBrowser jb = new JavaBrowser();
		//		jb.startServer();
		//System.out.println(jb.simple());
	}
	
	public void runWithJetty() throws Exception{
//		 String jetty_default=new java.io.File("./start.jar").exists()?".":"../..";;
//	     String jetty_home = System.getProperty("jetty.home",jetty_default);
	     Server server = new Server();
	        
	        Connector connector=new SelectChannelConnector();
	        connector.setPort(Integer.getInteger("jetty.port",8080).intValue());
	        server.setConnectors(new Connector[]{connector});
	        
	        WebAppContext webapp = new WebAppContext();
	        webapp.setContextPath("/");
	        webapp.setWar("javabrowser-0.0.1.war");
	        webapp.setTempDirectory(new File("target/webapp-tmp"));	
	        //webapp.setDefaultsDescriptor(jetty_home+"/etc/webdefault.xml");
	        
	        server.setHandler(webapp);
	        
	        server.start();
	        server.join();
	}

}
