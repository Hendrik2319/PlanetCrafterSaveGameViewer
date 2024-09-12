package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.function.BiConsumer;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.PhysicalValue;

class TerraformingCalculation // TODO: move whole terraforming calculation from TerraformingPanel
{
	private static TerraformingCalculation instance = null;
	static TerraformingCalculation getInstance()
	{
		return instance == null ? instance = new TerraformingCalculation() : instance;
	}
	
	private final Map<PhysicalValue,TerraformingAspect> data;

	TerraformingCalculation()
	{
		data = new EnumMap<>(PhysicalValue.class);
		for (PhysicalValue phVal : PhysicalValue.values())
			data.put(phVal, new TerraformingAspect(phVal));
	}

	TerraformingAspect getAspect(PhysicalValue physicalValue)
	{
		return data.get(physicalValue);
	}
	
	void clearData(PhysicalValue physicalValue)
	{
		getAspect(physicalValue).clearData();
	}
	
	void clearData()
	{
		data.forEach((phVal,aspect)->{
			aspect.clearData();
		});
	}

	WorldObjectData getWOData(PhysicalValue physicalValue, WorldObject worldObject)
	{
		return getAspect(physicalValue).getWOData(worldObject);
	}

	void foreachWOData(WorldObject worldObject, BiConsumer<PhysicalValue,WorldObjectData> action)
	{
		data.forEach((phVal,aspect)->{
			WorldObjectData woData = aspect.getWOData(worldObject);
			if (woData!=null)
				action.accept(phVal, woData);
		});
	}
	
	static class TerraformingAspect
	{
		final PhysicalValue physicalValue;
		private final Map<WorldObject,WorldObjectData> worldObjectData;

		TerraformingAspect(PhysicalValue physicalValue)
		{
			this.physicalValue = physicalValue;
			worldObjectData = new HashMap<>();
		}

		WorldObjectData getOrCreateWOData(WorldObject worldObject)
		{
			return worldObjectData.computeIfAbsent(worldObject, k->new WorldObjectData());
		}

		WorldObjectData getWOData(WorldObject worldObject)
		{
			return worldObjectData.get(worldObject);
		}

		void clearData()
		{
			worldObjectData.clear();
		}
	}
	
	record NearMachineOptimizer (
		WorldObject mo,
		double distance
	) {}
	
	static class WorldObjectData
	{
		final Vector<NearMachineOptimizer> nearMachineOptimizers;
		
		WorldObjectData()
		{
			nearMachineOptimizers = new Vector<>();
		}
		
		void addMachineOptimizer(WorldObject mo, double distance)
		{
			nearMachineOptimizers.add(new NearMachineOptimizer(mo, distance));
		}
		
	}
}
