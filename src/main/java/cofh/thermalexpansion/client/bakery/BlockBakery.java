package cofh.thermalexpansion.client.bakery;

import codechicken.lib.model.SimplePerspectiveAwareBakedModel;
import codechicken.lib.model.bakery.PlanarFaceBakery;
import codechicken.lib.texture.TextureUtils;
import codechicken.lib.util.TransformUtils;
import cofh.api.tileentity.ISidedTexture;
import cofh.lib.util.ItemWrapper;
import cofh.thermalexpansion.block.CommonProperties;
import cofh.thermalexpansion.client.IBlockLayerProvider;
import cofh.thermalexpansion.client.IBlockLayeredTextureProvider;
import cofh.thermalexpansion.client.IBlockTextureProvider;
import cofh.thermalexpansion.client.IControlledLayerProvider;
import cofh.thermalexpansion.client.model.SimplePerspectiveAwareBakedLayerModel;
import cofh.thermalexpansion.client.model.SimpleSmartBakedModel;
import cofh.thermalexpansion.client.model.SimpleSmartLayeredModel;
import cofh.thermalexpansion.client.model.SimpleSmartPerspectiveBakedItemModel;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by covers1624 on 25/10/2016.
 */
@SideOnly(Side.CLIENT)
public class BlockBakery implements IResourceManagerReloadListener {

    private static Cache<IExtendedBlockState, IBakedModel> stateModelCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();
    private static Cache<ItemWrapper, IBakedModel> itemModelCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

    static {
        TextureUtils.registerReloadListener(new BlockBakery());
    }

    public static IBlockState handleExtendedState(IExtendedBlockState state, TileEntity tileEntity) {
        Block block = state.getBlock();
        int blockMeta = block.getMetaFromState(state);

        if (block instanceof IBakeryBlock) {
            return ((IBakeryBlock) block).getCustomBakery().handleState(state, tileEntity);
        } else if (block instanceof IBlockLayeredTextureProvider) {
            IBlockLayeredTextureProvider textureProvider = (IBlockLayeredTextureProvider) block;

            HashMap<EnumFacing, Map<BlockRenderLayer, TextureAtlasSprite>> spriteFaceLayerMap = new HashMap<EnumFacing, Map<BlockRenderLayer, TextureAtlasSprite>>();
            for (EnumFacing face : EnumFacing.VALUES) {
                Map<BlockRenderLayer, TextureAtlasSprite> spriteLayerMap = new HashMap<BlockRenderLayer, TextureAtlasSprite>();
                for (int pass = 0; pass < textureProvider.getTexturePasses(); pass++) {
                    if (block instanceof IControlledLayerProvider) {
                        if (!((IControlledLayerProvider) block).shouldUsePass(pass, tileEntity)) {
                            continue;
                        }
                    }
                    BlockRenderLayer renderLayer = textureProvider.getRenderlayerForPass(pass);

                    TextureAtlasSprite sprite = ((ISidedTexture) tileEntity).getTexture(face.ordinal(), pass);
                    spriteLayerMap.put(renderLayer, sprite);
                }
                spriteFaceLayerMap.put(face, spriteLayerMap);
            }
            state = state.withProperty(CommonProperties.SPRITE_FACE_LAYER_PROPERTY, spriteFaceLayerMap);
            return state;
        }
        return state;
    }

