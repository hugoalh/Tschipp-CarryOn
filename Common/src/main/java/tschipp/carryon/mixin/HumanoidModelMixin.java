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

package tschipp.carryon.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tschipp.carryon.Constants;
import tschipp.carryon.client.render.ICarryOnRenderState;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.scripting.CarryOnScript.ScriptRender;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {

    @Shadow
    public ModelPart rightArm;

    @Shadow
    public ModelPart leftArm;

    @Inject(at = @At("RETURN"), method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V")
    private void onSetupAnimations(HumanoidRenderState state, CallbackInfo ci)
    {
        if(state instanceof ICarryOnRenderState carryOnRenderState && Constants.CLIENT_CONFIG.renderArms)
        {
            CarryOnData carry = carryOnRenderState.getCarryOnData();
            if(carry.isCarrying() && !state.isVisuallySwimming && !state.isFallFlying)
            {
                boolean sneaking = state.isCrouching;

                float x = 1.0f + (sneaking ? 0.2f : 0.0f) + (carry.isCarrying(CarryOnData.CarryType.BLOCK) ? 0.0f : 0.3f);
                float z = 0.05f;

                float width = carryOnRenderState.getRenderWidth();
                float offset = Math.min((width - 1) / 1.5f, 0.2f);

                if(carry.getActiveScript().isPresent())
                {
                    ScriptRender render = carry.getActiveScript().get().scriptRender();
                    boolean renderLeft = render.renderLeftArm();
                    boolean renderRight = render.renderRightArm();

                    Vec3 rotLeft = render.renderRotationLeftArm().getVec(-x, -offset, z);
                    Vec3 rotRight = render.renderRotationRightArm().getVec(-x, offset, -z);

                    if(renderLeft)
                        changeRotation(leftArm, (float) rotLeft.x, (float) rotLeft.y, (float) rotLeft.z);

                    if(renderRight)
                        changeRotation(rightArm, (float) rotRight.x, (float) rotRight.y, (float) rotRight.z);
                }
                else {
                    changeRotation(rightArm, -x, offset, -z);
                    changeRotation(leftArm, -x, -offset, z);
                }

            }

        }
    }

    @Unique
    private void changeRotation(ModelPart part, float x, float y, float z)
    {
        part.xRot = x;
        part.yRot = y;
        part.zRot = z;
    }

}
