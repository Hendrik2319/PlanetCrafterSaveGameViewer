package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypeValue;

class GeneralDataPanel extends JScrollPane implements ObjectTypesPanel.DataChangeListener {
	private static final long serialVersionUID = -9191759791973305801L;
	
	//private final Data data;
	private final EnergyPanel energyPanel;

	GeneralDataPanel(Data data) {
		//this.data = data;
		GridBagConstraints c;
		
		
		
		JPanel upperPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 1;
		
		c.gridheight = 1;
		c.gridx = 0;
		c.gridy = 0; upperPanel.add(createPanel("Terraforming"    , data.terraformingStates, TerraformingStatesPanel::new), c);
		c.gridy = 1; upperPanel.add(createPanel("General Data (1)", data.generalData1      , GeneralData1Panel      ::new), c);
		c.gridy = 2; upperPanel.add(createPanel("General Data (2)", data.generalData2      , GeneralData2Panel      ::new), c);
		
		c.gridheight = 3;
		c.gridx = 1;
		c.gridy = 0; upperPanel.add(createPanel("Player"          , data.playerStates      , PlayerStatesPanel      ::new), c);
		
		c.gridheight = 3;
		c.gridx = 2;
		c.gridy = 0; upperPanel.add(energyPanel = new EnergyPanel(data), c);
		
		
		
		JPanel lowerPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridy = 0;
		c.gridx = -1;
		
		c.gridx++;
		lowerPanel.add(
				new SimpleTablePanel<Data.Message>("Messages", data.messages,
					new SimpleTablePanel.Column("ID"      , String .class, 170, row->((Data.Message)row).stringId),
					new SimpleTablePanel.Column("Is Read?", Boolean.class,  60, row->((Data.Message)row).isRead  )
				), c);
		
		c.gridx++;
		lowerPanel.add(
				new SimpleTablePanel<Data.StoryEvent>("StoryEvents", data.storyEvents,
					new SimpleTablePanel.Column("ID"      , String .class, 230, row->((Data.StoryEvent)row).stringId)
				), c);
		
		c.gridx++;
		lowerPanel.add(
			new SimpleTablePanel<Data.Layer>("Layers", data.layers,
				new SimpleTablePanel.Column("ID"              , String.class,  75, row->((Data.Layer)row).layerId        ),
				new SimpleTablePanel.Column("Color Base      ", String.class, 180, row->((Data.Layer)row).colorBase      ),
				new SimpleTablePanel.Column("Color Custom    ", String.class,  90, row->((Data.Layer)row).colorCustom    ),
				new SimpleTablePanel.Column("Color BaseLerp  ", Long  .class,  90, row->((Data.Layer)row).colorBaseLerp  ),
				new SimpleTablePanel.Column("Color CustomLerp", Long  .class, 100, row->((Data.Layer)row).colorCustomLerp)
			), c);
		
		
		
		JPanel mainPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;
		
		c.gridy = 0; mainPanel.add(upperPanel, c);
		c.gridy = 1; mainPanel.add(lowerPanel, c);
		
		
		setViewportView(mainPanel);
		//System.out.printf("%d, %d%n", horizontalScrollBar.getUnitIncrement(), verticalScrollBar.getUnitIncrement());
		horizontalScrollBar.setUnitIncrement(10);
		verticalScrollBar  .setUnitIncrement(10);
		//System.out.printf("%d, %d%n", horizontalScrollBar.getUnitIncrement(), verticalScrollBar.getUnitIncrement());
	}
	
	@Override
	public void objectTypeValueChanged(String objectTypeID, ObjectTypeValue changedValue) {
		energyPanel.objectTypeValueChanged(objectTypeID, changedValue);
	}

	private <ValueType> JComponent createPanel(String title, Vector<ValueType> values, Function<ValueType,JPanel> panelConstructor) {
		if (values==null) throw new IllegalArgumentException();
		
		if (values.isEmpty()) {
			JPanel panel = new JPanel();
			panel.setBorder(BorderFactory.createTitledBorder(title));
			return panel;
		}
		
		if (values.size()==1) {
			JPanel panel = panelConstructor.apply(values.get(0));
			panel.setBorder(BorderFactory.createTitledBorder(title));
			return panel;
		}
		
		JTabbedPane panel = new JTabbedPane();
		panel.setBorder(BorderFactory.createTitledBorder(title));
		for (int i=0; i<values.size(); i++)
			panel.addTab(Integer.toString(i), panelConstructor.apply(values.get(i)));
		
		return panel;
	}

	private static JTextField createOutputTextField(String text) {
		JTextField comp = new JTextField(text);
		comp.setEditable(false);
		return comp;
	}

