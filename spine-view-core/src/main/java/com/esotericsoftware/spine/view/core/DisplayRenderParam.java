package com.esotericsoftware.spine.view.core;

public class DisplayRenderParam {
    boolean showBones;
    // debugRenderer.setBones(ui.debugBonesCheckbox.isChecked());
    boolean showRegions;
    // debugRenderer.setRegionAttachments(ui.debugRegionsCheckbox.isChecked());
    boolean showBoundingBoxes;
    //            debugRenderer.setBoundingBoxes(ui.debugBoundingBoxesCheckbox.isChecked());
    boolean showMeshHull;
    //            debugRenderer.setMeshHull(ui.debugMeshHullCheckbox.isChecked());
    boolean showMeshTriangle;
    //            debugRenderer.setMeshTriangles(ui.debugMeshTrianglesCheckbox.isChecked());
    boolean showPaths;
    //            debugRenderer.setPaths(ui.debugPathsCheckbox.isChecked());
    boolean showPoints;
    //            debugRenderer.setPoints(ui.debugPointsCheckbox.isChecked());
    boolean showClipping;
//            debugRenderer.setClipping(ui.debugClippingCheckbox.isChecked());

    float delta;
    float speed;

    float scaleX;
    float scaleY;

    float defaultMix;
//      state.getData().setDefaultMix(ui.mixSlider.getValue());
    boolean showPremultipliedAlpha;
//            renderer.setPremultipliedAlpha(ui.premultipliedCheckbox.isChecked());
//            batch.setPremultipliedAlpha(ui.premultipliedCheckbox.isChecked());


    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public boolean isShowBones() {
        return showBones;
    }

    public void setShowBones(boolean showBones) {
        this.showBones = showBones;
    }

    public boolean isShowRegions() {
        return showRegions;
    }

    public void setShowRegions(boolean showRegions) {
        this.showRegions = showRegions;
    }

    public boolean isShowBoundingBoxes() {
        return showBoundingBoxes;
    }

    public void setShowBoundingBoxes(boolean showBoundingBoxes) {
        this.showBoundingBoxes = showBoundingBoxes;
    }

    public boolean isShowMeshHull() {
        return showMeshHull;
    }

    public void setShowMeshHull(boolean showMeshHull) {
        this.showMeshHull = showMeshHull;
    }

    public boolean isShowMeshTriangle() {
        return showMeshTriangle;
    }

    public void setShowMeshTriangle(boolean showMeshTriangle) {
        this.showMeshTriangle = showMeshTriangle;
    }

    public boolean isShowPaths() {
        return showPaths;
    }

    public void setShowPaths(boolean showPaths) {
        this.showPaths = showPaths;
    }

    public boolean isShowPoints() {
        return showPoints;
    }

    public void setShowPoints(boolean showPoints) {
        this.showPoints = showPoints;
    }

    public boolean isShowClipping() {
        return showClipping;
    }

    public void setShowClipping(boolean showClipping) {
        this.showClipping = showClipping;
    }

    public float getDelta() {
        return delta;
    }

    public void setDelta(float delta) {
        this.delta = delta;
    }

    public float getScaleX() {
        return scaleX;
    }

    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }

    public float getDefaultMix() {
        return defaultMix;
    }

    public void setDefaultMix(float defaultMix) {
        this.defaultMix = defaultMix;
    }

    public boolean isShowPremultipliedAlpha() {
        return showPremultipliedAlpha;
    }

    public void setShowPremultipliedAlpha(boolean showPremultipliedAlpha) {
        this.showPremultipliedAlpha = showPremultipliedAlpha;
    }

    @Override
    public String toString() {
        return "DisplayRenderParam{" +
                "showBones=" + showBones +
                ", showRegions=" + showRegions +
                ", showBoundingBoxes=" + showBoundingBoxes +
                ", showMeshHull=" + showMeshHull +
                ", showMeshTriangle=" + showMeshTriangle +
                ", showPaths=" + showPaths +
                ", showPoints=" + showPoints +
                ", showClipping=" + showClipping +
                ", delta=" + delta +
                ", speed=" + speed +
                ", scaleX=" + scaleX +
                ", scaleY=" + scaleY +
                ", defaultMix=" + defaultMix +
                ", showPremultipliedAlpha=" + showPremultipliedAlpha +
                '}';
    }
}
