package com.esotericsoftware.spine.view.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.esotericsoftware.spine.*;
import com.esotericsoftware.spine.utils.TwoColorPolygonBatch;

public class SkeletonDisplayer extends ApplicationAdapter {

    String atlasGdxFilePath;
    String skeletonGdxFilePath;
    OrthographicCamera camera;
    TwoColorPolygonBatch batch;
    TextureAtlas atlas;
    SkeletonRenderer renderer;
    SkeletonRendererDebug debugRenderer;
    SkeletonData skeletonData;
    Skeleton skeleton;
    AnimationState state;

    DisplayRenderParam renderParam;

    public SkeletonData getSkeletonData() {
        return skeletonData;
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public TextureAtlas getAtlas() {
        return atlas;
    }

    @Override
    public void create() {
        super.create();
        batch = new TwoColorPolygonBatch(3100);
        camera = new OrthographicCamera();
        renderer = new SkeletonRenderer();
        debugRenderer = new SkeletonRendererDebug();
        loadSkeleton();
        acceptParams();
    }

    private void acceptParams() {
        if(skeleton == null || renderParam == null) return;
        state.getData().setDefaultMix(renderParam.defaultMix);
        renderer.setPremultipliedAlpha(renderParam.showPremultipliedAlpha);
        batch.setPremultipliedAlpha(renderParam.showPremultipliedAlpha);

        float scaleX = renderParam.scaleX, scaleY = renderParam.scaleY;
        if (skeleton.getScaleX() == 0) skeleton.setScaleX(0.01f);
        if (skeleton.getScaleY() == 0) skeleton.setScaleY(0.01f);
        skeleton.setScale(scaleX, scaleY);

        float delta = Math.min(renderParam.delta, 0.032f) * renderParam.speed;
        skeleton.update(delta);
        state.update(delta);
        state.apply(skeleton);
        skeleton.updateWorldTransform();

        batch.begin();
        renderer.draw(batch, skeleton);
        batch.end();

        debugRenderer.setBones(renderParam.showBones);
        debugRenderer.setRegionAttachments(renderParam.showRegions);
        debugRenderer.setBoundingBoxes(renderParam.showBoundingBoxes);
        debugRenderer.setMeshHull(renderParam.showMeshHull);
        debugRenderer.setMeshTriangles(renderParam.showMeshTriangle);
        debugRenderer.setPaths(renderParam.showPaths);
        debugRenderer.setPoints(renderParam.showPoints);
        debugRenderer.setClipping(renderParam.showClipping);
    }

    public void setRenderParam(DisplayRenderParam renderParam) {
        this.renderParam = renderParam;
        System.out.println(renderParam.toString());
        acceptParams();
    }

    void loadSkeleton() {

        // Setup a texture atlas that uses a white image for images not found in the atlas.
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(1, 1, 1, 0.33f));
        pixmap.fill();
        final TextureAtlas.AtlasRegion fake = new TextureAtlas.AtlasRegion(new Texture(pixmap), 0, 0, 32, 32);
        pixmap.dispose();

        atlas = new TextureAtlas(Gdx.files.internal(atlasGdxFilePath));

        // Load skeleton data.
        SkeletonJson json = new SkeletonJson(atlas);
        skeletonData = json.readSkeletonData(Gdx.files.internal(skeletonGdxFilePath));
        skeleton = new Skeleton(skeletonData);
        skeleton.updateWorldTransform();
        skeleton.setToSetupPose();
        skeleton = new Skeleton(skeleton); // Tests copy constructors.
        skeleton.updateWorldTransform();

        state = new AnimationState(new AnimationStateData(skeletonData));
        state.addListener(new AnimationState.AnimationStateAdapter() {

            public void event(AnimationState.TrackEntry entry, Event event) {
            }
        });

        // Populate UI.

//        ui.window.getTitleLabel().setText(skeletonFile.name());
//        {
//
//            Array<String> items = new Array();
//            for (Skin skin : skeletonData.getSkins())
//                items.add(skin.getName());
//            ui.skinList.setItems(items);
//        }
//        {
//            Array<String> items = new Array();
//            for (Animation animation : skeletonData.getAnimations())
//                items.add(animation.getName());
//            ui.animationList.setItems(items);
//        }
//        ui.trackButtons.getButtons().first().setChecked(true);
//
//        // Configure skeleton from UI.
//
//        if (ui.skinList.getSelected() != null) skeleton.setSkin(ui.skinList.getSelected());
//        setAnimation(skeletonData.getAnimations().get(0).getName(),true,0.3f, Animation.MixBlend.add,1.0f);

        // ui.animationList.clearListeners();
        // state.setAnimation(0, "walk", true);
    }

