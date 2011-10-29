package com.upgradingdave.plugin;

/*
 * Copyright 2011 Dave Paroulek (upgradingdave.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

//import javabrowser.JavaBrowser;

/**
 * Goal that starts a Java Browser Server
 * 
 * @goal start
 * @requiresDependencyResolution runtime
 */
public class MyMojo extends AbstractMojo {

	/**
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
     * The plugin descriptor
     * 
     * @parameter default-value="${descriptor}"
     */
    private PluginDescriptor descriptor;
	
	public void execute() throws MojoExecutionException {
		try {
			getLog().debug("Attempting to start javabrowser");
			setRuntimeClasspath();
			moveWar();
			runWithJetty();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public static void main(String args[]) {
		try {
			MyMojo mojo = new MyMojo();
			mojo.execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void setRuntimeClasspath() {

		List<String> runtimeClasspathElements = null;
		try {
//			// this doesn't work because I can't figure out how to load plugin descriptor
//			this.descriptor = (PluginDescriptor) getPluginContext().get("descriptor");
//			if(descriptor == null){
//	        	getLog().error("maven is too complicated ;-)");
//	        }
//			List<String> runtimeClasspath = project.getRuntimeClasspathElements();
//			List<String> newPluginClasspath = new ArrayList<String>();
//            newPluginClasspath.addAll(runtimeClasspath);
            //newPluginClasspath.addAll(providedClasspath);
            //newPluginClasspath.addAll(generatedClasspath);

//            extendPluginClasspath(newPluginClasspath);
//			runtimeClasspathElements = project.getRuntimeClasspathElements();
//			ClassRealm realm = descriptor.getClassRealm();
//
//			for (String element : runtimeClasspathElements)
//			{
//			    File elementFile = new File(element);
//			    getLog().error("Found dependency: "+ elementFile.getName());
//			    Thread.currentThread().setContextClassLoader(realm.getClassLoader());
//			    //realm.addConstituent(elementFile.toURI().toURL());
//			}
			
			//here's an attempt to build a new classworld and inject the runtime jars programmatically
			//create a new classloading space
	        ClassWorld world = new ClassWorld();

	        //use the existing ContextClassLoader in a realm of the classloading space
	        ClassRealm realm = world.newRealm("plugin.jetty.container", Thread.currentThread().getContextClassLoader());
	         
	        //create another realm for just the jars we have downloaded on-the-fly and make
	        //sure it is in a child-parent relationship with the current ContextClassLoader
	        //ClassRealm runtimeRealm = realm.createChildRealm("runtime");
	        
			runtimeClasspathElements = project.getRuntimeClasspathElements();
			//URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
			for (int i = 0; i < runtimeClasspathElements.size(); i++) {
				String element = (String) runtimeClasspathElements.get(i);
				getLog().error("Found dependency: "+ element);
				//runtimeUrls[i] = new File(element).toURI().toURL();
				realm.addConstituent(new File(element).toURI().toURL());
			}

	       //make the child realm the ContextClassLoader
	       Thread.currentThread().setContextClassLoader(realm.getClassLoader());
			
			// URLClassLoader newLoader = new URLClassLoader(runtimeUrls,
			// Thread.currentThread().getContextClassLoader());
			//
			// Class bundle = newLoader.loadClass("package.MyClass");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Not sure how to get jetty to load a war from the classpath, so first move
	 * the war into target
	 */
	public void moveWar() {
		File newWar = new File("target/javabrowser.war");
		if (newWar.exists()) {
			newWar.delete();
		}

		InputStream in = this.getClass().getClassLoader()
				.getResourceAsStream("javabrowser.war");
		OutputStream out = null;
		try {
			out = new FileOutputStream(newWar);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void runWithJetty() throws Exception {

        int port = Integer.getInteger("jetty.port", 9000).intValue();
        Server server = new Server();

        Connector connector = new SelectChannelConnector();
        connector.setPort(port);
        server.setConnectors(new Connector[] { connector });

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar("target/javabrowser.war");
        webapp.setTempDirectory(new File("target/javabrowser-tmp"));
        // webapp.setDefaultsDescriptor(jetty_home+"/etc/webdefault.xml");

        server.setHandler(webapp);

        server.start();
        server.join();
        getLog().info("JavaBrowser Server started on port "+port+", have fun!");
    }
}