	private static class EnergyPanel extends JPanel {
		private static final long serialVersionUID = 6260130212445154141L;
		
		private final ObjectsPanel sourcesPanel;
		private final ObjectsPanel consumersPanel;
		private final BudgetPanel  budgetPanel;

		EnergyPanel(Data data) {
			super(new BorderLayout());
			//setBorder(BorderFactory.createTitledBorder("Energy"));
			//setPreferredSize(new Dimension(250,200));
			
			sourcesPanel = new ObjectsPanel(data.worldObjects, "Energy Sources", true);
			consumersPanel = new ObjectsPanel(data.worldObjects, "Energy Consumers", false);
			budgetPanel = new BudgetPanel("Energy Budget", sourcesPanel, consumersPanel);
			
			JPanel centerPanel = new JPanel(new GridLayout(1,0));
			centerPanel.add(sourcesPanel);
			centerPanel.add(consumersPanel);
			
			add(budgetPanel, BorderLayout.NORTH);
			add(centerPanel, BorderLayout.CENTER);
			
			SwingUtilities.invokeLater(this::updateValues);
		}

		void objectTypeValueChanged(String objectTypeID, ObjectTypeValue changedValue) {
			if (changedValue==ObjectTypeValue.Energy || changedValue==ObjectTypeValue.Label) {
				updateValues();
			}
		}

		void updateValues() {
			sourcesPanel.updateValues();
			consumersPanel.updateValues();
			budgetPanel.updateValues();
		}
		
		private static class ObjectsPanel extends JScrollPane {
			private static final long serialVersionUID = -7778016200735929929L;
			
			private final Vector<WorldObject> worldObjects;
			private final boolean computeSources;
			private final ObjectsTableModel tableModel;
			private double totalSum;

