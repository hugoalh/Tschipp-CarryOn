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

package tschipp.carryon.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import tschipp.carryon.Constants;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnData.CarryType;
import tschipp.carryon.common.carry.CarryOnDataManager;
import tschipp.carryon.common.scripting.CarryOnScript;
import tschipp.carryon.common.scripting.CarryOnScript.ScriptRender;
import tschipp.carryon.platform.Services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

public class CarriedObjectRender
{

	private static SequencedMap<RenderType, ByteBufferBuilder> builders = new LinkedHashMap<>(Map.of(
			RenderType.glint(), new ByteBufferBuilder(RenderType.glint().bufferSize()),
			RenderType.armorEntityGlint(), new ByteBufferBuilder(RenderType.armorEntityGlint().bufferSize()),
			RenderType.glintTranslucent(), new ByteBufferBuilder(RenderType.glintTranslucent().bufferSize()),
			RenderType.entityGlint(), new ByteBufferBuilder(RenderType.entityGlint().bufferSize())
			//RenderType.entityGlintDirect(), new ByteBufferBuilder(RenderType.entityGlintDirect().bufferSize())
	));

	public static boolean drawFirstPerson(Player player, MultiBufferSource buffer, PoseStack matrix, int light, float partialTicks)
	{
		if(Services.PLATFORM.isModLoaded("firstperson") || Services.PLATFORM.isModLoaded("firstpersonmod"))
			return false;

		CarryOnData carry = CarryOnDataManager.getCarryData(player);
		try {
			if (carry.isCarrying(CarryType.BLOCK))
				drawFirstPersonBlock(player, buffer, matrix, light, CarryRenderHelper.getRenderState(player));
			else if (carry.isCarrying(CarryType.ENTITY))
				drawFirstPersonEntity(player, buffer, matrix, light, partialTicks);
		}
		catch (Exception e)
		{
			//hehe
		}

		if(carry.getActiveScript().isPresent())
		{
			ScriptRender render = carry.getActiveScript().get().scriptRender();
			if(!render.renderLeftArm() && player.getMainArm() == HumanoidArm.LEFT)
				return false;

			if(!render.renderRightArm() && player.getMainArm() == HumanoidArm.RIGHT)
				return false;
		}

		return carry.isCarrying();
	}

	private static void drawFirstPersonBlock(Player player, MultiBufferSource buffer, PoseStack matrix, int light, BlockState state)
	{
		matrix.pushPose();
		matrix.scale(2.5f, 2.5f, 2.5f);
		matrix.translate(0, -0.5, -1);
		RenderSystem.enableBlend();
		RenderSystem.disableCull();

		CarryOnData carry = CarryOnDataManager.getCarryData(player);

		if (Constants.CLIENT_CONFIG.facePlayer != CarryRenderHelper.isChest(state.getBlock())) {
			matrix.mulPose(Axis.YP.rotationDegrees(180));
			matrix.mulPose(Axis.XN.rotationDegrees(8));
		} else {
			matrix.mulPose(Axis.XP.rotationDegrees(8));
		}

		if(carry.getActiveScript().isPresent())
			CarryRenderHelper.performScriptTransformation(matrix, carry.getActiveScript().get());

		RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);

