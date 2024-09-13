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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;
import java.util.function.BiConsumer;
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

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Coord3;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.GeneralDataPanel.TerraformingStatesPanel;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.PhysicalValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeEvent;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeListener;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.TerraformingCalculation.TerraformingAspect;
import net.schwarzbaer.java.lib.gui.Tables;

class TerraformingPanel extends JPanel implements ObjectTypesChangeListener {
	private static final long serialVersionUID = 5787736919473135578L;
	
	private final SubPanel heatPanel;
	private final SubPanel pressurePanel;
	private final SubPanel oxygenePanel;
	private final SubPanel plantsPanel;
	private final SubPanel insectsPanel;
	private final SubPanel animalsPanel;
	private final EnumMap<PhysicalValue, SubPanel> subPanels;

	TerraformingPanel(Data data, GeneralDataPanel generalDataPanel) {
		super(new GridLayout(0,2));
		
		TerraformingStatesPanel terraformingStatesPanel = generalDataPanel.getTerraformingStatesPanel();
		
		subPanels     = new EnumMap<>(PhysicalValue.class);
		heatPanel     = addPanel(this, data, terraformingStatesPanel, PhysicalValue.Heat    );
		pressurePanel = addPanel(this, data, terraformingStatesPanel, PhysicalValue.Pressure);
		oxygenePanel  = addPanel(this, data, terraformingStatesPanel, PhysicalValue.Oxygen  );
		plantsPanel   = addPanel(this, data, terraformingStatesPanel, PhysicalValue.Plants  );
		insectsPanel  = addPanel(this, data, terraformingStatesPanel, PhysicalValue.Insects );
		animalsPanel  = addPanel(this, data, terraformingStatesPanel, PhysicalValue.Animals );
	}
	
	private static SubPanel addPanel(TerraformingPanel main, Data data, TerraformingStatesPanel terraformingStatesPanel, PhysicalValue physicalValue)
	{
		TerraformingAspect aspect = TerraformingCalculation.getInstance().getAspect(physicalValue);
		SubPanel subPanel = new SubPanel(data, terraformingStatesPanel, physicalValue, aspect);
		main.add(subPanel);
		main.subPanels.put(physicalValue, subPanel);
		return subPanel;
	}

	@Override
	public void objectTypesChanged(ObjectTypesChangeEvent event) {
		if (event.eventType!=ObjectTypesChangeEvent.EventType.ValueChanged)
			return;
		
		if (event.changedValue==null)
			return;
		
		switch (event.changedValue) {
		case Label_en: case Label_de:
		case Energy:
			updateAll();
			break;
			
		case Heat    :                         heatPanel    .updateContent(); break;
		case Pressure:                         pressurePanel.updateContent(); break;
		case Oxygen  : case OxygenMultiplier : oxygenePanel .updateContent(); break;
		case Plants  :                         plantsPanel  .updateContent(); break;
		case Insects : case InsectsMultiplier: insectsPanel .updateContent(); break;
		case Animals : case AnimalsMultiplier: animalsPanel .updateContent(); break;
			
		case ExpectsMultiplierFor:
			subPanels.forEach((phVal,panel)->{
				if (phVal.isMultiplierBased)
					panel.updateContent();
			});
			break;
			
		case BoosterRocket: case BoosterMultiplier:
			updateAll();
			break;
		
		case IsMachineOptomizer: case MORange: case IsMOFuse: case MOFuseMultiplier:
			updateAll();
			break;
			
		case Finished: case IsProducer: case Class_: break;
		}
	}

	private void updateAll()
	{
		subPanels.forEach((phVal,panel)->panel.updateContent());
	}
	
	private static class SubPanel extends JPanel {
		private static final long serialVersionUID = -6895163439241941603L;
		
		private final ObjectsTableModel tableModel;
		private final JTextField fieldProductionRate;
		private final JTextField fieldBoosterRockets;
		private final JTextField fieldProductionRateFinal;

		private final Data data;
		private final PhysicalValue physicalValue;
		private final TerraformingAspect terraformingAspect;
		
		private final TerraformingStatesPanel terraformingStatesPanel;

