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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;

import net.schwarzbaer.java.lib.gui.ValueListOutput;

class ObjectTypes extends HashMap<String, ObjectTypes.ObjectType> {
	private static final long serialVersionUID = 4515890497957737670L;

	enum ObjectTypeValue {
		Finished, Label, Heat, Pressure, Oxygen, Plants, Insects, Animals, Energy, ExpectsMultiplierFor, OxygenMultiplier, InsectsMultiplier, AnimalsMultiplier, BoosterRocket, IsProducer
	}

	enum PhysicalValue {
		Heat    ("pK/s" ),
		Pressure("nPa/s"),
		Oxygen  ("ppq/s", true, ot->ot.oxygenMultiplier),
		Plants  ("g/s"  ),
		Insects ("g/s"  , true, ot->ot.insectsMultiplier),
		Animals ("g/s"  , true, ot->ot.animalsMultiplier),
		;
		
		final String rateUnit;
		final boolean isMultiplierBased;
		final Function<ObjectType, Double> getMultiplierFcn;
		
		PhysicalValue(String rateUnit) {
			this(rateUnit, false, null);
		}
		PhysicalValue(String rateUnit, boolean isMultiplierBased, Function<ObjectType,Double> getMultiplierFcn) {
			this.rateUnit = rateUnit;
			this.isMultiplierBased = isMultiplierBased;
			this.getMultiplierFcn = getMultiplierFcn;
		}
		
		String formatRate(double value) {
			return String.format(Locale.ENGLISH, "%1.2f %s" , value, rateUnit);
		}
		
		static PhysicalValue valueOf_checked(String str) {
			try { return valueOf(str); }
			catch (Exception e) { return null; }
		}
	}

	static final String EnergyRateUnit = "kW";
	private final File datafile;

	ObjectTypes(File datafile)
	{
		this.datafile = datafile;
	}

	static String formatEnergyRate(double energy) {
		return String.format(Locale.ENGLISH, "%1.2f %s", energy, EnergyRateUnit);
	}

