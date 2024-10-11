package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.PlanetId;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectTypeValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.PhysicalValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeEvent;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeListener;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.PlanetCrafterSaveGameViewer.AppSettings;
import net.schwarzbaer.java.lib.gui.ContextMenu;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedTableModel;

class Achievements implements ObjectTypesChangeListener
{
	private static Achievements instance = null;
	static Achievements getInstance()
	{
		return instance == null
				? instance = new Achievements()
				: instance;
	}
	
	private static final Comparator<Achievement> ACHIEVEMENT_COMPARATOR = Comparator
	.<Achievement,Double>comparing(a->a.getLevel(), Comparator.nullsLast(Comparator.naturalOrder()))
	.thenComparing(a->a.objectTypeID, Comparator.nullsLast(Comparator.naturalOrder()))
	.thenComparing(a->a.getLabel());
	
	enum AchievementList {
		Oxygen, Heat, Pressure, Biomass, Plants, Insects, Animals, Terraformation, Stages;
		static AchievementList valueOf_checked(String str) {
			try { return valueOf(str); }
			catch (Exception e) { return null; }
		}

		Function<Double, String> getFormatter() {
			switch (this) {
			case Oxygen  : return Data.AchievedValues::formatOxygenLevel  ;
			case Heat    : return Data.AchievedValues::formatHeatLevel    ;
			case Pressure: return Data.AchievedValues::formatPressureLevel;
			case Biomass: case Plants: case Insects: case Animals:
				return Data.AchievedValues::formatBiomassLevel;
			case Terraformation: case Stages:
				return Data.AchievedValues::formatTerraformation;
			}
			return null;
		}
	}
	
	private final EnumMap<PlanetId,PlanetAchievements> achievements;
	
	private Achievements() {
		achievements = new EnumMap<>(PlanetId.class);
	}
	
	private static String getValue(String line, String prefix) {
		if (line.startsWith(prefix))
			return line.substring(prefix.length());
		return null;
	}

