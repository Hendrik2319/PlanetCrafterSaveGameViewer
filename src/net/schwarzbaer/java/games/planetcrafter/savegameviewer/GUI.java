package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collection;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.TerraformingStates;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;

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

	static JTextField createOutputTextField(String text, int horizontalAlignment) {
		JTextField comp = createOutputTextField(text);
		comp.setHorizontalAlignment(horizontalAlignment);
		return comp;
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
		
		private TerraformingStatesDialog(Window parent, String title, Vector<Data.TerraformingStates> terraformingStates) {
			super(parent, title);
			results = null;
			
			oxygenLevel   = new DoubleTextField(0.0);
			heatLevel     = new DoubleTextField(0.0);
			pressureLevel = new DoubleTextField(0.0);
			plantsLevel   = new DoubleTextField(0.0);
			insectsLevel  = new DoubleTextField(0.0);
			animalsLevel  = new DoubleTextField(0.0);
			
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
			addRow(contentPane, c, gridy++, "Oxygen Level"  , oxygenLevel  , initialValues==null ? null : initialValues.oxygenLevel  );
			addRow(contentPane, c, gridy++, "Heat Level"    , heatLevel    , initialValues==null ? null : initialValues.heatLevel    );
			addRow(contentPane, c, gridy++, "Pressure Level", pressureLevel, initialValues==null ? null : initialValues.pressureLevel);
			addRow(contentPane, c, gridy++, "Plants Level"  , plantsLevel  , initialValues==null ? null : initialValues.plantsLevel  );
			addRow(contentPane, c, gridy++, "Insects Level" , insectsLevel , initialValues==null ? null : initialValues.insectsLevel );
			addRow(contentPane, c, gridy++, "Animals Level" , animalsLevel , initialValues==null ? null : initialValues.animalsLevel );
			
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
			c.gridy = gridy;
			c.weightx = 0; c.gridx = 0; panel.add(new JLabel(label+": "), c);
			c.weightx = 1; c.gridx = 1; panel.add(txtField, c);
			c.weightx = 0; c.gridx = 2; panel.add(createButton("Set 0", true, e->{
				txtField.setValue(0);
				updateGUI();
			}), c);
			if (initialValue!=null) {
				c.weightx = 0; c.gridx = 3; panel.add(createButton("Reset", true, e->{
					txtField.setValue(initialValue);
					updateGUI();
				}), c);
			}
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
			btnOk.setEnabled(areAllValuesOk());
		}

		private class DoubleTextField extends JTextField {
			private static final long serialVersionUID = 7631623492689405688L;
			
			private double value;
			private boolean isOK;

			DoubleTextField(double value) {
				super(20);
				setValue(value);
				Color defaultBG = getBackground();
				isOK = true;
				
				Runnable setValue = ()->{
					isOK = false;
					try {
						double newValue = Double.parseDouble(getText());
						if (!Double.isNaN(newValue)) { this.value = newValue; isOK = true; }
					} catch (NumberFormatException e1) { }
					setBackground(isOK ? defaultBG : Color.RED);
					updateGUI();
				};
				
				addActionListener(e->{setValue.run();});
				addFocusListener(new FocusListener() {
					@Override public void focusGained(FocusEvent e) {}
					@Override public void focusLost(FocusEvent e) { setValue.run(); }
				});
			}

			boolean isOK() {
				return isOK;
			}

			void setValue(double value) {
				this.value = value;
				setText(String.format(Locale.ENGLISH, "%s", this.value));
				isOK = true;
			}
			
		}

		static TerraformingStates show(Window parent, String title, Vector<TerraformingStates> terraformingStates) {
			TerraformingStatesDialog dlg = new TerraformingStatesDialog(parent, title, terraformingStates);
			dlg.showDialog();
			return dlg.results;
		}
	}

}
