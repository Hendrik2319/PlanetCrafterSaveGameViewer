package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
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
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectTypeValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeEvent;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeListener;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.PlanetCrafterSaveGameViewer.AppSettings;
import net.schwarzbaer.java.lib.gui.GeneralIcons.GrayCommandIcons;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.ZoomableCanvas.ViewState;
import net.schwarzbaer.java.lib.image.linegeometry.Form;
import net.schwarzbaer.java.lib.image.linegeometry.LinesIO;
import net.schwarzbaer.java.tools.lineeditor.EditorViewFeature;
import net.schwarzbaer.java.tools.lineeditor.LineEditor;
import net.schwarzbaer.java.tools.lineeditor.LineEditor.GuideLinesStorage;

class MapShapes
{
	private final Window mainWindow;
	private final File datafile;
	private final HashMap<String,ObjectTypeData> data;
	
	public MapShapes(Window mainWindow, File datafile)
	{
		this.mainWindow = mainWindow;
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
		
		Vector<String> errors = new Vector<>();
		
		try (PrintWriter out = new PrintWriter(datafile, StandardCharsets.UTF_8)) {
			
			Vector<Entry<String, ObjectTypeData>> vector = new Vector<>(data.entrySet());
			vector.sort(Comparator.comparing(Entry<String, ObjectTypeData>::getKey,Data.caseIgnoringComparator));
			
			for (int otdIndex=0; otdIndex<vector.size(); otdIndex++)
			{
				Entry<String, ObjectTypeData> entry = vector.get(otdIndex);
				ObjectTypeData otd = entry.getValue();
				if (otd==null) {
					errors.add(String.format("Entry[%d].ObjectTypeData is null", otdIndex));
					continue;
				}
				if (otd.isEmpty()) continue;
				
				out.printf("ObjectType: %s%n", entry.getKey());
				if (otd.hideMarker) out.printf("hideMarker%n");
				
				Vector<MapShape> shapes = otd.shapes;
				for (int shapeIndex=0; shapeIndex<shapes.size(); shapeIndex++)
				{
					MapShape shape = shapes.get(shapeIndex);
					if (shape==null)
						errors.add(String.format("Entry[%d].ObjectTypeData.Shape[%d] is null", otdIndex, shapeIndex));
					else
					{
						out.printf("MapShape: %s%n", shape.label);
						if (shape==otd.selectedShape) out.printf("selectedShape%n");
						
						int otdIndex_ = otdIndex;
						int shapeIndex_ = shapeIndex;
						LinesIO.writeForms(out,shape.getForms(), err->{
							errors.add(String.format("Entry[%d].ObjectTypeData.Shape[%d].Forms -> %s", otdIndex_, shapeIndex_, err));
						});
						
						if (shape.guideLines==null)
							errors.add(String.format("Entry[%d].ObjectTypeData.Shape[%d].GuideLines is null", otdIndex, shapeIndex));
						else
							shape.guideLines.writeToFile(out);
					}
				}
				
				out.println();
			}
			
		} catch (IOException ex) {
			System.err.printf("IOException while writing MapShapes: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
		
		if (!errors.isEmpty())
		{
			errors.insertElementAt("Following errors occured during writing MapShapes to file:", 0);
			JOptionPane.showMessageDialog(mainWindow, errors.toArray(String[]::new), "Error", JOptionPane.ERROR_MESSAGE);;
		}
	}

	public ObjectTypeData getOTD(ObjectType objectType)
	{
		if (objectType==null) return null;
		return data.get(objectType.id);
	}

	public boolean hasShapes(ObjectType objectType)
	{
		ObjectTypeData otd = getOTD(objectType);
		if (otd==null) return false;
		
		return !otd.shapes.isEmpty();
	}
	
	public Vector<MapShape> getShapes(ObjectType objectType)
	{
		ObjectTypeData otd = getOTD(objectType);
		if (otd==null) return null;
		
		return otd.shapes;
	}

	public void setShowMarker(ObjectType objectType, boolean showMarker)
	{
		ObjectTypeData otd = getOTD(objectType);
		if (otd==null) return;
		
		otd.hideMarker = !showMarker;
		
		writeToFile();
	}

	public boolean shouldShowMarker(ObjectType objectType)
	{
		ObjectTypeData otd = getOTD(objectType);
		if (otd==null) return true;
		
		return !otd.hideMarker;
	}

	public void setSelectedShape(ObjectType objectType, MapShape shape)
	{
		ObjectTypeData otd = getOTD(objectType);
		if (otd==null) return;
		
		otd.selectedShape = shape;
		otd.hideMarker = shape!=null;
		
		writeToFile();
	}

	public MapShape getSelectedShape(ObjectType objectType)
	{
		ObjectTypeData otd = getOTD(objectType);
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
		private static final Color BGCOLOR_OBJECTTYPE_WITH_SHAPES = new Color(0xFFEB91);
		private static final long serialVersionUID = 3284148312241943876L;
		
		enum EventType{ HasGotFirstShape, RemovedSelectedShape, ChangedShapeName }
		
		record EditorEvent(EventType type, ObjectType objectType, MapShape shape)
		{
			private EditorEvent(EventType type, ObjectType objectType) { this(type, objectType, null); }
		}
		
		interface EditorListener
		{
			void notifyEvent(EditorEvent event);
		}
		
		private final LineEditor lineEditor;
		private final ObjectTypes objectTypes;
		private final MapShapes mapShapes;
		private final JPanel leftPanel;
		private JComponent lineEditorOptionsPanel;
		private ObjectType selectedObjectType;
		private Vector<MapShape> selectedShapes;
		private MapShape selectedShape;
		private final MapShapesEditorOptionsPanel mapShapesEditorOptionsPanel;
		private final EditorClipboard editorClipboard;
		private final EditorListener listener;
		
		public Editor(Window parent, String title, MapShapes mapShapes, ObjectTypes objectTypes, EditorListener listener)
		{
			super(parent, title, ModalityType.MODELESS, true);
			this.objectTypes = objectTypes;
			this.listener = listener;
			this.mapShapes = Objects.requireNonNull(mapShapes);
			selectedObjectType = null;
			selectedShapes = null;
			selectedShape = null;
			editorClipboard = new EditorClipboard();
			
			leftPanel = new JPanel(new BorderLayout(3,3));
			
			lineEditor = new LineEditor(new Rectangle2D.Double(-5,-5,10,10), new LineEditorContext(), new LineEditorBackground());
			lineEditorOptionsPanel = lineEditor.getInitialOptionsPanel();
			
			mapShapesEditorOptionsPanel = new MapShapesEditorOptionsPanel();
			
			leftPanel.add(mapShapesEditorOptionsPanel,BorderLayout.NORTH);
			leftPanel.add(lineEditorOptionsPanel,BorderLayout.CENTER);
			
			JPanel editorViewPanel = new JPanel(new BorderLayout(3,3));
			editorViewPanel.setBorder(BorderFactory.createTitledBorder("Shape"));
			editorViewPanel.add(lineEditor.getEditorView(),BorderLayout.CENTER);
			
			JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
			contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			contentPane.setLeftComponent(leftPanel);
			contentPane.setRightComponent(editorViewPanel);
			contentPane.setResizeWeight(0);
			
			createGUI(
				contentPane,
				GUI.createButton("Fit view to content", true, e->lineEditor.fitViewToContent(LineEditorBackground.createMinViewSize())),
				GUI.createButton("Save all shapes data in file", GrayCommandIcons.IconGroup.Save, true, e->this.mapShapes.writeToFile()),
				GUI.createButton("Close", true, e->closeDialog())
			);

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
			
			lineEditor.init();
		}
		
		private class MapShapesEditorOptionsPanel extends JPanel
		{
			private static final long serialVersionUID = 6491378825455249752L;
			
			private final JComboBox<ObjectType> cmbbxObjectTypes;
			private final JComboBox<MapShape> cmbbxMapShapes;
			private final ShapeButtonsPanel shapeButtonsPanel;
			
			MapShapesEditorOptionsPanel()
			{
				super(new GridBagLayout());
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				
				Tables.NonStringRenderer<ObjectType> cmbbxObjectTypesRenderer = new Tables.NonStringRenderer<>(
						obj -> obj==null ? "" : ((ObjectType)obj).getName()
				);
				cmbbxObjectTypesRenderer.setBackgroundColorizer(obj -> {
					if (obj instanceof ObjectType && Editor.this.mapShapes.hasShapes((ObjectType) obj))
						return BGCOLOR_OBJECTTYPE_WITH_SHAPES;
					return null;
				});
				
				cmbbxObjectTypes = new JComboBox<>(objectTypes.getListSortedByName());
				cmbbxObjectTypes.setRenderer(cmbbxObjectTypesRenderer);
				cmbbxMapShapes = new JComboBox<>();
				
				shapeButtonsPanel = new ShapeButtonsPanel(this);
				
				cmbbxObjectTypes.addActionListener(e->{
					int index = cmbbxObjectTypes.getSelectedIndex();
					selectedObjectType = index<0 ? null : cmbbxObjectTypes.getItemAt(index);
					//System.out.printf("ObjectType selected: %s%n", selectedObjectType==null ? "-- none --" : selectedObjectType.getName());
					ObjectTypeData otd = mapShapes.getOTD(selectedObjectType);
					selectedShapes = otd==null ? null : otd.shapes;
					updateCmbbxMapShapes(
						otd==null
							? null
							: otd.selectedShape!=null
								? otd.selectedShape
								: otd.shapes.isEmpty()
									? null :
									otd.shapes.firstElement()
					);
					shapeButtonsPanel.updateButtons();
				});
				cmbbxMapShapes.addActionListener(e->{
					int index = cmbbxMapShapes.getSelectedIndex();
					selectedShape = index<0 ? null : cmbbxMapShapes.getItemAt(index);
					//System.out.printf("Shape selected: %s%n", selectedShape==null ? "-- none --" : selectedShape.label);
					updateLineEditor();
					shapeButtonsPanel.updateButtons();
				});
				
				c.weightx = 0; c.gridwidth = 1;
				add(new JLabel("Object Type :  "), c);
				c.weightx = 1; c.gridwidth = GridBagConstraints.REMAINDER;
				add(cmbbxObjectTypes, c);
				
				c.weightx = 0; c.gridwidth = 1;
				add(new JLabel("Map Shapes :  "), c);
				c.weightx = 1; c.gridwidth = GridBagConstraints.REMAINDER;
				add(cmbbxMapShapes, c);
				
				c.weightx = 1; c.gridwidth = GridBagConstraints.REMAINDER;
				add(shapeButtonsPanel, c);
				
			}

			void repaintMapShapesCmbbx()
			{
				cmbbxMapShapes.repaint();
			}

			private void updateCmbbxMapShapes(MapShape newSelectedShape)
			{
				Vector<MapShape> displayedShapes = selectedShapes==null ? new Vector<>() : selectedShapes;
				cmbbxMapShapes.setModel(new DefaultComboBoxModel<>(displayedShapes));
				setSelectedShape(newSelectedShape);
			}

			void setSelectedShape(MapShape newSelectedShape)
			{
				cmbbxMapShapes.setSelectedItem(newSelectedShape);
			}

			void setSelectedObjectType(ObjectType objectType)
			{
				cmbbxObjectTypes.setSelectedItem(objectType);
			}

			void setObjectTypes(Vector<ObjectType> list, ObjectType selectedObjectType)
			{
				cmbbxObjectTypes.setModel(new DefaultComboBoxModel<>(list));
				setSelectedObjectType(selectedObjectType);
			}
		}
		
		private class ShapeButtonsPanel extends JPanel
		{
			private static final long serialVersionUID = 6371619802931430284L;
			
			private final JButton btnNewShape;
			private final JButton btnDeleteShape;
			private final JButton btnChangeShapeName;
			private final JButton btnCopyForms;
			private final JButton btnPasteForms;
			private final JButton btnCopyGuideLines;
			private final JButton btnPasteGuideLines;
			private final MapShapesEditorOptionsPanel mainOptionsPanel;

			ShapeButtonsPanel(MapShapesEditorOptionsPanel mainOptionsPanel)
			{
				super(new GridBagLayout());
				this.mainOptionsPanel = mainOptionsPanel;
				GridBagConstraints c;
				
				btnNewShape = GUI.createButton("New Shape", GrayCommandIcons.IconGroup.Add, false, e->{
					if (selectedObjectType==null) { System.err.println("ERROR: Can't create new shape, because no ObjectType is selected."); return; }
					createNewShape(selectedObjectType);
					mapShapes.writeToFile();
					updateButtons();
				});
				btnDeleteShape = GUI.createButton("Delete Shape", GrayCommandIcons.IconGroup.Delete, false, e->{
					if (selectedShape     ==null) { System.err.println("ERROR: Can't delete shape, because no shape is selected."); return; }
					if (selectedObjectType==null) { System.err.println("ERROR: Can't delete shape, because no ObjectType is selected."); return; }
					boolean wasDeleted = deleteShape(selectedObjectType, selectedShape);
					if (wasDeleted)
						mapShapes.writeToFile();
					updateButtons();
				});
				btnChangeShapeName = GUI.createButton("Change Shape Name", false, e->{
					if (selectedShape==null) { System.err.println("ERROR: Can't change shape name, because no shape is selected."); return; }
					String newName = askForShapeName(selectedShape.label, selectedShapes);
					if (newName!=null)
					{
						selectedShape.label = newName;
						this.mainOptionsPanel.repaintMapShapesCmbbx();
						mapShapes.writeToFile();
						listener.notifyEvent(new EditorEvent(EventType.ChangedShapeName, selectedObjectType, selectedShape));
					}
				});
				
				btnCopyForms = GUI.createButton("Copy Forms", GrayCommandIcons.IconGroup.Copy, false, e->{
					if (selectedShape!=null)
					{
						editorClipboard.set(EditorClipboard.copy(selectedShape.forms));
						updateButtons();
					}
				});
				btnCopyGuideLines = GUI.createButton("Copy GuideLines", GrayCommandIcons.IconGroup.Copy, false, e->{
					if (selectedShape!=null)
					{
						editorClipboard.set(EditorClipboard.copy(selectedShape.guideLines));
						updateButtons();
					}
				});
				
				btnPasteForms      = GUI.createButton("Paste Forms"     , GrayCommandIcons.IconGroup.Paste, false, e->{
					pasteSomething(
							"Forms", "forms",
							(clearFirst, shape) -> {
								if (clearFirst) shape.forms.clear();
								shape.forms.addAll(editorClipboard.getForms());
							},
							shape -> shape.forms.isEmpty()
					);
				});
				btnPasteGuideLines = GUI.createButton("Paste GuideLines", GrayCommandIcons.IconGroup.Paste, false, e->{
					pasteSomething(
							"GuideLines", "guide lines",
							(clearFirst, shape) -> {
								if (clearFirst)
									shape.guideLines.replace(editorClipboard.getGuideLines());
								else
									shape.guideLines.add    (editorClipboard.getGuideLines());
							},
							shape -> shape.guideLines.isEmpty()
					);
				});
				
				JPanel firstRow = new JPanel(new GridBagLayout());
				c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				c.weightx = 1;
				firstRow.add(btnNewShape,c);
				firstRow.add(btnDeleteShape,c);
				firstRow.add(btnChangeShapeName,c);
				
				c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				c.weightx = 1;
				
				c.gridwidth = GridBagConstraints.REMAINDER;
				add(firstRow,c);
				
				c.gridwidth = 1;
				add(btnCopyForms,c);
				c.gridwidth = GridBagConstraints.REMAINDER;
				add(btnPasteForms,c);
				
				c.gridwidth = 1;
				add(btnCopyGuideLines,c);
				c.gridwidth = GridBagConstraints.REMAINDER;
				add(btnPasteGuideLines,c);
			}

			void updateButtons()
			{
				btnNewShape       .setEnabled(selectedObjectType!=null);
				btnChangeShapeName.setEnabled(selectedShape!=null);
				btnDeleteShape    .setEnabled(selectedShape!=null);
				btnCopyForms      .setEnabled(selectedShape!=null && !selectedShape.forms     .isEmpty());
				btnCopyGuideLines .setEnabled(selectedShape!=null && !selectedShape.guideLines.isEmpty());
				btnPasteForms     .setEnabled(selectedObjectType!=null && editorClipboard.hasForms());
				btnPasteGuideLines.setEnabled(selectedObjectType!=null && editorClipboard.hasGuideLines());
			}
			
			private interface PasteAction
			{
				void pasteIntoShape(boolean clearFirst, MapShape shape);
			}

			private enum ItemsIntoExistingShapeOption
			{
				NewShape("Create a new shape"),
				Replace("Replace existing %s in selected shape"),
				Add("Add %s to selected shape"),
				;
				private final String formatStr;
				ItemsIntoExistingShapeOption(String formatStr) { this.formatStr = formatStr; }
				@Override public String toString() { return formatStr; }
				
				record Option(ItemsIntoExistingShapeOption value, String label) {
					@Override public String toString() { return label; }
				}
				
				static Option[] buildArray(String itemLabelInText) {
					return Arrays.stream(values())
							.map(opt -> new Option(opt, String.format(opt.formatStr, itemLabelInText)))
							.toArray(Option[]::new);
				}
				
				Option getFrom(Option[] arr) {
					for (Option opt : arr)
						if (opt.value==this)
							return opt;
					return null;
				}
			}

			private void pasteSomething(String itemLabelInTitle, String itemLabelInText, PasteAction pasteAction, Function<MapShape,Boolean> isEmpty)
			{
				if (selectedObjectType==null) return;
				
				MapShape targetShape = selectedShape;
				boolean clearFirst = false;
				
				if (selectedShape==null)
				{
					String title = "Create New Shape";
					String[] message = new String[] {
							"No shape is selected.",
							String.format( "A new shape will be created for ObjectType \"%s\".", selectedObjectType.getName() ),
							"Do you want to proceed?"
					};
					if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE))
						return;
					
					targetShape = createNewShape(selectedObjectType);
				}
				else if (!isEmpty.apply(selectedShape))
				{
					String title = "Existing "+itemLabelInTitle;
					String[] message = new String[] {
							String.format("There are %s in selected shape \"%s\".", itemLabelInText, selectedShape.label),
							"What do you want to do?"
					};
					
					ItemsIntoExistingShapeOption.Option[] options = ItemsIntoExistingShapeOption.buildArray(itemLabelInText);
					Object result = JOptionPane.showInputDialog(
							this, message, title,
							JOptionPane.QUESTION_MESSAGE, null,
							options, ItemsIntoExistingShapeOption.NewShape.getFrom(options)
					);
					
					if (result instanceof ItemsIntoExistingShapeOption.Option) // && result!=null
					{
						ItemsIntoExistingShapeOption decision = ((ItemsIntoExistingShapeOption.Option) result).value;
						switch (decision)
						{
							case NewShape:
								targetShape = createNewShape(selectedObjectType);
								break;
								
							case Replace:
								clearFirst = true;
								break;
								
							case Add:
								break;
						}
					}
				}
				pasteAction.pasteIntoShape(clearFirst,targetShape);
				mainOptionsPanel.setSelectedShape(targetShape); // to update list of forms
			}
		}
		
		private static class EditorClipboard
		{
			private Vector<Form>      copiedForms             = null;
			private GuideLinesStorage copiedGuideLinesStorage = null;

			boolean hasForms     () { return copiedForms             != null && !copiedForms            .isEmpty(); }
			boolean hasGuideLines() { return copiedGuideLinesStorage != null && !copiedGuideLinesStorage.isEmpty(); }
			
			Vector<Form>      getForms     () { return copiedForms             == null ? null : copy(copiedForms            ); }
			GuideLinesStorage getGuideLines() { return copiedGuideLinesStorage == null ? null : copy(copiedGuideLinesStorage); }
			
			void set(Vector<Form>      copiedForms)             { this.copiedForms             = copiedForms            ; }
			void set(GuideLinesStorage copiedGuideLinesStorage) { this.copiedGuideLinesStorage = copiedGuideLinesStorage; }
			
			static Vector<Form>      copy(Vector<Form>      forms) { return LineEditor.copy(forms); }
			static GuideLinesStorage copy(GuideLinesStorage gls  ) { return new GuideLinesStorage(gls); }
		}
		
		private class LineEditorContext implements LineEditor.Context
		{
			@Override public void switchOptionsPanel(JComponent panel)
			{
				if (lineEditorOptionsPanel!=null) leftPanel.remove(lineEditorOptionsPanel);
				lineEditorOptionsPanel = panel;
				if (lineEditorOptionsPanel!=null) leftPanel.add(lineEditorOptionsPanel,BorderLayout.CENTER);
				leftPanel.revalidate();
				leftPanel.repaint();
			}
			
			@Override public boolean canModifyFormsList()
			{
				return selectedObjectType!=null;
			}

			@Override public void guideLinesChanged(LineEditor.GuideLinesChangedEvent e)
			{
				//System.out.printf("MapShapes.Editor.guideLinesChanged: [%s] \"%s\"%n", e.type(), e.caller());
				switch (e.type())
				{
					case Added:
					case Changed:
					case Removed:
						Editor.this.mapShapes.writeToFile();
						break;
				}
			}
			@Override public void formsChanged(LineEditor.FormsChangedEvent e)
			{
				//System.out.printf("MapShapes.Editor.formsChanged: [%s] \"%s\"%n", e.type(), e.caller());
				switch (e.type())
				{
					case Added:
					case Removed:
					case Changed:
						Form[] newFormsList = e.newFormsList();
						if (newFormsList!=null) replaceForms(newFormsList);
						Editor.this.mapShapes.writeToFile();
						break;
				}
			}
			
			private void replaceForms(Form[] forms)
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
					//Editor.this.mapShapes.writeToFile();
					if (isNewShape)
						updateLineEditor();
				}
				else
					throw new IllegalStateException();
			}
		}
		
		private static class LineEditorBackground implements EditorViewFeature
		{
			private static final Color COLOR_LINE = new Color(0x70FF00);
			private static final Color COLOR_TEXT = new Color(0x70FF00);
			
			@Override
			public void draw(Graphics2D g2, int x, int y, int width, int height, ViewState viewState, Iterable<? extends FeatureLineForm> forms)
			{
				g2.setColor(COLOR_TEXT);
				TextDrawer td = new TextDrawer(g2, viewState, 0.3);
				td.drawText("FRONT",  0,  2, 0);
				td.drawText("LEFT" ,  2,  0,  Math.PI/2);
				td.drawText("RIGHT", -2,  0, -Math.PI/2);
				td.drawText("BACK" ,  0, -2, 0);
				td.cleanUp();
				
				double lineWidth = viewState.convertLength_LengthToScreenF(0.2);
				Stroke origStroke = g2.getStroke();
				g2.setStroke(new BasicStroke((float) lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				
				g2.setColor(COLOR_LINE);
				drawLine(g2, viewState,  0.0,0.0, 0,1.5);
				drawLine(g2, viewState,  0.3,1.2, 0,1.5);
				drawLine(g2, viewState, -0.3,1.2, 0,1.5);
				
				g2.setStroke(origStroke);
			}
			
			static Rectangle2D.Double createMinViewSize()
			{
				return new Rectangle2D.Double( -2.2, -2.2, 4.4, 4.4 );
			}

			private void drawLine(Graphics2D g2, ViewState viewState, double x1, double y1, double x2, double y2)
			{
				int x1_scr = viewState.convertPos_AngleToScreen_LongX(x1);
				int y1_scr = viewState.convertPos_AngleToScreen_LatY (y1);
				int x2_scr = viewState.convertPos_AngleToScreen_LongX(x2);
				int y2_scr = viewState.convertPos_AngleToScreen_LatY (y2);
				g2.drawLine(x1_scr, y1_scr, x2_scr, y2_scr);
			}

			private static class TextDrawer
			{
				private final ViewState viewState;
				private final Graphics2D g2;
				private final Font font;
				private final FontRenderContext frc;
				private final AffineTransform origTransform;
				private Font origFont;

				TextDrawer(Graphics2D g2, ViewState viewState, double fontSize)
				{
					this.viewState = viewState;
					this.g2 = g2;
					frc = this.g2.getFontRenderContext();
					origTransform = this.g2.getTransform();
					origFont = this.g2.getFont();
					
					double fontSize_scr = this.viewState.convertLength_LengthToScreenF(fontSize);
					font = origFont.deriveFont((float) fontSize_scr).deriveFont(Font.BOLD);
					this.g2.setFont(font);
				}
				
				void cleanUp()
				{
					g2.setTransform(origTransform);
					g2.setFont(origFont);
				}
				
				void drawText(String text, double centerX, double centerY, double rotation)
				{
					double centerX_scr = this.viewState.convertPos_AngleToScreen_LongXf(centerX);
					double centerY_scr = this.viewState.convertPos_AngleToScreen_LatYf (centerY);
					
					AffineTransform transform = new AffineTransform(origTransform);
					transform.translate( centerX_scr, centerY_scr );
					transform.rotate( rotation );
					g2.setTransform(transform);
					
					Rectangle2D sb = font.getStringBounds(text, frc);
					g2.drawString(text, Math.round(-sb.getCenterX()), Math.round(-sb.getCenterY()));
					
				}
			}


			@Override public void setEditorView(Component editorView) {}
			@Override public void addToEditorViewContextMenu(JPopupMenu contextMenu) {}
			@Override public void prepareContextMenuToShow() {}
		}

		private void updateLineEditor()
		{
			if (selectedShape==null)
			{
				lineEditor.setForms(null);
				lineEditor.setGuideLines(null);
			}
			else
			{
				lineEditor.setForms(selectedShape.forms.toArray(Form[]::new));
				lineEditor.setGuideLines(selectedShape.guideLines);
			}
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
			
			boolean removed = otd.shapes.remove(shape);
			if (removed)
			{
				selectedShapes = otd.shapes;
				mapShapesEditorOptionsPanel.updateCmbbxMapShapes(null);
				
				if (otd.selectedShape == shape)
				{
					otd.selectedShape = null;
					listener.notifyEvent(new EditorEvent(EventType.RemovedSelectedShape, objectType));
				}
			}
			
			return removed;
		}

		private MapShape createNewShape(ObjectType objectType)
		{
			if (objectType==null) throw new IllegalArgumentException();
			
			ObjectTypeData otd = mapShapes.data.get(objectType.id);
			if (otd==null) mapShapes.data.put(objectType.id, otd = new ObjectTypeData());
			
			int oldSize = otd.shapes.size();
			int index = oldSize+1;
			String name = String.format("Shape%03d", index);
			while (!isUniqueName(name, otd.shapes))
				name = String.format("Shape%03d", ++index);
			
			MapShape newShape = new MapShape(name);
			otd.shapes.add(newShape);
			
			selectedShapes = otd.shapes;
			mapShapesEditorOptionsPanel.updateCmbbxMapShapes(newShape);
			
			if (oldSize==0)
				listener.notifyEvent(new EditorEvent(EventType.HasGotFirstShape, objectType));
			
			return newShape;
		}

		public void showDialog(ObjectType objectType)
		{
			mapShapesEditorOptionsPanel.setSelectedObjectType(objectType);
			showDialog(Position.PARENT_CENTER);
		}

		public void updateAfterNewObjectTypes()
		{
			Vector<ObjectType> list = objectTypes.getListSortedByName();
			mapShapesEditorOptionsPanel.setObjectTypes(list,selectedObjectType);
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
