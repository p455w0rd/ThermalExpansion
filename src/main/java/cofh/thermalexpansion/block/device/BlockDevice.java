package cofh.thermalexpansion.block.device;

import cofh.thermalexpansion.block.BlockTEBase;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IStringSerializable;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockDevice extends BlockTEBase {

	public static final PropertyEnum<BlockDevice.Type> VARIANT = PropertyEnum.<BlockDevice.Type> create("type", BlockDevice.Type.class);

	public BlockDevice() {

		super(Material.iron);

		setUnlocalizedName("device");

		setHardness(15.0F);
		setResistance(25.0F);
	}

	@Override
	protected BlockState createBlockState() {

		return new BlockState(this, new IProperty[] { VARIANT });
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {

		for (int i = 0; i < BlockDevice.Type.METADATA_LOOKUP.length; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public int getDamageValue(World world, BlockPos pos) {

		IBlockState state = world.getBlockState(pos);
		return state.getBlock() != this ? 0 : state.getValue(VARIANT).getMetadata();
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {

		return this.getDefaultState().withProperty(VARIANT, BlockDevice.Type.byMetadata(meta));
	}

	@Override
	public int getMetaFromState(IBlockState state) {

		return state.getValue(VARIANT).getMetadata();
	}

	@Override
	public int damageDropped(IBlockState state) {

		return state.getValue(VARIANT).getMetadata();
	}

	/* ITileEntityProvider */
	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {

		if (metadata >= BlockDevice.Type.values().length) {
			return null;
		}
		switch (BlockDevice.Type.values()[metadata]) {
		case ACTIVATOR:
			return null;
		default:
			return null;
		}
	}

	/* IInitializer */
	@Override
	public boolean preInit() {

		GameRegistry.registerBlock(this, ItemBlockDevice.class, "device");

		return true;
	}

	@Override
	public boolean initialize() {

		return true;
	}

	@Override
	public boolean postInit() {

		return true;
	}

	/* TYPE */
	public static enum Type implements IStringSerializable {

		// @formatter:off
		ACTIVATOR(0, "activator", deviceActivator),
		BREAKER(1, "breaker", deviceBreaker),
		COLLECTOR(2, "collector", deviceCollector),
		NULLIFIER(3, "nullifier", deviceNullifier),
		BUFFER(4, "buffer", deviceBuffer);
		// @formatter:on

		private static final BlockDevice.Type[] METADATA_LOOKUP = new BlockDevice.Type[values().length];
		private final int metadata;
		private final String name;
		private final ItemStack stack;

		private final int light;

		private Type(int metadata, String name, ItemStack stack, int light) {

			this.metadata = metadata;
			this.name = name;
			this.stack = stack;

			this.light = light;
		}

		private Type(int metadata, String name, ItemStack stack) {

			this(metadata, name, stack, 0);
		}

		public int getMetadata() {

			return this.metadata;
		}

		@Override
		public String getName() {

			return this.name;
		}

		public ItemStack getStack() {

			return this.stack;
		}

		public int getLight() {

			return light;
		}

		public static Type byMetadata(int metadata) {

			if (metadata < 0 || metadata >= METADATA_LOOKUP.length) {
				metadata = 0;
			}
			return METADATA_LOOKUP[metadata];
		}

		static {
			for (Type type : values()) {
				METADATA_LOOKUP[type.getMetadata()] = type;
			}
		}
	}

	/* REFERENCES */
	public static ItemStack deviceActivator;
	public static ItemStack deviceBreaker;
	public static ItemStack deviceCollector;
	public static ItemStack deviceNullifier;
	public static ItemStack deviceBuffer;
	public static ItemStack deviceExtender;

}
