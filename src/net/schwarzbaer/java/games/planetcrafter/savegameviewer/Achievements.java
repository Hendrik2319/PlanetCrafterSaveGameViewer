package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Component;
import java.awt.Dimension;
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
import java.util.Vector;
import java.util.function.Function;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectType.PhysicalValue;

class Achievements {
	
	private static final Comparator<Achievement> ACHIEVEMENT_COMPARATOR = Comparator
	.<Achievement,Double>comparing(a->a.level, Comparator.nullsLast(Comparator.naturalOrder()))
	.thenComparing(a->a.label);
	private final Vector<Achievement>    oxygenAchievements;
	private final Vector<Achievement>      heatAchievements;
	private final Vector<Achievement>  pressureAchievements;
	private final Vector<Achievement>   biomassAchievements;
	private final Vector<Achievement> terraformAchievements;
	
	Achievements() {
		this.   oxygenAchievements = new Vector<>();
		this.     heatAchievements = new Vector<>();
		this. pressureAchievements = new Vector<>();
		this.  biomassAchievements = new Vector<>();
		this.terraformAchievements = new Vector<>();
	}

	static class ConfigDialog extends StandardDialog {
		private static final long serialVersionUID = -4205705986591481227L;

		public ConfigDialog(Window parent, Achievements achievements) {
			super(parent, "Achievements Configuration");
			
			JTabbedPane contentPane = new JTabbedPane();
			contentPane.addTab(PhysicalValue.Oxygen  .name(), new AchievementsTablePanel(achievements.   oxygenAchievements, Data.TerraformingStates::formatOxygenLevel   ));
			contentPane.addTab(PhysicalValue.Heat    .name(), new AchievementsTablePanel(achievements.     heatAchievements, Data.TerraformingStates::formatHeatLevel     ));
			contentPane.addTab(PhysicalValue.Pressure.name(), new AchievementsTablePanel(achievements. pressureAchievements, Data.TerraformingStates::formatPressureLevel ));
			contentPane.addTab(PhysicalValue.Biomass .name(), new AchievementsTablePanel(achievements.  biomassAchievements, Data.TerraformingStates::formatBiomassLevel  ));
			contentPane.addTab("Terraformation"             , new AchievementsTablePanel(achievements.terraformAchievements, Data.TerraformingStates::formatTerraformation));
			
			createGUI(contentPane, PlanetCrafterSaveGameViewer.createButton("Close", true, e->closeDialog()));
		}
		
		private static class AchievementsTablePanel extends JScrollPane {
			private static final long serialVersionUID = 5790599615513764895L;

			AchievementsTablePanel(Vector<Achievement> achievements, Function<Double, String> formatLevel) {
				
				AchievementsTableModel tableModel = new AchievementsTableModel(achievements, formatLevel);
				
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
					add(PlanetCrafterSaveGameViewer.createMenuItem("Show Column Widths", e->{
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
				Level("Level"      , Double.class,  75),
				Label("Achievement", String.class, 200),
				;
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

			AchievementsTableModel(Vector<Achievement> data, Function<Double,String> formatLevel) {
				super(ColumnID.values());
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
				}
				return null;
			}
		}

	}

	static Achievements readFromFile(File file) {
		Achievements achievements = new Achievements();
		
		System.out.printf("Read Achievements from file \"%s\" ...%n", file.getAbsolutePath());
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			
			String line;
			Achievement value;
			Vector<Achievement> currentList = null;
			while ( (line=in.readLine())!=null ) {
				
				if (line.isEmpty()) continue;
				if (line.equals(PhysicalValue.Oxygen  .name())) currentList = achievements.   oxygenAchievements;
				if (line.equals(PhysicalValue.Heat    .name())) currentList = achievements.     heatAchievements;
				if (line.equals(PhysicalValue.Pressure.name())) currentList = achievements. pressureAchievements;
				if (line.equals(PhysicalValue.Biomass .name())) currentList = achievements.  biomassAchievements;
				if (line.equals("Terraformation"             )) currentList = achievements.terraformAchievements;
				if ( (value=Achievement.parseLine(line))!=null    ) currentList.add(value);
				
			}
			
		} catch (FileNotFoundException ex) {
			//ex.printStackTrace();
		} catch (IOException ex) {
			System.err.printf("IOException while reading Achievements: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		achievements.   oxygenAchievements.sort( ACHIEVEMENT_COMPARATOR );
		achievements.     heatAchievements.sort( ACHIEVEMENT_COMPARATOR );
		achievements. pressureAchievements.sort( ACHIEVEMENT_COMPARATOR );
		achievements.  biomassAchievements.sort( ACHIEVEMENT_COMPARATOR );
		achievements.terraformAchievements.sort( ACHIEVEMENT_COMPARATOR );
		
		System.out.printf("Done%n");
		
		return achievements;
	}

	void writeToFile(File file) {
		
		System.out.printf("Write ObjectTypes to file \"%s\" ...%n", file.getAbsolutePath());
		
		try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
			
			writeToFile(out,PhysicalValue.Oxygen  .name(),   oxygenAchievements);
			writeToFile(out,PhysicalValue.Heat    .name(),     heatAchievements);
			writeToFile(out,PhysicalValue.Pressure.name(), pressureAchievements);
			writeToFile(out,PhysicalValue.Biomass .name(),  biomassAchievements);
			writeToFile(out,"Terraformation"             ,terraformAchievements);
			
		} catch (IOException ex) {
			System.err.printf("IOException while writing ObjectTypes: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}

	private void writeToFile(PrintWriter out, String label, Vector<Achievement> achievements) {
		out.println(label);
		for (Achievement a : achievements)
			out.println(a.toLine());
		out.println();
	}

	Achievement getNextAchievement(double level, PhysicalValue physicalValue) {
		Vector<Achievement> list = getAchievementsList(physicalValue);
		// pre: list is sorted by level, with level==null at end of list 
		for (Achievement a : list) {
			if (a.level==null)
				return null;
			if (a.level.doubleValue()>level)
				return a;
		}
		return null;
	}

	private Vector<Achievement> getAchievementsList(PhysicalValue physicalValue) {
		if (physicalValue==null)
			return terraformAchievements;
		switch (physicalValue) {
		case Oxygen  : return   oxygenAchievements;
		case Heat    : return     heatAchievements;
		case Pressure: return pressureAchievements;
		case Biomass : return  biomassAchievements;
		}
		return null;
	}

	static class Achievement {
		private String label;
		private Double level;

		Achievement() { this("",null); }
		Achievement(String label, Double level) {
			this.label = label;
			this.level = level;
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
}
