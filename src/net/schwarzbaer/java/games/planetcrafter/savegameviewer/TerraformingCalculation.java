package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Coord3;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypes.ObjectType;
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

	ActiveWorldObject getWOData(PhysicalValue physicalValue, WorldObject worldObject)
	{
		return getAspect(physicalValue).getWOData(worldObject);
	}

	void foreachWOData(WorldObject worldObject, BiConsumer<PhysicalValue,ActiveWorldObject> action)
	{
		data.forEach((phVal,aspect)->{
			ActiveWorldObject woData = aspect.getWOData(worldObject);
			if (woData!=null)
				action.accept(phVal, woData);
		});
	}
	
	static record BoosterRocket (
			WorldObject wo,
			Double boosterMultiplier
	) {}
	
	static record ActiveMachineOptimizer (
		WorldObject wo,
		Coord3 position,
		double range,
		double fuseMulti
	) {}
	
	record NearMachineOptimizer (
		WorldObject wo,
		double distance
	) {}
	
	static class ActiveWorldObject
	{
		final WorldObject wo;
		final double baseValue;
		final Double multiplier;
		      Double moMulti;
		final Vector<NearMachineOptimizer> nearMachineOptimizers;

		ActiveWorldObject(WorldObject wo, double baseValue, Double multiplier)
		{
			this.wo = Objects.requireNonNull(wo);
			this.baseValue = baseValue;
			this.multiplier = multiplier;
			nearMachineOptimizers = new Vector<>();
			moMulti = null;
		}
		
		void addMachineOptimizer(WorldObject wo, double distance)
		{
			nearMachineOptimizers.add(new NearMachineOptimizer(wo, distance));
		}
	}
	
	static class TerraformingAspect
	{
		final PhysicalValue physicalValue;
		private final Map<WorldObject,ActiveWorldObject> activeWorldObjects;
		private final Map<WorldObject,ActiveMachineOptimizer> machineOptimizers;
		private final Map<WorldObject,BoosterRocket> boosterRockets;
		private double totalSum;
		private double boosterMultiplier;
		private double totalSumBoosted;
		
		TerraformingAspect(PhysicalValue physicalValue)
		{
			this.physicalValue = physicalValue;
			activeWorldObjects = new HashMap<>();
			machineOptimizers  = new HashMap<>();
			boosterRockets     = new HashMap<>();
			totalSum = 0;
			boosterMultiplier = 0;
			totalSumBoosted = 0;
		}

		ActiveWorldObject getWOData(WorldObject worldObject)
		{
			return activeWorldObjects.get(worldObject);
		}
		
		void forEachActiveWorldObject(BiConsumer<? super WorldObject, ? super ActiveWorldObject> action) {
			activeWorldObjects.forEach(action);
		}

		void clearData()
		{
			activeWorldObjects.clear();
			machineOptimizers.clear();
			boosterRockets.clear();
			totalSum = 0;
			boosterMultiplier = 0;
			totalSumBoosted = 0;
		}

		void calculate(Vector<WorldObject> worldObjects)
		{
			clearData();
			
			for (WorldObject wo : worldObjects) {
				if (wo == null) continue;
				if (wo.objectType == null) continue;
				ObjectType ot = wo.objectType;
				
				if (ot.isBoosterRocketFor==physicalValue)
					boosterRockets.put(wo, new BoosterRocket(wo, ot.boosterMultiplier));
				
				if (wo.isInstalled())
				{
					ActiveMachineOptimizer amo = addMachineOptimizer(wo);
					if (amo != null)
						machineOptimizers.put(wo, amo);
					
					Double baseValue = physicalValue.getBaseValue.apply(ot);
					if (baseValue!=null)
					{
						if (ot.expectsMultiplierFor==physicalValue && physicalValue.isMultiplierBased)
						{
							Double multiplier = getMultiplierFromStoredObjects( wo, physicalValue.getMultiplier );
							if (multiplier!=null)
								activeWorldObjects.put(wo, new ActiveWorldObject(wo, baseValue, multiplier));
						}
						else
							activeWorldObjects.put(wo, new ActiveWorldObject(wo, baseValue, null));
					}
				}
			}
			
			// TODO: MO Capacity
			for (ActiveWorldObject aWoObj : activeWorldObjects.values()) {
				aWoObj.moMulti = null;
				for (ActiveMachineOptimizer machOpt : machineOptimizers.values()) {
					double distance = machOpt.position.getDistanceXYZ_m(aWoObj.wo.position);
					if (distance <= machOpt.range) {
						aWoObj.nearMachineOptimizers.add(new NearMachineOptimizer(machOpt.wo, distance));
						if (aWoObj.moMulti==null) aWoObj.moMulti = 0.0;
						aWoObj.moMulti += machOpt.fuseMulti; // values of multiple MOs will be summarized
					}
				}
			}
			
			totalSum = 0;
			for (ActiveWorldObject aWoObj : activeWorldObjects.values()) {
				totalSum += aWoObj.baseValue
						* (aWoObj.multiplier==null ? 1 : aWoObj.multiplier)
						* (aWoObj.moMulti   ==null ? 1 : aWoObj.moMulti   );
			}
			
			Double boosterMultiplier = null;
			for (BoosterRocket booster : boosterRockets.values()) {
				if (booster.boosterMultiplier!=null) {
					if (boosterMultiplier == null)
						boosterMultiplier = 0.0;
					boosterMultiplier += booster.boosterMultiplier;
				}
			}
			this.boosterMultiplier = boosterMultiplier == null ? 1.0 : boosterMultiplier;
			
			totalSumBoosted = totalSum * this.boosterMultiplier;
		}

		private Double getMultiplierFromStoredObjects(WorldObject wo, Function<ObjectType, Double> getMultiplier)
		{
			if (wo                  == null) return null;
			if (wo.list             == null) return null;
			if (wo.list.worldObjs   == null) return null;
			if (wo.list.worldObjs.length==0) return null;
			
			ObjectType[] objectTypes = WorldObject.getObjectTypes(wo.list.worldObjs);
			return ObjectTypes.sumUpMultipliers(objectTypes, getMultiplier);
		}

		private ActiveMachineOptimizer addMachineOptimizer(WorldObject wo)
		{
			if (wo                == null) return null;
			if (wo.position       == null) return null;
			if (wo.list           == null) return null;
			if (wo.list.worldObjs == null) return null;
			if (wo.objectType     == null) return null;
			ObjectType ot = wo.objectType;
			
			if (!ot.isMachineOptomizer) return null;
			if ( ot.moRange    == null) return null;
			
			
			double moMulti = Double.NaN;
			for (WorldObject fuse : wo.list.worldObjs) {
				if (fuse            == null) continue;
				if (fuse.objectType == null) continue;
				ObjectType ot_fuse = fuse.objectType;
				
				if (ot_fuse.isMOFuse != physicalValue) continue;
				if (ot_fuse.moFuseMultiplier  == null) continue;
				
				if (Double.isNaN(moMulti)) moMulti = 0;
				moMulti += ot_fuse.moFuseMultiplier; // values of multiple fuses in one optimizer will be summarized
			}
			if (Double.isNaN(moMulti))
				return null;
			
			return new ActiveMachineOptimizer(wo, wo.position, ot.moRange, moMulti);
		}

		int getNumberOfBoosterRockets()
		{
			return boosterRockets.size();
		}

		double getBoosterMultiplier()
		{
			return boosterMultiplier;
		}

		double getTotalSum()
		{
			return totalSum;
		}

		double getTotalSumBoosted()
		{
			// TODO Auto-generated method stub
			return totalSumBoosted;
		}
	}
}
