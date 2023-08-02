package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

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
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.lib.gui.StandardDialog;
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
	}

	static class Editor extends StandardDialog
	{
		private static final long serialVersionUID = 3284148312241943876L;
	
		public Editor(Window parent, String title, MapShapes mapShapes)
		{
			super(parent, title, ModalityType.MODELESS, true);
			// TODO Auto-generated constructor stub
		}
	
		public void showDialog(ObjectType objectType)
		{
			// TODO Auto-generated method stub
			showDialog(Position.PARENT_CENTER);
		}
		
	}
	
}
