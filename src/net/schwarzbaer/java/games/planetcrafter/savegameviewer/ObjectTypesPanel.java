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
import javax.swing.JOptionPane;
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
import net.schwarzbaer.system.ClipboardTools;

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
	
	static class ObjectTypesChangeEvent {
		
		enum EventType { ValueChanged, NewTypeAdded }
		
		final EventType eventType;
		final String objectTypeID;
		final ObjectTypeValue changedValue; // is null, if eventType == NewTypeAdded
		
		private ObjectTypesChangeEvent(EventType eventType, String objectTypeID, ObjectTypeValue changedValue) {
			this.eventType = eventType;
			this.objectTypeID = objectTypeID;
			this.changedValue = changedValue;
		}
		
		static ObjectTypesChangeEvent createValueChangedEvent(String objectTypeID, ObjectTypeValue changedValue) {
			return new ObjectTypesChangeEvent(EventType.ValueChanged, objectTypeID, changedValue);
		}
		
		static ObjectTypesChangeEvent createNewTypeAddedEvent(String objectTypeID) {
			return new ObjectTypesChangeEvent(EventType.NewTypeAdded, objectTypeID, null);
		}
	}
	
	interface ObjectTypesChangeListener {
		void objectTypesChanged(ObjectTypesChangeEvent event);
	}
	
	void    addObjectTypesChangeListener(ObjectTypesChangeListener dcl) { tableModel.objectTypesChangeListeners.   add(dcl); }
	void removeObjectTypesChangeListener(ObjectTypesChangeListener dcl) { tableModel.objectTypesChangeListeners.remove(dcl); }
	
	private class TableContextMenu extends ContextMenu {
		private static final long serialVersionUID = -2507135643891209882L;
		private ObjectType clickedRow;
		@SuppressWarnings("unused")
		private int clickedRowIndex;

		TableContextMenu() {
			clickedRow = null;
			clickedRowIndex = -1;
			
			JMenuItem miCopyID2Label = add(PlanetCrafterSaveGameViewer.createMenuItem("Copy ID to Clipboard", e->{
				if (clickedRow==null) return;
				ClipboardTools.copyStringSelectionToClipBoard(clickedRow.id);
			}));
			
			add(PlanetCrafterSaveGameViewer.createMenuItem("Create New Object Type", e->{
				boolean alreadyExists = true;
				String objectTypeID = null;;
				while (alreadyExists) {
					objectTypeID = JOptionPane.showInputDialog(ObjectTypesPanel.this, "Enter new Object Type ID", "Create New Object Type", JOptionPane.PLAIN_MESSAGE);
					if (objectTypeID==null) break;
					alreadyExists = tableModel.existsObjectTypeID(objectTypeID);
				}
				if (objectTypeID!=null) {
					tableModel.addNewObjectType(objectTypeID);
				}
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
						? "Copy ID to Clipboard"
						: String.format("Copy ID \"%s\" to Clipboard", clickedRow.id));
			});
		}
	}
	
	enum ObjectTypeValue {
		Label, Heat, Pressure, Oxygen, Biomass, Energy, OxygenBooster, BoosterRocket, Finished
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
			case heat    : return PhysicalValue.Heat    .formatRate((Double) value);
			case pressure: return PhysicalValue.Pressure.formatRate((Double) value);
			case oxygen  : return PhysicalValue.Oxygen  .formatRate((Double) value);
			case biomass : return PhysicalValue.Biomass .formatRate((Double) value);
			case energy  : return String.format(Locale.ENGLISH, "%1.2f kW"   , value);
			case oxygenBooster: return String.format(Locale.ENGLISH, "x %1.2f", value);
			}
			return null;
		}
	}
	
	private static class ObjectTypesTableModel extends Tables.SimplifiedTableModel<ObjectTypesTableModel.ColumnID>{
		
		enum ColumnID implements Tables.SimplifiedColumnIDInterface {
			finished          ("finished"      , Boolean      .class,  50, ObjectTypeValue.Finished),
			id                ("ID"            , String       .class, 130, null),
			label             ("Label"         , String       .class, 260, ObjectTypeValue.Label   ),
			heat              ("Heat"          , Double       .class,  80, ObjectTypeValue.Heat    ),
			pressure          ("Pressure"      , Double       .class,  80, ObjectTypeValue.Pressure),
			oxygen            ("Oxygen"        , Double       .class,  80, ObjectTypeValue.Oxygen  ),
			biomass           ("Biomass"       , Double       .class,  80, ObjectTypeValue.Biomass ),
			energy            ("Energy"        , Double       .class,  80, ObjectTypeValue.Energy  ),
			oxygenBooster     ("Oxy. Boost"    , Double       .class,  90, ObjectTypeValue.OxygenBooster),
			isBoosterRocketFor("Booster Rocket", PhysicalValue.class,  90, ObjectTypeValue.BoosterRocket),
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
		
		private final Vector<ObjectTypesChangeListener> objectTypesChangeListeners;
		private final Vector<String> objTypeIDs;
		private final HashMap<String, ObjectType> objectTypes;

		private ObjectTypesTableModel(ObjectTypesPanel panel, HashMap<String, ObjectType> objectTypes) {
			super(ColumnID.values());
			this.objectTypes = objectTypes;
			objTypeIDs = new Vector<>(objectTypes.keySet());
			objTypeIDs.sort(Data.caseIgnoringComparator);
			objectTypesChangeListeners = new Vector<>();
		}
		
		public void addNewObjectType(String objectTypeID) {
			for (int i=0; i<objTypeIDs.size(); i++) {
				String otID = objTypeIDs.get(i);
				int cmpVal = otID.compareToIgnoreCase(objectTypeID);
				if (cmpVal==0)
					System.err.printf("Can't create a new Object Type with ID \"%s\". This ID is already used.", objectTypeID);
				if (cmpVal>0) {
					objTypeIDs.insertElementAt(objectTypeID, i);
					objectTypes.put(objectTypeID, new ObjectType(objectTypeID));
					fireTableRowAdded(i);
					fireObjectTypeAddedEvent(objectTypeID);
					break;
				}
			}
		}

		public boolean existsObjectTypeID(String objectTypeID) {
			return objectTypes.containsKey(objectTypeID);
		}

		void fireObjectTypeAddedEvent(String objectTypeID) {
			ObjectTypesChangeEvent event = ObjectTypesChangeEvent.createNewTypeAddedEvent(objectTypeID);
			for (ObjectTypesChangeListener dcl : objectTypesChangeListeners)
				dcl.objectTypesChanged(event);
		}
		void fireValueChangedEvent(String objectTypeID, ObjectTypeValue value) {
			ObjectTypesChangeEvent event = ObjectTypesChangeEvent.createValueChangedEvent(objectTypeID, value);
			for (ObjectTypesChangeListener dcl : objectTypesChangeListeners)
				dcl.objectTypesChanged(event);
		}
		
		@SuppressWarnings("unused")
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
			case oxygen  : return row.oxygen;
			case biomass : return row.biomass;
			case energy  : return row.energy;
			case oxygenBooster     : return row.oxygenBooster;
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
			case oxygen  : row.oxygen   = (Double)aValue; break;
			case biomass : row.biomass  = (Double)aValue; break;
			case energy  : row.energy   = (Double)aValue; break;
			case oxygenBooster     : row.oxygenBooster = (Double)aValue; break;
			case isBoosterRocketFor: row.isBoosterRocketFor = (PhysicalValue)aValue; break;
			}
			fireValueChangedEvent(row.id, columnID.objectTypeValue);
		}
		
		
	}
}
