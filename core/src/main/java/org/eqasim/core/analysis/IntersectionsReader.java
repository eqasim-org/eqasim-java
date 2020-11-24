package org.eqasim.core.analysis;

import java.util.ArrayList;
import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class IntersectionsReader {
	
	public enum FlowsTag{
		Yes,
		No
	}
	
	ArrayList<Intersection> intersections;
	ArrayList<IntersectionWithFlows> intersectionsFlows;
	FlowsTag tag;
	
	public IntersectionsReader(boolean with_flows) {
		this.intersections = new ArrayList<Intersection>();
		this.intersectionsFlows = new ArrayList<IntersectionWithFlows>();
		
		if (with_flows) {
			this.tag = FlowsTag.Yes;
		}
		else {
			this.tag = FlowsTag.No;
		}
	}
	
	public void read_xml(String filepath) {
		
		try {
	         File inputFile = new File(filepath);
	         DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	         DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	         Document doc = dBuilder.parse(inputFile);
	         doc.getDocumentElement().normalize();
	         NodeList nList = doc.getElementsByTagName("Traffic-signaled_intersection");
	         NodeList nList2 = doc.getElementsByTagName("Non_traffic-signaled_intersection");
	         
	         for (int temp = 0; temp < nList.getLength() + nList2.getLength(); temp++) {
	        	 System.out.println(temp);
	        	 Node nNode = null;
	        	 String label = null;
	        	 if (temp < nList.getLength()) {
	        		 nNode = nList.item(temp);
	        		 label = "TS";
	        	 }
	        	 else {
	        		 nNode = nList2.item(temp - nList.getLength());
	        		 label = "NTS";
	        	 }
	             
	             if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	                 Element eElement = (Element) nNode;
	                 String idnode = eElement.getAttribute("node_id");
	                 
	                 NodeList linksList = eElement.getElementsByTagName("incoming_link");
	                 ArrayList<Id<Link>> links = new ArrayList<Id<Link>>();
	                 
	                 for(int j=0; j<linksList.getLength(); j++) {
	                	 Node nnNode = linksList.item(j);
	                	 Element e = (Element) nnNode;
	                	 String s = e.getAttribute("link_id");
	                	 links.add(Id.createLinkId(s));
	                 }
	                 
	                 NodeList groupsList = eElement.getElementsByTagName("group");
	                 ArrayList<ArrayList<Id<Link>>> groups = new ArrayList<ArrayList<Id<Link>>>();
	                 ArrayList<Id<Link>> links_in_groups = new ArrayList<Id<Link>>();
	                 
	                 for (int j=0; j<groupsList.getLength(); j++) {
	                	 Node nnNode = groupsList.item(j);
	                	 Element e = (Element) nnNode;
	                	 String link1 = e.getAttribute("link_1");
	                	 String link2 = e.getAttribute("link_2");
	                	 
	                	 ArrayList<Id<Link>> group = new ArrayList<Id<Link>>();
	                	 group.add(Id.createLinkId(link1));
	                	 group.add(Id.createLinkId(link2));
	                	 links_in_groups.add(Id.createLinkId(link1));
	                	 links_in_groups.add(Id.createLinkId(link2));
	                	 
	                	 groups.add(group);
	                 }
	                 
	                 for (int k = 0; k < links.size(); k++) {
	                	 Id<Link> lid = links.get(k);
	                	 if (! links_in_groups.contains(lid)) {
	                		 ArrayList<Id<Link>> group = new ArrayList<Id<Link>>();
	                		 group.add(lid);
	                		 groups.add(group);
	                	 }
	                 }
	                 
	                 if (this.tag == FlowsTag.No) {
	                	 Intersection current_inter = new Intersection(idnode, links, groups, label);
	                	 this.intersections.add(current_inter);
	                 }
	                 else {
	                	 IntersectionWithFlows current_inter = new IntersectionWithFlows(idnode, links, groups, label);
	                	 this.intersectionsFlows.add(current_inter);
	                 }
	                 
	             }    
	             
	         }    
	         
		}
		catch(Exception e) {
	         e.printStackTrace();
	      }
	}
	
	
	public static void main(String[] args) {
		
		IntersectionsReader ir = new IntersectionsReader(false);
		ir.read_xml("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/NEWOUTPUT.xml");
	}

}