    public static IBakedModel generateItemModel(ItemStack stack) {
        ItemWrapper wrapper = ItemWrapper.fromItemStack(stack);
        IBakedModel model = itemModelCache.getIfPresent(wrapper);

        if (model == null) {
            Block block = Block.getBlockFromItem(stack.getItem());
            if (block instanceof IBakeryBlock) {
                ICustomBlockBakery bakery = ((IBakeryBlock) block).getCustomBakery();
                List<BakedQuad> generalQuads = new LinkedList<BakedQuad>();
                Map<EnumFacing, List<BakedQuad>> faceQuadMap = new HashMap<EnumFacing, List<BakedQuad>>();
                generalQuads.addAll(bakery.bakeItemQuads(null, stack));

                for (EnumFacing face : EnumFacing.VALUES) {
                    List<BakedQuad> faceQuads = new LinkedList<BakedQuad>();

                    faceQuads.addAll(bakery.bakeItemQuads(face, stack));

                    faceQuadMap.put(face, faceQuads);
                }

                model = new SimpleSmartPerspectiveBakedItemModel(faceQuadMap, generalQuads, TransformUtils.DEFAULT_BLOCK);

            } else if (block instanceof IBlockLayeredTextureProvider) {

                IBlockLayeredTextureProvider provider = ((IBlockLayeredTextureProvider) block);
                LinkedList<BakedQuad> itemQuads = new LinkedList<BakedQuad>();
                itemQuads.addAll(bakeItemFace(EnumFacing.UP, provider.getTexture(EnumFacing.UP, stack.getMetadata())));
                itemQuads.addAll(bakeItemFace(EnumFacing.DOWN, provider.getTexture(EnumFacing.DOWN, stack.getMetadata())));
                for (EnumFacing face : EnumFacing.HORIZONTALS) {
                    itemQuads.addAll(bakeItemFace(face, provider.getTexture(face.getOpposite(), stack.getMetadata())));
                }
                model = new SimplePerspectiveAwareBakedModel(itemQuads, TransformUtils.DEFAULT_BLOCK);
            } else if (block instanceof IBlockTextureProvider) {
                IBlockTextureProvider provider = ((IBlockTextureProvider) block);
                Map<EnumFacing, List<BakedQuad>> faceQuadMap = new HashMap<EnumFacing, List<BakedQuad>>();
                for (EnumFacing face : EnumFacing.VALUES) {
                    List<BakedQuad> faceQuads = new LinkedList<BakedQuad>();

                    faceQuads.addAll(bakeItemFace(face, provider.getTexture(face, stack.getMetadata())));

                    faceQuadMap.put(face, faceQuads);
                }
                model = new SimpleSmartPerspectiveBakedItemModel(faceQuadMap, null, TransformUtils.DEFAULT_BLOCK);
            }

            if (model != null) {
                itemModelCache.put(wrapper, model);
            }
        }
        return model;
    }

    public static IBakedModel getCachedModel(IExtendedBlockState state) {
        IBakedModel model = stateModelCache.getIfPresent(state);
        if (model == null) {
            model = generateModel(state);
            stateModelCache.put(state, model);
        }
        return model;
    }

