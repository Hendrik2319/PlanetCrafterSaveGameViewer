package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.MapPanel.MapWorldObjectData;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectTypeCreator;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.Occurrence;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.TerraformingCalculation.NearActiveWorldObject;
import net.schwarzbaer.java.lib.gui.ValueListOutput;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.TraverseException;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Helper.KnownJsonValues;
import net.schwarzbaer.java.lib.jsonparser.JSON_Helper.KnownJsonValuesFactory;

class Data {
	static class NV extends JSON_Data.NamedValueExtra.Dummy {}
	static class  V extends JSON_Data.ValueExtra.Dummy {}
	static final Comparator<String> caseIgnoringComparator = Comparator.nullsLast(Comparator.<String,String>comparing(str->str.toLowerCase()).thenComparing(Comparator.naturalOrder()));
	private static final KnownJsonValuesFactory<NV, V> KJV_FACTORY = new KnownJsonValuesFactory<>("net.schwarzbaer.java.games.planetcrafter.savegameviewer.");

	static Data parse(Vector<Vector<Value<NV, V>>> jsonStructure, ObjectTypeCreator getOrCreateObjectType) {
		try {
			return new Data(jsonStructure, getOrCreateObjectType);
			
		} catch (ParseException ex) {
			System.err.printf("ParseException while parsing JSON structure (Data.parse()): %s%n", ex.getMessage());
			//ex.printStackTrace();
			return null;
			
		} catch (TraverseException ex) {
			System.err.printf("TraverseException while parsing JSON structure (Data.parse()): %s%n", ex.getMessage());
			//ex.printStackTrace();
			return null;
		}
	}
	
	interface RemoveStateListener {
		void someObjectsWereMarkedForRemoval();
	}
	private static Vector<RemoveStateListener> removeStateListeners = new Vector<>();
	static void    addRemoveStateListener(RemoveStateListener l) { removeStateListeners.   add(l); }
	static void removeRemoveStateListener(RemoveStateListener l) { removeStateListeners.remove(l); }
	static void  clearAllRemoveStateListeners() { removeStateListeners.clear(); }
	static void notifyAllRemoveStateListeners() {
		for (RemoveStateListener l : removeStateListeners)
			l.someObjectsWereMarkedForRemoval();
	}

	final AchievedValues achievedValues;
	final PlayerStates playerStates;
	final Vector<WorldObject> worldObjects;
	final Vector<ObjectList> objectLists;
	final GeneralData1 generalData1;
	final Vector<Message> messages;
	final Vector<StoryEvent> storyEvents;
	final GeneralData2 generalData2;
	final Vector<Layer> layers;
	final Vector<GeneratedWreck> generatedWrecks;
	final HashMap<Long,WorldObject> mapWorldObjects;
	final HashMap<Long,ObjectList> mapObjectLists;

	Vector<Vector<String>> toJsonStrs(AchievedValues modifiedAchievedValues) {
		Vector<Vector<String>> blocks = new Vector<>();
		blocks.add(modifiedAchievedValues==null ? Reversable.toJsonStrs(/* 0 */ achievedValues) : Reversable.toJsonStrs(modifiedAchievedValues));
		blocks.add(Reversable.toJsonStrs(/* 1 */ playerStates      ));
		blocks.add(Reversable.toJsonStrs(/* 2 */ worldObjects      ));
		blocks.add(Reversable.toJsonStrs(/* 3 */ objectLists       ));
		blocks.add(Reversable.toJsonStrs(/* 4 */ generalData1      ));
		blocks.add(Reversable.toJsonStrs(/* 5 */ messages          ));
		blocks.add(Reversable.toJsonStrs(/* 6 */ storyEvents       ));
		blocks.add(Reversable.toJsonStrs(/* 7 */ generalData2      ));
		blocks.add(Reversable.toJsonStrs(/* 8 */ layers            ));
		blocks.add(Reversable.toJsonStrs(/* 9 */ generatedWrecks   ));
		return blocks;
	}

	private Data(Vector<Vector<Value<NV, V>>> dataVec, ObjectTypeCreator getOrCreateObjectType) throws ParseException, TraverseException {
		if (dataVec==null) throw new IllegalArgumentException();
		
		KJV_FACTORY.clearStatementList();
		
		System.out.printf("Parsing JSON Structure ...%n");
		int blockIndex = 0;
		/* 0 */ achievedValues  = dataVec.size()<=blockIndex ? null : parseSingle( blockIndex, dataVec.get(blockIndex), AchievedValues::new, "AchievedValues", getOrCreateObjectType); blockIndex++;
		/* 1 */ playerStates    = dataVec.size()<=blockIndex ? null : parseSingle( blockIndex, dataVec.get(blockIndex), PlayerStates  ::new, "PlayerStates"  , getOrCreateObjectType); blockIndex++;
		/* 2 */ worldObjects    = dataVec.size()<=blockIndex ? null : parseArray ( blockIndex, dataVec.get(blockIndex), WorldObject   ::new, "WorldObjects"  , getOrCreateObjectType); blockIndex++;
		/* 3 */ objectLists     = dataVec.size()<=blockIndex ? null : parseArray ( blockIndex, dataVec.get(blockIndex), ObjectList    ::new, "ObjectLists"   , getOrCreateObjectType); blockIndex++;
		/* 4 */ generalData1    = dataVec.size()<=blockIndex ? null : parseSingle( blockIndex, dataVec.get(blockIndex), GeneralData1  ::new, "GeneralData1"                         ); blockIndex++;
		/* 5 */ messages        = dataVec.size()<=blockIndex ? null : parseArray ( blockIndex, dataVec.get(blockIndex), Message       ::new, "Messages"                             ); blockIndex++;
		/* 6 */ storyEvents     = dataVec.size()<=blockIndex ? null : parseArray ( blockIndex, dataVec.get(blockIndex), StoryEvent    ::new, "StoryEvents"                          ); blockIndex++;
		/* 7 */ generalData2    = dataVec.size()<=blockIndex ? null : parseSingle( blockIndex, dataVec.get(blockIndex), GeneralData2  ::new, "GeneralData2"                         ); blockIndex++;
		/* 8 */ layers          = dataVec.size()<=blockIndex ? null : parseArray ( blockIndex, dataVec.get(blockIndex), Layer         ::new, "Layers"                               ); blockIndex++;
		/* 9 */ generatedWrecks = dataVec.size()<=blockIndex ? null : parseArray ( blockIndex, dataVec.get(blockIndex), GeneratedWreck::new, "GeneratedWreck"                       ); blockIndex++;
		
		for (;blockIndex < dataVec.size(); blockIndex++)
		{
			Vector<Value<NV, V>> arr = dataVec.get(blockIndex);
			if (arr==null    ) throw new IllegalStateException("Block %d is null.".formatted(blockIndex));
			if (arr.isEmpty()) throw new IllegalStateException("Block %d is an empty array.".formatted(blockIndex));
			if (arr.size()>1) {
				System.err.printf("Block %d contains more values (=%d) than expected (=1).%n", blockIndex, arr.size());
				continue;
			}
			Value<NV, V> value = arr.get(0);
			if (value!=null) {
				System.err.printf("Block %d contains value.%n", blockIndex);
				continue;
			}
		}
		
		mapWorldObjects = new HashMap<>();
		System.out.printf("Processing Data ...%n");
		for (WorldObject wo : worldObjects) {
			
			if (!mapWorldObjects.containsKey(wo.id))
				mapWorldObjects.put(wo.id, wo);
			else {
				WorldObject other = mapWorldObjects.get(wo.id);
				System.err.printf("Non unique ID in WorldObject: %d (this:\"%s\", other:\"%s\")%n", wo.id, wo.objectTypeID, other.objectTypeID);
				wo.nonUniqueID = true;
				other.nonUniqueID = true;
			}
			
			if (0 < wo.listId)
				for (ObjectList ol : objectLists)
					if (ol.id==wo.listId) {
						ol.container = wo;
						wo.list = ol;
						break;
					}
		}
		
		mapObjectLists = new HashMap<>();
		for (ObjectList ol : objectLists) {
			
			if (!mapObjectLists.containsKey(ol.id))
				mapObjectLists.put(ol.id, ol);
			else {
				ObjectList other = mapObjectLists.get(ol.id);
				System.err.printf("Non unique ID in ObjectList: %d%n", ol.id);
				ol.nonUniqueID = true;
				other.nonUniqueID = true;
			}
			
			ol.worldObjs = generateWorldObjectArray(mapWorldObjects, ol.worldObjIds, wo -> {
				wo.container = ol.container;
				wo.containerList = ol;
			});
		}
		
		for (GeneratedWreck genWreck : generatedWrecks) {
			genWreck.worldObjsGenerated = generateWorldObjectArray(mapWorldObjects, genWreck.worldObjIdsGenerated, null);
			genWreck.worldObjsDropped   = generateWorldObjectArray(mapWorldObjects, genWreck.worldObjIdsDropped  , null);
		}
		
		
		System.out.printf("Done%n");
		
		KJV_FACTORY.showStatementList(System.err, "Unknown Fields in parsed Data");
	}
	
	private static WorldObject[] generateWorldObjectArray(HashMap<Long,WorldObject> mapWorldObjects, int[] worldObjIds, Consumer<WorldObject> postprocessWO)
	{
		return Arrays
				.stream(worldObjIds)
				.mapToObj(woId -> {
					WorldObject wo = mapWorldObjects.get((long)woId);
					if (wo!=null && postprocessWO!=null)
						postprocessWO.accept(wo);
					return wo;
				})
				.toArray(WorldObject[]::new);
	}
	
	interface ParseConstructor1<ValueType> {
		ValueType parse(Value<NV, V> value, String debugLabel) throws ParseException, TraverseException;
	}

	interface ParseConstructor2<ValueType> {
		ValueType parse(Value<NV, V> value, ObjectTypeCreator getOrCreateObjectType, String debugLabel) throws ParseException, TraverseException;
	}

