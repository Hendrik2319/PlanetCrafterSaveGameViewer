package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Window;
import java.util.Vector;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
import net.schwarzbaer.java.lib.gui.StandardDialog;

class MapShapes
{
	public MapShapes()
	{
		
	}

	public boolean hasShapes(ObjectType objectType)
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	public Vector<MapShape> getShapes(ObjectType row)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void setShowMarker(ObjectType objectType, boolean showMarker)
	{
		// TODO Auto-generated method stub
		
	}

	public Boolean shouldShowMarker(ObjectType objectType)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void setSelectedShape(ObjectType objectType, MapShape shape)
	{
		// TODO Auto-generated method stub
		
	}

	public MapShape getSelectedShape(ObjectType objectType)
	{
		// TODO Auto-generated method stub
		return null;
	}

	static class MapShape
	{
		String label;		
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
