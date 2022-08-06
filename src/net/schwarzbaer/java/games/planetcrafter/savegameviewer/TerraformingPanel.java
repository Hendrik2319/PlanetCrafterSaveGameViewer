package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.Supplier;

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
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.GeneralDataPanel.TerraformingStatesPanel;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectType.PhysicalValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeEvent;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeListener;

class TerraformingPanel extends JPanel implements ObjectTypesChangeListener {
	private static final long serialVersionUID = 5787736919473135578L;
	
	private final SubPanel heatPanel;
	private final SubPanel pressurePanel;
	private final SubPanel oxygenePanel;
	private final SubPanel plantsPanel;
	private final SubPanel insectsPanel;
	private final SubPanel animalsPanel;

	TerraformingPanel(Data data, GeneralDataPanel generalDataPanel) {
		super(new GridLayout(0,2));
		
		Vector<TerraformingStatesPanel> terraformingStatesPanels = generalDataPanel.getTerraformingStatesPanels();
		
		heatPanel     = new SubPanel(data, terraformingStatesPanels, PhysicalValue.Heat    );
		pressurePanel = new SubPanel(data, terraformingStatesPanels, PhysicalValue.Pressure);
		oxygenePanel  = new SubPanel(data, terraformingStatesPanels, PhysicalValue.Oxygen  );
		plantsPanel   = new SubPanel(data, terraformingStatesPanels, PhysicalValue.Plants  );
		insectsPanel  = new SubPanel(data, terraformingStatesPanels, PhysicalValue.Insects );
		animalsPanel  = new SubPanel(data, terraformingStatesPanels, PhysicalValue.Animals );
		
		add(heatPanel);
		add(pressurePanel);
		add(oxygenePanel);
		add(plantsPanel );
		add(insectsPanel);
		add(animalsPanel);
	}

	@Override
	public void objectTypesChanged(ObjectTypesChangeEvent event) {
		if (event.eventType!=ObjectTypesChangeEvent.EventType.ValueChanged)
			return;
		
		if (event.changedValue==null)
			return;
		
		switch (event.changedValue) {
		case Label: case Energy:
			heatPanel    .updateContent();
			pressurePanel.updateContent();
			oxygenePanel .updateContent();
			plantsPanel  .updateContent();
			insectsPanel .updateContent();
			animalsPanel .updateContent();
			break;
			
		case Heat    : heatPanel    .updateContent(); break;
		case Pressure: pressurePanel.updateContent(); break;
		case Plants  : plantsPanel  .updateContent(); break;
		case Animals : animalsPanel .updateContent(); break;
			
		case Oxygen:
		case OxygenMultiplier:
			oxygenePanel.updateContent();
			break;
			
		case Insects:
		case InsectsMultiplier:
			insectsPanel.updateContent();
			break;
			
		case MultiplierExpected:
			oxygenePanel.updateContent();
			insectsPanel.updateContent();
			break;
			
			
		case BoosterRocket: case Finished: case IsProducer: break;
		}
	}
	
	private static class SubPanel extends JPanel {
		private static final long serialVersionUID = -6895163439241941603L;
		
		private final ObjectsTableModel tableModel;
		private final JTextField fieldProductionRate;
		private final JTextField fieldBoosterRockets;
		private final JTextField fieldProductionRateFinal;

		private final Data data;
		private final PhysicalValue physicalValue;
		
		private final Vector<TerraformingStatesPanel> terraformingStatesPanels;

		SubPanel(Data data, Vector<TerraformingStatesPanel> terraformingStatesPanels, PhysicalValue physicalValue) {
			super(new BorderLayout(3,3));
			this.data = data;
			this.terraformingStatesPanels = terraformingStatesPanels;
			this.physicalValue = physicalValue;
			
			JPanel resumePanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weightx = 0; resumePanel.add(new JLabel("Production Rate: "),c);
			c.weightx = 1; resumePanel.add(fieldProductionRate = GUI.createOutputTextField("---"),c);
			
			c.weightx = 0; resumePanel.add(new JLabel("  Booster Rockets: "),c);
			c.weightx = 1; resumePanel.add(fieldBoosterRockets = GUI.createOutputTextField("---"),c);
			
			c.weightx = 0; resumePanel.add(new JLabel("  Final Production Rate: "),c);
			c.weightx = 1; resumePanel.add(fieldProductionRateFinal = GUI.createOutputTextField("---"),c);
			
			tableModel = new ObjectsTableModel(this.physicalValue);
			JTable table = new JTable(tableModel);
			JScrollPane tableScrollPane = new JScrollPane(table);
			table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			
			tableModel.setTable(table);
			tableModel.setColumnWidths(table);
			tableModel.setDefaultCellEditorsAndRenderers();
			
			new GUI.ObjectsTableContextMenu(table, tableModel);
			
			Dimension size = table.getPreferredSize();
			size.width  += 30;
			size.height = 150;
			tableScrollPane.setPreferredSize(size);
			
			add(resumePanel, BorderLayout.NORTH);
			add(tableScrollPane, BorderLayout.CENTER);
			setBorder(BorderFactory.createTitledBorder(this.physicalValue.toString()));
			
			Data.addRemoveStateListener(tableModel::updateRemoveStates);
			
			SwingUtilities.invokeLater(this::updateContent);
		}