	private static <ValueType> Vector<ValueType> parseArray(
			int blockIndex,
			Vector<Value<NV, V>> vector,
			ParseConstructor2<ValueType> parseConstructor,
			String debugLabel,
			ObjectTypeCreator getOrCreateObjectType
	) throws ParseException, TraverseException {
		return parseArray(blockIndex, vector, (v,dl)->parseConstructor.parse(v, getOrCreateObjectType, dl), debugLabel);
	}
	
	private static <ValueType> Vector<ValueType> parseArray(
			int blockIndex,
			Vector<Value<NV, V>> vector,
			ParseConstructor1<ValueType> parseConstructor,
			String debugLabel
	) throws ParseException, TraverseException {
		Vector<ValueType> parsedVec = new Vector<>();
		for (int i=0; i< vector.size(); i++) {
			Value<NV, V> val = vector.get(i);
			String newDebugLabel = String.format("%s[%d]", debugLabel, i);
			ValueType parsedValue = parseConstructor.parse(val, newDebugLabel);
			parsedVec.add(parsedValue);
		}
		return parsedVec;
	}

	private static <ValueType> ValueType parseSingle(
			int blockIndex,
			Vector<Value<NV, V>> vector,
			ParseConstructor2<ValueType> parseConstructor,
			String debugLabel,
			ObjectTypeCreator getOrCreateObjectType
	) throws ParseException, TraverseException {
		return parseSingle(blockIndex, vector, (v,dl)->parseConstructor.parse(v, getOrCreateObjectType, dl), debugLabel);
	}
	
	private static <ValueType> ValueType parseSingle(
			int blockIndex,
			Vector<Value<NV, V>> vector,
			ParseConstructor1<ValueType> parseConstructor,
			String debugLabel
	) throws ParseException, TraverseException {
		ValueType parsedValue = null;
		
		if (vector.isEmpty())
			System.err.printf("No entries found in block %d.%n", blockIndex);
		else
		{
			if (vector.size()>1)
				System.err.printf("Wrong number of entries found in block %d: Found %d enties, but expected 1 entry. Other entries than last will be ignored.%n", blockIndex, vector.size());
			Value<NV, V> val = vector.lastElement();
			String newDebugLabel = String.format("%s[%d]", debugLabel, vector.size()-1);
			parsedValue = parseConstructor.parse(val, newDebugLabel);
		}
			
		return parsedValue;
	}
	
	private static <ValueType> ValueType[] parseCommaSeparatedArray(
			String str, String debugLabel, String typeLabel,
			IntFunction<ValueType[]> createArray,
			Function<String,ValueType> parseValue)
					throws ParseException {
		return parseSeparatedArray(str, ",", debugLabel, typeLabel, createArray, parseValue);
	}
	
	private static <ValueType> ValueType[] parseSeparatedArray(
			String str, String delimiter, String debugLabel, String typeLabel,
			IntFunction<ValueType[]> createArray,
			Function<String,ValueType> parseValue)
					throws ParseException {
		
		if (str==null) throw new IllegalArgumentException();
		if (debugLabel==null) throw new IllegalArgumentException();
		
		if (str.isEmpty())
			return createArray.apply(0);
		
		String[] parts = str.split(delimiter,-1);
		ValueType[] results = createArray.apply(parts.length);
		for (int i=0; i<parts.length; i++) {
			String part = parts[i];
			results[i] = parseValue.apply(part);
			if (results[i] == null)
				throw new ParseException("%s: Can't convert value %d of list into %s: \"%s\"", debugLabel, i, typeLabel, part);
		}
		
		return results;
	}

	static double[] parseDoubleArray(String str, String debugLabel) throws ParseException {
		Double[] arr1 = parseCommaSeparatedArray(str, debugLabel, "Double", Double[]::new, valStr->{
			try { return Double.parseDouble(valStr); }
			catch (NumberFormatException e) { return null; }
		});
		
		double[] results = new double[arr1.length];
		for (int i=0; i<arr1.length; i++)
			results[i] = arr1[i].doubleValue();
		
		return results;
	}

	static int[] parseIntegerArray(String str, String debugLabel) throws ParseException {
		Integer[] arr1 = parseCommaSeparatedArray(str, debugLabel, "Integer", Integer[]::new, valStr->{
			try { return Integer.parseInt(valStr); }
			catch (NumberFormatException e) { return null; }
		});
		
		int[] results = new int[arr1.length];
		for (int i=0; i<arr1.length; i++)
			results[i] = arr1[i].intValue();
		
		return results;
	}

	static String[] parseStringArray(String str, String debugLabel) throws ParseException
	{
		return parseCommaSeparatedArray(str, debugLabel, "String", String[]::new, str1->str1);
	}

	static ObjectType[] parseObjectTypeArray(String str, ObjectTypeCreator getOrCreateObjectType, Occurrence occurrence, String debugLabel) throws ParseException
	{
		return parseCommaSeparatedArray(str, debugLabel, "ObjectType", ObjectType[]::new, objectTypeID->getOrCreateObjectType.getOrCreate(objectTypeID, occurrence));
	}

	static String toString(String[] strs)
	{
		return strs==null ? "<null>" : strs.length==0 ? "[]" : String.format("[ %s ]", String.join(", ", strs));
	}

	static class ParseException extends Exception {
		private static final long serialVersionUID = 7894187588880980010L;

		ParseException(String format, Object...args) {
			super(String.format(Locale.ENGLISH, format, args));
		}
	}
	
	private static abstract class Reversable {
		
		private enum Marker {
			Nothing, Removal_ByUser, Removal_ByData
		}
		
		private final boolean canBeRemoved;
		private Marker marker;

		Reversable(boolean canBeRemoved) {
			this.canBeRemoved = canBeRemoved;
			marker = Marker.Nothing;
		}

		boolean canMarkedByUser() {
			return canBeRemoved && marker!=Marker.Removal_ByData;
		}
		
		void markForRemoval(boolean isMarkedForRemoval, boolean isMarkedByUser) {
			if (!canBeRemoved && isMarkedForRemoval)
				throw new UnsupportedOperationException();
			if (marker==Marker.Removal_ByData) {
				if (isMarkedByUser)
					throw new IllegalStateException();
				marker = !isMarkedForRemoval ? Marker.Nothing : Marker.Removal_ByData;
			} else {
				marker = !isMarkedForRemoval ? Marker.Nothing : isMarkedByUser ? Marker.Removal_ByUser : Marker.Removal_ByData;
			}
			updateMarkerInChildren(isMarkedForRemoval);
		}
		
		protected void updateMarkerInChildren(boolean isMarkedForRemoval) {}

		boolean isMarkedForRemoval() {
			return marker == Marker.Removal_ByUser || marker == Marker.Removal_ByData;
		}

		abstract String toJsonStrs();

		static Vector<String> toJsonStrs(Reversable singleData) {
			Vector<String> strs = new Vector<>();
			strs.add(singleData.toJsonStrs());
			return strs;
		}

		static Vector<String> toJsonStrs(Vector<? extends Reversable> data) {
			Vector<String> strs = new Vector<>();
			for (Reversable value : data)
				if (!value.isMarkedForRemoval())
					strs.add(value.toJsonStrs());
			return strs;
		}
	}

	static class Color {
	
		final double r;
		final double g;
		final double b;
		final double a;

		Color(String str, String debugLabel) throws ParseException {
			Double[] arr = parseSeparatedArray(str, "-", debugLabel, "Double", Double[]::new, valStr->{
				valStr = valStr.replace(',', '.');
				try { return Double.parseDouble(valStr); }
				catch (NumberFormatException e) { return null; }
			});
			
			if (arr.length!=4)
				throw new ParseException("%s: Parsed value array has wrong length: %d (!=4)", debugLabel, arr.length);
			
			for (int i=0; i<arr.length; i++) {
				double val = arr[i];
				if (val<0 || val>1)
					throw new ParseException("%s: Value %d is not in expected range (0..1): %s", debugLabel, i, val);
			}
			
			r = arr[0];
			g = arr[1];
			b = arr[2];
			a = arr[3];
		}
		
		java.awt.Color getColor() {
			return new java.awt.Color((float)r, (float)g, (float)b, (float)a);
		}

		@Override
		public String toString() {
			return String.format("(r=%s, g=%s, b=%s, a=%s)", r, g, b, a);
		}
		
		
	}
	static class Coord3 {
		private final double x,y,z;
		
		Coord3(String str, String debugLabel) throws ParseException {
			double[] arr =  parseDoubleArray(str, debugLabel);
			if (arr.length!=3) throw new ParseException("%s: Unexpected length of array: %d (!=3)", debugLabel, arr.length);
			x = arr[0];
			y = arr[1];
			z = arr[2];
		}
		
		@Override public String toString() {
			if (isZero()) return "- 0 -";
			//return String.format(Locale.ENGLISH, "%1.5f, %1.5f, %1.5f", x, y, z);
			return String.format(Locale.ENGLISH, "%s, %s, %s", x, y, z);
		}
		
		boolean isZero() {
			return x==0 && y==0 && z==0;
		}
		
		void addTo(ValueListOutput out, int indentLevel) {
			out.add(indentLevel, "X", "%s (Map Y)" , x);
			out.add(indentLevel, "Y", "%s (Height)", y);
			out.add(indentLevel, "Z", "%s (Map X)" , z);
		}
		
		double getMapX() { return z; };
		double getMapY() { return x; };
		
		double getDistanceXZ_m(Coord3 pos) {
			Objects.requireNonNull(pos);
			return Math.sqrt( (x-pos.x)*(x-pos.x) + (z-pos.z)*(z-pos.z) );
		}
		
		double getDistanceXYZ_m(Coord3 pos) {
			Objects.requireNonNull(pos);
			return Math.sqrt( (x-pos.x)*(x-pos.x) + (y-pos.y)*(y-pos.y) + (z-pos.z)*(z-pos.z) );
		}
	}
	
