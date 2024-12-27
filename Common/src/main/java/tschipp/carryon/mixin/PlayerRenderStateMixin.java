package tschipp.carryon.mixin;

import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tschipp.carryon.client.render.ICarryOnRenderState;
import tschipp.carryon.common.carry.CarryOnData;

@Mixin(PlayerRenderState.class)
public class PlayerRenderStateMixin implements ICarryOnRenderState {

    @Unique
    public CarryOnData carryOnData = null;

    @Unique
    public float renderWidth = 0f;

    @Unique
    @Override
    public CarryOnData getCarryOnData() {
        return carryOnData;
    }

    @Unique
    @Override
    public void setCarryOnData(CarryOnData data) {
        carryOnData = data;
    }

    @Unique
    @Override
    public float getRenderWidth() {
        return renderWidth;
    }

    @Unique
    @Override
    public void setRenderWidth(float val) {
        renderWidth = val;
    }
}
