package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.JTable;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.FarWreckAreas.WreckArea;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;

class FarWreckAreaTablePanel extends TablePanelWithTextArea<WreckArea, FarWreckAreaTablePanel.FarWreckAreaTableModel.ColumnID, FarWreckAreaTablePanel.FarWreckAreaTableModel>
{
	private static final long serialVersionUID = -5756462259451478608L;

	FarWreckAreaTablePanel() {
		super(new FarWreckAreaTableModel(), true, ContextMenu::new, LayoutPos.Right, new Dimension(200,200));
	}
	
	private static class ContextMenu extends TableContextMenu
	{
		private static final long serialVersionUID = -798929965802654061L;

		public ContextMenu(JTable table, FarWreckAreaTableModel tableModel)
		{
			super(table);
			add(GUI.createMenuItem("Add empty area", e->{
				FarWreckAreas wreckAreas = FarWreckAreas.getInstance();
				wreckAreas.addEmptyArea();
				tableModel.updateData();
				wreckAreas.writeToFile();
			}));
		}
	}
	
	protected static class FarWreckAreaTableModel
		extends Tables.SimpleGetValueTableModel<WreckArea, FarWreckAreaTableModel.ColumnID>
		implements TablePanelWithTextArea.TableModelExtension<WreckArea>
	{
		// Column Widths: [22, 59, 450] in ModelOrder
		enum ColumnID implements Tables.SimpleGetValueTableModel.ColumnIDTypeInt<WreckArea> {
			Index    ("#"       , Integer.class,  25, null),
			Editable ("Editable", Boolean.class,  60, null),
			Points   ("Points"  , String .class, 450, wa->toString(wa.points)),
			;
			private SimplifiedColumnConfig cfg;
			private Function<WreckArea, ?> getValue;
			
			<ColumnClass> ColumnID(String name, Class<ColumnClass> colClass, int width, Function<WreckArea, ColumnClass> getValue) {
				this.getValue = getValue;
				cfg = new Tables.SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
			}

			private static String toString(Vector<Point2D.Double> points)
			{
				Iterable<String> it = ()->points
					.stream()
					.map(p->String.format(Locale.ENGLISH, "(%1.2f, %1.2f)", p.x, p.y))
					.iterator();
				return String.join(", ", it);
			}

			@Override public SimplifiedColumnConfig getColumnConfig() { return cfg; }
			@Override public Function<WreckArea, ?> getGetValue() { return getValue; }
		}
		
		FarWreckAreaTableModel() {
			super(ColumnID.values(), FarWreckAreas.getInstance().getAreas());
		}
		
		void updateData()
		{
			setData(FarWreckAreas.getInstance().getAreas());
		}

		@Override
		public String getRowText(WreckArea row, int rowIndex)
		{
			if (row==null)
				return "";
			
			if (row.points.isEmpty())
				return "no points";
			
			StringBuilder sb = new StringBuilder();
			
			for (Point2D.Double p : row.points)
				sb.append(String.format(Locale.ENGLISH, "(%1.4f, %1.4f)%n", p.x, p.y));
			
			return sb.toString();
		}

		@Override
		protected Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID, WreckArea row)
		{
			if (columnID == ColumnID.Index)
				return rowIndex+1;
			
			if (columnID == ColumnID.Editable)
				return FarWreckAreas.getInstance().getEditableArea() == row;
			
			return super.getValueAt(rowIndex, columnIndex, columnID, row);
		}

		@Override
		protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID)
		{
			return columnID==ColumnID.Editable || super.isCellEditable(rowIndex, columnIndex, columnID);
		}

		@Override
		protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID)
		{
			WreckArea row = getRow(rowIndex);
			
			if (columnID==ColumnID.Editable && row!=null)
			{
				FarWreckAreas.getInstance().setEditableArea(row);
				fireTableColumnUpdate(ColumnID.Editable);
			}
			
			super.setValueAt(aValue, rowIndex, columnIndex, columnID);
		}
		
		
	}

}
