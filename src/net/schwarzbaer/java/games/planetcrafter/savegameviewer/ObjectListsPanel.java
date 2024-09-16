package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.Supplier;

import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.ObjectList;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.lib.gui.Tables;

class ObjectListsPanel extends AbstractTablePanel<ObjectList, ObjectListsPanel.ObjectListsTableModel.ColumnID> {
	private static final long serialVersionUID = -1787920497956857504L;

	ObjectListsPanel(Data data, MapPanel mapPanel) {
		super( new ObjectListsTableModel(data), true, (table,tableModel) -> new TableContextMenu(table, tableModel, mapPanel), LayoutPos.Right, new Dimension(300,100) );
	}
	
	private static class TableContextMenu extends AbstractTablePanel.TableContextMenu {
		private static final long serialVersionUID = -5452206425591893443L;
		
		private int clickedRowIndex;
		private ObjectList clickedRow;

		TableContextMenu(JTable table, ObjectListsTableModel tableModel, MapPanel mapPanel) {
			super(table);
			clickedRowIndex = -1;
			
			addSeparator();
			
			JMenuItem miShowContainerInMap = add(GUI.createMenuItem("Show Container in Map", e->{
				if (clickedRow==null) return;
				if (!WorldObject.isInstalled(clickedRow.container))return;
				mapPanel.showWorldObject(clickedRow.container);
			}));
			
			addContextMenuInvokeListener((comp, x, y) -> {
				int rowV = table.rowAtPoint(new Point(x,y));
				clickedRowIndex = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
				clickedRow = clickedRowIndex<0 ? null : tableModel.getRow(clickedRowIndex);
				
				miShowContainerInMap.setEnabled(
					clickedRow!=null && WorldObject.isInstalled(clickedRow.container));
				miShowContainerInMap.setText(
					clickedRow==null || !WorldObject.isInstalled(clickedRow.container)
					? "Show Container in Map"
					: String.format("Show Container \"%s\" in Map", clickedRow.container.getName())
				);
			});
		}
	}
	
	static class GeneralTCR implements TableCellRenderer {
		
		private final Tables.LabelRendererComponent standardComp;
		private final Tables.CheckBoxRendererComponent boolComp;
		private final ObjectListsTableModel tableModel;

		GeneralTCR(ObjectListsTableModel tableModel) {
			this.tableModel = tableModel;
			standardComp = new Tables.LabelRendererComponent();
			boolComp = new Tables.CheckBoxRendererComponent();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
			int rowM    = rowV   <0 ? -1 : table.convertRowIndexToModel   (rowV   );
			int columnM = columnV<0 ? -1 : table.convertColumnIndexToModel(columnV);
			ObjectList row = tableModel.getRow(rowM);
			ObjectListsTableModel.ColumnID columnID = tableModel.getColumnID(columnM);
			
			final Component selectedRendererComponent;
			
			Supplier<Color> getCustomBackground = ()->{
				if (row==null) return null;
				if (!row.isMarkedForRemoval()) return null;
				if (!row.canMarkedByUser())
					return GUI.COLOR_Removal_ByData;
				else
					return GUI.COLOR_Removal_ByUser;
			};
			
			if (value instanceof Boolean)
			{
				boolean isChecked = (Boolean) value;
				selectedRendererComponent = boolComp;
				boolComp.configureAsTableCellRendererComponent(table, isChecked, null, isSelected, hasFocus, null, getCustomBackground);
				boolComp.setHorizontalAlignment(SwingConstants.CENTER);
				
			}
			else
			{
				selectedRendererComponent = standardComp;
				String valueStr = value != null 
						? value.toString()
						: columnID!=null && columnID.isOptional
							? "<undefined>"
							: null;
				
				if (columnID==ObjectListsTableModel.ColumnID.Filling && value instanceof Double)
					valueStr = String.format(Locale.ENGLISH, "%1.1f%%", ((Double) value)*100);
				
				standardComp.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus, getCustomBackground, null);
				if (value instanceof Number)
					standardComp.setHorizontalAlignment(SwingConstants.RIGHT);
				else
					standardComp.setHorizontalAlignment(SwingConstants.LEFT);
			}
			
			return selectedRendererComponent;
		}
	}
	
	static class ObjectListsTableModel extends AbstractTablePanel.AbstractTableModel<ObjectList, ObjectListsTableModel.ColumnID> {

		enum ColumnID implements Tables.SimplifiedColumnIDInterface {
			id         ("ID"                , Long   .class,  75),
			nonUniqueID("UnI"               , Boolean.class,  30),
			IsTwinID   ("Twin"              , Boolean.class,  35),
			container  ("Assigned Container", String .class, 350),
			size       ("Size"              , Long   .class,  50),
			Filling    ("Filling"           , Double .class,  50),
			worldObjs  ("Content"           , String .class, 400),
			demandItems("Demand Items"      , String .class, 200, true),
			supplyItems("Supply Items"      , String .class, 200, true),
			dronePrio  ("Drone Prio"        , Long   .class,  50, true),
			;
			private final Tables.SimplifiedColumnConfig cfg;
			private final boolean isOptional;
			ColumnID(String name, Class<?> colClass, int width) {
				this(name, colClass, width, false);
			}
			ColumnID(String name, Class<?> colClass, int width, boolean isOptional) {
				this.isOptional = isOptional;
				cfg = new Tables.SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
			}
			@Override public Tables.SimplifiedColumnConfig getColumnConfig() {
				return cfg;
			}
		
		}

		private final Data data;

		ObjectListsTableModel(Data data) {
			super(ColumnID.values(), data.objectLists);
			this.data = data;
		}

		@Override public void setDefaultCellEditorsAndRenderers() {
			GeneralTCR renderer = new GeneralTCR(this);
			table.setDefaultRenderer(Double    .class, renderer);
			table.setDefaultRenderer(Long      .class, renderer);
			table.setDefaultRenderer(Boolean   .class, renderer);
			table.setDefaultRenderer(String    .class, renderer);
		}

		@Override public String getRowText(ObjectList row, int rowIndex) {
			String str = row==null ? "No Data" : row.generateOutput();
			if (row!=null && data.mapWorldObjects.containsKey(row.id)) {
				WorldObject twin = data.mapWorldObjects.get(row.id);
				str += String.format("%n#################################%n");
				str += String.format(  "  Twin WorldObject with same ID%n%n");
				str += twin.generateOutput();
			}
			return str;
		}

		@Override protected Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID, ObjectList row) {
			switch (columnID) {
				case id         : return row.id;
				case nonUniqueID: return row.nonUniqueID;
				case IsTwinID   : return data.mapWorldObjects.containsKey(row.id);
				case container  : return row.container==null ? "--" : row.container.getShortDesc();
				case size       : return row.size;
				case Filling    : return row.worldObjs==null || row.size==0 ? null : row.worldObjs.length / (double) row.size;
				case worldObjs:
					//Iterator<String> it = Arrays.stream(row.worldObjIds).mapToObj(n->Integer.toString(n)).iterator();
					//Iterator<String> it = Arrays.stream(row.worldObjs).map(wo->wo==null ? "<????>" : wo.objType).iterator();
					Iterator<String> it = row.getContentResume().stream().map(e->String.format("%dx %s", e.getValue(), e.getKey())).iterator();
					return String.join(", ", (Iterable<String>)()->it);
				case demandItems: return ObjectList.toString(row.demandItems, row.demandItemsStr);
				case supplyItems: return ObjectList.toString(row.supplyItems, row.supplyItemsStr);
				case dronePrio  : return row.dronePrio;
			}
			return null;
		}

		
	}
}