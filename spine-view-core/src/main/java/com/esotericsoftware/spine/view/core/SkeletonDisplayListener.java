package com.esotericsoftware.spine.view.core;

public interface SkeletonDisplayListener {
    void resize(int width, int height);
    /**
     ((ScreenViewport)ui.stage.getViewport()).setUnitsPerPixel(1 / uiScale);
     ui.stage.getViewport().update(width, height, true);
     if (!ui.minimizeButton.isChecked()) ui.window.setHeight(height / uiScale + 8);
     */
}
