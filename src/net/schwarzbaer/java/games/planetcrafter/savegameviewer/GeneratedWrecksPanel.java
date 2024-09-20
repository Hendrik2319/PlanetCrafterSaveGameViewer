package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Dimension;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Coord3;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.GeneratedWreck;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Rotation;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectTypeClass;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.lib.gui.ValueListOutput;

class GeneratedWrecksPanel extends JSplitPane
{
	private static final long serialVersionUID = 5162885598069310200L;
	
	GeneratedWrecksPanel(PlanetCrafterSaveGameViewer main, Data data, MapPanel mapPanel)
	{
		super(JSplitPane.VERTICAL_SPLIT, true);
		
		WorldObjectsPanel worldObjsGeneratedPanel = new WorldObjectsPanel(main, new WorldObject[0], mapPanel);
		WorldObjectsPanel worldObjsDroppedPanel   = new WorldObjectsPanel(main, new WorldObject[0], mapPanel);
		
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
		static final Comparator<ObjectType> COMPARATOR_OBJECTTYPE = Comparator
				.<ObjectType,Integer>comparing(GeneratedWrecksTableModel::getObjectTypeClassOrder)
				.thenComparing(ot->ot.getName(),PlanetCrafterSaveGameViewer.STRING_COMPARATOR__IGNORING_CASE);
		
		private static int getObjectTypeClassOrder(ObjectType ot)
		{
			if (ot==null || ot.class_==null)
				return Integer.MAX_VALUE;
			
			switch (ot.class_)
			{
			case Special_Wreckage_important : return 1;
			case Special_Wreckage : return 2;
			default: return 100;
			}
		}
		
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
		
		private static class WreckageGroup
		{
			@SuppressWarnings("unused")
			final ObjectType objectType;
			final List<ContainerValues> containerValues;
			int nonContainers;
			
			WreckageGroup(ObjectType objectType)
			{
				this.objectType = objectType;
				containerValues = new Vector<>();
				nonContainers = 0;
			}
			
			private record ContainerValues(int amount, long size) {}
		}

		@Override
		public String getRowText(GeneratedWreck wreck, int rowIndex)
		{
			if (wreck.worldObjsGenerated==null)
				return "<no data>";
			
			Map<ObjectType,WreckageGroup> wreckage = new HashMap<>();
			Map<ObjectType,Integer> storedObjects = new HashMap<>();
			
			for (WorldObject wreckageObj : wreck.worldObjsGenerated) {
				ObjectType objectType = wreckageObj.objectType;
				if (objectType==null)
					continue;
				
				if (objectType.class_==ObjectTypeClass.Special_Wreckage_important || objectType.class_==ObjectTypeClass.Special_Wreckage)
				{
					WreckageGroup group = wreckage.computeIfAbsent(objectType, WreckageGroup::new);
					if (wreckageObj.list!=null)
					{
						long size = wreckageObj.list.size;
						int amount = wreckageObj.list.worldObjs==null ? 0 :wreckageObj.list.worldObjs.length;
						group.containerValues.add(new WreckageGroup.ContainerValues(amount, size));
					} else
						group.nonContainers++;
				}
				else if (wreckageObj.container!=null)
				{
					int n = storedObjects.computeIfAbsent(objectType, ot->0);
					storedObjects.put(objectType, n+1);
				}
			}
			
			ValueListOutput out = new ValueListOutput();
			
			if (!wreckage.isEmpty())
			{
				out.add(0, "Wreckage");
				Vector<ObjectType> keys = new Vector<>( wreckage.keySet() );
				keys.sort(COMPARATOR_OBJECTTYPE);
				for (ObjectType ot : keys)
				{
					WreckageGroup group = wreckage.get(ot);
					out.add(1, "%3dx %s".formatted( group.containerValues.size() + group.nonContainers, ot.getName() ));
					if (!group.containerValues.isEmpty())
					{
						group.containerValues.forEach(cv -> {
							out.add(2, "%d of %d items".formatted(cv.amount, cv.size));
						});
						if (group.nonContainers>0)
							out.add(2, "%d x Non Containers".formatted(group.nonContainers));
					}
				}
			}
			
			if (!storedObjects.isEmpty())
			{
				out.add(0, "Stored Objects");
				Vector<ObjectType> keys = new Vector<>( storedObjects.keySet() );
				keys.sort(COMPARATOR_OBJECTTYPE);
				for (ObjectType ot : keys)
					out.add(1, "%3dx %s".formatted( storedObjects.get(ot), ot.getName() ));
			}
			
			return out.generateOutput();
		}
	}
	
}
