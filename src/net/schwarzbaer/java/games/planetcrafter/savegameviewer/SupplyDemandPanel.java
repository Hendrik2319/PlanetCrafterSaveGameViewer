package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.ObjectList;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.ValueListOutput;

class SupplyDemandPanel extends TablePanelWithTextArea<SupplyDemandPanel.SupplyDemandTableModel.RowType, SupplyDemandPanel.SupplyDemandTableModel.ColumnID, SupplyDemandPanel.SupplyDemandTableModel>
{
	private static final long serialVersionUID = 3889765960212837045L;

	SupplyDemandPanel(Data data)
	{
		super( new SupplyDemandTableModel(data), true, /*(table,tableModel) -> new TableContextMenu(table, tableModel, mapPanel)*/null, LayoutPos.Right, new Dimension(300,100) );
	}
	
	static class GeneralTCR implements TableCellRenderer {
		
		private final Tables.LabelRendererComponent standardComp;
		private final SupplyDemandTableModel tableModel;

		GeneralTCR(SupplyDemandTableModel supplyDemandTableModel) {
			this.tableModel = supplyDemandTableModel;
			standardComp = new Tables.LabelRendererComponent();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
			//int rowM    = rowV   <0 ? -1 : table.convertRowIndexToModel   (rowV   );
			int columnM = columnV<0 ? -1 : table.convertColumnIndexToModel(columnV);
			//RowType row = tableModel.getRow(rowM);
			SupplyDemandTableModel.ColumnID columnID = tableModel.getColumnID(columnM);
			
			String valueStr = value != null ? value.toString() : null;
			
			if (columnID==SupplyDemandTableModel.ColumnID.Filling && value instanceof Double)
				valueStr = String.format(Locale.ENGLISH, "%1.1f%%", ((Double) value)*100);
			
			standardComp.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus);
			if (value instanceof Number)
				standardComp.setHorizontalAlignment(SwingConstants.RIGHT);
			else
				standardComp.setHorizontalAlignment(SwingConstants.LEFT);
			
			return standardComp;
		}
	}
	
	static class SupplyDemandTableModel extends TablePanelWithTextArea.AbstractTableModel<SupplyDemandTableModel.RowType, SupplyDemandTableModel.ColumnID>
	{
		enum ColumnID implements Tables.SimplifiedColumnIDInterface {
			id         ("ID"                , Long     .class,  75),
			container  ("Assigned Container", String   .class, 350),
			size       ("Size"              , Long     .class,  50),
			Filling    ("Filling"           , Double   .class,  50),
			worldObjs  ("Content"           , String   .class, 400),
			direction  ("Direction"         , Direction.class,  70),
			ObjectType ("Object Type"       , String   .class, 170),
			;
			private final Tables.SimplifiedColumnConfig cfg;
			ColumnID(String name, Class<?> colClass, int width) {
				cfg = new Tables.SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
			}
			@Override public Tables.SimplifiedColumnConfig getColumnConfig() {
				return cfg;
			}
		}
		
		enum Direction { Supply, Demand }

		record RowType(ObjectList ol, ObjectType ot, Direction direction) {}

		SupplyDemandTableModel(Data data)
		{
			super(ColumnID.values(), createRows(data));
		}

		private static Vector<RowType> createRows(Data data)
		{
			Vector<RowType> rows = new Vector<>();
			for (ObjectList ol : data.objectLists)
			{
				if (ol.supplyItems!=null)
					for (ObjectType ot : ol.supplyItems)
						rows.add(new RowType( ol, ot, Direction.Supply ));
				
				if (ol.demandItems!=null)
					for (ObjectType ot : ol.demandItems)
						rows.add(new RowType( ol, ot, Direction.Demand ));
			}
			return rows;
		}

		@Override public void setDefaultCellEditorsAndRenderers() {
			GeneralTCR renderer = new GeneralTCR(this);
			table.setDefaultRenderer(Double    .class, renderer);
			table.setDefaultRenderer(Long      .class, renderer);
			table.setDefaultRenderer(Boolean   .class, renderer);
			table.setDefaultRenderer(String    .class, renderer);
		}

		@Override
		public String getRowText(RowType row, int rowIndex)
		{
			if (row==null)
				return "No Data";
			
			ValueListOutput out = new ValueListOutput();
			
			out.add(0, "Object Type", "%s", row.ot.getName());
			out.add(0, "Direction"  , "%s", row.direction);
			
			out.addEmptyLine();
			out.add(0, "Object List");
			row.ol.generateOutput(out, 1, false);
			
			return out.generateOutput();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID, RowType row)
		{
			switch (columnID) {
				case ObjectType: return row.ot.getName();
				case direction : return row.direction;
				case id        : return row.ol.id;
				case container : return row.ol.container==null ? "--" : row.ol.container.wo().getShortDesc();
				case size      : return row.ol.size;
				case Filling   : return row.ol.worldObjs==null || row.ol.size==0 ? null : row.ol.worldObjs.length / (double) row.ol.size;
				case worldObjs : return String.join(", ", (Iterable<String>)()->row.ol.getContentResume().stream().map(e->String.format("%dx %s", e.getValue(), e.getKey())).iterator());
			}
			return null;
		}
		
	}
}
