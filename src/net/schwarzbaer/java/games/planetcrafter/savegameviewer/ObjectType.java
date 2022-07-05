package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

class ObjectType {
	
	enum PhysicalValue {
		Heat, Pressure, Oxygene, Biomass;
		
		static PhysicalValue valueOf_checked(String str) {
			try { return valueOf(str); }
			catch (Exception e) { return null; }
		}
	}
	
	final String id;
	boolean finished;
	String label;
	Double heat;
	Double pressure;
	Double oxygene;
	Double biomass;
	Double energy;
	Double oxygeneBooster;
	PhysicalValue isBoosterRocketFor;
	
	ObjectType(String id) {
		if (id==null) throw new IllegalArgumentException();
		this.id = id;
		finished = false;
		label    = null;
		heat     = null;
		pressure = null;
		oxygene  = null;
		biomass  = null;
		energy   = null;
		oxygeneBooster = null;
		isBoosterRocketFor = null;
	}
	
	static void writeToFile(File file, HashMap<String,ObjectType> objectTypes) {
		if (objectTypes==null) throw new IllegalArgumentException();
		
		System.out.printf("Write ObjectTypes to file \"%s\" ...%n", file.getAbsolutePath());
		
		try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
			
			Vector<Entry<String, ObjectType>> vector = new Vector<>(objectTypes.entrySet());
			vector.sort(Comparator.comparing(Entry<String, ObjectType>::getKey,Data.caseIgnoringComparator));
			
			for (Entry<String, ObjectType> entry : vector) {
				ObjectType ot = entry.getValue();
				                                 out.printf("ObjectType: "         +"%s%n", ot.id       );
				if (ot.label             !=null) out.printf("label = "             +"%s%n", ot.label    );
				if (ot.heat              !=null) out.printf("heat = "              +"%s%n", ot.heat     );
				if (ot.pressure          !=null) out.printf("pressure = "          +"%s%n", ot.pressure );
				if (ot.oxygene           !=null) out.printf("oxygene = "           +"%s%n", ot.oxygene  );
				if (ot.biomass           !=null) out.printf("biomass = "           +"%s%n", ot.biomass  );
				if (ot.energy            !=null) out.printf("energy = "            +"%s%n", ot.energy   );
				if (ot.oxygeneBooster    !=null) out.printf("oxygeneBooster = "    +"%s%n", ot.oxygeneBooster);
				if (ot.isBoosterRocketFor!=null) out.printf("isBoosterRocketFor = "+"%s%n", ot.isBoosterRocketFor.name());
				if (ot.finished                ) out.printf("<finished>"           +  "%n");
				
				out.println();
			}
			
		} catch (IOException ex) {
			System.err.printf("IOException while writing ObjectTypes to file \"%s\": %s%n", file.getAbsolutePath(), ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}

	static HashMap<String,ObjectType> readFromFile(File file) {
		HashMap<String,ObjectType> objectTypes = new HashMap<>();
		
		System.out.printf("Read ObjectTypes from file \"%s\" ...%n", file.getAbsolutePath());
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			
			String line, valueStr;
			ObjectType currentOT = null;
			while ( (line=in.readLine())!=null ) {
				
				if (line.isEmpty()) continue;
				if ( (valueStr=getValue(line,"ObjectType: "         ))!=null ) objectTypes.put(valueStr, currentOT = new ObjectType(valueStr));
				if ( (valueStr=getValue(line,"label = "             ))!=null ) currentOT.label    = valueStr;
				if ( (valueStr=getValue(line,"heat = "              ))!=null ) currentOT.heat     = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"pressure = "          ))!=null ) currentOT.pressure = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"oxygene = "           ))!=null ) currentOT.oxygene  = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"biomass = "           ))!=null ) currentOT.biomass  = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"energy = "            ))!=null ) currentOT.energy   = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"oxygeneBooster = "    ))!=null ) currentOT.oxygeneBooster     = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"isBoosterRocketFor = "))!=null ) currentOT.isBoosterRocketFor = PhysicalValue.valueOf_checked(valueStr);
				if ( (valueStr=getValue(line,"finished = "          ))!=null ) currentOT.finished = valueStr.equalsIgnoreCase("true");
				if (        line.equals(     "<finished>"           )        ) currentOT.finished = true;
				
			}
			
		} catch (FileNotFoundException ex) {
			//ex.printStackTrace();
		} catch (IOException ex) {
			System.err.printf("IOException while reading ObjectTypes from file \"%s\": %s%n", file.getAbsolutePath(), ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
		
		return objectTypes;
	}
	
	private static Double parseDouble(String str) {
		try { return Double.parseDouble(str); }
		catch (NumberFormatException e) { return null; }
	}

	private static String getValue(String line, String prefix) {
		if (line.startsWith(prefix))
			return line.substring(prefix.length());
		return null;
	}

	public static ObjectType getOrCreate(HashMap<String, ObjectType> objectTypes, String objectTypeID) {
		if (objectTypes==null) throw new IllegalArgumentException();
		if (objectTypeID==null) throw new IllegalArgumentException();
		
		ObjectType objectType = objectTypes.get(objectTypeID);
		if (objectType==null)
			objectTypes.put(objectTypeID, objectType = new ObjectType(objectTypeID));
		
		return objectType;
	}
}
