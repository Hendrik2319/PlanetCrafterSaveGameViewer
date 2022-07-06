package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.IntFunction;

import net.schwarzbaer.gui.ValueListOutput;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.TraverseException;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;

class Data {
	static class NV extends JSON_Data.NamedValueExtra.Dummy {}
	static class  V extends JSON_Data.ValueExtra.Dummy {}
	static final Comparator<String> caseIgnoringComparator = Comparator.nullsLast(Comparator.<String,String>comparing(str->str.toLowerCase()).thenComparing(Comparator.naturalOrder()));

	static Data parse(Vector<Vector<Value<NV, V>>> jsonStructure, HashMap<String,ObjectType> objectTypes) {
		try {
			return new Data(jsonStructure, objectTypes);
			
		} catch (ParseException ex) {
			System.err.printf("ParseException while parsing JSON structure: %s%n", ex.getMessage());
			//ex.printStackTrace();
			return null;
			
		} catch (TraverseException ex) {
			System.err.printf("TraverseException while parsing JSON structure: %s%n", ex.getMessage());
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
	
	private Data(Vector<Vector<Value<NV, V>>> dataVec, HashMap<String, ObjectType> objectTypes) throws ParseException, TraverseException {
		if (dataVec==null) throw new IllegalArgumentException();
		
		System.out.printf("Parsing JSON Structure ...%n");
		int blockIndex = 0;
		/* 0 */ terraformingStates = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), TerraformingStates::new, "TerraformingStates"); blockIndex++;
		/* 1 */ playerStates       = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), PlayerStates      ::new, "PlayerStates"      ); blockIndex++;
		/* 2 */ worldObjects       = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), WorldObject       ::new, "WorldObjects"      ); blockIndex++;
		/* 3 */ objectLists        = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), ObjectList        ::new, "ObjectLists"       ); blockIndex++;
		/* 4 */ generalData1       = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), GeneralData1      ::new, "GeneralData1"      ); blockIndex++;
		/* 5 */ messages           = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), Message           ::new, "Messages"          ); blockIndex++;
		/* 6 */ storyEvents        = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), StoryEvent        ::new, "StoryEvents"       ); blockIndex++;
		/* 7 */ generalData2       = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), GeneralData2      ::new, "GeneralData2"      ); blockIndex++;
		/* 8 */ layers             = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), Layer             ::new, "Layers"            ); blockIndex++;
		
		System.out.printf("Processing Data ...%n");
		for (WorldObject wo : worldObjects) {
			wo.objectType = ObjectType.getOrCreate(objectTypes, wo.objectTypeID);
			if (0 < wo.listId)
				for (ObjectList ol : objectLists)
					if (ol.id==wo.listId) {
						ol.container = wo;
						wo.list = ol;
						break;
					}
		}
		
		for (ObjectList ol : objectLists) {
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
		
		if (str==null) throw new IllegalArgumentException();
		if (debugLabel==null) throw new IllegalArgumentException();
		
		if (str.isEmpty())
			return createArray.apply(0);
		
		String[] parts = str.split(",",-1);
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
		final double biomassLevel;
		final double heatLevel;
		final double oxygenLevel;
		final double pressureLevel;
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
		 */
		TerraformingStates(Value<NV, V> value, String debugLabel) throws TraverseException {
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			biomassLevel  = JSON_Data.getFloatValue(object, "unitBiomassLevel" , debugLabel);
			heatLevel     = JSON_Data.getFloatValue(object, "unitHeatLevel"    , debugLabel);
			oxygenLevel   = JSON_Data.getFloatValue(object, "unitOxygenLevel"  , debugLabel);
			pressureLevel = JSON_Data.getFloatValue(object, "unitPressureLevel", debugLabel);
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
			if (value < 2000) return formatValue("%1.2f pk", value);
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
		PlayerStates(Value<NV, V> value, String debugLabel) throws TraverseException, ParseException {
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
		}
	}
	
	static class WorldObject {
		final long     id;
		final String   objectTypeID;
		final long     listId;
		final String   _liGrps;
		final Coord3   position;
		final Rotation rotation;
		final long     _wear;
		final String   _pnls;
		final String   _color;
		//final Coord3   color;
		final String   text;
		final long     growth;
		
		ObjectList     list;
		WorldObject    container;
		ObjectList     containerList;
		ObjectType     objectType;
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
			String positionStr, rotationStr/* , colorStr */;
			id           = JSON_Data.getIntegerValue(object, "id"    , debugLabel);
			objectTypeID = JSON_Data.getStringValue (object, "gId"   , debugLabel);
			listId       = JSON_Data.getIntegerValue(object, "liId"  , debugLabel);
			_liGrps      = JSON_Data.getStringValue (object, "liGrps", debugLabel);
			positionStr  = JSON_Data.getStringValue (object, "pos"   , debugLabel);
			rotationStr  = JSON_Data.getStringValue (object, "rot"   , debugLabel);
			_wear        = JSON_Data.getIntegerValue(object, "wear"  , debugLabel);
			_pnls        = JSON_Data.getStringValue (object, "pnls"  , debugLabel);
			_color       = JSON_Data.getStringValue (object, "color" , debugLabel);
			//colorStr    = JSON_Data.getStringValue (object, "color" , debugLabel);
			text         = JSON_Data.getStringValue (object, "text"  , debugLabel);
			growth       = JSON_Data.getIntegerValue(object, "grwth" , debugLabel);
			
			position     = new Coord3  (positionStr, debugLabel+".pos");
			rotation     = new Rotation(rotationStr, debugLabel+".rot");
			//color       = new Coord3  (colorStr   , debugLabel+".color");
			
			list      = null;
			container = null;
			containerList = null;
			objectType = null;
		}
		
		String getName() {
			if (objectType!=null && objectType.label!=null)
				return objectType.label;
			if (objectTypeID!=null)
				return String.format("{%s}", objectTypeID);
			return String.format("[%d]", id);
		}
		
		String generateOutput() {
			ValueListOutput out = new ValueListOutput();
			out.add(0, "Name", getName());
			out.add(0, "ID", id);
			out.add(0, "ObjectTypeID", objectTypeID);
			if (!text.isEmpty()   )   out.add(0, "Text", text);
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
		WorldObject container;

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
			worldObjs = null;
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
		final String colorBase;
		final String colorCustom;
		final long colorBaseLerp;
		final long colorCustomLerp;

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
		Layer(Value<NV, V> value, String debugLabel) throws TraverseException {
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			layerId         = JSON_Data.getStringValue (object, "layerId"        , debugLabel);
			colorBase       = JSON_Data.getStringValue (object, "colorBase"      , debugLabel);
			colorCustom     = JSON_Data.getStringValue (object, "colorCustom"    , debugLabel);
			colorBaseLerp   = JSON_Data.getIntegerValue(object, "colorBaseLerp"  , debugLabel);
			colorCustomLerp = JSON_Data.getIntegerValue(object, "colorCustomLerp", debugLabel);
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
