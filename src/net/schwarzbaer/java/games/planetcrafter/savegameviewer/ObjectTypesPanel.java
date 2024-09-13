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

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.MapShapes.MapShape;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectTypeClass;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectTypeValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.Occurrence;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.PhysicalValue;
import net.schwarzbaer.java.lib.gui.ContextMenu;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.lib.system.ClipboardTools;

class ObjectTypesPanel extends JScrollPane {
	private static final long serialVersionUID = 7789343749897706554L;
	private final ObjectTypesTableModel tableModel;
	private final JTable table;
	private final PlanetCrafterSaveGameViewer main;
	
	ObjectTypesPanel(PlanetCrafterSaveGameViewer main, ObjectTypes objectTypes, HashMap<String,Integer> amounts)
	{
		this.main = main;
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
	
	void notifyMapShapesEvent(MapShapes.Editor.EditorEvent event)
	{
		if (event==null) throw new IllegalArgumentException();
		
		int rowIndex = tableModel.getRowIndex(event.objectType());
		switch (event.type())
		{
			case HasGotFirstShape:
				if (rowIndex >= 0)
					tableModel.fireTableCellUpdate(rowIndex, ObjectTypesTableModel.ColumnID.showMarker);
				break;
				
			case RemovedSelectedShape:
				if (rowIndex >= 0)
				{
					tableModel.fireTableCellUpdate(rowIndex, ObjectTypesTableModel.ColumnID.mapShape);
					tableModel.fireTableCellUpdate(rowIndex, ObjectTypesTableModel.ColumnID.showMarker);
				}
				break;
				
			case ChangedShapeName:
				MapShape selectedShape = main.mapShapes.getSelectedShape(event.objectType());
				if (selectedShape==event.shape() && selectedShape!=null && rowIndex >= 0)
					tableModel.fireTableCellUpdate(rowIndex, ObjectTypesTableModel.ColumnID.mapShape);
				break;
		}
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
			
			JMenuItem miCopyLabelEn2Clipboard = add(GUI.createMenuItem("Copy Label (En) to Clipboard", e->{
				if (clickedRow==null) return;
				ClipboardTools.copyStringSelectionToClipBoard(clickedRow.label_en);
			}));
			
			JMenuItem miCopyLabelDe2Clipboard = add(GUI.createMenuItem("Copy Label (De) to Clipboard", e->{
				if (clickedRow==null) return;
				ClipboardTools.copyStringSelectionToClipBoard(clickedRow.label_de);
			}));
			
			JMenuItem miEditMapShapes = add(GUI.createMenuItem("Create/Edit MapShapes", e->{
				if (clickedRow==null) return;
				main.showMapShapesEditor(clickedRow);
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
				
				miCopyID2Clipboard     .setEnabled(clickedRow!=null);
				miCopyLabelEn2Clipboard.setEnabled(clickedRow!=null);
				miCopyLabelDe2Clipboard.setEnabled(clickedRow!=null);
				miEditMapShapes        .setEnabled(clickedRow!=null);
				miCopyID2Clipboard.setText(clickedRow == null
						? "Copy ID to Clipboard"
						: String.format("Copy ID \"%s\" to Clipboard", clickedRow.id));
				miCopyLabelEn2Clipboard.setText(clickedRow == null
						? "Copy Label (En) to Clipboard"
						: String.format("Copy Label (En) \"%s\" to Clipboard", clickedRow.label_en));
				miCopyLabelDe2Clipboard.setText(clickedRow == null
						? "Copy Label (De) to Clipboard"
						: String.format("Copy Label (De) \"%s\" to Clipboard", clickedRow.label_de));
				miEditMapShapes.setText(clickedRow == null
						? "Create/Edit MapShapes"
						: main.mapShapes.hasShapes(clickedRow)
							? String.format(  "Edit MapShapes of \"%s\"", clickedRow.getName())
							: String.format("Create MapShapes of \"%s\"", clickedRow.getName()));
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
				if (row.finished && !tableModel.isCellEditable(rowM, columnM, columnID)) return Color.LIGHT_GRAY;
				return null;
			};
			
			if (value instanceof Boolean) {
				boolean isChecked = (Boolean) value;
				rcCheckBox.configureAsTableCellRendererComponent(table, isChecked, null, isSelected, hasFocus, null, getCustomBG);
				rcCheckBox.setEnabled( tableModel.isCellEditable(rowM, columnM, columnID) );
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
			
			if (columnID.horizontalAlignement != null)
				return columnID.horizontalAlignement;
			
			if (Number.class.isAssignableFrom( columnID.cfg.columnClass) )
				return SwingConstants.RIGHT;
			
			return SwingConstants.LEFT;
		}

		private String getValueStr(Object value, ObjectTypesTableModel.ColumnID columnID) {
			if (value==null) return null;
			if (columnID==null) return value.toString();
			
			switch (columnID) {
			case finished: case id: case label_en: case label_de: case class_: case isBoosterRocketFor: case isProducer: case isMachineOptomizer: case isMOFuse:
			case expectsMultiplierFor: case occurrences: case amount: case showMarker: case effectOnTerraforming:
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
			case mapShape: return ((MapShape)value).label;
			case boosterMultiplier: return value instanceof Double multi ? String.format(Locale.ENGLISH, "%1.1f %%", multi*100) : "";
			case moRange: return String.format(Locale.ENGLISH, "%1.1f m", value);
			case moFuseMultiplier: return String.format(Locale.ENGLISH, "x %1.2f", value);
			}
			return null;
		}
	}
	
	private static class ObjectTypesTableModel extends Tables.SimplifiedTableModel<ObjectTypesTableModel.ColumnID>{
		
		enum ColumnID implements Tables.SimplifiedColumnIDInterface, SwingConstants {
			finished             ("finished"              , Boolean        .class,  50,   null, ObjectTypeValue.Finished             ),
			occurrences          ("Occ."                  , String         .class,  50,   null, null                                 ),
			amount               ("N"                     , Integer        .class,  30, CENTER, null                                 ),
			id                   ("ID"                    , String         .class, 130,   null, null                                 ),
			label_en             ("Label (En)"            , String         .class, 260,   null, ObjectTypeValue.Label_en             ),
			label_de             ("Label (De)"            , String         .class, 260,   null, ObjectTypeValue.Label_de             ),
			class_               ("Class"                 , ObjectTypeClass.class, 130,   null, ObjectTypeValue.Class_               ),
			showMarker           ("Show Marker?"          , Boolean        .class,  90,   null, null                                 ),
			mapShape             ("MapShape"              , MapShape       .class,  90,   null, null                                 ),
			effectOnTerraforming ("Terraf."               , Boolean        .class,  50,   null, null                                 ),
			heat                 ("Heat"                  , Double         .class,  80,   null, ObjectTypeValue.Heat                 ),
			pressure             ("Pressure"              , Double         .class,  80,   null, ObjectTypeValue.Pressure             ),
			oxygen               ("Oxygen"                , Double         .class,  80,   null, ObjectTypeValue.Oxygen               ),
			plants               ("Plants"                , Double         .class,  80,   null, ObjectTypeValue.Plants               ),
			insects              ("Insects"               , Double         .class,  80,   null, ObjectTypeValue.Insects              ),
			animals              ("Animals"               , Double         .class,  80,   null, ObjectTypeValue.Animals              ),
			energy               ("Energy"                , Double         .class,  80,   null, ObjectTypeValue.Energy               ),
			expectsMultiplierFor ("Multi Expected"        , PhysicalValue  .class,  90, CENTER, ObjectTypeValue.ExpectsMultiplierFor ),
			oxygenMultiplier     ("Oxy. Multi"            , Double         .class,  90,   null, ObjectTypeValue.OxygenMultiplier     ),
			insectsMultiplier    ("Insects Multi"         , Double         .class,  90,   null, ObjectTypeValue.InsectsMultiplier    ),
			animalsMultiplier    ("Animals Multi"         , Double         .class,  90,   null, ObjectTypeValue.AnimalsMultiplier    ),
			isBoosterRocketFor   ("Booster Rocket"        , PhysicalValue  .class,  90, CENTER, ObjectTypeValue.BoosterRocket        ),
			boosterMultiplier    ("Booster Multi"         , Double         .class,  90,   null, ObjectTypeValue.BoosterMultiplier    ),
			isMachineOptomizer   ("Is Machine Optomizer?" , Boolean        .class,  90,   null, ObjectTypeValue.IsMachineOptomizer   ),
			moRange              ("MO Range"              , Double         .class,  90,   null, ObjectTypeValue.MORange              ), 
			isMOFuse             ("Is MO Fuse for"        , PhysicalValue  .class,  90,   null, ObjectTypeValue.IsMOFuse             ),
			moFuseMultiplier     ("MO Fuse Multi"         , Double         .class,  90,   null, ObjectTypeValue.MOFuseMultiplier     ), 
			isProducer           ("Is Producer?"          , Boolean        .class,  90,   null, ObjectTypeValue.IsProducer           ),
			;
			private final SimplifiedColumnConfig cfg;
			private final Integer horizontalAlignement;
			private final ObjectTypeValue objectTypeValue;
			ColumnID(String name, Class<?> colClass, int width, Integer horizontalAlignement, ObjectTypeValue objectTypeValue) {
				cfg = new SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
				this.horizontalAlignement = horizontalAlignement;
				this.objectTypeValue = objectTypeValue;
			}
			@Override public SimplifiedColumnConfig getColumnConfig() {
				return cfg;
			}
		}
		
		private final Vector<ObjectTypesChangeListener> objectTypesChangeListeners;
		private final Vector<String> objTypeIDs;
		private final ObjectTypes objectTypes;
		private final HashMap<String, Integer> amounts;
		private final ObjectTypesPanel panel;

		private ObjectTypesTableModel(ObjectTypesPanel panel, ObjectTypes objectTypes, HashMap<String,Integer> amounts) {
			super(ColumnID.values());
			this.panel = panel;
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
			fireObjectTypesChangeEvent(ObjectTypesChangeEvent.createNewTypeAddedEvent(objectTypeID));
		}
		void fireValueChangedEvent(String objectTypeID, ObjectTypeValue value) {
			fireObjectTypesChangeEvent(ObjectTypesChangeEvent.createValueChangedEvent(objectTypeID, value));
		}
		
		private void fireObjectTypesChangeEvent(ObjectTypesChangeEvent event)
		{
			for (ObjectTypesChangeListener dcl : objectTypesChangeListeners)
				dcl.objectTypesChanged(event);
		}

		@Override
		public void fireTableCellUpdate(int rowIndex, ColumnID columnID) {
			super.fireTableCellUpdate(rowIndex, columnID);
		}

		void setDefaultCellEditorsAndRenderers() {
			
			ObjectTypesTableCellRenderer tcr = new ObjectTypesTableCellRenderer(this);
			setDefaultRenderers( clazz -> tcr );
			
			Tables.ComboboxCellEditor<MapShape> mapShapesCellEditor = new Tables.ComboboxCellEditor<>((rowM, columnM) -> {
				ObjectType row = getRow(rowM);
				Vector<MapShape> shapes = panel.main.mapShapes.getShapes(row);
				if (shapes==null) return new Vector<>();
				Vector<MapShape> list = new Vector<>();
				list.add(null);
				list.addAll(shapes);
				return list;
			});
			mapShapesCellEditor.setRenderer(shape -> shape instanceof MapShape ? ((MapShape)shape).label : "-- none --" );
			
			table.setDefaultEditor(PhysicalValue  .class, new Tables.ComboboxCellEditor<>(getVector(PhysicalValue  .values(), true)));
			table.setDefaultEditor(ObjectTypeClass.class, new Tables.ComboboxCellEditor<>(getVector(ObjectTypeClass.values(), true)));
			table.setDefaultEditor(MapShape       .class, mapShapesCellEditor);
		}
		
		private <ValueType> Vector<ValueType> getVector(ValueType[] values, boolean addNull)
		{
			Vector<ValueType> vector = new Vector<>(Arrays.asList(values));
			if (addNull) vector.insertElementAt(null, 0);
			return vector;
		}

		@Override public int getRowCount() { return objTypeIDs.size(); }
		
		private ObjectType getRow(int rowIndex) {
			if (rowIndex<0) return null;
			if (rowIndex>=objTypeIDs.size()) return null;
			return objectTypes.get(objTypeIDs.get(rowIndex), null);
		}
		
		int getRowIndex(ObjectType objectType)
		{
			return objectType==null ? -1 : objTypeIDs.indexOf(objectType.id);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
			ObjectType row = getRow(rowIndex);
			switch (columnID) {
			case finished: return row.finished;
			case id      : return row.id;
			case label_en: return row.label_en;
			case label_de: return row.label_de;
			case class_  : return row.class_;
			case effectOnTerraforming: return row.hasEffectOnTerraforming();
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
			case boosterMultiplier   : return row.boosterMultiplier;
			case isProducer  : return row.isProducer;
			case occurrences : return toString(row.occurrences);
			case amount      : return amounts.get(row.id);
			case showMarker  : return !panel.main.mapShapes.hasShapes(row) ? null : panel.main.mapShapes.shouldShowMarker(row);
			case mapShape    : return !panel.main.mapShapes.hasShapes(row) ? null : panel.main.mapShapes.getSelectedShape(row);
			case isMachineOptomizer : return row.isMachineOptomizer;
			case moRange            : return row.moRange;
			case isMOFuse           : return row.isMOFuse;
			case moFuseMultiplier   : return row.moFuseMultiplier;
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
			if (columnID==ColumnID.showMarker || columnID==ColumnID.mapShape) return panel.main.mapShapes.hasShapes(row);
			if (row.finished) return columnID==ColumnID.finished;
			return columnID!=ColumnID.id && columnID!=ColumnID.occurrences && columnID!=ColumnID.effectOnTerraforming;
		}

		@Override
		protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID) {
			// System.out.printf("setValueAt( %s%s )%n", aValue, aValue==null ? "" : String.format(" [%s]", aValue.getClass()));
			ObjectType row = getRow(rowIndex);
			switch (columnID) {
			case finished: row.finished = (boolean)aValue; fireTableRowUpdate(rowIndex); break;
			case occurrences: break;
			case amount     : break;
			case id         : break;
			case label_en: row.label_en = (String)aValue; if (row.label_en!=null && row.label_en.isBlank()) row.label_en = null; break;
			case label_de: row.label_de = (String)aValue; if (row.label_de!=null && row.label_de.isBlank()) row.label_de = null; break;
			case class_  : row.class_   = (ObjectTypeClass)aValue; break;
			case effectOnTerraforming: break;
			case heat    : row.heat     = (Double)aValue; break;
			case pressure: row.pressure = (Double)aValue; break;
			case oxygen  : row.oxygen   = (Double)aValue; break;
			case plants  : row.plants   = (Double)aValue; break;
			case insects : row.insects  = (Double)aValue; break;
			case animals : row.animals  = (Double)aValue; break;
			case energy  : row.energy   = (Double)aValue; break;
			case expectsMultiplierFor: row.expectsMultiplierFor = (PhysicalValue)aValue; break;
			case oxygenMultiplier    : row.oxygenMultiplier     = (Double       )aValue; break;
			case insectsMultiplier   : row.insectsMultiplier    = (Double       )aValue; break;
			case animalsMultiplier   : row.animalsMultiplier    = (Double       )aValue; break;
			case isBoosterRocketFor  : row.isBoosterRocketFor   = (PhysicalValue)aValue;
				if (row.isBoosterRocketFor!=null && row.boosterMultiplier==null) {
					row.boosterMultiplier = ObjectTypes.DEFAULT_BOOSTER_MULTIPLIER;
					fireValueChangedEvent(row.id, ColumnID.boosterMultiplier.objectTypeValue);
					fireTableCellUpdate(rowIndex, ColumnID.boosterMultiplier);
				}
				break;
			case boosterMultiplier   : row.boosterMultiplier    = (Double )aValue; break;
			case isProducer          : row.isProducer           = (Boolean)aValue; break;
			case showMarker          : if (panel.main.mapShapes.hasShapes(row)) panel.main.mapShapes.setShowMarker(row, (boolean)aValue); break;
			case mapShape            : if (panel.main.mapShapes.hasShapes(row)) panel.main.mapShapes.setSelectedShape(row, (MapShape)aValue); fireTableCellUpdate(rowIndex, ColumnID.showMarker); break;
			case isMachineOptomizer : row.isMachineOptomizer = (boolean      )aValue; break;
			case moRange            : row.moRange            = (Double       )aValue; break;
			case isMOFuse           : row.isMOFuse           = (PhysicalValue)aValue; break;
			case moFuseMultiplier   : row.moFuseMultiplier   = (Double       )aValue; break;
			}
			fireValueChangedEvent(row.id, columnID.objectTypeValue);
		}
		
		
	}
}
