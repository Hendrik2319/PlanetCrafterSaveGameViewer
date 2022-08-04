package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Iterator;
import java.util.function.Supplier;

import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.ObjectList;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;

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
			int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
			ObjectList row = tableModel.getRow(rowM);
			
			final Component selectedRendererComponent;
			
			Supplier<Color> getCustomBackground = ()->{
				if (row==null) return null;
				if (!row.isMarkedForRemoval()) return null;
				if (!row.canMarkedByUser())
					return GUI.COLOR_Removal_ByData;
				else
					return GUI.COLOR_Removal_ByUser;
			};
				
			if (value instanceof Boolean) {
				boolean isChecked = (Boolean) value;
				selectedRendererComponent = boolComp;
				boolComp.configureAsTableCellRendererComponent(table, isChecked, null, isSelected, hasFocus, null, getCustomBackground);
				boolComp.setHorizontalAlignment(SwingConstants.CENTER);
				
			} else {
				selectedRendererComponent = standardComp;
				String valueStr = value==null ? null : value.toString();
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
			id         ("ID"       , Long   .class,  75),
			twinID     ("#"        , Boolean.class,  30),
			container  ("Container", String .class, 350),
			size       ("Size"     , Long   .class,  50),
			worldObjs  ("Content"  , String .class, 600),
			;
			private final Tables.SimplifiedColumnConfig cfg;
			ColumnID(String name, Class<?> colClass, int width) {
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

		@Override public void setDefaultRenderers() {
			GeneralTCR renderer = new GeneralTCR(this);
			table.setDefaultRenderer(Long      .class, renderer);
			table.setDefaultRenderer(Boolean   .class, renderer);
			table.setDefaultRenderer(String    .class, renderer);
		}

		@Override protected String getRowText(ObjectList row, int rowIndex) {
			String str = row==null ? "No Data" : row.generateOutput();
			if (data.mapWorldObjects.containsKey(row.id)) {
				WorldObject twin = data.mapWorldObjects.get(row.id);
				str += String.format("%n#################################%n");
				str += String.format(  "  Twin WorldObject with same ID%n%n");
				str += twin.generateOutput();
			}
			return str;
		}

		@Override protected Object getValueAt(int rowIndex, int columnIndex, ObjectListsTableModel.ColumnID columnID, ObjectList row) {
			switch (columnID) {
			case id       : return row.id;
			case twinID   : return data.mapWorldObjects.containsKey(row.id);
			case container: return row.container==null ? "--" : row.container.getShortDesc();
			case size     : return row.size;
			case worldObjs:
				//Iterator<String> it = Arrays.stream(row.worldObjIds).mapToObj(n->Integer.toString(n)).iterator();
				//Iterator<String> it = Arrays.stream(row.worldObjs).map(wo->wo==null ? "<????>" : wo.objType).iterator();
				Iterator<String> it = row.getContentResume().stream().map(e->String.format("%dx %s", e.getValue(), e.getKey())).iterator();
				return String.join(", ", (Iterable<String>)()->it);
			}
			return null;
		}
		
	}
}