
 /**
  * This template file was generated by dynaTrace client.
  * The dynaTrace community portal can be found here: http://community.dynatrace.com/
  * For information how to publish a plugin please visit http://community.dynatrace.com/plugins/contribute/
  **/ 

package com.dynatrace.license.count.monitor;

import com.dynatrace.diagnostics.pdk.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.net.ssl.*;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.client.ClientProtocolException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class counter implements Monitor {

	private final Logger log = Logger.getLogger(counter.class.getName());

	// measure constants
	private final String METRIC_GROUP = "Agent Count";
	private final String MSR_DOT_NET = "dotNet Hosts";
	private final String MSR_DOT_NET_INDIVIDUAL = "dotNet Agents";
	private final String MSR_JAVA = "Java Agents";
	private final String MSR_WEB_SERVER = "Web Server Agents";
	private final String MSR_IIS_WS = "Web Server Hosts_IIS";
	private final String MSR_IIS_WS_INDIVIDUAL = "Web Server Agents_IIS";
	private final String MSR_APACHE_WS = "Web Server Hosts_Apache";
	private final String MSR_APACHE_WS_INDIVIDUAL = "Web Server Agents_Apache";
	private final String MSR_MESSAGE_BROKER = "Message Broker Agents";
	private final String MSR_NATIVE = "Native Agents";
	private final String MSR_UNLICENSED = "Unlicensed Agents";
	private final String MSR_LICENSED = "Licensed Agents";
	private final String MSR_PHP = "PHP Agents";
	private final String MSR_CICS = "CICS Agents";
	private final String MSR_DB = "Database Agents";
	private final String MSR_NODEJS = "NodeJS Agents";
	
	private Collection<MonitorMeasure>  measures  = null;
	private MonitorMeasure dynamicMeasure;
	
	private NodeList allnodes;
	
	private double java;
	private double web;
	private double mb;
	private double dotnetindividual;
	private double web_iis_individual;
	private double web_apache_individual;
	private double nativeagent;
	private double unlicensedagent;
	private double licensedagent;
	private double phpagent;
	private double cicsagent;
	private double dbagent;
	private double nodejsagent;
	private List<Node> dotnetList;
	private List<Node> web_iisList;
	private List<Node> web_apacheList;
	
	private URLConnection connection;
	private URL overviewurl;
	
	private String urlprotocol;
	private int urlport;
	private String restURL;
	private String username;
	private String password;
	
	private String profileName;
	private String[] arrayprofileName;
	private String agentGroup;
	
	private String hostName;
	private String[] arrayhostName;
	
	private String collectorName;
	private String[] arraycollectorName;
	
	private String split;
	private String xmlPath;
	
	@Override
	public Status setup(MonitorEnvironment env) throws Exception {
		
		log.finer("*****BEGIN PLUGIN LOGGING*****");
		log.finer("Entering setup method");
		log.finer("Entering variables from plugin.xml");
		
		urlprotocol = env.getConfigString("protocol");
		urlport = env.getConfigLong("httpPort").intValue();
		
		restURL = "/rest/management/agents";
		
		username = env.getConfigString("username");
		password = env.getConfigPassword("password");
		
		profileName = env.getConfigString("systemProfileName");
		arrayprofileName = env.getConfigString("systemProfileName").split(";");
		agentGroup = String.valueOf(env.getConfigBoolean("agentGroup"));
		
		collectorName = env.getConfigString("collectorName");
		arraycollectorName = env.getConfigString("collectorName").split(";");
		
		hostName = env.getConfigString("hostName");
		arrayhostName = env.getConfigString("hostName").split(";");
		
		split = env.getConfigString("systemSplit");
		
		xmlPath = "/agents/agentinformation";
		
		log.finer("URL Protocol: " + urlprotocol);
		log.finer("URL Port: " + urlport);
		log.finer("REST URL: " + restURL);
		log.finer("Username: " + username);
		log.finer("Profile Name: " + profileName);
		log.finer("Agent Group: " + agentGroup);
		log.finer("Collector Name: " + collectorName);
		log.finer("Host Name: " + hostName);
		log.finer("Split: " + split);
		log.finer("xmlPath: " + xmlPath);
				
		log.finer("Exiting setup method");
			
		return new Status(Status.StatusCode.Success);
	}

	
	@Override
	public Status execute(MonitorEnvironment env) throws MalformedURLException {
		
		log.finer("Entering execute method");
		
		log.finer("Entering URL Setup");
		overviewurl = new URL(urlprotocol, env.getHost().getAddress(), urlport, restURL);		
		
		log.info("Executing URL: " + overviewurl.toString());
		log.finer("Executing URL: " + overviewurl.toString());
		
		try {
			
			log.finer("Entering username/password setup");
			String userpass = username + ":" + password;
			String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
		
			disableCertificateValidation();
				
			//URL to grab XML file
			log.finer("Entering XML file grab");
			connection = overviewurl.openConnection();
			connection.setRequestProperty("Authorization", basicAuth);
			connection.setConnectTimeout(50000);

			InputStream responseIS = connection.getInputStream();	
			DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = xmlFactory.newDocumentBuilder();
			Document xmlDoc = docBuilder.parse(responseIS);
			
			xmlDoc.getDocumentElement().normalize();
			allnodes = xmlDoc.getElementsByTagName("agentinformation");
			log.finer("Number of Nodes: " + allnodes.getLength());
			
			//checks for splitting options
			switch (split) {
				case "System Profile": //"System Profile"
					log.finer("Entering split by System Profile");
					resetCounts();
					
					//used to store unique profile names
					Set<String> uniqueProfile = new HashSet<String>();
					
					for (int i = 0; i < allnodes.getLength(); i++) {						
						Element element = (Element) allnodes.item(i);
						uniqueProfile.add(element.getElementsByTagName("systemProfile").item(0).getTextContent());
					}
					
					log.finer("Number of Unique System Profiles: " + uniqueProfile.size());
					String[] arrayProfile = uniqueProfile.toArray(new String[0]);
					
					if (arrayprofileName.length==1) { //either none or one filter
						if(arrayprofileName[0].equals("")) { //uses no filter
							//if they want all System Profiles
							log.finer("Entering split by all System Profiles");
							for (int i = 0; i < uniqueProfile.size(); ++i) {
								log.finer("Splitting for System Profile: " + arrayProfile[i]);
								String tempProfileName = arrayProfile[i];
								resetCounts();
								
								if(agentGroup.equals("true")) {
									for (int j = 0; j < uniqueProfile.size(); ++j) {
										//used to store unique agent group names
										Set<String> uniqueAgentGroup = new HashSet<String>();
										
										for (int k = 0; k < allnodes.getLength(); ++k) {
											Element element = (Element) allnodes.item(k);
											if (element.getElementsByTagName("systemProfile").item(0).getTextContent().equalsIgnoreCase(tempProfileName))
												uniqueAgentGroup.add(element.getElementsByTagName("agentGroup").item(0).getTextContent());
										}
										
										log.finer("Number of Unique Agent Groups: " + uniqueAgentGroup.size());
										String[] arrayAgentGroup = uniqueAgentGroup.toArray(new String[0]);
										
										for (int l = 0;l < arrayAgentGroup.length;l++) {
											log.finer("Splitting for Agent Group: " + arrayAgentGroup[l]);
											String tempAgentGroupName = arrayAgentGroup[l];
											resetCounts();
											
											analyzeAllNodesDouble("systemProfile", tempProfileName, "agentGroup", tempAgentGroupName);
											collectDynamicMetrics(env, "System Profile/Agent Group Name", tempProfileName + "/" + tempAgentGroupName);
										}
									}
								}
								else {
									analyzeAllNodes("systemProfile", tempProfileName);
									collectDynamicMetrics(env, "System Profile Name", tempProfileName);
								}
							}
						}
						else { //uses one filter
							//if they want a specific System Profile
							log.finer("Entering split by one System Profile");
							log.finer("Splitting for System Profile: " + profileName);
							if(agentGroup.equals("true")) {
								resetCounts();
								
								//used to store unique agent group names
								Set<String> uniqueAgentGroup = new HashSet<String>();
								
								for (int k = 0; k < allnodes.getLength(); ++k) {
									Element element = (Element) allnodes.item(k);
									if (element.getElementsByTagName("systemProfile").item(0).getTextContent().equalsIgnoreCase(profileName))
										uniqueAgentGroup.add(element.getElementsByTagName("agentGroup").item(0).getTextContent());
								}
								
								log.finer("Number of Unique Agent Groups: " + uniqueAgentGroup.size());
								String[] arrayAgentGroup = uniqueAgentGroup.toArray(new String[0]);
								
								for (int l = 0;l < arrayAgentGroup.length;l++) {
									log.finer("Splitting for Agent Group: " + arrayAgentGroup[l]);
									String tempAgentGroupName = arrayAgentGroup[l];
									resetCounts();
									
									analyzeAllNodesDouble("systemProfile", profileName, "agentGroup", tempAgentGroupName);
									collectDynamicMetrics(env, "System Profile/Agent Group Name", profileName + "/" + tempAgentGroupName);
								}
							}
							else {
								resetCounts();
								
								analyzeAllNodes("systemProfile", profileName);
								collectDynamicMetrics(env, "System Profile Name", profileName);
							}
						}
					}
					else { //uses multiple filters
						//if they want multiple System Profiles
						log.finer("Entering split by multiple System Profiles");
						
						for (int i = 0; i < arrayprofileName.length; ++i) {
							log.finer("Splitting for System Profile: " + arrayprofileName[i]);
							String tempProfileName = arrayprofileName[i];
							
							if(agentGroup.equals("true")) {
								resetCounts();
								
								//used to store unique agent group names
								Set<String> uniqueAgentGroup = new HashSet<String>();
								
								for (int k = 0; k < allnodes.getLength(); ++k) {
									Element element = (Element) allnodes.item(k);
									if (element.getElementsByTagName("systemProfile").item(0).getTextContent().equalsIgnoreCase(tempProfileName))
										uniqueAgentGroup.add(element.getElementsByTagName("agentGroup").item(0).getTextContent());
								}
								
								log.finer("Number of Unique Agent Groups: " + uniqueAgentGroup.size());
								String[] arrayAgentGroup = uniqueAgentGroup.toArray(new String[0]);
								
								for (int l = 0;l < arrayAgentGroup.length;l++) {
									log.finer("Splitting for Agent Group: " + arrayAgentGroup[l]);
									String tempAgentGroupName = arrayAgentGroup[l];
									resetCounts();
									
									analyzeAllNodesDouble("systemProfile", tempProfileName, "agentGroup", tempAgentGroupName);
									collectDynamicMetrics(env, "System Profile/Agent Group Name", tempProfileName + "/" + tempAgentGroupName);
								}
							}
							else {
								resetCounts();
								
								analyzeAllNodes("systemProfile", tempProfileName);
								collectDynamicMetrics(env, "System Profile Name", tempProfileName);
							}
						}
					}
					break;
				case "Host": //"Host"
					log.finer("Entering split by Host");
					resetCounts();
					
					//used to store unique host names
					Set<String> uniqueHost = new HashSet<String>();
					
					for (int i = 0; i < allnodes.getLength(); i++) {						
						Element element = (Element) allnodes.item(i);
						uniqueHost.add(element.getElementsByTagName("host").item(0).getTextContent());
					}
					
					log.finer("Number of Unique Hosts: " + uniqueHost.size());
					String[] arrayHost = uniqueHost.toArray(new String[0]);
					
					if (arrayhostName.length==1) { //either none or one filter
						if(arrayhostName[0].equals("")) { //uses no filter
							//if they want all Hosts
							log.finer("Entering split by all Hosts");
							for (int i = 0; i < uniqueHost.size(); ++i) {
								log.finer("Splitting for Host: " + arrayHost[i]);
								String tempHostName = arrayHost[i];
								resetCounts();
								
								analyzeAllNodes("host", tempHostName);
								collectDynamicMetrics(env, "Host Name", tempHostName);
							}
						}
						else { //uses one filter 
							//if they want a specific Host
							log.finer("Entering split by one Host");
							log.finer("Splitting for Host: " + hostName);
							resetCounts();
							
							analyzeAllNodes("host", hostName);
							collectDynamicMetrics(env, "Host Name", hostName);
						}
					}
					else { //uses multiple filters
						//if they want multiple Hosts
						log.finer("Entering split by multiple Hosts");
						for (int i = 0; i < arrayhostName.length; ++i) {
							log.finer("Splitting for Host: " + arrayhostName[i]);
							String tempHostName = arrayhostName[i];
							resetCounts();
							
							analyzeAllNodes("host", tempHostName);
							collectDynamicMetrics(env, "Host Name", tempHostName);
						}
					}
					break;
				case "Collector": //"Collector"
					log.finer("Entering split by Collector");
					resetCounts();
					
					//used to store unique collector names
					Set<String> uniqueCollector = new HashSet<String>();
					
					for (int i = 0; i < allnodes.getLength(); i++) {						
						Element element = (Element) allnodes.item(i);
						uniqueCollector.add(element.getElementsByTagName("collectorName").item(0).getTextContent());
					}
					
					log.finer("Number of Unique Collectors: " + uniqueCollector.size());
					String[] arrayCollector = uniqueCollector.toArray(new String[0]);
					
					if (arraycollectorName.length==1) { //either none or one filter
						if(arraycollectorName[0].equals("")) { //uses no filter
							//if they want all Collectors
							log.finer("Entering split by all Collectors");
							for (int i = 0; i < uniqueCollector.size(); ++i) {
								log.finer("Splitting for Collector: " + arrayCollector[i]);
								String tempCollectorName = arrayCollector[i];
								resetCounts();
								
								analyzeAllNodes("collectorName", tempCollectorName);
								collectDynamicMetrics(env, "Collector Name", tempCollectorName);
							}
						}
						else { //uses one filter 
							//if they want a specific Collector
							log.finer("Entering split by one Collector");
							log.finer("Splitting for Collector: " + collectorName);
							resetCounts();
							
							analyzeAllNodes("collectorName", collectorName);
							collectDynamicMetrics(env, "Collector Name", collectorName);
						}
					}
					else { //uses multiple filters
						//if they want multiple Hosts
						log.finer("Entering split by multiple Collectors");
						
						for (int i = 0; i < arraycollectorName.length; ++i) {
							log.finer("Splitting for Collector: " + arraycollectorName[i]);
							String tempCollectorName = arraycollectorName[i];
							resetCounts();
							
							analyzeAllNodes("collectorName", tempCollectorName);
							collectDynamicMetrics(env, "Collector Name", tempCollectorName);
						}
					}
					break;
				case "No Splitting": //"No Splitting"
					log.finer("Entering split by No Splitting");
					resetCounts();
					
					for (int i = 0; i < allnodes.getLength(); i++) {						
						Element element = (Element) allnodes.item(i);
						analyzeElement(element, i);
					}
					
					collectStaticMetrics(env);
					break;
				case "Agent Version": //"Agent Version"
					log.finer("Entering split by Agent Version");
					resetCounts();
					
					//used to store unique version numbers
					Set<String> uniqueVersion = new HashSet<String>();
					
					for (int i = 0; i < allnodes.getLength(); i++) {						
						Element oneelement = (Element) allnodes.item(i);
						NodeList onenode = oneelement.getElementsByTagName("agentProperties");
						Element element = (Element) onenode.item(0);
						uniqueVersion.add(element.getElementsByTagName("agentVersion").item(0).getTextContent());
					}
					
					log.finer("Number of Unique Agent Versions: " + uniqueVersion.size());
					String[] arrayVersion = uniqueVersion.toArray(new String[0]);
					
					//if they want all Agent Versions
					log.finer("Entering split by all Agent Versions");
					for (int i = 0; i < uniqueVersion.size(); ++i) {
						log.finer("Splitting for Agent Version: " + arrayVersion[i]);
						String tempVersionName = arrayVersion[i];
						resetCounts();
						
						for (int j = 0; j < allnodes.getLength(); j++) {						
							Element oneelement = (Element) allnodes.item(j);
							NodeList onenode = oneelement.getElementsByTagName("agentProperties");
							Element element = (Element) onenode.item(0);
							
							if (element.getElementsByTagName("agentVersion").item(0).getTextContent().equalsIgnoreCase(tempVersionName)) {
								analyzeElement(oneelement, j);
							}
						}
						
						collectDynamicMetrics(env, "Version Number", tempVersionName);
					}
					break;
			}
		} catch (ClientProtocolException e) {
			log.info("ClientProtocolException: " + e);
			return new Status(Status.StatusCode.ErrorInternal);

		} catch (IOException e) {
			log.info("IOException: " + e);
			return new Status(Status.StatusCode.ErrorInternal);

		} catch (Exception e) {
			log.info("Exception: " + e);
			return new Status(Status.StatusCode.ErrorInternal);
		}
		
		log.finer("Exiting execute method");
		log.finer("*****END PLUGIN LOGGING*****");
		
		return new Status(Status.StatusCode.Success);
	}
	
	private void collectStaticMetrics(MonitorEnvironment env) {
		log.finer("Entering collectStaticMetrics method");
		
		//used to store unique .NET server names
		Set<String> uniqueNet = new HashSet<String>();
		Set<String> uniqueIIS = new HashSet<String>();
		Set<String> uniqueApache = new HashSet<String>();
		
		for (int j = 0; j < dotnetList.size(); ++j) {
			Element element = (Element) dotnetList.get(j);
			uniqueNet.add(element.getElementsByTagName("agentHost").item(0).getTextContent());
		}

		for (int k = 0; k < web_iisList.size(); ++k) {
			Element element = (Element) web_iisList.get(k);
			uniqueIIS.add(element.getElementsByTagName("agentHost").item(0).getTextContent());
		}

		for (int l = 0; l < web_apacheList.size(); ++l) {
			Element element = (Element) web_apacheList.get(l);
			uniqueApache.add(element.getElementsByTagName("agentHost").item(0).getTextContent());
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_JAVA)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue(java);
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_WEB_SERVER)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue(web);
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_MESSAGE_BROKER)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue(mb);
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_NATIVE)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue(nativeagent);
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_UNLICENSED)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue(unlicensedagent);
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_LICENSED)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue(licensedagent);
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_PHP)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue(phpagent);
		}
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_CICS)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue(cicsagent);
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_DB)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue(dbagent);
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_NODEJS)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue(nodejsagent);
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_DOT_NET_INDIVIDUAL)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue(dotnetindividual);
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_DOT_NET)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue((double)uniqueNet.size());
		}

		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_IIS_WS_INDIVIDUAL)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue(web_iis_individual);
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_IIS_WS)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue((double)uniqueIIS.size());
		}

		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_APACHE_WS_INDIVIDUAL)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue(web_apache_individual);
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_APACHE_WS)) != null) {
			for (MonitorMeasure measure : measures)
				measure.setValue((double)uniqueApache.size());
		}
		
		log.finer("Exiting collectStaticMetrics method");
	}

	private void collectDynamicMetrics(MonitorEnvironment env, String tempSplitString, String tempSplitName) {
		log.finer("Entering collectDynamicMetrics method");
		
		//used to store unique .NET server names
		Set<String> uniqueNet = new HashSet<String>();
		Set<String> uniqueIIS = new HashSet<String>();
		Set<String> uniqueApache = new HashSet<String>();
		
		for (int m = 0; m < dotnetList.size(); ++m) {
			Element element = (Element) dotnetList.get(m);
			uniqueNet.add(element.getElementsByTagName("agentHost").item(0).getTextContent());
		}

		for (int n = 0; n < web_iisList.size(); ++n) {
			Element element = (Element) web_iisList.get(n);
			uniqueIIS.add(element.getElementsByTagName("agentHost").item(0).getTextContent());
		}

		for (int p = 0; p < web_apacheList.size(); ++p) {
			Element element = (Element) web_apacheList.get(p);
			uniqueApache.add(element.getElementsByTagName("agentHost").item(0).getTextContent());
		}
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_JAVA)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
			    dynamicMeasure.setValue(java);
			}								
		}	
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_WEB_SERVER)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
			    dynamicMeasure.setValue(web);
			}								
		}	
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_MESSAGE_BROKER)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
			    dynamicMeasure.setValue(mb);
			}								
		}	
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_NATIVE)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
			    dynamicMeasure.setValue(nativeagent);
			}								
		}	
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_UNLICENSED)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
			    dynamicMeasure.setValue(unlicensedagent);
			}								
		}		
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_LICENSED)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
			    dynamicMeasure.setValue(licensedagent);
			}								
		}	
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_PHP)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
			    dynamicMeasure.setValue(phpagent);
			}								
		}		
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_DB)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
			    dynamicMeasure.setValue(dbagent);
			}								
		}		
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_NODEJS)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
			    dynamicMeasure.setValue(nodejsagent);
			}								
		}			
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_CICS)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
			    dynamicMeasure.setValue(cicsagent);
			}								
		}		
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_DOT_NET_INDIVIDUAL)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
			    dynamicMeasure.setValue(dotnetindividual);
			}								
		}	
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_DOT_NET)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
		    	dynamicMeasure.setValue((double)uniqueNet.size());
			}
		}		
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_IIS_WS_INDIVIDUAL)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
			    dynamicMeasure.setValue(web_iis_individual);
			}								
		}	
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_IIS_WS)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
		    	dynamicMeasure.setValue((double)uniqueIIS.size());
			}
		}		
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_APACHE_WS_INDIVIDUAL)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
			    dynamicMeasure.setValue(web_apache_individual);
			}								
		}	
		
		if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_APACHE_WS)) != null) {
			for (MonitorMeasure measure : measures){
				dynamicMeasure = env.createDynamicMeasure(measure, tempSplitString, tempSplitName);
		    	dynamicMeasure.setValue((double)uniqueApache.size());
			}
		}
		log.finer("Exiting collectDynamicMetrics method");
	}

	@Override
	public void teardown(MonitorEnvironment env) throws Exception {

	}
	
	public void disableCertificateValidation() {
		log.finer("Entering disableCertificateValidation method");  
		// Create a trust manager that does not validate certificate chains
		  TrustManager[] trustAllCerts = new TrustManager[] { 
		    new X509TrustManager() {
		      public X509Certificate[] getAcceptedIssuers() { 
		        return new X509Certificate[0]; 
		      }
		      public void checkClientTrusted(X509Certificate[] certs, String authType) {}
		      public void checkServerTrusted(X509Certificate[] certs, String authType) {}
		  }};

		  // Ignore differences between given hostname and certificate hostname
		  HostnameVerifier hv = new HostnameVerifier() {
		    public boolean verify(String hostname, SSLSession session) { return true; }
		  };

		  // Install the all-trusting trust manager
		  try {
		    SSLContext sc = SSLContext.getInstance("SSL");
		    sc.init(null, trustAllCerts, new SecureRandom());
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		    HttpsURLConnection.setDefaultHostnameVerifier(hv);
		  } catch (Exception e) {}
		  
		  log.finer("Exiting disableCertificateValidation method");
	}
	
	public void resetCounts() {
			log.finer("Entering resetCounts method");  
			java = 0;
			web = 0;
			mb = 0;
			dotnetindividual = 0;
			web_iis_individual = 0;
			web_apache_individual = 0;
			nativeagent = 0;
			unlicensedagent = 0;
			licensedagent = 0;
			phpagent = 0;
			cicsagent = 0;
			dbagent = 0;
			nodejsagent = 0;
			dotnetList = new ArrayList<>();
			web_iisList = new ArrayList<>();
			web_apacheList = new ArrayList<>();
			
			log.finer("Exiting resetCounts method");
	}

	public void analyzeAllNodes(String elementTag, String searchString) {
		log.finer("Entering analyzeAllNodes method");  
		
		for (int j = 0; j < allnodes.getLength(); j++) {						
			Element element = (Element) allnodes.item(j);
			
			if (element.getElementsByTagName(elementTag).item(0).getTextContent().equalsIgnoreCase(searchString)) {
				analyzeElement(element, j);
			}
		}
		log.finer("Exiting analyzeAllNodes method");
	}

	public void analyzeAllNodesDouble(String elementTag, String searchString, String elementTagSecond, String searchStringSecond) {
		log.finer("Entering analyzeAllNodesDouble method");  
		
		for (int j = 0; j < allnodes.getLength(); j++) {						
			Element element = (Element) allnodes.item(j);
			if (element.getElementsByTagName(elementTag).item(0).getTextContent().equalsIgnoreCase(searchString)) {
				if (element.getElementsByTagName(elementTagSecond).item(0).getTextContent().equalsIgnoreCase(searchStringSecond)) {
					analyzeElement(element, j);
				}
			}
		}
		log.finer("Exiting analyzeAllNodesDouble method");
	}
	
	public void analyzeElement(Element element, int i) {
		log.finer("Entering analyzeElement method");  
		
		if (element.getElementsByTagName("licenseInformation").getLength() > 0) {
			if (element.getElementsByTagName("licenseInformation").item(0).getTextContent().toLowerCase().contains("license ok")) {
				licensedagent++;
				
				switch (element.getElementsByTagName("technologyType").item(0).getTextContent().toLowerCase()) {
					case "java":								
						java++;
						break;
                    //Web Server for 6.5 Support
                    case "web server":
						web++;
						if (element.getElementsByTagName("agentInstanceName").item(0).getTextContent().toLowerCase().contains("[apache")) {
							web_apache_individual++;
							web_apacheList.add(allnodes.item(i));
							}
						if (element.getElementsByTagName("agentInstanceName").item(0).getTextContent().toLowerCase().contains("[iis")) {
							web_iis_individual++;
							web_iisList.add(allnodes.item(i));
							}
						break;
                    //Web Server for 7.0
                    case "iis":
						web++;
						web_iis_individual++;
						web_iisList.add(allnodes.item(i));
						break;
                    case "apache":
						web++;
						web_apache_individual++;
						web_apacheList.add(allnodes.item(i));
						break;
					case "websphere message broker":
						mb++;
						break;
					case "IBM Integration Bus":
						mb++;
						break;
					case "native":
						nativeagent++;
						break;
					case "php":
						phpagent++;
						break;
					case "cics":
						if (element.getElementsByTagName("capture").item(0).getTextContent().equalsIgnoreCase("true"))
							cicsagent++;
						break;
					case "database":
						dbagent++;
						break;
					case "node.js":
						nodejsagent++;
						break;
					case ".net":
						dotnetindividual++;
						dotnetList.add(allnodes.item(i));
						break;
				}
			}
			else if (element.getElementsByTagName("licenseInformation").item(0).getTextContent().toLowerCase().contains("license exhausted")) {
				unlicensedagent++;
			}
			else {
				if (element.getElementsByTagName("capture").item(0).getTextContent().equalsIgnoreCase("false"))
					unlicensedagent++;
			}
		}
		log.finer("Exiting analyzeElement method");
	}
}
