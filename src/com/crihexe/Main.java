package com.crihexe;

import static com.crihexe.ldp.LDPNode.node;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.audiveris.proxymusic.ScorePartwise;
import org.audiveris.proxymusic.util.Marshalling;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.crihexe.ldp.LDPNode;
import com.scalified.tree.TraversalAction;
import com.scalified.tree.TreeNode;

public class Main {
	
	private final String[] KEYS_FIFTHS = new String[] {"C-", "G-", "D-", "A-", "E-", "B-", "F", "C", "G", "D", "A", "E", "B", "F+", "C+"};	// just add 7 to musicXML fifths to get the index
	private final HashMap<String, String> DURATIONS = new HashMap<>();
	private final HashMap<String, String> ACCIDENTALS = new HashMap<>();
	
	private String title;	// main Title of the score unless a movement is specified
	private String title_number;
	private boolean titleFound;	// it is possible that only the movement is defined as the title
	
	private String movement;	// To use as the main Title of the score, using the actual title as subtitle
	private String movement_number;
	private boolean movementFound;
	
	private String subtitle;
	private boolean forcedSubtitle;	// it is possible that in the credits an explicit subtitle is defined. in this case "movement" or "title" basically gets fucked
	
	private String author;	// the main author (or the composer)
	private boolean authorFound;
	
	private String arranger;	// the arranger (or sometimes the author if not specified)
	private boolean arrangerFound;
	
	private Map<String, String> parts = new HashMap<>();	// the parts specified in <part-list> as Map<ID, NAME>
	
	/*private String pageWidth;
	private String pageHeight;
	private String leftMargin;
	private String rightMargin;
	private String topMargin;
	private String bottomMargin;*/
	
	
	private LDPNode lastBarlineNode;
	
