package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Supplier;

import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectType.PhysicalValue;

class ObjectTypesPanel extends JScrollPane {
	private static final long serialVersionUID = 7789343749897706554L;
	private ObjectTypesTableModel tableModel;
	private JTable table;
	
	ObjectTypesPanel(HashMap<String,ObjectType> objectTypes) {
		
		tableModel = new ObjectTypesTableModel(this, objectTypes);
		table = new JTable(tableModel);
		table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		tableModel.setTable(table);
		tableModel.setColumnWidths(table);
		tableModel.setDefaultCellEditorsAndRenderers();
		
		new TableContextMenu().addTo(table);
		
		setViewportView(table);
	}
	
	interface DataChangeListener {
		void objectTypeValueChanged(String objectTypeID, ObjectTypeValue changedValue);
	}
	
	void    addDataChangeListener(DataChangeListener dcl) { tableModel.dataChangeListeners.   add(dcl); }
	void removeDataChangeListener(DataChangeListener dcl) { tableModel.dataChangeListeners.remove(dcl); }
	
	private class TableContextMenu extends ContextMenu {
		private static final long serialVersionUID = -2507135643891209882L;
		private ObjectType clickedRow;
		private int clickedRowIndex;

		TableContextMenu() {
			clickedRow = null;
			clickedRowIndex = -1;
			
			JMenuItem miCopyID2Label = add(PlanetCrafterSaveGameViewer.createMenuItem("Copy ID value to Label", e->{
				if (clickedRow==null) return;
				clickedRow.label = clickedRow.id;
				tableModel.fireTableCellUpdate(clickedRowIndex, ObjectTypesTableModel.ColumnID.label);
				tableModel.notifyDataChangeListeners(clickedRow.id, ObjectTypeValue.Label);
			}));
			
			addSeparator();
			
			add(PlanetCrafterSaveGameViewer.createMenuItem("Show Column Widths", e->{
				System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(table));
			}));
			
			
			addContextMenuInvokeListener((comp, x, y) -> {
				int rowV = table.rowAtPoint(new Point(x,y));
				int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
				clickedRowIndex = rowM;
				clickedRow = rowM<0 ? null : tableModel.getRow(rowM);
				
				miCopyID2Label.setEnabled(clickedRow!=null);
				miCopyID2Label.setText(clickedRow == null
						? "Copy ID value to Label"
						: String.format("Copy ID value \"%s\" to Label", clickedRow.id));
			});
		}
	}
	
	enum ObjectTypeValue {
		Label, Heat, Pressure, Oxygene, Biomass, Energy, OxygeneBooster, BoosterRocket
	}
	
	private static class ObjectTypesTableCellRenderer implements TableCellRenderer {
		
		private Tables.LabelRendererComponent rendererComponent;
		private ObjectTypesTableModel tableModel;

		ObjectTypesTableCellRenderer(ObjectTypesTableModel tableModel) {
			this.tableModel = tableModel;
			rendererComponent = new Tables.LabelRendererComponent();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
			
			int rowM    = table.convertRowIndexToModel   (rowV   );
			int columnM = table.convertColumnIndexToModel(columnV);
			ObjectTypesTableModel.ColumnID columnID = tableModel.getColumnID(columnM);
			ObjectType row = tableModel.getRow(rowM);
			
			String valueStr = getValueStr(value, columnID);
			Supplier<Color> getCustomBG = ()->{
				if (row==null) return null;
				if (row.finished && columnID!=ObjectTypesTableModel.ColumnID.finished) return Color.LIGHT_GRAY;
				return null;
			};
			rendererComponent.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus, getCustomBG, null);
			
			int hAlign = getHorizontalAlignment(columnID);
			rendererComponent.setHorizontalAlignment(hAlign);
			
			return rendererComponent;
		}

		private int getHorizontalAlignment(ObjectTypesTableModel.ColumnID columnID) {
			if (columnID==null)
				return SwingConstants.LEFT;
			
			if (columnID.cfg.columnClass == Double.class)
				return SwingConstants.RIGHT;
			
			return SwingConstants.LEFT;
		}

		private String getValueStr(Object value, ObjectTypesTableModel.ColumnID columnID) {
			if (value==null) return null;
			if (columnID==null) return value.toString();
			
			switch (columnID) {
			case finished: return null;
			case id: case label: case isBoosterRocketFor:
				 return value.toString();
			case heat    : return String.format(Locale.ENGLISH, "%1.2f pK/s" , value);
			case pressure: return String.format(Locale.ENGLISH, "%1.2f nPa/s", value);
			case oxygene : return String.format(Locale.ENGLISH, "%1.2f ppq/s", value);
			case biomass : return String.format(Locale.ENGLISH, "%1.2f g/s"  , value);
			case energy  : return String.format(Locale.ENGLISH, "%1.2f kW"   , value);
			case oxygeneBooster: return String.format(Locale.ENGLISH, "x %1.2f", value);
			}
			return null;
		}
	}
	
	private static class ObjectTypesTableModel extends Tables.SimplifiedTableModel<ObjectTypesTableModel.ColumnID>{
		
		enum ColumnID implements Tables.SimplifiedColumnIDInterface {
			finished          ("finished"      , Boolean      .class,  50, null),
			id                ("ID"            , String       .class, 130, null),
			label             ("Label"         , String       .class, 260, ObjectTypeValue.Label   ),
			heat              ("Heat"          , Double       .class,  80, ObjectTypeValue.Heat    ),
			pressure          ("Pressure"      , Double       .class,  80, ObjectTypeValue.Pressure),
			oxygene           ("Oxygene"       , Double       .class,  80, ObjectTypeValue.Oxygene ),
			biomass           ("Biomass"       , Double       .class,  80, ObjectTypeValue.Biomass ),
			energy            ("Energy"        , Double       .class,  80, ObjectTypeValue.Energy  ),
			oxygeneBooster    ("Oxy. Boost"    , Double       .class,  90, ObjectTypeValue.OxygeneBooster),
			isBoosterRocketFor("Booster Rocket", PhysicalValue.class,  90, ObjectTypeValue.BoosterRocket ),
			;
			private final SimplifiedColumnConfig cfg;
			private final ObjectTypeValue objectTypeValue;
			ColumnID(String name, Class<?> colClass, int width, ObjectTypeValue objectTypeValue) {
				this.objectTypeValue = objectTypeValue;
				cfg = new SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() {
				return cfg;
			}
		}
		
		private final Vector<DataChangeListener> dataChangeListeners;
		private final Vector<String> objTypeIDs;
		private final HashMap<String, ObjectType> objectTypes;

		private ObjectTypesTableModel(ObjectTypesPanel panel, HashMap<String, ObjectType> objectTypes) {
			super(ColumnID.values());
			this.objectTypes = objectTypes;
			objTypeIDs = new Vector<>(objectTypes.keySet());
			objTypeIDs.sort(Data.caseIgnoringComparator);
			dataChangeListeners = new Vector<>();
		}
		
		void notifyDataChangeListeners(String objectTypeID, ObjectTypeValue value) {
			for (DataChangeListener dcl : dataChangeListeners)
				dcl.objectTypeValueChanged(objectTypeID, value);
		}
		
		void fireTableCellUpdate(int rowIndex, ColumnID columnID) {
			int columnIndex = getColumn(columnID);
			super.fireTableCellUpdate(rowIndex, columnIndex);
		}

		void setDefaultCellEditorsAndRenderers() {
			
			ObjectTypesTableCellRenderer tcr = new ObjectTypesTableCellRenderer(this);
			table.setDefaultRenderer(String.class, tcr);
			table.setDefaultRenderer(Double.class, tcr);
			table.setDefaultRenderer(PhysicalValue.class, tcr);
			
			Vector<PhysicalValue> values = new Vector<>(Arrays.asList(PhysicalValue.values()));
			values.insertElementAt(null, 0);
			table.setDefaultEditor(PhysicalValue.class, new Tables.ComboboxCellEditor<PhysicalValue>(values));
		}

		@Override public int getRowCount() { return objTypeIDs.size(); }
		
		private ObjectType getRow(int rowIndex) {
			if (rowIndex<0) return null;
			if (rowIndex>=objTypeIDs.size()) return null;
			return objectTypes.get(objTypeIDs.get(rowIndex));
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
			ObjectType row = getRow(rowIndex);
			switch (columnID) {
			case finished: return row.finished;
			case id      : return row.id;
			case label   : return row.label;
			case heat    : return row.heat;
			case pressure: return row.pressure;
			case oxygene : return row.oxygene;
			case biomass : return row.biomass;
			case energy  : return row.energy;
			case oxygeneBooster    : return row.oxygeneBooster;
			case isBoosterRocketFor: return row.isBoosterRocketFor;
			}
			return null;
		}

		@Override protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID) {
			ObjectType row = getRow(rowIndex);
			if (row==null) return false;
			if (row.finished) return columnID==ColumnID.finished;
			return columnID!=ColumnID.id;
		}

		@Override
		protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID) {
			System.out.printf("setValueAt( %s%s )%n", aValue, aValue==null ? "" : String.format(" [%s]", aValue.getClass()));
			ObjectType row = getRow(rowIndex);
			switch (columnID) {
			case finished: row.finished = (Boolean)aValue;
			case id      : break;
			case label   : row.label    = (String)aValue; if (row.label!=null && row.label.isBlank()) row.label = null; break;
			case heat    : row.heat     = (Double)aValue; break;
			case pressure: row.pressure = (Double)aValue; break;
			case oxygene : row.oxygene  = (Double)aValue; break;
			case biomass : row.biomass  = (Double)aValue; break;
			case energy  : row.energy   = (Double)aValue; break;
			case oxygeneBooster    : row.oxygeneBooster = (Double)aValue; break;
			case isBoosterRocketFor: row.isBoosterRocketFor = (PhysicalValue)aValue; break;
			}
			if (columnID.objectTypeValue!=null)
				notifyDataChangeListeners(row.id, columnID.objectTypeValue);
		}
		
		
	}
}
