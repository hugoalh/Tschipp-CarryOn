/*
 * GNU Lesser General Public License v3
 * Copyright (C) 2024 Tschipp
 * mrtschipp@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package tschipp.carryon.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tschipp.carryon.CarryOnCommon;
import tschipp.carryon.Constants;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnData.CarryType;
import tschipp.carryon.common.carry.CarryOnDataManager;
import tschipp.carryon.common.carry.PickupHandler;
import tschipp.carryon.common.carry.PlacementHandler;
import tschipp.carryon.common.scripting.ScriptReloadListener;
import tschipp.carryon.config.ConfigLoader;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = Constants.MOD_ID)
public class CommonEvents
{
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onBlockClick(PlayerInteractEvent.RightClickBlock event)
	{
		if (event.isCanceled())
			return;

		Player player = event.getEntity();
		Level level = event.getLevel();
		BlockPos pos = event.getPos();

		if (level.isClientSide)
			return;

		boolean success = false;

		CarryOnData carry = CarryOnDataManager.getCarryData(player);
		if (!carry.isCarrying()) {
			if (PickupHandler.tryPickUpBlock((ServerPlayer) player, pos, level, (pState, pPos) -> {
				BlockEvent.BreakEvent breakEvent = new BreakEvent(level, pPos, pState, player);
				MinecraftForge.EVENT_BUS.post(breakEvent);
				return !breakEvent.isCanceled();
			})) {
				success = true;
			}
		} else {
			if (carry.isCarrying(CarryType.BLOCK)) {
				PlacementHandler.tryPlaceBlock((ServerPlayer) player, pos, event.getFace(), (pos2, state) -> {
					BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos2);
					EntityPlaceEvent event1 = new EntityPlaceEvent(snapshot, level.getBlockState(pos), player);
					MinecraftForge.EVENT_BUS.post(event1);
					return !event1.isCanceled();
				});
			} else {
				PlacementHandler.tryPlaceEntity((ServerPlayer) player, pos, event.getFace(), (pPos, toPlace) -> {
					if (toPlace instanceof Mob mob) {
						FinalizeSpawn checkSpawn = new FinalizeSpawn(mob, (ServerLevelAccessor) level, pPos.x, pPos.y, pPos.z, level.getCurrentDifficultyAt(new BlockPos((int) pPos.x, (int) pPos.y, (int) pPos.z)), EntitySpawnReason.EVENT, null, null, null);
						MinecraftForge.EVENT_BUS.post(checkSpawn);
						return event.getResult() != Result.DENY;
					}
					return true;
				});
			}
			success = true;
		}

		if (success) {
			event.setUseBlock(Event.Result.DENY);
			event.setUseItem(Event.Result.DENY);
			event.setCancellationResult(InteractionResult.SUCCESS);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onEntityRightClick(PlayerInteractEvent.EntityInteract event)
	{
		if (event.isCanceled())
			return;

		Player player = event.getEntity();
		Level level = event.getLevel();
		Entity target = event.getTarget();

		if (level.isClientSide)
			return;

		CarryOnData carry = CarryOnDataManager.getCarryData(player);
		if (!carry.isCarrying()) {
			if (PickupHandler.tryPickupEntity((ServerPlayer) player, target, (toPickup) -> {
				EntityPickupEvent pickupEvent = new EntityPickupEvent((ServerPlayer) player, toPickup);
				MinecraftForge.EVENT_BUS.post(pickupEvent);
				return !pickupEvent.isCanceled();
			})) {
				event.setCancellationResult(InteractionResult.SUCCESS);
				event.setCanceled(true);
				return;
			}
		} else if (carry.isCarrying(CarryType.ENTITY) || carry.isCarrying(CarryType.PLAYER)) {
			PlacementHandler.tryStackEntity((ServerPlayer) player, target);
		}
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event)
	{
		CarryOnCommon.registerCommands(event.getDispatcher());
	}

	@SubscribeEvent
	public static void onDatapackRegister(AddReloadListenerEvent event)
	{
		event.addListener(new ScriptReloadListener());
	}

	@SubscribeEvent
	public static void onDatapackSync(OnDatapackSyncEvent event)
	{
		ServerPlayer player = event.getPlayer();
		if (player == null) {
			for (ServerPlayer p : event.getPlayerList().getPlayers())
				ScriptReloadListener.syncScriptsWithClient(p);
		} else
			ScriptReloadListener.syncScriptsWithClient(player);
	}

	@SubscribeEvent
	public static void onTagsUpdate(TagsUpdatedEvent event)
	{
		ConfigLoader.onConfigLoaded(event.getRegistryAccess());
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event)
	{
		for (ServerPlayer player : event.getServer().getPlayerList().getPlayers())
			CarryOnCommon.onCarryTick(player);
	}

	@SubscribeEvent
	public static void onClone(Clone event)
	{
		if (!event.getOriginal().level().isClientSide)
			PlacementHandler.placeCarriedOnDeath((ServerPlayer) event.getOriginal(), (ServerPlayer) event.getEntity(), event.isWasDeath());
	}

	@SubscribeEvent
	public static void harvestSpeed(BreakSpeed event)
	{
		if (!CarryOnCommon.onTryBreakBlock(event.getEntity()))
			event.setNewSpeed(0);
	}

	@SubscribeEvent
	public static void attackEntity(AttackEntityEvent event)
	{
		if(!CarryOnCommon.onAttackedByPlayer(event.getEntity()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onBreakBlock(BreakEvent event)
	{
		if (!CarryOnCommon.onTryBreakBlock(event.getPlayer())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void playerAttack(LivingAttackEvent event)
	{
		if(event.getEntity() instanceof Player player)
			CarryOnCommon.onPlayerAttacked(player);
	}

}
