package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Coord3;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.ObjectList;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Rotation;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.lib.gui.Tables;

class WorldObjectsPanel extends AbstractTablePanel<WorldObject, WorldObjectsPanel.WorldObjectsTableModel.ColumnID, WorldObjectsPanel.WorldObjectsTableModel> {
	private static final long serialVersionUID = 8733627835226098636L;

	WorldObjectsPanel(PlanetCrafterSaveGameViewer main, Data data, MapPanel mapPanel) {
		super(new WorldObjectsTableModel(data), false, (table,tableModel) -> new TableContextMenu(main, table, tableModel, mapPanel), LayoutPos.Right, new Dimension(300, 200));
	}

	WorldObjectsPanel(PlanetCrafterSaveGameViewer main, WorldObject[] worldObjects, MapPanel mapPanel) {
		this(main, worldObjects, mapPanel, LayoutPos.Right, new Dimension(300, 200));
	}

	WorldObjectsPanel(PlanetCrafterSaveGameViewer main, WorldObject[] worldObjects, MapPanel mapPanel, LayoutPos textAreaPos, Dimension textAreaSize) {
		super(new WorldObjectsTableModel(worldObjects), false, (table,tableModel) -> new TableContextMenu(main, table, tableModel, mapPanel), textAreaPos, textAreaSize);
	}
	
	void setData(WorldObject[] worldObjs)
	{
		tableModel.setData(worldObjs);
	}

	private static class TableContextMenu extends AbstractTablePanel.TableContextMenu {
		private static final long serialVersionUID = -8757567111391531443L;
		private int clickedRowIndex;
		private WorldObject clickedRow;
		private int[] selectedRowIndexes;
		private WorldObject[] selectedRows;

		TableContextMenu(PlanetCrafterSaveGameViewer main, JTable table, WorldObjectsTableModel tableModel, MapPanel mapPanel) {
			super(table);
			clickedRowIndex = -1;
			
			addSeparator();
			
			JMenuItem miShowInMap = add(GUI.createMenuItem("Show in Map", e->{
				if (!WorldObject.isInstalled(clickedRow)) return;
				mapPanel.showWorldObject(clickedRow);
			}));
			
			JMenuItem miShowContainerInMap = add(GUI.createMenuItem("Show Container in Map", e->{
				if (clickedRow==null) return;
				if (!WorldObject.isInstalled(clickedRow.container))return;
				mapPanel.showWorldObject(clickedRow.container);
			}));
			
			addSeparator();
			
			JMenuItem miEditMapShapes = add(GUI.createMenuItem("Create/Edit MapShapes", e->{
				if (clickedRow==null) return;
				main.showMapShapesEditor(clickedRow.objectType);
			}));
			
			addSeparator();
			
			JMenuItem miMarkForRemoval = add(GUI.createMenuItem("Mark clicked object for removal", e->{
				if (clickedRow!=null && clickedRow.canMarkedByUser()) {
					clickedRow.markForRemoval( !clickedRow.isMarkedForRemoval(), true );
					tableModel.fireTableRowUpdate(clickedRowIndex);
					Data.notifyAllRemoveStateListeners();
				}
			}));
			
			JMenuItem miMarkSelectedForRemoval = add(GUI.createMenuItem("Mark selected object(s) for removal", e->{
				boolean markForRemoval = selectedRows[0]==null || !selectedRows[0].isMarkedForRemoval();
				for (WorldObject wo : selectedRows)
					if (wo!=null && wo.canMarkedByUser())
						wo.markForRemoval( markForRemoval, true );
				tableModel.fireTableUpdate();
				Data.notifyAllRemoveStateListeners();
			}));
			
			
			addContextMenuInvokeListener((comp, x, y) -> {
				int rowV = table.rowAtPoint(new Point(x,y));
				clickedRowIndex = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
				clickedRow = clickedRowIndex<0 ? null : tableModel.getRow(clickedRowIndex);
				
				int[] selectedRowIndexesV = table.getSelectedRows();
				selectedRowIndexes = new int[selectedRowIndexesV.length];
				selectedRows = new WorldObject[selectedRowIndexesV.length];
				for (int i=0; i<selectedRowIndexes.length; i++) {
					selectedRowIndexes[i] = selectedRowIndexesV[i]<0 ? -1 : table.convertRowIndexToModel(selectedRowIndexesV[i]);
					selectedRows[i] = selectedRowIndexes[i]<0 ? null : tableModel.getRow(selectedRowIndexes[i]);
				}
				
				miMarkForRemoval.setEnabled(clickedRow!=null && clickedRow.canMarkedByUser());
				miMarkForRemoval.setText(
						clickedRow == null
							? "Mark clicked object for removal"
							: clickedRow.isMarkedForRemoval()
								? String.format("Remove Removal Marker from \"%s\"", clickedRow.getName())
								: String.format("Mark \"%s\" for removal", clickedRow.getName())
				);
				
				miMarkSelectedForRemoval.setEnabled(selectedRows.length>0);
				miMarkSelectedForRemoval.setText(
						selectedRows.length == 0
							? "Mark selected object(s) for removal"
							: selectedRows[0]!=null && selectedRows[0].isMarkedForRemoval()
								? String.format("Remove Removal Marker from %d selected object%s", selectedRows.length, selectedRows.length==1 ? "" : "s")
								: String.format("Mark %d selected object%s for removal", selectedRows.length, selectedRows.length==1 ? "" : "s")
				);
				
				miShowInMap.setEnabled(
					WorldObject.isInstalled(clickedRow) );
				miShowInMap.setText(
					! WorldObject.isInstalled(clickedRow)
					? "Show in Map"
					: String.format("Show \"%s\" in Map", clickedRow.getName())
				);
				
				miShowContainerInMap.setEnabled(
					clickedRow!=null && WorldObject.isInstalled(clickedRow.container));
				miShowContainerInMap.setText(
					clickedRow==null || !WorldObject.isInstalled(clickedRow.container)
					? "Show Container in Map"
					: String.format("Show Container \"%s\" in Map", clickedRow.container.getName())
				);
				
				miEditMapShapes.setEnabled(clickedRow!=null);
				miEditMapShapes.setText(
					clickedRow == null
					? "Create/Edit MapShapes"
					: main.mapShapes.hasShapes(clickedRow.objectType)
						? String.format(  "Edit MapShapes of \"%s\"", clickedRow.getName())
						: String.format("Create MapShapes of \"%s\"", clickedRow.getName())
				);
			});
		}
	}
	
