package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;
import java.util.function.BiConsumer;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.FileChooser;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Coord3;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.NV;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.ObjectList;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Rotation;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.V;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
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
		// TODO: more tabs in GUI
	}
	
	private static class WorldObjectsPanel extends AbstractTablePanel<WorldObject, WorldObjectsPanel.WorldObjectsTableModel.ColumnID> {
		private static final long serialVersionUID = 8733627835226098636L;

		WorldObjectsPanel(Vector<WorldObject> worldObjects) {
			super(new WorldObjectsTableModel(worldObjects));
		}
		
		private static class WorldObjectsTableModel extends AbstractTablePanel.AbstractTableModel<WorldObject, WorldObjectsTableModel.ColumnID> {

			enum ColumnID implements Tables.SimplifiedColumnIDInterface {
				id       ("ID"       , Long    .class,  75),
				objType  ("Name"     , String  .class, 130),
				container("Container", String  .class, 350),
				listId   ("List-ID"  , Long    .class,  70),
				_liGrps  ("[liGrps]" , String  .class,  50),
				position ("Position" , Coord3  .class, 200),
				rotation ("Rotation" , Rotation.class, 205),
				_wear    ("[wear]"   , Long    .class,  50),
				_pnls    ("[pnls]"   , String  .class,  90),
				_color   ("[color]"  , String  .class,  50),
				//color    (""       , Coord3  .class,  50),
				text     ("Text"     , String  .class, 120),
				_growth  ("[growth]" , Long    .class,  60),
				;
				private final SimplifiedColumnConfig cfg;
				ColumnID(String name, Class<?> colClass, int width) {
					cfg = new SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
				}
				@Override public SimplifiedColumnConfig getColumnConfig() {
					return cfg;
				}
			
			}

			WorldObjectsTableModel(Vector<WorldObject> data) {
				super(ColumnID.values(), data);
			}

			@Override protected String getRowText(WorldObject row, int rowIndex) {
				return "Dummy"; // TODO: WorldObjectsTableModel.getRowText
			}

			@Override protected Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID, WorldObject row) {
				switch (columnID) {
				case _color  : return row._color;
				case _growth : return row._growth;
				case _liGrps : return row._liGrps;
				case _pnls   : return row._pnls;
				case _wear   : return row._wear;
				case id      : return row.id;
				case listId  : return row.listId;
				case objType : return row.objType;
				case position: return row.position;
				case rotation: return row.rotation;
				case text    : return row.text;
				case container:
					if (row.containerList==null)
						return null;
					if (row.container==null)
						return String.format("<UnknownContainer> [List:%d]", row.containerList.id);
					return String.format("%s (\"%s\", Pos:%s)", row.container.objType, row.container.text, row.container.position); 
				}
				return null;
			}
			
		}
	}
	
	private static class ObjectListsPanel extends AbstractTablePanel<ObjectList, ObjectListsPanel.ObjectListsTableModel.ColumnID> {
		private static final long serialVersionUID = -1787920497956857504L;

		ObjectListsPanel(Vector<ObjectList> objectLists) {
			super(new ObjectListsTableModel(objectLists));
		}
		
		private static class ObjectListsTableModel extends AbstractTablePanel.AbstractTableModel<ObjectList, ObjectListsTableModel.ColumnID> {

			enum ColumnID implements Tables.SimplifiedColumnIDInterface {
				id         ("ID"       , Long  .class,  75),
				container  ("Container", String.class, 130),
				size       ("Size"     , Long  .class,  50),
				worldObjs  ("Content"  , String.class, 600),
				;
				private final SimplifiedColumnConfig cfg;
				ColumnID(String name, Class<?> colClass, int width) {
					cfg = new SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
				}
				@Override public SimplifiedColumnConfig getColumnConfig() {
					return cfg;
				}
			
			}

			ObjectListsTableModel(Vector<ObjectList> data) {
				super(ColumnID.values(), data);
			}

			@Override protected String getRowText(ObjectList row, int rowIndex) {
				return "Dummy"; // TODO ObjectListsTableModel.getRowText
			}

			@Override protected Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID, ObjectList row) {
				switch (columnID) {
				case id       : return row.id;
				case container: return row.container==null ? "--" : row.container.objType;
				case size     : return row.size;
				case worldObjs:
					//Iterator<String> it = Arrays.stream(row.worldObjIds).mapToObj(n->Integer.toString(n)).iterator();
					Iterator<String> it = Arrays.stream(row.worldObjs).map(wo->wo==null ? "<????>" : wo.objType).iterator();
					return String.join(", ", (Iterable<String>)()->it);
				}
				return null;
			}
			
		}
	}
	
	private static class AbstractTablePanel<ValueType, ColumnID extends Tables.SimplifiedColumnIDInterface> extends JPanel {
		private static final long serialVersionUID = 5518131959056782917L;

		AbstractTablePanel(AbstractTableModel<ValueType, ColumnID> tableModel) {
			this(tableModel, BorderLayout.SOUTH);
		}
		AbstractTablePanel(AbstractTableModel<ValueType, ColumnID> tableModel, String textAreaPos_BL) {
			super(new BorderLayout(3,3));
			
			JTextArea textArea = new JTextArea();
			textArea.setEditable(false);
			textArea.setLineWrap(false);
			
			JTable table = new JTable(tableModel);
			table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.getSelectionModel().addListSelectionListener(e -> {
				int rowV = table.getSelectedRow();
				if (rowV<0) return;
				int rowM = table.convertRowIndexToModel(rowV);
				if (rowM<0) return;
				ValueType row = tableModel.getRow(rowM);
				String str = tableModel.getRowText(row,rowM);
				textArea.setText(str);
			});
			tableModel.setTable(table);
			tableModel.setColumnWidths(table);
			
			new TableContextMenu(table,tableModel);
			
			JScrollPane tableScrollPane = new JScrollPane(table);
			JScrollPane textareaScrollPane = new JScrollPane(textArea);
			
			add(tableScrollPane, BorderLayout.CENTER);
			add(textareaScrollPane,textAreaPos_BL);
		}
		
		protected class TableContextMenu extends ContextMenu {
			private static final long serialVersionUID = 1755523803906870773L;

			TableContextMenu(JTable table, AbstractTableModel<ValueType,ColumnID> tableModel) {
				add(createMenuItem("Show Column Widths", e->{
					System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(table));
				}));
				
				addTo(table);
			}
		}

		protected static JMenuItem createMenuItem(String title, ActionListener al) {
			JMenuItem comp = new JMenuItem(title);
			if (al!=null) comp.addActionListener(al);
			return comp;
		}
		
		protected static abstract class AbstractTableModel<ValueType, ColumnID extends Tables.SimplifiedColumnIDInterface> extends Tables.SimplifiedTableModel<ColumnID> {
			
			final Vector<ValueType> data;

			protected AbstractTableModel(ColumnID[] columns, Vector<ValueType> data) {
				super(columns);
				this.data = data;
			}
			
			@Override public int getRowCount() { return data.size(); }
			
			ValueType getRow(int rowIndex) {
				if (rowIndex < 0) return null;
				if (data.size() <= rowIndex) return null;
				return data.get(rowIndex);
			}

			@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				ValueType row = getRow(rowIndex);
				if (row==null) return null;
				return getValueAt(rowIndex, columnIndex, columnID, row);
			}
			
			protected abstract Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID, ValueType row);
			protected abstract String getRowText(ValueType row, int rowIndex);
			
		}
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
