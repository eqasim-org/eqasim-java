package org.eqasim.core.analysis;

import java.util.ArrayList;
import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

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
	         //System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
	         NodeList nList = doc.getElementsByTagName("intersection");
	         //System.out.println("----------------------------");
	         
	         for (int temp = 0; temp < nList.getLength(); temp++) {
	             Node nNode = nList.item(temp);
	             //System.out.println("\nCurrent Element :" + nNode.getNodeName());
	             
	             if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	                 Element eElement = (Element) nNode;
	                 String idnode = eElement.getAttribute("node_id");
	                 NodeList nnList = eElement.getElementsByTagName("incoming_link");
	                 ArrayList<String> links = new ArrayList<String>();
	                 //System.out.println(idnode);
	                 
	                 for(int j=0; j<nnList.getLength(); j++) {
	                	 Node nnNode = nnList.item(j);
	                	 Element e = (Element) nnNode;
	                	 links.add(e.getAttribute("link_id"));
	                 }
	                 
	                 //System.out.println(links.toString());
	    	         //System.out.println("----------------------------");
	                 
	                 Intersection current_inter = new Intersection(idnode, links);
	                 this.intersections.add(current_inter);
	                 
	             }    
	             
	         }    
	         
		}
		catch(Exception e) {
	         e.printStackTrace();
	      }
	}

}