	static class Rotation {
		private final double x,y,z,w;
		Rotation(String str, String debugLabel) throws ParseException {
			double[] arr =  parseDoubleArray(str, debugLabel);
			if (arr.length!=4) throw new ParseException("%s: Unexpected length of array: %d (!=4)", debugLabel, arr.length);
			x = arr[0];
			y = arr[1];
			z = arr[2];
			w = arr[3];
		}
		
		@Override public String toString() {
			if (isZero()) return "- 0 -";
			//return String.format(Locale.ENGLISH, "%1.5f, %1.5f, %1.5f, %1.5f", x, y, z, w);
			return String.format(Locale.ENGLISH, "%s, %s, %s, %s", x, y, z, w);
		}
		
		boolean isZero() {
			return x==0 && y==0 && z==0 && w==0;
		}
		
		void addTo(ValueListOutput out, int indentLevel) {
			out.add(indentLevel, "X", x);
			out.add(indentLevel, "Y", y);
			out.add(indentLevel, "Z", z);
			out.add(indentLevel, "W", w);
		}
		
		AffineTransform computeMapTransform() {
			/*
			https://de.wikipedia.org/wiki/Quaternion#Drehungen_im_dreidimensionalen_Raum
			
			q = q0 + q1*i + q2*j + q3*k
			
			     q0² + q1² - q2² - q3²    -2*q0q3 + 2*q1q2        2*q0q2 + 2*q1q3
			D =    2*q0q3 + 2*q1q2      q0² - q1² + q2² - q3²    -2*q0q1 + 2*q2q3   
			      -2*q0q2 + 2*q1q3         2*q0q1 + 2*q2q3     q0² - q1² - q2² + q3²
			      
			q = w + x*i + y*j + z*k
			
			     w² + x² - y² - z²    -2*wz + 2*xy        2*wy + 2*xz
			D =    2*wz + 2*xy      w² - x² + y² - z²    -2*wx + 2*yz   
			      -2*wy + 2*xz         2*wx + 2*yz     w² - x² - y² + z²
			
			     D11  D12  D13
			  =  D21  D22  D23
			     D31  D32  D33
			
			      [ x']   [  D11  D12  D13  ] [ x ]   [ D11*x + D12*y + D13*z ]
			      [ y'] = [  D21  D22  D23  ] [ y ] = [ D21*x + D22*y + D23*z ]
			      [ z']   [  D31  D32  D33  ] [ z ]   [ D31*x + D32*y + D33*z ]
			
			MapX -> z
			MapY -> x
			
			      [ MapY']   [  D11  D12  D13  ] [ MapY ]   [ D11*MapY + D12*0 + D13*MapX ]
			      [  0   ] = [  D21  D22  D23  ] [  0   ] = [ D21*MapY + D22*0 + D23*MapX ]
			      [ MapX']   [  D31  D32  D33  ] [ MapX ]   [ D31*MapY + D32*0 + D33*MapX ]
			
			      [ MapY']   [  D11  D13  ] [ MapY ]   [ D11*MapY + D13*MapX ]
			      [ MapX']   [  D31  D33  ] [ MapX ]   [ D31*MapY + D33*MapX ]
			
			      [ MapX']   [  D33  D31  ] [ MapX ]   [ D33*MapX + D31*MapY ]
			      [ MapY']   [  D13  D11  ] [ MapY ]   [ D13*MapX + D11*MapY ]
			 */
			
			double D11 = w*w + x*x - y*y - z*z; // w² + x² - y² - z²
			double D33 = w*w - x*x - y*y + z*z; // w² - x² - y² + z²
			double D13 =  2*w*y + 2*x*z; //  2*wy + 2*xz
			double D31 = -2*w*y + 2*x*z; // -2*wy + 2*xz
			
			/*
			      [ x']   [  m00  m01  m02  ] [ x ]   [ m00x + m01y + m02 ]
			      [ y'] = [  m10  m11  m12  ] [ y ] = [ m10x + m11y + m12 ]
			      [ 1 ]   [   0    0    1   ] [ 1 ]   [         1         ]
			AffineTransform(double m00, double m10, double m01, double m11, double m02, double m12)
			
			MapX -> z   =>  m00 = D33, m01 = D31
			MapY -> x   =>  m11 = D11, m10 = D13
			Rotation only  =>  m02 = 0, m12 = 0
			
			      [ x']   [  D33  D31   0   ] [ x ]   [ D33*x + D31*y + 0 ]
			      [ y'] = [  D13  D11   0   ] [ y ] = [ D13*x + D11*y + 0 ]
			      [ 1 ]   [   0    0    1   ] [ 1 ]   [         1         ]
			 */
			
			return new AffineTransform(D33, D13, D31, D11, 0, 0);
		}
	}
	static class AchievedValues extends Reversable {
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(AchievedValues.class)
				.add("unitOxygenLevel"    , Value.Type.Float)
				.add("unitHeatLevel"      , Value.Type.Float)
				.add("unitPressureLevel"  , Value.Type.Float)
		//		.add("unitBiomassLevel"   , Value.Type.Float)
				.add("unitPlantsLevel"    , Value.Type.Float)
				.add("unitInsectsLevel"   , Value.Type.Float)
				.add("unitAnimalsLevel"   , Value.Type.Float)
				.add("terraTokens"        , Value.Type.Integer)
				.add("allTimeTerraTokens" , Value.Type.Integer)
				.add("unlockedGroups"        , Value.Type.String )
				.add("openedInstanceSeed"    , Value.Type.Integer)
				.add("openedInstanceTimeLeft", Value.Type.Integer);
		
		final double oxygenLevel;
		final double heatLevel;
		final double pressureLevel;
		//final double biomassLevel;
		final double plantsLevel ;
		final double insectsLevel;
		final double animalsLevel;
		final long terraTokens;
		final long allTimeTerraTokens;

		final Long openedInstanceSeed;
		final Long openedInstanceTimeLeft;
		final String[] unlockedGroups;
		final ObjectType[] unlockedObjectTypes;
		
		private final String unlockedGroupsStr;
		
		/*
			Block[0]: 1 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [4]
			        unitBiomassLevel:Float
			        unitHeatLevel:Float
			        unitOxygenLevel:Float
			        unitPressureLevel:Float
			 * Insecs Update
			        unitPlantsLevel :Float
			        unitInsectsLevel:Float
			        unitAnimalsLevel:Float
			 * Trading Update
			        allTimeTerraTokens:Integer
			        terraTokens:Integer
			 * V 1.0
			        openedInstanceSeed:Integer
			        openedInstanceTimeLeft:Integer
			        unlockedGroups:String
		 */
		AchievedValues(Value<NV, V> value, ObjectTypeCreator getOrCreateObjectType, String debugLabel) throws TraverseException, ParseException {
			super(false);
			
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			oxygenLevel        = JSON_Data.getFloatValue  (object, "unitOxygenLevel"    , debugLabel);
			heatLevel          = JSON_Data.getFloatValue  (object, "unitHeatLevel"      , debugLabel);
			pressureLevel      = JSON_Data.getFloatValue  (object, "unitPressureLevel"  , debugLabel);
		//	biomassLevel       = JSON_Data.getFloatValue  (object, "unitBiomassLevel"   , debugLabel);
			plantsLevel        = JSON_Data.getFloatValue  (object, "unitPlantsLevel"    , debugLabel);
			insectsLevel       = JSON_Data.getFloatValue  (object, "unitInsectsLevel"   , debugLabel);
			animalsLevel       = JSON_Data.getFloatValue  (object, "unitAnimalsLevel"   , debugLabel);
			terraTokens        = JSON_Data.getIntegerValue(object, "terraTokens"        , debugLabel);
			allTimeTerraTokens = JSON_Data.getIntegerValue(object, "allTimeTerraTokens" , debugLabel);
			
			unlockedGroupsStr      = JSON_Data.getStringValue (object, "unlockedGroups"        , true, false, debugLabel);
			openedInstanceSeed     = JSON_Data.getIntegerValue(object, "openedInstanceSeed"    , true, false, debugLabel);
			openedInstanceTimeLeft = JSON_Data.getIntegerValue(object, "openedInstanceTimeLeft", true, false, debugLabel);
			
			if (unlockedGroupsStr!=null)
			{
				unlockedGroups = parseStringArray(unlockedGroupsStr, debugLabel+".unlockedGroups");
				unlockedObjectTypes = new ObjectType[unlockedGroups.length];
				for (int i=0; i<unlockedGroups.length; i++)
					unlockedObjectTypes[i] = getOrCreateObjectType.getOrCreate( unlockedGroups[i], Occurrence.Blueprint );
			}
			else
			{
				unlockedGroups = null;
				unlockedObjectTypes = null; 
			}
			
			KNOWN_JSON_VALUES.scanUnexpectedValues(object);
		}

		AchievedValues(double oxygenLevel, double heatLevel, double pressureLevel,
				double plantsLevel, double insectsLevel, double animalsLevel,
				long terraTokens, long allTimeTerraTokens,
				AchievedValues remainingValues) {
			super(false);
			this.oxygenLevel = oxygenLevel;
			this.heatLevel = heatLevel;
			this.pressureLevel = pressureLevel;
			this.plantsLevel = plantsLevel;
			this.insectsLevel = insectsLevel;
			this.animalsLevel = animalsLevel;
			this.terraTokens = terraTokens;
			this.allTimeTerraTokens = allTimeTerraTokens;
			
			this.openedInstanceSeed     = remainingValues==null ? null : remainingValues.openedInstanceSeed    ;
			this.openedInstanceTimeLeft = remainingValues==null ? null : remainingValues.openedInstanceTimeLeft;
			this.unlockedGroupsStr      = remainingValues==null ? null : remainingValues.unlockedGroupsStr     ;
			this.unlockedGroups         = remainingValues==null ? null : copyOf(remainingValues.unlockedGroups     );
			this.unlockedObjectTypes    = remainingValues==null ? null : copyOf(remainingValues.unlockedObjectTypes);
		}
		
