package cofh.thermalexpansion.render;

import codechicken.lib.lighting.LightModel;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.block.ICCBlockRenderer;
import codechicken.lib.render.buffer.BakingVertexBuffer;
import codechicken.lib.render.item.IItemRenderer;
import codechicken.lib.texture.TextureUtils.IIconRegister;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Translation;
import cofh.core.render.IconRegistry;
import cofh.core.render.RenderUtils;
import cofh.lib.render.RenderHelper;
import cofh.thermalexpansion.block.TEBlocks;
import cofh.thermalexpansion.block.simple.BlockFrame;
import cofh.thermalexpansion.block.simple.BlockFrame.Types;
import cofh.thermalexpansion.client.bakery.ISimpleBlockBakery;
import cofh.thermalexpansion.core.TEProps;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RenderFrame implements ISimpleBlockBakery, IIconRegister {

    public static final RenderFrame instance = new RenderFrame();

    static CCModel modelCenter = CCModel.quadModel(24);
    static CCModel modelFrame = CCModel.quadModel(48);

    static {
        //TEProps.renderIdFrame = RenderingRegistry.getNextAvailableRenderId();
        //RenderingRegistry.registerBlockHandler(instance);

        //MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(TEBlocks.blockFrame), instance);

        modelCenter.generateBlock(0, 0.15, 0.15, 0.15, 0.85, 0.85, 0.85).computeNormals();

        Cuboid6 box = new Cuboid6(0, 0, 0, 1, 1, 1);
        double inset = 0.1875;
        modelFrame = CCModel.quadModel(48).generateBlock(0, box);
        CCModel.generateBackface(modelFrame, 0, modelFrame, 24, 24);
        modelFrame.computeNormals();
        for (int i = 24; i < 48; i++) {
            modelFrame.verts[i].vec.add(modelFrame.normals()[i].copy().multiply(inset));
        }
        modelFrame.computeLighting(LightModel.standardLightModel).shrinkUVs(RenderHelper.RENDER_OFFSET);
    }

    public static void initialize() {

    }

    @Override
    public void registerIcons(TextureMap textureMap) {
        IconRegistry.addIcon("FrameMachineBottom", "thermalexpansion:blocks/machine/machine_frame_bottom", textureMap);
        IconRegistry.addIcon("FrameMachineTop", "thermalexpansion:blocks/machine/machine_frame_top", textureMap);
        IconRegistry.addIcon("FrameMachineSide", "thermalexpansion:blocks/machine/machine_frame_side", textureMap);
        IconRegistry.addIcon("FrameCellBasic", "thermalexpansion:blocks/cell/cell_basic", textureMap);
        IconRegistry.addIcon("FrameCellHardened", "thermalexpansion:blocks/cell/cell_hardened", textureMap);
        IconRegistry.addIcon("FrameCellReinforced", "thermalexpansion:blocks/cell/cell_reinforced", textureMap);
        IconRegistry.addIcon("FrameCellResonant", "thermalexpansion:blocks/cell/cell_resonant", textureMap);
        IconRegistry.addIcon("FrameTesseract", "thermalexpansion:blocks/tesseract/tesseract", textureMap);
        IconRegistry.addIcon("FrameIlluminator", "thermalexpansion:blocks/light/illuminator_frame", textureMap);

        IconRegistry.addIcon("FrameMachineInner", "thermalexpansion:blocks/machine/machine_frame_inner", textureMap);
        IconRegistry.addIcon("FrameCellBasicInner", "thermalexpansion:blocks/cell/cell_basic_inner", textureMap);
        IconRegistry.addIcon("FrameCellHardenedInner", "thermalexpansion:blocks/cell/cell_hardened_inner", textureMap);
        IconRegistry.addIcon("FrameCellReinforcedInner", "thermalexpansion:blocks/cell/cell_reinforced_inner", textureMap);
        IconRegistry.addIcon("FrameCellResonantInner", "thermalexpansion:blocks/cell/cell_resonant_inner", textureMap);
        IconRegistry.addIcon("FrameTesseractInner", "thermalexpansion:blocks/tesseract/tesseract_inner", textureMap);
        IconRegistry.addIcon("FrameIlluminatorInner", "thermalexpansion:blocks/config/config_none", textureMap);

        IconRegistry.addIcon("FrameCenter" + 0, "thermalfoundation:blocks/storage/tin", textureMap);
        IconRegistry.addIcon("FrameCenter" + 1, "thermalfoundation:blocks/storage/electrum", textureMap);
        IconRegistry.addIcon("FrameCenter" + 2, "thermalfoundation:blocks/storage/signalum", textureMap);
        IconRegistry.addIcon("FrameCenter" + 3, "thermalfoundation:blocks/storage/enderium", textureMap);
        IconRegistry.addIcon("FrameCenter" + 4, "thermalexpansion:blocks/cell/cell_center_solid", textureMap);
        IconRegistry.addIcon("FrameCenter" + 5, "thermalexpansion:blocks/cell/cell_center_solid", textureMap);
        IconRegistry.addIcon("FrameCenter" + 6, "thermalexpansion:blocks/config/config_none", textureMap);
        IconRegistry.addIcon("FrameCenter" + 7, "thermalfoundation:blocks/fluid/redstone_still", textureMap);
        IconRegistry.addIcon("FrameCenter" + 8, "thermalexpansion:blocks/config/config_none", textureMap);
        IconRegistry.addIcon("FrameCenter" + 9, "thermalfoundation:blocks/fluid/redstone_still", textureMap);
        IconRegistry.addIcon("FrameCenter" + 10, "thermalexpansion:blocks/config/config_none", textureMap);
        IconRegistry.addIcon("FrameCenter" + 11, "thermalfoundation:blocks/fluid/ender_still", textureMap);
        IconRegistry.addIcon("FrameCenter" + 12, "thermalexpansion:blocks/config/config_none", textureMap);
    }

    public TextureAtlasSprite getIcon(int side, int metadata) {

        if (side == 6) {
            return getInnerIcon(metadata);
        } else if (side == 7) {
            return getCenterIcon(metadata);
        }
        return getFrameIcon(side, metadata);
    }

    private TextureAtlasSprite getFrameIcon(int side, int metadata) {

        switch (Types.values()[metadata]) {
            case CELL_BASIC:
                return IconRegistry.getIcon("FrameCellBasic");
            case CELL_HARDENED:
                return IconRegistry.getIcon("FrameCellHardened");
            case CELL_REINFORCED_EMPTY:
            case CELL_REINFORCED_FULL:
                return IconRegistry.getIcon("FrameCellReinforced");
            case CELL_RESONANT_EMPTY:
            case CELL_RESONANT_FULL:
                return IconRegistry.getIcon("FrameCellResonant");
            case TESSERACT_EMPTY:
            case TESSERACT_FULL:
                return IconRegistry.getIcon("FrameTesseract");
            case ILLUMINATOR:
                return IconRegistry.getIcon("FrameIlluminator");
            default:
                if (side == 0) {
                    return IconRegistry.getIcon("FrameMachineBottom");
                }
                if (side == 1) {
                    return IconRegistry.getIcon("FrameMachineTop");
                }
                return IconRegistry.getIcon("FrameMachineSide");
        }
    }

    private TextureAtlasSprite getInnerIcon(int metadata) {

        switch (Types.values()[metadata]) {
            case CELL_BASIC:
                return IconRegistry.getIcon("FrameCellBasicInner");
            case CELL_HARDENED:
                return IconRegistry.getIcon("FrameCellHardenedInner");
            case CELL_REINFORCED_EMPTY:
            case CELL_REINFORCED_FULL:
                return IconRegistry.getIcon("FrameCellReinforcedInner");
            case CELL_RESONANT_EMPTY:
            case CELL_RESONANT_FULL:
                return IconRegistry.getIcon("FrameCellResonantInner");
            case TESSERACT_EMPTY:
            case TESSERACT_FULL:
                return IconRegistry.getIcon("FrameTesseractInner");
            case ILLUMINATOR:
                return IconRegistry.getIcon("FrameIlluminatorInner");
            default:
                return IconRegistry.getIcon("FrameMachineInner");
        }
    }

    private TextureAtlasSprite getCenterIcon(int metadata) {

        return IconRegistry.getIcon("FrameCenter", metadata);
    }

    @Override
    public List<BakedQuad> bakeQuads(EnumFacing face, IExtendedBlockState state) {
        if (face == null){
            BakingVertexBuffer buffer = BakingVertexBuffer.create();
            buffer.begin(7, DefaultVertexFormats.ITEM);
            CCRenderState ccrs = CCRenderState.instance();
            ccrs.reset();
            ccrs.bind(buffer);
            int meta = state.getBlock().getMetaFromState(state);
            renderFrame(ccrs, meta);
            renderCenter(ccrs, meta);
            buffer.finishDrawing();
            return buffer.bake();
        }
        return new ArrayList<BakedQuad>();
    }

    @Override
    public IExtendedBlockState handleState(IExtendedBlockState state, TileEntity tileEntity) {
        return state;//NOOP
    }

    @Override
    public List<BakedQuad> bakeItemQuads(EnumFacing face, ItemStack stack) {
        if (face == null){
            BakingVertexBuffer buffer = BakingVertexBuffer.create();
            buffer.begin(7, DefaultVertexFormats.ITEM);
            CCRenderState ccrs = CCRenderState.instance();
            ccrs.reset();
            ccrs.bind(buffer);
            int meta = stack.getMetadata();
            renderFrame(ccrs, meta);
            renderCenter(ccrs, meta);
            buffer.finishDrawing();
            return buffer.bake();
        }
        return new ArrayList<BakedQuad>();
    }

    public void renderCenter(CCRenderState ccrs, int metadata) {

        modelCenter.render(ccrs, RenderUtils.getIconTransformation(getIcon(7, metadata)));
    }

    public void renderFrame(CCRenderState ccrs, int metadata) {

        for (int i = 0; i < 6; i++) {
            modelFrame.render(ccrs, i * 4, i * 4 + 4,  RenderUtils.getIconTransformation(getIcon(i, metadata)));
            modelFrame.render(ccrs, i * 4 + 24, i * 4 + 28,  RenderUtils.getIconTransformation(getIcon(6, metadata)));
        }
    }
}
