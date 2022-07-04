package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Vector;
import java.util.function.BiConsumer;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.gui.FileChooser;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.NV;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.V;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Helper;
import net.schwarzbaer.java.lib.jsonparser.JSON_Helper.OptionalValues;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.ParseException;
import net.schwarzbaer.system.Settings;

public class PlanetCrafterSaveGameViewer {

	public static void main(String[] args) {
		// String pathname = "c:\\Users\\Hendrik 2\\AppData\\LocalLow\\MijuGames\\Planet Crafter\\Survival-1.json";
		// scanFileContent(pathname);
		
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		new PlanetCrafterSaveGameViewer().initialize();
	}
	
	@SuppressWarnings("unused")
	private static void scanFileContent(String pathname) {
		File file = new File(pathname);
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
		ValueContainer<Integer> blockIndex = new ValueContainer<>(0);
		ValueContainer<Integer> entriesCount = new ValueContainer<>(0);
		
		new IterativeJsonParser().parse(content, (val,ch) -> {
			entriesCount.value++;
			optionalValues.scan(val,"ParseResult");
			if (ch==null || ch.equals('@')) {
				System.out.printf("Block[%d]: %d entries%n", blockIndex.value, entriesCount.value);
				optionalValues.show("-> Format", System.out);
				optionalValues.clear();
				blockIndex.value++;
				entriesCount.value = 0;
			}
		}, '@','|');
	}

	private static final AppSettings settings = new AppSettings();
	
	private final StandardMainWindow mainWindow;
	private final FileChooser jsonFileChooser;
	private File openFile;
	private final JTabbedPane dataTabPane;

	PlanetCrafterSaveGameViewer() {
		openFile = null;
		jsonFileChooser = new FileChooser("JSON File", "json");
		
		mainWindow = new StandardMainWindow("Planet Crafter - SaveGame Viewer");
		dataTabPane = new JTabbedPane();
		mainWindow.startGUI(dataTabPane);
		settings.registerAppWindow(mainWindow);
		updateWindowTitle();
	}

	private void updateWindowTitle() {
		mainWindow.setTitle(
			openFile == null
			?               "Planet Crafter - SaveGame Viewer"
			: String.format("Planet Crafter - SaveGame Viewer - \"%s\"", openFile.getName())
		);
	}

	private void initialize() {
		jsonFileChooser.setCurrentDirectory(guessDirectory());
		
		// String pathname = "c:\\Users\\Hendrik 2\\AppData\\LocalLow\\MijuGames\\Planet Crafter\\Survival-1.json";
		File file = settings.getFile(AppSettings.ValueKey.OpenFile, null);
		if (file==null || !file.isFile()) {
			file = null;
			if (jsonFileChooser.showOpenDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
				file = jsonFileChooser.getSelectedFile();
		}
		
		Vector<Vector<JSON_Data.Value<NV,V>>> data = file==null || !file.isFile() ? null : readContent(file);
		Data parsedData = data==null ? null : Data.parse(data);
		
		if (parsedData!=null) {
			settings.putFile(AppSettings.ValueKey.OpenFile, file);
			this.openFile = file;
			setGUI(parsedData);
			updateWindowTitle();
		}
	}

	private File guessDirectory() {
		File currentDir = null;
		
		// c:\Users\Hendrik 2\AppData\LocalLow\MijuGames\Planet Crafter\
		String user_home = System.getProperty("user.home"); // "C:\Users\Hendrik 2"
		if (user_home!=null) {
			currentDir = new File(user_home, "AppData\\LocalLow\\MijuGames\\Planet Crafter");
			if (!currentDir.isDirectory())
				currentDir = null;
		}
		if (currentDir==null || !currentDir.isDirectory())
			currentDir = new File("./");
		
		return currentDir;
	}

	private Vector<Vector<Value<NV, V>>> readContent(File file) {
		byte[] bytes;
		try { bytes = Files.readAllBytes(file.toPath()); }
		catch (IOException ex) {
			System.err.printf("IOException while reading file \"%s\".", ex.getMessage());
			//ex.printStackTrace();
			return null;
		}
		
		String content = new String(bytes);
		Vector<Vector<Value<NV, V>>> fileData = new Vector<>();
		Vector<Value<NV, V>> blockData = new Vector<>();
		
		new IterativeJsonParser().parse(content, (val,ch) -> {
			blockData.add(val);
			if (ch.equals('@')) {
				System.out.printf("Block[%d]: %d entries%n", fileData.size(), blockData.size());
				fileData.add(new Vector<>(blockData));
				blockData.clear();
			}
		}, '@','|');
		
		if (!blockData.isEmpty()) {
			System.out.printf("Block[%d]: %d entries%n", fileData.size(), blockData.size());
			fileData.add(new Vector<>(blockData));
			blockData.clear();
		}
		
		return fileData;
	}

	private void setGUI(Data data) {
		dataTabPane.removeAll();
		dataTabPane.addTab("General", new GeneralTabPanel(data));
		dataTabPane.addTab("World Objects", new WorldObjectsPanel(data.worldObjects));
		dataTabPane.addTab("Object Lists", new ObjectListsPanel(data.objectLists));
		
		MapPanel mapPanel = new MapPanel(data.worldObjects);
		dataTabPane.addTab("Map", mapPanel);
		SwingUtilities.invokeLater(mapPanel::initialize);
		// TODO: more tabs in GUI
	}
	
	private static class GeneralTabPanel extends JPanel {
		private static final long serialVersionUID = -9191759791973305801L;

		GeneralTabPanel(Data data) {
			// TODO: GeneralTabPanel
		}
	}

	private static class ValueContainer<Val> {
		Val value;
		ValueContainer(Val value) { this.value = value; }
	}
	
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


	private static class AppSettings extends Settings.DefaultAppSettings<AppSettings.ValueGroup, AppSettings.ValueKey> {
		public enum ValueKey {
			OpenFile
		}
	
		enum ValueGroup implements Settings.GroupKeys<ValueKey> {
			;
			ValueKey[] keys;
			ValueGroup(ValueKey...keys) { this.keys = keys;}
			@Override public ValueKey[] getKeys() { return keys; }
		}
		
		public AppSettings() { super(PlanetCrafterSaveGameViewer.class, ValueKey.values()); }
	}
}
