package tschipp.carryon.client.render;

import tschipp.carryon.common.carry.CarryOnData;

public interface ICarryOnRenderState {

    CarryOnData getCarryOnData();

    void setCarryOnData(CarryOnData data);

    float getRenderWidth();

    void setRenderWidth(float val);

}