		private static <Val> Val[] copyOf(Val[] arr) {
			return arr==null ? null : Arrays.copyOf(arr, arr.length);
		}

		@Override String toJsonStrs() {
			return toJsonStr( removeNulls(
					toFloatValueStr  ("unitOxygenLevel"   , oxygenLevel  , "%1.8f"),
					toFloatValueStr  ("unitHeatLevel"     , heatLevel    , "%1.8f"),
					toFloatValueStr  ("unitPressureLevel" , pressureLevel, "%1.8f"),
					toFloatValueStr  ("unitPlantsLevel"   , plantsLevel  , "%1.8f"),
					toFloatValueStr  ("unitInsectsLevel"  , insectsLevel , "%1.8f"),
					toFloatValueStr  ("unitAnimalsLevel"  , animalsLevel , "%1.8f"),
					toIntegerValueStr("terraTokens"       , terraTokens           ),
					toIntegerValueStr("allTimeTerraTokens", allTimeTerraTokens    ),
					toStringValueStr ("unlockedGroups"        , unlockedGroupsStr     , true),
					toIntegerValueStr("openedInstanceSeed"    , openedInstanceSeed    , true),
					toIntegerValueStr("openedInstanceTimeLeft", openedInstanceTimeLeft, true)
			) );
		}

		double getTerraformLevel() {
			return oxygenLevel
					+ heatLevel
					+ pressureLevel
					+ plantsLevel
					+ insectsLevel
					+ animalsLevel;
		}

		double getBiomassLevel() {
			return plantsLevel
					+ insectsLevel
					+ animalsLevel;
		}

		private static String formatValue(String format, Object value) {
			return String.format(Locale.ENGLISH, format, value);
		}

		static String formatTerraformation(double value) {
			if (value < 2000) return formatValue("%1.2f Ti", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f kTi", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f MTi", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f GTi", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f TTi", value);
			value/=1000;
			return formatValue("%1.2f PTi", value);
		}

