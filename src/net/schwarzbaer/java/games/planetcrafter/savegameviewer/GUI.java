package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.PlanetCrafterSaveGameViewer.AppSettings;
import net.schwarzbaer.java.lib.gui.ContextMenu;
import net.schwarzbaer.java.lib.gui.Disabler;
import net.schwarzbaer.java.lib.gui.GeneralIcons;
import net.schwarzbaer.java.lib.gui.HSColorChooser;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnIDInterface;

class GUI {
	static final Color COLOR_Removal_ByData = new Color(0xFFD5D5);
	static final Color COLOR_Removal_ByUser = new Color(0xFF7F7F);
	static final Color COLOR_Removal_Partially = new Color(0xFFD5D5);
	static final Color COLOR_Removal_Fully     = new Color(0xFF7F7F);

	enum ActionCommand {
		ReloadSaveGame, OpenSaveGame, WriteReducedSaveGame, ScanSaveGame, ConfigureAchievements, ReloadSaveGameAutoSwitch, ShowMapShapesEditor, SetLabelLanguageDE, SetLabelLanguageEN
	}
	
	private static <Type extends AbstractButton> Type setAbstractButton(
			Type comp,
			String title,
			GeneralIcons.IconGroup icons,
			Boolean isChecked,
			boolean isEnabled,
			ActionListener al,
			Disabler<ActionCommand> disabler, ActionCommand ac
		) {
		Icon enabledIcon  = icons !=null ? icons.getEnabledIcon () : null;
		Icon disabledIcon = icons !=null ? icons.getDisabledIcon() : null;
		return setAbstractButton(comp, title, enabledIcon, disabledIcon, isChecked, isEnabled, al, disabler, ac);
	}

	private static <Type extends AbstractButton> Type setAbstractButton(
			Type comp,
			String title,
			Icon enabledIcon,
			Icon disabledIcon,
			Boolean isChecked,
			boolean isEnabled,
			ActionListener al,
			Disabler<ActionCommand> disabler,
			ActionCommand ac
		) {
		if (title       !=null) comp.setText(title);
		if (enabledIcon !=null) comp.setIcon(enabledIcon);
		if (disabledIcon!=null) comp.setDisabledIcon(disabledIcon);
		if (isChecked   !=null) comp.setSelected(isChecked);
		if (al!=null) {
			comp.addActionListener(al);
			if (ac!=null) {
				comp.setActionCommand(ac.name());
				if (disabler!=null) disabler.add(ac, comp);
			}
		}
		comp.setEnabled(isEnabled);
		return comp;
	}
	
	private static <Type extends AbstractButton> Type addToButtonGroup(ButtonGroup bg, Type comp) {
		if (bg!=null) bg.add(comp);
		return comp;
	}
	
	static JCheckBox createCheckBox(String title, boolean isChecked, boolean isEnabled, Consumer<Boolean> valueChanged, Disabler<ActionCommand> disabler, ActionCommand ac) {
		JCheckBox comp = new JCheckBox();
		return setAbstractButton(comp, title, null, isChecked, isEnabled, e->valueChanged.accept(comp.isSelected()), disabler, ac);
	}

	static JButton createButton(String title, GeneralIcons.IconGroup icons, boolean isEnabled, ActionListener al, Disabler<ActionCommand> disabler, ActionCommand ac) {
		return setAbstractButton(new JButton(), title, icons, null, isEnabled, al, disabler, ac);
	}
	
	static JButton createButton(String title, GeneralIcons.IconGroup icons, boolean isEnabled, ActionListener al) {
		return setAbstractButton(new JButton(), title, icons, null, isEnabled, al, null, null);
	}
	
	static JButton createButton(String title, boolean isEnabled, ActionListener al) {
		return setAbstractButton(new JButton(), title, null, null, isEnabled, al, null, null);
	}
	
	static JRadioButton createRadioButton(String title, boolean isChecked, ButtonGroup bg, boolean isEnabled, ActionListener al, Disabler<ActionCommand> disabler, ActionCommand ac) {
		return addToButtonGroup(bg, setAbstractButton(new JRadioButton(), title, null, null, isChecked, isEnabled, al, disabler, ac));
	}
	
	static JMenuItem createMenuItem(String title, GeneralIcons.IconGroup icons, boolean isEnabled, ActionListener al, Disabler<ActionCommand> disabler, ActionCommand ac) {
		return setAbstractButton(new JMenuItem(), title, icons, null, isEnabled, al, disabler, ac);
	}

	static JMenuItem createMenuItem(String title, GeneralIcons.IconGroup icons, boolean isEnabled, ActionListener al) {
		return setAbstractButton(new JMenuItem(), title, icons, null, isEnabled, al, null, null);
	}