	public Main() {
		DURATIONS.put("128th", "o");
		DURATIONS.put("64th", "i");
		DURATIONS.put("32th", "t");
		
		DURATIONS.put("16th", "s");
		DURATIONS.put("sixteenth", "s");
		DURATIONS.put("1", "s");
		
		DURATIONS.put("8th", "e");
		DURATIONS.put("eighth", "e");
		DURATIONS.put("2", "e");
		
		DURATIONS.put("3", "e.");
		
		DURATIONS.put("4th", "q");
		DURATIONS.put("quarter", "q");
		DURATIONS.put("4", "q");
		
		DURATIONS.put("6", "q.");
		
		DURATIONS.put("half", "h");
		DURATIONS.put("8", "h");
		
		DURATIONS.put("12", "h.");
		
		DURATIONS.put("whole", "w");
		DURATIONS.put("16", "w");
		
		DURATIONS.put("24", "w.");
		
		DURATIONS.put("breve", "b");
		DURATIONS.put("32", "b");
		
		DURATIONS.put("48", "b.");
		
		DURATIONS.put("long", "l");
		DURATIONS.put("64", "l");
		
		ACCIDENTALS.put("sharp", "+");
		ACCIDENTALS.put("natural", "=");
		ACCIDENTALS.put("flat", "-");
		ACCIDENTALS.put("double-sharp", "x");
		ACCIDENTALS.put("sharp-sharp", "++");
		ACCIDENTALS.put("flat-flat", "--");
		ACCIDENTALS.put("natural-sharp", "=+");
		ACCIDENTALS.put("natural-flat", "=-");
		
		try {
			
			StringBuilder ldp = new StringBuilder();
			
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(new File("mxml.musicxml"));
			doc.getDocumentElement().normalize();
			
			ScorePartwise score = (ScorePartwise) Marshalling.unmarshal(new FileInputStream(new File("mxml.musicxml")));
			
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
					Element e = toElement(n);
					String value = first(e.getElementsByTagName("credit-words")).getTextContent();
					String type = first(e.getElementsByTagName("credit-type")).getTextContent();
					if(type.equals("title")) {
						title = value;
						titleFound = true;
					} else if(type.equals("subtitle")) {
						subtitle = value;
						forcedSubtitle = true;
					// let's also check for some other metadata
					} else if(type.equals("author")) {
						author = value;
						authorFound = true;
					} else if(type.equals("composer")) {
						author = value;
						authorFound = true;
					} else if(type.equals("arranger")) {
						arranger = value;
						arrangerFound = true;
					}/* else if(type.equals("lyricist")) {	// too detailed, maybe in the future
						
					} else if(type.equals("poet")) {
						
					}*/
				}
			});
			
			// let's now get the parts from <score-part>
			forEach(doc.getElementsByTagName("score-part"), n -> {
				Element e = toElement(n);
				String id = e.getAttribute("id");	// let's get the part id
				String name = "";
				if(!n.hasChildNodes())
					name = n.getTextContent();
				else {
					Node m = e.getElementsByTagName("part-abbreviation-display").item(0);	// min priority
					if(m != null) name = m.getTextContent();
					m = e.getElementsByTagName("part-abbreviation").item(0);
					if(m != null) name = m.getTextContent();
					m = e.getElementsByTagName("instrument-name").item(0);
					if(m != null) name = m.getTextContent();
					m = e.getElementsByTagName("part-name").item(0);
					if(m != null) name = m.getTextContent();
					e.getElementsByTagName("part-name-display").item(0);	// max priority
					if(m != null) name = m.getTextContent();
				}
				parts.put(id, name);
			});
			
			// let's get some page layout
			/*
			try {pageWidth = first(doc.getElementsByTagName("page-width")).getTextContent();} catch(Exception ee) {}
			try {pageHeight = first(doc.getElementsByTagName("page-height")).getTextContent();} catch(Exception ee) {}
				
			try {leftMargin = first(doc.getElementsByTagName("leftMargin")).getTextContent();} catch(Exception ee) {}
			try {rightMargin = first(doc.getElementsByTagName("rightMargin")).getTextContent();} catch(Exception ee) {}
			try {topMargin = first(doc.getElementsByTagName("topMargin")).getTextContent();} catch(Exception ee) {}
			try {bottomMargin = first(doc.getElementsByTagName("bottomMargin")).getTextContent();} catch(Exception ee) {}
			*/
			
			
			
			
			// let's now define the final attributes
			String finalTitle = "";
			String finalSubtitle = "";
			boolean subtitleFound;
			
			if(movementFound) {
				finalTitle = movement;
				if(forcedSubtitle)
					finalSubtitle = subtitle;
				else
					finalSubtitle = title;
				subtitleFound = true;
			} else {
				finalTitle = title;
				finalSubtitle = subtitle;
				subtitleFound = forcedSubtitle;
			}
			
			// let's now begin to write the LDP file
			final LDPNode tree = new LDPNode("score");
			tree.add(node("vers", "2.0"));
			tree.add(node("defineStyle", "\"title\"", 
						node("font", "\"Arial\"", "22pt"), 
						node("color", "#000000")
					));
			tree.add(node("defineStyle", "\"subtitle\"", 
						node("font", "\"Arial\"", "16pt"), 
						node("color", "#000000")
					));
			tree.add(node("title", "center", "\"" + finalTitle + "\"", 
						node("style", "\"title\"")/*,	// TODO set position of metadata
						node("dx", ""),
						node("dy", "")*/
					));
			if(subtitleFound)
				tree.add(node("title", "center", "\"" + finalSubtitle + "\"", 
						node("style", "\"subtitle\"")
					));
				// TODO add author and arranger
				// maybe some options (opt) here
			;
			
			// now the hard part, let's get the music data and put it into ldp format
			forEach(doc.getElementsByTagName("part"), n -> {
				Element e = toElement(n);
				String id = e.getAttribute("id");
				String instrumentName = parts.get(id);
				
				LDPNode local = tree.add(node("instrument"));
				
				local.add(node(id));
				local.add(node("name", "\"" + instrumentName + "\""));
				
				final int staves;
				
				Element attribute = toElement(first(toElement(first(e.getElementsByTagName("measure"))).getElementsByTagName("attributes")));
				int temp = 1;
				try {
					temp = Integer.parseInt(first(attribute.getElementsByTagName("staves")).getTextContent());
				} catch(NodeTaskException ex) {}
				staves = temp;
				local.add(node("staves", ""+staves));	// only works with strings and LDPNode
				
				final LDPNode data = local.add(node("musicData"));
				
				forEach(e.getElementsByTagName("measure"), m -> {
					Element measure = toElement(m);
					
					
					int finalNum = 1;
					int finalDen = 1;
					try {
						Element attrib = toElement(first(measure.getElementsByTagName("attributes")));
						
						forEach(attrib.getElementsByTagName("clef"), c -> {
							Element clef = toElement(c);
							LDPNode clefNode = data.add(node("clef", clef.getElementsByTagName("sign").item(0).getTextContent()));
							if(staves > 1)
								clefNode.add(node("p" + clef.getAttribute("number")));
						});
						try {
							int fifths = Integer.parseInt(first(attrib.getElementsByTagName("fifths")).getTextContent());
							data.add(node("key", KEYS_FIFTHS[fifths+7]));
						} catch(Exception ex) {}
						try {	// doing all of this because apparently musicXML could specify more than a fraction in order to represent a fraction equals to the sum of all of the times
							ArrayList<Integer> beats = new ArrayList<>();
							ArrayList<Integer> beatsT = new ArrayList<>();
							forEach(attrib.getElementsByTagName("beats"), b -> 
								beats.add(Integer.parseInt(b.getTextContent())));
							forEach(attrib.getElementsByTagName("beat-type"), b -> 
								beatsT.add(Integer.parseInt(b.getTextContent())));
							int num = beats.get(0);
							int den = beatsT.get(0);
							for(int i = 1; i < beats.size(); i++) {
								Pair<Integer, Integer> frac = addFraction(num, den, beats.get(i), beatsT.get(i));
								num = frac.first;
								den = frac.second;
							}
							finalNum = num;
							finalDen = den;
							data.add(node("time", ""+num, ""+den));
						} catch(Exception ex) {}
					} catch(NodeTaskException ex) {}
					
					final double timeFraction = (double)finalNum/(double)finalDen;
					
					forEachPrev(measure.getElementsByTagName("note"), (no, prev) -> {
						Element note = toElement(no);
						
						boolean rest = false;
						boolean fullRest = false;
						try {
							String measureAttr = toElement(first(note.getElementsByTagName("rest"))).getAttribute("measure");
							rest = true;	// aka if there is an element rest, this note is a rest
							fullRest = measureAttr.equals("yes");
						} catch(Exception ex) {}
						
						boolean hide = note.getAttribute("print-object").equals("no");
						
						String step = "";
						String octave = "";
						int duration = -1;
						String type = "";
						int voice = -1;
						
						try {
							step = first(note.getElementsByTagName("step")).getTextContent();
							octave = first(note.getElementsByTagName("octave")).getTextContent();
						} catch(Exception ex) {}
						try {
							duration = Integer.parseInt(first(note.getElementsByTagName("duration")).getTextContent());	// TODO attention with dotted values. its like the normal note + normal/2
						} catch(Exception ex) {}
						try {
							type = first(note.getElementsByTagName("type")).getTextContent();
						} catch(Exception ex) {}
						try {
							voice = Integer.parseInt(first(note.getElementsByTagName("voice")).getTextContent());
						} catch(Exception ex) {}
						
						int p = voice/4+1;	// try to find the staff by voice
						try {	// set p to the actual value if staff tag is specified
							p = Integer.parseInt(first(note.getElementsByTagName("staff")).getTextContent());
						} catch(Exception ex) {}
						
						String ldpAccidental = "";
						try {
							String accidental = first(note.getElementsByTagName("accidental")).getTextContent();
							ldpAccidental = ACCIDENTALS.get(accidental);
						} catch(Exception ex) {}	// TODO !!!!!!!!!! pay attention to make every first() or toElement() function under a try-catch block. (not really every lol but pay attention!)
						
						boolean chord = note.getElementsByTagName("chord").getLength() > 0;
						
						String ldpDuration = DURATIONS.get(type);
						try {
							duration = Integer.parseInt(first(note.getElementsByTagName("duration")).getTextContent());
							// TODO I dont know how it works... i mean what if the time signature is something like 3/8? so im gonna try with 3/4 and 4/4, then maybe if I remember (TODO TODO) I will try
							//int fullMeasure = finalNum*finalDen;	// TODO so if its like 3/4 the full measure duration is 12, 4/4 -> 16. with 3/8 its 24, so I think this is wrong but LOL idc
							//fullMeasure/duration;
							
							// update: maybe I actually realized that <duration> is not related with time signature
							ldpDuration = DURATIONS.get(""+duration);	// so I updated the durations map
							
							// to make a rest as long as a full measure, we have to know if its duration is equal to the max duration of the measure
							if(rest)
								if(fullRest || duration == timeFraction*16)
									ldpDuration = DURATIONS.get("whole");
						} catch(Exception ex) {}
						
						LDPNode toAdd = data;
						if(chord) {
							LDPNode prevParent = (LDPNode) prev.parent();
							if(prevParent.data().equals("chord"))
								toAdd = prevParent;
							else {
								toAdd = data.add(node("chord"));
								prevParent.remove(prev);
								toAdd.add(prev);
							}
						}
						
						if(rest)
							return toAdd.add(node(hide?"goFwd":"r", ldpDuration, "v"+voice, "p"+p));
						return toAdd.add(node("n", (ldpAccidental==null?"":ldpAccidental) + step.toLowerCase() + octave, ldpDuration, "v"+voice, "p"+p));
					});
					
					lastBarlineNode = data.add(node("barline", "simple"));
				});
				data.remove(lastBarlineNode);
				data.add(node("barline", "end"));
			});
			
			
			
			
			
			
			
			// let's finally traverse the tree in order to print the ldp data
			final int topNodeLevel = tree.level();
			
			tree.traversePreOrder(new TraversalAction<TreeNode<String>>() {
				
				private int lastLevel = -1;

				@Override
				public void perform(TreeNode<String> node) {
					int nodeLevel = node.level() - topNodeLevel;
					if(nodeLevel < lastLevel) 
						for(int i = 0; i < lastLevel - nodeLevel; i++)
							System.out.print(")");
					System.out.print(" ");
					if(!node.isLeaf()) System.out.print("(");
					System.out.print(node.data());
					lastLevel = nodeLevel;
				}

				@Override
				public boolean isCompleted() {
					return false;
				}
				
			});
			System.out.println(tree.toString());
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
				//e.printStackTrace();
			}
			
		return list.getLength() > 0;
	}
	public boolean forEachPrev(NodeList list, NodeTaskPrev action) {
		LDPNode prev = null;
		for(int i = 0; i < list.getLength(); i++)
			try {
				prev = action.exec(list.item(i), prev);
			} catch(NodeTaskException e) {
				//e.printStackTrace();
			}
			
		return list.getLength() > 0;
	}
	
	public Pair<Integer, Integer> addFraction(int num1, int den1, int num2, int den2) {
		int den3 = gcd(den1, den2);
		den3 = (den1 * den2) / den3;
		int num3 = num1*(den3/den1) + num2*(den3/den2);
		int cf = gcd(num3, den3);
		num3 = num3/cf;
		den3 = den3/cf;
		return new Pair<Integer, Integer>(num3, den3);
	}
	
	public int gcd(int a, int b) {
		if(a == 0) return b;
		return gcd(b%a, a);
	}
	
	interface NodeTask {
		void exec(Node n) throws NodeTaskException;
	}
	
	interface NodeTaskPrev {
		LDPNode exec(Node n, LDPNode prev) throws NodeTaskException;
	}
	
	class Pair<K, V> {
		
		public K first;
		public V second;
		
		public Pair(K first, V second) {
			this.first = first;
			this.second = second;
		}
		
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
