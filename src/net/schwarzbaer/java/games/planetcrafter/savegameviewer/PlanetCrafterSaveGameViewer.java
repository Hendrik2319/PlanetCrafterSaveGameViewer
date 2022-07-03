package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Vector;
import java.util.function.BiConsumer;

import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Helper;
import net.schwarzbaer.java.lib.jsonparser.JSON_Helper.OptionalValues;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.ParseException;

public class PlanetCrafterSaveGameViewer {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		File file = new File("c:\\Users\\Hendrik 2\\AppData\\LocalLow\\MijuGames\\Planet Crafter\\Survival-1.json");
		if (!file.isFile()) return;
		
		byte[] bytes;
		try { bytes = Files.readAllBytes(file.toPath()); }
		catch (IOException ex) {
			System.err.printf("IOException while reading file \"%s\".", ex.getMessage());
			//ex.printStackTrace();
			return;
		}
		
		String content = new String(bytes);
		OptionalValues<NV, V> optionalValues = new JSON_Helper.OptionalValues<NV,V>();
		Vector<JSON_Data.Value<NV,V>> values = new Vector<>();
		ValueContainer<Integer> blockIndex = new ValueContainer<>(0);
		
		new IterativeJsonParser().parse(content, (val,ch) -> {
			values.add(val);
			optionalValues.scan(val,"ParseResult");
			if (ch==null || ch.equals('@')) {
				System.out.printf("Block[%d]: %d entries%n", blockIndex.value, values.size());
				optionalValues.show("-> Format", System.out);
				optionalValues.clear();
				blockIndex.value++;
			}
		}, '@','|');
	}
	
	private static class ValueContainer<Val> {
		Val value;
		ValueContainer(Val value) { this.value = value; }
	}
	
	private static class NV extends JSON_Data.NamedValueExtra.Dummy {}
	private static class  V extends JSON_Data.ValueExtra.Dummy {}
	
	private static class IterativeJsonParser {
		
		private String content = null;
		private Character glueChar = null;

		void parse(String json_text, BiConsumer<JSON_Data.Value<NV,V>, Character> consumeValue, Character...glueChars) {
			content = json_text.trim();
			Vector<Character> knownGlueChars = new Vector<>(Arrays.asList(glueChars));
			try {
				
				while( !content.isEmpty() ) {
					JSON_Parser<NV, V> parser = new JSON_Parser<NV,V>(content, null);
					glueChar = null;
					JSON_Data.Value<NV,V> result = parser.parse_withParseException(str -> {
						//if (str.length()>40) System.out.printf("Remaining Text: \"%s...\"%n", str.substring(0, 40));
						//else                 System.out.printf("Remaining Text: \"%s\"%n", str);
						content = str.trim();
						if (!content.isEmpty()) {
							char ch = content.charAt(0);
							//System.out.printf("GlueChar: \"%s\"%n", ch);
							if (knownGlueChars.contains((Character)ch)) {
								content = content.substring(1);
								glueChar = ch;
							}
						}
					});
					consumeValue.accept(result,glueChar);
				}
				
			} catch (ParseException ex) {
				System.err.printf("ParseException while parsing content of file \"%s\".", ex.getMessage());
				//ex.printStackTrace();
				return;
			}
		}
	}

}
