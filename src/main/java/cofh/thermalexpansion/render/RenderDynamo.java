package cofh.thermalexpansion.render;

import codechicken.lib.render.buffer.BakingVertexBuffer;
import codechicken.lib.texture.TextureUtils;
import codechicken.lib.texture.TextureUtils.IIconRegister;
import cofh.core.render.IconRegistry;
import cofh.core.render.RenderUtils;
import cofh.lib.render.RenderHelper;
import codechicken.lib.lighting.LightModel;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import cofh.thermalexpansion.block.CommonProperties;
import cofh.thermalexpansion.block.dynamo.BlockDynamo;
import cofh.thermalexpansion.block.dynamo.BlockDynamo.Types;
import cofh.thermalexpansion.block.dynamo.TileDynamoBase;
import cofh.thermalexpansion.client.bakery.ISimpleBlockBakery;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.property.IExtendedBlockState;

import java.util.ArrayList;
import java.util.List;

public class RenderDynamo implements IIconRegister, ISimpleBlockBakery {

	public static final RenderDynamo instance = new RenderDynamo();

	static TextureAtlasSprite textureCoil;
	static TextureAtlasSprite[] textureBase = new TextureAtlasSprite[BlockDynamo.Types.values().length];
	static CCModel[][] modelCoil = new CCModel[2][6];
	static CCModel[][] modelBase = new CCModel[2][6];
	static CCModel[] modelAnimation = new CCModel[6];

	static {
		//TEProps.renderIdDynamo = RenderingRegistry.getNextAvailableRenderId();
		//RenderingRegistry.registerBlockHandler(instance);

		generateModels();
	}

	public static void initialize() {

		textureCoil = IconRegistry.getIcon("DynamoCoilRedstone");

		for (int i = 0; i < textureBase.length; i++) {
			textureBase[i] = IconRegistry.getIcon("Dynamo", i);
		}
	}

    @Override
    public void registerIcons(TextureMap textureMap) {
        IconRegistry.addIcon("DynamoCoilRedstone", "thermalexpansion:blocks/dynamo/dynamo_coil_redstone", textureMap);

        IconRegistry.addIcon("Dynamo" + Types.STEAM.ordinal(), "thermalexpansion:blocks/dynamo/dynamo_steam", textureMap);
        IconRegistry.addIcon("Dynamo" + Types.MAGMATIC.ordinal(), "thermalexpansion:blocks/dynamo/dynamo_magmatic", textureMap);
        IconRegistry.addIcon("Dynamo" + Types.COMPRESSION.ordinal(), "thermalexpansion:blocks/dynamo/dynamo_compression", textureMap);
        IconRegistry.addIcon("Dynamo" + Types.REACTANT.ordinal(), "thermalexpansion:blocks/dynamo/dynamo_reactant", textureMap);
        IconRegistry.addIcon("Dynamo" + Types.ENERVATION.ordinal(), "thermalexpansion:blocks/dynamo/dynamo_enervation", textureMap);
    }

    @Override
    public IExtendedBlockState handleState(IExtendedBlockState state, TileEntity tile) {
        TileDynamoBase dynamo = (TileDynamoBase) tile;
        state = state.withProperty(CommonProperties.FACING_PROPERTY, dynamo.getFacing());
        state = state.withProperty(CommonProperties.ACTIVE_PROPERTY, dynamo.isActive);
        state = state.withProperty(CommonProperties.TYPE_PROPERTY, dynamo.getType());
        state = state.withProperty(CommonProperties.ACTIVE_SPRITE_PROPERTY, new ResourceLocation(dynamo.getActiveIcon().getIconName()));
        return state;
    }

    @Override
    public List<BakedQuad> bakeQuads(EnumFacing face, IExtendedBlockState state) {
        if (face == null) {
            int facing = state.getValue(CommonProperties.FACING_PROPERTY);
            boolean active = state.getValue(CommonProperties.ACTIVE_PROPERTY);
            int type = state.getValue(CommonProperties.TYPE_PROPERTY);
            TextureAtlasSprite activeSprite = TextureUtils.getTexture(state.getValue(CommonProperties.ACTIVE_SPRITE_PROPERTY));

            BakingVertexBuffer buffer = BakingVertexBuffer.create();
            buffer.begin(7, DefaultVertexFormats.ITEM);
            CCRenderState ccrs = CCRenderState.instance();
            ccrs.reset();
            ccrs.bind(buffer);
            renderCoil(ccrs, facing, active);
            renderAnimation(ccrs, facing, active, type, activeSprite);
            renderBase(ccrs, facing, active, type);
            buffer.finishDrawing();
            return buffer.bake();
        }
        return new ArrayList<BakedQuad>();
    }

    @Override
    public List<BakedQuad> bakeItemQuads(EnumFacing face, ItemStack stack) {
        if(face == null){
            BakingVertexBuffer buffer = BakingVertexBuffer.create();
            buffer.begin(7, DefaultVertexFormats.ITEM);
            CCRenderState ccrs = CCRenderState.instance();
            ccrs.reset();
            ccrs.bind(buffer);
            renderCoil(ccrs, 1, false);
            renderBase(ccrs, 1, false, stack.getMetadata());
            buffer.finishDrawing();
            return buffer.bake();
        }
        return new ArrayList<BakedQuad>();
    }

