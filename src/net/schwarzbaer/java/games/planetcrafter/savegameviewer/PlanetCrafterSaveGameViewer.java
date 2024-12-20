package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Achievements.PlanetAchievements;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.NV;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.PlanetId;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.V;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.GUI.ActionCommand;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.lib.gui.Disabler;
import net.schwarzbaer.java.lib.gui.FileChooser;
import net.schwarzbaer.java.lib.gui.GeneralIcons;
import net.schwarzbaer.java.lib.gui.GeneralIcons.GrayCommandIcons;
import net.schwarzbaer.java.lib.gui.IconSource;
import net.schwarzbaer.java.lib.gui.ProgressDialog;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.StandardMainWindow;
import net.schwarzbaer.java.lib.gui.ValueListOutput;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Helper;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.ParseException;
import net.schwarzbaer.java.lib.system.DateTimeFormatter;
import net.schwarzbaer.java.lib.system.Settings;

public class PlanetCrafterSaveGameViewer implements ActionListener {
	
	private static IconSource.CachedIcons<FlagIcons> FlagIconsIS = IconSource.createCachedIcons(25, 18, "/icons/Flags.png", FlagIcons.values());
	public enum FlagIcons { DE,GB; public Icon getIcon() { return FlagIconsIS.getCachedIcon(this); } }
	enum LabelLanguage { EN, DE }

	        static final String FILE_OBJECT_TYPES        = "PlanetCrafterSaveGameViewer - ObjectTypes.data";
	        static final String FILE_ACHIEVEMENTS        = "PlanetCrafterSaveGameViewer - Achievements.data";
	private static final String FILE_MAPSHAPES           = "PlanetCrafterSaveGameViewer - MapShapes.data";
            static final String FILE_MAPBGIMAGE_BASE     = "PlanetCrafterSaveGameViewer - MapBackgroundImage";
	private static final String FILE_AUTOCRAFTER_TRADING = "PlanetCrafterSaveGameViewer - AutoCrafterTrading.data";
	        static final String FILE_FARWRECKAREAS       = "PlanetCrafterSaveGameViewer - FarWreckAreas.data";

	public static void main(String[] args) {
		//String pathname = "c:\\Users\\Hendrik 2\\AppData\\LocalLow\\MijuGames\\Planet Crafter\\Survival-1.json";
		//scanFile(pathname);
		//GeneralDataPanel.TerraformingStatesPanel.testDurationFormater();
		
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		staticInitialize();
		new PlanetCrafterSaveGameViewer().initialize();
	}
	
	        static final DateTimeFormatter dtFormatter = new DateTimeFormatter();
	        static final Comparator<String> STRING_COMPARATOR__IGNORING_CASE = Comparator.<String,String>comparing(str->str.toLowerCase()).thenComparing(Comparator.naturalOrder());
	private static final boolean DEBUG_SCANFILECONTENT = false;
	private static       LabelLanguage currentLabelLanguage = AppSettings.getInstance().getEnum(AppSettings.ValueKey.LabelLanguage, LabelLanguage.EN, LabelLanguage.class);

	        final StandardMainWindow mainWindow;
	private final Disabler<ActionCommand> disabler;
	private final FileChooser jsonFileChooser;
	private final JTabbedPane dataTabPane;
	private       GeneralDataPanel generalDataPanel;
	private       ObjectTypesPanel objectTypesPanel;
	private final AutoReloader autoReloader;
	private final MapShapes.Editor mapShapesEditor;
	
	private       File openFile;
	private       Data loadedData;
	        final MapShapes mapShapes;
	private final AutoCrafterTrading autoCrafterTrading;

