package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

class FarWreckAreas
{
	private static FarWreckAreas instance = null;
	static FarWreckAreas getInstance()
	{
		return instance == null
				? instance = new FarWreckAreas()
				: instance;
	}
	
	private final Vector<WreckArea> areas;
	private WreckArea editableArea;
	
	private FarWreckAreas()
	{
		areas = new Vector<>();
		editableArea = null;
	}

	Vector<WreckArea> getAreas()
	{
		return areas;
	}

	void addEmptyArea()
	{
		areas.add(new WreckArea());
	}

	WreckArea getEditableArea() { return editableArea; }
	void setEditableArea(WreckArea editableArea) { this.editableArea = editableArea; }

	private static String getValue(String line, String prefix) {
		if (line.startsWith(prefix))
			return line.substring(prefix.length());
		return null;
	}

	private static Point2D.Double parsePoint(String xStr, String yStr)
	{
		double x,y;
		try { x = Double.parseDouble(xStr); } catch (NumberFormatException e) { System.err.printf("NumberFormatException while parsing \"%s\": %s%n", xStr, e.getMessage()); return null; }
		try { y = Double.parseDouble(yStr); } catch (NumberFormatException e) { System.err.printf("NumberFormatException while parsing \"%s\": %s%n", yStr, e.getMessage()); return null; }
		return new Point2D.Double(x,y);
	}

	void readFromFile()
	{
		File file = new File(PlanetCrafterSaveGameViewer.FILE_FARWRECKAREAS);
		if(!file.isFile()) return;
		
		areas.clear();
		editableArea = null;
		
		System.out.printf("Read FarWreckAreas from file \"%s\" ...%n", file.getAbsolutePath());
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			
			String line, valueStr;
			WreckArea currentWreckArea = null;
			
			while ( (line=in.readLine())!=null )
			{
				if (line.isBlank()) continue;
				
				if (line.equals("[WreckArea]")) {
					currentWreckArea = new WreckArea();
					areas.add(currentWreckArea);
				};
				
				if (currentWreckArea != null)
				{
					if ( (valueStr=getValue(line,"P "))!=null)
					{
						int pos = valueStr.indexOf('|');
						if (pos>0) {
							String xStr = valueStr.substring(0,pos);
							String yStr = valueStr.substring(pos+1);
							Point2D.Double p = parsePoint(xStr, yStr);
							if (p==null)
								System.err.printf("Can't parse \"%s\" to XY coordinates", valueStr);
							else
								currentWreckArea.points.add(p);
						}
					}
				}
			}
			
		} catch (FileNotFoundException ex) {
			//ex.printStackTrace();
		} catch (IOException ex) {
			System.err.printf("IOException while reading FarWreckAreas: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}
	
	void writeToFile()
	{
		File file = new File(PlanetCrafterSaveGameViewer.FILE_FARWRECKAREAS); 	
		
		System.out.printf("Write FarWreckAreas to file \"%s\" ...%n", file.getAbsolutePath());
		
		try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
			
			for (WreckArea area : areas)
			{
				out.println("[WreckArea]");
				for (Point2D.Double p : area.points)
					out.printf(Locale.ENGLISH, "P %1.4f|%1.4f%n", p.x, p.y);
				out.println();
			}
			
		} catch (IOException ex) {
			System.err.printf("IOException while writing FarWreckAreas: %s%n", ex.getMessage());
			//ex.printStackTrace();
		}
		
		System.out.printf("Done%n");
	}

	static class WreckArea
	{
		final Vector<Point2D.Double> points;
		
		WreckArea()
		{
			points = new Vector<>();
		}
		
		void addPoint(Data.Coord3 pos)
		{
			if (pos==null) return;
			points.add(new Point2D.Double( pos.getMapX(), pos.getMapY() ));
			sortPoints();
		}

		private void sortPoints()
		{
			Point2D.Double center = computeCenter(points);
			List<Point2D.Double> sortedPoints = points
				.stream()
				.map(p->new SortablePoint(p, Math.atan2(p.y-center.y, p.x-center.x)))
				.sorted(Comparator.comparing(sp->sp.angle))
				.map(sp->sp.p)
				.toList();
			points.clear();
			points.addAll(sortedPoints);
		}
		
		record SortablePoint(Point2D.Double p, double angle) {}
		
		private static Point2D.Double computeCenter(Vector<Point2D.Double> points)
		{
			Point2D.Double min = null;
			Point2D.Double max = null;
			for (Point2D.Double p : points) {
				if (min==null)
					min = new Point2D.Double( p.x, p.y );
				else {
					min.x = Math.min(min.x, p.x);
					min.y = Math.min(min.y, p.y);
				}
				if (max==null)
					max = new Point2D.Double( p.x, p.y );
				else {
					max.x = Math.max(max.x, p.x);
					max.y = Math.max(max.y, p.y);
				}
			}
			if (min==null || max==null) return null;
			return new Point2D.Double( (max.x+min.x)/2, (max.y+min.y)/2 );
		}
	}
}
