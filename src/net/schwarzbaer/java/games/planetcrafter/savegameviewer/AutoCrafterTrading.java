package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectTypeClassClass;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectTypeValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeEvent;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeListener;
import net.schwarzbaer.java.lib.gui.GeneralIcons.GrayCommandIcons;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.Tables;

class AutoCrafterTrading implements ObjectTypesChangeListener
{
	private final File datafile;
	private final HashMap<String,AutoCrafterTradingItem> data;
	private final ObjectTypes objectTypes;
	private AutoCrafterTradingPanel panel;
	private final RecipeEditorDialog recipeEditorDialog;

	AutoCrafterTrading(ObjectTypes objectTypes, File datafile, Window mainWindow)
	{
		this.objectTypes = objectTypes;
		this.datafile = datafile;
		data = new HashMap<>();
		panel = null;
		recipeEditorDialog = new RecipeEditorDialog(mainWindow);
	}
	
	Component createNewPanel()
	{
		return panel = new AutoCrafterTradingPanel();
	}

	private AutoCrafterTradingItem getItem(ObjectType ot)
	{
		return ot==null ? null : data.get(ot.id);
	}

	private ObjectType chooseObjectType(
			Component parent,
			Predicate<ObjectType> predicate,
			String dlgTitleIfNothingFound,
			String dlgMessageIfNothingFound,
			String dlgTitleOfSelectDialog, String dlgMessageOfSelectDialog
	) {
		Vector<ObjectType> types = objectTypes.collectTypes(predicate);
		
		if (types.isEmpty())
		{
			JOptionPane.showMessageDialog(parent, dlgMessageIfNothingFound, dlgTitleIfNothingFound, JOptionPane.INFORMATION_MESSAGE);
			return null;
		}
		
		types.sort(Comparator.comparing(ObjectType::getName, Data.caseIgnoringComparator));
		ObjectTypeInGUI[] values = types.stream().map(ObjectTypeInGUI::new).toArray(ObjectTypeInGUI[]::new);
		Object result = JOptionPane.showInputDialog(parent, dlgMessageOfSelectDialog, dlgTitleOfSelectDialog, JOptionPane.QUESTION_MESSAGE, null, values, null);
		if (result instanceof ObjectTypeInGUI) // &&  !=null
			return ((ObjectTypeInGUI) result).ot;
		
		return null;
	}

	@Override
	public void objectTypesChanged(ObjectTypesChangeEvent event)
	{
		if (event.eventType == ObjectTypesChangeEvent.EventType.ValueChanged && event.changedValue == ObjectTypeValue.Label)
		{
			if (panel!=null)
				panel.tableModel.updateContent();
		}
	}