    private static void generateModels() {

		double d1 = RenderHelper.RENDER_OFFSET;
		double d2 = 6F / 16F;
		double d3 = 10F / 16F;

		modelCoil[0][1] = CCModel.quadModel(24).generateBox(0, -4, 0, -4, 8, 8, 8, 0, 0, 32, 32, 16).computeNormals().shrinkUVs(d1);
		modelCoil[1][1] = CCModel.quadModel(24).generateBox(0, -4, 0, -4, 8, 8, 8, 0, 16, 32, 32, 16).computeNormals().shrinkUVs(d1);

		modelBase[0][1] = CCModel.quadModel(24).generateBox(0, -8, -8, -8, 16, 10, 16, 0, 0, 64, 64, 16).computeNormals().shrinkUVs(d1);
		modelBase[1][1] = CCModel.quadModel(24).generateBox(0, -8, -8, -8, 16, 10, 16, 0, 32, 64, 64, 16).computeNormals().shrinkUVs(d1);

		modelAnimation[0] = CCModel.quadModel(16).generateBlock(0, d1, d2 - d1, d1, 1 - d1, 1 - d1, 1 - d1, 3).computeNormals();
		modelAnimation[1] = CCModel.quadModel(16).generateBlock(0, d1, d1, d1, 1 - d1, d3 - d1, 1 - d1, 3).computeNormals();

		modelAnimation[2] = CCModel.quadModel(16).generateBlock(0, d1, d1, d2 - d1, 1 - d1, 1 - d1, 1 - d1, 12).computeNormals();
		modelAnimation[3] = CCModel.quadModel(16).generateBlock(0, d1, d1, d1, 1 - d1, 1 - d1, d3 - d1, 12).computeNormals();

		modelAnimation[4] = CCModel.quadModel(16).generateBlock(0, d2 - d1, d1, d1, 1 - d1, 1 - d1, 1 - d1, 48).computeNormals();
		modelAnimation[5] = CCModel.quadModel(16).generateBlock(0, d1, d1, d1, d3 - d1, 1 - d1, 1 - d1, 48).computeNormals();

		for (int j = modelAnimation.length; j-- > 0;) {
			modelAnimation[j].computeLighting(LightModel.standardLightModel);
		}

		for (int i = 0; i < modelCoil.length; i++) {
			CCModel.generateSidedModels(modelCoil[i], 1, new Vector3());
			for (int j = modelCoil[i].length; j-- > 0;) {
				modelCoil[i][j].computeLighting(LightModel.standardLightModel);
			}
		}
		for (int i = 0; i < modelBase.length; i++) {
			CCModel.generateSidedModels(modelBase[i], 1, new Vector3());
			for (int j = modelBase[i].length; j-- > 0;) {
				modelBase[i][j].computeLighting(LightModel.standardLightModel);
			}
		}
	}

	public void renderCoil(CCRenderState ccrs, int facing, boolean active) {


		if (active) {
			modelCoil[0][facing].render(ccrs, new Translation(0.5, 0.5, 0.5), RenderUtils.getIconTransformation(textureCoil));
		} else {
			modelCoil[1][facing].render(ccrs, new Translation(0.5, 0.5, 0.5), RenderUtils.getIconTransformation(textureCoil));
		}
	}

	public void renderBase(CCRenderState ccrs, int facing, boolean active, int type) {


		if (active) {
			modelBase[0][facing].render(ccrs, new Translation(0.5, 0.5, 0.5), RenderUtils.getIconTransformation(textureBase[type]));
		} else {
			modelBase[1][facing].render(ccrs, new Translation(0.5, 0.5, 0.5), RenderUtils.getIconTransformation(textureBase[type]));
		}
	}

	public void renderAnimation(CCRenderState ccrs, int facing, boolean active, int type, TextureAtlasSprite icon) {

		if (active) {
			modelAnimation[facing].render(ccrs, RenderUtils.getIconTransformation(icon));
		}
	}

	//@Override
	//public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
//
	//	RenderUtils.preItemRender();
//
	//	CCRenderState.startDrawing();
	//	renderCoil(1, false, -0.5, -0.5, -0.5);
	//	renderBase(1, false, metadata, -0.5, -0.5, -0.5);
	//	CCRenderState.draw();
//
	//	RenderUtils.postItemRender();
	//}

	//@Override
	//public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
//
	//	TileEntity tile = world.getTileEntity(x, y, z);
	//	if (!(tile instanceof TileDynamoBase)) {
	//		return false;
	//	}
	//	TileDynamoBase theTile = (TileDynamoBase) tile;
//
	//	RenderUtils.preWorldRender(world, x, y, z);
	//	renderCoil(theTile.getFacing(), theTile.isActive, x, y, z);
	//	renderAnimation(theTile.getFacing(), theTile.isActive, theTile.getType(), theTile.getActiveIcon(), x, y, z);
	//	renderBase(theTile.getFacing(), theTile.isActive, theTile.getType(), x, y, z);
//
	//	return true;
	//}

	//@Override
	//public boolean shouldRender3DInInventory(int modelId) {
//
	//	return true;
	//}

	//@Override
	//public int getRenderId() {
//
	//	return TEProps.renderIdDynamo;
	//}

}