		ItemStack stack = new ItemStack(state.getBlock().asItem());
		BakedModel model = CarryRenderHelper.getRenderBlock(player);
		CarryRenderHelper.renderBakedModel(stack, matrix, buffer, light, model);

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		matrix.popPose();
	}

	private static void drawFirstPersonEntity(Player player, MultiBufferSource buffer, PoseStack matrix, int light, float partialTicks) {
		EntityRenderDispatcher manager = Minecraft.getInstance().getEntityRenderDispatcher();
		Entity entity = CarryRenderHelper.getRenderEntity(player);
		CarryOnData carry = CarryOnDataManager.getCarryData(player);

		if (entity != null)
		{
			Vec3 playerpos = CarryRenderHelper.getExactPos(player, partialTicks);

			entity.setPos(playerpos.x, playerpos.y, playerpos.z);
			entity.xRotO = 0.0f;
			entity.yRotO = 0.0f;
			entity.setYHeadRot(0.0f);

			float height = entity.getBbHeight();
			float width = entity.getBbWidth();

			matrix.pushPose();
			matrix.scale(0.8f, 0.8f, 0.8f);
			matrix.mulPose(Axis.YP.rotationDegrees(180));
			matrix.translate(0.0, -height - .1, width + 0.1);

			manager.setRenderShadow(false);

			Optional<CarryOnScript> res = carry.getActiveScript();
			if(res.isPresent())
			{
				CarryOnScript script = res.get();
				CarryRenderHelper.performScriptTransformation(matrix, script);
			}

			if (entity instanceof LivingEntity)
				((LivingEntity) entity).hurtTime = 0;

			try {
				manager.render(entity, 0, 0, 0, 0f, matrix, buffer, light);
			}
			catch (Exception e)
			{
			}
			manager.setRenderShadow(true);
			matrix.popPose();

		}

		// RenderSystem.disableAlphaTest();
	}

	/**
	 * Draws the third person view of entities and blocks
	 * @param partialticks
	 * @param mat
	 */
	public static void drawThirdPerson(float partialticks, Matrix4f mat) {
		Minecraft mc = Minecraft.getInstance();
		Level level = mc.level;
		int light = 0;
		int perspective = CarryRenderHelper.getPerspective();
		EntityRenderDispatcher manager = mc.getEntityRenderDispatcher();

		PoseStack matrix = new PoseStack();
		matrix.mulPose(mat);

		RenderSystem.enableBlend();
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();

		BufferSource buffer = MultiBufferSource.immediateWithBuffers(builders, builders.get(RenderType.glint()));

		for (Player player : level.players())
		{
			try {

				CarryOnData carry = CarryOnDataManager.getCarryData(player);

				if (perspective == 0 && player == mc.player && !(Services.PLATFORM.isModLoaded("firstperson") || Services.PLATFORM.isModLoaded("firstpersonmod") || Services.PLATFORM.isModLoaded("realcamera")))
					continue;

				light = manager.getPackedLightCoords(player, partialticks);

				if (carry.isCarrying(CarryType.BLOCK)) {
					BlockState state = CarryRenderHelper.getRenderState(player);

					CarryRenderHelper.applyBlockTransformations(player, partialticks, matrix, state.getBlock());

					ItemStack tileItem = new ItemStack(state.getBlock().asItem());
					BakedModel model = CarryRenderHelper.getRenderBlock(player);

					//ModelOverridesHandler.hasCustomOverrideModel(state, tag) ? ModelOverridesHandler.getCustomOverrideModel(state, tag, level, player) : tileItem.isEmpty() ? mc.getBlockRenderer().getBlockModel(state) : mc.getItemRenderer().getModel(tileItem, level, player, 0);
//
					Optional<CarryOnScript> res = carry.getActiveScript();
					if (res.isPresent()) {
						CarryOnScript script = res.get();
						CarryRenderHelper.performScriptTransformation(matrix, script);
					}

					RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
					RenderSystem.enableCull();

					PoseStack.Pose p = matrix.last();
					PoseStack copy = new PoseStack();
					copy.mulPose(p.pose());
					matrix.popPose();

					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

					CarryRenderHelper.renderBakedModel(tileItem, copy, buffer, light, model);

					matrix.popPose();
				} else if (carry.isCarrying(CarryType.ENTITY)) {
					Entity entity = CarryRenderHelper.getRenderEntity(player);

					if (entity != null) {
						CarryRenderHelper.applyEntityTransformations(player, partialticks, matrix, entity);

						manager.setRenderShadow(false);

						Optional<CarryOnScript> res = carry.getActiveScript();
						if (res.isPresent()) {
							CarryOnScript script = res.get();
							CarryRenderHelper.performScriptTransformation(matrix, script);
						}

						if (entity instanceof LivingEntity le)
							le.hurtTime = 0;

						RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

						manager.render(entity, 0, 0, 0, 0f, matrix, buffer, light);
						matrix.popPose();
						manager.setRenderShadow(true);
						matrix.popPose();
					}
				}


			}
			catch (Exception e)
			{
			}

		}
		buffer.endLastBatch();

		buffer.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
		buffer.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
		buffer.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
		buffer.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));

		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

}