	PlanetCrafterSaveGameViewer() {
		mainWindow = new StandardMainWindow("Planet Crafter - SaveGame Viewer");
		
		openFile = null;
		loadedData = null;
		generalDataPanel = null;
		objectTypesPanel = null;
		autoCrafterTrading = new AutoCrafterTrading(new File(FILE_AUTOCRAFTER_TRADING), mainWindow);
		TerraformingCalculation.getInstance().clearData();
		
		jsonFileChooser = new FileChooser("JSON File", "json");
		
		disabler = new Disabler<>();
		disabler.setCareFor(ActionCommand.values());
		
		autoReloader = new AutoReloader();
		
		dataTabPane = new JTabbedPane();
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(new MyToolBar(), BorderLayout.PAGE_START);
		contentPane.add(dataTabPane, BorderLayout.CENTER);
		
		mainWindow.startGUI(contentPane);
		//mainWindow.startGUI(contentPane, new MyMenuBar());
		mainWindow.setIconImagesFromResource("/icons/icon_%d_green.png", 16,24,32,48,64,96);
		
		AppSettings.getInstance().registerAppWindow(mainWindow);
		
		mapShapes = new MapShapes(mainWindow, new File(FILE_MAPSHAPES));
		mapShapesEditor = new MapShapes.Editor(mainWindow, "MapShapes Editor", mapShapes, event -> {
			switch (event.type())
			{
				case HasGotFirstShape:
				case RemovedSelectedShape:
				case ChangedShapeName:
					if (objectTypesPanel!=null)
						objectTypesPanel.notifyMapShapesEvent(event);
					break;
			}
		});
		
		updateWindowTitle();
		updateGuiAccess();
	}
	
	private void updateWindowTitle() {
		String filename        = openFile==null ? "" : String.format(" [%s, %s]", openFile.getName(), dtFormatter.getTimeStr(openFile.lastModified(), false, true, false, true, false));
		String saveDisplayName = loadedData==null || loadedData.generalData2==null || loadedData.generalData2.saveDisplayName==null ? "" : String.format(" \"%s\"", loadedData.generalData2.saveDisplayName);
		String spacer          = filename.isEmpty() && saveDisplayName.isEmpty() ? "" : " -";
		mainWindow.setTitle( String.format("Planet Crafter - SaveGame Viewer%s%s%s", spacer, saveDisplayName, filename) );
	}

	private void updateGuiAccess() {
		disabler.setEnable(ac->{
			switch (ac) {
			case OpenSaveGame:
			case ScanSaveGame:
				break;
			
			case ReloadSaveGameAutoSwitch:
			case ReloadSaveGame:
			case WriteReducedSaveGame:
				return openFile!=null;
				
			case ShowEditAchievements:
			case ShowMapShapesEditor:
			case SetLabelLanguageDE:
			case SetLabelLanguageEN:
			case MemoryInfo:
				break;
			}
			return null;
		});
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		ActionCommand ac;
		try { ac = ActionCommand.valueOf(e.getActionCommand()); }
		catch (Exception ex) { return; }
		
		actionPerformed(ac);
	}

