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
	
	ArrayList<Intersection> intersections;
	
	public IntersectionsReader() {
		this.intersections = new ArrayList<Intersection>();
	}
	
	public void read_xml(String filepath) {
		
		try {
	         File inputFile = new File(filepath);
	         DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	         DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	         Document doc = dBuilder.parse(inputFile);
	         doc.getDocumentElement().normalize();
	         NodeList nList = doc.getElementsByTagName("intersection");
	         
	         for (int temp = 0; temp < nList.getLength(); temp++) {
	             Node nNode = nList.item(temp);
	             
	             if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	                 Element eElement = (Element) nNode;
	                 String idnode = eElement.getAttribute("node_id");
	                 ArrayList<String> alreadyseen = new ArrayList<String>();
	                 NodeList nnList = eElement.getElementsByTagName("incoming_link");
	                 ArrayList<ArrayList<Id<Link>>> groups = new ArrayList<ArrayList<Id<Link>>>();
	                 
	                 for(int j=0; j<nnList.getLength(); j++) {
	                	 Node nnNode = nnList.item(j);
	                	 Element e = (Element) nnNode;
	                	 String s = e.getAttribute("link_id");
	                	 if (! alreadyseen.contains(s)) {
	                		 alreadyseen.add(s);
	                		 ArrayList<Id<Link>> group = new ArrayList<Id<Link>>();
	                		 group.add(Id.createLinkId(s));
	                		 String same = e.getAttribute("same_road_as");
	                		 if (! same.isEmpty()) {
	                			 group.add(Id.createLinkId(same));
	                			 alreadyseen.add(same);
	                		 }
		                	 groups.add(group);
	                	 }
	                 }
	                 
	                 
	                 Intersection current_inter = new Intersection(idnode, groups);
	                 this.intersections.add(current_inter);
	                 
	             }    
	             
	         }    
	         
		}
		catch(Exception e) {
	         e.printStackTrace();
	      }
	}

}