		private Double getValue(WorldObject wo) {
			if (wo == null) return null;
			if (wo.objectType == null) return null;
			
			switch (physicalValue) {
			case Oxygen  : return wo.objectType.oxygen;
			case Heat    : return wo.objectType.heat;
			case Pressure: return wo.objectType.pressure;
			case Plants  : return wo.objectType.plants;
			case Insects : return wo.objectType.insects;
			case Animals : return wo.objectType.animals;
			}
			return null;
		}

		void updateContent() {
			
			HashMap<RowIndex,ObjectsTableRow> tableContent = new HashMap<>();
			double totalSum = 0.0;
			int numberOfBoosterRockets = 0;
			
			for (WorldObject wo : data.worldObjects) {
				if (wo == null) continue;
				if (wo.objectType == null) continue;
				
				Double value = getValue(wo);
				if (value!=null && wo.isInstalled()) {
					Double multiplier = null;
					if (wo.objectType.multiplierExpected) {
						if (physicalValue==PhysicalValue.Oxygen) {
							multiplier = getMultiplier(wo,ot->ot.oxygenMultiplier);
							if (multiplier==null) continue;
						} else if (physicalValue==PhysicalValue.Insects) {
							multiplier = getMultiplier(wo,ot->ot.insectsMultiplier);
							if (multiplier==null) continue;
						}
					}
					
					RowIndex rowIndex = new RowIndex(wo.objectTypeID, multiplier==null ? 0 : multiplier.doubleValue());
					ObjectsTableRow row = tableContent.get(rowIndex);
					if (row==null) tableContent.put(rowIndex, row = new ObjectsTableRow(wo.getName(), multiplier));
					
					row.add(wo,value);
					totalSum += value * (multiplier==null ? 1 : multiplier.doubleValue());
				}
				
				if (wo.objectType.isBoosterRocketFor==physicalValue) {
					numberOfBoosterRockets++;
				}
			}
			double boosterMultiplier = 1;
			if (0 < numberOfBoosterRockets)
				boosterMultiplier = 10*numberOfBoosterRockets;
			
			double totalSumFinal = totalSum*boosterMultiplier;
			
			fieldProductionRate     .setText(physicalValue.formatRate(totalSum));
			fieldBoosterRockets     .setText(Integer.toString(numberOfBoosterRockets));
			fieldProductionRateFinal.setText(physicalValue.formatRate(totalSumFinal));
			tableModel.setData(tableContent.values());
			
			for (TerraformingStatesPanel panel : terraformingStatesPanels)
				panel.setRateOfPhysicalValue(physicalValue, totalSumFinal);
		}
		
		private Double getMultiplier(WorldObject wo, Function<ObjectType, Double> getMultiplier) {
			if (wo.list==null) return null;
			if (wo.list.worldObjs.length==0) return null;
			
			ObjectType[] objectTypes = Data.WorldObject.getObjectTypes(wo.list.worldObjs);
			return ObjectType.sumUpMultipliers(objectTypes, getMultiplier);
		}

		private static class RowIndex {
			
			final String objectTypeID;
			final double multiplier;
			
			RowIndex(String objectTypeID, double multiplier) {
				if (objectTypeID==null) throw new IllegalArgumentException();
				this.objectTypeID = objectTypeID;
				this.multiplier = multiplier;
			}
			
			@Override public int hashCode() {
				return Double.hashCode(multiplier) ^ objectTypeID.hashCode();
			}
			
			@Override public boolean equals(Object obj) {
				if (obj instanceof RowIndex) {
					RowIndex other = (RowIndex) obj;
					return (other.multiplier==this.multiplier) && other.objectTypeID.equals(this.objectTypeID);
				}
				return false;
			}
		}
		
		private static class ObjectsTableRow extends GUI.ObjectsTableRow {
			
			final Double multiplier;
			double baseSum;
			double energySum;
			
			ObjectsTableRow(String name, Double multiplier) {
				super(name);
				this.multiplier = multiplier;
				baseSum = 0;
				energySum = 0;
			}