	private void actionPerformed(ActionCommand ac)
	{
		switch (ac) {
			
			case ReloadSaveGame:
				readFile(openFile);
				break;
				
			case OpenSaveGame:
				if (jsonFileChooser.showOpenDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
					readFile(jsonFileChooser.getSelectedFile());
				break;
				
			case ScanSaveGame:
				if (jsonFileChooser.showOpenDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
					scanFile(jsonFileChooser.getSelectedFile());
				break;
				
			case WriteReducedSaveGame:
				if (loadedData!=null) {
					if (openFile!=null)
						jsonFileChooser.setSelectedFile(openFile);
					if (jsonFileChooser.showSaveDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
						writeReducedFile(jsonFileChooser.getSelectedFile(), loadedData);
				}
				break;
				
			case ShowEditAchievements:
				PlanetId planet = loadedData==null ? null : loadedData.getPlanet();
				Achievements.ConfigDialog dlg = new Achievements.ConfigDialog(mainWindow, planet, loadedData==null ? null : loadedData.achievedValues);
				dlg.showDialog(StandardDialog.Position.PARENT_CENTER);
				if (dlg.wereValuesChanged())
					Achievements.getInstance().writeToFile();
				if (generalDataPanel!=null)
					generalDataPanel.updateAfterAchievementsChange();
				break;
			
			case ReloadSaveGameAutoSwitch:
				break;
				
			case ShowMapShapesEditor:
				mapShapesEditor.showDialog();
				break;
				
			case SetLabelLanguageDE: setLabelLanguage(LabelLanguage.DE); break;
			case SetLabelLanguageEN: setLabelLanguage(LabelLanguage.EN); break;
			
			case MemoryInfo:
				showMemoryInfo();
				break;
		}
		
	}
	
	private void showMemoryInfo()
	{
		ValueListOutput out = new ValueListOutput();
		
		Runtime.Version version = Runtime.version();
		out.add(0, "Runtime Version", "%s", version==null ? "<null>" : version.toString());
		
		Runtime runtime = Runtime.getRuntime();
		if (runtime!=null)
		{
			out.add(0, "Memory");
			out.add(1, "max"  , "%12d byte (%10s)", runtime.  maxMemory(), formatMemory(runtime.  maxMemory()));
			out.add(1, "total", "%12d byte (%10s)", runtime.totalMemory(), formatMemory(runtime.totalMemory()));
			out.add(1, "free" , "%12d byte (%10s)", runtime. freeMemory(), formatMemory(runtime. freeMemory()));
		}
		
		System.out.print(out.generateOutput());
	}
	
	private static String formatMemory(long valueL) {
		double value = valueL;
		if (value < 2000) return formatValue("%d B", valueL);
		value/=1000;
		if (value < 2000) return formatValue("%1.2f kB", value);
		value/=1000;
		if (value < 2000) return formatValue("%1.2f MB", value);
		value/=1000;
		if (value < 2000) return formatValue("%1.2f GB", value);
		value/=1000;
		return formatValue("%1.2f TB", value);
	}
	private static String formatValue(String format, Object value) {
		return String.format(Locale.ENGLISH, format, value);
	}

	private void setLabelLanguage(LabelLanguage lang)
	{
		currentLabelLanguage = lang;
		AppSettings.getInstance().putEnum(AppSettings.ValueKey.LabelLanguage, currentLabelLanguage);
		setGUI(loadedData);
	}

	static LabelLanguage getCurrentLabelLanguage()
	{
		return currentLabelLanguage;
	}

	private class AutoReloader {
		private static final Color COLOR_BUTTON_BG_RELOAD = new Color(0xB7FF00);
		private final FileChangeObserver fileChangeObserver;
		
		AutoReloader() {
			fileChangeObserver = new FileChangeObserver(2000, (dateChanged, sizeChanged, isFileChanged) ->
			{
				//System.out.printf("AutoReloader.filePropsChanged( dateChanged:%s, sizeChanged:%s, isFileChanged:%s )%n", dateChanged, sizeChanged, isFileChanged);
				if (dateChanged || sizeChanged || isFileChanged)
				{
					if (isActive())
						actionPerformed(ActionCommand.ReloadSaveGame);
					else
						setButton(COLOR_BUTTON_BG_RELOAD);

				}
				//System.out.printf("AutoReloader.filePropsChanged( dateChanged:%s, sizeChanged:%s, isFileChanged:%s ) -> finished%n", dateChanged, sizeChanged, isFileChanged);
			});
			fileChangeObserver.start();
		}
		
		void setFile(File file)
		{
			setButton(null);
			fileChangeObserver.setFile(file);
		}

		private void setButton(Color color)
		{
			SwingUtilities.invokeLater( ()->{
				disabler.configureAbstractButton(ActionCommand.ReloadSaveGame, btn -> {
					btn.setBackground(color);
				});
			} );
		}
		
		boolean isActive()                { return AppSettings.getInstance().getBool(AppSettings.ValueKey.ReloadAutomatically, false ); }
		void    setActive(boolean active) {        AppSettings.getInstance().putBool(AppSettings.ValueKey.ReloadAutomatically, active); }
		
	}

	private class MyToolBar extends JToolBar {
		private static final long serialVersionUID = -545321067655154725L;
		
		MyToolBar() {
			this.setFloatable(false);
			add(createButton  ("Open SaveGame"         , GrayCommandIcons.IconGroup.Folder, true , ActionCommand.OpenSaveGame        ));
			add(createButton  ("Reload SaveGame"       , GrayCommandIcons.IconGroup.Reload, false, ActionCommand.ReloadSaveGame      ));
			add(createCheckBox("Reload Automatically"  , autoReloader.isActive()          , true , autoReloader::setActive, ActionCommand.ReloadSaveGameAutoSwitch));
			add(createButton  ("Write Reduced SaveGame", GrayCommandIcons.IconGroup.Save  , false, ActionCommand.WriteReducedSaveGame));
			//addSeparator();
			//add(createButton  ("Scan SaveGame"         , GrayCommandIcons.IconGroup.Folder, true , ActionCommand.ScanSaveGame        ));
			addSeparator();
			add(createButton  ("Show/Edit Achievements", null                             , true , ActionCommand.ShowEditAchievements));
			addSeparator();
			add(createButton  ("MapShapes Editor"      , null                             , true , ActionCommand.ShowMapShapesEditor  ));
			addSeparator();
			add(new JLabel("Language of Labels:"));
			ButtonGroup bgLanguage = new ButtonGroup();
			add(createRadioButton("EN", getCurrentLabelLanguage() == LabelLanguage.EN, bgLanguage, true, ActionCommand.SetLabelLanguageEN));
			add(createRadioButton("DE", getCurrentLabelLanguage() == LabelLanguage.DE, bgLanguage, true, ActionCommand.SetLabelLanguageDE));
			addSeparator();
			add(createButton("Memory Info", GrayCommandIcons.IconGroup.Memory, true , ActionCommand.MemoryInfo));
		}
		
		JCheckBox createCheckBox(String title, boolean isChecked, boolean isEnabled, Consumer<Boolean> valueChanged, ActionCommand ac) {
			return GUI.createCheckBox(title, isChecked, isEnabled, valueChanged, disabler, ac);
		}
		JButton createButton(String title, GeneralIcons.IconGroup icons, boolean isEnabled, ActionCommand ac) {
			return GUI.createButton(title, icons, isEnabled, PlanetCrafterSaveGameViewer.this, disabler, ac); 
		}
		JRadioButton createRadioButton(String title, boolean isChecked, ButtonGroup bg, boolean isEnabled, ActionCommand ac) {
			return GUI.createRadioButton(title, isChecked, bg, isEnabled, PlanetCrafterSaveGameViewer.this, disabler, ac); 
		}
	}

	@SuppressWarnings("unused")
	private class MyMenuBar extends JMenuBar {
		private static final long serialVersionUID = 940262053656728621L;

		MyMenuBar() {
			JMenu filesMenu = add(new JMenu("Files"));
			filesMenu.add(createMenuItem("Open SaveGame"         , GrayCommandIcons.IconGroup.Folder, true , ActionCommand.OpenSaveGame));
			filesMenu.add(createMenuItem("Reload SaveGame"       , GrayCommandIcons.IconGroup.Reload, false, ActionCommand.ReloadSaveGame));
			filesMenu.add(createMenuItem("Write Reduced SaveGame", GrayCommandIcons.IconGroup.Save  , false, ActionCommand.WriteReducedSaveGame));
			filesMenu.addSeparator();
			filesMenu.add(GUI.createMenuItem("Quit", e->System.exit(0)));
			
			JMenu achievementsMenu = add(new JMenu("Achievements"));
			achievementsMenu.add(createMenuItem("Show/Edit Achievements", null, true, ActionCommand.ShowEditAchievements));
		}
		
		JMenuItem createMenuItem(String title, GeneralIcons.IconGroup icons, boolean isEnabled, ActionCommand ac) {
			return GUI.createMenuItem(title, icons, isEnabled, PlanetCrafterSaveGameViewer.this, disabler, ac);
		}
	}

	private static void staticInitialize()
	{
		ObjectTypes objectTypes = ObjectTypes.getInstance();
		objectTypes.readFromFile();
		
		Achievements achievements = Achievements.getInstance();
		achievements.readFromFile();
		achievements.updateObjectTypeAssignments();
		achievements.sortAchievements();
	}

	private void initialize() {
		jsonFileChooser.setCurrentDirectory(guessDirectory());
		
		autoCrafterTrading.readFromFile();
		mapShapes.readFromFile();
		mapShapesEditor.updateAfterNewObjectTypes();
		FarWreckAreas.getInstance().readFromFile();
		
		// String pathname = "c:\\Users\\Hendrik 2\\AppData\\LocalLow\\MijuGames\\Planet Crafter\\Survival-1.json";
		File file = AppSettings.getInstance().getFile(AppSettings.ValueKey.OpenFile, null);
		if (file==null || !file.isFile()) {
			file = null;
			if (jsonFileChooser.showOpenDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
				file = jsonFileChooser.getSelectedFile();
		}
		
		readFile(file);
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

	private void readFile(File file) {
		if (file==null) return;
		if (!file.isFile()) return;
		
		String title = String.format("Read File \"%s\" [%s]", file.getName(), file.getParent());
		ProgressDialog.runWithProgressDialog(mainWindow, title, 400, pd->{
			
			Vector<Vector<JSON_Data.Value<NV,V>>> jsonStructure = readContent(pd, file);
			if (Thread.currentThread().isInterrupted()) { System.out.println("File Reading Aborted"); return; }
			if (jsonStructure==null) return;
			
			showIndeterminateTask(pd, "Parse JSON Structure");
			HashSet<String> newObjectTypes = new HashSet<>();
			ObjectTypes objectTypes = ObjectTypes.getInstance();
			Data data = Data.parse(
					jsonStructure,
					(objectTypeID,occurrence) -> objectTypes.getOrCreate(objectTypeID, occurrence, newObjectTypes)
			);
			if (Thread.currentThread().isInterrupted()) { System.out.println("File Reading Aborted"); return; }
			if (data == null) return;
			
			showIndeterminateTask(pd, "Write new ObjectTypes to File");
			objectTypes.writeToFile();
			
			if (!newObjectTypes.isEmpty()) {
				Vector<String> vec = new Vector<>(newObjectTypes);
				vec.sort(Data.caseIgnoringComparator);
				vec.insertElementAt("Some new Object Types found:", 0);
				String[] message = vec.toArray(String[]::new);
				JOptionPane.showMessageDialog(mainWindow, message, "New ObjectTypes", JOptionPane.INFORMATION_MESSAGE);
			}
			
			SwingUtilities.invokeLater(()->{
				pd.setTaskTitle("Update GUI");
				pd.setIndeterminate(true);
				
				AppSettings.getInstance().putFile(AppSettings.ValueKey.OpenFile, file);
				loadedData = data;
				openFile = file;
				autoReloader.setFile(file);
				
				setGUI(data);
				updateWindowTitle();
				updateGuiAccess();
			});
		});
		
	}

	private void writeReducedFile(File file, Data data) {
		if (file==null) return;
		
		Data.AchievedValues modifiedAchievedValues;
		String msg = "Do you want to change Terraforming States?";
		String dlgTitle = "Modified Terraforming States";
		int result = JOptionPane.showConfirmDialog(mainWindow, msg, dlgTitle, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (JOptionPane.YES_OPTION == result) {
			modifiedAchievedValues = GUI.AchievedValuesDialog.show(mainWindow, "Modify Achieved Values", data.achievedValues);
			if (modifiedAchievedValues==null)
				return;
		} else if (JOptionPane.NO_OPTION == result)
			modifiedAchievedValues = null;
		else
			return;
		
		String title = String.format("Write Reduced File \"%s\" [%s]", file.getName(), file.getParent());
		ProgressDialog.runWithProgressDialog(mainWindow, title, 400, pd->{
			
			showIndeterminateTask(pd, "Create JSON Code");
			Vector<Vector<String>> jsonStrs = data.toJsonStrs(modifiedAchievedValues);
			if (Thread.currentThread().isInterrupted()) { System.out.println("File Writing Aborted"); return; }
			
			writeContent(pd, file, jsonStrs);
		
		});
	}

	void showMapShapesEditor(ObjectType objectType)
	{
		mapShapesEditor.showDialog(objectType);
	}

	private static void showIndeterminateTask(ProgressDialog pd, String taskTitle) {
		SwingUtilities.invokeLater(()->{
			pd.setTaskTitle(taskTitle);
			pd.setIndeterminate(true);
		});
	}

	private static void showTask(ProgressDialog pd, String taskTitle, int max) {
		SwingUtilities.invokeLater(()->{
			pd.setTaskTitle(taskTitle);
			pd.setValue(0, max);
		});
	}

	private static void setTaskValue(ProgressDialog pd, int value) {
		SwingUtilities.invokeLater(()->{
			pd.setValue(value);
		});
	}

	private void setGUI(Data data) {
		Data.clearAllRemoveStateListeners();
		dataTabPane.removeAll();
		TerraformingCalculation.getInstance().clearData();
		
		PlanetId planet = data.getPlanet();
		if (planet==null) planet=PlanetId.Prime;
		PlanetAchievements achievements = Achievements.getInstance().getOrCreate(planet);
		
		generalDataPanel = new GeneralDataPanel(data,achievements);
		TerraformingPanel terraformingPanel = new TerraformingPanel(data, generalDataPanel);
		MapPanel mapPanel = new MapPanel(this, data, planet);
		
		HashMap<String, Integer> amounts = new HashMap<>();
		if (data.worldObjects!=null)
			for (WorldObject wo : data.worldObjects)
			{
				Integer n = amounts.get( wo.objectTypeID );
				if (n==null) n = 0;
				amounts.put( wo.objectTypeID, n+1 );
			}
		
		objectTypesPanel = new ObjectTypesPanel(this, amounts);
		objectTypesPanel.addObjectTypesChangeListener(e -> ObjectTypes.getInstance().writeToFile());
		objectTypesPanel.addObjectTypesChangeListener(mapPanel);
		objectTypesPanel.addObjectTypesChangeListener(terraformingPanel);
		objectTypesPanel.addObjectTypesChangeListener(generalDataPanel);
		objectTypesPanel.addObjectTypesChangeListener(Achievements.getInstance());
		objectTypesPanel.addObjectTypesChangeListener(mapShapesEditor);
		objectTypesPanel.addObjectTypesChangeListener(autoCrafterTrading);
		
		String titleFarWreckAreaTablePanel = "[ Far Wreck Areas at \"%s\" ]".formatted(planet);
		dataTabPane.addTab("General", generalDataPanel);
		dataTabPane.addTab("Map", mapPanel);
		dataTabPane.addTab("Terraforming", terraformingPanel);
		dataTabPane.addTab("World Objects", new WorldObjectsPanel(this,data,mapPanel));
		dataTabPane.addTab("Object Lists", new ObjectListsPanel(data,mapPanel));
		dataTabPane.addTab("Supply -> Demand", new SupplyDemandPanel(data));
		if (data.generatedWrecks!=null && !data.generatedWrecks.isEmpty())
			dataTabPane.addTab("Generated Wrecks", new GeneratedWrecksPanel(this,data,mapPanel));
		dataTabPane.addTab("[ Object Types ]", objectTypesPanel);
		dataTabPane.addTab("[ AutoCrafter Trading ]", autoCrafterTrading.createNewPanel());
		dataTabPane.addTab(titleFarWreckAreaTablePanel, new FarWreckAreaTablePanel(planet));
		
		mapShapesEditor.updateAfterNewObjectTypes();
		
		SwingUtilities.invokeLater(() -> {
			mapPanel.initialize();
		});
	}

	private static Vector<Vector<JSON_Data.Value<NV, V>>> readContent(ProgressDialog pd, File file) {
		showIndeterminateTask(pd, "Read Content");
		byte[] bytes;
		try { bytes = Files.readAllBytes(file.toPath()); }
		catch (IOException ex) {
			System.err.printf("IOException while reading file \"%s\".", ex.getMessage());
			//ex.printStackTrace();
			return null;
		}
		if (Thread.currentThread().isInterrupted()) return null;
		String content = new String(bytes);
		
		if (DEBUG_SCANFILECONTENT) {
			showIndeterminateTask(pd, "Scan JSON Structure");
			scanFileContent(content);
		}
		
		showIndeterminateTask(pd, "Create JSON Structure");
		Vector<Vector<JSON_Data.Value<NV, V>>> fileData = new Vector<>();
		Vector<JSON_Data.Value<NV, V>> blockData = new Vector<>();
		
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
		if (Thread.currentThread().isInterrupted()) return null;
		
		return fileData;
	}

	private static void writeContent(ProgressDialog pd, File file, Vector<Vector<String>> jsonStrs) {
		if (Thread.currentThread().isInterrupted()) return;
		
		int lineAmount = 0;
		for (Vector<String> block : jsonStrs)
			lineAmount += block.size();
		
		showTask(pd, "Write JSON code to file", lineAmount);
		
		try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
			
			int lineCounter = 0;
			for (Vector<String> block : jsonStrs) {
				out.print("\r");
				boolean isFirst = true;
				for (String line : block) {
					if (Thread.currentThread().isInterrupted()) return;
					if (!isFirst) out.print("|\n");
					isFirst = false;
					out.print(line);
					setTaskValue(pd, ++lineCounter);
				}
				out.print("\r@");
			}
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@SuppressWarnings("unused")
	private static void scanFile(String pathname) {
		scanFile(new File(pathname));
	}

	private static void scanFile(File file) {
		if (!file.isFile()) return;
		
		byte[] bytes;
		try { bytes = Files.readAllBytes(file.toPath()); }
		catch (IOException ex) {
			System.err.printf("IOException while reading file \"%s\".", ex.getMessage());
			//ex.printStackTrace();
			return;
		}
		
		String content = new String(bytes);
		scanFileContent(content);
	}

	private static void scanFileContent(String content) {
		JSON_Helper.OptionalValues<NV, V> optionalValues = new JSON_Helper.OptionalValues<>();
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
					if (Thread.currentThread().isInterrupted()) break;
					
					glueChar = null;
					boolean detected = detectGlueChar(knownGlueChars);
					JSON_Data.Value<NV,V> result = detected ? null : JSON_Parser.parse_withParseException(content, null, str -> {
						//if (str.length()>40) System.out.printf("Remaining Text: \"%s...\"%n", str.substring(0, 40));
						//else                 System.out.printf("Remaining Text: \"%s\"%n", str);
						content = str.trim();
						if (!content.isEmpty())
							detectGlueChar(knownGlueChars);
					});
					consumeValue.accept(result,glueChar);
				}
				
			} catch (ParseException ex) {
				System.err.printf("ParseException while parsing content of file \"%s\".", ex.getMessage());
				//ex.printStackTrace();
				return;
			}
		}

		private boolean detectGlueChar(Vector<Character> knownGlueChars) {
			char ch = content.charAt(0);
			//System.out.printf("GlueChar: \"%s\"%n", ch);
			if (knownGlueChars.contains((Character)ch)) {
				content = content.substring(1).trim();
				glueChar = ch;
				return true;
			}
			return false;
		}
	}
	
	static class FileChangeObserver
	{
		private final int rate_ms;
		private final ScheduledExecutorService scheduler;
		private ScheduledFuture<?> runningTask;
		private FileProperties file;
		private final ChangeListener listener;
		
		FileChangeObserver(int rate_ms, ChangeListener listener)
		{
			this.rate_ms = rate_ms;
			this.listener = listener;
			scheduler = Executors.newSingleThreadScheduledExecutor();
			runningTask = null;
			file = null;
		}
		
		synchronized void setFile(File file)
		{
			this.file = file==null ? null : FileProperties.create( file.getAbsoluteFile() );
			if (runningTask == null) start();
		}
		
		private synchronized void start()
		{
			stop();
			runningTask = scheduler.scheduleAtFixedRate(this::checkFile, 10, rate_ms, TimeUnit.MILLISECONDS);
		}
		
		private void checkFile()
		{
			FileProperties notifyFile = null;
			synchronized (this) {
				if (file!=null && file.hasChanged())
				{
					notifyFile = file;
					file = FileProperties.create( file.file );
				}
			}
			if (notifyFile!=null)
				notifyFile.notifyListener( listener );
		}

		private synchronized void stop()
		{
			if (runningTask!=null)
			{
				runningTask.cancel(false);
				runningTask = null;
			}
		}
		
		interface ChangeListener
		{
			void filePropsChanged(boolean dateChanged, boolean sizeChanged, boolean isFileChanged);
		}
		
		private record FileProperties(File file, long date, long size, boolean isFile)
		{
			static FileProperties create(File file)
			{
				return new FileProperties(file, file.lastModified(), file.length(), file.isFile());
			}
			
			void notifyListener(ChangeListener listener)
			{
				listener.filePropsChanged(
					date != file.lastModified(),
					size != file.length(),
					isFile != file.isFile()
				);
			}

			boolean hasChanged()
			{
				return
					date != file.lastModified() ||
					size != file.length() ||
					isFile != file.isFile();
			}
		}
	}


	static class AppSettings extends Settings.DefaultAppSettings<AppSettings.ValueGroup, AppSettings.ValueKey> {
		
		private static AppSettings instance = null;
		static AppSettings getInstance()
		{
			return instance == null
					? instance = new AppSettings()
					: instance;
		}
		
		public enum ValueKey {
			OpenFile,
			AchievementsConfigDialogWidth,
			AchievementsConfigDialogHeight,
			AchievementsConfigDialogShowTabbedView,
			ObjectTypeColors,
			ReloadAutomatically,
			LabelLanguage,
			MapShapesEditor_WindowX,
			MapShapesEditor_WindowY,
			MapShapesEditor_WindowWidth,
			MapShapesEditor_WindowHeight,
			MapShapesEditor_SplitPaneDivider,
			MapBackgroundImage_Brightness,
			MapBackgroundImage_Contrast,
			MapBackgroundImage_ShowBgImage,
			MapBackgroundImage_FixPoint_Map1X,
			MapBackgroundImage_FixPoint_Map1Y,
			MapBackgroundImage_FixPoint_Map2X,
			MapBackgroundImage_FixPoint_Map2Y,
			MapBackgroundImage_FixPoint_Image1X,
			MapBackgroundImage_FixPoint_Image1Y,
			MapBackgroundImage_FixPoint_Image2X,
			MapBackgroundImage_FixPoint_Image2Y,
			MapView_ShowWreckAreas,
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