			ObjectsPanel(Vector<Data.WorldObject> worldObjects, String title, boolean computeSources) {
				this.worldObjects = worldObjects;
				this.computeSources = computeSources;
				totalSum = 0;
				
				tableModel = new ObjectsTableModel();
				JTable table = new JTable(tableModel);
				table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
				table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				
				tableModel.setTable(table);
				tableModel.setColumnWidths(table);
				tableModel.setDefaultCellEditorsAndRenderers();
				
				setViewportView(table);
				Dimension size = table.getPreferredSize();
				size.width  += 30;
				size.height = 150;
				setPreferredSize(size);
				
				setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), getBorder()));
			}

			void updateValues() {
				HashMap<String,ObjectsTableRow> tableContent = new HashMap<>();
				totalSum = 0.0;
				for (WorldObject wo : worldObjects) {
					if (wo == null) continue;
					if (wo.objectType == null) continue;
					if (wo.objectType.energy == null) continue;
					
					double energy = wo.objectType.energy.doubleValue();
					if (( computeSources && energy>0) ||
						(!computeSources && energy<0) ) {
						ObjectsTableRow row = tableContent.get(wo.objectTypeID);
						if (row==null) tableContent.put(wo.objectTypeID, row = new ObjectsTableRow(wo.getName()));
						row.count++;
						row.sum += energy;
						totalSum += energy;
					}
				}
				tableModel.setData(tableContent);
			}

			double getSum() { return totalSum; }
			
			private static class ObjectsTableRow {
				final String name;
				int count;
				double sum;
				ObjectsTableRow(String name) {
					this.name = name;
					count = 0;
					sum = 0;
				}
			}
			
			private static class ObjectsTableCellRenderer implements TableCellRenderer {
				
				private final Tables.LabelRendererComponent rendererComponent;

				ObjectsTableCellRenderer() {
					rendererComponent = new Tables.LabelRendererComponent();
				}

				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					
					String valueStr = value==null ? null : value.toString();
					if (value instanceof Double ) valueStr = String.format(Locale.ENGLISH, "%1.2f kW", value);
					if (value instanceof Integer) valueStr = String.format(Locale.ENGLISH, "%d x ", value);
					
					rendererComponent.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus);
					rendererComponent.setHorizontalAlignment(SwingConstants.RIGHT);
					return rendererComponent;
				}
				
			}
			
			private static class ObjectsTableModel extends Tables.SimplifiedTableModel<ObjectsTableModel.ColumnID>{
				
				enum ColumnID implements Tables.SimplifiedColumnIDInterface {
					Count ("Count" , Integer.class,  50),
					Name  ("Name"  , String .class, 130),
					Energy("Energy", Double .class,  80),
					;
					private final SimplifiedColumnConfig cfg;
					ColumnID(String name, Class<?> colClass, int width) {
						cfg = new SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
					}
					@Override public SimplifiedColumnConfig getColumnConfig() {
						return cfg;
					}
				}

				private Vector<ObjectsTableRow> rows;

				ObjectsTableModel() {
					super(ColumnID.values());
					rows = null;
				}
				
				public void setDefaultCellEditorsAndRenderers() {
					ObjectsTableCellRenderer tcr = new ObjectsTableCellRenderer();
					table.setDefaultRenderer(Integer.class, tcr);
					table.setDefaultRenderer(Double.class, tcr);
				}

				void setData(HashMap<String,ObjectsTableRow> data) {
					rows = new Vector<>(data.values());
					rows.sort(Comparator.<ObjectsTableRow,Double>comparing(row->Math.abs(row.sum),Comparator.reverseOrder()).thenComparing(row->row.name));
					fireTableUpdate();
				}

				@Override public int getRowCount() {
					return rows==null ? 0 : rows.size();
				}

				@Override
				public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
					if (rows==null) return null;
					if (rowIndex<0) return null;
					if (rowIndex>=rows.size()) return null;
					ObjectsTableRow row = rows.get(rowIndex);
					
					switch (columnID) {
					case Count : return row.count;
					case Name  : return row.name;
					case Energy: return row.sum;
					}
					return null;
				}
				
			}
		}
		
		private static class BudgetPanel extends JPanel {
			private static final long serialVersionUID = 8583714497440761807L;
			
			private final JTextField fieldConsumption;
			private final JTextField fieldProduction;
			private final JTextField fieldBudget;
			private final ObjectsPanel sourcesPanel;
			private final ObjectsPanel consumersPanel;

			BudgetPanel(String title, ObjectsPanel sourcesPanel, ObjectsPanel consumersPanel) {
				super(new GridBagLayout());
				this.sourcesPanel = sourcesPanel;
				this.consumersPanel = consumersPanel;
				setBorder(BorderFactory.createTitledBorder(title));
				
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				
				c.weighty = 0;
				c.gridwidth  = 1;
				c.gridheight = 1;
				c.gridy = 0;
				c.gridx = -1;
				
				c.weightx = 0; c.gridx++; add(new JLabel("Production: "),c);
				c.weightx = 1; c.gridx++; add(fieldProduction = createOutputTextField("---"),c);
				
				c.weightx = 0; c.gridx++; add(new JLabel("  Consumption: "),c);
				c.weightx = 1; c.gridx++; add(fieldConsumption = createOutputTextField("---"),c);
				
				c.weightx = 0; c.gridx++; add(new JLabel("  Budget: "),c);
				c.weightx = 1; c.gridx++; add(fieldBudget = createOutputTextField("---"),c);
			}

			void updateValues() {
				double sumSources   = sourcesPanel  .getSum();
				double sumConsumers = consumersPanel.getSum();
				fieldProduction .setText(String.format(Locale.ENGLISH, "%1.2f kW",  sumSources  ));
				fieldConsumption.setText(String.format(Locale.ENGLISH, "%1.2f kW", -sumConsumers));
				fieldBudget     .setText(String.format(Locale.ENGLISH, "%1.2f kW", sumSources+sumConsumers));
			}
		}
	}

	private static class TerraformingStatesPanel extends JPanel {
		private static final long serialVersionUID = 6272012218012618784L;

		TerraformingStatesPanel(Data.TerraformingStates data) {
			super(new GridBagLayout());
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weighty = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridy = -1;
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Oxygen: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(formatOxygenLevel(data.oxygenLevel)), c);
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Heat: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(formatHeatLevel(data.heatLevel)), c);
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Pressure: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(formatPressureLevel(data.pressureLevel)), c);
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Biomass: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(formatBiomassLevel(data.biomassLevel)), c);
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Terraformation: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(formatTerraformation(data.oxygenLevel + data.heatLevel + data.pressureLevel + data.biomassLevel)), c);
			
			c.gridy++;
			c.weighty = 1;
			c.weightx = 1;
			c.gridwidth = 2;
			c.gridx = 0;
			add(new JLabel(), c);
		}

		private String formatTerraformation(double value) {
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

		private String formatValue(String format, double value) {
			return String.format(Locale.ENGLISH, format, value);
		}

		private String formatOxygenLevel(double value) {
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

		private String formatHeatLevel(double value) {
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

		private String formatPressureLevel(double value) {
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

		private String formatBiomassLevel(double value) {
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

	private static class PlayerStatesPanel extends JPanel {
		private static final long serialVersionUID = 6272012218012618784L;

		PlayerStatesPanel(Data.PlayerStates data) {
			super(new GridBagLayout());
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weighty = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridy = -1;
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Health: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format(Locale.ENGLISH, "%1.2f %%", data.health)), c);
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Thirst: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format(Locale.ENGLISH, "%1.2f %%", data.thirst)), c);
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Oxygen: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.oxygen)), c);
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Position: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.position)), c);
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Rotation: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.rotation)), c);
			
			Vector<String> unlockedGroups = new Vector<>(Arrays.asList(data.unlockedGroups));
			unlockedGroups.sort(Data.caseIgnoringComparator);
			JTextArea textArea = new JTextArea(String.join(", ", unlockedGroups));
			textArea.setEditable(false);
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);
			JScrollPane textAreaScrollPane = new JScrollPane(textArea);
			textAreaScrollPane.setPreferredSize(new Dimension(100,100));
			//textAreaScrollPane.setBorder(BorderFactory.createTitledBorder("Unlocked Groups"));
			textAreaScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Unlocked Groups"), textAreaScrollPane.getBorder()));
			
			c.gridy++;
			c.gridx = 0; 
			c.gridwidth = 2;
			c.weightx = 1;
			c.weighty = 1;
			add(textAreaScrollPane, c);
			