    public void setAnimationByIndex(int index, boolean loop, float mixDuration, Animation.MixBlend mixBlend,float alpha) {
        System.out.println("setAnimationByIndex index:"+index+", loop:"+loop+", mixDuration:"+mixDuration+", mixBlend:"+mixBlend+", alpha:"+alpha);
        int track = index;
//                items.add(animation.getName());
        AnimationState.TrackEntry current = state.getCurrent(track);
        AnimationState.TrackEntry entry;
        if (current == null) {
            state.setEmptyAnimation(track, 0);
            entry = state.addAnimation(track, skeletonData.getAnimations().get(track), loop, 0);
            entry.setMixDuration(mixDuration);
        } else {
            entry = state.setAnimation(track, skeletonData.getAnimations().get(track), loop);
        }
        entry.setMixBlend(mixBlend);
        entry.setAlpha(alpha);
    }

    public void setAnimation (String name, boolean loop, float mixDuration, Animation.MixBlend mixBlend,float alpha) {
        int index = -1;
        int animationSize = skeletonData.getAnimations().size;
        for (int i = 0; i <animationSize; i++) {
            if(name.equals(skeletonData.getAnimations().get(i).getName())){
                index = i;
                break;
            }
        }
        if(index == -1){
            return;
        }
        setAnimationByIndex(index,loop,mixDuration,mixBlend,alpha);
    }

    public void render () {
        Gdx.gl.glClearColor(112 / 255f, 111 / 255f, 118 / 255f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float delta = Gdx.graphics.getDeltaTime();
        camera.update();
        batch.getProjectionMatrix().set(camera.combined);
        debugRenderer.getShapeRenderer().setProjectionMatrix(camera.combined);

        // Draw skeleton origin lines.
        ShapeRenderer shapes = debugRenderer.getShapeRenderer();
        if (state != null) {
            shapes.setColor(Color.DARK_GRAY);
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.line(0, -99999, 0, 99999);
            shapes.line(-99999, 0, 99999, 0);
            shapes.end();
        }

        if (skeleton != null) {
            // Reload if skeleton file was modified.
//            if (reloadTimer <= 0) {
//                lastModifiedCheck -= delta;
//                if (lastModifiedCheck < 0) {
//                    lastModifiedCheck = checkModifiedInterval;
//                    long time = skeletonFile.lastModified();
//                    if (time != 0 && skeletonModified != time) reloadTimer = reloadDelay;
//                    FileHandle atlasFile = atlasFile(skeletonFile);
//                    time = atlasFile == null ? 0 : atlasFile.lastModified();
//                    if (time != 0 && atlasModified != time) reloadTimer = reloadDelay;
//                }
//            } else {
//                reloadTimer -= delta;
//                if (reloadTimer <= 0) {
//                    loadSkeleton(skeletonFile);
//                    ui.toast("Reloaded.");
//                }
//            }

            // Pose and render skeleton.
//            state.getData().setDefaultMix(ui.mixSlider.getValue());
//            renderer.setPremultipliedAlpha(ui.premultipliedCheckbox.isChecked());
//            batch.setPremultipliedAlpha(ui.premultipliedCheckbox.isChecked());
//
//            float scaleX = ui.xScaleSlider.getValue(), scaleY = ui.yScaleSlider.getValue();
//            if (skeleton.scaleX == 0) skeleton.scaleX = 0.01f;
//            if (skeleton.scaleY == 0) skeleton.scaleY = 0.01f;
//            skeleton.setScale(scaleX, scaleY);
//
//            delta = Math.min(delta, 0.032f) * ui.speedSlider.getValue();
            skeleton.update(delta);
            state.update(delta);
            state.apply(skeleton);
            skeleton.updateWorldTransform();

            batch.begin();
            renderer.draw(batch, skeleton);
            batch.end();

//            debugRenderer.setBones(ui.debugBonesCheckbox.isChecked());
//            debugRenderer.setRegionAttachments(ui.debugRegionsCheckbox.isChecked());
//            debugRenderer.setBoundingBoxes(ui.debugBoundingBoxesCheckbox.isChecked());
//            debugRenderer.setMeshHull(ui.debugMeshHullCheckbox.isChecked());
//            debugRenderer.setMeshTriangles(ui.debugMeshTrianglesCheckbox.isChecked());
//            debugRenderer.setPaths(ui.debugPathsCheckbox.isChecked());
//            debugRenderer.setPoints(ui.debugPointsCheckbox.isChecked());
//            debugRenderer.setClipping(ui.debugClippingCheckbox.isChecked());
            debugRenderer.draw(skeleton);
        }
    }


    public OrthographicCamera getCamera() {
        return camera;
    }

    public AnimationState getState() {
        return state;
    }

    public void resize (int width, int height) {
        float x = camera.position.x, y = camera.position.y;
        camera.setToOrtho(false);
        camera.position.set(x, y, 0);
    }

    public void resetCameraPosition(float width,float uiScale){
        camera.position.x = -width / 2 * uiScale;
        camera.position.y = Gdx.graphics.getHeight() / 4;
    }
}

