package com.esotericsoftware.spine;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.spine.view.core.DisplayRenderParam;
import com.esotericsoftware.spine.view.core.SkeletonDisplayer;
import com.esotericsoftware.spine.view.core.SkeletonDisplayerBuilder;

import java.awt.*;
import java.io.File;

public class SkeletonControlBoard extends ApplicationAdapter {

    static final float checkModifiedInterval = 0.250f;
    static final float reloadDelay = 1;
    static float uiScale = 1;
    static String[] dataSuffixes = {".json", ".skel"};
    static String[] atlasSuffixes = {".atlas", "-pro.atlas", "-ess.atlas"};
    static String[] extraSuffixes = {"", ".txt", ".bytes"};
    static String[] args;

    UI ui;
    FileHandle skeletonFile;
    long skeletonModified, atlasModified;
    float lastModifiedCheck, reloadTimer;
    StringBuilder status = new StringBuilder();
    Preferences prefs;
    SkeletonDisplayer skeletonDisplayer;

    public void create () {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException (Thread thread, Throwable ex) {
                ex.printStackTrace();
                Runtime.getRuntime().halt(0); // Prevent Swing from keeping JVM alive.
            }
        });

        prefs = Gdx.app.getPreferences("spine-skeletonviewer");
        ui = new UI();

        ui.loadPrefs();

        if (args.length == 0) {
            loadSkeleton(
                    Gdx.files.internal(Gdx.app.getPreferences("spine-skeletonviewer").getString("lastFile", "spineboy/spineboy.json")));
        } else {
            loadSkeleton(Gdx.files.internal(args[0]));
        }

        ui.loadPrefs();
        ui.prefsLoaded = true;
    }

    private FileHandle atlasFile (FileHandle skeletonFile, String baseName) {
        for (String extraSuffix : extraSuffixes) {
            for (String suffix : atlasSuffixes) {
                FileHandle file = skeletonFile.sibling(baseName + suffix + extraSuffix);
                if (file.exists()) return file;
            }
        }
        return null;
    }

    FileHandle atlasFile (FileHandle skeletonFile) {
        String baseName = skeletonFile.name();
        for (String extraSuffix : extraSuffixes) {
            for (String dataSuffix : dataSuffixes) {
                String suffix = dataSuffix + extraSuffix;
                if (baseName.endsWith(suffix)) {
                    FileHandle file = atlasFile(skeletonFile, baseName.substring(0, baseName.length() - suffix.length()));
                    if (file != null) return file;
                }
            }
        }
        return atlasFile(skeletonFile, baseName);
    }

    void loadSkeleton (final FileHandle skeletonFile) {
        if (skeletonFile == null) return;

        FileHandle atlasFile = atlasFile(skeletonFile);

        skeletonDisplayer = new SkeletonDisplayerBuilder()
                .setAtlasGdxFilePath("xiaoqiao/xiaoqiao.atlas")
                .setSkeletonGdxFilePath("xiaoqiao/xiaoqiao.json")
                .build();

        skeletonDisplayer.create();

        this.skeletonFile = skeletonFile;
        skeletonModified = skeletonFile.lastModified();
        atlasModified = atlasFile == null ? 0 : atlasFile.lastModified();
        lastModifiedCheck = checkModifiedInterval;
        prefs.putString("lastFile", skeletonFile.path());
        prefs.flush();

        // Populate UI.

        ui.window.getTitleLabel().setText(skeletonFile.name());
        {

            Array<String> items = new Array();
            for (Skin skin : skeletonDisplayer.getSkeletonData().getSkins())
                items.add(skin.getName());
            ui.skinList.setItems(items);
        }
        {
            Array<String> items = new Array();
            for (Animation animation : skeletonDisplayer.getSkeletonData().getAnimations())
                items.add(animation.getName());
            ui.animationList.setItems(items);
        }
        ui.trackButtons.getButtons().first().setChecked(true);

        // Configure skeleton from UI.

        if (ui.skinList.getSelected() != null) skeletonDisplayer.getSkeleton().setSkin(ui.skinList.getSelected());
        setAnimation();

        // ui.animationList.clearListeners();
        // state.setAnimation(0, "walk", true);
    }

    private void setAnimation(){
        if (ui.animationList.getSelected() != null){
            int track = ui.trackButtons.getCheckedIndex();
            skeletonDisplayer.setAnimationByIndex(track,ui.loopCheckbox.isChecked(),
                    ui.mixSlider.getValue(),
                    ui.addCheckbox.isChecked() ? Animation.MixBlend.add : Animation.MixBlend.replace,
                    ui.alphaSlider.getValue());
        }
    }

    public void render () {
        float delta = Gdx.graphics.getDeltaTime();
        if (skeletonDisplayer.getSkeleton() != null) {
            // Reload if skeleton file was modified.
            if (reloadTimer <= 0) {
                lastModifiedCheck -= delta;
                if (lastModifiedCheck < 0) {
                    lastModifiedCheck = checkModifiedInterval;
                    long time = skeletonFile.lastModified();
                    if (time != 0 && skeletonModified != time) reloadTimer = reloadDelay;
                    FileHandle atlasFile = atlasFile(skeletonFile);
                    time = atlasFile == null ? 0 : atlasFile.lastModified();
                    if (time != 0 && atlasModified != time) reloadTimer = reloadDelay;
                }
            } else {
                reloadTimer -= delta;
                if (reloadTimer <= 0) {
                    loadSkeleton(skeletonFile);
                    ui.toast("Reloaded.");
                }
            }

            // Pose and render skeleton.
            DisplayRenderParam displayRenderParam = new DisplayRenderParam();
            updateRenderValues(displayRenderParam,delta);
            skeletonDisplayer.setRenderParam(displayRenderParam);
            skeletonDisplayer.render();
        }

        if (skeletonDisplayer.getState() != null) {
            // AnimationState status.
            status.setLength(0);
            for (int i = skeletonDisplayer.getState().getTracks().size - 1; i >= 0; i--) {
                AnimationState.TrackEntry entry = skeletonDisplayer.getState().getTracks().get(i);
                if (entry == null) continue;
                status.append(i);
                status.append(": [LIGHT_GRAY]");
                status(entry);
                status.append("[WHITE]");
                status.append(entry.animation.name);
                status.append('\n');
            }
            ui.statusLabel.setText(status);
        }

        // Render UI.
        ui.render();

        // Draw indicator lines for animation and mix times.
    }

    private void updateRenderValues(DisplayRenderParam renderParam,float delta){
        renderParam.setDefaultMix(ui.mixSlider.getValue());
        renderParam.setShowPremultipliedAlpha(ui.premultipliedCheckbox.isChecked());
        renderParam.setScaleX(ui.xScaleSlider.getValue());
        renderParam.setScaleY(ui.yScaleSlider.getValue());
        renderParam.setDelta(delta);
        renderParam.setSpeed(ui.speedSlider.getValue());
        renderParam.setShowBones(ui.debugBonesCheckbox.isChecked());
        renderParam.setShowRegions(ui.debugRegionsCheckbox.isChecked());
        renderParam.setShowBoundingBoxes(ui.debugBoundingBoxesCheckbox.isChecked());
        renderParam.setShowMeshHull(ui.debugMeshHullCheckbox.isChecked());
        renderParam.setShowMeshTriangle(ui.debugMeshTrianglesCheckbox.isChecked());
        renderParam.setShowPaths(ui.debugPathsCheckbox.isChecked());
        renderParam.setShowPoints(ui.debugPointsCheckbox.isChecked());
        renderParam.setShowClipping(ui.debugClippingCheckbox.isChecked());
    }

    void status (AnimationState.TrackEntry entry) {
        AnimationState.TrackEntry from = entry.mixingFrom;
        if (from == null) return;
        status(from);
        status.append(from.animation.name);
        status.append(' ');
        status.append(Math.min(100, (int)(entry.mixTime / entry.mixDuration * 100)));
        status.append("% -> ");
    }

    void resetCameraPosition () {
      skeletonDisplayer.resetCameraPosition(ui.window.getWidth(),uiScale);
    }

    public void resize (int width, int height) {
        skeletonDisplayer.resize(width, height);
        ((ScreenViewport)ui.stage.getViewport()).setUnitsPerPixel(1 / uiScale);
        ui.stage.getViewport().update(width, height, true);
        if (!ui.minimizeButton.isChecked()) ui.window.setHeight(height / uiScale + 8);
    }

    class UI {
        boolean prefsLoaded;

        Stage stage = new Stage(new ScreenViewport());
        com.badlogic.gdx.scenes.scene2d.ui.Skin skin = new com.badlogic.gdx.scenes.scene2d.ui.Skin(
                Gdx.files.internal("skin/skin.json"));

        com.badlogic.gdx.scenes.scene2d.ui.Window window = new com.badlogic.gdx.scenes.scene2d.ui.Window("Skeleton", skin);
        Table root = new Table(skin);
        TextButton openButton = new TextButton("Open", skin);
        TextButton minimizeButton = new TextButton("-", skin);

        Slider loadScaleSlider = new Slider(0.1f, 3, 0.01f, false, skin);
        com.badlogic.gdx.scenes.scene2d.ui.Label loadScaleLabel = new com.badlogic.gdx.scenes.scene2d.ui.Label("100%", skin);
        TextButton loadScaleResetButton = new TextButton("Reload", skin);

        Slider zoomSlider = new Slider(0.01f, 10, 0.01f, false, skin);
        com.badlogic.gdx.scenes.scene2d.ui.Label zoomLabel = new com.badlogic.gdx.scenes.scene2d.ui.Label("100%", skin);
        TextButton zoomResetButton = new TextButton("Reset", skin);

        Slider xScaleSlider = new Slider(-2, 2, 0.01f, false, skin);
        com.badlogic.gdx.scenes.scene2d.ui.Label xScaleLabel = new com.badlogic.gdx.scenes.scene2d.ui.Label("100%", skin);
        TextButton xScaleResetButton = new TextButton("Reset", skin);

        Slider yScaleSlider = new Slider(-2, 2, 0.01f, false, skin);
        com.badlogic.gdx.scenes.scene2d.ui.Label yScaleLabel = new com.badlogic.gdx.scenes.scene2d.ui.Label("100%", skin);
        TextButton yScaleResetButton = new TextButton("Reset", skin);

        CheckBox debugBonesCheckbox = new CheckBox("Bones", skin);
        CheckBox debugRegionsCheckbox = new CheckBox("Regions", skin);
        CheckBox debugBoundingBoxesCheckbox = new CheckBox("Bounds", skin);
        CheckBox debugMeshHullCheckbox = new CheckBox("Mesh hull", skin);
        CheckBox debugMeshTrianglesCheckbox = new CheckBox("Triangles", skin);
        CheckBox debugPathsCheckbox = new CheckBox("Paths", skin);
        CheckBox debugPointsCheckbox = new CheckBox("Points", skin);
        CheckBox debugClippingCheckbox = new CheckBox("Clipping", skin);

        CheckBox premultipliedCheckbox = new CheckBox("Premultiplied", skin);

        CheckBox linearCheckbox = new CheckBox("Linear", skin);

        TextButton bonesSetupPoseButton = new TextButton("Bones", skin);
        TextButton slotsSetupPoseButton = new TextButton("Slots", skin);
        TextButton setupPoseButton = new TextButton("Both", skin);

        com.badlogic.gdx.scenes.scene2d.ui.List<String> skinList = new com.badlogic.gdx.scenes.scene2d.ui.List(skin);
        com.badlogic.gdx.scenes.scene2d.ui.ScrollPane skinScroll = new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane(skinList, skin, "bg");

        ButtonGroup<TextButton> trackButtons = new ButtonGroup();
        CheckBox loopCheckbox = new CheckBox("Loop", skin);
        CheckBox addCheckbox = new CheckBox("Add", skin);

        Slider alphaSlider = new Slider(0, 1, 0.01f, false, skin);
        com.badlogic.gdx.scenes.scene2d.ui.Label alphaLabel = new com.badlogic.gdx.scenes.scene2d.ui.Label("100%", skin);

        com.badlogic.gdx.scenes.scene2d.ui.List<String> animationList = new List(skin);
        com.badlogic.gdx.scenes.scene2d.ui.ScrollPane animationScroll = new ScrollPane(animationList, skin, "bg");

        Slider speedSlider = new Slider(0, 3, 0.01f, false, skin);
        com.badlogic.gdx.scenes.scene2d.ui.Label speedLabel = new com.badlogic.gdx.scenes.scene2d.ui.Label("1.0x", skin);
        TextButton speedResetButton = new TextButton("Reset", skin);

        Slider mixSlider = new Slider(0, 4, 0.01f, false, skin);
        com.badlogic.gdx.scenes.scene2d.ui.Label mixLabel = new com.badlogic.gdx.scenes.scene2d.ui.Label("0.3s", skin);

        com.badlogic.gdx.scenes.scene2d.ui.Label statusLabel = new com.badlogic.gdx.scenes.scene2d.ui.Label("", skin);
        WidgetGroup toasts = new WidgetGroup();

        UI () {
            initialize();
            layout();
            events();
        }

        void initialize () {
            skin.getFont("default").getData().markupEnabled = true;

            for (int i = 0; i < 6; i++)
                trackButtons.add(new TextButton(i + "", skin, "toggle"));

            premultipliedCheckbox.setChecked(true);

            linearCheckbox.setChecked(true);

            loopCheckbox.setChecked(true);

            loadScaleSlider.setValue(1);
            loadScaleSlider.setSnapToValues(new float[] {0.5f, 1, 1.5f, 2, 2.5f}, 0.09f);

            zoomSlider.setValue(1);
            zoomSlider.setSnapToValues(new float[] {1, 2}, 0.30f);

            xScaleSlider.setValue(1);
            xScaleSlider.setSnapToValues(new float[] {-1.5f, -1, -0.5f, 0.5f, 1, 1.5f}, 0.12f);

            yScaleSlider.setValue(1);
            yScaleSlider.setSnapToValues(new float[] {-1.5f, -1, -0.5f, 0.5f, 1, 1.5f}, 0.12f);

            skinList.getSelection().setRequired(false);
            skinList.getSelection().setToggle(true);

            animationList.getSelection().setRequired(false);
            animationList.getSelection().setToggle(true);

            mixSlider.setValue(0.3f);
            mixSlider.setSnapToValues(new float[] {1, 1.5f, 2, 2.5f, 3, 3.5f}, 0.12f);

            speedSlider.setValue(1);
            speedSlider.setSnapToValues(new float[] {0.5f, 0.75f, 1, 1.25f, 1.5f, 2, 2.5f}, 0.09f);

            alphaSlider.setValue(1);
            alphaSlider.setDisabled(true);

            addCheckbox.setDisabled(true);

            window.setMovable(false);
            window.setResizable(false);
            window.setKeepWithinStage(false);
            window.setX(-3);
            window.setY(-2);

            window.getTitleLabel().setColor(new com.badlogic.gdx.graphics.Color(0xc1ffffff));
            window.getTitleTable().add(openButton).space(3);
            window.getTitleTable().add(minimizeButton).width(20);

            skinScroll.setFadeScrollBars(false);

            animationScroll.setFadeScrollBars(false);
        }

        void layout () {
            float resetWidth = loadScaleResetButton.getPrefWidth();

            root.defaults().space(6);
            root.columnDefaults(0).top().right().padTop(3);
            root.columnDefaults(1).left();
            root.add("Load scale:");
            {
                Table table = table();
                table.add(loadScaleLabel).width(29);
                table.add(loadScaleSlider).growX();
                table.add(loadScaleResetButton).width(resetWidth);
                root.add(table).fill().row();
            }
            root.add("Zoom:");
            {
                Table table = table();
                table.add(zoomLabel).width(29);
                table.add(zoomSlider).growX();
                table.add(zoomResetButton).width(resetWidth);
                root.add(table).fill().row();
            }
            root.add("Scale X:");
            {
                Table table = table();
                table.add(xScaleLabel).width(29);
                table.add(xScaleSlider).growX();
                table.add(xScaleResetButton).width(resetWidth);
                root.add(table).fill().row();
            }
            root.add("Scale Y:");
            {
                Table table = table();
                table.add(yScaleLabel).width(29);
                table.add(yScaleSlider).growX();
                table.add(yScaleResetButton).width(resetWidth);
                root.add(table).fill().row();
            }
            root.add("Debug:");
            root.add(table(debugBonesCheckbox, debugRegionsCheckbox, debugBoundingBoxesCheckbox)).row();
            root.add();
            root.add(table(debugPathsCheckbox, debugPointsCheckbox, debugClippingCheckbox)).row();
            root.add();
            root.add(table(debugMeshHullCheckbox, debugMeshTrianglesCheckbox)).row();
            root.add("Atlas alpha:");
            {
                Table table = table();
                table.add(premultipliedCheckbox);
                table.add("Filtering:").growX().getActor().setAlignment(Align.right);
                table.add(linearCheckbox);
                root.add(table).fill().row();
            }

            root.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0x4e4e4eff)))).height(1).fillX().colspan(2).pad(-3, 0, 1, 0)
                    .row();

            root.add("Setup pose:");
            root.add(table(bonesSetupPoseButton, slotsSetupPoseButton, setupPoseButton)).row();
            root.add("Skin:");
            root.add(skinScroll).grow().minHeight(64).row();

            root.add(new Image(skin.newDrawable("white", new com.badlogic.gdx.graphics.Color(0x4e4e4eff)))).height(1).fillX().colspan(2).pad(1, 0, 1, 0).row();

            root.add("Track:");
            {
                Table table = table();
                for (TextButton button : trackButtons.getButtons())
                    table.add(button);
                table.add(loopCheckbox);
                table.add(addCheckbox);
                root.add(table).row();
            }
            root.add("Entry alpha:");
            {
                Table table = table();
                table.add(alphaLabel).width(29);
                table.add(alphaSlider).growX();
                root.add(table).fill().row();
            }
            root.add("Animation:");
            root.add(animationScroll).grow().minHeight(64).row();
            root.add("Speed:");
            {
                Table table = table();
                table.add(speedLabel).width(29);
                table.add(speedSlider).growX();
                table.add(speedResetButton);
                root.add(table).fill().row();
            }
            root.add("Default mix:");
            {
                Table table = table();
                table.add(mixLabel).width(29);
                table.add(mixSlider).growX();
                root.add(table).fill().row();
            }

            window.add(root).grow();
            window.pack();
            stage.addActor(window);

            stage.addActor(statusLabel);

            {
                Table table = new Table();
                table.setFillParent(true);
                table.setTouchable(Touchable.disabled);
                stage.addActor(table);
                table.pad(10, 10, 22, 10).bottom().right();
                table.add(toasts);
            }

            {
                Table table = new Table();
                table.setFillParent(true);
                table.setTouchable(Touchable.disabled);
                stage.addActor(table);
                table.pad(10).top().right();
                table.defaults().right();
                table.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("", skin, "default", Color.LIGHT_GRAY)); // Version.
            }
        }

        void events () {
            window.addListener(new InputListener() {
                public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                    event.cancel();
                    return true;
                }
            });

            openButton.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    FileDialog fileDialog = new FileDialog((Frame)null, "Choose skeleton file");
                    fileDialog.setMode(FileDialog.LOAD);
                    fileDialog.setVisible(true);
                    String name = fileDialog.getFile();
                    String dir = fileDialog.getDirectory();
                    if (name == null || dir == null) return;
                    loadSkeleton(new FileHandle(new File(dir, name).getAbsolutePath()));
                    ui.toast("Loaded.");
                }
            });

            setupPoseButton.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    if (skeletonDisplayer.getSkeleton() != null) skeletonDisplayer.getSkeleton().setToSetupPose();
                }
            });
            bonesSetupPoseButton.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    if (skeletonDisplayer.getSkeleton() != null) skeletonDisplayer.getSkeleton().setBonesToSetupPose();
                }
            });
            slotsSetupPoseButton.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    if (skeletonDisplayer.getSkeleton() != null) skeletonDisplayer.getSkeleton().setSlotsToSetupPose();
                }
            });

            minimizeButton.addListener(new ClickListener() {
                public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                    event.cancel();
                    return super.touchDown(event, x, y, pointer, button);
                }

                public void clicked (InputEvent event, float x, float y) {
                    if (minimizeButton.isChecked()) {
                        window.getCells().get(0).setActor(null);
                        window.setHeight(37);
                        minimizeButton.setText("+");
                    } else {
                        window.getCells().get(0).setActor(root);
                        window.setHeight(Gdx.graphics.getHeight() / uiScale + 8);
                        minimizeButton.setText("-");
                    }
                }
            });

            loadScaleSlider.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    loadScaleLabel.setText(Integer.toString((int)(loadScaleSlider.getValue() * 100)) + "%");
                    if (!loadScaleSlider.isDragging()) {
                        loadSkeleton(skeletonFile);
                        ui.toast("Reloaded.");
                    }
                    loadScaleResetButton.setText(loadScaleSlider.getValue() == 1 ? "Reload" : "Reset");
                }
            });
            loadScaleResetButton.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    resetCameraPosition();
                    if (loadScaleSlider.getValue() == 1) {
                        loadSkeleton(skeletonFile);
                        ui.toast("Reloaded.");
                    } else
                        loadScaleSlider.setValue(1);
                    loadScaleResetButton.setText("Reload");
                }
            });

            zoomSlider.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    zoomLabel.setText(Integer.toString((int)(zoomSlider.getValue() * 100)) + "%");
                    float newZoom = 1 / zoomSlider.getValue();
                    skeletonDisplayer.getCamera().position.x -= window.getWidth() / 2 * (newZoom - skeletonDisplayer.getCamera().zoom);
                    skeletonDisplayer.getCamera().zoom = newZoom;
                }
            });
            zoomResetButton.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    resetCameraPosition();
                    float x = skeletonDisplayer.getCamera().position.x;
                    zoomSlider.setValue(1);
                    skeletonDisplayer.getCamera().position.x = x;
                }
            });

            xScaleSlider.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    if (xScaleSlider.getValue() == 0) xScaleSlider.setValue(0.01f);
                    xScaleLabel.setText(Integer.toString((int)(xScaleSlider.getValue() * 100)) + "%");
                }
            });
            xScaleResetButton.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    xScaleSlider.setValue(1);
                }
            });

            yScaleSlider.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    if (yScaleSlider.getValue() == 0) yScaleSlider.setValue(0.01f);
                    yScaleLabel.setText(Integer.toString((int)(yScaleSlider.getValue() * 100)) + "%");
                }
            });
            yScaleResetButton.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    yScaleSlider.setValue(1);
                }
            });

            speedSlider.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    speedLabel.setText(Float.toString((int)(speedSlider.getValue() * 100) / 100f) + "x");
                }
            });
            speedResetButton.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    speedSlider.setValue(1);
                }
            });

            alphaSlider.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    alphaLabel.setText(Integer.toString((int)(alphaSlider.getValue() * 100)) + "%");
                    int track = trackButtons.getCheckedIndex();
                    if (track > 0) {
                        AnimationState.TrackEntry current = skeletonDisplayer.getState().getCurrent(track);
                        if (current != null) {
                            current.setAlpha(alphaSlider.getValue());
                            current.resetRotationDirections();
                        }
                    }
                }
            });

            mixSlider.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    mixLabel.setText(Float.toString((int)(mixSlider.getValue() * 100) / 100f) + "s");
                    if (skeletonDisplayer.getState() != null) skeletonDisplayer.getState().getData().setDefaultMix(mixSlider.getValue());
                }
            });

            InputListener scrollFocusListener = new InputListener() {
                public void enter (InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    if (pointer == -1) stage.setScrollFocus(event.getListenerActor());
                }

                public void exit (InputEvent event, float x, float y, int pointer, Actor toActor) {
                    if (pointer == -1 && stage.getScrollFocus() == event.getListenerActor()) stage.setScrollFocus(null);
                }
            };

            animationList.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    if (skeletonDisplayer.getState() != null) {
                        String name = animationList.getSelected();
                        if (name == null)
                            skeletonDisplayer.getState().setEmptyAnimation(trackButtons.getCheckedIndex(), mixSlider.getValue());
                        else
                            setAnimation();
                    }
                }
            });
            animationScroll.addListener(scrollFocusListener);

            loopCheckbox.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    setAnimation();
                }
            });

            addCheckbox.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    setAnimation();
                }
            });

            linearCheckbox.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    if (skeletonDisplayer.getAtlas() == null) return;
                    Texture.TextureFilter filter = linearCheckbox.isChecked() ? Texture.TextureFilter.Linear : Texture.TextureFilter.Nearest;
                    for (Texture texture : skeletonDisplayer.getAtlas().getTextures())
                        texture.setFilter(filter, filter);
                }
            });

            skinList.addListener(new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    if (skeletonDisplayer.getSkeleton() != null) {
                        String skinName = skinList.getSelected();
                        if (skinName == null)
                            skeletonDisplayer.getSkeleton().setSkin((Skin)null);
                        else
                            skeletonDisplayer.getSkeleton().setSkin(skinName);
                        skeletonDisplayer.getSkeleton().setSlotsToSetupPose();
                    }
                }
            });
            skinScroll.addListener(scrollFocusListener);

            ChangeListener trackButtonListener = new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    int track = trackButtons.getCheckedIndex();
                    if (track == -1) return;
                    AnimationState.TrackEntry current = skeletonDisplayer.getState().getCurrent(track);
                    animationList.getSelection().setProgrammaticChangeEvents(false);
                    animationList.setSelected(current == null ? null : current.animation.name);
                    animationList.getSelection().setProgrammaticChangeEvents(true);

                    alphaSlider.setDisabled(track == 0);
                    alphaSlider.setValue(current == null ? 1 : current.alpha);

                    addCheckbox.setDisabled(track == 0);

                    if (current != null) {
                        loopCheckbox.setChecked(current.getLoop());
                        addCheckbox.setChecked(current.getMixBlend() == Animation.MixBlend.add);
                    }
                }
            };
            for (TextButton button : trackButtons.getButtons())
                button.addListener(trackButtonListener);

            Gdx.input.setInputProcessor(new InputMultiplexer(stage, new InputAdapter() {
                float offsetX;
                float offsetY;

                public boolean touchDown (int screenX, int screenY, int pointer, int button) {
                    offsetX = screenX;
                    offsetY = Gdx.graphics.getHeight() - 1 - screenY;
                    return false;
                }

                public boolean touchDragged (int screenX, int screenY, int pointer) {
                    float deltaX = screenX - offsetX;
                    float deltaY = Gdx.graphics.getHeight() - 1 - screenY - offsetY;

                    skeletonDisplayer.getCamera().position.x -= deltaX * skeletonDisplayer.getCamera().zoom;
                    skeletonDisplayer.getCamera().position.y -= deltaY * skeletonDisplayer.getCamera().zoom;

                    offsetX = screenX;
                    offsetY = Gdx.graphics.getHeight() - 1 - screenY;
                    return false;
                }

                public boolean touchUp (int screenX, int screenY, int pointer, int button) {
                    savePrefs();
                    return false;
                }

                public boolean scrolled (int amount) {
                    float zoom = zoomSlider.getValue(), zoomMin = zoomSlider.getMinValue(), zoomMax = zoomSlider.getMaxValue();
                    float speedAlpha = Math.min(1.2f, (zoom - zoomMin) / (zoomMax - zoomMin) * 3.5f);
                    zoom -= Interpolation.linear.apply(0.02f, 0.2f, speedAlpha) * Math.signum(amount);
                    zoomSlider.setValue(MathUtils.clamp(zoom, zoomMin, zoomMax));
                    return false;
                }
            }));

            ChangeListener savePrefsListener = new ChangeListener() {
                public void changed (ChangeEvent event, Actor actor) {
                    if (actor instanceof Slider && ((Slider)actor).isDragging()) return;
                    savePrefs();
                }
            };
            debugBonesCheckbox.addListener(savePrefsListener);
            debugRegionsCheckbox.addListener(savePrefsListener);
            debugMeshHullCheckbox.addListener(savePrefsListener);
            debugMeshTrianglesCheckbox.addListener(savePrefsListener);
            debugPathsCheckbox.addListener(savePrefsListener);
            debugPointsCheckbox.addListener(savePrefsListener);
            debugClippingCheckbox.addListener(savePrefsListener);
            premultipliedCheckbox.addListener(savePrefsListener);
            loopCheckbox.addListener(savePrefsListener);
            addCheckbox.addListener(savePrefsListener);
            speedSlider.addListener(savePrefsListener);
            speedResetButton.addListener(savePrefsListener);
            mixSlider.addListener(savePrefsListener);
            loadScaleSlider.addListener(savePrefsListener);
            loadScaleResetButton.addListener(savePrefsListener);
            zoomSlider.addListener(savePrefsListener);
            zoomResetButton.addListener(savePrefsListener);
            animationList.addListener(savePrefsListener);
            skinList.addListener(savePrefsListener);
        }

        Table table (Actor... actors) {
            Table table = new Table(skin);
            table.defaults().space(6);
            table.add(actors);
            return table;
        }

        void render () {
            if (skeletonDisplayer.getState() != null && skeletonDisplayer.getState().getCurrent(trackButtons.getCheckedIndex()) == null) {
                animationList.getSelection().setProgrammaticChangeEvents(false);
                animationList.setSelected(null);
                animationList.getSelection().setProgrammaticChangeEvents(true);
            }

            statusLabel.pack();
            if (minimizeButton.isChecked())
                statusLabel.setPosition(10, 25, Align.bottom | Align.left);
            else
                statusLabel.setPosition(window.getWidth() + 6, 5, Align.bottom | Align.left);

            stage.act();
            stage.draw();
        }

        void toast (String text) {
            Table table = new Table();
            table.add(new Label(text, skin));
            table.getColor().a = 0;
            table.pack();
            table.setPosition(-table.getWidth(), -3 - table.getHeight());
            table.addAction(Actions.sequence( //
                    Actions.parallel(Actions.moveBy(0, table.getHeight(), 0.3f), Actions.fadeIn(0.3f)), //
                    Actions.delay(5f), //
                    Actions.parallel(Actions.moveBy(0, table.getHeight(), 0.3f), Actions.fadeOut(0.3f)), //
                    Actions.removeActor() //
            ));
            for (Actor actor : toasts.getChildren())
                actor.addAction(Actions.moveBy(0, table.getHeight(), 0.3f));
            toasts.addActor(table);
            toasts.getParent().toFront();
        }

        void savePrefs () {
            if (!prefsLoaded) return;
            prefs.putBoolean("debugBones", debugBonesCheckbox.isChecked());
            prefs.putBoolean("debugRegions", debugRegionsCheckbox.isChecked());
            prefs.putBoolean("debugMeshHull", debugMeshHullCheckbox.isChecked());
            prefs.putBoolean("debugMeshTriangles", debugMeshTrianglesCheckbox.isChecked());
            prefs.putBoolean("debugPaths", debugPathsCheckbox.isChecked());
            prefs.putBoolean("debugPoints", debugPointsCheckbox.isChecked());
            prefs.putBoolean("debugClipping", debugClippingCheckbox.isChecked());
            prefs.putBoolean("premultiplied", premultipliedCheckbox.isChecked());
            prefs.putBoolean("loop", loopCheckbox.isChecked());
            prefs.putBoolean("add", addCheckbox.isChecked());
            prefs.putFloat("speed", speedSlider.getValue());
            prefs.putFloat("mix", mixSlider.getValue());
            prefs.putFloat("scale", loadScaleSlider.getValue());
            prefs.putFloat("zoom", zoomSlider.getValue());
            prefs.putFloat("x", skeletonDisplayer.getCamera().position.x);
            prefs.putFloat("y", skeletonDisplayer.getCamera().position.y);
            if (skeletonDisplayer.getState() != null) {
                AnimationState.TrackEntry current = skeletonDisplayer.getState().getCurrent(0);
                if (current != null) {
                    String name = current.animation.name;
                    if (name.equals("<empty>")) name = current.next == null ? "" : current.next.animation.name;
                    prefs.putString("animationName", name);
                }
            }
            if (skinList.getSelected() != null) prefs.putString("skinName", skinList.getSelected());
            prefs.flush();
        }

        void loadPrefs () {
            try {
                debugBonesCheckbox.setChecked(prefs.getBoolean("debugBones", true));
                debugRegionsCheckbox.setChecked(prefs.getBoolean("debugRegions", false));
                debugMeshHullCheckbox.setChecked(prefs.getBoolean("debugMeshHull", false));
                debugMeshTrianglesCheckbox.setChecked(prefs.getBoolean("debugMeshTriangles", false));
                debugPathsCheckbox.setChecked(prefs.getBoolean("debugPaths", true));
                debugPointsCheckbox.setChecked(prefs.getBoolean("debugPoints", true));
                debugClippingCheckbox.setChecked(prefs.getBoolean("debugClipping", true));
                premultipliedCheckbox.setChecked(prefs.getBoolean("premultiplied", true));
                loopCheckbox.setChecked(prefs.getBoolean("loop", true));
                addCheckbox.setChecked(prefs.getBoolean("add", false));
                speedSlider.setValue(prefs.getFloat("speed", 0.3f));
                mixSlider.setValue(prefs.getFloat("mix", 0.3f));

                zoomSlider.setValue(prefs.getFloat("zoom", 1));
                skeletonDisplayer.getCamera().zoom = 1 / prefs.getFloat("zoom", 1);
                skeletonDisplayer.getCamera().position.x = prefs.getFloat("x", 0);
                skeletonDisplayer.getCamera().position.y = prefs.getFloat("y", 0);

                loadScaleSlider.setValue(prefs.getFloat("scale", 1));
                animationList.setSelected(prefs.getString("animationName", null));
                skinList.setSelected(prefs.getString("skinName", null));
            } catch (Throwable ex) {
                System.out.println("Unable to read preferences:");
                ex.printStackTrace();
            }
        }
    }
}