//			c.gridy = 6;
//			c.weighty = 1;
//			c.weightx = 1;
//			c.gridwidth = 2;
//			c.gridx = 0;
//			add(new JLabel(), c);
		}
	}

	private static class GeneralData1Panel extends JPanel {
		private static final long serialVersionUID = 6272012218012618784L;

		GeneralData1Panel(Data.GeneralData1 data) {
			super(new GridBagLayout());
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weighty = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridy = -1;
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Crafted Objects: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.craftedObjects)), c);
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Total SaveFile Load: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.totalSaveFileLoad)), c);
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Total SaveFile Time: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.totalSaveFileTime)), c);
			
			c.gridy++;
			c.weighty = 1;
			c.weightx = 1;
			c.gridwidth = 2;
			c.gridx = 0;
			add(new JLabel(), c);
		}
	}

	private static class GeneralData2Panel extends JPanel {
		private static final long serialVersionUID = 6272012218012618784L;

		GeneralData2Panel(Data.GeneralData2 data) {
			super(new GridBagLayout());
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weighty = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridy = -1;
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Has Played Intro: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.hasPlayedIntro)), c);
			
			c.gridy++;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Mode: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.mode)), c);
			
			c.gridy++;
			c.weighty = 1;
			c.weightx = 1;
			c.gridwidth = 2;
			c.gridx = 0;
			add(new JLabel(), c);
		}
	}
	
	private static class SimpleTablePanel<ValueType> extends JScrollPane {
		private static final long serialVersionUID = -8500969138264337829L;

		SimpleTablePanel(String title, Vector<ValueType> data, Column... columns) {
			SimpleTableModel<ValueType> tableModel = new SimpleTableModel<ValueType>(data,columns);
			JTable table = new JTable(tableModel);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			tableModel.setTable(table);
			tableModel.setColumnWidths(table);
			
			new TableContextMenu(table,tableModel);
			
			setViewportView(table);
			//setBorder(BorderFactory.createTitledBorder(title));
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), getBorder()));

			
			//Dimension size = table.getPreferredScrollableViewportSize();
			Dimension size = table.getPreferredSize();
			size.width  += 30;
			size.height += 50;
			setPreferredSize(size);
			
			//setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			//setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		}
		
		private class TableContextMenu extends ContextMenu {
			private static final long serialVersionUID = 1755523803906870773L;

			TableContextMenu(JTable table, SimpleTableModel<ValueType> tableModel) {
				add(PlanetCrafterSaveGameViewer.createMenuItem("Show Column Widths", e->{
					System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(table));
				}));
				
				addTo(table);
			}
		}
		
		private static class SimpleTableModel<ValueType> extends Tables.SimplifiedTableModel<Column> {

			private final Vector<ValueType> data;

			protected SimpleTableModel(Vector<ValueType> data, Column[] columns) {
				super(columns);
				this.data = data;
			}

			@Override public int getRowCount() { return data.size(); }

			@Override public Object getValueAt(int rowIndex, int columnIndex, Column columnID) {
				if (rowIndex<0) return null;
				if (rowIndex>=data.size()) return null;
				ValueType row = data.get(rowIndex);
				return columnID.getValue.apply(row);
			}
			
		}
		
		static class Column implements Tables.SimplifiedColumnIDInterface {
			
			private final SimplifiedColumnConfig config;
			private final Function<Object, Object> getValue;

			Column(String name, Class<?> columnClass, int width, Function<Object,Object> getValue) {
				this.getValue = getValue;
				config = new SimplifiedColumnConfig(name, columnClass, 20, -1, width, width);
			}

			@Override public SimplifiedColumnConfig getColumnConfig() {
				return config;
			}
		}
	}
}