	void readFromFile() {
		System.out.printf("Read ObjectTypes from file \"%s\" ...%n", datafile.getAbsolutePath());
		clear();
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(datafile), StandardCharsets.UTF_8))) {
			
			String line, valueStr;
			ObjectType currentOT = null;
			while ( (line=in.readLine())!=null ) {
				
				if (line.isEmpty()) continue;
				if ( (valueStr=getValue(line,"ObjectType: "           ))!=null ) put(valueStr, currentOT = new ObjectType(valueStr, null));
				if ( (valueStr=getValue(line,"label = "               ))!=null ) currentOT.label    = valueStr;
				if ( (valueStr=getValue(line,"heat = "                ))!=null ) currentOT.heat     = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"pressure = "            ))!=null ) currentOT.pressure = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"oxygen = "              ))!=null ) currentOT.oxygen   = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"biomass = "             ))!=null ) currentOT.plants   = parseDouble(valueStr); // legacy
				if ( (valueStr=getValue(line,"plants = "              ))!=null ) currentOT.plants   = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"insects = "             ))!=null ) currentOT.insects  = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"animals = "             ))!=null ) currentOT.animals  = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"energy = "              ))!=null ) currentOT.energy   = parseDouble(valueStr);
				if (        line.equals(     "multiplierExpected"     )        ) currentOT.expectsMultiplierFor = PhysicalValue.Oxygen; // legacy:  boolean multiplierExpected 
				if ( (valueStr=getValue(line,"expectsMultiplierFor = "))!=null ) currentOT.expectsMultiplierFor = PhysicalValue.valueOf_checked(valueStr);
				if ( (valueStr=getValue(line,"oxygenBooster = "       ))!=null ) currentOT.oxygenMultiplier = parseDouble(valueStr); // legacy
				if ( (valueStr=getValue(line,"oxygenMultiplier = "    ))!=null ) currentOT.oxygenMultiplier = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"insectsBooster = "      ))!=null ) currentOT.insectsMultiplier = parseDouble(valueStr); // legacy
				if ( (valueStr=getValue(line,"insectsMultiplier = "   ))!=null ) currentOT.insectsMultiplier = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"animalsMultiplier = "   ))!=null ) currentOT.animalsMultiplier = parseDouble(valueStr);
				if ( (valueStr=getValue(line,"isBoosterRocketFor = "  ))!=null ) currentOT.isBoosterRocketFor = PhysicalValue.valueOf_checked(valueStr);
				if ( (valueStr=getValue(line,"occurrences = "         ))!=null ) Occurrence.parseDataStr(valueStr, currentOT.occurrences);
				if (        line.equals(     "isProducer"             )        ) currentOT.isProducer = true;
				if ( (valueStr=getValue(line,"finished = "            ))!=null ) currentOT.finished = valueStr.equalsIgnoreCase("true"); // legacy
				if (        line.equals(     "<finished>"             )        ) currentOT.finished = true;
				
			}
			
		} catch (FileNotFoundException ex) {
			//ex.printStackTrace();
		} catch (IOException ex) {
			System.err.printf("IOException while reading ObjectTypes: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}

	void writeToFile() {
		System.out.printf("Write ObjectTypes to file \"%s\" ...%n", datafile.getAbsolutePath());
		
		try (PrintWriter out = new PrintWriter(datafile, StandardCharsets.UTF_8)) {
			
			Vector<Entry<String, ObjectType>> vector = new Vector<>(entrySet());
			vector.sort(Comparator.comparing(Entry<String, ObjectType>::getKey,Data.caseIgnoringComparator));
			
			for (Entry<String, ObjectType> entry : vector) {
				ObjectType ot = entry.getValue();
				                                    out.printf("ObjectType: "           +"%s%n", ot.id       );
				if ( ot.label               !=null) out.printf("label = "               +"%s%n", ot.label    );
				if ( ot.heat                !=null) out.printf("heat = "                +"%s%n", ot.heat     );
				if ( ot.pressure            !=null) out.printf("pressure = "            +"%s%n", ot.pressure );
				if ( ot.oxygen              !=null) out.printf("oxygen = "              +"%s%n", ot.oxygen   );
				if ( ot.plants              !=null) out.printf("plants = "              +"%s%n", ot.plants   );
				if ( ot.insects             !=null) out.printf("insects = "             +"%s%n", ot.insects  );
				if ( ot.animals             !=null) out.printf("animals = "             +"%s%n", ot.animals  );
				if ( ot.energy              !=null) out.printf("energy = "              +"%s%n", ot.energy   );
				if ( ot.expectsMultiplierFor!=null) out.printf("expectsMultiplierFor = "+"%s%n", ot.expectsMultiplierFor.name());
				if ( ot.oxygenMultiplier    !=null) out.printf("oxygenMultiplier = "    +"%s%n", ot.oxygenMultiplier);
				if ( ot.insectsMultiplier   !=null) out.printf("insectsMultiplier = "   +"%s%n", ot.insectsMultiplier);
				if ( ot.animalsMultiplier   !=null) out.printf("animalsMultiplier = "   +"%s%n", ot.animalsMultiplier);
				if ( ot.isBoosterRocketFor  !=null) out.printf("isBoosterRocketFor = "  +"%s%n", ot.isBoosterRocketFor.name());
				if (!ot.occurrences.isEmpty()     ) out.printf("occurrences = "         +"%s%n", Occurrence.toDataStr(ot.occurrences));
				if ( ot.isProducer                ) out.printf("isProducer"             +  "%n");
				if ( ot.finished                  ) out.printf("<finished>"             +  "%n");
				
				out.println();
			}
			
		} catch (IOException ex) {
			System.err.printf("IOException while writing ObjectTypes: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}

	Vector<ObjectType> getListSortedByName()
	{
		Vector<ObjectType> list = new Vector<>(this.values());
		list.sort(Comparator.<ObjectType,String>comparing(ot->ot.getName(), Data.caseIgnoringComparator));
		return list;
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

	static Double sumUpMultipliers(ObjectType[] objectTypes, Function<ObjectType,Double> getMultiplier) {
		if (objectTypes==null) return null;
		
		Double multiplierSum = null;
		for (ObjectType ot : objectTypes) {
			Double multiplier = getMultiplier.apply(ot);
			if (multiplier!=null) {
				if (multiplierSum == null)
					multiplierSum = multiplier;
				else
					multiplierSum += multiplier;
			}
		}
		return multiplierSum;
	}

	ObjectType get(String objectTypeID, Occurrence occurence) {
		ObjectType ot = super.get(objectTypeID);
		if (occurence!=null && ot!=null) ot.addOccurence(occurence);
		
		return ot;
	}
	
	interface ObjectTypeCreator {
		ObjectType getOrCreate(String objectTypeID, Occurrence occurrence);
	}

	ObjectType getOrCreate(String objectTypeID, Occurrence occurrence, HashSet<String> newObjectTypes) {
		if (objectTypeID==null) throw new IllegalArgumentException();
		
		ObjectType ot = get(objectTypeID);
		if (ot==null) {
			ot = new ObjectType(objectTypeID, null);
			put(objectTypeID, ot);
			if (newObjectTypes!=null)
				newObjectTypes.add(objectTypeID);
		}
		ot.addOccurence(occurrence);
		
		return ot;
	}
	
	enum Occurrence {
		WorldObject, Achievement, Blueprint, User, Product, ObjectList;
		
		String getShortLabel() {
			switch (this) {
				case Achievement: return "A";
				case Blueprint  : return "B";
				case Product    : return "P";
				case User       : return "U";
				case WorldObject: return "WO";
				case ObjectList : return "OL";
			}
			return "??";
		}

		private static void parseDataStr(String dataStr, EnumSet<Occurrence> occurrences) {
			occurrences.clear();
			if (dataStr==null) return;
			if (dataStr.isEmpty()) return;
			String[] parts = dataStr.split(",",-1);
			for (String part : parts) {
				Occurrence o;
				try { o = valueOf(part); }
				catch (Exception e) { continue; }
				occurrences.add(o);
			}
		}

		private static String toDataStr(EnumSet<Occurrence> occurrences) {
			Iterable<String> it = ()->occurrences.stream().sorted().map(Occurrence::name).iterator();
			return String.join(",", it);
		}
	}
	
	ObjectType findObjectTypeByID(String objectTypeID, Occurrence occurence) {
		if (objectTypeID==null) return null;
		ObjectType ot = get(objectTypeID);
		if (ot!=null) ot.addOccurence(occurence);
		return ot; 
	}
	
	ObjectType findObjectTypeByName(String name, Occurrence occurence) {
		if (name==null) return null;
		for (ObjectType ot : values())
			if (name.equals(ot.getName())) {
				ot.addOccurence(occurence);
				return ot;
			}
		return null;
	}

	static class ObjectType {
		final String id;
		boolean finished;
		String label;
		Double heat;
		Double pressure;
		Double oxygen;
		Double plants;
		Double insects;
		Double animals;
		Double energy;
		PhysicalValue expectsMultiplierFor;
		Double oxygenMultiplier;
		Double insectsMultiplier;
		Double animalsMultiplier;
		PhysicalValue isBoosterRocketFor;
		boolean isProducer;
		final EnumSet<Occurrence> occurrences;
		
		ObjectType(String id, Occurrence occurrence) {
			if (id==null) throw new IllegalArgumentException();
			this.id = id;
			finished = false;
			label    = null;
			heat     = null;
			pressure = null;
			oxygen   = null;
			plants   = null;
			insects  = null;
			animals  = null;
			energy   = null;
			expectsMultiplierFor = null;
			oxygenMultiplier = null;
			insectsMultiplier = null;
			animalsMultiplier = null;
			isBoosterRocketFor = null;
			isProducer = false;
			occurrences = EnumSet.noneOf(Occurrence.class);
			if (occurrence!=null) occurrences.add(occurrence);
		}
		
		void addOccurence(Occurrence occurrence) {
			if (occurrence==null) return;
			if (occurrence!=Occurrence.User)
				occurrences.remove(Occurrence.User); 
			occurrences.add(occurrence);
		}

		String getName() {
			if (label!=null && !label.isBlank())
				return label;
			return String.format("{%s}", id);
		}

		boolean isActive() {
			return hasEffectOnTerraforming() || energy  !=null;
		}
	
		boolean hasEffectOnTerraforming()
		{
			return
					oxygen  !=null ||
					heat    !=null ||
					pressure!=null ||
					plants  !=null ||
					insects !=null ||
					animals !=null;
		}

		void addActiveOutputTo(ValueListOutput out, int indentLevel, ObjectType[] children) {
			if (oxygen  !=null) {
				if (expectsMultiplierFor == PhysicalValue.Oxygen)
					            addActiveOutputLineTo(out, indentLevel, "Oxygen"  , PhysicalValue.Oxygen  ::formatRate, oxygen  , sumUpMultipliers(children, expectsMultiplierFor.getMultiplierFcn), true);
				else            addActiveOutputLineTo(out, indentLevel, "Oxygen"  , PhysicalValue.Oxygen  ::formatRate, oxygen  , null, false);
			}
			if (heat    !=null) addActiveOutputLineTo(out, indentLevel, "Heat"    , PhysicalValue.Heat    ::formatRate, heat    , null, false);
			if (pressure!=null) addActiveOutputLineTo(out, indentLevel, "Pressure", PhysicalValue.Pressure::formatRate, pressure, null, false);
			if (plants  !=null) addActiveOutputLineTo(out, indentLevel, "Plants"  , PhysicalValue.Plants  ::formatRate, plants  , null, false);
			if (insects !=null) {
				if (expectsMultiplierFor == PhysicalValue.Insects)
					            addActiveOutputLineTo(out, indentLevel, "Insects" , PhysicalValue.Insects ::formatRate, insects , sumUpMultipliers(children, expectsMultiplierFor.getMultiplierFcn), true);
				else            addActiveOutputLineTo(out, indentLevel, "Insects" , PhysicalValue.Insects ::formatRate, insects , null, false);
			}
			if (animals !=null) {
				if (expectsMultiplierFor == PhysicalValue.Animals)
					            addActiveOutputLineTo(out, indentLevel, "Animals" , PhysicalValue.Animals ::formatRate, animals , sumUpMultipliers(children, expectsMultiplierFor.getMultiplierFcn), true);
				else            addActiveOutputLineTo(out, indentLevel, "Animals" , PhysicalValue.Animals ::formatRate, animals , null, false);
			}
			if (energy  !=null) addActiveOutputLineTo(out, indentLevel, "Energy"  , ObjectTypes::formatEnergyRate     , energy  , null, false);
		}
	
		void addActiveOutputLineTo(ValueListOutput out, int indentLevel, String label, Function<Double,String> formatRate, double rate, Double multiplier, boolean multiplierExpected) {
			if (multiplier != null) {
				out.add(indentLevel, label, "%s", String.format(Locale.ENGLISH, "%1.2f x %s", multiplier, formatRate.apply(rate)));
				out.add(indentLevel,  null, "%s", formatRate.apply(rate*multiplier));
			} else if (multiplierExpected)
				out.add(indentLevel, label, "%s", "<multiplier expected>");
			else
				out.add(indentLevel, label, "%s", formatRate.apply(rate));
		}
	}
}
