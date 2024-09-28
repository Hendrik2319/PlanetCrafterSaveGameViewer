package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Vector;

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

class TablePanelWithTextArea<
			ValueType,
			ColumnID extends Tables.SimplifiedColumnIDInterface,
			TableModelType extends Tables.SimplifiedTableModel<ColumnID> & TablePanelWithTextArea.TableModelExtension<ValueType>
		>
		extends JPanel
{
	private static final long serialVersionUID = 5518131959056782917L;
	
	protected final JTable table;
	protected final TableModelType tableModel;
	protected final JTextArea textArea;
	private   final JScrollPane textareaScrollPane;
	
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
	
	TablePanelWithTextArea(TableModelType tableModel, boolean singleLineSelectionOnly) {
		this(tableModel, singleLineSelectionOnly, null, LayoutPos.Bottom, new Dimension(100,100));
	}
	TablePanelWithTextArea(TableModelType tableModel, boolean singleLineSelectionOnly, TableContextMenuConstructor<TableModelType> tcmConstructor) {
		this(tableModel, singleLineSelectionOnly, tcmConstructor, LayoutPos.Bottom, new Dimension(100,100));
	}
	TablePanelWithTextArea(TableModelType tableModel, boolean singleLineSelectionOnly, LayoutPos textAreaPos, Dimension textAreaSize) {
		this(tableModel, singleLineSelectionOnly, null, textAreaPos, textAreaSize);
	}
	TablePanelWithTextArea(TableModelType tableModel, boolean singleLineSelectionOnly, TableContextMenuConstructor<TableModelType> tcmConstructor, LayoutPos textAreaPos, Dimension textAreaSize) {
		super(new BorderLayout(3,3));
		this.tableModel = tableModel;
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setLineWrap(false);
		
		table = new JTable(this.tableModel);
		table.setRowSorter(new Tables.SimplifiedRowSorter(this.tableModel));
		table.setSelectionMode(singleLineSelectionOnly ? ListSelectionModel.SINGLE_SELECTION : ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.getSelectionModel().addListSelectionListener(e -> {
			int rowV = table.getSelectedRow();
			if (rowV<0) return;
			int rowM = table.convertRowIndexToModel(rowV);
			if (rowM<0) return;
			ValueType row = this.tableModel.getRow(rowM);
			tableSelectionChanged(row);
			String str = this.tableModel.getRowText(row,rowM);
			setText(str);
		});
		this.tableModel.setTable(table);
		this.tableModel.setColumnWidths(table);
		this.tableModel.setDefaultCellEditorsAndRenderers();
		
		TableContextMenu tableContextMenu;
		if (tcmConstructor==null) tableContextMenu = new TableContextMenu(table);
		else tableContextMenu = tcmConstructor.create(table,this.tableModel);
		tableContextMenu.addTo(table);
		
		JScrollPane tableScrollPane = new JScrollPane(table);
		tableContextMenu.addTo(tableScrollPane);
		textareaScrollPane = new JScrollPane(textArea);
		textareaScrollPane.setPreferredSize(textAreaSize);
		
		add(tableScrollPane, BorderLayout.CENTER);
		add(textareaScrollPane,LayoutPos.getBorderLayoutValue(textAreaPos));
		
		GUI.reduceTextAreaFontSize(table, 1, textArea);
	}
	
	protected void tableSelectionChanged(ValueType row)	{}
	
	public void setText(String str)
	{
		ScrollPosition scrollPos = ScrollPosition.getVertical(textareaScrollPane);
		textArea.setText(str);
		if (scrollPos!=null)
			SwingUtilities.invokeLater(()->scrollPos.setVertical(textareaScrollPane));
	}
	
	void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) {
		table.setDefaultRenderer(columnClass, renderer);
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
	
	interface TableModelExtension<RowType> extends Tables.StandardTableModelExtension<RowType>  {
		String getRowText(RowType row, int rowIndex);
	}
	
	protected static abstract class AbstractTableModel<ValueType, ColumnID extends Tables.SimplifiedColumnIDInterface> extends Tables.SimplifiedTableModel<ColumnID> implements TableModelExtension<ValueType> {
		
		final Vector<ValueType> data;

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

		@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
			ValueType row = getRow(rowIndex);
			if (row==null) return null;
			return getValueAt(rowIndex, columnIndex, columnID, row);
		}
		
		protected abstract Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID, ValueType row);
		@Override
		public abstract String getRowText(ValueType row, int rowIndex);
		
	}
}