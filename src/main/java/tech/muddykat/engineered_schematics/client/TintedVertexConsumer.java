package tech.muddykat.engineered_schematics.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Tints and fades everything passed through it, standing in for the schematic shader while a shader pack is loaded.
 */
public class TintedVertexConsumer implements VertexConsumer {
    // Stronger than the shader's 30% mix, since a multiplier can only darken and shader pack lighting washes it out further
    private static final float TINT_STRENGTH = 0.65f;
    private final VertexConsumer delegate;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    public TintedVertexConsumer(VertexConsumer delegate, float red, float green, float blue, float alpha) {
        this.delegate = delegate;
        this.red = 1 - TINT_STRENGTH + TINT_STRENGTH * red;
        this.green = 1 - TINT_STRENGTH + TINT_STRENGTH * green;
        this.blue = 1 - TINT_STRENGTH + TINT_STRENGTH * blue;
        this.alpha = alpha;
    }

    @Override
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        delegate.vertex(x, y, z, red * this.red, green * this.green, blue * this.blue, alpha * this.alpha, u, v, overlay, light, normalX, normalY, normalZ);
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color((int)(red * this.red), (int)(green * this.green), (int)(blue * this.blue), (int)(alpha * this.alpha));
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        delegate.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        delegate.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        delegate.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        delegate.defaultColor(red, green, blue, alpha);
    }

    @Override
    public void unsetDefaultColor() {
        delegate.unsetDefaultColor();
    }
}
