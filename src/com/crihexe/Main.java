package com.crihexe;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Main {
	
	private String title;	// main Title of the score unless a movement is specified
	private String title_number;
	private boolean titleFound;	// it is possible that only the movement is defined as the title
	
	private String movement;	// To use as the main Title of the score, using the actual title as subtitle
	private String movement_number;
	private boolean movementFound;
	
	private String subtitle;
	private boolean forcedSubtitle;	// it is possible that in the credits an explicit subtitle is defined. in this case "movement" basically gets fucked
	
	public Main() {
		try {
			
			StringBuilder ldp = new StringBuilder();
			
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(new File("mxml.musicxml"));
			doc.getDocumentElement().normalize();
			
			// !!! score header
			// note that the last (in the page) valid found tag will be used!
			
			/*
				forEach(doc.getElementsByTagName("tag"), n -> {
					
				});
			 */
			
			// let's get the title
			// first check in <work> (for the dumbest musicXML exporter)
			forEach(doc.getElementsByTagName("work"), n -> {
				if(!n.hasChildNodes()) {
					title = n.getTextContent();
					titleFound = true;
				}
			});
			// now in <work-title>
			forEach(doc.getElementsByTagName("work-title"), n -> {
				if(!n.hasChildNodes()) {
					title = n.getTextContent();
					titleFound = true;
				}
			});
			// trying to get the number from <work-number>
			forEach(doc.getElementsByTagName("work-number"), n -> {
				if(!n.hasChildNodes())
					title_number = n.getTextContent();
			});
			// let's also get the movement real quick from <movement-title>
			forEach(doc.getElementsByTagName("movement-title"), n -> {
				if(!n.hasChildNodes()) {
					movement = n.getTextContent();
					movementFound = true;
				}
			});
			// and <movement-number>
			forEach(doc.getElementsByTagName("movement-number"), n -> {
				if(!n.hasChildNodes())
					movement_number = n.getTextContent();
			});
			// let's now check the credits (only <credit> for dumb exporters)
			forEach(doc.getElementsByTagName("credit"), n -> {
				if(!n.hasChildNodes()) {
					title = n.getTextContent();
					titleFound = true;
				} else {	// ok now for serious exporters:
					String value = first(toElement(n).getElementsByTagName("credit-words")).getTextContent();
					String type = first(toElement(n).getElementsByTagName("credit-type")).getTextContent();
					if(type.equals("title")) {
						title = value;
						titleFound = true;
					} else if(type.equals("subtitle")) {
						subtitle = value;
						forcedSubtitle = true;
					// let's also check for some other metadata
					} else if(type.equals("author")) {
						
					} else if(type.equals("composer")) {
						
					} else if(type.equals("arranger")) {
						
					} else if(type.equals("lyricist")) {
						
					} else if(type.equals("part name")) {
						
					} else if(type.equals("poet")) {
						
					}
				}
			});
			
			
			// let's now try to get the author, composer, arranger, poet
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Node first(NodeList list) throws NodeTaskException {
		if(list.getLength() == 0) throw new NodeTaskException("Empty list");
		return list.item(0);
	}
	
	public Element toElement(Node n) throws NodeTaskException {
		if(n.getNodeType() == Node.ELEMENT_NODE)
			return (Element) n;
		throw new NodeTaskException("Not an element");
	}
	
	public boolean forEach(NodeList list, NodeTask action) {
		for(int i = 0; i < list.getLength(); i++)
			try {
				action.exec(list.item(i));
			} catch(NodeTaskException e) {
				e.printStackTrace();
			}
			
		return list.getLength() > 0;
	}
	
	interface NodeTask {
		void exec(Node n) throws NodeTaskException;
	}
	
	class NodeTaskException extends Exception {
		private static final long serialVersionUID = 1L;

		public NodeTaskException() {
			super();
		}
		
		public NodeTaskException(String message) {
			super(message);
		}
		
	}
	
	public static void main(String[] args) {
		new Main();
	}

}
