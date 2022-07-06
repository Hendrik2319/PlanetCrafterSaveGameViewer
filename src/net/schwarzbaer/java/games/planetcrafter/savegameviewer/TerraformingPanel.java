package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.TerraformingStates;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectType.PhysicalValue;

class TerraformingPanel extends JPanel implements ObjectTypesPanel.DataChangeListener {
	private static final long serialVersionUID = 5787736919473135578L;
	private SubPanel heatPanel;
	private SubPanel pressurePanel;
	private SubPanel oxygenePanel;
	private SubPanel biomassPanel;

	TerraformingPanel(Data data) {
		super(new GridLayout(0,2));
		
		heatPanel     = new SubPanel(data, PhysicalValue.Heat    );
		pressurePanel = new SubPanel(data, PhysicalValue.Pressure);
		oxygenePanel  = new SubPanel(data, PhysicalValue.Oxygen );
		biomassPanel  = new SubPanel(data, PhysicalValue.Biomass );
		
		add(heatPanel);
		add(pressurePanel);
		add(oxygenePanel);
		add(biomassPanel);
	}

	@Override
	public void objectTypeValueChanged(String objectTypeID, ObjectTypesPanel.ObjectTypeValue changedValue) {
		if (changedValue==null)
			return;
		
		switch (changedValue) {
		case BoosterRocket: case Finished: case Energy:
			break;
			
		case Label:
			heatPanel    .updateContent();
			pressurePanel.updateContent();
			oxygenePanel .updateContent();
			biomassPanel .updateContent();
			break;
			
		case Heat:
			heatPanel.updateContent();
			break;
			
		case Pressure:
			pressurePanel.updateContent();
			break;
			
		case Oxygen:
		case OxygenBooster:
			oxygenePanel.updateContent();
			break;
			
		case Biomass:
			biomassPanel.updateContent();
			break;
		}
	}
	
	private static class SubPanel extends JPanel {
		private static final long serialVersionUID = -6895163439241941603L;
		
		private final ObjectsTableModel tableModel;
		private final JTextField fieldProductionRate;
		private final JTextField fieldProductionRateFinal;
		private final JTextField fieldCurrentLevel;
		
		private final Data data;
		private final PhysicalValue physicalValue;
		private double totalSumFinal;

		SubPanel(Data data, PhysicalValue physicalValue) {
			super(new BorderLayout());
			this.data = data;
			this.physicalValue = physicalValue;
			totalSumFinal = 0;
			
			JPanel resumePanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weightx = 0; resumePanel.add(new JLabel("Production Rate: "),c);
			c.weightx = 1; resumePanel.add(fieldProductionRate = PlanetCrafterSaveGameViewer.createOutputTextField("---"),c);
			
			c.weightx = 0; resumePanel.add(new JLabel("  Boostered by Rockets: "),c);
			c.weightx = 1; resumePanel.add(fieldProductionRateFinal = PlanetCrafterSaveGameViewer.createOutputTextField("---"),c);
			
			c.weightx = 0; resumePanel.add(new JLabel("  Current Level: "),c);
			c.weightx = 1; resumePanel.add(fieldCurrentLevel = PlanetCrafterSaveGameViewer.createOutputTextField("---"),c);
			
			tableModel = new ObjectsTableModel(this.physicalValue);
			JTable table = new JTable(tableModel);
			JScrollPane tableScrollPane = new JScrollPane(table);
			table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			
			tableModel.setTable(table);
			tableModel.setColumnWidths(table);
			tableModel.setDefaultCellEditorsAndRenderers();
			
			Dimension size = table.getPreferredSize();
			size.width  += 30;
			size.height = 150;
			tableScrollPane.setPreferredSize(size);
			
			add(resumePanel, BorderLayout.NORTH);
			add(tableScrollPane, BorderLayout.CENTER);
			setBorder(BorderFactory.createTitledBorder(this.physicalValue.toString()));
			
			SwingUtilities.invokeLater(this::updateContent);
		}

		private Double getValue(WorldObject wo) {
			if (wo == null) return null;
			if (wo.objectType == null) return null;
			
			switch (physicalValue) {
			case Heat    : return wo.objectType.heat;
			case Pressure: return wo.objectType.pressure;
			case Oxygen  : return wo.objectType.oxygen;
			case Biomass : return wo.objectType.biomass;
			}
			return null;
		}

		private String getCurrentLevelAsString(Vector<TerraformingStates> terraformingStates) {
			if (terraformingStates==null) return null;
			if (terraformingStates.isEmpty()) return null;
			
			// TODO Auto-generated method stub
			TerraformingStates states = terraformingStates.firstElement();
			switch (physicalValue) {
			case Heat    : return Data.TerraformingStates.formatHeatLevel    (states.heatLevel    );
			case Pressure: return Data.TerraformingStates.formatPressureLevel(states.pressureLevel);
			case Oxygen  : return Data.TerraformingStates.formatOxygenLevel  (states.oxygenLevel  );
			case Biomass : return Data.TerraformingStates.formatBiomassLevel (states.biomassLevel );
			}
			return null;
		}