	static class GeneralTCR implements TableCellRenderer {
		
		private final Tables.LabelRendererComponent standardComp;
		private final Tables.CheckBoxRendererComponent boolComp;
		private final Tables.ColorRendererComponent colorComp;
		private final WorldObjectsTableModel tableModel;

		GeneralTCR(WorldObjectsTableModel tableModel) {
			this.tableModel = tableModel;
			colorComp = new Tables.ColorRendererComponent();
			standardComp = new Tables.LabelRendererComponent();
			boolComp = new Tables.CheckBoxRendererComponent();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
			int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
			int columnM = columnV<0 ? -1 : table.convertColumnIndexToModel(columnV);
			WorldObjectsTableModel.ColumnID columnID = tableModel.getColumnID(columnM);
			WorldObject row = tableModel.getRow(rowM);
			
			final Component selectedRendererComponent;
			
			Supplier<Color> getCustomBackground = ()->{
				if (row==null) return null;
				if (!row.isMarkedForRemoval()) return null;
				if (!row.canMarkedByUser())
					return GUI.COLOR_Removal_ByData;
				else
					return GUI.COLOR_Removal_ByUser;
			};
			
			if (value instanceof Data.Color) {
				Data.Color color = (Data.Color) value;
				value = color.getColor();
			}
			
			if (value instanceof Color) {
				selectedRendererComponent = colorComp;
				Supplier<String> getSurrogateText = ()->{
					if (columnID==WorldObjectsTableModel.ColumnID.color && row!=null)
						return row.colorStr;
					return null;
				};
				colorComp.configureAsTableCellRendererComponent(table, value, isSelected, hasFocus, getSurrogateText, getCustomBackground, null);
			} else
				
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
	
	static class WorldObjectsTableModel
			extends Tables.SimpleGetValueTableModel2<WorldObjectsTableModel, WorldObject, WorldObjectsTableModel.ColumnID>
			implements AbstractTablePanel.TableModelExtension<WorldObject>
	{
		// Column Widths: [75, 30, 35, 130, 130, 350, 70, 120, 60, 130, 36, 200, 33, 205, 50, 90, 50] in ModelOrder
		enum ColumnID implements Tables.SimpleGetValueTableModel2.ColumnIDTypeInt2<WorldObjectsTableModel,WorldObject> {
			id          ("ID"          , Long      .class,  75,        row  -> row.id),
			NonUniqueID ("UnI"         , Boolean   .class,  30,        row  -> row.isEmptyWO ? null : row.nonUniqueID),
			twinID      ("Twin"        , Boolean   .class,  35, (model,row) -> row.isEmptyWO ? null : model.getMapObjectLists().containsKey(row.id)),
			objectTypeID("ObjectTypeID", String    .class, 130,        row  -> row.isEmptyWO ? null : row.objectTypeID),
			Name        ("Name"        , String    .class, 130,        row  -> row.isEmptyWO ? null : row.getName()),
			container   ("Container"   , String    .class, 350,        row  -> row.isEmptyWO ? null : row.getContainerLabel()),
			listId      ("List-ID"     , Long      .class,  70,        row  -> row.isEmptyWO ? null : row.listId),
			text        ("Text"        , String    .class, 120,        row  -> row.isEmptyWO ? null : row.text),
			growth      ("Growth"      , Long      .class,  60,        row  -> row.isEmptyWO ? null : row.growth),
			product     ("Product"     , String    .class, 130,        row  -> row.isEmptyWO ? null : getProductsStr(row)),
			has_position("Pos."        , Boolean   .class,  35,        row  -> row.isEmptyWO ? null : row.position!=null && !row.position.isZero()),
			position    ("Position"    , Coord3    .class, 200,        row  -> row.isEmptyWO ? null : row.position),
			has_rotation("Rot."        , Boolean   .class,  35,        row  -> row.isEmptyWO ? null : row.rotation!=null && !row.rotation.isZero()),
			rotation    ("Rotation"    , Rotation  .class, 205,        row  -> row.isEmptyWO ? null : row.rotation),
			color       ("Color"       , Data.Color.class,  50,        row  -> row.isEmptyWO ? null : row.color),
			mods        ("Mods"        , String    .class,  90,        row  -> row.isEmptyWO ? null : row.mods),
			_wear       ("[wear]"      , Long      .class,  50,        row  -> row.isEmptyWO ? null : row._wear),
			_set        ("[set]"       , Long      .class,  50,        row  -> row.isEmptyWO ? null : row._set),
			;
			private final Tables.SimplifiedColumnConfig cfg;
			private final Function<WorldObject, ?> getValue;
			private final BiFunction<WorldObjectsTableModel, WorldObject, ?> getValueM;
			
			<ColumnClass> ColumnID(String name, Class<ColumnClass> colClass, int width, BiFunction<WorldObjectsTableModel, WorldObject, ColumnClass> getValueM) {
				this(name, colClass, width, null, getValueM);
			}
			<ColumnClass> ColumnID(String name, Class<ColumnClass> colClass, int width, Function<WorldObject, ColumnClass> getValue) {
				this(name, colClass, width, getValue, null);
			}
			<ColumnClass> ColumnID(String name, Class<ColumnClass> colClass, int width, Function<WorldObject, ColumnClass> getValue, BiFunction<WorldObjectsTableModel, WorldObject, ColumnClass> getValueM) {
				this.getValue = getValue;
				this.getValueM = getValueM;
				cfg = new Tables.SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
			}
			@Override public Tables.SimplifiedColumnConfig getColumnConfig() { return cfg; }
			@Override public   Function<                        WorldObject, ?> getGetValue () { return getValue ; }
			@Override public BiFunction<WorldObjectsTableModel, WorldObject, ?> getGetValueM() { return getValueM; }
		}

		private final Data data;

		WorldObjectsTableModel(WorldObject[] worldObjects) {
			super( getArrayWithout(ColumnID.values(),ColumnID.twinID), worldObjects );
			this.data = null;
		}
		
		WorldObjectsTableModel(Data data) {
			super( ColumnID.values(), data.worldObjects );
			this.data = data;
		}

		private static ColumnID[] getArrayWithout(ColumnID[] arr1, ColumnID... arr2)
		{
			return Arrays
					.stream(arr1)
					.filter(columnID -> {
						for (ColumnID columnID2 : arr2)
							if (columnID2 == columnID)
								return false;
						return true;
					})
					.toArray(ColumnID[]::new);
		}

		@Override protected WorldObjectsTableModel getThis() { return this; }

		@Override public void setDefaultCellEditorsAndRenderers() {
			GeneralTCR renderer = new GeneralTCR(this);
			setDefaultRenderers(class_ -> renderer);
		}

		@Override public String getRowText(WorldObject row, int rowIndex) {
			String str = row==null ? "No Data" : row.generateOutput();
			HashMap<Long, ObjectList> mapObjectLists = getMapObjectLists();
			if (row!=null && mapObjectLists.containsKey(row.id)) {
				ObjectList twin = mapObjectLists.get(row.id);
				str += String.format("%n#################################%n");
				str += String.format(  "  Twin ObjectList with same ID%n%n");
				str += twin.generateOutput();
			}
			return str;
		}

		private HashMap<Long, ObjectList> getMapObjectLists()
		{
			if (data!=null && data.mapObjectLists!=null)
				return data.mapObjectLists;
			return new HashMap<>();
		}

		private static String getProductsStr(WorldObject row)
		{
			if (row.products   !=null &&  row.products   .length>0 ) return ObjectTypes.ObjectType.toString(row.products);
			if (row.productIDs !=null &&  row.productIDs .length>0 ) return Data.toString(row.productIDs);
			if (row.productsStr!=null && !row.productsStr.isEmpty()) return String.format("{ %s }", row.productsStr);
			return null;
		}

		@Override public void fireTableRowUpdate(int rowIndex) {
			super.fireTableRowUpdate(rowIndex);
		}
	}
}