			void add(WorldObject wo, double value) {
				add(wo);
				baseSum += value;
				if (wo.objectType.energy!=null)
					energySum += wo.objectType.energy;
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
				int    rowM =    rowV<0 ? -1 : table.   convertRowIndexToModel(   rowV);
				int columnM = columnV<0 ? -1 : table.convertColumnIndexToModel(columnV);
				ObjectsTableRow  row = rowM<0 ? null : tableModel.getRow(rowM);
				ObjectsTableModel.ColumnID columnID = columnM<0 ? null : tableModel.getColumnID(columnM);
				
				String valueStr;
				if (value==null)
					valueStr = null;
				else {
					valueStr = value.toString();
					if (columnID!=null)
						switch (columnID) {
						case Name : break;
						case Count:
							valueStr = String.format(Locale.ENGLISH, "%d x ", value); break;
						case Multiplier:
							valueStr = String.format(Locale.ENGLISH, "x %1.2f", value); break;
						case Energy:
							valueStr = String.format(Locale.ENGLISH, "%1.2f %s", value, ObjectType.EnergyRateUnit); break;
						case Efficiency:
							valueStr = String.format(Locale.ENGLISH, "%1.2f (%s)/%s", value, tableModel.physicalValue.rateUnit, ObjectType.EnergyRateUnit); break;
						case BaseSum: case FinalSum:
							valueStr = tableModel.physicalValue.formatRate((Double) value); break;
						}
				}
				
				Supplier<Color> getCustomBackground = ObjectsTableRow.createCustomBackgroundFunction(row);
				rendererComponent.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus, getCustomBackground, null);
				if (value instanceof Number)
					rendererComponent.setHorizontalAlignment(SwingConstants.RIGHT);
				else
					rendererComponent.setHorizontalAlignment(SwingConstants.LEFT);
				
				return rendererComponent;
			}
			
		}
		
		private static class ObjectsTableModel extends GUI.ObjectsTableModel<ObjectsTableRow, ObjectsTableModel.ColumnID> {
			
			enum ColumnID implements Tables.SimplifiedColumnIDInterface {
				Count     ("Count"     , Integer.class,  50),
				Name      ("Name"      , String .class, 130),
				BaseSum   ("Base Sum"  , Double .class, 100),
				Multiplier("Multi"     , Double .class,  50),
				FinalSum  ("Final Sum" , Double .class, 100),
				Energy    ("Energy"    , Double .class,  80),
				Efficiency("Efficiency", Double .class, 110),
				;
				private final Tables.SimplifiedColumnConfig cfg;
				ColumnID(String name, Class<?> colClass, int width) {
					cfg = new Tables.SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
				}
				@Override public Tables.SimplifiedColumnConfig getColumnConfig() {
					return cfg;
				}
			}

			private final PhysicalValue physicalValue;

			ObjectsTableModel(PhysicalValue physicalValue) {
				super( getColumns(physicalValue) );
				this.physicalValue = physicalValue;
			}
			
			private static ColumnID[] getColumns(PhysicalValue physicalValue) {
				switch (physicalValue) {
				case Oxygen:
				case Insects:
					return ColumnID.values();
				default:
					return new ColumnID[] {ColumnID.Count, ColumnID.Name, ColumnID.FinalSum, ColumnID.Energy, ColumnID.Efficiency};
				}
			}
			
			void setDefaultCellEditorsAndRenderers() {
				ObjectsTableCellRenderer tcr = new ObjectsTableCellRenderer(this);
				table.setDefaultRenderer(Integer.class, tcr);
				table.setDefaultRenderer(Double.class, tcr);
				table.setDefaultRenderer(String.class, tcr);
			}

			@Override protected void setData(Collection<ObjectsTableRow> data) {
				super.setData(data);
				rows.sort(
						Comparator
						.<ObjectsTableRow,String>comparing(row->row.name)
						.thenComparing(row->row.multiplier,Comparator.nullsFirst(Comparator.naturalOrder()))
				);
				fireTableUpdate();
			}

			@Override
			public String getColumnName(int columnIndex) {
				ColumnID columnID = getColumnID(columnIndex);
				if (physicalValue!=PhysicalValue.Oxygen &&
					physicalValue!=PhysicalValue.Insects &&
						(columnID==ColumnID.BaseSum || columnID==ColumnID.FinalSum)) return "Sum";
				return super.getColumnName(columnIndex);
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				ObjectsTableRow row = getRow(rowIndex);
				if (row==null) return null;
				double multiplier = row.multiplier==null ? 1 : row.multiplier.doubleValue();
				
				switch (columnID) {
				case Count     : return row.getCount();
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
