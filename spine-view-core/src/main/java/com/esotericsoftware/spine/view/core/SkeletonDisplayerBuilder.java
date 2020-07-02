package com.esotericsoftware.spine.view.core;

public class SkeletonDisplayerBuilder {
    String atlasGdxFilePath;
    String skeletonGdxFilePath;
    SkeletonDisplayListener displayListener;

    public SkeletonDisplayerBuilder setAtlasGdxFilePath(String atlasGdxFilePath) {
        this.atlasGdxFilePath = atlasGdxFilePath;
        return this;
    }

    public SkeletonDisplayerBuilder setSkeletonGdxFilePath(String skeletonGdxFilePath) {
        this.skeletonGdxFilePath = skeletonGdxFilePath;
        return this;
    }

//    public SkeletonDisplayerBuilder setSkeletonDisplayListener(SkeletonDisplayListener displayListener) {
//        this.displayListener = displayListener;
//        return this;
//    }

    public SkeletonDisplayer build(){
        SkeletonDisplayer displayer = new SkeletonDisplayer();
        displayer.atlasGdxFilePath = atlasGdxFilePath;
        displayer.skeletonGdxFilePath = skeletonGdxFilePath;
//        displayer.displayListener = displayListener;
        return displayer;
    }
}
