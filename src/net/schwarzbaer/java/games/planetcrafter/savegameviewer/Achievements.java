package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

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
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectTypeValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeEvent;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeListener;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.PlanetCrafterSaveGameViewer.AppSettings;
import net.schwarzbaer.java.lib.gui.ContextMenu;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedTableModel;

class Achievements implements ObjectTypesChangeListener {
	
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
	
	private final EnumMap<AchievementList,Vector<Achievement>> achievements;
	private ObjectTypes objectTypes;
	
	Achievements() {
		achievements = new EnumMap<>(AchievementList.class);
		objectTypes = null;
	}

	static Achievements readFromFile() {
		File file = new File(PlanetCrafterSaveGameViewer.FILE_ACHIEVEMENTS); 		
		Achievements achievements = new Achievements();
		
		System.out.printf("Read Achievements from file \"%s\" ...%n", file.getAbsolutePath());
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			
			String line;
			Achievement value;
			Vector<Achievement> currentList = null;
			while ( (line=in.readLine())!=null ) {
				
				if (line.isEmpty())
					continue;
				
				AchievementList listType = AchievementList.valueOf_checked(line);
				if (listType!=null) {
					currentList = achievements.achievements.get(listType);
					if (currentList==null)
						achievements.achievements.put(listType, currentList = new Vector<>());
					continue;
				}
				
				if ( (value=Achievement.parseLine(line))!=null )
					if (!value.isEmpty())
						currentList.add(value);
				
			}
			
		} catch (FileNotFoundException ex) {
			//ex.printStackTrace();
		} catch (IOException ex) {
			System.err.printf("IOException while reading Achievements: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
		
		return achievements;
	}

	void sortAchievements() {
		forEachList((al,list) -> list.sort( ACHIEVEMENT_COMPARATOR ));
	}
	
	void writeToFile() {
		File file = new File(PlanetCrafterSaveGameViewer.FILE_ACHIEVEMENTS); 		
		System.out.printf("Write Achievements to file \"%s\" ...%n", file.getAbsolutePath());
		
		try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
			
			forEachList((al,list) -> {
				if (!list.isEmpty()) {
					Vector<Achievement> sorted = new Vector<>(list);
					sorted.sort(ACHIEVEMENT_COMPARATOR);
					out.println(al.name());
					for (Achievement a : sorted)
						out.println(a.toLine());
					out.println();
				}
			});
			
		} catch (IOException ex) {
			System.err.printf("IOException while writing Achievements: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}

	void forEachList(BiConsumer<AchievementList,Vector<Achievement>> action) {
		for (AchievementList al : AchievementList.values()) {
			Vector<Achievement> list = achievements.get(al);
			if (list!=null)
				action.accept(al,list);
		}
	}

	Achievement getNextAchievement(double level, AchievementList listType) {
		Vector<Achievement> list = getAchievementsList(listType);
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

	private Vector<Achievement> getAchievementsList(AchievementList listType) {
		return achievements.get(listType);
	}

	Double getAchievementRatio(double level, AchievementList listType)
	{
		Vector<Achievement> list = getAchievementsList(listType);
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

	@Override
	public void objectTypesChanged(ObjectTypesChangeEvent event) {
		switch (event.eventType) {
		case NewTypeAdded: updateObjectTypeAssignments(); break;
		case ValueChanged: if (ObjectTypeValue.isLabel( event.changedValue )) updateObjectTypeAssignments(); break;
		}
	}

	void setObjectTypesData(ObjectTypes objectTypes) {
		this.objectTypes = objectTypes;
		updateObjectTypeAssignments();
	}

	private void updateObjectTypeAssignments() {
		if (objectTypes==null) return;
		boolean somethingChanged = false;
		for (AchievementList listID : AchievementList.values()) {
			Vector<Achievement> list = achievements.get(listID);
			if (list!=null)
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
		if (somethingChanged)
			writeToFile();
	}
	
	private ObjectType findObjectTypeByID(String objectTypeID) {
		if (objectTypes==null) return null;
		return objectTypes.findObjectTypeByID(objectTypeID, ObjectTypes.Occurrence.Achievement);
	}
	
	private ObjectType findObjectTypeByName(String name) {
		if (objectTypes==null) return null;
		return objectTypes.findObjectTypeByName(name, ObjectTypes.Occurrence.Achievement);
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

	static class ConfigDialog extends StandardDialog {
		private static final long serialVersionUID = -4205705986591481227L;
		
		private boolean showTabbedView;
		private final JButton btnSwitchView;
		private final JButton btnClose;
		private final EnumMap<AchievementList,AchievementsTablePanel> panels;
	
		public ConfigDialog(Window parent, Achievements achievements) {
			super(parent, "Achievements Configuration");
			
			showTabbedView = AppSettings.getInstance().getBool(AppSettings.ValueKey.AchievementsConfigDialogShowTabbedView, true);
			btnSwitchView = GUI.createButton(
				showTabbedView ? "Switch to Parallel View" : "Switch to Tabbed View",
				true, e->switchView()
			);
			btnClose = GUI.createButton("Close", true, e->closeDialog());
			
			panels = new EnumMap<>(AchievementList.class);
			for (AchievementList al : AchievementList.values()) {
				Vector<Achievement> list = achievements.achievements.get(al);
				if (list == null) achievements.achievements.put(al, list = new Vector<>());
				boolean showObjType      = al!=AchievementList.Stages;
				boolean showTIEquivalent = al!=AchievementList.Stages && al!=AchievementList.Terraformation;
				panels.put(al, new AchievementsTablePanel(achievements, al, list, al.getFormatter(), showObjType, showTIEquivalent));
			}
			
			createView();
			
			AppSettings.getInstance().registerWindowSizeListener(
					this,
					AppSettings.ValueKey.AchievementsConfigDialogWidth,
					AppSettings.ValueKey.AchievementsConfigDialogHeight,
					-1, -1);
		}
	
		private void switchView() {
			showTabbedView = !showTabbedView;
			AppSettings.getInstance().putBool(AppSettings.ValueKey.AchievementsConfigDialogShowTabbedView, showTabbedView);
			btnSwitchView.setText(showTabbedView ? "Switch to Parallel View" : "Switch to Tabbed View");
			createView();
		}
	
		private void createView() {
			JComponent contentPane;
			
			if (showTabbedView) {
				JTabbedPane tabbedPane = new JTabbedPane();
				contentPane = tabbedPane;
				for (AchievementList al : AchievementList.values()) {
					AchievementsTablePanel panel = panels.get(al);
					tabbedPane.addTab(al.name(), panel);
				}
				
			} else {
				JPanel gridPanel = new JPanel(new GridBagLayout());
				contentPane = gridPanel;
				
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				c.weightx = 1;
				c.weighty = 1;
				
				for (AchievementList al : AchievementList.values()) {
					switch (al) {
					
					case Oxygen  : addSubPanel(gridPanel, al, c, 1, 0, 0); break;
					case Heat    : addSubPanel(gridPanel, al, c, 1, 1, 0); break;
					case Pressure: addSubPanel(gridPanel, al, c, 1, 2, 0); break;
					
					case Plants  : addSubPanel(gridPanel, al, c, 1, 0, 1); break;
					case Insects : addSubPanel(gridPanel, al, c, 1, 1, 1); break;
					case Animals : addSubPanel(gridPanel, al, c, 1, 2, 1); break;
					
					case Biomass : addSubPanel(gridPanel, al, c, 1, 0, 2); break;
					case Stages  : addSubPanel(gridPanel, al, c, 1, 1, 2); break;
					
					case Terraformation:
						addSubPanel(gridPanel, al, c, 3, 3, 0); break;
					}
				}
				
				c.gridheight = 1;
				c.gridx = 2;
				c.gridy = 2;
				gridPanel.add( new JLabel(), c );
			}
			
			createGUI( contentPane, btnSwitchView, btnClose );
		}
	
		private void addSubPanel(JPanel panel, AchievementList al, GridBagConstraints c, int gridheight, int gridx, int gridy) {
			AchievementsTablePanel subPanel = panels.get(al);
			addTitledBorder(al.name(), subPanel);
			
			c.gridheight = gridheight;
			c.gridx = gridx;
			c.gridy = gridy;
			panel.add(subPanel, c);
		}
	
		private void addTitledBorder(String title, AchievementsTablePanel subPanel) {
			subPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), subPanel.getDefaultBorder()));
		}
	
		private static class AchievementsTablePanel extends JScrollPane {
			private static final long serialVersionUID = 5790599615513764895L;
			private final Border defaultBorder;
	
			AchievementsTablePanel(Achievements main, AchievementList listID, Vector<Achievement> list, Function<Double, String> formatLevel, boolean showObjType, boolean showTIEquivalent) {
				
				AchievementsTableModel tableModel = new AchievementsTableModel(main, list, formatLevel, showObjType, showTIEquivalent);
				
				JTable table = new JTable(tableModel);
				table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
				table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				
				tableModel.setTable(table);
				tableModel.setColumnWidths(table);
				tableModel.setDefaultCellEditorsAndRenderers();
				
				new TableContextMenu(table,tableModel);
				
				setViewportView(table);
				Dimension size = table.getPreferredSize();
				size.width  += 30;
				size.height = 250;
				setPreferredSize(size);
				System.out.printf("PreferredWidth: [%s] %d%n", listID, size.width);
				
				defaultBorder = getBorder();
			}
			
	
			Border getDefaultBorder() {
				return defaultBorder;
			}


			private static class TableContextMenu extends ContextMenu {
				private static final long serialVersionUID = -2414452359411563344L;
	
				TableContextMenu(JTable table, AchievementsTableModel tableModel) {
					add(GUI.createMenuItem("Show Column Widths", e->{
						System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(table));
					}));
					
					addTo(table);
				}
			}
		}
		
		private static class AchievementsTableCellRenderer implements TableCellRenderer {
			
			private final AchievementsTableModel tableModel;
			private final Function<Double, String> formatLevel;
			private final Tables.LabelRendererComponent rendererComponent;
	
			AchievementsTableCellRenderer(AchievementsTableModel tableModel, Function<Double,String> formatLevel) {
				this.tableModel = tableModel;
				this.formatLevel = formatLevel;
				rendererComponent = new Tables.LabelRendererComponent();
			}
	
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
				int columnM = columnV<0 ? -1 : table.convertColumnIndexToModel(columnV);
				AchievementsTableModel.ColumnID columnID = columnM<0 ? null : tableModel.getColumnID(columnM);
				
				String valueStr = value==null ? null : value.toString();
				if (columnID!=null)
					switch (columnID) {
					case Level:
						if (value instanceof Double)
							valueStr = formatLevel.apply((Double) value);
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
				
				rendererComponent.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus);
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
					if (showObjType && showTIEquivalent) return values();
					if (showObjType     ) return new ColumnID[] { Level, Label, ObjectType };
					if (showTIEquivalent) return new ColumnID[] { Level, Label, TI_Equiv };
					return new ColumnID[] { Level, Label };
				}
				
				private final SimplifiedColumnConfig cfg;
				ColumnID(String name, Class<?> colClass, int width) {
					cfg = new SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
				}
				@Override public SimplifiedColumnConfig getColumnConfig() {
					return cfg;
				}
			}
	
			private final Achievements main;
			private final Vector<Achievement> data;
			private final Function<Double, String> formatLevel;
	
			AchievementsTableModel(Achievements main, Vector<Achievement> data, Function<Double,String> formatLevel, boolean showObjType, boolean showTIEquivalent) {
				super( ColumnID.values(showObjType, showTIEquivalent) );
				this.main = main;
				this.data = data;
				this.formatLevel = formatLevel;
			}
	
			void setDefaultCellEditorsAndRenderers() {
				AchievementsTableCellRenderer tcr = new AchievementsTableCellRenderer(this,formatLevel);
				table.setDefaultRenderer(Double.class, tcr);
				table.setDefaultRenderer(String.class, tcr);
				table.setDefaultRenderer(ObjectType.class, tcr);
				
				Tables.ComboboxCellEditor<ObjectType> tce = new Tables.ComboboxCellEditor<>(()->getSorted(main.objectTypes.values()));
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
				return data.size()+1;
			}
	
			private Achievement getRow(int rowIndex) {
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
					ot = main.findObjectTypeByName(labelStr);
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
			}
			
		}
	
	}
}
