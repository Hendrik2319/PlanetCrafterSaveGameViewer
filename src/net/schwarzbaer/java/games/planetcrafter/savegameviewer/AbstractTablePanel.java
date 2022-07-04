package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;

class AbstractTablePanel<ValueType, ColumnID extends Tables.SimplifiedColumnIDInterface> extends JPanel {
	private static final long serialVersionUID = 5518131959056782917L;
	
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
	
	AbstractTablePanel(AbstractTablePanel.AbstractTableModel<ValueType, ColumnID> tableModel) {
		this(tableModel, LayoutPos.Bottom, new Dimension(100,100));
	}
	AbstractTablePanel(AbstractTablePanel.AbstractTableModel<ValueType, ColumnID> tableModel, LayoutPos textAreaPos, Dimension textAreaSize) {
		super(new BorderLayout(3,3));
		
		JTextArea textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setLineWrap(false);
		
		JTable table = new JTable(tableModel);
		table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.getSelectionModel().addListSelectionListener(e -> {
			int rowV = table.getSelectedRow();
			if (rowV<0) return;
			int rowM = table.convertRowIndexToModel(rowV);
			if (rowM<0) return;
			ValueType row = tableModel.getRow(rowM);
			String str = tableModel.getRowText(row,rowM);
			textArea.setText(str);
		});
		tableModel.setTable(table);
		tableModel.setColumnWidths(table);
		
		new TableContextMenu(table,tableModel);
		
		JScrollPane tableScrollPane = new JScrollPane(table);
		JScrollPane textareaScrollPane = new JScrollPane(textArea);
		textareaScrollPane.setPreferredSize(textAreaSize);
		
		add(tableScrollPane, BorderLayout.CENTER);
		add(textareaScrollPane,LayoutPos.getBorderLayoutValue(textAreaPos));
	}
	
	protected class TableContextMenu extends ContextMenu {
		private static final long serialVersionUID = 1755523803906870773L;

		TableContextMenu(JTable table, AbstractTablePanel.AbstractTableModel<ValueType,ColumnID> tableModel) {
			add(PlanetCrafterSaveGameViewer.createMenuItem("Show Column Widths", e->{
				System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(table));
			}));
			
			addTo(table);
		}
	}
	
	protected static abstract class AbstractTableModel<ValueType, ColumnID extends Tables.SimplifiedColumnIDInterface> extends Tables.SimplifiedTableModel<ColumnID> {
		
		final Vector<ValueType> data;

		protected AbstractTableModel(ColumnID[] columns, Vector<ValueType> data) {
			super(columns);
			this.data = data;
		}
		
		@Override public int getRowCount() { return data.size(); }
		
		ValueType getRow(int rowIndex) {
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
		protected abstract String getRowText(ValueType row, int rowIndex);
		
	}
}