	static JMenuItem createMenuItem(String title, boolean isEnabled, ActionListener al) {
		return setAbstractButton(new JMenuItem(), title, null, null, isEnabled, al, null, null);
	}

	static JMenuItem createMenuItem(String title, Icon icon, ActionListener al) {
		return setAbstractButton(new JMenuItem(), title, icon, null, null, true, al, null, null);
	}

	static JMenuItem createMenuItem(String title, ActionListener al) {
		return setAbstractButton(new JMenuItem(), title, null, null, true, al, null, null);
	}

	static JCheckBoxMenuItem createCheckBoxMenuItem(String title, boolean checked, Consumer<Boolean> setValue) {
		JCheckBoxMenuItem comp = new JCheckBoxMenuItem();
		ActionListener al = setValue==null ? null : e -> setValue.accept(comp.isSelected());
		return setAbstractButton(comp, title, null, checked, true, al, null, null);
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
	
	static JSlider createSlider(int orientation, int min, int max, int value, ChangeListener chl) {
		JSlider comp = new JSlider(orientation, min, max, value);
		if (chl!=null) comp.addChangeListener(chl);
		return comp;
	}
	
	static void reduceTextAreaFontSize(Component baseComp, int diff, JTextArea textArea)
	{
		Font baseFont = baseComp.getFont();
		if (baseFont==null) return;
		reduceTextAreaFontSize(baseFont.getSize() + diff, textArea);
	}
	static void reduceTextAreaFontSize(int maxFontSize, JTextArea textArea)
	{
		Font textAreaFont = textArea.getFont();
		if (textAreaFont==null) return;
		int textAreaFontSize = textAreaFont.getSize();
		
		if (maxFontSize < textAreaFontSize)
			textArea.setFont( textAreaFont.deriveFont((float)maxFontSize) );
	}
	
	static class ColorIcon implements Icon {

		private final int width;
		private final int height;
		private final Color fillColor;
		private final Color borderColor;
		
		ColorIcon(int width, int height, Color fillColor, Color borderColor)
		{
			this.width = width;
			this.height = height;
			this.fillColor = fillColor;
			this.borderColor = borderColor;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			g.setColor(borderColor);
			g.drawRect(x, y, width-1, height-1);
			g.setColor(fillColor);
			g.fillRect(x+1, y+1, width-1, height-1);
		}

		@Override public int getIconWidth () { return width ; }
		@Override public int getIconHeight() { return height; }
	}
	
	static class ColorTCE extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = -6683601147557918947L;
		
		private final ColorTCR renderer;
		private final HSColorChooser.ColorDialog hsColorChooserDialog;
		private Color result;
		
		ColorTCE(Window window, ColorTCR renderer, String dlgTitle) {
			this.renderer = renderer;
			hsColorChooserDialog = new HSColorChooser.ColorDialog(window, dlgTitle, true, Color.GRAY);
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
				hsColorChooserDialog.showDialog(HSColorChooser.PARENT_CENTER);
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
				value = color.getColor(true);
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
		
		private final Vector<Data.WorldObject> objects;
		        final String name;
		private       RemovalState removalState;
		
		ObjectsTableRow(String name) {
			this.name = name;
			objects = new Vector<>();
			removalState = RemovalState.None;
		}
		
		void markForRemoval(boolean b) {
			removalState = b ? RemovalState.Fully : RemovalState.None;
			for (Data.WorldObject wo : objects)
				wo.markForRemoval(b, false);
		}
	
		RemovalState getRemovalState() {
			return removalState;
		}
	
		void updateRemoveState() {
			boolean none = true;
			boolean all = true;
			for (Data.WorldObject wo : objects) {
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
	
		void add(Data.WorldObject wo) {
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

	static class AchievedValuesDialog extends StandardDialog {
		private static final long serialVersionUID = -580668583006732866L;
		
		private final JButton btnOk;
		private final DoubleTextField oxygenLevel  ;
		private final DoubleTextField heatLevel    ;
		private final DoubleTextField pressureLevel;
		private final DoubleTextField plantsLevel  ;
		private final DoubleTextField insectsLevel ;
		private final DoubleTextField animalsLevel ;
		private final JTextField biomassLevel;
		private final JTextField terraformLevel;
		private final LongTextField terraTokens;
		private final LongTextField allTimeTerraTokens;
		private       Data.AchievedValues results;
		private final Data.AchievedValues initialValues;

		private AchievedValuesDialog(Window parent, String title, Data.AchievedValues initialValues) {
			super(parent, title);
			this.initialValues = initialValues;
			results = null;
			
			oxygenLevel        = new DoubleTextField(0.0, Data.AchievedValues::formatOxygenLevel);
			heatLevel          = new DoubleTextField(0.0, Data.AchievedValues::formatHeatLevel);
			pressureLevel      = new DoubleTextField(0.0, Data.AchievedValues::formatPressureLevel);
			biomassLevel       = DoubleTextField.createFormattedValueOutput("");
			plantsLevel        = new DoubleTextField(0.0, Data.AchievedValues::formatBiomassLevel);
			insectsLevel       = new DoubleTextField(0.0, Data.AchievedValues::formatBiomassLevel);
			animalsLevel       = new DoubleTextField(0.0, Data.AchievedValues::formatBiomassLevel);
			terraformLevel     = DoubleTextField.createFormattedValueOutput("");
			terraTokens        = new LongTextField  (0L , Data.AchievedValues::formatTerraTokens);
			allTimeTerraTokens = new LongTextField  (0L , Data.AchievedValues::formatTerraTokens);
			
			if (this.initialValues!=null) {
				oxygenLevel       .setValue(this.initialValues.oxygenLevel       );
				heatLevel         .setValue(this.initialValues.heatLevel         );
				pressureLevel     .setValue(this.initialValues.pressureLevel     );
				plantsLevel       .setValue(this.initialValues.plantsLevel       );
				insectsLevel      .setValue(this.initialValues.insectsLevel      );
				animalsLevel      .setValue(this.initialValues.animalsLevel      );
				terraTokens       .setValue(this.initialValues.terraTokens       );
				allTimeTerraTokens.setValue(this.initialValues.allTimeTerraTokens);
			}
			
			JPanel contentPane = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridwidth = 1;
			
			int gridy = 0;
			addRow(contentPane, c, gridy++, "Oxygen Level"         , oxygenLevel       , this.initialValues==null ? null : this.initialValues.oxygenLevel  , 0.0);
			addRow(contentPane, c, gridy++, "Heat Level"           , heatLevel         , this.initialValues==null ? null : this.initialValues.heatLevel    , 0.0);
			addRow(contentPane, c, gridy++, "Pressure Level"       , pressureLevel     , this.initialValues==null ? null : this.initialValues.pressureLevel, 0.0);
			addRow(contentPane, c, gridy++, "Biomass Level"        , biomassLevel      );
			addRow(contentPane, c, gridy++, "Plants Level"         , plantsLevel       , this.initialValues==null ? null : this.initialValues.plantsLevel , 0.0);
			addRow(contentPane, c, gridy++, "Insects Level"        , insectsLevel      , this.initialValues==null ? null : this.initialValues.insectsLevel, 0.0);
			addRow(contentPane, c, gridy++, "Animals Level"        , animalsLevel      , this.initialValues==null ? null : this.initialValues.animalsLevel, 0.0);
			addRow(contentPane, c, gridy++, "Terraformation Level" , terraformLevel    );
			addRow(contentPane, c, gridy++, "Terra Tokens"         , terraTokens       , this.initialValues==null ? null : this.initialValues.terraTokens       , 0L);
			addRow(contentPane, c, gridy++, "All Time Terra Tokens", allTimeTerraTokens, this.initialValues==null ? null : this.initialValues.allTimeTerraTokens, 0L);
			
			c.weightx = 1;
			c.weighty = 1;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			contentPane.add(new JLabel(), c);
			
			createGUI(contentPane,
					createButton("Set All to 0", true, e->{
						oxygenLevel       .setValue(0.0);
						heatLevel         .setValue(0.0);
						pressureLevel     .setValue(0.0);
						plantsLevel       .setValue(0.0);
						insectsLevel      .setValue(0.0);
						animalsLevel      .setValue(0.0);
						terraTokens       .setValue(0L);
						allTimeTerraTokens.setValue(0L);
						updateGUI();
					}),
					this.initialValues==null ? null : createButton("Reset All", true, e->{
						oxygenLevel       .setValue(this.initialValues.oxygenLevel       );
						heatLevel         .setValue(this.initialValues.heatLevel         );
						pressureLevel     .setValue(this.initialValues.pressureLevel     );
						plantsLevel       .setValue(this.initialValues.plantsLevel       );
						insectsLevel      .setValue(this.initialValues.insectsLevel      );
						animalsLevel      .setValue(this.initialValues.animalsLevel      );
						terraTokens       .setValue(this.initialValues.terraTokens       );
						allTimeTerraTokens.setValue(this.initialValues.allTimeTerraTokens);
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

		private <ValueType> void addRow(JPanel panel, GridBagConstraints c, int gridy, String label, NumberTextField<ValueType> txtField, ValueType initialValue, ValueType zeroValue) {
			int gridx = 0;
			c.gridy = gridy;
			c.weightx = 0; c.gridx = gridx++; panel.add(new JLabel(label+": "), c);
			c.weightx = 0; c.gridx = gridx++; panel.add(txtField.formattedValueOutput, c);
			c.weightx = 1; c.gridx = gridx++; panel.add(txtField, c);
			if (zeroValue!=null) {
				c.weightx = 0; c.gridx = gridx++; panel.add(createButton("Set 0", true, e->{
					txtField.setValue(zeroValue);
					updateGUI();
				}), c);
			}
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
			return  oxygenLevel       .isOK() &&
					heatLevel         .isOK() &&
					pressureLevel     .isOK() &&
					plantsLevel       .isOK() &&
					insectsLevel      .isOK() &&
					animalsLevel      .isOK() &&
					terraTokens       .isOK() &&
					allTimeTerraTokens.isOK();
		}

		private void createResult() {
			results = !areAllValuesOk() ? null : new Data.AchievedValues(
					oxygenLevel       .value,
					heatLevel         .value,
					pressureLevel     .value,
					plantsLevel       .value,
					insectsLevel      .value,
					animalsLevel      .value,
					terraTokens       .value,
					allTimeTerraTokens.value,
					initialValues
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
				terraformLevel.setText(Data.AchievedValues.formatTerraformation(val));
			}
			
			if (plantsLevel  .isOK() &&
				insectsLevel .isOK() &&
				animalsLevel .isOK()) {
				double val =
						plantsLevel  .value+
						insectsLevel .value+
						animalsLevel .value;
				biomassLevel.setText(Data.AchievedValues.formatBiomassLevel(val));
			}
		}

		private class DoubleTextField extends NumberTextField<Double>
		{
			private static final long serialVersionUID = -725198905418022339L;

			DoubleTextField(Double value, Function<Double, String> getFormattedValueStr)
			{
				super(value, getFormattedValueStr);
			}

			@Override Double parseValue(String str) throws NumberFormatException
			{
				double newValue = Double.parseDouble(str);
				if (Double.isNaN(newValue)) return null;
				return newValue;
			}
		}

		private class LongTextField extends NumberTextField<Long>
		{
			private static final long serialVersionUID = 9212649389660894581L;

			LongTextField(Long value, Function<Long, String> getFormattedValueStr)
			{
				super(value, getFormattedValueStr);
			}

			@Override Long parseValue(String str) throws NumberFormatException
			{
				long newValue = Long.parseLong(str);
				if (newValue<0) return null;
				return newValue;
			}
		}

		private abstract class NumberTextField<ValueType> extends JTextField {
			private static final long serialVersionUID = 7631623492689405688L;
			
			ValueType value;
			private boolean isOK;
			private final JTextField formattedValueOutput;
			private final Function<ValueType, String> getFormattedValueStr;
			private final Color defaultBG;

			NumberTextField(ValueType value, Function<ValueType,String> getFormattedValueStr) {
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
					ValueType newValue = parseValue(getText());
					if (newValue!=null) changeValue(newValue);
				} catch (NumberFormatException e1) { }
				setBackground(isOK ? defaultBG : Color.RED);
				updateGUI();
			}

			abstract ValueType parseValue(String str) throws NumberFormatException;

			boolean isOK() {
				return isOK;
			}

			void setValue(ValueType value) {
				changeValue(value);
				setText(String.format(Locale.ENGLISH, "%s", this.value));
			}

			private void changeValue(ValueType value) {
				this.value = value;
				formattedValueOutput.setText(getFormattedValueStr.apply(this.value));
				isOK = true;
			}
			
		}

		static Data.AchievedValues show(Window parent, String title, Data.AchievedValues initialValues) {
			AchievedValuesDialog dlg = new AchievedValuesDialog(parent, title, initialValues);
			dlg.showDialog();
			return dlg.results;
		}
	}

	static class ObjectTypeColorsDialog extends StandardDialog {
		private static final long serialVersionUID = 7975796985768293299L;
		
		private final ColorTableModel tableModel;
		private final JTable table;

		ObjectTypeColorsDialog(Window window, String title) {
			super(window, title);
			
			tableModel = new ColorTableModel();
			table = new JTable(tableModel);
			JScrollPane contentPane = new JScrollPane(table);
			
			table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			
			tableModel.setTable(table);
			tableModel.setColumnWidths(table);
			tableModel.setDefaultCellEditorsAndRenderers(this);
			
			new ObjectsTableContextMenu(table, tableModel);
			
			Dimension size = table.getPreferredSize();
			size.width  += 30;
			size.height = 500;
			contentPane.setPreferredSize(size);
			
			createGUI(contentPane, createButton("Close", true, e->closeDialog()));
		}

		public HashMap<String, Color> showDialogAndGetColors() {
			String colorArrStr = AppSettings.getInstance().getString(AppSettings.ValueKey.ObjectTypeColors, "");
			//System.out.printf("ObjectTypeColorsDialog.input: %s%n", colorArrStr);
			HashMap<String, Color> colors = decodeColors(colorArrStr);
			//colors.forEach((id,color)->System.out.printf("ObjectTypeColorsDialog.input: \"%s\", %s%n", id, color));
			tableModel.setData(colors);
			
			showDialog();
			
			if (tableModel.wasSomethingChanged()) {
				colors = tableModel.getData();
				//colors.forEach((id,color)->System.out.printf("ObjectTypeColorsDialog.result: \"%s\", %s%n", id, color));
				colorArrStr = encodeColors(colors);
				//System.out.printf("ObjectTypeColorsDialog.result: %s%n", colorArrStr);
				AppSettings.getInstance().putString(AppSettings.ValueKey.ObjectTypeColors, colorArrStr);
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
		
		private static class ObjectsTableContextMenu extends ContextMenu {
			private static final long serialVersionUID = 7730330222817550351L;
			private int clickedRowIndex;
			private String clickedObjectTypeID;

			ObjectsTableContextMenu(JTable table, ColorTableModel tableModel) {
				
				JMenuItem miRemoveColor = add(createMenuItem("Remove Color", e->{
					if (clickedObjectTypeID==null) return;
					tableModel.removeColor(clickedObjectTypeID);
				}));
				
				addTo(table);
				
				addContextMenuInvokeListener((comp, x, y) ->
				{
					int rowV = table.rowAtPoint(new Point(x,y));
					clickedRowIndex = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
					clickedObjectTypeID = clickedRowIndex<0 ? null : tableModel.getObjectTypeID(clickedRowIndex);
					
					miRemoveColor.setEnabled( clickedObjectTypeID==null ? false : tableModel.hasColor(clickedObjectTypeID) );
					String label = clickedObjectTypeID==null ? null : tableModel.getLabel(clickedObjectTypeID);
					miRemoveColor.setText(
							label != null && !label.isBlank()
								? String.format("Remove Color from \"%s\"", label)
								: clickedObjectTypeID != null
									? String.format("Remove Color from ID:%s", clickedObjectTypeID)
									: "Remove Color"
					);
				});
			}
		}

		private static class ColorTableModel extends Tables.SimplifiedTableModel<ColorTableModel.ColumnID>{

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
			private final ObjectTypes objectTypes;

			protected ColorTableModel() {
				super(ColumnID.values());
				somethingWasChanged = false;
				colors = new HashMap<>();
				objectTypeIDs = new Vector<>();
				objectTypes = ObjectTypes.getInstance();
			}

			void setDefaultCellEditorsAndRenderers(Window window) {
				ColorTCR colorTCR = new ColorTCR(null);
				ColorTCE colorTCE = new ColorTCE(window, colorTCR, "Select color of object type");
				table.setDefaultRenderer(Color.class, colorTCR);
				table.setDefaultEditor  (Color.class, colorTCE);
			}

			boolean wasSomethingChanged() {
				return somethingWasChanged;
			}

			void setData(HashMap<String, Color> colors) {
				this.colors.clear();
				this.colors.putAll(colors);
				somethingWasChanged = false;
				HashSet<String> objectTypeIDSet = new HashSet<>(objectTypes.keySet());
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

			boolean hasColor(String objectTypeID)
			{
				return colors.containsKey(objectTypeID);
			}

			String getLabel(String objectTypeID)
			{
				if (objectTypes==null) return null;
				ObjectType ot = objectTypes.get(objectTypeID, null);
				if (ot==null) return null;
				return ot.getLabel();
			}

			@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				String objectTypeID = getObjectTypeID(rowIndex);
				if (objectTypeID==null) return null;
				
				switch (columnID) {
				case ID   : return objectTypeID;
				case Label: return getLabel(objectTypeID);
				case Color: return colors.get(objectTypeID);
				}
				return null;
			}

			@Override protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID) {
				return columnID==ColumnID.Color;
			}

			void removeColor(String objectTypeID)
			{
				TableCellEditor editor = table.getCellEditor();
				if (editor!=null) editor.cancelCellEditing();
				colors.remove(objectTypeID);
				somethingWasChanged = true;
				fireTableColumnUpdate(ColumnID.Color);
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
