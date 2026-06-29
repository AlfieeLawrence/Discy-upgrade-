package net.discyupgrade.core.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.discyupgrade.core.block.DiscoBallBlockEntity;
import net.discyupgrade.core.util.ModIdentifier;

public class DiscoBallRenderer implements BlockEntityRenderer<DiscoBallBlockEntity> {
    private static final ResourceLocation TEXTURE = ModIdentifier.of("textures/block/disco_ball.png");

    public DiscoBallRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(DiscoBallBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 1.0, 0.5);
        float spin = be.getSpin() + (be.isActive() ? partialTick * 4f : 0f);
        pose.mulPose(Axis.YP.rotationDegrees(spin));

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        var poseEntry = pose.last();
        var mat = poseEntry.pose();
        var normal = poseEntry.normal();
        float s = 0.45f;
        float y0 = -0.9f;

        quad(vc, mat, normal, light, overlay, -s, y0, -s, s, y0, -s, s, y0, s, -s, y0, s, 0, 0, 1, 0);
        quad(vc, mat, normal, light, overlay, s, y0, s, s, y0, -s, -s, y0, -s, -s, y0, s, 1, 0, 0, 1);
        quad(vc, mat, normal, light, overlay, -s, y0, s, s, y0, s, s, y0, -s, -s, y0, -s, 0, 0, 0, 1);
        quad(vc, mat, normal, light, overlay, -s, y0, -s, -s, y0, s, s, y0, s, s, y0, -s, 0, 0, 1, 1);

        pose.popPose();

        if (be.isActive() && be.getLevel() != null && be.getLevel().random.nextInt(4) == 0) {
            double x = be.getBlockPos().getX() + 0.5;
            double y = be.getBlockPos().getY() + 0.2;
            double z = be.getBlockPos().getZ() + 0.5;
            int[] colors = {0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0xFF00FF};
            int c = colors[be.getLevel().random.nextInt(colors.length)];
            float r = ((c >> 16) & 0xFF) / 255f;
            float g = ((c >> 8) & 0xFF) / 255f;
            float b = (c & 0xFF) / 255f;
            be.getLevel().addParticle(net.minecraft.core.particles.DustParticleOptions.REDSTONE,
                    x, y, z, r, g, b);
        }
    }

    private static void quad(VertexConsumer vc, org.joml.Matrix4f mat, org.joml.Matrix3f normal,
                             int light, int overlay,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float u0, float v0, float u1, float v1) {
        vc.vertex(mat, x0, y0, z0).color(255, 255, 255, 255).uv(u0, v0)
                .overlayCoords(overlay).uv2(light).normal(normal, 0, 1, 0).endVertex();
        vc.vertex(mat, x1, y1, z1).color(255, 255, 255, 255).uv(u1, v0)
                .overlayCoords(overlay).uv2(light).normal(normal, 0, 1, 0).endVertex();
        vc.vertex(mat, x2, y2, z2).color(255, 255, 255, 255).uv(u1, v1)
                .overlayCoords(overlay).uv2(light).normal(normal, 0, 1, 0).endVertex();
        vc.vertex(mat, x3, y3, z3).color(255, 255, 255, 255).uv(u0, v1)
                .overlayCoords(overlay).uv2(light).normal(normal, 0, 1, 0).endVertex();
    }
}
