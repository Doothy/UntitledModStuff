package org.doothy.untitled.items;

public interface LightningSoundHelper {
    void start();
    void stop();
    void applyRecoil(); // Add this

    class Holder {
        public static LightningSoundHelper INSTANCE = new LightningSoundHelper() {
            @Override public void start() {}
            @Override public void stop() {}
            @Override public void applyRecoil() {} // Add this
        };
    }
}