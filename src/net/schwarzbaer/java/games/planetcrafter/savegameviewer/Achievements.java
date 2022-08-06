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
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;

class Achievements {
	
	private static final Comparator<Achievement> ACHIEVEMENT_COMPARATOR = Comparator
	.<Achievement,Double>comparing(a->a.level, Comparator.nullsLast(Comparator.naturalOrder()))
	.thenComparing(a->a.label);
	
	enum AchievementList {
		Oxygen, Heat, Pressure, Biomass, Plants, Insects, Animals, Terraformation, Stages;
		static AchievementList valueOf_checked(String str) {
			try { return valueOf(str); }
			catch (Exception e) { return null; }
		}

		Function<Double, String> getFormatter() {
			switch (this) {
			case Oxygen  : return Data.TerraformingStates::formatOxygenLevel  ;
			case Heat    : return Data.TerraformingStates::formatHeatLevel    ;
			case Pressure: return Data.TerraformingStates::formatPressureLevel;
			case Biomass: case Plants: case Insects: case Animals:
				return Data.TerraformingStates::formatBiomassLevel;
			case Terraformation: case Stages:
				return Data.TerraformingStates::formatTerraformation;
			}
			return null;
		}
	}
	
	private final EnumMap<AchievementList,Vector<Achievement>> achievements;
	
	Achievements() {
		achievements = new EnumMap<>(AchievementList.class);
	}

	static Achievements readFromFile(File file) {
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
		
		achievements.forEachList((al,list) -> list.sort( ACHIEVEMENT_COMPARATOR ));
		
		System.out.printf("Done%n");
		
		return achievements;
	}
	
	void forEachList(BiConsumer<AchievementList,Vector<Achievement>> action) {
		for (AchievementList al : AchievementList.values()) {
			Vector<Achievement> list = achievements.get(al);
			if (list!=null)
				action.accept(al,list);
		}
	}

	void writeToFile(File file) {
		
		System.out.printf("Write Achievements to file \"%s\" ...%n", file.getAbsolutePath());
		
		try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
			
			forEachList((al,list) -> {
				if (!list.isEmpty()) {
					out.println(al.name());
					for (Achievement a : list)
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

	static class Achievement {
		private String label;
		private Double level;

		Achievement() { this("",null); }
		Achievement(String label, Double level) {
			this.label = label;
			this.level = level;
		}
		
		boolean isEmpty() {
			return (label==null || label.isEmpty()) && level==null;
		}

		static Achievement parseLine(String line) {
			int pos = line.indexOf('|');
			if (pos<0) return null;
			Double level = null;
			if (pos>0)
				try { level = Double.parseDouble(line.substring(0,pos)); }
				catch (NumberFormatException e) { return null; }
			return new Achievement(line.substring(pos+1), level);
		}

		String toLine() {
			if (level==null) return String.format("|%s", label);
			return String.format("%s|%s", level, label);
		}

		String getLabel() { return label; }
		Double getLevel() { return level; }
	}

	static class ConfigDialog extends StandardDialog {
		private static final long serialVersionUID = -4205705986591481227L;
		
		private boolean showTabbedView;
		private final JButton btnSwitchView;
		private final JButton btnClose;
		private final EnumMap<AchievementList,AchievementsTablePanel> panels;
	
		public ConfigDialog(Window parent, Achievements achievements) {
			super(parent, "Achievements Configuration");
			
			showTabbedView = PlanetCrafterSaveGameViewer.settings.getBool(PlanetCrafterSaveGameViewer.AppSettings.ValueKey.AchievementsConfigDialogShowTabbedView, true);
			btnSwitchView = GUI.createButton(
				showTabbedView ? "Switch to Parallel View" : "Switch to Tabbed View",
				true, e->switchView()
			);
			btnClose = GUI.createButton("Close", true, e->closeDialog());
			
			panels = new EnumMap<>(AchievementList.class);
			for (AchievementList al : AchievementList.values()) {
				Vector<Achievement> list = achievements.achievements.get(al);
				if (list == null) achievements.achievements.put(al, list = new Vector<>());
				panels.put(al, new AchievementsTablePanel(list, al.getFormatter(), al!=AchievementList.Stages && al!=AchievementList.Terraformation));
			}
			
			createView();
			
			PlanetCrafterSaveGameViewer.settings.registerWindowSizeListener(
					this,
					PlanetCrafterSaveGameViewer.AppSettings.ValueKey.AchievementsConfigDialogWidth,
					PlanetCrafterSaveGameViewer.AppSettings.ValueKey.AchievementsConfigDialogHeight,
					-1, -1);
		}
	
		private void switchView() {
			showTabbedView = !showTabbedView;
			PlanetCrafterSaveGameViewer.settings.putBool(PlanetCrafterSaveGameViewer.AppSettings.ValueKey.AchievementsConfigDialogShowTabbedView, showTabbedView);
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
					case Stages  : addSubPanel(gridPanel, al, c, 1, 3, 0); break;
					
					case Biomass : addSubPanel(gridPanel, al, c, 1, 0, 1); break;
					case Plants  : addSubPanel(gridPanel, al, c, 1, 1, 1); break;
					case Insects : addSubPanel(gridPanel, al, c, 1, 2, 1); break;
					case Animals : addSubPanel(gridPanel, al, c, 1, 3, 1); break;
					
					case Terraformation:
						addSubPanel(gridPanel, al, c, 2, 4, 0); break;
					}
				}
				
				c.gridheight = 1;
				c.gridy = 0;
				c.gridx = 3;
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
			subPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), subPanel.getBorder()));
		}
	