		void updateContent() {
			
			HashMap<String,ObjectsTableRow> tableContent = new HashMap<>();
			double totalSum = 0.0;
			int amountOfBoosterRockets = 0;
			
			for (WorldObject wo : data.worldObjects) {
				if (wo == null) continue;
				if (wo.objectType == null) continue;
				
				Double value = getValue(wo);
				if (value != null) {
					ObjectsTableRow row = tableContent.get(wo.objectTypeID);
					if (row==null) tableContent.put(wo.objectTypeID, row = new ObjectsTableRow(wo.getName()));
					row.count++;
					row.baseSum += value;
					if (wo.objectType.energy!=null)
						row.energySum += wo.objectType.energy;
					totalSum += value;
				}
				
				if (wo.objectType.isBoosterRocketFor==physicalValue) {
					amountOfBoosterRockets++;
				}
			}
			totalSumFinal = totalSum;
			if (0 < amountOfBoosterRockets)
				totalSumFinal = totalSum*10*amountOfBoosterRockets;
			
			fieldProductionRate     .setText(physicalValue.formatRate(totalSum));
			fieldProductionRateFinal.setText(physicalValue.formatRate(totalSumFinal));
			fieldCurrentLevel.setText(getCurrentLevelAsString(data.terraformingStates));
			tableModel.setData(tableContent);
		}
		
		private static class ObjectsTableRow {
			
			final String name;
			final Double multiplier;
			int count;
			double baseSum;
			double energySum;
			
			ObjectsTableRow(String name) {
				this(name, null);
			}
			ObjectsTableRow(String name, Double multiplier) {
				this.name = name;
				this.multiplier = multiplier;
				count = 0;
				baseSum = 0;
				energySum = 0;
			}
		}
		
		private static class ObjectsTableCellRenderer implements TableCellRenderer {
			
			private final Tables.LabelRendererComponent rendererComponent;
			private final ObjectsTableModel tableModel;

			ObjectsTableCellRenderer(ObjectsTableModel tableModel) {
				this.tableModel = tableModel;
				rendererComponent = new Tables.LabelRendererComponent();
			}

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
				int columnM = table.convertColumnIndexToModel(columnV);
				ObjectsTableModel.ColumnID columnID = tableModel.getColumnID(columnM);
				
				String valueStr;
				if (value==null)
					valueStr = null;
				else {
					valueStr = value.toString();
					switch (columnID) {
					case Name : break;
					case Count:
						valueStr = String.format(Locale.ENGLISH, "%d x ", value); break;
					case Multiplier:
						valueStr = String.format(Locale.ENGLISH, "x %1.2f", value); break;
					case Energy:
						valueStr = String.format(Locale.ENGLISH, "%1.2f kW", value); break;
					case Efficiency:
						valueStr = String.format(Locale.ENGLISH, "%1.2f (%s)/kW", value, tableModel.physicalValue.rateUnit); break;
					case BaseSum: case FinalSum:
						valueStr = tableModel.physicalValue.formatRate((Double) value); break;
					}
				}
				
				rendererComponent.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus);
				rendererComponent.setHorizontalAlignment(SwingConstants.RIGHT);
				return rendererComponent;
			}
			
		}
		
		private static class ObjectsTableModel extends Tables.SimplifiedTableModel<ObjectsTableModel.ColumnID>{
			
			enum ColumnID implements Tables.SimplifiedColumnIDInterface {
				Count     ("Count"     , Integer.class,  50),
				Name      ("Name"      , String .class, 130),
				BaseSum   ("Base Sum"  , Double .class,  80),
				Multiplier("Multi"     , Double .class,  50),
				FinalSum  ("Final Sum" , Double .class,  80),
				Energy    ("Energy"    , Double .class,  80),
				Efficiency("Efficiency", Double .class, 100),
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
			private final PhysicalValue physicalValue;

			ObjectsTableModel(PhysicalValue physicalValue) {
				super(
					physicalValue==PhysicalValue.Oxygen ?
						ColumnID.values()
						: new ColumnID[] {ColumnID.Count, ColumnID.Name, ColumnID.FinalSum, ColumnID.Energy, ColumnID.Efficiency}
				);
				this.physicalValue = physicalValue;
				rows = null;
			}
			
			public void setDefaultCellEditorsAndRenderers() {
				ObjectsTableCellRenderer tcr = new ObjectsTableCellRenderer(this);
				table.setDefaultRenderer(Integer.class, tcr);
				table.setDefaultRenderer(Double.class, tcr);
			}

			void setData(HashMap<String,ObjectsTableRow> data) {
				rows = new Vector<>(data.values());
				rows.sort(Comparator.<ObjectsTableRow,String>comparing(row->row.name));
				fireTableUpdate();
			}

			@Override public int getRowCount() {
				return rows==null ? 0 : rows.size();
			}

			@Override
			public String getColumnName(int columnIndex) {
				ColumnID columnID = getColumnID(columnIndex);
				if (physicalValue!=PhysicalValue.Oxygen && (columnID==ColumnID.BaseSum || columnID==ColumnID.FinalSum)) return "Sum";
				return super.getColumnName(columnIndex);
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				if (rows==null) return null;
				if (rowIndex<0) return null;
				if (rowIndex>=rows.size()) return null;
				ObjectsTableRow row = rows.get(rowIndex);
				double multiplier = row.multiplier==null ? 1 : row.multiplier.doubleValue();
				
				switch (columnID) {
				case Count     : return row.count;
				case Name      : return row.name;
				case BaseSum   : return row.baseSum;
				case Multiplier: return row.multiplier;
				case FinalSum  : return row.baseSum*multiplier;
				case Energy    : return row.energySum;
				case Efficiency: return row.baseSum*multiplier/Math.abs(row.energySum);
				}
				return null;
			}
			
		}
	}

}
