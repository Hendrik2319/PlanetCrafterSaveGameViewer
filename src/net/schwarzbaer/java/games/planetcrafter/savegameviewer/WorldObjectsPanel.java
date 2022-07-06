package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Dimension;
import java.util.Vector;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Coord3;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Rotation;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;

class WorldObjectsPanel extends AbstractTablePanel<WorldObject, WorldObjectsPanel.WorldObjectsTableModel.ColumnID> {
	private static final long serialVersionUID = 8733627835226098636L;

	WorldObjectsPanel(Vector<WorldObject> worldObjects) {
		super(new WorldObjectsTableModel(worldObjects), LayoutPos.Right, new Dimension(300, 200));
	}
	
	static class WorldObjectsTableModel extends AbstractTablePanel.AbstractTableModel<WorldObject, WorldObjectsTableModel.ColumnID> {

		enum ColumnID implements Tables.SimplifiedColumnIDInterface {
			id          ("ID"          , Long    .class,  75),
			objectTypeID("ObjectTypeID", String  .class, 130),
			Name        ("Name"        , String  .class, 130),
			container   ("Container"   , String  .class, 350),
			listId      ("List-ID"     , Long    .class,  70),
			text        ("Text"        , String  .class, 120),
			growth      ("Growth"      , Long    .class,  60),
			position    ("Position"    , Coord3  .class, 200),
			rotation    ("Rotation"    , Rotation.class, 205),
			_color      ("[color]"     , String  .class,  50),
			//color     (""            , Coord3  .class,  50),
			_wear       ("[wear]"      , Long    .class,  50),
			_liGrps     ("[liGrps]"    , String  .class,  50),
			_pnls       ("[pnls]"      , String  .class,  90),
			;
			private final SimplifiedColumnConfig cfg;
			ColumnID(String name, Class<?> colClass, int width) {
				cfg = new SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() {
				return cfg;
			}
		
		}

		WorldObjectsTableModel(Vector<WorldObject> data) {
			super(ColumnID.values(), data);
		}

		@Override protected String getRowText(WorldObject row, int rowIndex) {
			return row==null ? "No Data" : row.generateOutput();
		}

		@Override protected Object getValueAt(int rowIndex, int columnIndex, WorldObjectsTableModel.ColumnID columnID, WorldObject row) {
			switch (columnID) {
			case _color       : return row._color;
			case _liGrps      : return row._liGrps;
			case _pnls        : return row._pnls;
			case _wear        : return row._wear;
			case growth       : return row.growth;
			case id           : return row.id;
			case listId       : return row.listId;
			case objectTypeID : return row.objectTypeID;
			case position     : return row.position;
			case rotation     : return row.rotation;
			case text         : return row.text;
			case Name         : return row.getName();
			case container:
				if (row.containerList==null)
					return null;
				if (row.container==null) {
					if (row.containerList.id==1)
						return "Player Inventory";
					if (row.containerList.id==2)
						return "Player Equipment";
					return String.format("<UnknownContainer> [List:%d]", row.containerList.id);
				}
				//return String.format("%s (\"%s\", Pos:%s)", row.container.objType, row.container.text, row.container.position);
				return row.container.getShortDesc();
			}
			return null;
		}
		
	}
}