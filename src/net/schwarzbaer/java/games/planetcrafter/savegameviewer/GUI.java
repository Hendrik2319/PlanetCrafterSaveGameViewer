package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.HSColorChooser;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.TerraformingStates;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;

class GUI {
	static final Color COLOR_Removal_ByData = new Color(0xFFD5D5);
	static final Color COLOR_Removal_ByUser = new Color(0xFF7F7F);
	static final Color COLOR_Removal_Partially = new Color(0xFFD5D5);
	static final Color COLOR_Removal_Fully     = new Color(0xFF7F7F);

	static JButton createButton(String title, boolean isEnabled, ActionListener al) {
		JButton comp = new JButton(title);
		if (al!=null) comp.addActionListener(al);
		comp.setEnabled(isEnabled);
		return comp;
	}

	static JMenuItem createMenuItem(String title, ActionListener al) {
		return createMenuItem(title, true, al);
	}

	static JMenuItem createMenuItem(String title, boolean isEnabled, ActionListener al) {
		JMenuItem comp = new JMenuItem(title);
		if (al!=null) comp.addActionListener(al);
		comp.setEnabled(isEnabled);
		return comp;
	}

	static JTextField createOutputTextField(String text) {
		JTextField comp = new JTextField(text);
		comp.setEditable(false);
		return comp;
	}

	static JTextField createOutputTextField(String text, int size) {
		JTextField comp = new JTextField(text,size);
		comp.setEditable(false);
		return comp;
	}

	static JTextField createOutputTextField(String text, int size, int horizontalAlignment) {
		JTextField comp = createOutputTextField(text,size);
		comp.setHorizontalAlignment(horizontalAlignment);
		return comp;
	}
	
	static class ColorTCE extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = -6683601147557918947L;
		
		private final ColorTCR renderer;
		private final HSColorChooser.ColorDialog hsColorChooserDialog;
		private Color result;
		
		ColorTCE(Window window, ColorTCR renderer) {
			this.renderer = renderer;
			hsColorChooserDialog = new HSColorChooser.ColorDialog(window, "title", Color.GRAY);
			result = null;
		}

		@Override public boolean shouldSelectCell(EventObject anEvent) {
			return true;
		}

