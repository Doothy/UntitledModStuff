package org.doothy.untitled.items;

/**
 * Interface for handling client-side lightning sound effects.
 * This allows the server-side item code to trigger client-side sounds without crashing.
 */
public interface LightningSoundHelper {
    /** Starts the sound effect. */
    void start();
    /** Stops the sound effect. */
    void stop();
    /** Applies recoil effect (visual/sound). */
    void applyRecoil();

    /**
     * Holder class for the singleton instance.
     * The instance is replaced by the client-side implementation during client initialization.
     */
    class Holder {
        public static LightningSoundHelper INSTANCE = new LightningSoundHelper() {
            @Override public void start() {}
            @Override public void stop() {}
            @Override public void applyRecoil() {}
        };
    }
}