		private static class AchievementsTablePanel extends JScrollPane {
			private static final long serialVersionUID = 5790599615513764895L;
	
			AchievementsTablePanel(Vector<Achievement> achievements, Function<Double, String> formatLevel, boolean showTIEquivalent) {
				
				AchievementsTableModel tableModel = new AchievementsTableModel(achievements, formatLevel, showTIEquivalent);
				
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
			
			private final Tables.LabelRendererComponent rendererComponent;
			private final Function<Double, String> formatLevel;
	
			AchievementsTableCellRenderer(Function<Double,String> formatLevel) {
				this.formatLevel = formatLevel;
				rendererComponent = new Tables.LabelRendererComponent();
			}
	
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				
				String valueStr = value==null ? null : value.toString();
				if (value instanceof Double) valueStr = formatLevel.apply((Double) value);
				
				rendererComponent.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus);
				rendererComponent.setHorizontalAlignment(SwingConstants.RIGHT);
				return rendererComponent;
			}
			
		}
		
		private static class AchievementsTableModel extends Tables.SimplifiedTableModel<AchievementsTableModel.ColumnID> {
	
			enum ColumnID implements Tables.SimplifiedColumnIDInterface {
				Level   ("Level"      , Double.class,  70),
				Label   ("Achievement", String.class, 200),
				TI_Equiv("TI Equiv."  , Double.class,  70),
				;
				static ColumnID[] values(boolean showTIEquivalent) {
					return !showTIEquivalent ? new ColumnID[] { Level, Label } : values();
				}
				
				private final SimplifiedColumnConfig cfg;
				ColumnID(String name, Class<?> colClass, int width) {
					cfg = new SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
				}
				@Override public SimplifiedColumnConfig getColumnConfig() {
					return cfg;
				}
			}
	
			private final Vector<Achievement> data;
			private final Function<Double, String> formatLevel;
	
			AchievementsTableModel(Vector<Achievement> data, Function<Double,String> formatLevel, boolean showTIEquivalent) {
				super( ColumnID.values(showTIEquivalent) );
				this.data = data;
				this.formatLevel = formatLevel;
			}
	
			void setDefaultCellEditorsAndRenderers() {
				table.setDefaultRenderer(Double.class, new AchievementsTableCellRenderer(formatLevel));
			}
	
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
				case Label: return a.label;
				case Level: return a.level;
				case TI_Equiv: return a.level==null ? null : Data.TerraformingStates.formatTerraformation(a.level);
				}
				return null;
			}
	
			@Override protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID) {
				switch (columnID) {
				case Label: case Level: return true;
				case TI_Equiv: break;
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
				switch (columnID) {
				case Label: a.label = (String) aValue; break;
				case Level: a.level = (Double) aValue; break;
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
