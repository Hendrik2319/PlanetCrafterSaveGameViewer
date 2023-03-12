package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.util.Arrays;
import java.util.EnumSet;
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
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectTypeValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.Occurrence;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.PhysicalValue;
import net.schwarzbaer.system.ClipboardTools;

class ObjectTypesPanel extends JScrollPane {
	private static final long serialVersionUID = 7789343749897706554L;
	private ObjectTypesTableModel tableModel;
	private JTable table;
	
	ObjectTypesPanel(ObjectTypes objectTypes, HashMap<String,Integer> amounts) {
		
		tableModel = new ObjectTypesTableModel(this, objectTypes, amounts);
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
			
			JMenuItem miCopyID2Clipboard = add(GUI.createMenuItem("Copy ID to Clipboard", e->{
				if (clickedRow==null) return;
				ClipboardTools.copyStringSelectionToClipBoard(clickedRow.id);
			}));
			
			JMenuItem miCopyLabel2Clipboard = add(GUI.createMenuItem("Copy Label to Clipboard", e->{
				if (clickedRow==null) return;
				ClipboardTools.copyStringSelectionToClipBoard(clickedRow.label);
			}));
			
			add(GUI.createMenuItem("Create New Object Type", e->{
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
			
			add(GUI.createMenuItem("Show Column Widths", e->{
				System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(table));
			}));
			
			
			addContextMenuInvokeListener((comp, x, y) -> {
				int rowV = table.rowAtPoint(new Point(x,y));
				int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
				clickedRowIndex = rowM;
				clickedRow = rowM<0 ? null : tableModel.getRow(rowM);
				
				miCopyID2Clipboard   .setEnabled(clickedRow!=null);
				miCopyLabel2Clipboard.setEnabled(clickedRow!=null);
				miCopyID2Clipboard.setText(clickedRow == null
						? "Copy ID to Clipboard"
						: String.format("Copy ID \"%s\" to Clipboard", clickedRow.id));
				miCopyLabel2Clipboard.setText(clickedRow == null
						? "Copy Label to Clipboard"
						: String.format("Copy Label \"%s\" to Clipboard", clickedRow.label));
			});
		}
	}
	
	private static class ObjectTypesTableCellRenderer implements TableCellRenderer {
		
		private final ObjectTypesTableModel tableModel;
		private final Tables.LabelRendererComponent rcLabel;
		private final Tables.CheckBoxRendererComponent rcCheckBox;

		ObjectTypesTableCellRenderer(ObjectTypesTableModel tableModel) {
			this.tableModel = tableModel;
			rcLabel = new Tables.LabelRendererComponent();
			rcCheckBox = new Tables.CheckBoxRendererComponent();
			rcCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
			
			int rowM    = table.convertRowIndexToModel   (rowV   );
			int columnM = table.convertColumnIndexToModel(columnV);
			ObjectTypesTableModel.ColumnID columnID = tableModel.getColumnID(columnM);
			ObjectType row = tableModel.getRow(rowM);
			
			Supplier<Color> getCustomBG = ()->{
				if (row==null) return null;
				if (row.finished && columnID!=ObjectTypesTableModel.ColumnID.finished) return Color.LIGHT_GRAY;
				return null;
			};
			
			if (value instanceof Boolean) {
				boolean isChecked = (Boolean) value;
				rcCheckBox.configureAsTableCellRendererComponent(table, isChecked, null, isSelected, hasFocus, null, getCustomBG);
				rcCheckBox.setEnabled(columnID==ObjectTypesTableModel.ColumnID.finished || (row!=null && !row.finished));
				return rcCheckBox;
				
			} else {
				rcLabel.configureAsTableCellRendererComponent(table, null, getValueStr(value, columnID), isSelected, hasFocus, getCustomBG, null);
				rcLabel.setHorizontalAlignment(getHorizontalAlignment(columnID));
				return rcLabel;
			}
			
		}

		private int getHorizontalAlignment(ObjectTypesTableModel.ColumnID columnID) {
			if (columnID==null)
				return SwingConstants.LEFT;
			
			if (columnID == ObjectTypesTableModel.ColumnID.amount)
				return SwingConstants.CENTER;
			
			if (Number.class.isAssignableFrom( columnID.cfg.columnClass) )
				return SwingConstants.RIGHT;
			
			return SwingConstants.LEFT;
		}

		private String getValueStr(Object value, ObjectTypesTableModel.ColumnID columnID) {
			if (value==null) return null;
			if (columnID==null) return value.toString();
			
			switch (columnID) {
			case finished: case id: case label: case isBoosterRocketFor: case isProducer: case expectsMultiplierFor: case occurrences: case amount:
				 return value.toString();
			case heat    : return PhysicalValue.Heat    .formatRate((Double) value);
			case pressure: return PhysicalValue.Pressure.formatRate((Double) value);
			case oxygen  : return PhysicalValue.Oxygen  .formatRate((Double) value);
			case plants  : return PhysicalValue.Plants  .formatRate((Double) value);
			case insects : return PhysicalValue.Insects .formatRate((Double) value);
			case animals : return PhysicalValue.Animals .formatRate((Double) value);
			case energy  : return ObjectTypes.formatEnergyRate((Double) value);
			case oxygenMultiplier : return String.format(Locale.ENGLISH, "x %1.2f", value);
			case insectsMultiplier: return String.format(Locale.ENGLISH, "x %1.2f", value);
			case animalsMultiplier: return String.format(Locale.ENGLISH, "x %1.2f", value);
			}
			return null;
		}
	}
	
	private static class ObjectTypesTableModel extends Tables.SimplifiedTableModel<ObjectTypesTableModel.ColumnID>{
		
		enum ColumnID implements Tables.SimplifiedColumnIDInterface {
			finished             ("finished"      , Boolean      .class,  50, ObjectTypeValue.Finished),
			occurrences          ("Occ."          , String       .class,  50, null),
			amount               ("N"             , Integer      .class,  30, null),
			id                   ("ID"            , String       .class, 130, null),
			label                ("Label"         , String       .class, 260, ObjectTypeValue.Label   ),
			heat                 ("Heat"          , Double       .class,  80, ObjectTypeValue.Heat    ),
			pressure             ("Pressure"      , Double       .class,  80, ObjectTypeValue.Pressure),
			oxygen               ("Oxygen"        , Double       .class,  80, ObjectTypeValue.Oxygen  ),
			plants               ("Plants"        , Double       .class,  80, ObjectTypeValue.Plants  ),
			insects              ("Insects"       , Double       .class,  80, ObjectTypeValue.Insects ),
			animals              ("Animals"       , Double       .class,  80, ObjectTypeValue.Animals ),
			energy               ("Energy"        , Double       .class,  80, ObjectTypeValue.Energy  ),
			expectsMultiplierFor ("Multi Expected", PhysicalValue.class,  90, ObjectTypeValue.ExpectsMultiplierFor),
			oxygenMultiplier     ("Oxy. Multi"    , Double       .class,  90, ObjectTypeValue.OxygenMultiplier),
			insectsMultiplier    ("Insects Multi" , Double       .class,  90, ObjectTypeValue.InsectsMultiplier),
			animalsMultiplier    ("Animals Multi" , Double       .class,  90, ObjectTypeValue.AnimalsMultiplier),
			isBoosterRocketFor   ("Booster Rocket", PhysicalValue.class,  90, ObjectTypeValue.BoosterRocket),
			isProducer           ("Is Producer?"  , Boolean      .class,  90, ObjectTypeValue.IsProducer),
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
		private final ObjectTypes objectTypes;
		private final HashMap<String, Integer> amounts;

		private ObjectTypesTableModel(ObjectTypesPanel panel, ObjectTypes objectTypes, HashMap<String,Integer> amounts) {
			super(ColumnID.values());
			this.objectTypes = objectTypes;
			this.amounts = amounts;
			objTypeIDs = new Vector<>(this.objectTypes.keySet());
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
					objectTypes.put(objectTypeID, new ObjectType(objectTypeID, ObjectTypes.Occurrence.User));
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
		
		@Override
		public void fireTableCellUpdate(int rowIndex, ColumnID columnID) {
			super.fireTableCellUpdate(rowIndex, columnID);
		}

		void setDefaultCellEditorsAndRenderers() {
			
			ObjectTypesTableCellRenderer tcr = new ObjectTypesTableCellRenderer(this);
			table.setDefaultRenderer(String.class, tcr);
			table.setDefaultRenderer(Double.class, tcr);
			table.setDefaultRenderer(Integer.class, tcr);
			table.setDefaultRenderer(Boolean.class, tcr);
			table.setDefaultRenderer(PhysicalValue.class, tcr);
			
			Vector<PhysicalValue> values = new Vector<>(Arrays.asList(PhysicalValue.values()));
			values.insertElementAt(null, 0);
			table.setDefaultEditor(PhysicalValue.class, new Tables.ComboboxCellEditor<PhysicalValue>(values));
		}

		@Override public int getRowCount() { return objTypeIDs.size(); }
		
		private ObjectType getRow(int rowIndex) {
			if (rowIndex<0) return null;
			if (rowIndex>=objTypeIDs.size()) return null;
			return objectTypes.get(objTypeIDs.get(rowIndex), null);
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
			case plants  : return row.plants;
			case insects : return row.insects;
			case animals : return row.animals;
			case energy  : return row.energy;
			case expectsMultiplierFor: return row.expectsMultiplierFor;   
			case oxygenMultiplier    : return row.oxygenMultiplier;
			case insectsMultiplier   : return row.insectsMultiplier;
			case animalsMultiplier   : return row.animalsMultiplier;
			case isBoosterRocketFor  : return row.isBoosterRocketFor;
			case isProducer : return row.isProducer;
			case occurrences: return toString(row.occurrences);
			case amount: return amounts.get(row.id);
			}
			return null;
		}

		private String toString(EnumSet<Occurrence> occurrences) {
			if (occurrences.isEmpty()) return " - ";
			Iterable<String> it = ()->occurrences.stream().sorted().map(Occurrence::getShortLabel).iterator();
			return String.join(", ", it);
		}

		@Override protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID) {
			ObjectType row = getRow(rowIndex);
			if (row==null) return false;
			if (row.finished) return columnID==ColumnID.finished;
			return columnID!=ColumnID.id && columnID!=ColumnID.occurrences;
		}

		@Override
		protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID) {
			// System.out.printf("setValueAt( %s%s )%n", aValue, aValue==null ? "" : String.format(" [%s]", aValue.getClass()));
			ObjectType row = getRow(rowIndex);
			switch (columnID) {
			case finished: row.finished = (Boolean)aValue; fireTableRowUpdate(rowIndex); break;
			case occurrences: break;
			case amount     : break;
			case id         : break;
			case label   : row.label    = (String)aValue; if (row.label!=null && row.label.isBlank()) row.label = null; break;
			case heat    : row.heat     = (Double)aValue; break;
			case pressure: row.pressure = (Double)aValue; break;
			case oxygen  : row.oxygen   = (Double)aValue; break;
			case plants  : row.plants   = (Double)aValue; break;
			case insects : row.insects  = (Double)aValue; break;
			case animals : row.animals  = (Double)aValue; break;
			case energy  : row.energy   = (Double)aValue; break;
			case expectsMultiplierFor: row.expectsMultiplierFor = (PhysicalValue)aValue; break;
			case oxygenMultiplier    : row.oxygenMultiplier     = (Double)aValue; break;
			case insectsMultiplier   : row.insectsMultiplier    = (Double)aValue; break;
			case animalsMultiplier   : row.animalsMultiplier    = (Double)aValue; break;
			case isBoosterRocketFor  : row.isBoosterRocketFor   = (PhysicalValue)aValue; break;
			case isProducer          : row.isProducer           = (Boolean)aValue; break;
			}
			fireValueChangedEvent(row.id, columnID.objectTypeValue);
		}
		
		
	}
}
