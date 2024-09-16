package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.util.function.Function;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Coord3;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.GeneratedWreck;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Rotation;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;

class GeneratedWrecksPanel extends JSplitPane
{
	private static final long serialVersionUID = 5162885598069310200L;
	
	private final JTable wrecksTable;
	private final GeneratedWrecksTableModel wrecksTableModel;
	private final WorldObjectsPanel worldObjsGeneratedPanel;
	private final WorldObjectsPanel worldObjsDroppedPanel;
	
	GeneratedWrecksPanel(PlanetCrafterSaveGameViewer main, Data data, MapPanel mapPanel)
	{
		super(JSplitPane.VERTICAL_SPLIT, true);
		
		wrecksTable = new JTable(wrecksTableModel = new GeneratedWrecksTableModel(data));
		wrecksTable.setRowSorter(new Tables.SimplifiedRowSorter(wrecksTableModel));
		wrecksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		wrecksTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		wrecksTableModel.setTable(wrecksTable);
		wrecksTableModel.setColumnWidths(wrecksTable);
		wrecksTableModel.setDefaultCellEditorsAndRenderers();
		wrecksTable.setPreferredScrollableViewportSize(wrecksTable.getMinimumSize());
		JScrollPane wrecksTableScrollPane = new JScrollPane(wrecksTable);
		
		new AbstractTablePanel.TableContextMenu(wrecksTable).addTo(wrecksTable);
		
		worldObjsGeneratedPanel = new WorldObjectsPanel(main, new WorldObject[0], mapPanel);
		worldObjsDroppedPanel   = new WorldObjectsPanel(main, new WorldObject[0], mapPanel);
		
		wrecksTable.getSelectionModel().addListSelectionListener(e -> {
			int rowV = wrecksTable.getSelectedRow();
			if (rowV<0) return;
			int rowM = wrecksTable.convertRowIndexToModel(rowV);
			if (rowM<0) return;
			
			GeneratedWreck row = wrecksTableModel.getRow(rowM);
			worldObjsGeneratedPanel.setData(row.worldObjsGenerated);
			worldObjsDroppedPanel  .setData(row.worldObjsDropped  );
		});
		
		JTabbedPane objectListTablesPanel = new JTabbedPane();
		objectListTablesPanel.addTab("woIdsGenerated", worldObjsGeneratedPanel);
		objectListTablesPanel.addTab("woIdsDropped"  , worldObjsDroppedPanel  );
		
		setTopComponent(wrecksTableScrollPane);
		setBottomComponent(objectListTablesPanel);
	}
	
	static class GeneratedWrecksTableModel extends Tables.SimpleGetValueTableModel<GeneratedWreck, GeneratedWrecksTableModel.ColumnID>
	{
		// Column Widths: [45, 45, 70, 50, 35, 200, 35, 205, 120, 100, 90] in ModelOrder
		enum ColumnID implements Tables.SimpleGetValueTableModel.ColumnIDTypeInt<GeneratedWreck> {
			index             ("index"            , Long    .class,  45, row -> row.index            ),
			owner             ("owner"            , Long    .class,  45, row -> row.owner            ),
			seed              ("seed"             , Long    .class,  70, row -> row.seed             ),
			version           ("version"          , Long    .class,  50, row -> row.version          ),
			has_position      ("Pos."             , Boolean .class,  35, row -> row.position!=null && !row.position.isZero()),
			position          ("Position"         , Coord3  .class, 200, row -> row.position         ),
			has_rotation      ("Rot."             , Boolean .class,  35, row -> row.rotation!=null && !row.rotation.isZero()),
			rotation          ("Rotation"         , Rotation.class, 205, row -> row.rotation         ),
			wrecksWOGenerated ("wrecksWOGenerated", Boolean .class, 120, row -> row.wrecksWOGenerated),
			woIdsGenerated    ("woIdsGenerated"   , Integer .class, 100, row -> row.worldObjsGenerated.length),
			woIdsDropped      ("woIdsDropped"     , Integer .class,  90, row -> row.worldObjsDropped  .length),
			;
			private final SimplifiedColumnConfig cfg;
			private final Function<GeneratedWreck, ?> getValue;
			
			<ColumnClass> ColumnID(String name, Class<ColumnClass> colClass, int width, Function<GeneratedWreck, ColumnClass> getValue) {
				this.getValue = getValue;
				cfg = new Tables.SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
			}

			@Override public SimplifiedColumnConfig getColumnConfig() { return cfg; }
			@Override public Function<GeneratedWreck, ?> getGetValue() { return getValue; }
		}

		GeneratedWrecksTableModel(Data data)
		{
			super(ColumnID.values(), data.generatedWrecks);
		}
	}
	
}
