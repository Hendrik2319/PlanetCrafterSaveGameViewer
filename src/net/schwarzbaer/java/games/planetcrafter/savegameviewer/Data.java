package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.IntFunction;

import net.schwarzbaer.gui.ValueListOutput;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.MapPanel.MapWorldObjectData;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.TraverseException;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;

class Data {
	static class NV extends JSON_Data.NamedValueExtra.Dummy {}
	static class  V extends JSON_Data.ValueExtra.Dummy {}
	static final Comparator<String> caseIgnoringComparator = Comparator.nullsLast(Comparator.<String,String>comparing(str->str.toLowerCase()).thenComparing(Comparator.naturalOrder()));

	static Data parse(Vector<Vector<Value<NV, V>>> jsonStructure, Function<String,ObjectType> getOrCreateObjectType) {
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

	final Vector<TerraformingStates> terraformingStates;
	final Vector<PlayerStates> playerStates;
	final Vector<WorldObject> worldObjects;
	final Vector<ObjectList> objectLists;
	final Vector<GeneralData1> generalData1;
	final Vector<Message> messages;
	final Vector<StoryEvent> storyEvents;
	final Vector<GeneralData2> generalData2;
	final Vector<Layer> layers;
	final HashMap<Long,WorldObject> mapWorldObjects;
	final HashMap<Long,ObjectList> mapObjectLists;
	
	private Data(Vector<Vector<Value<NV, V>>> dataVec, Function<String,ObjectType> getOrCreateObjectType) throws ParseException, TraverseException {
		if (dataVec==null) throw new IllegalArgumentException();
		
		ParseConstructor<PlayerStates> createPlayerStates = (value, debugLabel) -> new PlayerStates(value, getOrCreateObjectType, debugLabel);
		
		System.out.printf("Parsing JSON Structure ...%n");
		int blockIndex = 0;
		/* 0 */ terraformingStates = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), TerraformingStates::new, "TerraformingStates"); blockIndex++;
		/* 1 */ playerStates       = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), createPlayerStates     , "PlayerStates"      ); blockIndex++;
		/* 2 */ worldObjects       = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), WorldObject       ::new, "WorldObjects"      ); blockIndex++;
		/* 3 */ objectLists        = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), ObjectList        ::new, "ObjectLists"       ); blockIndex++;
		/* 4 */ generalData1       = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), GeneralData1      ::new, "GeneralData1"      ); blockIndex++;
		/* 5 */ messages           = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), Message           ::new, "Messages"          ); blockIndex++;
		/* 6 */ storyEvents        = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), StoryEvent        ::new, "StoryEvents"       ); blockIndex++;
		/* 7 */ generalData2       = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), GeneralData2      ::new, "GeneralData2"      ); blockIndex++;
		/* 8 */ layers             = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), Layer             ::new, "Layers"            ); blockIndex++;
		
		mapWorldObjects = new HashMap<>();
		System.out.printf("Processing Data ...%n");
		for (WorldObject wo : worldObjects) {
			
			if (!mapWorldObjects.containsKey(wo.id)) mapWorldObjects.put(wo.id, wo);
			else {
				WorldObject other = mapWorldObjects.get(wo.id);
				System.err.printf("Non unique ID in WorldObject: %d (this:\"%s\", other:\"%s\")%n", wo.id, wo.objectTypeID, other.objectTypeID);
			}
			
			wo.objectType = getOrCreateObjectType.apply(wo.objectTypeID);
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
			
			if (!mapObjectLists.containsKey(ol.id)) mapObjectLists.put(ol.id, ol);
			else System.err.printf("Non unique ID in ObjectList: %d%n", ol.id);
			
			int[] worldObjIds = ol.worldObjIds;
			ol.worldObjs = new WorldObject[worldObjIds.length];
			for (int i=0; i<worldObjIds.length; i++) {
				int woId = worldObjIds[i];
				ol.worldObjs[i] = null;
				for (WorldObject wo : worldObjects)
					if (wo.id==woId) {
						ol.worldObjs[i] = wo;
						wo.container = ol.container;
						wo.containerList = ol;
						break;
					}
			}
		}
		
		
		System.out.printf("Done%n");
	}
	
	private static <ValueType> Vector<ValueType> parseArray(Vector<Value<NV, V>> vector, ParseConstructor<ValueType> parseConstructor, String debugLabel) throws ParseException, TraverseException {
		Vector<ValueType> parsedVec = new Vector<>();
		for (int i=0; i< vector.size(); i++) {
			Value<NV, V> val = vector.get(i);
			String newDebugLabel = String.format("%s[%d]", debugLabel, i);
			ValueType parsedValue = parseConstructor.parse(val, newDebugLabel);
			parsedVec.add(parsedValue);
		}
		return parsedVec;
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

	static String[] parseStringArray(String str, String debugLabel) throws ParseException {
		return parseCommaSeparatedArray(str, debugLabel, "String", String[]::new, str1->str1);
	}

	interface ParseConstructor<ValueType> {
		ValueType parse(Value<NV, V> value, String debugLabel) throws ParseException, TraverseException;
	}
	
	static class ParseException extends Exception {
		private static final long serialVersionUID = 7894187588880980010L;

		ParseException(String format, Object...args) {
			super(String.format(Locale.ENGLISH, format, args));
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
	}
	static class TerraformingStates {
		final double oxygenLevel;
		final double heatLevel;
		final double pressureLevel;
		//final double biomassLevel;
		final double plantsLevel ;
		final double insectsLevel;
		final double animalsLevel;
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
		 */
		TerraformingStates(Value<NV, V> value, String debugLabel) throws TraverseException {
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			oxygenLevel   = JSON_Data.getFloatValue(object, "unitOxygenLevel"  , debugLabel);
			heatLevel     = JSON_Data.getFloatValue(object, "unitHeatLevel"    , debugLabel);
			pressureLevel = JSON_Data.getFloatValue(object, "unitPressureLevel", debugLabel);
			//biomassLevel  = JSON_Data.getFloatValue(object, "unitBiomassLevel" , debugLabel);
			plantsLevel   = JSON_Data.getFloatValue(object, "unitPlantsLevel"  , debugLabel);
			insectsLevel  = JSON_Data.getFloatValue(object, "unitInsectsLevel" , debugLabel);
			animalsLevel  = JSON_Data.getFloatValue(object, "unitAnimalsLevel" , debugLabel);
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

		private static String formatValue(String format, double value) {
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
			return formatValue("%1.2f TTi", value);
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
			return formatValue("%1.2f Mt", value);
		}
	}

	static class PlayerStates {
		final double health;
		final double oxygen;
		final double thirst;
		final Coord3 position;
		final Rotation rotation;
		final String[] unlockedGroups;
		final ObjectType[] unlockedObjectTypes;
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
		PlayerStates(Value<NV, V> value, Function<String, ObjectType> getOrCreateObjectType, String debugLabel) throws TraverseException, ParseException {
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			String positionStr, rotationStr, unlockedGroupsStr; 
			health            = JSON_Data.getFloatValue (object, "playerGaugeHealth", debugLabel);
			oxygen            = JSON_Data.getFloatValue (object, "playerGaugeOxygen", debugLabel);
			thirst            = JSON_Data.getFloatValue (object, "playerGaugeThirst", debugLabel);
			positionStr       = JSON_Data.getStringValue(object, "playerPosition"   , debugLabel);
			rotationStr       = JSON_Data.getStringValue(object, "playerRotation"   , debugLabel);
			unlockedGroupsStr = JSON_Data.getStringValue(object, "unlockedGroups"   , debugLabel);
			
			position = new Coord3  (positionStr, debugLabel+".playerPosition");
			rotation = new Rotation(rotationStr, debugLabel+".playerRotation");
			unlockedGroups = parseStringArray(unlockedGroupsStr, debugLabel+".unlockedGroups");
			
			unlockedObjectTypes = new ObjectType[unlockedGroups.length];
			for (int i=0; i<unlockedGroups.length; i++)
				unlockedObjectTypes[i] = getOrCreateObjectType.apply( unlockedGroups[i] );
		}
		
		boolean isPositioned() {
			return !rotation.isZero() || !position.isZero();
		}
	}
	
	static class WorldObject {
		final long     id;
		final String   objectTypeID;
		final long     listId;
		final String   _liGrps;
		final Coord3   position;
		final String   positionStr;
		final Rotation rotation;
		final String   rotationStr;
		final long     _wear;
		final String   _pnls;
		final String   colorStr;
		final Color    color;
		final String   text;
		final long     growth;
		
		ObjectList     list; // list associated with listId
		WorldObject    container; // container, it is containing this object
		ObjectList     containerList; // list, it is containing this object
		ObjectType     objectType;
		MapWorldObjectData mapWorldObjectData;
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
		WorldObject(Value<NV, V> value, String debugLabel) throws TraverseException, ParseException {
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			id           = JSON_Data.getIntegerValue(object, "id"    , debugLabel);
			objectTypeID = JSON_Data.getStringValue (object, "gId"   , debugLabel);
			listId       = JSON_Data.getIntegerValue(object, "liId"  , debugLabel);
			_liGrps      = JSON_Data.getStringValue (object, "liGrps", debugLabel); // result ID in GeneticManipulator1
			positionStr  = JSON_Data.getStringValue (object, "pos"   , debugLabel);
			rotationStr  = JSON_Data.getStringValue (object, "rot"   , debugLabel);
			_wear        = JSON_Data.getIntegerValue(object, "wear"  , debugLabel);
			_pnls        = JSON_Data.getStringValue (object, "pnls"  , debugLabel);
			colorStr     = JSON_Data.getStringValue (object, "color" , debugLabel); // "1-1-1-1" in OutsideLamp1
			text         = JSON_Data.getStringValue (object, "text"  , debugLabel);
			growth       = JSON_Data.getIntegerValue(object, "grwth" , debugLabel);
			
			position     = new Coord3  (positionStr, debugLabel+".pos");
			rotation     = new Rotation(rotationStr, debugLabel+".rot");
			color        = colorStr.isEmpty() ? null : new Color(colorStr, debugLabel+".color");
			
			list      = null;
			container = null;
			containerList = null;
			objectType = null;
			mapWorldObjectData = new MapWorldObjectData();
			
			// -------------------------------------------------------------------
			//      show special values in fields
			// -------------------------------------------------------------------
			boolean _liGrpsIsNotEmpty = !_liGrps.isEmpty();
			boolean _wearIsNotZero    =  _wear  !=0;
			//boolean _pnlsIsNotEmpty   = !_pnls  .isEmpty();
			//boolean _colorIsNotEmpty  = !colorStr .isEmpty();
			if (//_pnlsIsNotEmpty   ||
				//_colorIsNotEmpty  ||
				_wearIsNotZero    ||
				_liGrpsIsNotEmpty ) {
				Vector<String> vars = new Vector<>();
				if (_liGrpsIsNotEmpty) vars.add("_liGrps");
				if (_wearIsNotZero   ) vars.add("_wear");
				//if (_pnlsIsNotEmpty  ) vars.add("_pnls");
				//if (_colorIsNotEmpty ) vars.add("_color");
				vars.sort(null);
				System.err.printf("Special WorldObject: { "+
						"id"    +":"+  "%d"  +", "+  // Int  
						"gId"   +":"+"\"%s\""+", "+  // Str
						"liId"  +":"+  "%d"  +", "+  // Int
						"liGrps"+":"+"\"%s\""+", "+  // Str
						"pos"   +":"+"\"%s\""+", "+  // Str
						"rot"   +":"+"\"%s\""+", "+  // Str
						"wear"  +":"+  "%d"  +", "+  // Int
						"pnls"  +":"+"\"%s\""+", "+  // Str
						"color" +":"+"\"%s\""+", "+  // Str
						"text"  +":"+"\"%s\""+", "+  // Str
						"grwth" +":"+  "%d"  +       // Int
						" }, special vars: %s%n", 
						id          ,
						objectTypeID,
						listId      ,
						_liGrps     ,
						positionStr ,
						rotationStr ,
						_wear       ,
						_pnls       ,
						colorStr      ,
						text        ,
						growth      ,
						String.join(", ", vars)
						);
			}
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
			out.add(0, "ObjectTypeID", objectTypeID);
			if (!text   .isEmpty())   out.add(0, "Text", text);
			if ( color!=null       ) {
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
			if (listId>0) {
				out.add(0, "Is a Container");
				out.add(1, "List-ID", "%d%s", listId, list==null ? "(no list found)" : "");
				if (list!=null) {
					out.add(1, "Size", "%d", list.size);
					out.add(1, "Content", "%d items", list.worldObjs.length);
					Vector<Map.Entry<String, Integer>> content = list.getContentResume();
					for (Map.Entry<String, Integer> entry : content)
						out.add(2, null, "%dx %s", entry.getValue(), entry.getKey());
				}
			}
			
			return out.generateOutput();
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
	}

	static class ObjectList {
		final long id;
		final long size;
		final int[] worldObjIds;
		WorldObject[] worldObjs;
		WorldObject container; // container using this list

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
		ObjectList(Value<NV, V> value, String debugLabel) throws TraverseException, ParseException {
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			String woIdsStr;
			id          = JSON_Data.getIntegerValue(object, "id"    , debugLabel);
			size        = JSON_Data.getIntegerValue(object, "size"  , debugLabel);
			woIdsStr    = JSON_Data.getStringValue (object, "woIds" , debugLabel);
			
			worldObjIds = parseIntegerArray(woIdsStr, debugLabel+".woIds");
			worldObjs = null; // will be set in post processing at end of Data constructor
			container = null;
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

		public String generateOutput() {
			ValueListOutput out = new ValueListOutput();
			
			out.add(0, "ID", id);
			out.add(0, "Size", "%d", size);
			if (container!=null) {
				out.add(0, "Container using this list");
				container.addShortDescTo(out,1);
			}
			out.add(0, "Content", "%d items", worldObjs.length);
			Vector<Map.Entry<String, Integer>> content = getContentResume();
			for (Map.Entry<String, Integer> entry : content)
				out.add(1, null, "%dx %s", entry.getValue(), entry.getKey());
			
			out.add(0, "Content IDs", "%d items", worldObjIds.length);
			for (int woID : worldObjIds)
				out.add(1, null, woID);
			
			return out.generateOutput();
		}
	}

	static class GeneralData1 {
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
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			craftedObjects    = JSON_Data.getIntegerValue(object, "craftedObjects"   , debugLabel);
			totalSaveFileLoad = JSON_Data.getIntegerValue(object, "totalSaveFileLoad", debugLabel);
			totalSaveFileTime = JSON_Data.getIntegerValue(object, "totalSaveFileTime", debugLabel);
		}
	}

	static class Message {
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
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			isRead   = JSON_Data.getBoolValue  (object, "isRead"  , debugLabel);
			stringId = JSON_Data.getStringValue(object, "stringId", debugLabel);
		}
	}

	static class StoryEvent {
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
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			stringId = JSON_Data.getStringValue(object, "stringId", debugLabel);
		}
	}

	static class GeneralData2 {
		final boolean hasPlayedIntro;
		final String mode;

		/*
			Block[7]: 1 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [2]
			        hasPlayedIntro:Bool
			        mode:String
		 */
		GeneralData2(Value<NV, V> value, String debugLabel) throws TraverseException {
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			hasPlayedIntro = JSON_Data.getBoolValue  (object, "hasPlayedIntro", debugLabel);
			mode           = JSON_Data.getStringValue(object, "mode"          , debugLabel);
		}
	}

	static class Layer {
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
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			layerId         = JSON_Data.getStringValue (object, "layerId"        , debugLabel);
			colorBaseStr    = JSON_Data.getStringValue (object, "colorBase"      , debugLabel);
			colorCustomStr  = JSON_Data.getStringValue (object, "colorCustom"    , debugLabel);
			colorBaseLerp   = JSON_Data.getIntegerValue(object, "colorBaseLerp"  , debugLabel);
			colorCustomLerp = JSON_Data.getIntegerValue(object, "colorCustomLerp", debugLabel);
			colorBase       = colorBaseStr  .isEmpty() ? null : new Color(colorBaseStr  , debugLabel+".colorBase"  );
			colorCustom     = colorCustomStr.isEmpty() ? null : new Color(colorCustomStr, debugLabel+".colorCustom");
		}
	}

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
Block[2]: 3033 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [11]
        color:String
        gId:String
        grwth:Integer
        id:Integer
        liGrps:String
        liId:Integer
        pnls:String
        pos:String
        rot:String
        text:String
        wear:Integer
Block[3]: 221 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [3]
        id:Integer
        size:Integer
        woIds:String
Block[4]: 1 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [3]
        craftedObjects:Integer
        totalSaveFileLoad:Integer
        totalSaveFileTime:Integer
Block[5]: 6 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [2]
        isRead:Bool
        stringId:String
Block[6]: 6 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [1]
        stringId:String
Block[7]: 1 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [2]
        hasPlayedIntro:Bool
        mode:String
Block[8]: 10 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [5]
        colorBase:String
        colorBaseLerp:Integer
        colorCustom:String
        colorCustomLerp:Integer
        layerId:String
 */
}
