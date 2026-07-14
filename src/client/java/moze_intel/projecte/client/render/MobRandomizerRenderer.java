package moze_intel.projecte.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.content.entity.MobRandomizerProjectile;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Camera-facing sprite renderer matching ProjectE's mob randomizer orb. */
public final class MobRandomizerRenderer
      extends EntityRenderer<MobRandomizerProjectile, EntityRenderState> {
    private static final Identifier TEXTURE = ProjectEAPI.id(
          "textures/entity/randomizer.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);

    public MobRandomizerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected int getBlockLightLevel(MobRandomizerProjectile entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void submit(
          EntityRenderState state,
          PoseStack poseStack,
          SubmitNodeCollector collector,
          CameraRenderState camera
    ) {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(camera.orientation);
        collector.submitCustomGeometry(
              poseStack, RENDER_TYPE,
              (pose, consumer) -> buildQuad(state, pose, consumer, -1));
        if (state.outlineColor != EntityRenderState.NO_OUTLINE) {
            RENDER_TYPE.outline().ifPresent(outline -> collector.submitCustomGeometry(
                  poseStack, outline,
                  (pose, consumer) -> buildQuad(
                        state, pose, consumer, state.outlineColor)));
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void buildQuad(
          EntityRenderState state,
          PoseStack.Pose pose,
          VertexConsumer consumer,
          int color
    ) {
        vertex(consumer, pose, state.lightCoords, 0, 0, 0, 1, color);
        vertex(consumer, pose, state.lightCoords, 1, 0, 1, 1, color);
        vertex(consumer, pose, state.lightCoords, 1, 1, 1, 0, color);
        vertex(consumer, pose, state.lightCoords, 0, 1, 0, 0, color);
    }

    private static void vertex(
          VertexConsumer consumer,
          PoseStack.Pose pose,
          int light,
          float x,
          int y,
          int u,
          int v,
          int color
    ) {
        consumer.addVertex(pose, x - 0.5F, y, 0)
              .setColor(color)
              .setUv(u, v)
              .setOverlay(OverlayTexture.NO_OVERLAY)
              .setLight(light)
              .setNormal(pose, 0, 1, 0);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
