package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectTypeValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeEvent;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeListener;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.PlanetCrafterSaveGameViewer.AppSettings;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.image.linegeometry.Form;
import net.schwarzbaer.java.lib.image.linegeometry.LinesIO;
import net.schwarzbaer.java.tools.lineeditor.LineEditor;
import net.schwarzbaer.java.tools.lineeditor.LineEditor.GuideLinesStorage;

class MapShapes
{
	private final File datafile;
	private final HashMap<String,ObjectTypeData> data;
	
	public MapShapes(File datafile)
	{
		this.datafile = datafile;
		data = new HashMap<>();
	}
	
	private static class ObjectTypeData
	{
		final Vector<MapShape> shapes;
		MapShape selectedShape;
		boolean hideMarker;
		
		ObjectTypeData()
		{
			this.shapes = new Vector<>();
			this.selectedShape = null;
			this.hideMarker = false;
		}

		boolean isEmpty()
		{
			return shapes.isEmpty() && selectedShape==null && !hideMarker;
		}
	}

	public void readFromFile()
	{
		System.out.printf("Read MapShapes from file \"%s\" ...%n", datafile.getAbsolutePath());
		data.clear();
		
		LinesIO linesIO = new LinesIO(LineEditor.createFormFactory());
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(datafile), StandardCharsets.UTF_8))) {
			
			String line, valueStr;
			ObjectTypeData objectTypeData = null;
			MapShape shape = null;
			
			while ( (line=in.readLine())!=null ) {
				
				if (line.isEmpty()) continue;
				if ( (valueStr=getValue(line,"ObjectType: "))!=null )
				{
					data.put(valueStr, objectTypeData = new ObjectTypeData());
					shape = null;
				}
				
				if (objectTypeData!=null)
				{
					if (line.equals("hideMarker"))
						objectTypeData.hideMarker = true;
					
					if ((valueStr=getValue(line,"MapShape: "))!=null)
						objectTypeData.shapes.add(shape = new MapShape(valueStr));
					
					if (shape!=null)
					{
						if (line.equals("selectedShape"))
							objectTypeData.selectedShape = shape;
						
						linesIO.parseFormLine(line, shape.forms::add);
						shape.guideLines.parseLine(line);
					}
				}
				
			}
			
		}
		catch (FileNotFoundException ex) {}
		catch (IOException ex)
		{
			System.err.printf("IOException while reading MapShapes: %s%n", ex.getMessage());
			// ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}

	private static String getValue(String line, String prefix) {
		if (line.startsWith(prefix))
			return line.substring(prefix.length());
		return null;
	}
	
	void writeToFile() {
		System.out.printf("Write MapShapes to file \"%s\" ...%n", datafile.getAbsolutePath());
		
		try (PrintWriter out = new PrintWriter(datafile, StandardCharsets.UTF_8)) {
			
			Vector<Entry<String, ObjectTypeData>> vector = new Vector<>(data.entrySet());
			vector.sort(Comparator.comparing(Entry<String, ObjectTypeData>::getKey,Data.caseIgnoringComparator));
			
			for (Entry<String, ObjectTypeData> entry : vector) {
				ObjectTypeData otd = entry.getValue();
				if (otd.isEmpty()) continue;
				
				out.printf("ObjectType: %s%n", entry.getKey());
				if (otd.hideMarker) out.printf("hideMarker%n");
				
				for (MapShape shape : otd.shapes)
				{
					out.printf("MapShape: %s%n", shape.label);
					if (shape==otd.selectedShape) out.printf("selectedShape%n");
					
					LinesIO.writeForms(out,shape.getForms());
					shape.guideLines.writeToFile(out);
				}
				
				out.println();
			}
			
		} catch (IOException ex) {
			System.err.printf("IOException while writing ObjectTypes: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}

	public boolean hasShapes(ObjectType objectType)
	{
		if (objectType==null) return false;
		
		ObjectTypeData otd = data.get(objectType.id);
		if (otd==null) return false;
		
		return !otd.shapes.isEmpty();
	}
	
	public Vector<MapShape> getShapes(ObjectType objectType)
	{
		if (objectType==null) return null;
		
		ObjectTypeData otd = data.get(objectType.id);
		if (otd==null) return null;
		
		return otd.shapes;
	}

	public void setShowMarker(ObjectType objectType, boolean showMarker)
	{
		if (objectType==null) return;
		
		ObjectTypeData otd = data.get(objectType.id);
		if (otd==null) return;
		
		otd.hideMarker = !showMarker;
		
		writeToFile();
	}

	public boolean shouldShowMarker(ObjectType objectType)
	{
		if (objectType==null) return true;
		
		ObjectTypeData otd = data.get(objectType.id);
		if (otd==null) return true;
		
		return !otd.hideMarker;
	}

	public void setSelectedShape(ObjectType objectType, MapShape shape)
	{
		if (objectType==null) return;
		
		ObjectTypeData otd = data.get(objectType.id);
		if (otd==null) return;
		
		otd.selectedShape = shape;
		
		writeToFile();
	}

	public MapShape getSelectedShape(ObjectType objectType)
	{
		if (objectType==null) return null;
		
		ObjectTypeData otd = data.get(objectType.id);
		if (otd==null) return null;
		
		return otd.selectedShape;
	}

	static class MapShape
	{
		String label;
		private final GuideLinesStorage guideLines;
		private final Vector<Form> forms;
		
		public MapShape(String label)
		{
			this.label = label;
			guideLines = new GuideLinesStorage();
			forms = new Vector<>();
		}

		public Vector<Form> getForms()
		{
			return forms;
		}

		@Override public String toString()
		{
			return label;
		}
	}

	static class Editor extends StandardDialog implements ObjectTypesChangeListener
	{
		private static final long serialVersionUID = 3284148312241943876L;
		private final JPanel leftPanel;
		private final LineEditor lineEditor;
		private JComponent valuePanel;
		private final JComboBox<ObjectType> cmbbxObjectTypes;
		private final JComboBox<MapShape> cmbbxMapShapes;
		private final MapShapes mapShapes;
		private ObjectType selectedObjectType;
		private Vector<MapShape> selectedShapes;
		private MapShape selectedShape;
		private final ObjectTypes objectTypes;
	
		public Editor(Window parent, String title, MapShapes mapShapes, ObjectTypes objectTypes)
		{
			super(parent, title, ModalityType.MODELESS, true);
			this.objectTypes = objectTypes;
			this.mapShapes = Objects.requireNonNull(mapShapes);
			selectedObjectType = null;
			selectedShape = null;
			
			leftPanel = new JPanel(new BorderLayout(3,3));
			
			lineEditor = new LineEditor(new LineEditor.Context() {
				
				@Override public void switchOptionsPanel(JComponent panel)
				{
					if (valuePanel!=null) leftPanel.remove(valuePanel);
					valuePanel = panel;
					if (valuePanel!=null) leftPanel.add(valuePanel,BorderLayout.CENTER);
					leftPanel.revalidate();
					leftPanel.repaint();
				}
				
				@Override public void replaceForms(Form[] forms)
				{
					boolean isNewShape = false;
					if (selectedShape==null)
					{
						if (selectedObjectType==null)
						{
							System.err.println("ERROR: New forms were created, but not ObjectType was selected.");
							return;
						}
						createNewShape(selectedObjectType);
						isNewShape = true;
					}
					if (selectedShape!=null)
					{
						selectedShape.forms.clear();
						selectedShape.forms.addAll(Arrays.asList(forms));
						Editor.this.mapShapes.writeToFile();
						if (isNewShape)
							updateLineEditorFormsList();
					}
					else
						throw new IllegalStateException();
				}
				
				@Override public boolean canCreateNewForm()
				{
					return selectedObjectType!=null;
				}
			});
			valuePanel = lineEditor.getInitialOptionsPanel();
			
			cmbbxObjectTypes = new JComboBox<>(this.objectTypes.getListSortedByName());
			cmbbxObjectTypes.setRenderer(new Tables.NonStringRenderer<>(obj->obj==null ? "" : ((ObjectType)obj).getName()));
			cmbbxMapShapes = new JComboBox<>();
			
			JButton btnNewShape = GUI.createButton("New Shape", false, e->{
				if (selectedObjectType==null) { System.err.println("ERROR: Can't create new shape, because no ObjectType is selected."); return; }
				createNewShape(selectedObjectType);
				this.mapShapes.writeToFile();
			});
			JButton btnDeleteShape = GUI.createButton("Delete Shape", false, e->{
				if (selectedShape     ==null) { System.err.println("ERROR: Can't delete shape, because no shape is selected."); return; }
				if (selectedObjectType==null) { System.err.println("ERROR: Can't delete shape, because no ObjectType is selected."); return; }
				boolean wasDeleted = deleteShape(selectedObjectType, selectedShape);
				if (wasDeleted) this.mapShapes.writeToFile();
			});
			JButton btnChangeShapeName = GUI.createButton("Change Shape Name", false, e->{
				if (selectedShape==null) { System.err.println("ERROR: Can't change shape name, because no shape is selected."); return; }
				String newName = askForShapeName(selectedShape.label, selectedShapes);
				if (newName!=null)
				{
					selectedShape.label = newName;
					cmbbxMapShapes.repaint();
					this.mapShapes.writeToFile();
				}
			});
			JButton btnSaveAllShapes = GUI.createButton("Save all shapes data in file", GUI.ToolbarIcons.Save, true, e->{
				this.mapShapes.writeToFile();
			});
			
			cmbbxObjectTypes.addActionListener(e->{
				int index = cmbbxObjectTypes.getSelectedIndex();
				selectedObjectType = index<0 ? null : cmbbxObjectTypes.getItemAt(index);
				System.out.printf("ObjectType selected: %s%n", selectedObjectType==null ? "-- none --" : selectedObjectType.getName());
				btnNewShape.setEnabled(selectedObjectType!=null);
				selectedShapes = selectedObjectType==null ? null : this.mapShapes.getShapes(selectedObjectType);
				updateCmbbxMapShapes();
			});
			cmbbxMapShapes.addActionListener(e->{
				int index = cmbbxMapShapes.getSelectedIndex();
				selectedShape = index<0 ? null : cmbbxMapShapes.getItemAt(index);
				System.out.printf("Shape selected: %s%n", selectedShape==null ? "-- none --" : selectedShape.label);
				btnChangeShapeName.setEnabled(selectedShape!=null);
				btnDeleteShape    .setEnabled(selectedShape!=null);
				updateLineEditorFormsList();
			});
			
			JPanel panelShapeButtons = new JPanel(new GridLayout(0,1));
			panelShapeButtons.add(btnNewShape);
			panelShapeButtons.add(btnDeleteShape);
			panelShapeButtons.add(btnChangeShapeName);
			panelShapeButtons.add(btnSaveAllShapes);
			
			JPanel leftUpperPanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weightx = 0; c.gridwidth = 1;
			leftUpperPanel.add(new JLabel("Object Type :  "), c);
			c.weightx = 1; c.gridwidth = GridBagConstraints.REMAINDER;
			leftUpperPanel.add(cmbbxObjectTypes, c);
			
			c.weightx = 0; c.gridwidth = 1;
			leftUpperPanel.add(new JLabel("Map Shapes :  "), c);
			c.weightx = 1; c.gridwidth = GridBagConstraints.REMAINDER;
			leftUpperPanel.add(cmbbxMapShapes, c);
			
			c.weightx = 1; c.gridwidth = GridBagConstraints.REMAINDER;
			leftUpperPanel.add(panelShapeButtons, c);
			
			//leftUpperPanel.setPreferredSize(new Dimension(100,100));
			
			leftPanel.add(leftUpperPanel,BorderLayout.NORTH);
			leftPanel.add(valuePanel,BorderLayout.CENTER);
			
			JPanel editorViewPanel = new JPanel(new BorderLayout(3,3));
			editorViewPanel.setBorder(BorderFactory.createTitledBorder("Shape"));
			editorViewPanel.add(lineEditor.getEditorView(),BorderLayout.CENTER);
			
			JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
			contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			contentPane.setLeftComponent(leftPanel);
			contentPane.setRightComponent(editorViewPanel);
			contentPane.setResizeWeight(0);
			
			createGUI(contentPane, GUI.createButton("Close", true, e->closeDialog()));
			
			PlanetCrafterSaveGameViewer.settings.registerExtraWindow(this,
				AppSettings.ValueKey.MapShapesEditor_WindowX,
				AppSettings.ValueKey.MapShapesEditor_WindowY,
				AppSettings.ValueKey.MapShapesEditor_WindowWidth,
				AppSettings.ValueKey.MapShapesEditor_WindowHeight
			);
			
			PlanetCrafterSaveGameViewer.settings.registerSplitPaneDividers(
				new AppSettings.SplitPaneDividersDefinition<>(this, AppSettings.ValueKey.class)
				.add(contentPane, AppSettings.ValueKey.MapShapesEditor_SplitPaneDivider)
			);
		}

		private void updateLineEditorFormsList()
		{
			lineEditor.setForms(selectedShape==null ? null : selectedShape.forms.toArray(Form[]::new));
		}

		private void updateCmbbxMapShapes()
		{
			Vector<MapShape> displayedShapes = selectedShapes==null ? new Vector<>() : selectedShapes;
			cmbbxMapShapes.setModel(new DefaultComboBoxModel<>(displayedShapes));
			cmbbxMapShapes.setSelectedItem(null);
		}
	
		private String askForShapeName(String oldName, Vector<MapShape> selectedShapes)
		{
			while (true) {
				String newName = JOptionPane.showInputDialog(this, "Enter a new shape name:", oldName);
				if (newName==null || newName.equals(oldName))
					return null;
				if (isUniqueName(newName, selectedShapes))
					return newName;
			}
		}

		private boolean isUniqueName(String name, Vector<MapShape> shapes)
		{
			if (name==null) throw new IllegalArgumentException();
			if (shapes==null || shapes.isEmpty()) return true;
			for (MapShape shape : shapes)
				if (name.equals(shape.label))
					return false;
			return true;
		}

		private boolean deleteShape(ObjectType objectType, MapShape shape)
		{
			if (objectType==null) throw new IllegalArgumentException();
			if (shape     ==null) throw new IllegalArgumentException();
			
			ObjectTypeData otd = mapShapes.data.get(objectType.id);
			if (otd==null) return false;
			
			String title = "Are you sure ?";
			String message = String.format("Are you sure that you want to delete shape \"%s\"?", shape.label);
			if (JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
				return false;
			
			if (otd.selectedShape == shape)
				otd.selectedShape = null;
			
			boolean removed = otd.shapes.remove(shape);
			if (removed)
			{
				selectedShapes = otd.shapes;
				updateCmbbxMapShapes();
				cmbbxMapShapes.setSelectedItem(null);
			}
			
			return removed;
		}

		private void createNewShape(ObjectType objectType)
		{
			if (objectType==null) throw new IllegalArgumentException();
			
			ObjectTypeData otd = mapShapes.data.get(objectType.id);
			if (otd==null) mapShapes.data.put(objectType.id, otd = new ObjectTypeData());
			
			int index = otd.shapes.size()+1;
			String name = String.format("Shape%03d", index);
			while (!isUniqueName(name, otd.shapes))
				name = String.format("Shape%03d", ++index);
			
			MapShape newShape = new MapShape(name);
			otd.shapes.add(newShape);
			
			selectedShapes = otd.shapes;
			updateCmbbxMapShapes();
			cmbbxMapShapes.setSelectedItem(newShape);
		}

		public void showDialog(ObjectType objectType)
		{
			cmbbxObjectTypes.setSelectedItem(objectType);
			showDialog(Position.PARENT_CENTER);
		}

		public void updateAfterNewObjectTypes()
		{
			Vector<ObjectType> list = objectTypes.getListSortedByName();
			cmbbxObjectTypes.setModel(new DefaultComboBoxModel<>(list));
			cmbbxObjectTypes.setSelectedItem(selectedObjectType);
		}

		@Override
		public void objectTypesChanged(ObjectTypesChangeEvent event)
		{
			if (event==null) return;
			switch (event.eventType)
			{
				case NewTypeAdded: updateAfterNewObjectTypes(); break;
				case ValueChanged: if (event.changedValue==ObjectTypeValue.Label) updateAfterNewObjectTypes(); break;
			}
		}
	}
	
}
