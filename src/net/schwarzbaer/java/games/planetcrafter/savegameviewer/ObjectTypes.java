package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.Predicate;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.PlanetCrafterSaveGameViewer.LabelLanguage;

class ObjectTypes extends HashMap<String, ObjectTypes.ObjectType> {
	private static final long serialVersionUID = 4515890497957737670L;

	enum ObjectTypeValue {
		Finished, Label_en, Label_de, Class_,
		Heat, Pressure, Oxygen, Plants, Insects, Animals, Energy,
		ExpectsMultiplierFor, OxygenMultiplier, InsectsMultiplier, AnimalsMultiplier,
		BoosterRocket, BoosterMultiplier,
		IsMachineOptomizer, MORange, MOCapacity, 
		IsMOFuse, MOFuseMultiplier, 
		IsProducer,
		;
		static boolean isLabel(ObjectTypeValue val) {
			return val == Label_en || val == Label_de;
		}
	}
	
	enum ObjectTypeClassClass { Resource, Structure, Equipment, Special, Vehicle }
	enum ObjectTypeClass {
		Equipment             ( "Equipment"               , ObjectTypeClassClass.Equipment ),
		Equipment_Vehicle     ( "Equipment, Vehicle"      , ObjectTypeClassClass.Equipment ),
		Equipment_Player      ( "Equipment, Player"       , ObjectTypeClassClass.Equipment ),
		Equipment_Machine     ( "Equipment, Machine"      , ObjectTypeClassClass.Equipment ),
		Resource              ( "Resource"                , ObjectTypeClassClass.Resource  ),
		Resource_Craftable    ( "Resource, craftable"     , ObjectTypeClassClass.Resource  ),
		Resource_Minable      ( "Resource, minable"       , ObjectTypeClassClass.Resource  ),
		Resource_Minable_Cheap( "Resource, minable, cheap", ObjectTypeClassClass.Resource  ),
		Resource_Food         ( "Resource, Food"          , ObjectTypeClassClass.Resource  ),
		Resource_SeedEgg      ( "Resource, Seed/Egg"      , ObjectTypeClassClass.Resource  ),
		Structure             ( "Structure"               , ObjectTypeClassClass.Structure ),
		Structure_Furniture   ( "Structure, Furniture"    , ObjectTypeClassClass.Structure ),
		Structure_Storage     ( "Structure, Storage"      , ObjectTypeClassClass.Structure ),
		Structure_Building    ( "Structure, Building"     , ObjectTypeClassClass.Structure ),
		Structure_Machine     ( "Structure, Machine"      , ObjectTypeClassClass.Structure ),
		Special               ( "Special"                 , ObjectTypeClassClass.Special   ),
		Special_Rocket        ( "Special, Rocket"         , ObjectTypeClassClass.Special   ),
		Special_Hidden        ( "Special, hidden"         , ObjectTypeClassClass.Special   ),
		Special_Wreckage      ( "Special, Wreckage"       , ObjectTypeClassClass.Special   ),
		Special_Quest         ( "Special, Quest Item"     , ObjectTypeClassClass.Special   ),
		Special_Blueprint     ( "Special, Blueprint"      , ObjectTypeClassClass.Special   ),
		Special_Money         ( "Special, Money"          , ObjectTypeClassClass.Special   ),
		Vehicle               ( "Vehicle"                 , ObjectTypeClassClass.Vehicle   ),
		;
		private final String label;
		final ObjectTypeClassClass class_;
		ObjectTypeClass(String label, ObjectTypeClassClass class_) { this.label = label; this.class_ = class_; }
		@Override public String toString() { return label; }
		
		static ObjectTypeClass valueOf_checked(String str) {
			try { return valueOf(str); }
			catch (Exception e) { return null; }
		}
	}

	enum PhysicalValue {
		Heat    ("pK/s" , ot->ot.heat    ),
		Pressure("nPa/s", ot->ot.pressure),
		Oxygen  ("ppq/s", ot->ot.oxygen  , ot->ot.oxygenMultiplier),
		Plants  ("g/s"  , ot->ot.plants  ),
		Insects ("g/s"  , ot->ot.insects , ot->ot.insectsMultiplier),
		Animals ("g/s"  , ot->ot.animals , ot->ot.animalsMultiplier),
		;
		