		static String formatOxygenLevel(double value) {
			if (value < 2000) return formatValue("%1.2f ppq", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f ppt", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f ppb", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f ppm", value);
			value/=1000;
			if (value < 20  ) return formatValue("%1.2f ‰", value);
			value/=10;
			return formatValue("%1.2f %%", value);
		}

		static String formatHeatLevel(double value) {
			if (value < 2000) return formatValue("%1.2f pK", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f nK", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f µK", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f mK", value);
			value/=1000;
			return formatValue("%1.2f K", value);
		}

		static String formatPressureLevel(double value) {
			if (value < 2000) return formatValue("%1.2f nPa", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f µPa", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f mPa", value);
			value/=1000;
			if (value < 200 ) return formatValue("%1.2f Pa", value);
			value/=100;
			return formatValue("%1.2f hPa", value);
		}

		static String formatBiomassLevel(double value) {
			if (value < 2000) return formatValue("%1.2f g", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f kg", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f t", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f kt", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f Mt", value);
			value/=1000;
			if (value < 2000) return formatValue("%1.2f Gt", value);
			value/=1000;
			return formatValue("%1.2f Tt", value);
		}

		static String formatTerraTokens(long value) {
			if (value < 2000) return formatValue("%d T", value);
			value/=1000;
			if (value < 2000) return formatValue("%d kT", value);
			value/=1000;
			return formatValue("%d Mio.T", value);
		}
	}

	static class PlayerStates extends Reversable {
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(PlayerStates.class)
				// V 1.0 values
				.add("id"         , Value.Type.Integer)
				.add("name"       , Value.Type.String )
				.add("inventoryId", Value.Type.Integer)
				.add("equipmentId", Value.Type.Integer)
				.add("host"       , Value.Type.Bool   )
				// old values
				.add("playerGaugeHealth", Value.Type.Float)
				.add("playerGaugeOxygen", Value.Type.Float)
				.add("playerGaugeThirst", Value.Type.Float)
				.add("playerPosition"   , Value.Type.String)
				.add("playerRotation"   , Value.Type.String)
				.add("unlockedGroups"   , Value.Type.String);
				
		
		// V 1.0 values
		final Long id;
		final String name;
		final Long inventoryId;
		final Long equipmentId;
		final Boolean isHost;
		
		// old values
		final double health;
		final double oxygen;
		final double thirst;
		final Coord3 position;
		final Rotation rotation;
		final String[] unlockedGroups;
		final ObjectType[] unlockedObjectTypes;
		private final String positionStr;
		private final String rotationStr;
		private final String unlockedGroupsStr;

		/*
			Block[1]: 1 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [6]
			        playerGaugeHealth:Float
			        playerGaugeOxygen:Float
			        playerGaugeThirst:Float
			        playerPosition:String
			        playerRotation:String
			        unlockedGroups:String
		 */
		PlayerStates(Value<NV, V> value, ObjectTypeCreator getOrCreateObjectType, String debugLabel) throws TraverseException, ParseException {
			super(false);
			
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			// "id"         :76561198016584395
			// "name"       :"SchwarzBaer"
			// "inventoryId":3
			// "equipmentId":4
			
			id          = JSON_Data.getIntegerValue(object, "id"         , true, false, debugLabel);
			name        = JSON_Data.getStringValue (object, "name"       , true, false, debugLabel);
			inventoryId = JSON_Data.getIntegerValue(object, "inventoryId", true, false, debugLabel);
			equipmentId = JSON_Data.getIntegerValue(object, "equipmentId", true, false, debugLabel);
			isHost      = JSON_Data.getBoolValue   (object, "host"       , true, false, debugLabel);
			
			health            = JSON_Data.getFloatValue (object, "playerGaugeHealth", debugLabel);
			oxygen            = JSON_Data.getFloatValue (object, "playerGaugeOxygen", debugLabel);
			thirst            = JSON_Data.getFloatValue (object, "playerGaugeThirst", debugLabel);
			positionStr       = JSON_Data.getStringValue(object, "playerPosition"   , debugLabel);
			rotationStr       = JSON_Data.getStringValue(object, "playerRotation"   , debugLabel);
			unlockedGroupsStr = JSON_Data.getStringValue(object, "unlockedGroups"   , true, false, debugLabel);
			
			position = new Coord3  (positionStr, debugLabel+".playerPosition");
			rotation = new Rotation(rotationStr, debugLabel+".playerRotation");
			
			if (unlockedGroupsStr!=null)
			{
				unlockedGroups = parseStringArray(unlockedGroupsStr, debugLabel+".unlockedGroups");
				unlockedObjectTypes = new ObjectType[unlockedGroups.length];
				for (int i=0; i<unlockedGroups.length; i++)
					unlockedObjectTypes[i] = getOrCreateObjectType.getOrCreate( unlockedGroups[i], Occurrence.Blueprint );
			}
			else
			{
				unlockedGroups = null;
				unlockedObjectTypes = null; 
			}
			
			KNOWN_JSON_VALUES.scanUnexpectedValues(object);
		}
		
		@Override String toJsonStrs() {
			return toJsonStr( removeNulls(
					toIntegerValueStr("id"         , id         , true),
					toStringValueStr ("name"       , name       , true),
					toIntegerValueStr("inventoryId", inventoryId, true),
					toIntegerValueStr("equipmentId", equipmentId, true),
					
					toStringValueStr("playerPosition"   , positionStr      ),
					toStringValueStr("playerRotation"   , rotationStr      ),
					toStringValueStr("unlockedGroups"   , unlockedGroupsStr, true),
					toFloatValueStr ("playerGaugeOxygen", oxygen, "%1.1f"  ),
					toFloatValueStr ("playerGaugeThirst", thirst, "%1.6f"  ),
					toFloatValueStr ("playerGaugeHealth", health, "%1.6f"  ),
					
					toBoolValueStr   ("host", isHost, true)
			) );
		}

		boolean isPositioned() {
			return !rotation.isZero() || !position.isZero();
		}
	}
	
	static class WorldObject extends Reversable {
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(WorldObject.class)
				.add("id"    , Value.Type.Integer)
				.add("gId"   , Value.Type.String )
				.add("siIds" , Value.Type.String )
				.add("liId"  , Value.Type.Integer)
				.add("liGrps", Value.Type.String )
				.add("pos"   , Value.Type.String )
				.add("rot"   , Value.Type.String )
				.add("wear"  , Value.Type.Integer)
				.add("pnls"  , Value.Type.String )
				.add("color" , Value.Type.String )
				.add("text"  , Value.Type.String )
				.add("grwth" , Value.Type.Integer)
				.add("set"   , Value.Type.Integer)
				.add("trtInd", Value.Type.Integer)
				.add("trtVal", Value.Type.Integer)
				;
		
		final long     id;
		final String   objectTypeID;
		final long     listId;
		final String   _siIds;
		final String   productsStr;
		final Coord3   position;
		final String   positionStr;
		final Rotation rotation;
		final String   rotationStr;
		final long     _wear;
		final String   mods;
		final String   colorStr;
		final Color    color;
		final String   text;
		final long     growth;
		final Long     _set;
		final Long     _trtInd;
		final Long     _trtVal;
		
		final ObjectType objectType;
		final String[]   productIDs;
		final ObjectType[] products;
		
		boolean        nonUniqueID; // is <id> unique over all WorldObjects
		ObjectList     list; // list associated with listId
		WorldObject    container; // container, it is containing this object
		ObjectList     containerList; // list, that contains this object
		final MapWorldObjectData mapWorldObjectData;
		
		/*
			Block[2]: 3033 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [11]
			        id    :Integer
			        gId   :String
			        liId  :Integer
			        liGrps:String
			        pos   :String
			        rot   :String
			        wear  :Integer
			        pnls  :String
			        color :String
			        text  :String
			        grwth :Integer
		 */
		WorldObject(Value<NV, V> value, ObjectTypeCreator getOrCreateObjectType, String debugLabel) throws TraverseException, ParseException {
			super(true);
			
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			id           = JSON_Data.getIntegerValue(object, "id"    , debugLabel);
			objectTypeID = JSON_Data.getStringValue (object, "gId"   , debugLabel);
			listId       = JSON_Data.getIntegerValue(object, "liId"  , debugLabel);
			_siIds       = JSON_Data.getStringValue (object, "siIds" , true, false, debugLabel);
			productsStr  = JSON_Data.getStringValue (object, "liGrps", debugLabel);
			positionStr  = JSON_Data.getStringValue (object, "pos"   , debugLabel);
			rotationStr  = JSON_Data.getStringValue (object, "rot"   , debugLabel);
			_wear        = JSON_Data.getIntegerValue(object, "wear"  , debugLabel);
			mods         = JSON_Data.getStringValue (object, "pnls"  , debugLabel);
			colorStr     = JSON_Data.getStringValue (object, "color" , debugLabel); // "1-1-1-1" in OutsideLamp1
			text         = JSON_Data.getStringValue (object, "text"  , debugLabel);
			growth       = JSON_Data.getIntegerValue(object, "grwth" , debugLabel);
			_set         = JSON_Data.getIntegerValue(object, "set"   , true, false, debugLabel);
			_trtInd      = JSON_Data.getIntegerValue(object, "trtInd", true, false, debugLabel);
			_trtVal      = JSON_Data.getIntegerValue(object, "trtVal", true, false, debugLabel);
			
			KNOWN_JSON_VALUES.scanUnexpectedValues(object);
			
			position     = new Coord3  (positionStr, debugLabel+".pos");
			rotation     = new Rotation(rotationStr, debugLabel+".rot");
			color        = colorStr.isEmpty() ? null : new Color(colorStr, debugLabel+".color");
			
			objectType   = getOrCreateObjectType.getOrCreate(objectTypeID, Occurrence.WorldObject);
			
			productIDs = productsStr.isEmpty() ? new String[0] : parseStringArray(productsStr, debugLabel+".liGrps");
			products   = new ObjectType[productIDs.length];
			for (int i=0; i<productIDs.length; i++)
				products[i] = productIDs[i].isEmpty()
					? null
					: getOrCreateObjectType.getOrCreate(productIDs[i], Occurrence.Product);
			
			list          = null;
			container     = null;
			containerList = null;
			mapWorldObjectData = new MapWorldObjectData();
			
			// -------------------------------------------------------------------
			//      show special values in fields
			// -------------------------------------------------------------------
			//boolean _liGrpsIsNotEmpty = !productID.isEmpty();
			boolean _wearIsNotZero    =  _wear  !=0;
			//boolean _pnlsIsNotEmpty   = !mods.isEmpty();
			//boolean _colorIsNotEmpty  = !colorStr.isEmpty();
			if (//_pnlsIsNotEmpty   ||
				//_colorIsNotEmpty  ||
				//_liGrpsIsNotEmpty ||
				_wearIsNotZero     ) {
				Vector<String> vars = new Vector<>();
				//if (_liGrpsIsNotEmpty) vars.add("_liGrps");
				if (_wearIsNotZero   ) vars.add("_wear");
				//if (_pnlsIsNotEmpty  ) vars.add("_pnls");
				//if (_colorIsNotEmpty ) vars.add("_color");
				vars.sort(null);
				System.err.printf("Special WorldObject: { "+
						"id"    +":"+  "%d"  +", "+  // Int  
						"gId"   +":"+"\"%s\""+", "+  // Str
						"liId"  +":"+  "%d"  +", "+  // Int
						"siIds" +":"+"\"%s\""+", "+  // Str
						"liGrps"+":"+"\"%s\""+", "+  // Str
						"pos"   +":"+"\"%s\""+", "+  // Str
						"rot"   +":"+"\"%s\""+", "+  // Str
						"wear"  +":"+  "%d"  +", "+  // Int
						"pnls"  +":"+"\"%s\""+", "+  // Str
						"color" +":"+"\"%s\""+", "+  // Str
						"text"  +":"+"\"%s\""+", "+  // Str
						"grwth" +":"+  "%d"  +", "+  // Int
						"set"   +":"+  "%d"  +       // Int
						"trtInd"+":"+  "%d"  +       // Int
						"trtVal"+":"+  "%d"  +       // Int
						" }, special vars: %s%n", 
						id          ,
						objectTypeID,
						listId      ,
						_siIds      ,
						productsStr ,
						positionStr ,
						rotationStr ,
						_wear       ,
						mods        ,
						colorStr    ,
						text        ,
						growth      ,
						_set        ,
						_trtInd     ,
						_trtVal     ,
						String.join(", ", vars)
						);
			}
		}
		
		@Override String toJsonStrs() {
			return toJsonStr( removeNulls(
					toIntegerValueStr("id"    , id          ),
					toStringValueStr ("gId"   , objectTypeID),
					toIntegerValueStr("liId"  , listId      ),
					toStringValueStr ("siIds" , _siIds      , true),
					toStringValueStr ("liGrps", productsStr ),
					toStringValueStr ("pos"   , positionStr ),
					toStringValueStr ("rot"   , rotationStr ),
					toIntegerValueStr("wear"  , _wear       ),
					toStringValueStr ("pnls"  , mods        ),
					toStringValueStr ("color" , colorStr    ),
					toStringValueStr ("text"  , text        ),
					toIntegerValueStr("grwth" , growth      ),
					toIntegerValueStr("set"   , _set        , true),
					toIntegerValueStr("trtInd", _trtInd     , true),
					toIntegerValueStr("trtVal", _trtVal     , true)
			) );
		}

		@Override
		protected void updateMarkerInChildren(boolean isMarkedForRemoval) {
			if (list!=null)
				list.markForRemoval(isMarkedForRemoval, false);
		}

		String getName() {
			if (objectType!=null)
				return objectType.getName();
			if (objectTypeID!=null)
				return String.format("{%s}", objectTypeID);
			return String.format("[%d]", id);
		}
		
		String generateOutput() {
			ValueListOutput out = new ValueListOutput();
			out.add(0, "Name", getName());
			out.add(0, "ID", id);
			if (nonUniqueID) out.add(0, null, "%s", "is not unique");
			out.add(0, "ObjectTypeID", objectTypeID);
			
			if (!text.isEmpty())
				out.add(0, "Text", text);
			
			if (growth>0)
				out.add(0, "Growth", "%d%%", growth);
			
			if (color!=null) {
				out.add(0, "Color");
				out.add(1, null, "%s", color);
				out.add(1, null, "\"%s\"", colorStr);
			} else if (!colorStr.isEmpty())
				out.add(0, "Color", colorStr);
			
			if (!position.isZero()) { out.add(0, "Position"); position.addTo(out,1); }
			if (!rotation.isZero()) { out.add(0, "Rotation"); rotation.addTo(out,1); }
			
			if (containerList!=null) {
				out.add(0, "Is IN a Container");
				if (container==null)
					out.add(1, null, "<UnknownContainer> [List:%d]", containerList.id);
				else
					container.addShortDescTo(out, 1);
			}
			
			if (objectType!=null)
			{
				if (objectType.hasEffectOnTerraforming())
				{
					out.add(0, "Effect on Terraforming");
					PlanetCrafterSaveGameViewer.terraformingCalculation.foreachAWO(this, false, (phVal,awo) -> {
						if (awo != null)
							generateActiveOutputLine(out, 1, phVal.toString(), phVal::formatRate, awo.baseValue, awo.multiplier, awo.moMulti);
						else
						{
							Double baseValue = phVal.getBaseValue.apply(objectType);
							if (baseValue!=null && objectType.expectsMultiplierFor == phVal)
								generateNotFullActiveOutputLine(out, 1, phVal.toString(), phVal::formatRate, baseValue, "expects multiplier item");
						}
					});
				}
				if (objectType.energy !=null)
				{
					out.add(0, "Is Active");
					generateActiveOutputLine(out, 1, "Energy", ObjectTypes::formatEnergyRate, objectType.energy, null, null);
				}
			}
			
			PlanetCrafterSaveGameViewer.terraformingCalculation.foreachAWO(this, true, (phVal, awo) -> {
				if (!awo.nearMachineOptimizers.isEmpty())
				{
					out.add(0, "Near MachineOptimizers (%s)".formatted(phVal));
					awo.nearMachineOptimizers
						.stream()
						.sorted( Comparator.<TerraformingCalculation.NearMachineOptimizer,Double>comparing(nmo->nmo.distance()) )
						.forEach( nmo -> {
							String distStr = String.format(Locale.ENGLISH, "%1.2f m", nmo.distance());
							WorldObject wo = nmo.amo().wo();
							if (wo!=null)
								out.add(1, distStr, "%s", wo.getShortDesc());
						} );
				}
			});
			
			PlanetCrafterSaveGameViewer.terraformingCalculation.foreachAMO(this, true, (phVal, amo)-> {
				Vector<NearActiveWorldObject> nearAWOs = amo.nearAWOs();
				if (!nearAWOs.isEmpty())
				{
					out.add(0, "Is MachineOptimizer (%s) for".formatted(phVal));
					nearAWOs.forEach(nawo -> {
						String distStr = String.format(Locale.ENGLISH, "%1.2f m", nawo.distance());
						WorldObject wo = nawo.awo().wo;
						if (wo!=null)
							out.add(1, distStr, "%s", wo.getShortDesc());
					});
				}
			});
			
			if (products!=null && products.length>0)
				generateOutput(out, 0, "Products", products);
			else if (productIDs!=null && productIDs.length>0)
				out.add(0, "Products", "%s", Data.toString(productIDs));
			else if (productsStr!=null && !productsStr.isEmpty())
				out.add(0, "Products", "{ %s }", productsStr);
			
			if (listId>0) {
				out.add(0, "Is a Container");
				out.add(1, "List-ID", "%d%s", listId, list==null ? "(no list found)" : "");
				if (list!=null) {
					out.add(1, "Size", "%d", list.size);
					out.add(1, "Content", "%d items", list.worldObjs.length);
					Vector<Map.Entry<String, Integer>> content = list.getContentResume();
					for (Map.Entry<String, Integer> entry : content)
						out.add(2, null, "%dx %s", entry.getValue(), entry.getKey());
					generateOutput(out, 1, "Demand", list.demandItems);
					generateOutput(out, 1, "Supply", list.supplyItems);
				}
			}
			
			return out.generateOutput();
		}
		
		private static void generateActiveOutputLine(ValueListOutput out, int indentLevel, String label, Function<Double,String> formatRate, double rate, Double multiplier, Double moMulti) {
			if (multiplier!=null && moMulti!=null)
			{
				out.add(indentLevel, label, "%s", String.format(Locale.ENGLISH, "(%1.2f + %1.2f) x %s", multiplier, moMulti, formatRate.apply(rate)));
				out.add(indentLevel,  null, "%s", formatRate.apply(rate*(multiplier+moMulti)));
			}
			else if (multiplier!=null || moMulti!=null)
			{
				double x = multiplier!=null ? multiplier : moMulti!=null ? moMulti : 1;
				out.add(indentLevel, label, "%s", String.format(Locale.ENGLISH, "%1.2f x %s", x, formatRate.apply(rate)));
				out.add(indentLevel,  null, "%s", formatRate.apply(rate*x));
			}
			else
				out.add(indentLevel, label, "%s", formatRate.apply(rate));
		}
		
		
		private static void generateNotFullActiveOutputLine(ValueListOutput out, int indentLevel, String label, Function<Double,String> formatRate, double rate, String reason) {
			out.add(indentLevel, label, "(%s) -> %s", formatRate.apply(rate), reason);
		}

		private static void generateOutput(ValueListOutput out, int indentLevel, String label, ObjectType[] items)
		{
			if (items!=null && items.length>0)
			{
				out.add(indentLevel, label);
				for (ObjectType item : items)
					out.add(indentLevel+1, item.getName());
			}
		}

		static ObjectType[] getObjectTypes(WorldObject[] worldObjs) {
			ObjectType[] ots = new ObjectType[worldObjs==null ? 0 : worldObjs.length];
			for (int i=0; i<ots.length; i++) {
				WorldObject wo = worldObjs[i];
				ots[i] = wo==null ? null : wo.objectType;
			}
			return ots;
		}

		boolean isInstalled() {
			return !rotation.isZero() || !position.isZero();
		}
		
		static boolean isInstalled(WorldObject wo) {
			return wo!=null && wo.isInstalled();
		}

		String getShortDesc() {
			return String.format("%s (\"%s\", Pos:%s)", getName(), text, position);
		}

		public void addShortDescTo(ValueListOutput out, int indentLevel) {
			out.add(indentLevel, "Name", getName());
			out.add(indentLevel, "Text", text);
			out.add(indentLevel, "Position");
			position.addTo(out,indentLevel+1);
		}

		public String getContainerLabel() {
			if (containerList==null)
				return null;
			if (container==null) {
				if (containerList.id > 0xFFFFFFFFL)
					return String.format("<UnknownContainer> [List:%d]", containerList.id);
				switch ((int) containerList.id) {
					case 1: return "Player Inventory (old)";
					case 2: return "Player Equipment (old)";
					case 3: return "Player Inventory";
					case 4: return "Player Equipment";
					default: return String.format("<UnknownContainer> [List:%d]", containerList.id);
				}
			}
			//return String.format("%s (\"%s\", Pos:%s)", row.container.objType, row.container.text, row.container.position);
			return container.getShortDesc();
		}
	}

	static class ObjectList extends Reversable {
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(ObjectList.class)
				.add("id"        , Value.Type.Integer)
				.add("size"      , Value.Type.Integer)
				.add("woIds"     , Value.Type.String )
				.add("demandGrps", Value.Type.String )
				.add("supplyGrps", Value.Type.String )
				.add("priority"  , Value.Type.Integer);
		
		final long id;
		final long size;
		final int[] worldObjIds;
		WorldObject[] worldObjs;
		WorldObject container; // container using this list
		private final String woIdsStr;
		boolean nonUniqueID; // is <id> unique over all ObjectLists
		final Long dronePrio;
		final String demandItemsStr;
		final String supplyItemsStr;
		final ObjectType[] demandItems;
		final ObjectType[] supplyItems;

		/*
			Block[3]: 221 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [3]
			        id:Integer
			        size:Integer
			        woIds:String
		 */
		ObjectList(Value<NV, V> value, ObjectTypeCreator getOrCreateObjectType, String debugLabel) throws TraverseException, ParseException {
			super(true);
			
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			id             = JSON_Data.getIntegerValue(object, "id"        , debugLabel);
			size           = JSON_Data.getIntegerValue(object, "size"      , debugLabel);
			woIdsStr       = JSON_Data.getStringValue (object, "woIds"     , debugLabel);
			demandItemsStr = JSON_Data.getStringValue (object, "demandGrps", true, false, debugLabel);
			supplyItemsStr = JSON_Data.getStringValue (object, "supplyGrps", true, false, debugLabel);
			dronePrio      = JSON_Data.getIntegerValue(object, "priority"  , true, false, debugLabel);
			
			KNOWN_JSON_VALUES.scanUnexpectedValues(object);
			
			worldObjIds = parseIntegerArray(woIdsStr, debugLabel+".woIds");
			worldObjs = null; // will be set in post processing at end of Data constructor
			container = null;
			
			demandItems = demandItemsStr==null ? null : parseObjectTypeArray(demandItemsStr, getOrCreateObjectType, Occurrence.ObjectList, debugLabel+".demandGrps");
			supplyItems = supplyItemsStr==null ? null : parseObjectTypeArray(supplyItemsStr, getOrCreateObjectType, Occurrence.ObjectList, debugLabel+".supplyGrps");
		}

		@Override String toJsonStrs() {
			return toJsonStr( removeNulls(
					toIntegerValueStr("id"   , id      ),
					toStringValueStr ("woIds", woIdsStr),
					toIntegerValueStr("size" , size    ),
					toStringValueStr ("demandGrps", demandItemsStr, true),
					toStringValueStr ("supplyGrps", supplyItemsStr, true),
					toIntegerValueStr("priority"  , dronePrio     , true)
			) );
		}

		@Override
		protected void updateMarkerInChildren(boolean isMarkedForRemoval) {
			if (worldObjs==null) return;
			for (WorldObject wo : worldObjs)
				wo.markForRemoval(isMarkedForRemoval, false);
		}

		Vector<Map.Entry<String,Integer>> getContentResume() {
			HashMap<String,Integer> content = new HashMap<>();
			for (WorldObject wo : worldObjs) {
				String woType = wo==null ? "<Unknown Item>" : wo.getName();
				Integer n = content.get(woType);
				if (n==null) n = 0;
				content.put(woType,n+1);
			}
			Vector<Map.Entry<String, Integer>> resume = new Vector<>(content.entrySet());
			resume.sort(Comparator.<Map.Entry<String, Integer>,String>comparing(Map.Entry<String,Integer>::getKey, caseIgnoringComparator));
			return resume;
		}

		String generateOutput() {
			ValueListOutput out = new ValueListOutput();
			generateOutput(out, 0, true);
			return out.generateOutput();
		}

		void generateOutput(ValueListOutput out, int indentLevel, boolean showContentIDs)
		{
			out.add(indentLevel, "ID", id);
			if (nonUniqueID)
				out.add(indentLevel, null, "%s", "is not unique");
			
			if (worldObjs==null || size==0)
				out.add(indentLevel, "Size", "%d", size);
			else
				out.add(indentLevel, "Size", "%d (%1.1f%% filled)",  size, worldObjs.length / (double) size * 100);
			
			if (container!=null) {
				out.add(indentLevel, "Container using this list");
				container.addShortDescTo(out,indentLevel+1);
			}
			
			if (supplyItems!=null || supplyItemsStr!=null)
				out.add(indentLevel, "Supply", "%s", toString(supplyItems, supplyItemsStr));
			if (demandItems!=null || demandItemsStr!=null)
				out.add(indentLevel, "Demand", "%s", toString(demandItems, demandItemsStr));
			if (dronePrio!=null)
				out.add(indentLevel, "Drone Prio", dronePrio);
			
			out.add(indentLevel, "Content", "%d items", worldObjs.length);
			Vector<Map.Entry<String, Integer>> content = getContentResume();
			for (Map.Entry<String, Integer> entry : content)
				out.add(indentLevel+1, null, "%dx %s", entry.getValue(), entry.getKey());
			
			if (showContentIDs)
			{
				out.add(indentLevel, "Content IDs", "%d items", worldObjIds.length);
				for (int woID : worldObjIds)
					out.add(indentLevel+1, null, woID);
			}
		}
		static String toString(ObjectType[] objectTypeArr, String objectTypeArrStr)
		{
			if (objectTypeArr!=null)
				return String.join(", ", (Iterable<String>)()->Arrays.stream(objectTypeArr).map(ot->ot==null ? "<null>" : ot.getName()).iterator());
			if (objectTypeArrStr!=null)
				return String.format("\"%s\"", objectTypeArrStr);
			return null;
		}
	}

	static class GeneralData1 extends Reversable {
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(GeneralData1.class)
				.add("craftedObjects"   , Value.Type.Integer)
				.add("totalSaveFileLoad", Value.Type.Integer)
				.add("totalSaveFileTime", Value.Type.Integer);
		
		final long craftedObjects;
		final long totalSaveFileLoad;
		final long totalSaveFileTime;

		/*
			Block[4]: 1 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [3]
			        craftedObjects   :Integer
			        totalSaveFileLoad:Integer
			        totalSaveFileTime:Integer
		 */
		GeneralData1(Value<NV, V> value, String debugLabel) throws TraverseException {
			super(false);
			
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			craftedObjects    = JSON_Data.getIntegerValue(object, "craftedObjects"   , debugLabel);
			totalSaveFileLoad = JSON_Data.getIntegerValue(object, "totalSaveFileLoad", debugLabel);
			totalSaveFileTime = JSON_Data.getIntegerValue(object, "totalSaveFileTime", debugLabel);
			
			KNOWN_JSON_VALUES.scanUnexpectedValues(object);
		}

		@Override String toJsonStrs() {
			return toJsonStr(
					toIntegerValueStr("craftedObjects"   , craftedObjects   ),
					toIntegerValueStr("totalSaveFileLoad", totalSaveFileLoad),
					toIntegerValueStr("totalSaveFileTime", totalSaveFileTime)
					);
		}
	}

	static class Message extends Reversable {
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(Message.class)
				.add("isRead"  , Value.Type.Bool  )
				.add("stringId", Value.Type.String);
		
		final boolean isRead;
		final String stringId;

		/*
			Block[5]: 6 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [2]
			        isRead  :Bool
			        stringId:String
		 */
		Message(Value<NV, V> value, String debugLabel) throws TraverseException {
			super(false);
			
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			isRead   = JSON_Data.getBoolValue  (object, "isRead"  , debugLabel);
			stringId = JSON_Data.getStringValue(object, "stringId", debugLabel);
			
			KNOWN_JSON_VALUES.scanUnexpectedValues(object);
		}

		@Override String toJsonStrs() {
			return toJsonStr(
					toStringValueStr("stringId", stringId),
					toBoolValueStr  ("isRead"  , isRead  )
					);
		}
	}

	static class StoryEvent extends Reversable {
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(StoryEvent.class)
				.add("stringId", Value.Type.String);
		
		final String stringId;

		/*
			Block[6]: 6 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [1]
			        stringId:String
		 */
		StoryEvent(Value<NV, V> value, String debugLabel) throws TraverseException {
			super(false);
			
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			stringId = JSON_Data.getStringValue(object, "stringId", debugLabel);
			
			KNOWN_JSON_VALUES.scanUnexpectedValues(object);
		}

		@Override String toJsonStrs() {
			return toJsonStr(
					toStringValueStr("stringId", stringId)
					);
		}
	}

	static class GeneralData2 extends Reversable {
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(GeneralData2.class)
				// old values
				.add("hasPlayedIntro", Value.Type.Bool  )
				.add("mode"          , Value.Type.String)
				// V 1.0
		        .add("dyingConsequencesLabel"    , Value.Type.String )
		        .add("freeCraft"                 , Value.Type.Bool   )
		        .add("gameStartLocation"         , Value.Type.String )
		        .add("modifierGaugeDrain"        , Value.Type.Float  )
		        .add("modifierMeteoOccurence"    , Value.Type.Float  )
		        .add("modifierMultiplayerTerraformationFactor", Value.Type.Float)
		        .add("modifierPowerConsumption"  , Value.Type.Float  )
		        .add("modifierTerraformationPace", Value.Type.Float  )
		        .add("planetId"                  , Value.Type.String )
		        .add("randomizeMineables"        , Value.Type.Bool   )
		        .add("saveDisplayName"           , Value.Type.String )
		        .add("startLocationLabel"        , Value.Type.String )
		        .add("unlockedAutocrafter"       , Value.Type.Bool   )
		        .add("unlockedDrones"            , Value.Type.Bool   )
		        .add("unlockedEverything"        , Value.Type.Bool   )
		        .add("unlockedOreExtrators"      , Value.Type.Bool   )
		        .add("unlockedSpaceTrading"      , Value.Type.Bool   )
		        .add("unlockedTeleporters"       , Value.Type.Bool   )
		        .add("worldSeed"                 , Value.Type.Integer)
				;
		
		// old values
		final boolean hasPlayedIntro;
		final String  mode;
		
		// V 1.0
        final String  dyingConsequencesLabel;
        final Boolean freeCraft             ;
        final String  gameStartLocation     ;
        final Double  modifierGaugeDrain    ;
        final Double  modifierMeteoOccurence;
        final Double  modifierMultiplayerTerraformationFactor;
        final Double  modifierPowerConsumption  ;
        final Double  modifierTerraformationPace;
        final String  planetId                  ;
        final Boolean randomizeMineables        ;
        final String  saveDisplayName           ;
        final String  startLocationLabel        ;
        final Boolean unlockedAutocrafter       ;
        final Boolean unlockedDrones            ;
        final Boolean unlockedEverything        ;
        final Boolean unlockedOreExtrators      ;
        final Boolean unlockedSpaceTrading      ;
        final Boolean unlockedTeleporters       ;
        final Long    worldSeed                 ;

		/*
			Block[7]: 1 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [2]
			        hasPlayedIntro:Bool
			        mode:String
			   * V 1.0
			        dyingConsequencesLabel:String
			        freeCraft:Bool
			        gameStartLocation:String
			        modifierGaugeDrain:Float
			        modifierMeteoOccurence:Float
			        modifierMultiplayerTerraformationFactor:Float
			        modifierPowerConsumption:Float
			        modifierTerraformationPace:Float
			        planetId:String
			        randomizeMineables:Bool
			        saveDisplayName:String
			        startLocationLabel:String
			        unlockedAutocrafter:Bool
			        unlockedDrones:Bool
			        unlockedEverything:Bool
			        unlockedOreExtrators:Bool
			        unlockedSpaceTrading:Bool
			        unlockedTeleporters:Bool
			        worldSeed:Integer
		 */
		GeneralData2(Value<NV, V> value, String debugLabel) throws TraverseException {
			super(false);
			
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			hasPlayedIntro = JSON_Data.getBoolValue  (object, "hasPlayedIntro", debugLabel);
			mode           = JSON_Data.getStringValue(object, "mode"          , debugLabel);
			dyingConsequencesLabel     = JSON_Data.getStringValue (object, "dyingConsequencesLabel"    , true, false, debugLabel);
			freeCraft                  = JSON_Data.getBoolValue   (object, "freeCraft"                 , true, false, debugLabel);
			gameStartLocation          = JSON_Data.getStringValue (object, "gameStartLocation"         , true, false, debugLabel);
			modifierGaugeDrain         = JSON_Data.getFloatValue  (object, "modifierGaugeDrain"        , true, false, debugLabel);
			modifierMeteoOccurence     = JSON_Data.getFloatValue  (object, "modifierMeteoOccurence"    , true, false, debugLabel);
			modifierMultiplayerTerraformationFactor = JSON_Data.getFloatValue(object, "modifierMultiplayerTerraformationFactor", true, false, debugLabel);
			modifierPowerConsumption   = JSON_Data.getFloatValue  (object, "modifierPowerConsumption"  , true, false, debugLabel);
			modifierTerraformationPace = JSON_Data.getFloatValue  (object, "modifierTerraformationPace", true, false, debugLabel);
			planetId                   = JSON_Data.getStringValue (object, "planetId"                  , true, false, debugLabel);
			randomizeMineables         = JSON_Data.getBoolValue   (object, "randomizeMineables"        , true, false, debugLabel);
			saveDisplayName            = JSON_Data.getStringValue (object, "saveDisplayName"           , true, false, debugLabel);
			startLocationLabel         = JSON_Data.getStringValue (object, "startLocationLabel"        , true, false, debugLabel);
			unlockedAutocrafter        = JSON_Data.getBoolValue   (object, "unlockedAutocrafter"       , true, false, debugLabel);
			unlockedDrones             = JSON_Data.getBoolValue   (object, "unlockedDrones"            , true, false, debugLabel);
			unlockedEverything         = JSON_Data.getBoolValue   (object, "unlockedEverything"        , true, false, debugLabel);
			unlockedOreExtrators       = JSON_Data.getBoolValue   (object, "unlockedOreExtrators"      , true, false, debugLabel);
			unlockedSpaceTrading       = JSON_Data.getBoolValue   (object, "unlockedSpaceTrading"      , true, false, debugLabel);
			unlockedTeleporters        = JSON_Data.getBoolValue   (object, "unlockedTeleporters"       , true, false, debugLabel);
			worldSeed                  = JSON_Data.getIntegerValue(object, "worldSeed"                 , true, false, debugLabel);
			
			KNOWN_JSON_VALUES.scanUnexpectedValues(object);
		}

		@Override String toJsonStrs() {
			return toJsonStr( removeNulls(
					toStringValueStr ("saveDisplayName"     , saveDisplayName     , true),
					toStringValueStr ("planetId"            , planetId            , true),
					toBoolValueStr   ("unlockedSpaceTrading", unlockedSpaceTrading, true),
					toBoolValueStr   ("unlockedOreExtrators", unlockedOreExtrators, true),
					toBoolValueStr   ("unlockedTeleporters" , unlockedTeleporters , true),
					toBoolValueStr   ("unlockedDrones"      , unlockedDrones      , true),
					toBoolValueStr   ("unlockedAutocrafter" , unlockedAutocrafter , true),
					toBoolValueStr   ("unlockedEverything"  , unlockedEverything  , true),
					toBoolValueStr   ("freeCraft"           , freeCraft           , true),
					toBoolValueStr   ("randomizeMineables"  , randomizeMineables  , true),
					toFloatValueStr  ("modifierTerraformationPace"             , modifierTerraformationPace             , "%1.3f", true), // :1.0,
					toFloatValueStr  ("modifierPowerConsumption"               , modifierPowerConsumption               , "%1.3f", true), // :1.0,
					toFloatValueStr  ("modifierGaugeDrain"                     , modifierGaugeDrain                     , "%1.3f", true), // :1.0,
					toFloatValueStr  ("modifierMeteoOccurence"                 , modifierMeteoOccurence                 , "%1.3f", true), // :1.0,
					toFloatValueStr  ("modifierMultiplayerTerraformationFactor", modifierMultiplayerTerraformationFactor, "%1.3f", true), // :0.5,
					toStringValueStr ("mode"                  , mode                        ),
					toStringValueStr ("dyingConsequencesLabel", dyingConsequencesLabel, true),
					toStringValueStr ("startLocationLabel"    , startLocationLabel    , true),
					toIntegerValueStr("worldSeed"             , worldSeed             , true),
					toBoolValueStr   ("hasPlayedIntro"        , hasPlayedIntro              ),
					toStringValueStr ("gameStartLocation"     , gameStartLocation     , true)
			) );
			/*
				"saveDisplayName":"NeuStart",
				"planetId":"Prime",
				"unlockedSpaceTrading":false,
				"unlockedOreExtrators":false,
				"unlockedTeleporters":false,
				"unlockedDrones":false,
				"unlockedAutocrafter":false,
				"unlockedEverything":false,
				"freeCraft":false,
				"randomizeMineables":false,
				"modifierTerraformationPace":1.0,
				"modifierPowerConsumption":1.0,
				"modifierGaugeDrain":1.0,
				"modifierMeteoOccurence":1.0,
				"modifierMultiplayerTerraformationFactor":0.5,
				"mode":"Standard",
				"dyingConsequencesLabel":"DropSomeItems",
				"startLocationLabel":"Standard",
				"worldSeed":1837713506,
				"hasPlayedIntro":true,
				"gameStartLocation":"Standard"
			 */
		}
	}

	static class Layer extends Reversable {
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(Layer.class)
				.add("layerId"        , Value.Type.String )
				.add("colorBase"      , Value.Type.String )
				.add("colorCustom"    , Value.Type.String )
				.add("colorBaseLerp"  , Value.Type.Integer)
				.add("colorCustomLerp", Value.Type.Integer);
		
		final String layerId;
		final String colorBaseStr;
		final Color  colorBase;
		final String colorCustomStr;
		final Color  colorCustom;
		final long   colorBaseLerp;
		final long   colorCustomLerp;
		/*
			Block[8]: 10 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [5]
			        colorBase      :String
			        colorBaseLerp  :Integer
			        colorCustom    :String
			        colorCustomLerp:Integer
			        layerId        :String
		 */
		Layer(Value<NV, V> value, String debugLabel) throws TraverseException, ParseException {
			super(false);
			
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			layerId         = JSON_Data.getStringValue (object, "layerId"        , debugLabel);
			colorBaseStr    = JSON_Data.getStringValue (object, "colorBase"      , debugLabel);
			colorCustomStr  = JSON_Data.getStringValue (object, "colorCustom"    , debugLabel);
			colorBaseLerp   = JSON_Data.getIntegerValue(object, "colorBaseLerp"  , debugLabel);
			colorCustomLerp = JSON_Data.getIntegerValue(object, "colorCustomLerp", debugLabel);
			colorBase       = colorBaseStr  .isEmpty() ? null : new Color(colorBaseStr  , debugLabel+".colorBase"  );
			colorCustom     = colorCustomStr.isEmpty() ? null : new Color(colorCustomStr, debugLabel+".colorCustom");
			
			KNOWN_JSON_VALUES.scanUnexpectedValues(object);
		}
		@Override String toJsonStrs() {
			return toJsonStr(
					toStringValueStr ("layerId"        , layerId        ),
					toStringValueStr ("colorBase"      , colorBaseStr   ),
					toStringValueStr ("colorCustom"    , colorCustomStr ),
					toIntegerValueStr("colorBaseLerp"  , colorBaseLerp  ),
					toIntegerValueStr("colorCustomLerp", colorCustomLerp)
					);
		}
	}
	
	static class GeneratedWreck extends Reversable {
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(GeneratedWreck.class)
				.add("index"            , Value.Type.Integer)
				.add("owner"            , Value.Type.Integer)
				.add("pos"              , Value.Type.String )
				.add("rot"              , Value.Type.String )
				.add("seed"             , Value.Type.Integer)
				.add("version"          , Value.Type.Integer)
				.add("woIdsDropped"     , Value.Type.String )
				.add("woIdsGenerated"   , Value.Type.String )
				.add("wrecksWOGenerated", Value.Type.Bool   )
				;
		
		final long owner;
		final long index;
		final long seed;
		final String positionStr;
		final String rotationStr;
		final boolean wrecksWOGenerated;
		final String woIdsGeneratedStr;
		final String woIdsDroppedStr;
		final long version;
		final Coord3 position;
		final Rotation rotation;
		final int[] worldObjIdsGenerated;
		final int[] worldObjIdsDropped;
		WorldObject[] worldObjsGenerated;
		WorldObject[] worldObjsDropped;
		/*
			Block[9]: 4 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [9]
			        index:Integer
			        owner:Integer
			        pos:String
			        rot:String
			        seed:Integer
			        version:Integer
			        woIdsDropped:String
			        woIdsGenerated:String
			        wrecksWOGenerated:Bool
		 */
		GeneratedWreck(Value<NV, V> value, String debugLabel) throws TraverseException, ParseException {
			super(false);
			
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			
			owner             = JSON_Data.getIntegerValue(object, "owner"            , debugLabel);
			index             = JSON_Data.getIntegerValue(object, "index"            , debugLabel);
			seed              = JSON_Data.getIntegerValue(object, "seed"             , debugLabel);
			positionStr       = JSON_Data.getStringValue (object, "pos"              , debugLabel);
			rotationStr       = JSON_Data.getStringValue (object, "rot"              , debugLabel);
			wrecksWOGenerated = JSON_Data.getBoolValue   (object, "wrecksWOGenerated", debugLabel);
			woIdsGeneratedStr = JSON_Data.getStringValue (object, "woIdsGenerated"   , debugLabel);
			woIdsDroppedStr   = JSON_Data.getStringValue (object, "woIdsDropped"     , debugLabel);
			version           = JSON_Data.getIntegerValue(object, "version"          , debugLabel);
			
			KNOWN_JSON_VALUES.scanUnexpectedValues(object);
			
			position     = new Coord3  (positionStr, debugLabel+".pos");
			rotation     = new Rotation(rotationStr, debugLabel+".rot");
			
			worldObjIdsGenerated = parseIntegerArray(woIdsGeneratedStr, debugLabel+".woIdsGenerated");
			worldObjIdsDropped   = parseIntegerArray(woIdsDroppedStr  , debugLabel+".woIdsDropped"  );
			worldObjsGenerated   = null; // will be set in post processing at end of Data constructor
			worldObjsDropped     = null; // will be set in post processing at end of Data constructor
		}

		/*
		{
			owner             : Integer  "owner"             : 0,
			index             : Integer  "index"             : 8,
			seed              : Integer  "seed"              : 918858038,
			pos               : String   "pos"               : "1250.623,-51.60085,-215.7026",
			rot               : String   "rot"               : "-0.00115074,-0.3532449,-0.01073181,-0.9354687",
			wrecksWOGenerated : Bool     "wrecksWOGenerated" : true,
			woIdsGenerated    : String   "woIdsGenerated"    : "201452419,202369113,203968859,207932212,201424085,207712834,209497896,202373334,209937124,206989988,205799139,204648740,208671081,206534054,202431090,206499924,204544640,203603571,202232768,209388159,202845280,204124057,203717578,209268118,202357245,207198963,202153493,201685910,206480036,205077028,201191712,202485828,207289632,203690340,201014368,204360325,205780337,204413805,204036308,207891881,201831365,203585199,206396549,207711567,202103217,208292663,206204652,206992836,208008348,209131384,207066315,207732344,208078307,205478495,205215464,201970288,208364795,203965928,208676997,201133323,205862319,204974453,202728619,209038829,201559246,209188869,203387855,209436217,209925179,209425174,204783011,201385478,209529894,207762127,209351799,204724641,206996314,203974885,205947855,208780571,206153862,209878592,201363582,205141697,206720235,203498295,203969654,208433335,206870610,201753920,206902608,209879451,207279083,209426652,202814257,205621820,208169453,201257307,206688735,201065125,201122931,206019711,208933136,208668648",
			woIdsDropped      : String   "woIdsDropped"      : "202848071,206270512",
			version           : Integer  "version"           : 8
		}|
		*/
		@Override String toJsonStrs() {
			return toJsonStr( removeNulls(
					toIntegerValueStr("owner"            , owner            ),
					toIntegerValueStr("index"            , index            ),
					toIntegerValueStr("seed"             , seed             ),
					toStringValueStr ("pos"              , positionStr      ),
					toStringValueStr ("rot"              , rotationStr      ),
					toBoolValueStr   ("wrecksWOGenerated", wrecksWOGenerated),
					toStringValueStr ("woIdsGenerated"   , woIdsGeneratedStr),
					toStringValueStr ("woIdsDropped"     , woIdsDroppedStr  ),
					toIntegerValueStr("version"          , version          )
			) );
		}
		
	}
	
	static String toFloatValueStr  (String field, double  value, String format) { return toFloatValueStr  (field, value, format, false); }
	static String toIntegerValueStr(String field, long    value               ) { return toIntegerValueStr(field, value,         false); }
	static String toBoolValueStr   (String field, boolean value               ) { return toBoolValueStr   (field, value,         false); }
	static String toStringValueStr (String field, String  value               ) { return toStringValueStr (field, value,         false); }
	
	static String toFloatValueStr  (String field, Double  value, String format, boolean canBeUnSet) { return canBeUnSet && value==null ? null : String.format(Locale.ENGLISH, "\"%s\":"+format, field, value); }
	static String toIntegerValueStr(String field, Long    value,                boolean canBeUnSet) { return canBeUnSet && value==null ? null : String.format(                "\"%s\":%d"     , field, value); }
	static String toBoolValueStr   (String field, Boolean value,                boolean canBeUnSet) { return canBeUnSet && value==null ? null : String.format(                "\"%s\":%s"     , field, value); }
	static String toStringValueStr (String field, String  value,                boolean canBeUnSet) { return canBeUnSet && value==null ? null : String.format(                "\"%s\":\"%s\"" , field, value); }
	
	static String toJsonStr(String...strings) {
		return String.format("{%s}", String.join(",", strings));
	}
	static String[] removeNulls(String...strings) {
		if (strings==null) return null;
		return Arrays.stream(strings).filter(str->str!=null).toArray(String[]::new);
	}
}