    public static IBakedModel generateModel(IExtendedBlockState state) {
        if (state.getBlock() instanceof IBakeryBlock) {
            ICustomBlockBakery bakery = ((IBakeryBlock) state.getBlock()).getCustomBakery();
            if (bakery instanceof ISimpleBlockBakery) {
                ISimpleBlockBakery simpleBakery = ((ISimpleBlockBakery) bakery);
                List<BakedQuad> generalQuads = new LinkedList<BakedQuad>();
                Map<EnumFacing, List<BakedQuad>> faceQuadsMap = new HashMap<EnumFacing, List<BakedQuad>>();
                //General non face culled quads.
                generalQuads.addAll(simpleBakery.bakeQuads(null, state));

                //Culled quads.
                for (EnumFacing face : EnumFacing.VALUES) {
                    List<BakedQuad> faceQuads = new LinkedList<BakedQuad>();

                    faceQuads.addAll(simpleBakery.bakeQuads(face, state));

                    faceQuadsMap.put(face, faceQuads);
                }
                return new SimpleSmartBakedModel(faceQuadsMap, generalQuads, TransformUtils.DEFAULT_BLOCK);
            }
            if (bakery instanceof ILayeredBlockBakery) {
                ILayeredBlockBakery layeredBakery = ((ILayeredBlockBakery) bakery);
                Map<EnumFacing, Map<BlockRenderLayer, List<BakedQuad>>> faceLayerQuads = new HashMap<EnumFacing, Map<BlockRenderLayer, List<BakedQuad>>>();
                Map<BlockRenderLayer, List<BakedQuad>> generalLayerQuads = new HashMap<BlockRenderLayer, List<BakedQuad>>();
                IBlockLayerProvider layerProvider = ((IBlockLayerProvider) state.getBlock());

                for (int i = 0; i < layerProvider.getTexturePasses(); i++) {
                    LinkedList<BakedQuad> quads = new LinkedList<BakedQuad>();
                    BlockRenderLayer layer = layerProvider.getRenderlayerForPass(i);
                    quads.addAll(layeredBakery.bakeLayerFace(null, i, layer, state));
                    generalLayerQuads.put(layer, quads);
                }

                for (EnumFacing face : EnumFacing.VALUES) {
                    Map<BlockRenderLayer, List<BakedQuad>> layerQuads = new HashMap<BlockRenderLayer, List<BakedQuad>>();
                    for (int i = 0; i < layerProvider.getTexturePasses(); i++) {
                        BlockRenderLayer layer = layerProvider.getRenderlayerForPass(i);
                        LinkedList<BakedQuad> quads = new LinkedList<BakedQuad>();

                        quads.addAll(layeredBakery.bakeLayerFace(face, i, layer, state));

                        layerQuads.put(layer, quads);
                    }
                    faceLayerQuads.put(face, layerQuads);
                }
                return new SimpleSmartLayeredModel(faceLayerQuads, generalLayerQuads, TransformUtils.DEFAULT_BLOCK);
            }
        }
        if (state.getBlock() instanceof IBlockTextureProvider && !(state.getBlock() instanceof IBlockLayeredTextureProvider)) {
            IBlockTextureProvider provider = ((IBlockTextureProvider) state.getBlock());

            Map<EnumFacing, List<BakedQuad>> quadFaceMap = new HashMap<EnumFacing, List<BakedQuad>>();
            for (EnumFacing face : EnumFacing.VALUES) {
                LinkedList<BakedQuad> quads = new LinkedList<BakedQuad>();

                quads.addAll(bakeBlockFace(face, provider.getTexture(face, state.getBlock().getMetaFromState(state))));

                quadFaceMap.put(face, quads);
            }
            return new SimpleSmartBakedModel(quadFaceMap, null, TransformUtils.DEFAULT_BLOCK);

        }
        Map<EnumFacing, Map<BlockRenderLayer, List<BakedQuad>>> quadFaceLayerMap = generateQuadFaceLayerMap(state);
        Map<EnumFacing, List<BakedQuad>> quadFaceMap = new HashMap<EnumFacing, List<BakedQuad>>();//Dummy map.
        TextureAtlasSprite particle = null;//state.getValue(PARTICLE_SPRITE_PROPERTY);
        return new SimplePerspectiveAwareBakedLayerModel(quadFaceLayerMap, quadFaceMap, particle, TransformUtils.DEFAULT_BLOCK);
    }

    public static Map<EnumFacing, Map<BlockRenderLayer, List<BakedQuad>>> generateQuadFaceLayerMap(IExtendedBlockState state) {
        Map<EnumFacing, Map<BlockRenderLayer, TextureAtlasSprite>> spriteFaceLayerMap = state.getValue(CommonProperties.SPRITE_FACE_LAYER_PROPERTY);
        Map<EnumFacing, Map<BlockRenderLayer, List<BakedQuad>>> quadFaceLayerMap = new HashMap<EnumFacing, Map<BlockRenderLayer, List<BakedQuad>>>();
        for (EnumFacing face : spriteFaceLayerMap.keySet()) {
            Map<BlockRenderLayer, TextureAtlasSprite> spriteLayerMap = spriteFaceLayerMap.get(face);
            Map<BlockRenderLayer, List<BakedQuad>> quadLayerMap = new HashMap<BlockRenderLayer, List<BakedQuad>>();
            for (BlockRenderLayer layer : spriteLayerMap.keySet()) {
                LinkedList<BakedQuad> quads = new LinkedList<BakedQuad>();
                quads.addAll(bakeBlockFace(face, spriteLayerMap.get(layer)));
                quadLayerMap.put(layer, quads);
            }
            quadFaceLayerMap.put(face, quadLayerMap);
        }
        return quadFaceLayerMap;
    }

    public static List<BakedQuad> bakeItemFace(EnumFacing face, TextureAtlasSprite sprite) {
        return ImmutableList.copyOf(PlanarFaceBakery.shadeQuadFaces(PlanarFaceBakery.bakeFace(face, sprite, DefaultVertexFormats.ITEM)));
    }

    public static List<BakedQuad> bakeBlockFace(EnumFacing face, TextureAtlasSprite sprite) {
        return ImmutableList.of(PlanarFaceBakery.bakeFace(face, sprite, DefaultVertexFormats.ITEM));
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        itemModelCache.invalidateAll();
        stateModelCache.invalidateAll();
    }
}