		SubPanel(Data data, TerraformingStatesPanel terraformingStatesPanel, PhysicalValue physicalValue, TerraformingAspect terraformingAspect) {
			super(new BorderLayout(3,3));
			this.data = data;
			this.terraformingStatesPanel = terraformingStatesPanel;
			this.physicalValue = physicalValue;
			this.terraformingAspect = terraformingAspect;
			
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
		
		private static record ActiveMachineOptimizer (
			WorldObject wo,
			Coord3 position,
			double range,
			double fuseMulti
		) {}

		void updateContent()
		{
			HashMap<RowIndex,ObjectsTableRow> tableContent = new HashMap<>();
			double totalSum = 0.0;
			int numberOfBoosterRockets = 0;
			double boosterMultiplier = 0;
			terraformingAspect.clearData();
			
			Vector<ActiveMachineOptimizer> activeMOs = determineActiveMOs();
			
			for (WorldObject wo : data.worldObjects) {
				if (wo == null) continue;
				if (wo.objectType == null) continue;
				
				Double value = getValue(wo);
				if (value!=null && wo.isInstalled()) {
					Double multiplier = null;
					if (wo.objectType.expectsMultiplierFor==physicalValue && physicalValue.isMultiplierBased)
					{
						multiplier = getMultiplier( wo, physicalValue.getMultiplierFcn );
						if (multiplier==null) continue;
					}
					
					Double moMulti = getTotalMultiOfMOsInRange(activeMOs, wo.position, (amo,dist) -> {
						terraformingAspect.getOrCreateWOData(wo).addMachineOptimizer(amo.wo,dist);
					});
					
					RowIndex rowIndex = new RowIndex(
							wo.objectTypeID,
							multiplier==null ? 0 : multiplier.doubleValue(),
							moMulti   ==null ? 0 : moMulti   .doubleValue()
					);
					ObjectsTableRow row = tableContent.get(rowIndex);
					if (row==null) tableContent.put(rowIndex, row = new ObjectsTableRow(wo.getName(), multiplier, moMulti));
					
					row.add(wo,value);
					totalSum += computeMultipliedValue(value, multiplier, moMulti);
				}
				
				if (wo.objectType.isBoosterRocketFor==physicalValue) {
					if (wo.objectType.boosterMultiplier!=null)
						boosterMultiplier += wo.objectType.boosterMultiplier;
					numberOfBoosterRockets++;
				}
			}
			
			if (boosterMultiplier==0) boosterMultiplier = 1;
			double totalSumFinal = totalSum*boosterMultiplier;
			
			String strBoosterRockets = numberOfBoosterRockets == 0 ? "---" : String.format(Locale.ENGLISH, "%d Ro. (= %1.1f %%)", numberOfBoosterRockets, boosterMultiplier*100);
			fieldProductionRate     .setText(physicalValue.formatRate(totalSum));
			fieldBoosterRockets     .setText(strBoosterRockets);
			fieldProductionRateFinal.setText(physicalValue.formatRate(totalSumFinal));
			tableModel.setData(tableContent.values());
			
			terraformingStatesPanel.setRateOfPhysicalValue(physicalValue, totalSumFinal);
		}

		private static double computeMultipliedValue(double value, Double multiplier, Double moMulti)
		{
			return value
					* (multiplier==null ? 1 : multiplier.doubleValue())
					* (moMulti   ==null ? 1 : moMulti   .doubleValue());
		}

		private Vector<ActiveMachineOptimizer> determineActiveMOs()
		{
			Vector<ActiveMachineOptimizer> activeMOs = new Vector<>();
			for (WorldObject wo : data.worldObjects) {
				if (wo                == null) continue;
				if (wo.position       == null) continue;
				if (wo.list           == null) continue;
				if (wo.list.worldObjs == null) continue;
				if (wo.objectType     == null) continue;
				ObjectType ot = wo.objectType;
				
				if (!ot.isMachineOptomizer) continue;
				if ( ot.moRange    == null) continue;
				
				
				double moMulti = Double.NaN;
				for (WorldObject fuse : wo.list.worldObjs) {
					if (fuse            == null) continue;
					if (fuse.objectType == null) continue;
					ObjectType ot_f = fuse.objectType;
					
					if (ot_f.isMOFuse != physicalValue) continue;
					if (ot_f.moFuseMultiplier  == null) continue;
					
					if (Double.isNaN(moMulti)) moMulti = 0;
					moMulti += ot_f.moFuseMultiplier; // values of multiple fuses in one optimizer will be summarized
				}
				
				if (!Double.isNaN(moMulti))
					activeMOs.add(new ActiveMachineOptimizer(wo, wo.position, ot.moRange, moMulti));
			}
			return activeMOs;
		}
		
		private Double getTotalMultiOfMOsInRange(Vector<ActiveMachineOptimizer> activeMOs, Coord3 woPos, BiConsumer<ActiveMachineOptimizer,Double> foundAMO)
		{
			if (woPos==null) return null;
			
			Double moMulti = null;
			for (ActiveMachineOptimizer activeMO : activeMOs)
			{
				double distance = activeMO.position.getDistanceXYZ_m(woPos);
				if (distance <= activeMO.range) {
					foundAMO.accept(activeMO, distance);
					if (moMulti==null) moMulti = 0.0;
					moMulti += activeMO.fuseMulti; // values of multiple MOs will be summarized
				}
			}
			
			return moMulti;
		}

		private Double getMultiplier(WorldObject wo, Function<ObjectType, Double> getMultiplier) {
			if (wo.list==null) return null;
			if (wo.list.worldObjs.length==0) return null;
			
			ObjectType[] objectTypes = WorldObject.getObjectTypes(wo.list.worldObjs);
			return ObjectTypes.sumUpMultipliers(objectTypes, getMultiplier);
		}

		private static class RowIndex {
			
			final String objectTypeID;
			final double multiplier;
			final double moMulti;
			
			RowIndex(String objectTypeID, double multiplier, double moMulti) {
				if (objectTypeID==null) throw new IllegalArgumentException();
				this.objectTypeID = objectTypeID;
				this.multiplier = multiplier;
				this.moMulti = moMulti;
			}

			@Override
			public int hashCode() {
				return Objects.hash(moMulti, multiplier, objectTypeID);
			}

			@Override
			public boolean equals(Object obj)
			{
				if (this == obj               ) return true;
				if (!(obj instanceof RowIndex)) return false;
				RowIndex other = (RowIndex) obj;
				return Double.doubleToLongBits(moMulti) == Double.doubleToLongBits(other.moMulti)
						&& Double.doubleToLongBits(multiplier) == Double.doubleToLongBits(other.multiplier)
						&& Objects.equals(objectTypeID, other.objectTypeID);
			}
			
		}
		
		private static class ObjectsTableRow extends GUI.ObjectsTableRow {
			
			final Double multiplier;
			final Double moMulti;
			double baseSum;
			double energySum;
			
			ObjectsTableRow(String name, Double multiplier, Double moMulti) {
				super(name);
				this.multiplier = multiplier;
				this.moMulti = moMulti;
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
						case Multiplier: case MOMulti:
							valueStr = String.format(Locale.ENGLISH, "x %1.2f", value); break;
						case Energy:
							valueStr = String.format(Locale.ENGLISH, "%1.2f %s", value, ObjectTypes.EnergyRateUnit); break;
						case Efficiency:
							valueStr = String.format(Locale.ENGLISH, "%1.2f (%s)/%s", value, tableModel.physicalValue.rateUnit, ObjectTypes.EnergyRateUnit); break;
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
				MOMulti   ("Mo-Multi"  , Double .class,  55),
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
				this.physicalValue = Objects.requireNonNull( physicalValue );
			}
			
			private static ColumnID[] getColumns(PhysicalValue physicalValue) {
				if (physicalValue.isMultiplierBased)
					return ColumnID.values();
				
				return new ColumnID[] {
						ColumnID.Count     ,
						ColumnID.Name      ,
						ColumnID.BaseSum   ,
					//	ColumnID.Multiplier,
						ColumnID.MOMulti   ,
						ColumnID.FinalSum  ,
						ColumnID.Energy    ,
						ColumnID.Efficiency,
				};
			}
			
			void setDefaultCellEditorsAndRenderers() {
				ObjectsTableCellRenderer tcr = new ObjectsTableCellRenderer(this);
				setDefaultRenderers(class_ -> tcr);
			}

			@Override protected void setData(Collection<ObjectsTableRow> data) {
				super.setData(data);
				rows.sort(
						Comparator
						.<ObjectsTableRow,String>comparing(row->row.name)
						.thenComparing(row->row.multiplier,Comparator.nullsFirst(Comparator.naturalOrder()))
						.thenComparing(row->row.moMulti   ,Comparator.nullsFirst(Comparator.naturalOrder()))
				);
				fireTableUpdate();
			}

			@Override
			public String getColumnName(int columnIndex) {
				ColumnID columnID = getColumnID(columnIndex);
				if (!physicalValue.isMultiplierBased && (columnID==ColumnID.BaseSum || columnID==ColumnID.FinalSum)) return "Sum";
				return super.getColumnName(columnIndex);
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				ObjectsTableRow row = getRow(rowIndex);
				if (row==null) return null;
				
				switch (columnID) {
				case Count     : return row.getCount();
				case Name      : return row.name;
				case BaseSum   : return row.baseSum;
				case Multiplier: return row.multiplier;
				case MOMulti   : return row.moMulti;
				case FinalSum  : return computeMultipliedValue(row.baseSum, row.multiplier, row.moMulti);
				case Energy    : return row.energySum;
				case Efficiency: return computeMultipliedValue(row.baseSum, row.multiplier, row.moMulti) / Math.abs(row.energySum);
				}
				return null;
			}
		}
	}

}
