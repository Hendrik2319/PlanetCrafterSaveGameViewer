package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Vector;
import java.util.function.Supplier;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.java.lib.gui.ContextMenu;
import net.schwarzbaer.java.lib.gui.ScrollPosition;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedTableModel;

class TwoSidedTablePanel<
			ValueType,
			ColumnID extends Tables.SimplifiedColumnIDInterface,
			TableModelType extends Tables.SimplifiedTableModel<ColumnID> & Tables.StandardTableModelExtension<ValueType>,
			SideComponent extends Component
		>
		extends JPanel
{
	private static final long serialVersionUID = 5518131959056782917L;
	
	protected final JTable table;
	protected final TableModelType tableModel;
	protected final SideComponent sideComp;
	protected final JScrollPane sideCompScrollPane;
	protected final TableContextMenu tableContextMenu;
	
	enum LayoutPos {
		Top, Right, Bottom, Left;
		private static String getBorderLayoutValue(LayoutPos pos) {
			if (pos!=null)
				switch (pos) {
				case Bottom: return BorderLayout.SOUTH;
				case Left: return BorderLayout.WEST;
				case Right: return BorderLayout.EAST;
				case Top: return BorderLayout.NORTH;
				}
			return BorderLayout.SOUTH;
		}
	}
	
	TwoSidedTablePanel(
			Supplier<SideComponent> createSideComp, TableModelType tableModel, boolean singleLineSelectionOnly, boolean useScrollPane4SideComp,
			TableContextMenuConstructor<TableModelType> tcmConstructor,
			LayoutPos sideCompPos, Dimension sideCompSize
	) {
		super(new BorderLayout(3,3));
		this.tableModel = tableModel;
		
		table = new JTable(this.tableModel);
		table.setRowSorter(new Tables.SimplifiedRowSorter(this.tableModel));
		table.setSelectionMode(singleLineSelectionOnly ? ListSelectionModel.SINGLE_SELECTION : ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.getSelectionModel().addListSelectionListener(e -> {
			if (table.getSelectedRowCount()==1)
			{
				int rowV = table.getSelectedRow();
				if (rowV<0) return;
				int rowM = table.convertRowIndexToModel(rowV);
				if (rowM<0) return;
				ValueType row = this.tableModel.getRow(rowM);
				tableSelectionChanged(row,rowM);
			}
			else
			{
				int[] rowsV = table.getSelectedRows();
				int[] rowsM = Arrays
						.stream(rowsV)
						.map(rowV -> rowV<0 ? -1 : table.convertRowIndexToModel(rowV))
						.toArray();
				tableSelectionChanged(rowsM);
			}
		});
		this.tableModel.setTable(table);
		this.tableModel.setColumnWidths(table);
		this.tableModel.setDefaultCellEditorsAndRenderers();
		
		if (tcmConstructor==null) tableContextMenu = new TableContextMenu(table);
		else tableContextMenu = tcmConstructor.create(table,this.tableModel);
		tableContextMenu.addTo(table);
		
		JScrollPane tableScrollPane = new JScrollPane(table);
		tableContextMenu.addTo(tableScrollPane);
		
		sideComp = createSideComp.get();
		
		Component comp2Add;
		if (useScrollPane4SideComp) {
			sideCompScrollPane = new JScrollPane(sideComp);
			sideCompScrollPane.setPreferredSize(sideCompSize);
			comp2Add = sideCompScrollPane;
		} else {
			sideCompScrollPane = null;
			sideComp.setPreferredSize(sideCompSize);
			comp2Add = sideComp;
		}
		
		add(tableScrollPane, BorderLayout.CENTER);
		add(comp2Add,LayoutPos.getBorderLayoutValue(sideCompPos));
	}
	
	protected void tableSelectionChanged(int[] rowsM) {}
	protected void tableSelectionChanged(ValueType row, int rowM)	{}
	
	void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) {
		table.setDefaultRenderer(columnClass, renderer);
	}
	
	static class TablePanelWithTextArea<
			ValueType,
			ColumnID extends Tables.SimplifiedColumnIDInterface,
			TableModelType extends Tables.SimplifiedTableModel<ColumnID> & TablePanelWithTextArea.TableModelExtension<ValueType>
		>
		extends TwoSidedTablePanel<ValueType,ColumnID,TableModelType,JTextArea>
	{
		private static final long serialVersionUID = -2806050155502426529L;

		TablePanelWithTextArea(TableModelType tableModel, boolean singleLineSelectionOnly, LayoutPos textAreaPos, Dimension textAreaSize) {
			this(tableModel, singleLineSelectionOnly, null, textAreaPos, textAreaSize);
		}
		TablePanelWithTextArea(TableModelType tableModel, boolean singleLineSelectionOnly, TableContextMenuConstructor<TableModelType> tcmConstructor, LayoutPos textAreaPos, Dimension textAreaSize) {
			super(TablePanelWithTextArea::createTextArea, tableModel, singleLineSelectionOnly, true, tcmConstructor, textAreaPos, textAreaSize);
			GUI.reduceTextAreaFontSize(table, 1, sideComp);
		}
		
		private static JTextArea createTextArea() {
			JTextArea textArea = new JTextArea();
			textArea.setEditable(false);
			textArea.setLineWrap(false);
			return textArea;
		}
		
		interface TableModelExtension<RowType> extends Tables.StandardTableModelExtension<RowType>  {
			String getRowText(RowType row, int rowIndex);
		}
		
		@Override
		protected void tableSelectionChanged(ValueType row, int rowM)
		{
			super.tableSelectionChanged(row, rowM);
			setText(tableModel.getRowText(row,rowM));
		}
		
		@Override
		protected void tableSelectionChanged(int[] rowsM)
		{
			super.tableSelectionChanged(rowsM);
			setText("");
		}
		
		void setText(String str)
		{
			ScrollPosition scrollPos = ScrollPosition.getVertical(sideCompScrollPane);
			sideComp.setText(str);
			if (scrollPos!=null)
				SwingUtilities.invokeLater(()->scrollPos.setVertical(sideCompScrollPane));
		}
	}
	
	protected interface TableContextMenuConstructor<TableModelType> {
		TableContextMenu create(JTable table, TableModelType tableModel);
	}

	static class TableContextMenu extends ContextMenu {
		private static final long serialVersionUID = 1755523803906870773L;

		TableContextMenu(JTable table) {
			add(GUI.createMenuItem("Show Column Widths", e->{
				System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(table));
			}));
		}
	}
	
	protected static abstract class AbstractTableModel<
				ValueType,
				ColumnID extends Tables.SimplifiedColumnIDInterface
			>
			extends Tables.SimplifiedTableModel<ColumnID>
			implements TablePanelWithTextArea.TableModelExtension<ValueType>
	{
		protected final Vector<ValueType> data;

		protected AbstractTableModel(ColumnID[] columns, Vector<ValueType> data) {
			super(columns);
			this.data = data;
		}

		@Override
		public void setDefaultCellEditorsAndRenderers() {}

		@Override
		public int getRowCount() { return data.size(); }
		
		@Override
		public ValueType getRow(int rowIndex) {
			if (rowIndex < 0) return null;
			if (data.size() <= rowIndex) return null;
			return data.get(rowIndex);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
			ValueType row = getRow(rowIndex);
			if (row==null) return null;
			return getValueAt(rowIndex, columnIndex, columnID, row);
		}
		
		protected abstract Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID, ValueType row);
	}
}