	void readFromFile() {
		File file = new File(PlanetCrafterSaveGameViewer.FILE_ACHIEVEMENTS); 		
		achievements.clear();
		
		System.out.printf("Read Achievements from file \"%s\" ...%n", file.getAbsolutePath());
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			
			String line;
			String valueStr; 
			PlanetId currentPlanet = PlanetId.Prime;
			PlanetAchievements planetAchievements = null;
			Vector<Achievement> currentList = null;
			Achievement value;
			boolean planetHeaderFound = false;
			
			while ( (line=in.readLine())!=null ) {
				
				if (line.isEmpty())
					continue;
				
				if ( (valueStr=getValue(line, "Planet: "))!=null ) {
					currentPlanet = PlanetId.parse(valueStr);
					if (currentPlanet != null)
						achievements.put(currentPlanet, planetAchievements = new PlanetAchievements());
					else
						planetAchievements = null;
					currentList = null;
					planetHeaderFound = true;
					continue;
				}
				
				AchievementList listType = AchievementList.valueOf_checked(line);
				if (listType!=null) {
					if (planetAchievements==null && !planetHeaderFound) {
						currentPlanet = PlanetId.Prime;
						achievements.put(currentPlanet, planetAchievements = new PlanetAchievements());
					}
					if (planetAchievements!=null) {
						currentList = planetAchievements.achievements.computeIfAbsent(listType, al->new Vector<>());
					} else
						currentList = null;
					continue;
				}
				
				if (currentList!=null) {
					if ( (value=Achievement.parseLine(line))!=null && !value.isEmpty())
						currentList.add(value);
					continue;
				}
				
			}
			
		} catch (FileNotFoundException ex) {
			//ex.printStackTrace();
		} catch (IOException ex) {
			System.err.printf("IOException while reading Achievements: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}

	void writeToFile() {
		File file = new File(PlanetCrafterSaveGameViewer.FILE_ACHIEVEMENTS); 		
		System.out.printf("Write Achievements to file \"%s\" ...%n", file.getAbsolutePath());
		
		try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
			
			achievements.forEach((planet,map) -> {
				if (map.achievements.isEmpty())
					return;
				
				out.printf("Planet: %s%n", planet);
				out.println();
				
				map.achievements.forEach((al,list) -> {
					if (!list.isEmpty()) {
						Vector<Achievement> sorted = new Vector<>(list);
						sorted.sort(ACHIEVEMENT_COMPARATOR);
						out.println(al.name());
						for (Achievement a : sorted)
							out.println(a.toLine());
						out.println();
					}
				});
			});
			
		} catch (IOException ex) {
			System.err.printf("IOException while writing Achievements: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}

	void sortAchievements() {
		achievements.forEach(
				(planet,pa) -> pa.achievements.forEach(
						(al,list) -> list.sort( ACHIEVEMENT_COMPARATOR )
				)
		);
	}

	class PlanetAchievements
	{
		private final EnumMap<AchievementList,Vector<Achievement>> achievements;
		
		PlanetAchievements()
		{
			achievements = new EnumMap<>(AchievementList.class);
		}
	
		Achievement getNextAchievement(double level, AchievementList listType) {
			Vector<Achievement> list = achievements.get(listType);
			// pre: list is sorted by level, with level==null at end of list 
			if (list!=null)
				for (Achievement a : list) {
					if (a.level==null)
						return null;
					if (a.level.doubleValue()>level)
						return a;
				}
			return null;
		}
	
		Double getAchievementRatio(double level, AchievementList listType)
		{
			Vector<Achievement> list = achievements.get(listType);
			// pre: list is sorted by level, with level==null at end of list 
			if (list!=null)
			{
				double lastAchievementLevel = 0;
				for (Achievement a : list) {
					if (a.level==null)
						return null;
					double achievementLevel = a.level.doubleValue();
					if (achievementLevel>level)
						return (level-lastAchievementLevel) / (achievementLevel-lastAchievementLevel);
					lastAchievementLevel = achievementLevel;
				}
			}
			return null;
		}
	}

	@Override
	public void objectTypesChanged(ObjectTypesChangeEvent event) {
		switch (event.eventType) {
		case NewTypeAdded: updateObjectTypeAssignments(); break;
		case ValueChanged: if (ObjectTypeValue.isLabel( event.changedValue )) updateObjectTypeAssignments(); break;
		}
	}

	PlanetAchievements getOrCreate(PlanetId planet)
	{
		return achievements.computeIfAbsent(planet, p->new PlanetAchievements());
	}

	private Vector<Achievement> getOrCreate(PlanetId planet, AchievementList listID)
	{
		return getOrCreate(planet)
				.achievements.computeIfAbsent(listID, al->new Vector<>());
	}

	private Vector<Achievement> get(PlanetId planet, AchievementList listID)
	{
		PlanetAchievements planetAchievements = achievements.get(planet);
		return planetAchievements==null ? null : planetAchievements.achievements.get(listID);
	}

	void updateObjectTypeAssignments() {
		boolean somethingChanged = false;
		for (PlanetId planet : PlanetId.values()) {
			PlanetAchievements planetAchievements = achievements.get(planet);
			if (planetAchievements==null) continue;
			for (AchievementList listID : AchievementList.values()) {
				Vector<Achievement> list = planetAchievements.achievements.get(listID);
				if (list==null) continue;
				for (Achievement a : list) {
					if (a.objectTypeID!=null) {
						ObjectType ot = findObjectTypeByID(a.objectTypeID);
						if (a.objectType != ot) somethingChanged = true;
						a.label = null;
						a.objectType = ot;
						// a.objectTypeID;
						
					} else if (a.label!=null && !a.label.isEmpty()) {
						ObjectType ot = findObjectTypeByName(a.label);
						if (a.objectType != ot) somethingChanged = true;
						// a.label;
						a.objectType = ot;
						a.objectTypeID = ot==null ? null : ot.id;
					}
				}
			}
		}
		if (somethingChanged)
			writeToFile();
	}
	
	private static ObjectType findObjectTypeByID(String objectTypeID) {
		return ObjectTypes.getInstance().findObjectTypeByID(objectTypeID, ObjectTypes.Occurrence.Achievement);
	}
	
	private static ObjectType findObjectTypeByName(String name) {
		return ObjectTypes.getInstance().findObjectTypeByName(name, ObjectTypes.Occurrence.Achievement);
	}

	private static Map<AchievementList, Double> getTerraformLevels(Data.AchievedValues terraformLevels)
	{
		Map<AchievementList, Double> map = new EnumMap<>(AchievementList.class);
		if (terraformLevels!=null)
			for (AchievementList listID : AchievementList.values())
				switch(listID)
				{
				case Oxygen        : map.put(listID, terraformLevels.oxygenLevel        ); break;
				case Heat          : map.put(listID, terraformLevels.heatLevel          ); break;
				case Pressure      : map.put(listID, terraformLevels.pressureLevel      ); break;
				case Plants        : map.put(listID, terraformLevels.plantsLevel        ); break;
				case Insects       : map.put(listID, terraformLevels.insectsLevel       ); break;
				case Animals       : map.put(listID, terraformLevels.animalsLevel       ); break;
				case Biomass       : map.put(listID, terraformLevels.getBiomassLevel()  ); break;
				case Terraformation:
				case Stages        : map.put(listID, terraformLevels.getTerraformLevel()); break;
				}
		return map;
	}

	private static Map<AchievementList, Double> getTerraformRates()
	{
		Map<AchievementList, Double> map = new EnumMap<>(AchievementList.class);
		
		for (PhysicalValue phVal : PhysicalValue.values())
		{
			double rate = TerraformingCalculation.getInstance().getAspect(phVal).getTotalSumBoosted();
			switch (phVal)
			{
			case Oxygen  : map.put(AchievementList.Oxygen  , rate); break;
			case Heat    : map.put(AchievementList.Heat    , rate); break;
			case Pressure: map.put(AchievementList.Pressure, rate); break;
			case Plants  : map.put(AchievementList.Plants  , rate); break;
			case Insects : map.put(AchievementList.Insects , rate); break;
			case Animals : map.put(AchievementList.Animals , rate); break;
			}
		}
		
		for (AchievementList listID : AchievementList.values())
			switch (listID)
			{
			case Oxygen: case Heat: case Pressure: case Plants: case Insects: case Animals:
				break;
				
			case Biomass:
				map.put(listID,
						map.get(AchievementList.Plants ) +
						map.get(AchievementList.Insects) +
						map.get(AchievementList.Animals)
				); 
				break;
				
			case Terraformation: case Stages:
				map.put(listID,
						map.get(AchievementList.Oxygen  ) +
						map.get(AchievementList.Heat    ) +
						map.get(AchievementList.Pressure) +
						map.get(AchievementList.Plants  ) +
						map.get(AchievementList.Insects ) +
						map.get(AchievementList.Animals )
				); 
				break;
			}
		
		return map;
	}

	static class Achievement {
		private Double level;
		private String label;
		private String objectTypeID;
		private ObjectType objectType;

		Achievement() { this(null,null,null); }
		Achievement(Double level, String label, String objectTypeID) {
			this.label = label;
			this.objectTypeID = objectTypeID;
			this.level = level;
			objectType = null;
		}
		Achievement(Achievement other) {
			this.label        = other.label       ;
			this.objectTypeID = other.objectTypeID;
			this.level        = other.level       ;
			this.objectType   = other.objectType  ;
		}
		
		boolean isEmpty() {
			return (label==null || label.isEmpty()) && (objectTypeID==null || objectTypeID.isEmpty()) && level==null;
		}

		static Achievement parseLine(String line) {
			int pos = line.indexOf('|');
			if (pos<0) return null;
			Double level = null;
			if (pos>0)
				try { level = Double.parseDouble(line.substring(0,pos)); }
				catch (NumberFormatException e) { return null; }
			
			String labelStr = line.substring(pos+1);
			String label = null;
			String objectTypeID = null;
			if (labelStr.startsWith("{") && labelStr.endsWith("}"))
				objectTypeID = labelStr.substring(1, labelStr.length()-1);
			else
				label = labelStr;
			
			return new Achievement(level, label, objectTypeID);
		}

		String toLine() {
			String labelStr =
					objectType!=null
						? String.format("{%s}", objectType.id)
						: objectTypeID!=null
							? String.format("{%s}", objectTypeID)
							: label!=null
								? label
								: "";
			if (level==null) return String.format("|%s", labelStr);
			return String.format("%s|%s", level, labelStr);
		}

		Double getLevel() {
			return level;
		}
		
		String getLabel() {
			if (objectType!=null)
				return objectType.getName();
			if (objectTypeID!=null)
				return String.format("{%s}", objectTypeID);
			return label;
		}
	}

	static class ConfigDialog extends StandardDialog
	{
		private static final long serialVersionUID = -4205705986591481227L;
		
		private final JButton btnSwitchView;
		private final JButton btnClose;
		private final JLabel statusOutput;
		private final EnumMap<AchievementList,AchievementsTablePanel> panels;
		private final JComboBox<PlanetId> planetSelector;
		private final Map<AchievementList,Double> terraformRates;
		private final Map<AchievementList,Double> terraformLevels;
		private boolean showTabbedView;
		private boolean valuesWereChanged;
	
		public ConfigDialog(Window parent, PlanetId currentPlanet, Data.AchievedValues terraformLevels)
		{
			super(parent, "Achievements Configuration");
			valuesWereChanged = false;
			showTabbedView = AppSettings.getInstance().getBool(AppSettings.ValueKey.AchievementsConfigDialogShowTabbedView, true);
			
			this.terraformRates  = getTerraformRates();
			this.terraformLevels = getTerraformLevels(terraformLevels);
			
			statusOutput = new JLabel("");
			statusOutput.setBorder(
					BorderFactory.createCompoundBorder(
							BorderFactory.createTitledBorder(""),
							BorderFactory.createEmptyBorder(0, 10, 0, 10)
					)
			);
			
			btnSwitchView = GUI.createButton(
				showTabbedView ? "Switch to Parallel View" : "Switch to Tabbed View",
				true, e->switchView()
			);
			btnClose = GUI.createButton("Close", true, e->closeDialog());
			
			panels = new EnumMap<>(AchievementList.class);
			for (AchievementList al : AchievementList.values()) {
				boolean showObjType      = al!=AchievementList.Stages;
				boolean showTIEquivalent = al!=AchievementList.Stages && al!=AchievementList.Terraformation;
				panels.put(al, new AchievementsTablePanel(
						al, al.getFormatter(),
						showObjType, showTIEquivalent,
						()->valuesWereChanged = true,
						statusOutput::setText
				));
			}
			
			planetSelector = new JComboBox<>(PlanetId.values());
			planetSelector.setSelectedItem(currentPlanet);
			planetSelector.addActionListener(e -> {
				int index = planetSelector.getSelectedIndex();
				PlanetId planet = planetSelector.getItemAt(index);
				fillPanels(planet, planet==currentPlanet);
			});
			fillPanels(currentPlanet, true);
			
			createView();
			
			AppSettings.getInstance().registerWindowSizeListener(
					this,
					AppSettings.ValueKey.AchievementsConfigDialogWidth,
					AppSettings.ValueKey.AchievementsConfigDialogHeight,
					-1, -1);
		}

		boolean wereValuesChanged()
		{
			return valuesWereChanged;
		}

		private void fillPanels(PlanetId planet, boolean isCurrentPlanet)
		{
			panels.forEach((al,panel) ->
				panel.setData(
					planet,
					Achievements.getInstance().getOrCreate(planet, al),
					isCurrentPlanet ? terraformLevels.get(al) : null,
					isCurrentPlanet ? terraformRates .get(al) : null
				)
			);
		}
		
		private void switchView() {
			showTabbedView = !showTabbedView;
			AppSettings.getInstance().putBool(AppSettings.ValueKey.AchievementsConfigDialogShowTabbedView, showTabbedView);
			btnSwitchView.setText(showTabbedView ? "Switch to Parallel View" : "Switch to Tabbed View");
			createView();
		}
		
		private void createView() {
			JComponent centerPanel =
					showTabbedView
						? new TabbedView(panels)
						: new GridView(panels);
			
			JPanel planetSelectorPanel = new JPanel(new BorderLayout());
			planetSelectorPanel.setBorder(
					BorderFactory.createCompoundBorder(
							BorderFactory.createTitledBorder(""),
							BorderFactory.createEmptyBorder(1, 4, 2, 2)
					)
			);
			planetSelectorPanel.add(new JLabel("Planet: "), BorderLayout.WEST);
			planetSelectorPanel.add(planetSelector, BorderLayout.CENTER);
			
			JPanel southPanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 1;
			c.weightx = 1;
			southPanel.add(statusOutput,c);
			c.weightx = 0;
			southPanel.add(planetSelectorPanel,c);
			southPanel.add(btnSwitchView,c);
			southPanel.add(btnClose,c);
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			contentPane.add(centerPanel,BorderLayout.CENTER);
			contentPane.add(southPanel,BorderLayout.SOUTH);
			
			createGUI( contentPane );
		}
		
		private static class TabbedView extends JTabbedPane
		{
			private static final long serialVersionUID = -3488609567657081456L;

			TabbedView(EnumMap<AchievementList,AchievementsTablePanel> panels)
			{
				for (AchievementList al : AchievementList.values()) {
					AchievementsTablePanel panel = panels.get(al);
					addTab(al.name(), panel);
				}
			}
		}
		
		private static class GridView extends JPanel
		{
			private static final long serialVersionUID = -1351998282109197686L;
			private final EnumMap<AchievementList, AchievementsTablePanel> panels;
			private final GridBagConstraints c;

			GridView(EnumMap<AchievementList,AchievementsTablePanel> panels)
			{
				super(new GridBagLayout());
				this.panels = panels;
				
				c = new GridBagConstraints();
				
				c.fill = GridBagConstraints.BOTH;
				c.weightx = 1;
				c.weighty = 1;
				
				for (AchievementList al : AchievementList.values()) {
					switch (al) {
					
					case Oxygen  : addSubPanel(al, 1, 0, 0); break;
					case Heat    : addSubPanel(al, 1, 1, 0); break;
					case Pressure: addSubPanel(al, 1, 2, 0); break;
					
					case Plants  : addSubPanel(al, 1, 0, 1); break;
					case Insects : addSubPanel(al, 1, 1, 1); break;
					case Animals : addSubPanel(al, 1, 2, 1); break;
					
					case Biomass : addSubPanel(al, 1, 0, 2); break;
					case Stages  : addSubPanel(al, 1, 1, 2); break;
					
					case Terraformation:
						addSubPanel(al, 3, 3, 0); break;
					}
				}
				
				c.gridheight = 1;
				c.gridx = 2;
				c.gridy = 2;
				add(new JLabel(), c);
			}

			private void addSubPanel(AchievementList al, int gridheight, int gridx, int gridy) {
				AchievementsTablePanel subPanel = panels.get(al);
				subPanel.setBorder(
						BorderFactory.createCompoundBorder(
								BorderFactory.createTitledBorder(al.name()),
								subPanel.defaultBorder
						)
				);
				
				c.gridheight = gridheight;
				c.gridx = gridx;
				c.gridy = gridy;
				add(subPanel, c);
			}
		}
	
		private static class AchievementsTablePanel extends JScrollPane
		{
			private static final long serialVersionUID = 5790599615513764895L;
			
			private final Border defaultBorder;
			private final AchievementsTableModel tableModel;
			private final TableContextMenu tableContextMenu;
			private Double terraformLevel;
			private Double terraformRate;
	
			AchievementsTablePanel(
					AchievementList listID,
					Function<Double, String> formatLevel,
					boolean showObjType,
					boolean showTIEquivalent,
					Runnable notifyValuesWereChanged,
					Consumer<String> setStatus
			) {
				tableModel = new AchievementsTableModel(formatLevel, showObjType, showTIEquivalent, notifyValuesWereChanged);
				
				JTable table = new JTable(tableModel);
				table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
				table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				table.getSelectionModel().addListSelectionListener(e -> {
					int rowV = table.getSelectedRow();
					int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
					Achievement achievement = tableModel.getRow(rowM);
					
					if (terraformLevel==null || terraformRate==null || achievement==null || achievement.level==null)
						setStatus.accept("");
					
					else if (achievement.level <= terraformLevel)
						setStatus.accept("Achievement \"%s\" has already been reached".formatted(achievement.getLabel()));
					
					else
					{
						double prevLevel = tableModel.getMaxAchievementLevelBelow(terraformLevel);
						double ratio = (terraformLevel-prevLevel) / (achievement.level-prevLevel);
						String ratioStr = String.format(Locale.ENGLISH, "(%1.2f%%)", ratio*100);
						double timeToReach_s = (achievement.level - terraformLevel) / terraformRate;
						String timeToReachStr = GeneralDataPanel.TerraformingStatesPanel.getDurationsString_s(timeToReach_s);
						setStatus.accept(String.format("Achievement \"%s\" will be reached %s %s", achievement.getLabel(), timeToReachStr, ratioStr));
					}
				});
				
				tableModel.setTable(table);
				tableModel.setColumnWidths(table);
				tableModel.setDefaultCellEditorsAndRenderers();
				
				tableContextMenu = new TableContextMenu(table,tableModel, listID);
				tableContextMenu.addTo(table);
				tableContextMenu.addTo(this);
				
				setViewportView(table);
				Dimension size = table.getPreferredSize();
				size.width  += 30;
				size.height = 250;
				setPreferredSize(size);
				
				defaultBorder = getBorder();
			}
			
			void setData(PlanetId displayedPlanet, Vector<Achievement> list, Double terraformLevel, Double terraformRate)
			{
				this.terraformLevel = terraformLevel;
				this.terraformRate = terraformRate;
				tableModel.setData(list, terraformLevel);
				tableContextMenu.setDisplayedPlanet(displayedPlanet);
			}

			private static class TableContextMenu extends ContextMenu {
				private static final long serialVersionUID = -2414452359411563344L;
				private PlanetId displayedPlanet;
				private final PlanetMenu addAchsMenu;
				private final PlanetMenu replaceAchsMenu;
	
				TableContextMenu(JTable table, AchievementsTableModel tableModel, AchievementList listID)
				{
					displayedPlanet = null;
					
					add(addAchsMenu     = new PlanetMenu("Add Achievements from ...", planet -> {
						Vector<Achievement> list = Achievements.getInstance().get(planet, listID);
						if (list!=null)
							tableModel.addData(list);
					}));
					add(replaceAchsMenu = new PlanetMenu("Replace with Achievements from ...", planet -> {
						Vector<Achievement> list = Achievements.getInstance().get(planet, listID);
						if (list!=null)
							tableModel.replaceData(list);
					}));
					
					addSeparator();
					
					add(GUI.createMenuItem("Show Column Widths", e->{
						System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(table));
					}));
				}

				void setDisplayedPlanet(PlanetId displayedPlanet)
				{
					if (this.displayedPlanet!=null) {
						addAchsMenu    .getMenuItem( this.displayedPlanet ).setEnabled(true);
						replaceAchsMenu.getMenuItem( this.displayedPlanet ).setEnabled(true);
					}
					
					this.displayedPlanet = displayedPlanet;
					
					if (this.displayedPlanet!=null) {
						addAchsMenu    .getMenuItem( this.displayedPlanet ).setEnabled(false);
						replaceAchsMenu.getMenuItem( this.displayedPlanet ).setEnabled(false);
					}
				}

				private static class PlanetMenu extends JMenu
				{
					private static final long serialVersionUID = -5292266203258190325L;
					private final EnumMap<PlanetId, JMenuItem> menuItems;

					PlanetMenu(String title, Consumer<PlanetId> action)
					{
						super(title);
						menuItems = new EnumMap<>(PlanetId.class);
						for (PlanetId planet : PlanetId.values())
							menuItems.put(planet, add( GUI.createMenuItem( planet.toString(), e -> action.accept(planet) ) ) );
					}

					JMenuItem getMenuItem(PlanetId planet)
					{
						return menuItems.get(planet);
					}
				}
			}
		}
		
		private static class AchievementsTableCellRenderer implements TableCellRenderer {
			
			private static final Color BGCOLOR_ACHIEVED = new Color(0xFEEFA5);
			private final AchievementsTableModel tableModel;
			private final Function<Double, String> formatLevel;
			private final Tables.LabelRendererComponent rendererComponent;
			private       Double terraformLevel;
	
			AchievementsTableCellRenderer(AchievementsTableModel tableModel, Function<Double,String> formatLevel) {
				this.tableModel = tableModel;
				this.formatLevel = formatLevel;
				this.terraformLevel = null;
				rendererComponent = new Tables.LabelRendererComponent();
			}
	
			void setTerraformLevel(Double terraformLevel)
			{
				this.terraformLevel = terraformLevel;
			}

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
				int columnM = columnV<0 ? -1 : table.convertColumnIndexToModel(columnV);
				AchievementsTableModel.ColumnID columnID = columnM<0 ? null : tableModel.getColumnID(columnM);
				
				Supplier<Color> getCustomBackground = null;
				String valueStr = value==null ? null : value.toString();
				if (columnID!=null)
					switch (columnID) {
					case Level:
						if (value instanceof Double valueL)
						{
							valueStr = formatLevel.apply(valueL);
							if (terraformLevel!=null)
								getCustomBackground = () -> valueL < terraformLevel ? BGCOLOR_ACHIEVED : null;
						}
						break;
						
					case Label:
						break;
						
					case ObjectType:
						if (value instanceof ObjectType)
							valueStr = String.format("{ %s }", ((ObjectType) value).id);
						break;
						
					case TI_Equiv:
						if (value instanceof Double)
							valueStr = Data.AchievedValues.formatTerraformation((Double) value);
						break;
					}
				
				rendererComponent.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus, getCustomBackground, null);
				if (value instanceof Number)
					rendererComponent.setHorizontalAlignment(SwingConstants.RIGHT);
				else
					rendererComponent.setHorizontalAlignment(SwingConstants.LEFT);
				
				return rendererComponent;
			}
			
		}
		
		private static class AchievementsTableModel extends Tables.SimplifiedTableModel<AchievementsTableModel.ColumnID> {
	
			enum ColumnID implements Tables.SimplifiedColumnIDInterface {
				Level     ("Level"      , Double    .class,  70),
				Label     ("Achievement", String    .class, 160),
				ObjectType("Object Type", ObjectType.class, 140),
				TI_Equiv  ("TI Equiv."  , Double    .class,  70),
				;
				static ColumnID[] values(boolean showObjType, boolean showTIEquivalent) {
					return Arrays
							.stream(values())
							.filter(id -> switch (id) {
								case ObjectType -> showObjType;
								case TI_Equiv   -> showTIEquivalent;
								default -> true;
							})
							.toArray(ColumnID[]::new);
				}
				
				private final SimplifiedColumnConfig cfg;
				ColumnID(String name, Class<?> colClass, int width) {
					cfg = new SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
				}
				@Override public SimplifiedColumnConfig getColumnConfig() {
					return cfg;
				}
			}
	
			private final AchievementsTableCellRenderer tcr;
			private Vector<Achievement> data;
			private final Runnable notifyValuesWereChanged;
	
			AchievementsTableModel(Function<Double,String> formatLevel, boolean showObjType, boolean showTIEquivalent, Runnable notifyValuesWereChanged) {
				super( ColumnID.values(showObjType, showTIEquivalent) );
				this.notifyValuesWereChanged = notifyValuesWereChanged;
				tcr = new AchievementsTableCellRenderer(this, formatLevel);
				data = null;
			}
			
			double getMaxAchievementLevelBelow(double terraformLevel)
			{
				double maxLevel = 0;
				if (data!=null)
					for (Achievement a : data)
						if (a.level!=null && a.level < terraformLevel)
							maxLevel = Math.max(a.level, maxLevel);
				
				return maxLevel;
			}

			void addData(Vector<Achievement> data) {
				if (     data==null) return; // no data to add
				if (this.data==null) return; // no achievement list assigned
				this.data.addAll(getDeepCopyOf(data));
				this.data.sort(ACHIEVEMENT_COMPARATOR);
				notifyValuesWereChanged.run();
				fireTableUpdate();
			}
			
			void replaceData(Vector<Achievement> data) {
				if (     data==null) return; // no data to replace
				if (this.data==null) return; // no achievement list assigned
				this.data.clear();
				this.data.addAll(getDeepCopyOf(data));
				this.data.sort(ACHIEVEMENT_COMPARATOR);
				notifyValuesWereChanged.run();
				fireTableUpdate();
			}

			private static List<Achievement> getDeepCopyOf(Vector<Achievement> data)
			{
				return data
						.stream()
						.map(a->new Achievement(a))
						.toList();
			}
			
			void setData(Vector<Achievement> data, Double terraformLevel) {
				this.data = data;
				tcr.setTerraformLevel(terraformLevel);
				fireTableUpdate();
			}
	
			void setDefaultCellEditorsAndRenderers() {
				setDefaultRenderers(class_-> tcr);
				
				Tables.ComboboxCellEditor<ObjectType> tce = new Tables.ComboboxCellEditor<>(()->getSorted(ObjectTypes.getInstance().values()));
				Function<Object,String> rend = obj->{
					if (obj instanceof ObjectType ot) {
						String label = ot.getLabel();
						if (label!=null && !label.isBlank()) return label;
						return String.format("{ %s }", ot.id);
					}
					return obj==null ? null : obj.toString();
				};
				tce.setRenderer(rend);
				
				table.setDefaultEditor(ObjectType.class, tce);
			}

			private Vector<ObjectType> getSorted(Collection<ObjectType> objectTypes)
			{
				objectTypes = objectTypes
					.stream()
					.map(ot->{
						String label = ot.getLabel();
						if (label == null || label.isBlank()) label = ot.id;
						return new ObjectTypeSortContainer(label.toLowerCase(), ot);
					})
					.sorted(Comparator.<ObjectTypeSortContainer,String>comparing(otsc->otsc.str))
					.map(otsc->otsc.ot)
					.toList();
				return new Vector<>(objectTypes);
			}
			private record ObjectTypeSortContainer(String str, ObjectType ot) {}
	
			@Override public int getRowCount() {
				return data==null ? 0 : data.size()+1;
			}
	
			private Achievement getRow(int rowIndex) {
				if (data==null) return null;
				if (rowIndex<0) return null;
				if (rowIndex>=data.size()) return null;
				return data.get(rowIndex);
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				Achievement a = getRow(rowIndex);
				if (a==null) return null;
				switch (columnID) {
				case Level     : return a.getLevel();
				case Label     : return a.getLabel();
				case ObjectType: return a.objectType;
				case TI_Equiv  : return a.getLevel();
				}
				return null;
			}
	
			@Override protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID) {
				switch (columnID) {
				case Level:
				case Label:
				case ObjectType:
					return true;
					
				case TI_Equiv:
					break;
				}
				return false;
			}
	
			@Override protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID) {
				if (data==null) return;
				
				Achievement a;
				if (rowIndex==data.size()) {
					// create a new achievement
					data.add(a = new Achievement());
					
				} else {
					a = getRow(rowIndex);
					if (a==null) return;
				}
				
				//System.out.printf("setValue( Achievement, %s, %s)%n", aValue==null ? "<null>" : aValue, columnID);
				ObjectType ot;
				switch (columnID) {
				case Level:
					a.level = (Double) aValue;
					break;
					
				case Label:
					String labelStr = (String) aValue;
					ot = findObjectTypeByName(labelStr);
					if (ot!=null) {
						a.label = null;
						a.objectType = ot;
						a.objectTypeID = ot.id;
					} else {
						a.label = labelStr;
						a.objectType = null;
						a.objectTypeID = null;
					}
					break;
					
				case ObjectType:
					ot = (ObjectType) aValue;
					a.label = null;
					if (ot!=null) {
						a.objectType = ot;
						a.objectTypeID = ot.id;
					} else {
						a.objectType = null;
						a.objectTypeID = null;
					}
					break;
					
				case TI_Equiv: break;
				}
				
				if (a.isEmpty())
					SwingUtilities.invokeLater(()->{
						data.remove(a);
						fireTableRowRemoved(rowIndex);
					});
				else
					SwingUtilities.invokeLater(()->{
						data.sort(ACHIEVEMENT_COMPARATOR);
						fireTableUpdate();
					});
				
				notifyValuesWereChanged.run();
			}
			
		}
	
	}
}