	void readFromFile()
	{
		System.out.printf("Read AutoCrafterTrading data from file \"%s\" ...%n", datafile.getAbsolutePath());
		data.clear();
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(datafile), StandardCharsets.UTF_8)))
		{
			String line, valueStr;
			AutoCrafterTradingItem item = null;
			
			while ( (line=in.readLine())!=null )
			{
				
				if (line.isEmpty()) continue;
				if ( (valueStr=getValue(line,"ObjectType: "))!=null )
				{
					item = null;
					String objectTypeID = valueStr;
					ObjectType ot = objectTypes.get(objectTypeID);
					if (ot!=null)
						data.put(objectTypeID, item = new AutoCrafterTradingItem(ot));
				}
				
				if (item!=null)
				{
					
					if ( (valueStr=getValue(line,"TerraTokens = "))!=null )
					{
						try {
							item.terraTokens = Long.parseLong(valueStr);
						}
						catch (NumberFormatException ex)
						{
							System.err.printf("Can't parse Long in line \"%s\": %s%n", line, ex.getMessage());
							item.terraTokens = null;
						}
					}
					
					if ( line.equals("Minable") )
					{
						item.isMinable = true;
					}
					
					if ( (valueStr=getValue(line,"Recipe = "))!=null )
					{
						String objectTypeID = valueStr;
						ObjectType ot = objectTypes.get(objectTypeID);
						if (ot!=null)
							item.recipe.add(ot);
					}
					
				}
				
			}
			
		}
		catch (FileNotFoundException ex) {}
		catch (IOException ex)
		{
			System.err.printf("IOException while reading AutoCrafterTrading data: %s%n", ex.getMessage());
			// ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}

	private static String getValue(String line, String prefix) {
		if (line.startsWith(prefix))
			return line.substring(prefix.length());
		return null;
	}

	void writeToFile()
	{
		System.out.printf("Write AutoCrafterTrading data to file \"%s\" ...%n", datafile.getAbsolutePath());
		
		try (PrintWriter out = new PrintWriter(datafile, StandardCharsets.UTF_8)) {
			
			Vector<Entry<String, AutoCrafterTradingItem>> vector = new Vector<>(data.entrySet());
			vector.sort(Comparator.comparing(Entry<String, AutoCrafterTradingItem>::getKey,Data.caseIgnoringComparator));
			
			for (Entry<String, AutoCrafterTradingItem> entry : vector) {
				AutoCrafterTradingItem act = entry.getValue();
				
				out.printf("ObjectType: %s%n", entry.getKey());
				
				if (act.terraTokens!=null) out.printf("TerraTokens = %d%n", act.terraTokens);
				if (act.isMinable        ) out.printf("Minable%n");
				
				for (ObjectType recipeItem : act.recipe)
					if (recipeItem!=null)
						out.printf("Recipe = %s%n", recipeItem.id);
				
				out.println();
			}
			
		} catch (IOException ex) {
			System.err.printf("IOException while writing AutoCrafterTrading data: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}

	private record ObjectTypeInGUI(ObjectType ot)
	{
		@Override public String toString()
		{
			return ot.getName();
		}
	}

	private static class AutoCrafterTradingItem
	{
		final ObjectType objectType;
		Long terraTokens;
		boolean isMinable;
		final Vector<ObjectType> recipe;
		
		AutoCrafterTradingItem(ObjectType objectType)
		{
			this.objectType = Objects.requireNonNull(objectType);
			this.terraTokens = null;
			this.isMinable = false;
			this.recipe = new Vector<>();
		}
		
		boolean recipeIsMissing()
		{
			return !isMinable && recipe.isEmpty();
		}
	
		static class DummyItem extends AutoCrafterTradingItem
		{
			final String reason;
			DummyItem(ObjectType objectType, String reason) { super(objectType); this.reason = reason; }
		}
	}
	
	private class RecipeEditorDialog extends StandardDialog
	{
		private static final long serialVersionUID = -3714840866276417932L;
		
		private final JLabel labelTaskText;
		private final ListModelImpl listModel;
		private final JList<ObjectTypeInGUI> list;
		
		private AutoCrafterTradingItem currentItem;
		private boolean wasChanged;
		private int selectedIndex;
		private ObjectTypeInGUI selectedValue;

		public RecipeEditorDialog(Window parent)
		{
			super(parent, "Recipe", ModalityType.APPLICATION_MODAL, true);
			currentItem = null;
			wasChanged = false;
			selectedIndex = -1;
			selectedValue = null;
			
			labelTaskText = new JLabel("<task text>");
			
			ButtonPanel buttonPanel = new ButtonPanel();
			
			list = new JList<>(listModel = new ListModelImpl());
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			list.getSelectionModel().addListSelectionListener(e->{
				selectedValue = list.getSelectedValue();
				selectedIndex = list.getSelectedIndex();
				buttonPanel.updateButtons();
			});
			JScrollPane listScrollPane = new JScrollPane(list);
			
			JPanel contentPane = new JPanel(new BorderLayout());
			contentPane.add(labelTaskText, BorderLayout.NORTH);
			contentPane.add(buttonPanel, BorderLayout.EAST);
			contentPane.add(listScrollPane, BorderLayout.CENTER);
			
			createGUI(contentPane,
					GUI.createButton("Ok", true, e->{
						currentItem.recipe.clear();
						List<ObjectType> otList = listModel.data.stream().map(ObjectTypeInGUI::ot).toList();
						currentItem.recipe.addAll(otList);
						wasChanged = true;
						closeDialog();
					}),
					GUI.createButton("Cancel", true, e->{
						closeDialog();
					})
			);
			
			buttonPanel.updateButtons();
		}
		
		private void configureDialog(AutoCrafterTradingItem currentItem)
		{
			this.currentItem = Objects.requireNonNull(currentItem);
			labelTaskText.setText(String.format("Set recipe for resource \"%s\":", currentItem.objectType.getName()));
			listModel.replaceData(currentItem.recipe);
		}

		public boolean showDialog(AutoCrafterTradingItem currentItem)
		{
			configureDialog(currentItem);
			wasChanged = false;
			showDialog(Position.PARENT_CENTER);
			return wasChanged;
		}
		
		private class ButtonPanel extends JPanel
		{
			private static final long serialVersionUID = 2924076421566808320L;
			
			private final JButton btnAdd      ;
			private final JButton btnDublicate;
			private final JButton btnRemove   ;
			private final JButton btnMoveUp   ;
			private final JButton btnMoveDown ;

			ButtonPanel()
			{
				super(new GridLayout(0, 1));
				add(btnAdd = GUI.createButton("Add", GrayCommandIcons.IconGroup.Add, true, e->{
					ObjectType result = chooseObjectType(
						RecipeEditorDialog.this,
						ot -> {
							if (ot==null) return false;
							if (ot.class_==null) return false;
							return (ot.class_.class_==ObjectTypeClassClass.Resource);
						},
						"No suitable ObjectTypes",
						"Sorry, there are no suitable ObjectTypes that are usable as resource.",
						"Select ObjectType",
						"Select an ObjectType as base of the new item :"
					);
					if (result!=null)
					{
						int newIndex = listModel.add(result);
						if (newIndex>=0) list.setSelectedIndex(newIndex);
					}
				}));
				add(btnDublicate = GUI.createButton("Dublicate", GrayCommandIcons.IconGroup.Delete, true, e->{
					if (selectedValue!=null)
					{
						int newIndex = listModel.add(selectedValue.ot);
						if (newIndex>=0) list.setSelectedIndex(newIndex);
					}
				}));
				add(btnRemove = GUI.createButton("Remove", GrayCommandIcons.IconGroup.Delete, true, e->{
					if (listModel.isInRange(selectedIndex))
					{
						listModel.remove(selectedIndex);
						list.clearSelection();
					}
				}));
				add(btnMoveUp = GUI.createButton("Up", GrayCommandIcons.IconGroup.Up, true, e->{
					if (listModel.canMoveUp(selectedIndex))
					{
						int newIndex = listModel.moveUp(selectedIndex);
						if (newIndex>=0) list.setSelectedIndex(newIndex);
					}
				}));
				add(btnMoveDown = GUI.createButton("Down", GrayCommandIcons.IconGroup.Down, true, e->{
					if (listModel.canMoveDown(selectedIndex))
					{
						int newIndex = listModel.moveDown(selectedIndex);
						if (newIndex>=0) list.setSelectedIndex(newIndex);
					}
				}));
			}
			
			void updateButtons()
			{
				btnAdd      .setEnabled(true);
				btnDublicate.setEnabled(selectedValue!=null);
				btnRemove   .setEnabled(listModel.isInRange(selectedIndex));
				btnMoveUp   .setEnabled(listModel.canMoveUp(selectedIndex));
				btnMoveDown .setEnabled(listModel.canMoveDown(selectedIndex));
			}
		}
		
		private static class ListModelImpl implements ListModel<ObjectTypeInGUI>
		{
			private final Vector<ListDataListener> listeners = new Vector<>();
			private final Vector<ObjectTypeInGUI> data = new Vector<>();
			
			void replaceData(Vector<ObjectType> recipe)
			{
				List<ObjectTypeInGUI> list = recipe.stream().map(ObjectTypeInGUI::new).toList();
				int oldSize = getSize();
				data.clear();
				data.addAll(list);
				fireContentsChanged(this, 0, Math.max(oldSize, getSize()));
			}
			
			boolean isInRange  (int index) { return 0 <= index && index   < data.size(); }
			boolean canMoveDown(int index) { return 0 <= index && index+1 < data.size(); }
			boolean canMoveUp  (int index) { return 0 <  index && index   < data.size(); }

			int moveUp(int index)
			{
				if (index>=data.size()) return -1;
				if (index<=0) return -1;
				ObjectTypeInGUI value = data.get(index);
				data.removeElementAt(index);
				data.insertElementAt(value, index-1);
				fireContentsChanged(this, index-1, index);
				return index-1;
			}
			
			int moveDown(int index)
			{
				if (index+1>=data.size()) return -1;
				if (index<0) return -1;
				ObjectTypeInGUI value = data.get(index);
				data.removeElementAt(index);
				data.insertElementAt(value, index+1);
				fireContentsChanged(this, index, index+1);
				return index+1;
			}
			
			int add(ObjectType ot)
			{
				data.add(new ObjectTypeInGUI(ot));
				fireIntervalAdded(this, data.size()-1, data.size()-1);
				return data.size()-1;
			}
			
			void remove(int index)
			{
				if (index>=data.size()) return;
				if (index<0) return;
				data.removeElementAt(index);
				fireIntervalRemoved(this, index, index);
			}

			@Override public int getSize() { return data.size(); }
			@Override public ObjectTypeInGUI getElementAt(int index) { return data.get(index); }
			@Override public void    addListDataListener(ListDataListener l) { listeners.   add(l); }
			@Override public void removeListDataListener(ListDataListener l) { listeners.remove(l); }
			
			void fireContentsChanged(Object source, int startIndex, int endIndex) {
				ListDataEvent e = new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED, startIndex, endIndex);
				for (ListDataListener l : listeners) l.contentsChanged(e);
			}
			void fireIntervalAdded(Object source, int startIndex, int endIndex) {
				ListDataEvent e = new ListDataEvent(source, ListDataEvent.INTERVAL_ADDED, startIndex, endIndex);
				for (ListDataListener l : listeners) l.intervalAdded(e);
			}
			void fireIntervalRemoved(Object source, int startIndex, int endIndex) {
				ListDataEvent e = new ListDataEvent(source, ListDataEvent.INTERVAL_ADDED, startIndex, endIndex);
				for (ListDataListener l : listeners) l.intervalRemoved(e);
			}
		}
	}

	private class AutoCrafterTradingPanel extends JSplitPane
	{
		private static final long serialVersionUID = 4347662941011335267L;

		private final JTable table;
		private final AutoCrafterTradingTableModel tableModel;
		private final TableToolBar tableToolBar;
		private AutoCrafterTradingItem selectedItem;
		
		AutoCrafterTradingPanel() {
			super(JSplitPane.HORIZONTAL_SPLIT, true);
			setResizeWeight(1);
			
			selectedItem = null;
			
			tableModel = new AutoCrafterTradingTableModel();
			table = new JTable(tableModel);
			table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			
			tableModel.setTable(table);
			tableModel.setColumnWidths(table);
			tableModel.setDefaultCellEditorsAndRenderers();
			
			JScrollPane tableScrollPane = new JScrollPane(table);
			
			JPanel tablePanel = new JPanel(new BorderLayout());
			tablePanel.add(tableScrollPane, BorderLayout.CENTER);
			tablePanel.add(tableToolBar = new TableToolBar(), BorderLayout.PAGE_START);
			
			JTree tree = new JTree(new DefaultTreeModel(null));
			tree.setCellRenderer(new AutoCrafterTradingTreeCellRenderer());
			JScrollPane treeScrollPane = new JScrollPane(tree);
			
			tablePanel    .setPreferredSize(new Dimension(100,100));
			treeScrollPane.setPreferredSize(new Dimension(100,100));
			
			setTopComponent(tablePanel);
			setBottomComponent(treeScrollPane);
			
			table.getSelectionModel().addListSelectionListener(ev -> {
				int rowV = table.getSelectedRow();
				int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
				selectedItem = tableModel.getRow(rowM);
				tableToolBar.updateToolBar();
				
				DefaultTreeModel treeModel;
				if (selectedItem == null)
					treeModel = new DefaultTreeModel(null);
				else
					treeModel = new DefaultTreeModel(new AutoCrafterTradingTreeNode(null, selectedItem, AutoCrafterTrading.this::getItem), true);
				
				tree.setModel(treeModel);
			});
		}
		
		private class TableToolBar extends JToolBar
		{
			private static final long serialVersionUID = 5386079318563349840L;
			private final JButton btnNewRow;
			private final JButton btnSetRecipe;
		
			TableToolBar()
			{
				add(btnNewRow = GUI.createButton("New Row", GrayCommandIcons.IconGroup.Add, true, e->addNewRow()));
				add(btnSetRecipe = GUI.createButton("Set Recipe", true, e->editRecipe()));
				addSeparator();
				add(GUI.createButton("Show Column Widths", true, e->{
					System.out.printf("Column Widths: %s%n", Tables.SimplifiedTableModel.getColumnWidthsAsString(table));
				}));
			}

			void updateToolBar()
			{
				btnNewRow   .setEnabled(true);
				btnSetRecipe.setEnabled(selectedItem!=null);
				btnSetRecipe.setText(
					selectedItem==null || selectedItem.recipe.isEmpty()
						? "Set Recipe"
						: "Change Recipe"
				);
			}

			private void editRecipe()
			{
				if (selectedItem==null) return;
				boolean wasChanged = recipeEditorDialog.showDialog(selectedItem);
				if (wasChanged)
				{
					tableModel.fireTableColumnUpdate(AutoCrafterTradingTableModel.ColumnID.Recipe);
					writeToFile();
				}
			}
			
			private void addNewRow()
			{
				ObjectType result = chooseObjectType(
					panel,
					ot -> {
						if (ot==null) return false;
						if (data.containsKey(ot.id)) return false;
						if (ot.class_==null) return false;
						return (ot.class_.class_==ObjectTypeClassClass.Resource);
					},
					"No suitable ObjectTypes",
					"Sorry, there are no suitable ObjectTypes that are usable as resource.",
					"Select ObjectType",
					"Select an ObjectType as base of the new item :"
				);
				
				if (result!=null)
				{
					data.put(result.id, new AutoCrafterTradingItem(result));
					tableModel.updateContent();
					AutoCrafterTrading.this.writeToFile();
				}
			}
		}
	}
	
	private static class AutoCrafterTradingTableCellRenderer implements TableCellRenderer
	{
		private static final Color BGCOLOR_UNSET_DATA    = new Color(0xFFCBCB);
		private static final Color BGCOLOR_SELLABLE_ITEM = new Color(0xFFF27F);
		private final Tables.CheckBoxRendererComponent checkBox;
		private final Tables.LabelRendererComponent label;
		private final AutoCrafterTradingTableModel tableModel;

		AutoCrafterTradingTableCellRenderer(AutoCrafterTradingTableModel tableModel)
		{
			this.tableModel = tableModel;
			checkBox = new Tables.CheckBoxRendererComponent();
			checkBox.setHorizontalAlignment(SwingConstants.CENTER);
			label    = new Tables.LabelRendererComponent();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV)
		{
			Component rendererComp;
			
			int columnM = columnV<0 ? -1 : table.convertColumnIndexToModel(columnV);
			int rowM    = rowV   <0 ? -1 : table.convertRowIndexToModel(rowV);
			AutoCrafterTradingItem item = tableModel.getRow(rowM);
			AutoCrafterTradingTableModel.ColumnID columnID = tableModel.getColumnID(columnM);
			
			Supplier<Color> getCustomBackground = ()->{
				if (item!=null)
				{
					if (item.recipeIsMissing())
						return BGCOLOR_UNSET_DATA;
					if (item.terraTokens!=null)
						return BGCOLOR_SELLABLE_ITEM;
				}
				return null;
			};
			
			if (value instanceof Boolean)
			{
				boolean isChecked = (Boolean) value;
				rendererComp = checkBox;
				checkBox.configureAsTableCellRendererComponent(table, isChecked, null, isSelected, hasFocus, null, getCustomBackground);
			}
			else
			{
				rendererComp = label;
				String valueStr;
				if (columnID==AutoCrafterTradingTableModel.ColumnID.Recipe && item!=null && item.recipeIsMissing())
					valueStr = "-- is missing --";
				else if (columnID!=null && columnID.toString!=null)
					valueStr = columnID.toString.apply(value);
				else
					valueStr = value==null ? null : value.toString();
				
				if (Number.class.isInstance(value))
					label.setHorizontalAlignment(SwingConstants.RIGHT);
				else
					label.setHorizontalAlignment(SwingConstants.LEFT);
				
				label.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus, getCustomBackground, null);
			}
			
			return rendererComp;
		}
		
	}
	
	private class AutoCrafterTradingTableModel extends Tables.SimpleGetValueTableModel<AutoCrafterTradingItem, AutoCrafterTradingTableModel.ColumnID>
	{
		private enum ColumnID implements Tables.SimpleGetValueTableModel.ColumnIDTypeInt<AutoCrafterTradingItem>
		{
			// Column Widths: [93, 116, 90, 57, 776] in ModelOrder
			Resource  ("Resource"   , String                     .class, 100, row->row.objectType.getName()),
			Class     ("Class"      , ObjectTypes.ObjectTypeClass.class, 180, row->row.objectType.class_   ),
			Price     ("Price"      , Long                       .class,  90, row->row.terraTokens         , Data.AchievedValues::formatTerraTokens),
			IsMinable ("Is Minable?", Boolean                    .class,  70, row->row.isMinable           ),
			Recipe    ("Recipe"     , String                     .class, 700, row->toString(row.recipe)    ),
			;
			private final Tables.SimplifiedColumnConfig cfg;
			private final Function<AutoCrafterTradingItem, ?> getValue;
			private final Function<Object,String> toString;
			
			private <ColumnType> ColumnID(String name, Class<ColumnType> columnClass, int width, Function<AutoCrafterTradingItem, ColumnType> getValue)
			{
				this(name, columnClass, width, getValue, null);
			}
			private <ColumnType> ColumnID(String name, Class<ColumnType> columnClass, int width, Function<AutoCrafterTradingItem, ColumnType> getValue, Function<ColumnType,String> toString)
			{
				this.getValue = getValue;
				this.cfg = new Tables.SimplifiedColumnConfig(name, columnClass, 20, -1, width, width);
				this.toString = toString==null ? null : obj -> {
					if (columnClass.isInstance(obj))
						return toString.apply(columnClass.cast(obj));
					return obj==null ? null : obj.toString();
				};
			}

			@Override public Tables.SimplifiedColumnConfig getColumnConfig() { return cfg; }
			@Override public Function<AutoCrafterTradingItem, ?> getGetValue() { return getValue; }

			private static String toString(Vector<ObjectType> recipe)
			{
				Iterable<String> it = ()->recipe.stream().map(ObjectType::getName).iterator();
				return String.join(", ", it);
			}
		}

		AutoCrafterTradingTableModel()
		{
			super(ColumnID.values());
			updateContent();
		}

		void updateContent()
		{
			setData(makeRows(data));
		}

		private Vector<AutoCrafterTradingItem> makeRows(HashMap<String, AutoCrafterTradingItem> data)
		{
			Vector<AutoCrafterTradingItem> vector = new Vector<>(data.values());
			vector.sort(Comparator.<AutoCrafterTradingItem,String>comparing(act -> act.objectType.getName(), Data.caseIgnoringComparator));
			return vector;
		}

		@Override
		public void setDefaultCellEditorsAndRenderers()
		{
			AutoCrafterTradingTableCellRenderer tcr = new AutoCrafterTradingTableCellRenderer(this);
			setAllDefaultRenderers(class_ -> tcr);
		}

		@Override
		protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID)
		{
			if (columnID==null) return false;
			switch (columnID)
			{
				case Resource:
				case Class:
					return false;
					
				case Price:
				case IsMinable:
					return true;
					
				case Recipe:
					return false;
			}
			return false;
		}

		@Override
		protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID)
		{
			if (columnID==null) return;
			AutoCrafterTradingItem row = getRow(rowIndex);
			if (row==null) return;
			
			switch (columnID)
			{
				case Resource : break;
				case Class    : break;
				case Price    : row.terraTokens = (Long   )aValue; break;
				case IsMinable: row.isMinable   = (Boolean)aValue; break;
				case Recipe   : break;
			}
			
			writeToFile();
		}
		
	}

	private static class AutoCrafterTradingTreeCellRenderer extends DefaultTreeCellRenderer
	{
		private static final long serialVersionUID = 5770385389024005077L;
		private static final Color COLOR_UNDEFINED_ITEM    = Color.RED;
		private static final Color COLOR_RECIPE_IS_MISSING = Color.MAGENTA;
		private static final Color COLOR_SELLABLE          = new Color(0x00B1FF);
		private static final Color COLOR_MINABLE           = new Color(0x00CF00);

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean isExpanded, boolean leaf, int row, boolean hasFocus)
		{
			Component rendererComponent = super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded, leaf, row, hasFocus);
			
			if (value instanceof AutoCrafterTradingTreeNode)
			{
				AutoCrafterTradingTreeNode treeNode = (AutoCrafterTradingTreeNode) value;
				if (treeNode.item instanceof AutoCrafterTradingItem.DummyItem)
					setForeground(COLOR_UNDEFINED_ITEM);
				
				else if (treeNode.item.recipeIsMissing())
					setForeground(COLOR_RECIPE_IS_MISSING);
				
				else if (treeNode.item.terraTokens!=null)
					setForeground(COLOR_SELLABLE);
				
				else if (treeNode.item.isMinable)
					setForeground(COLOR_MINABLE);
			}
			
			return rendererComponent;
		}
		
	}

	private static class AutoCrafterTradingTreeNode extends AbstractTreeNode<AutoCrafterTradingTreeNode,AutoCrafterTradingTreeNode>
	{
		private final AutoCrafterTradingItem item;
		private final Function<ObjectType, AutoCrafterTradingItem> getItem;
		
		AutoCrafterTradingTreeNode(AutoCrafterTradingTreeNode parent, AutoCrafterTradingItem item, Function<ObjectType,AutoCrafterTradingItem> getItem)
		{
			super(parent, true);
			this.getItem = getItem;
			this.item = Objects.requireNonNull(item);
		}
		
		@Override public String toString()
		{
			String str = item.objectType.getName();
			
			if (item instanceof AutoCrafterTradingItem.DummyItem)
				str += String.format(" !! %s !!", ((AutoCrafterTradingItem.DummyItem) item).reason);
			else
			{
				if (item.terraTokens!=null)
					str += String.format(" (%s)", Data.AchievedValues.formatTerraTokens(item.terraTokens));
				if (item.recipeIsMissing())
					str += " (Recipe is missing)";
			}
			
			return str;
		}

		@Override void prepareChildren()
		{
			if (children == null)
			{
				children = new Vector<>();
				if (!item.isMinable)
				{
					for (ObjectType resource : item.recipe)
					{
						AutoCrafterTradingItem childItem = getItem.apply(resource);
						if (childItem==null) childItem = new AutoCrafterTradingItem.DummyItem(resource, "undefined");
						children.add(new AutoCrafterTradingTreeNode(this, childItem, getItem));
					}
				}
			}
		}
	}

	private static abstract class AbstractTreeNode<ParentType extends TreeNode, ChildType extends TreeNode> implements TreeNode
	{
		protected final ParentType parent;
		protected Vector<ChildType> children;
		private final boolean allowsChildren;
	
		AbstractTreeNode(ParentType parent, boolean allowsChildren)
		{
			this.parent = parent;
			this.allowsChildren = allowsChildren;
			children = null;
		}
		
		abstract void prepareChildren();
		@Override public abstract String toString();

		@Override
		public ChildType getChildAt(int childIndex)
		{
			prepareChildren();
			if (childIndex <  0) return null;
			if (childIndex >= children.size()) return null;
			return children.get(childIndex);
		}
	
		@Override public ParentType getParent() { return parent; }
		@Override public int getChildCount() { prepareChildren(); return children.size(); }
		@Override public int getIndex(TreeNode node) { prepareChildren(); return children.indexOf(node); }
		@Override public boolean getAllowsChildren() { return allowsChildren; }
		@Override public boolean isLeaf() { prepareChildren(); return children.isEmpty(); }
		@Override public Enumeration<ChildType> children() { prepareChildren(); return children.elements(); }
	}
}
