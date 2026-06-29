package net.discyupgrade.core.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.discyupgrade.core.block.LaserEmitterBlock;
import net.discyupgrade.core.block.LaserEmitterBlockEntity;

public class LaserEmitterRenderer implements BlockEntityRenderer<LaserEmitterBlockEntity> {
    public LaserEmitterRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(LaserEmitterBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (!be.isPowered() || be.getLevel() == null) return;

        BlockState state = be.getBlockState();
        Direction facing = state.getValue(LaserEmitterBlock.FACING);
        BlockPos pos = be.getBlockPos();

        Vec3 start = Vec3.atCenterOf(pos).add(facing.getStepX() * 0.55, 0.55, facing.getStepZ() * 0.55);
        Vec3 end = traceBeam(be, start, facing);

        int color = be.getLaserColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.85f;

        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        var mat = pose.last().pose();
        float dx = (float) (end.x - start.x);
        float dy = (float) (end.y - start.y);
        float dz = (float) (end.z - start.z);
        float nx = -dz;
        float nz = dx;
        float len = (float) Math.sqrt(nx * nx + nz * nz);
        if (len < 0.001f) {
            nx = 0.05f;
            nz = 0;
        } else {
            nx = nx / len * 0.03f;
            nz = nz / len * 0.03f;
        }

        float sx = (float) start.x - pos.getX();
        float sy = (float) start.y - pos.getY();
        float sz = (float) start.z - pos.getZ();
        float ex = sx + dx;
        float ey = sy + dy;
        float ez = sz + dz;

        vc.vertex(mat, sx - nx, sy, sz - nz).color(r, g, b, a).endVertex();
        vc.vertex(mat, sx + nx, sy, sz + nz).color(r, g, b, a).endVertex();
        vc.vertex(mat, ex + nx, ey, ez + nz).color(r, g, b, a).endVertex();
        vc.vertex(mat, ex - nx, ey, ez - nz).color(r, g, b, a).endVertex();
    }

    private static Vec3 traceBeam(LaserEmitterBlockEntity be, Vec3 start, Direction facing) {
        Vec3 step = new Vec3(facing.getStepX(), 0, facing.getStepZ()).scale(0.25);
        Vec3 current = start;
        for (int i = 0; i < 48; i++) {
            current = current.add(step);
            BlockPos check = BlockPos.containing(current);
            if (!be.getLevel().getBlockState(check).isAir()) {
                return current;
            }
        }
        return current;
    }
}
