package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Dimension;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Coord3;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.GeneratedWreck;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Rotation;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;

class GeneratedWrecksPanel extends JSplitPane
{
	private static final long serialVersionUID = 5162885598069310200L;
	
	GeneratedWrecksPanel(PlanetCrafterSaveGameViewer main, Data data, MapPanel mapPanel)
	{
		super(JSplitPane.VERTICAL_SPLIT, true);
		
		WorldObjectsPanel worldObjsGeneratedPanel = new WorldObjectsPanel(main, new WorldObject[0], mapPanel);
		WorldObjectsPanel worldObjsDroppedPanel = new WorldObjectsPanel(main, new WorldObject[0], mapPanel);
		
		JTabbedPane objectListTablesPanel = new JTabbedPane();
		objectListTablesPanel.addTab("woIdsGenerated", worldObjsGeneratedPanel);
		objectListTablesPanel.addTab("woIdsDropped"  , worldObjsDroppedPanel  );
		
		GeneratedWrecksTablePanel wrecksTablePanel = new GeneratedWrecksTablePanel(data, wreck -> {
			worldObjsGeneratedPanel.setData(wreck.worldObjsGenerated);
			worldObjsDroppedPanel  .setData(wreck.worldObjsDropped  );
		});
		
		setTopComponent(wrecksTablePanel);
		setBottomComponent(objectListTablesPanel);
	}
	
	static class GeneratedWrecksTablePanel extends TablePanelWithTextArea<GeneratedWreck, GeneratedWrecksTableModel.ColumnID, GeneratedWrecksTableModel>
	{
		private static final long serialVersionUID = 3538383643672212291L;
		private Consumer<GeneratedWreck> tableSelectionChanged;
		
		GeneratedWrecksTablePanel(Data data, Consumer<GeneratedWreck> tableSelectionChanged) {
			super(new GeneratedWrecksTableModel(data), true, LayoutPos.Right, new Dimension(300, 200));
			this.tableSelectionChanged = tableSelectionChanged;
			table.setPreferredScrollableViewportSize(table.getMinimumSize());
		}

		@Override
		protected void tableSelectionChanged(GeneratedWreck row)
		{
			tableSelectionChanged.accept(row);
			super.tableSelectionChanged(row);
		}
	}
	
	static class GeneratedWrecksTableModel
			extends Tables.SimpleGetValueTableModel<GeneratedWreck, GeneratedWrecksTableModel.ColumnID>
			implements TablePanelWithTextArea.TableModelExtension<GeneratedWreck>
	{
		// Column Widths: [45, 75, 70, 50, 35, 200, 35, 205, 120, 100, 90] in ModelOrder
		enum ColumnID implements Tables.SimpleGetValueTableModel.ColumnIDTypeInt<GeneratedWreck> {
			index             ("index"            , Long    .class,  45, row -> row.index            ),
			owner             ("owner"            , Long    .class,  75, row -> row.owner            ),
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

		@Override
		public String getRowText(GeneratedWreck row, int rowIndex)
		{
			// TODO Auto-generated method stub
			return "t.b.d. (%d)".formatted(rowIndex);
		}
	}
	
}