		@Override public Object getCellEditorValue() {
			return result;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowV, int columnV) {
			Component rendererComponent = renderer.getTableCellRendererComponent(table, value, isSelected, true, rowV, columnV);
			
			new Thread(()->{
				result = null;
				Color color = MapPanel.COLOR_WORLDOBJECT_FILL;
				if (value instanceof Color) {
					color = (Color) value;
					result = color;
				}
				hsColorChooserDialog.setInitialColor(color);
				hsColorChooserDialog.showDialog(StandardDialog.Position.PARENT_CENTER);
				Color dlgResult = hsColorChooserDialog.getColor();
				if (dlgResult==null) {
					fireEditingCanceled();
				} else {
					result = dlgResult;
					fireEditingStopped();
				}
			}).start();
			
			return rendererComponent;
		}

//		@Override public void cancelCellEditing() {
//			System.err.printf("ColorTCE.cancelCellEditing [START]%n");
//			super.cancelCellEditing();
//			System.err.printf("ColorTCE.cancelCellEditing [END]%n");
//		}
//
//		@Override public boolean stopCellEditing() {
//			System.err.printf("ColorTCE.stopCellEditing [START]%n");
//			boolean b = super.stopCellEditing();
//			System.err.printf("ColorTCE.stopCellEditing [END]%n");
//			return b;
//		}
//
//		@Override
//		protected void fireEditingStopped() {
//			System.err.printf("ColorTCE.fireEditingStopped [START]%n");
//			super.fireEditingStopped();
//			System.err.printf("ColorTCE.fireEditingStopped [END]%n");
//		}
//
//		@Override
//		protected void fireEditingCanceled() {
//			System.err.printf("ColorTCE.fireEditingCanceled [START]%n");
//			super.fireEditingCanceled();
//			System.err.printf("ColorTCE.fireEditingCanceled [END]%n");
//		}
	}
	
	static class ColorTCR implements TableCellRenderer {
		
		interface SurrogateTextSource {
			String getSurrogateText(int rowM, int columnM);
		}
		
		private final Tables.ColorRendererComponent rendererComponent;
		private final SurrogateTextSource getSurrogateText;

		ColorTCR(SurrogateTextSource getSurrogateText) {
			this.getSurrogateText = getSurrogateText;
			rendererComponent = new Tables.ColorRendererComponent();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
			if (value instanceof Data.Color) {
				Data.Color color = (Data.Color) value;
				value = color.getColor();
			}
			rendererComponent.configureAsTableCellRendererComponent(table, value, isSelected, hasFocus, ()->{
				if (getSurrogateText==null) return null;
				int rowM = table.convertRowIndexToModel(rowV);
				int columnM = table.convertColumnIndexToModel(columnV);
				return getSurrogateText.getSurrogateText(rowM, columnM);
			});
			return rendererComponent;
		}
	}
	
	static class ObjectsTableContextMenu extends ContextMenu {
		private static final long serialVersionUID = 612689032221865765L;
		private int clickedRowIndex;
		private ObjectsTableRow clickedRow;
	
		ObjectsTableContextMenu(JTable table, ObjectsTableModel<? extends ObjectsTableRow, ?> tableModel) {
			add(createMenuItem("Show Column Widths", e->{
				System.out.printf("Column Widths: %s%n", Tables.SimplifiedTableModel.getColumnWidthsAsString(table));
			}));
			
			addSeparator();
			
			JMenuItem miMarkForRemoval = add(createMenuItem("Mark clicked object for removal", e->{
				if (clickedRow!=null) {
					clickedRow.markForRemoval( true );
					tableModel.fireTableRowUpdate(clickedRowIndex);
					Data.notifyAllRemoveStateListeners();
				}
			}));
			
			JMenuItem miRemoveRemovalMarker = add(createMenuItem("Remove Removal Marker from clicked object", e->{
				if (clickedRow!=null) {
					clickedRow.markForRemoval( false );
					tableModel.fireTableRowUpdate(clickedRowIndex);
					Data.notifyAllRemoveStateListeners();
				}
			}));
			
			addContextMenuInvokeListener((comp, x, y) -> {
				int rowV = table.rowAtPoint(new Point(x,y));
				clickedRowIndex = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
				clickedRow = clickedRowIndex<0 ? null : tableModel.getRow(clickedRowIndex);
				
				miMarkForRemoval     .setEnabled(clickedRow!=null && clickedRow.getRemovalState()!=ObjectsTableRow.RemovalState.Fully);
				miRemoveRemovalMarker.setEnabled(clickedRow!=null && clickedRow.getRemovalState()!=ObjectsTableRow.RemovalState.None );
				miMarkForRemoval.setText(
						clickedRow == null
							? "Mark clicked object for removal"
							: String.format("Mark %s for removal", clickedRow.getName())
				);
				miRemoveRemovalMarker.setText(
						clickedRow == null
							? "Remove Removal Marker from clicked object"
							: String.format("Remove Removal Marker from %s", clickedRow.getName())
				);
			});
			
			addTo(table);
		}
	}

	static class ObjectsTableRow {
		
		enum RemovalState { Fully, Partially, None }
		
		private final Vector<WorldObject> objects;
		        final String name;
		private       RemovalState removalState;
		
		ObjectsTableRow(String name) {
			this.name = name;
			objects = new Vector<>();
			removalState = RemovalState.None;
		}
		
		void markForRemoval(boolean b) {
			removalState = b ? RemovalState.Fully : RemovalState.None;
			for (WorldObject wo : objects)
				wo.markForRemoval(b, false);
		}
	
		RemovalState getRemovalState() {
			return removalState;
		}
	
		void updateRemoveState() {
			boolean none = true;
			boolean all = true;
			for (WorldObject wo : objects) {
				if (wo.isMarkedForRemoval()) none = false;
				else                         all  = false;
			}
			
			if (none) //  || (none && all) -> no object -> None
				removalState = RemovalState.None;
			else if (all)
				removalState = RemovalState.Fully;
			else
				removalState = RemovalState.Partially;
		}
	
		void add(WorldObject wo) {
			objects.add(wo);
		}
		
		int getCount() {
			return objects.size();
		}
		
		String getName() {
			return String.format("%d %s", objects.size(), name) ;
		}
	
		static Supplier<Color> createCustomBackgroundFunction(ObjectsTableRow row) {
			return ()->{
				if (row==null) return null;
				RemovalState removalState = row.getRemovalState();
				if (removalState==RemovalState.Fully    ) return COLOR_Removal_Fully;
				if (removalState==RemovalState.Partially) return COLOR_Removal_Partially;
				return null;
			};
		}
	}

	static abstract class ObjectsTableModel<RowType extends ObjectsTableRow, ColumnID extends Tables.SimplifiedColumnIDInterface> extends Tables.SimplifiedTableModel<ColumnID> {
		
		protected final Vector<RowType> rows;
		
		ObjectsTableModel(ColumnID[] columns) {
			super(columns);
			rows = new Vector<>();
		}
		
		protected void setData(Collection<RowType> rows) {
			this.rows.clear();
			this.rows.addAll(rows);
		}

		void updateRemoveStates() {
			if (rows==null) return;
			for (ObjectsTableRow row : rows)
				row.updateRemoveState();
			table.repaint();
		}

		@Override public int getRowCount() {
			return rows==null ? 0 : rows.size();
		}

		RowType getRow(int rowIndex) {
			if (rows==null) return null;
			if (rowIndex<0) return null;
			if (rowIndex>=rows.size()) return null;
			return rows.get(rowIndex);
		}

		@Override public void fireTableRowUpdate(int rowIndex) {
			super.fireTableRowUpdate(rowIndex);
		}
	}

	static class TerraformingStatesDialog extends StandardDialog {
		private static final long serialVersionUID = -580668583006732866L;
		
		private final JButton btnOk;
		private final DoubleTextField oxygenLevel  ;
		private final DoubleTextField heatLevel    ;
		private final DoubleTextField pressureLevel;
		private final DoubleTextField plantsLevel  ;
		private final DoubleTextField insectsLevel ;
		private final DoubleTextField animalsLevel ;
		private Data.TerraformingStates results;

		private JTextField biomassLevel;

		private JTextField terraformLevel;
		
		private TerraformingStatesDialog(Window parent, String title, Vector<Data.TerraformingStates> terraformingStates) {
			super(parent, title);
			results = null;
			
			oxygenLevel    = new DoubleTextField(0.0, Data.TerraformingStates::formatOxygenLevel);
			heatLevel      = new DoubleTextField(0.0, Data.TerraformingStates::formatHeatLevel);
			pressureLevel  = new DoubleTextField(0.0, Data.TerraformingStates::formatPressureLevel);
			biomassLevel   = DoubleTextField.createFormattedValueOutput("");
			plantsLevel    = new DoubleTextField(0.0, Data.TerraformingStates::formatBiomassLevel);
			insectsLevel   = new DoubleTextField(0.0, Data.TerraformingStates::formatBiomassLevel);
			animalsLevel   = new DoubleTextField(0.0, Data.TerraformingStates::formatBiomassLevel);
			terraformLevel = DoubleTextField.createFormattedValueOutput("");
			
			final Data.TerraformingStates initialValues;
			if (!terraformingStates.isEmpty()) {
				initialValues = terraformingStates.firstElement();
				oxygenLevel  .setValue(initialValues.oxygenLevel  );
				heatLevel    .setValue(initialValues.heatLevel    );
				pressureLevel.setValue(initialValues.pressureLevel);
				plantsLevel  .setValue(initialValues.plantsLevel  );
				insectsLevel .setValue(initialValues.insectsLevel );
				animalsLevel .setValue(initialValues.animalsLevel );
			} else
				initialValues = null;
			
			JPanel contentPane = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridwidth = 1;
			
			int gridy = 0;
			addRow(contentPane, c, gridy++, "Oxygen Level"        , oxygenLevel   , initialValues==null ? null : initialValues.oxygenLevel  );
			addRow(contentPane, c, gridy++, "Heat Level"          , heatLevel     , initialValues==null ? null : initialValues.heatLevel    );
			addRow(contentPane, c, gridy++, "Pressure Level"      , pressureLevel , initialValues==null ? null : initialValues.pressureLevel);
			addRow(contentPane, c, gridy++, "Biomass Level"       , biomassLevel  );
			addRow(contentPane, c, gridy++, "Plants Level"        , plantsLevel   , initialValues==null ? null : initialValues.plantsLevel  );
			addRow(contentPane, c, gridy++, "Insects Level"       , insectsLevel  , initialValues==null ? null : initialValues.insectsLevel );
			addRow(contentPane, c, gridy++, "Animals Level"       , animalsLevel  , initialValues==null ? null : initialValues.animalsLevel );
			addRow(contentPane, c, gridy++, "Terraformation Level", terraformLevel);
			
			c.weightx = 1;
			c.weighty = 1;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			contentPane.add(new JLabel(), c);
			
			createGUI(contentPane,
					createButton("Set All to 0", true, e->{
						oxygenLevel  .setValue(0);
						heatLevel    .setValue(0);
						pressureLevel.setValue(0);
						plantsLevel  .setValue(0);
						insectsLevel .setValue(0);
						animalsLevel .setValue(0);
						updateGUI();
					}),
					initialValues==null ? null : createButton("Reset All", true, e->{
						oxygenLevel  .setValue(initialValues.oxygenLevel  );
						heatLevel    .setValue(initialValues.heatLevel    );
						pressureLevel.setValue(initialValues.pressureLevel);
						plantsLevel  .setValue(initialValues.plantsLevel  );
						insectsLevel .setValue(initialValues.insectsLevel );
						animalsLevel .setValue(initialValues.animalsLevel );
						updateGUI();
					}),
					btnOk = createButton("Ok", true, e->{
						createResult();
						if (results!=null) closeDialog();
					}),
					createButton("Cancel", true, e->{
						closeDialog();
					}));
			
			updateGUI();
		}

		private void addRow(JPanel panel, GridBagConstraints c, int gridy, String label, DoubleTextField txtField, Double initialValue) {
			int gridx = 0;
			c.gridy = gridy;
			c.weightx = 0; c.gridx = gridx++; panel.add(new JLabel(label+": "), c);
			c.weightx = 0; c.gridx = gridx++; panel.add(txtField.formattedValueOutput, c);
			c.weightx = 1; c.gridx = gridx++; panel.add(txtField, c);
			c.weightx = 0; c.gridx = gridx++; panel.add(createButton("Set 0", true, e->{
				txtField.setValue(0);
				updateGUI();
			}), c);
			if (initialValue!=null) {
				c.weightx = 0; c.gridx = gridx++; panel.add(createButton("Reset", true, e->{
					txtField.setValue(initialValue);
					updateGUI();
				}), c);
			}
		}

		private void addRow(JPanel panel, GridBagConstraints c, int gridy, String label, JTextField formattedValueOutput) {
			int gridx = 0;
			c.gridy = gridy;
			c.weightx = 0; c.gridx = gridx++; panel.add(new JLabel(label+": "), c);
			c.weightx = 0; c.gridx = gridx++; panel.add(formattedValueOutput, c);
		}

		private boolean areAllValuesOk() {
			return  oxygenLevel  .isOK() &&
					heatLevel    .isOK() &&
					pressureLevel.isOK() &&
					plantsLevel  .isOK() &&
					insectsLevel .isOK() &&
					animalsLevel .isOK();
		}

		private void createResult() {
			results = !areAllValuesOk() ? null : new Data.TerraformingStates(
					oxygenLevel  .value,
					heatLevel    .value,
					pressureLevel.value,
					plantsLevel  .value,
					insectsLevel .value,
					animalsLevel .value
					);
		}
		
		private void updateGUI() {
			boolean areAllValuesOk = areAllValuesOk();
			btnOk.setEnabled(areAllValuesOk);
			
			if (areAllValuesOk) {
				double val =
						oxygenLevel  .value+
						heatLevel    .value+
						pressureLevel.value+
						plantsLevel  .value+
						insectsLevel .value+
						animalsLevel .value;
				terraformLevel.setText(Data.TerraformingStates.formatTerraformation(val));
			}
			
			if (plantsLevel  .isOK() &&
				insectsLevel .isOK() &&
				animalsLevel .isOK()) {
				double val =
						plantsLevel  .value+
						insectsLevel .value+
						animalsLevel .value;
				biomassLevel.setText(Data.TerraformingStates.formatBiomassLevel(val));
			}
		}

		private class DoubleTextField extends JTextField {
			private static final long serialVersionUID = 7631623492689405688L;
			
			private double value;
			private boolean isOK;
			private final JTextField formattedValueOutput;
			private final Function<Double, String> getFormattedValueStr;
			private final Color defaultBG;

			DoubleTextField(double value, Function<Double,String> getFormattedValueStr) {
				super(String.format(Locale.ENGLISH, "%s", value), 20);
				this.value = value;
				this.getFormattedValueStr = getFormattedValueStr;
				defaultBG = getBackground();
				isOK = true;
				formattedValueOutput = createFormattedValueOutput(this.getFormattedValueStr.apply(this.value));
				setHorizontalAlignment(JTextField.RIGHT);
				addActionListener(e->processInput());
				addFocusListener(new FocusListener() {
					@Override public void focusGained(FocusEvent e) {}
					@Override public void focusLost(FocusEvent e) { processInput(); }
				});
			}

			static JTextField createFormattedValueOutput(String txt) {
				return createOutputTextField(txt, 10, JTextField.RIGHT);
			}

			private void processInput() {
				isOK = false;
				try {
					double newValue = Double.parseDouble(getText());
					if (!Double.isNaN(newValue))
						changeValue(newValue);
				} catch (NumberFormatException e1) { }
				setBackground(isOK ? defaultBG : Color.RED);
				updateGUI();
			}

			boolean isOK() {
				return isOK;
			}

			void setValue(double value) {
				changeValue(value);
				setText(String.format(Locale.ENGLISH, "%s", this.value));
			}

			private void changeValue(double value) {
				this.value = value;
				formattedValueOutput.setText(getFormattedValueStr.apply(this.value));
				isOK = true;
			}
			
		}

		static TerraformingStates show(Window parent, String title, Vector<TerraformingStates> terraformingStates) {
			TerraformingStatesDialog dlg = new TerraformingStatesDialog(parent, title, terraformingStates);
			dlg.showDialog();
			return dlg.results;
		}
	}

	static class ObjectTypeColorsDialog extends StandardDialog {
		private static final long serialVersionUID = 7975796985768293299L;
		
		private final PlanetCrafterSaveGameViewer main;
		private final ColorTableModel tableModel;
		private final JTable table;


		ObjectTypeColorsDialog(PlanetCrafterSaveGameViewer main, String title) {
			super(main.getMainWindow(), title);
			this.main = main;
			
			tableModel = new ColorTableModel();
			table = new JTable(tableModel);
			JScrollPane contentPane = new JScrollPane(table);
			
			table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			
			tableModel.setTable(table);
			tableModel.setColumnWidths(table);
			tableModel.setDefaultCellEditorsAndRenderers(this);
			
			Dimension size = table.getPreferredSize();
			size.width  += 30;
			size.height = 500;
			contentPane.setPreferredSize(size);
			
			createGUI(contentPane, createButton("Close", true, e->closeDialog()));
		}

		public HashMap<String, Color> showDialogAndGetColors() {
			String colorArrStr = PlanetCrafterSaveGameViewer.settings.getString(PlanetCrafterSaveGameViewer.AppSettings.ValueKey.ObjectTypeColors, "");
			//System.out.printf("ObjectTypeColorsDialog.input: %s%n", colorArrStr);
			HashMap<String, Color> colors = decodeColors(colorArrStr);
			//colors.forEach((id,color)->System.out.printf("ObjectTypeColorsDialog.input: \"%s\", %s%n", id, color));
			tableModel.setData(colors, main.getObjectTypes());
			
			showDialog();
			
			if (tableModel.wasSomethingChanged()) {
				colors = tableModel.getData();
				//colors.forEach((id,color)->System.out.printf("ObjectTypeColorsDialog.result: \"%s\", %s%n", id, color));
				colorArrStr = encodeColors(colors);
				//System.out.printf("ObjectTypeColorsDialog.result: %s%n", colorArrStr);
				PlanetCrafterSaveGameViewer.settings.putString(PlanetCrafterSaveGameViewer.AppSettings.ValueKey.ObjectTypeColors, colorArrStr);
			}
			
			return colors;
		}

		private static String encodeColors(HashMap<String, Color> colors) {
			Vector<Entry<String, Color>> entries = new Vector<>(colors.entrySet());
			entries.sort(Comparator.comparing(Entry<String, Color>::getKey));
			Iterable<String> it = ()->entries.stream().map(e->String.format("%s,%06X", e.getKey(), e.getValue().getRGB())).iterator();
			return String.join("|", it);
		}

		private static HashMap<String, Color> decodeColors(String colorArrStr) {
			HashMap<String, Color> colors = new HashMap<>();
			String[] parts = colorArrStr.split("\\|",-1);
			//System.out.printf("parts: %s%n", Arrays.toString(parts));
			for (String part : parts) {
				int pos = part.indexOf(',');
				if (pos<0) continue;
				String objectTypeID = part.substring(0, pos);
				String colorStr     = part.substring(pos+1);
				int colorVal;
				try { colorVal = Integer.parseUnsignedInt(colorStr, 16); }
				catch (NumberFormatException e) {
					System.err.printf("Can't parse color value \"%s\" of ObjectTypeID \"%s\"%n", colorStr, objectTypeID);
					continue;
				}
				colors.put(objectTypeID, new Color(colorVal));
			}
			return colors;
		}

		public class ColorTableModel extends Tables.SimplifiedTableModel<ColorTableModel.ColumnID>{

			enum ColumnID implements SimplifiedColumnIDInterface {
				ID    ("ID"   , String.class, 130),
				Label ("Label", String.class, 260),
				Color ("Color", Color .class,  60),
				
				;
				private final SimplifiedColumnConfig cfg;
				ColumnID(String name, Class<?> columnClass, int width) {
					cfg = new SimplifiedColumnConfig(name, columnClass, 20, -1, width, width);
				}

				@Override public SimplifiedColumnConfig getColumnConfig() { return cfg; }
			
			}
			
			private boolean somethingWasChanged;
			private final HashMap<String, Color> colors;
			private final Vector<String> objectTypeIDs;
			private ObjectTypes objectTypes;

			protected ColorTableModel() {
				super(ColumnID.values());
				somethingWasChanged = false;
				colors = new HashMap<>();
				objectTypeIDs = new Vector<>();
				objectTypes = null;
			}

			void setDefaultCellEditorsAndRenderers(Window window) {
				ColorTCR colorTCR = new ColorTCR(null);
				ColorTCE colorTCE = new ColorTCE(window, colorTCR);
				table.setDefaultRenderer(Color.class, colorTCR);
				table.setDefaultEditor  (Color.class, colorTCE);
			}

			boolean wasSomethingChanged() {
				return somethingWasChanged;
			}

			void setData(HashMap<String, Color> colors, ObjectTypes objectTypes) {
				this.objectTypes = objectTypes;
				this.colors.clear();
				this.colors.putAll(colors);
				somethingWasChanged = false;
				HashSet<String> objectTypeIDSet = new HashSet<>(this.objectTypes.keySet());
				objectTypeIDSet.addAll(this.colors.keySet());
				objectTypeIDs.clear();
				objectTypeIDs.addAll(objectTypeIDSet);
				objectTypeIDs.sort(Comparator.comparing(String::toLowerCase));
			}

			HashMap<String, Color> getData() {
				return new HashMap<>(colors);
			}

			private String getObjectTypeID(int rowIndex) {
				if (rowIndex<0) return null;
				if (rowIndex>objectTypeIDs.size()) return null;
				return objectTypeIDs.get(rowIndex);
			}

			@Override public int getRowCount() {
				return objectTypeIDs.size();
			}

			@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				String objectTypeID = getObjectTypeID(rowIndex);
				if (objectTypeID==null) return null;
				
				switch (columnID) {
				case ID:
					return objectTypeID;
					
				case Label:
					if (objectTypes==null) return null;
					ObjectType ot = objectTypes.get(objectTypeID, null);
					if (ot==null) return null;
					return ot.label;
					
				case Color:
					return colors.get(objectTypeID);
				}
				return null;
			}

			@Override protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID) {
				return columnID==ColumnID.Color;
			}

			@Override
			protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID) {
				String objectTypeID = getObjectTypeID(rowIndex);
				if (objectTypeID==null) return;
				
				//System.out.printf("ColorTableModel.setValueAt(%s,%s): [%s] %s%n", objectTypeID, columnID, aValue==null ? "" : aValue.getClass().getName(), aValue==null ? "<null>" : aValue.toString());
				
				switch (columnID) {
				case ID: case Label: break;
				case Color:
					if (aValue == null) {
						colors.remove(objectTypeID);
						somethingWasChanged = true;
					} else if (aValue instanceof Color) {
						Color newColor = (Color) aValue;
						Color oldColor = colors.put(objectTypeID, newColor);
						somethingWasChanged = !newColor.equals(oldColor);
					}
					break;
				}
			}
		
		}
	
	}

}
