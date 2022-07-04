package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.ObjectList;

class ObjectListsPanel extends AbstractTablePanel<ObjectList, ObjectListsPanel.ObjectListsTableModel.ColumnID> {
	private static final long serialVersionUID = -1787920497956857504L;

	ObjectListsPanel(Vector<ObjectList> objectLists) {
		super(new ObjectListsTableModel(objectLists));
	}
	
	static class ObjectListsTableModel extends AbstractTablePanel.AbstractTableModel<ObjectList, ObjectListsTableModel.ColumnID> {

		enum ColumnID implements Tables.SimplifiedColumnIDInterface {
			id         ("ID"       , Long  .class,  75),
			container  ("Container", String.class, 130),
			size       ("Size"     , Long  .class,  50),
			worldObjs  ("Content"  , String.class, 600),
			;
			private final SimplifiedColumnConfig cfg;
			ColumnID(String name, Class<?> colClass, int width) {
				cfg = new SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() {
				return cfg;
			}
		
		}

		ObjectListsTableModel(Vector<ObjectList> data) {
			super(ColumnID.values(), data);
		}

		@Override protected String getRowText(ObjectList row, int rowIndex) {
			return "Dummy"; // TODO: ObjectListsTableModel.getRowText
		}

		@Override protected Object getValueAt(int rowIndex, int columnIndex, ObjectListsTableModel.ColumnID columnID, ObjectList row) {
			switch (columnID) {
			case id       : return row.id;
			case container: return row.container==null ? "--" : row.container.objType;
			case size     : return row.size;
			case worldObjs:
				//Iterator<String> it = Arrays.stream(row.worldObjIds).mapToObj(n->Integer.toString(n)).iterator();
				Iterator<String> it = Arrays.stream(row.worldObjs).map(wo->wo==null ? "<????>" : wo.objType).iterator();
				return String.join(", ", (Iterable<String>)()->it);
			}
			return null;
		}
		
	}
}