		final String rateUnit;
		final boolean isMultiplierBased;
		final Function<ObjectType, Double> getMultiplier;
		final Function<ObjectType, Double> getBaseValue;
		
		PhysicalValue(String rateUnit, Function<ObjectType,Double> getBaseValue) {
			this(rateUnit, getBaseValue, null);
		}
		PhysicalValue(String rateUnit, Function<ObjectType,Double> getBaseValue, Function<ObjectType,Double> getMultiplier) {
			this.rateUnit = rateUnit;
			this.getBaseValue = getBaseValue;
			this.isMultiplierBased = getMultiplier!=null;
			this.getMultiplier = getMultiplier;
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
	static final double DEFAULT_BOOSTER_MULTIPLIER = 10;
	
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
				if (     (valueStr=getValue(line,"ObjectType: "           ))!=null ) put(valueStr, currentOT = new ObjectType(valueStr, null));
				if (currentOT != null) {
					if ( (valueStr=getValue(line,"label = "               ))!=null ) currentOT.label_en = valueStr;
					if ( (valueStr=getValue(line,"label_en = "            ))!=null ) currentOT.label_en = valueStr;
					if ( (valueStr=getValue(line,"label_de = "            ))!=null ) currentOT.label_de = valueStr;
					if ( (valueStr=getValue(line,"class = "               ))!=null ) currentOT.class_   = ObjectTypeClass.valueOf_checked(valueStr);
					if ( (valueStr=getValue(line,"heat = "                ))!=null ) currentOT.heat     = parseDouble(valueStr);
					if ( (valueStr=getValue(line,"pressure = "            ))!=null ) currentOT.pressure = parseDouble(valueStr);
					if ( (valueStr=getValue(line,"oxygen = "              ))!=null ) currentOT.oxygen   = parseDouble(valueStr);
					if ( (valueStr=getValue(line,"plants = "              ))!=null ) currentOT.plants   = parseDouble(valueStr);
					if ( (valueStr=getValue(line,"insects = "             ))!=null ) currentOT.insects  = parseDouble(valueStr);
					if ( (valueStr=getValue(line,"animals = "             ))!=null ) currentOT.animals  = parseDouble(valueStr);
					if ( (valueStr=getValue(line,"energy = "              ))!=null ) currentOT.energy   = parseDouble(valueStr);
					if ( (valueStr=getValue(line,"expectsMultiplierFor = "))!=null ) currentOT.expectsMultiplierFor = PhysicalValue.valueOf_checked(valueStr);
					if ( (valueStr=getValue(line,"oxygenMultiplier = "    ))!=null ) currentOT.oxygenMultiplier     = parseDouble(valueStr);
					if ( (valueStr=getValue(line,"insectsMultiplier = "   ))!=null ) currentOT.insectsMultiplier    = parseDouble(valueStr);
					if ( (valueStr=getValue(line,"animalsMultiplier = "   ))!=null ) currentOT.animalsMultiplier    = parseDouble(valueStr);
					if ( (valueStr=getValue(line,"isBoosterRocketFor = "  ))!=null ) currentOT.isBoosterRocketFor   = PhysicalValue.valueOf_checked(valueStr);
					if ( (valueStr=getValue(line,"boosterMultiplier = "   ))!=null ) currentOT.boosterMultiplier    = parseDouble(valueStr);
					if ( (valueStr=getValue(line,"occurrences = "         ))!=null ) Occurrence.parseDataStr(valueStr, currentOT.occurrences);
					
					if (        line.equals(     "isMachineOptomizer"     )        ) currentOT.isMachineOptomizer = true;
					if ( (valueStr=getValue(line,"moRange = "             ))!=null ) currentOT.moRange            = parseDouble (valueStr);
					if ( (valueStr=getValue(line,"moCapacity = "          ))!=null ) currentOT.moCapacity         = parseInteger(valueStr);
					
					if ( (valueStr=getValue(line,"isMOFuse = "            ))!=null ) currentOT.isMOFuse           = PhysicalValue.valueOf_checked(valueStr);
					if ( (valueStr=getValue(line,"moFuseMultiplier = "    ))!=null ) currentOT.moFuseMultiplier   = parseDouble(valueStr);
					
					if (        line.equals(     "isProducer"             )        ) currentOT.isProducer = true;
					if (        line.equals(     "<finished>"             )        ) currentOT.finished = true;
				}
			}
			forEach( (id,ot) -> setDefaultValues(ot) );
			
		} catch (FileNotFoundException ex) {
			//ex.printStackTrace();
		} catch (IOException ex) {
			System.err.printf("IOException while reading ObjectTypes: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}

	private void setDefaultValues(ObjectType ot)
	{
		if (ot.isBoosterRocketFor!=null && ot.boosterMultiplier==null)
			ot.boosterMultiplier = DEFAULT_BOOSTER_MULTIPLIER;
	}

	void writeToFile() {
		System.out.printf("Write ObjectTypes to file \"%s\" ...%n", datafile.getAbsolutePath());
		
		try (PrintWriter out = new PrintWriter(datafile, StandardCharsets.UTF_8)) {
			
			Vector<Entry<String, ObjectType>> vector = new Vector<>(entrySet());
			vector.sort(Comparator.comparing(Entry<String, ObjectType>::getKey,Data.caseIgnoringComparator));
			
			for (Entry<String, ObjectType> entry : vector) {
				ObjectType ot = entry.getValue();
				                                    out.printf("ObjectType: "           +"%s%n", ot.id       );
				if ( ot.label_en            !=null) out.printf("label_en = "            +"%s%n", ot.label_en );
				if ( ot.label_de            !=null) out.printf("label_de = "            +"%s%n", ot.label_de );
				if ( ot.class_              !=null) out.printf("class = "               +"%s%n", ot.class_.name());
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
				if ( ot.boosterMultiplier   !=null) out.printf("boosterMultiplier = "   +"%s%n", ot.boosterMultiplier);
				if (!ot.occurrences.isEmpty()     ) out.printf("occurrences = "         +"%s%n", Occurrence.toDataStr(ot.occurrences));
				
				if ( ot.isMachineOptomizer        ) out.printf("isMachineOptomizer"     +  "%n");
				if ( ot.moRange             !=null) out.printf("moRange = "             +"%s%n", ot.moRange);
				if ( ot.moCapacity          !=null) out.printf("moCapacity = "          +"%s%n", ot.moCapacity);
				
				if ( ot.isMOFuse            !=null) out.printf("isMOFuse = "            +"%s%n", ot.isMOFuse.name());
				if ( ot.moFuseMultiplier    !=null) out.printf("moFuseMultiplier = "    +"%s%n", ot.moFuseMultiplier);
				
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

	private static Integer parseInteger(String str) {
		try { return Integer.parseInt(str); }
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
			Double multiplier = ot==null ? null : getMultiplier.apply(ot);
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

	Vector<ObjectType> collectTypes(Predicate<ObjectType> predicate)
	{
		if (predicate==null) throw new IllegalArgumentException();
		
		Vector<ObjectType> vector = new Vector<>();
		forEach((id,ot) -> {
			if (predicate.test(ot))
				vector.add(ot);
		});
		
		return vector;
	}

	static class ObjectType {
		final String id;
		boolean finished;
		String label_en;
		String label_de;
		ObjectTypeClass class_;
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
		Double boosterMultiplier;
		boolean isMachineOptomizer;
		Double moRange;
		Integer moCapacity;
		PhysicalValue isMOFuse; 
		Double moFuseMultiplier; 
		boolean isProducer;
		final EnumSet<Occurrence> occurrences;
		
		ObjectType(String id, Occurrence occurrence) {
			if (id==null) throw new IllegalArgumentException();
			this.id = id;
			finished = false;
			label_en = null;
			label_de = null;
			class_   = null;
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

		String getLabel() {
			LabelLanguage lang = PlanetCrafterSaveGameViewer.getCurrentLabelLanguage();
			boolean label_en_isOk = label_en!=null && !label_en.isBlank();
			boolean label_de_isOk = label_de!=null && !label_de.isBlank();
			if (lang==LabelLanguage.EN && label_en_isOk) return label_en;
			if (lang==LabelLanguage.DE && label_de_isOk) return label_de;
			if (label_en_isOk) return String.format("[%s]", label_en);
			if (label_de_isOk) return String.format("[%s]", label_de);
			return null;
		}

		String getName() {
			String label = getLabel();
			return label != null ? label : String.format("{%s}", id);
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
		
		static String toString(ObjectType[] objectTypes)
		{
			return Data.toString( Arrays.stream(objectTypes).map(ot -> ot==null ? null : ot.getName()).toArray(String[]::new) );
		}
	}
}
