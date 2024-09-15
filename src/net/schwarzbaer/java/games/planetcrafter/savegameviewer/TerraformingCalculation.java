package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.util.Comparator;
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

class TerraformingCalculation
{
	private final Map<PhysicalValue,TerraformingAspect> aspects;

	TerraformingCalculation()
	{
		aspects = new EnumMap<>(PhysicalValue.class);
		for (PhysicalValue phVal : PhysicalValue.values())
			aspects.put(phVal, new TerraformingAspect(phVal));
	}

	TerraformingAspect getAspect(PhysicalValue physicalValue)
	{
		return aspects.get(physicalValue);
	}
	
	void clearData(PhysicalValue physicalValue)
	{
		getAspect(physicalValue).clearData();
	}
	
	void clearData()
	{
		aspects.forEach((phVal,aspect)->{
			aspect.clearData();
		});
	}

	void foreachAWO(WorldObject worldObject, boolean skipNulls, BiConsumer<PhysicalValue,ActiveWorldObject> action)
	{
		aspects.forEach((phVal,aspect)->{
			ActiveWorldObject awo = aspect.getAWO(worldObject);
			if (awo!=null || !skipNulls)
				action.accept(phVal, awo);
		});
	}

	void foreachAMO(WorldObject worldObject, boolean skipNulls, BiConsumer<PhysicalValue,ActiveMachineOptimizer> action)
	{
		aspects.forEach((phVal,aspect)->{
			ActiveMachineOptimizer amo = aspect.getAMO(worldObject);
			if (amo!=null || !skipNulls)
				action.accept(phVal, amo);
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
		int capacity,
		double fuseMulti,
		Vector<NearActiveWorldObject> nearAWOs
	) {
		ActiveMachineOptimizer(WorldObject wo, Coord3 position, double range, int capacity, double fuseMulti) {
			this(wo, position, range, capacity, fuseMulti, new Vector<>());
		}
	}
	
	record NearMachineOptimizer (
		ActiveMachineOptimizer amo,
		double distance
	) {}
	
	record NearActiveWorldObject (
		ActiveWorldObject awo,
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
		
		void addMachineOptimizer(ActiveMachineOptimizer amo, double distance)
		{
			nearMachineOptimizers.add(new NearMachineOptimizer(amo, distance));
		}
	}
	
	static class TerraformingAspect
	{
		private final PhysicalValue physicalValue;
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

		int    getNumberOfBoosterRockets() { return boosterRockets.size(); }
		double getBoosterMultiplier     () { return boosterMultiplier;     }
		double getTotalSum              () { return totalSum;              }
		double getTotalSumBoosted       () { return totalSumBoosted;       }

		ActiveWorldObject getAWO(WorldObject worldObject)
		{
			return activeWorldObjects.get(worldObject);
		}
		
		ActiveMachineOptimizer getAMO(WorldObject worldObject)
		{
			return machineOptimizers.get(worldObject);
		}

		void forEachAWO(BiConsumer<? super WorldObject, ? super ActiveWorldObject> action) {
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
					ActiveMachineOptimizer amo = computeMachineOptimizer(wo);
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
			
			// find ActiveWorldObjects near to an ActiveMachineOptimizer
			for (ActiveMachineOptimizer machOpt : machineOptimizers.values()) {
				machOpt.nearAWOs.clear();
				
				for (ActiveWorldObject aWoObj : activeWorldObjects.values()) {
					double distance = machOpt.position.getDistanceXYZ_m(aWoObj.wo.position);
					if (distance <= machOpt.range)
						machOpt.nearAWOs.add(new NearActiveWorldObject(aWoObj, distance));
				}
				
				machOpt.nearAWOs.sort(Comparator.comparing(nawo->nawo.distance));
				
				if (machOpt.capacity < machOpt.nearAWOs.size())
					machOpt.nearAWOs.setSize(machOpt.capacity);
			}
			
			// clear MachineOptimizer values in ActiveWorldObjects
			for (ActiveWorldObject awo : activeWorldObjects.values()) {
				awo.moMulti = null;
				awo.nearMachineOptimizers.clear();
			}
			
			// set MachineOptimizer values in ActiveWorldObjects near to an ActiveMachineOptimizer
			for (ActiveMachineOptimizer machOpt : machineOptimizers.values()) {
				for (NearActiveWorldObject nawo : machOpt.nearAWOs) {
					nawo.awo.nearMachineOptimizers.add(new NearMachineOptimizer(machOpt, nawo.distance));
					if (nawo.awo.moMulti==null) nawo.awo.moMulti = 0.0;
					nawo.awo.moMulti += machOpt.fuseMulti; // values of multiple MOs will be summarized
				}
			}
			
			totalSum = 0;
			for (ActiveWorldObject aWoObj : activeWorldObjects.values()) {
				double multiplier;
				if (aWoObj.multiplier!=null && aWoObj.moMulti!=null)
					multiplier = aWoObj.multiplier + aWoObj.moMulti;
				else
					multiplier =
							aWoObj.multiplier!=null
								? aWoObj.multiplier
								: aWoObj.moMulti!=null
										? aWoObj.moMulti
										: 1;
				totalSum += aWoObj.baseValue * multiplier;
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

		private ActiveMachineOptimizer computeMachineOptimizer(WorldObject wo)
		{
			if (wo                == null) return null;
			if (wo.position       == null) return null;
			if (wo.list           == null) return null;
			if (wo.list.worldObjs == null) return null;
			if (wo.objectType     == null) return null;
			ObjectType ot = wo.objectType;
			
			if (!ot.isMachineOptomizer) return null;
			if ( ot.moRange    == null) return null;
			if ( ot.moCapacity == null) return null;
			
			
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
			
			return new ActiveMachineOptimizer(wo, wo.position, ot.moRange, ot.moCapacity, moMulti);
		}
	}
}
