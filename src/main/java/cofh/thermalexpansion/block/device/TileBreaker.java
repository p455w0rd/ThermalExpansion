package cofh.thermalexpansion.block.device;

import cofh.api.inventory.IInventoryConnection;
import cofh.core.CoFHProps;
import cofh.core.entity.CoFHFakePlayer;
import cofh.lib.util.helpers.BlockHelper;
import cofh.lib.util.helpers.FluidHelper;
import cofh.lib.util.helpers.InventoryHelper;
import cofh.lib.util.helpers.ServerHelper;
import cofh.thermalexpansion.block.device.BlockDevice.Types;
import cofh.thermalexpansion.gui.client.device.GuiBreaker;
import cofh.thermalexpansion.gui.container.ContainerTEBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.LinkedList;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class TileBreaker extends TileDeviceBase implements IFluidHandler, IInventoryConnection, ITickable {

	public static void initialize() {

		int type = BlockDevice.Types.BREAKER.ordinal();

		defaultSideConfig[type] = new SideConfig();
		defaultSideConfig[type].numConfig = 2;
		defaultSideConfig[type].slotGroups = new int[][] { {}, {} };
		defaultSideConfig[type].allowInsertionSide = new boolean[] { false, false };
		defaultSideConfig[type].allowExtractionSide = new boolean[] { false, false };
		defaultSideConfig[type].allowInsertionSlot = new boolean[] {};
		defaultSideConfig[type].allowExtractionSlot = new boolean[] {};
		defaultSideConfig[type].sideTex = new int[] { 0, 4 };
		defaultSideConfig[type].defaultSides = new byte[] { 0, 0, 0, 0, 0, 0 };

		GameRegistry.registerTileEntity(TileBreaker.class, "thermalexpansion.Breaker");
	}

	CoFHFakePlayer myFakePlayer;
	LinkedList<ItemStack> stuffedItems = new LinkedList<ItemStack>();

	public boolean augmentFluid;

	public TileBreaker() {

		super(Types.BREAKER);
	}

	@Override
	public void cofh_validate() {

		if (ServerHelper.isServerWorld(worldObj)) {
			myFakePlayer = new CoFHFakePlayer((WorldServer) worldObj);
		}
		super.cofh_validate();
	}

	@Override
	public void setDefaultSides() {

		sideCache = getDefaultSides();
		sideCache[facing ^ 1] = 1;
	}

	@Override
	public void update() {

		if (ServerHelper.isClientWorld(worldObj)) {
			return;
		}
		if (worldObj.getTotalWorldTime() % CoFHProps.TIME_CONSTANT_HALF == 0 && redstoneControlOrDisable()) {
			if (!isEmpty()) {
				outputBuffer();
			}
			if (isEmpty()) {
				breakBlock();
			}
		}
	}

	public boolean isEmpty() {

		return stuffedItems.size() == 0;
	}

	public void breakBlock() {

        BlockPos offsetPos = getPos().offset(EnumFacing.VALUES[facing]);
		IBlockState state = worldObj.getBlockState(offsetPos);
		FluidStack theStack = augmentFluid ? FluidHelper.getFluidFromWorld(worldObj, offsetPos, true) : null;
		if (theStack != null) {
			for (int i = 0; i < 6 && theStack.amount > 0; i++) {
				if (sideCache[i] == 1) {
					theStack.amount -= FluidHelper.insertFluidIntoAdjacentFluidHandler(this, i, theStack, true);
				}
			}
			worldObj.setBlockToAir(offsetPos);
		} else if (CoFHFakePlayer.isBlockBreakable(myFakePlayer, worldObj, offsetPos)) {
			stuffedItems.addAll(BlockHelper.breakBlock(worldObj, myFakePlayer, offsetPos, state, 0, true, false));
		}
	}

	public void outputBuffer() {

		for (EnumFacing face : EnumFacing.VALUES) {
			if (face.ordinal() != facing && sideCache[face.ordinal()] == 1) {

                BlockPos offsetPos = getPos().offset(face);
				TileEntity theTile = worldObj.getTileEntity(offsetPos);

				if (InventoryHelper.isInsertion(theTile)) {
					LinkedList<ItemStack> newStuffed = new LinkedList<ItemStack>();
					for (ItemStack curItem : stuffedItems) {
						if (curItem == null || curItem.getItem() == null) {
							curItem = null;
						} else {
							curItem = InventoryHelper.addToInsertion(theTile, face, curItem);
						}
						if (curItem != null) {
							newStuffed.add(curItem);
						}
					}
					stuffedItems = newStuffed;
				}
			}
		}
	}

	/* GUI METHODS */
	@Override
	public Object getGuiClient(InventoryPlayer inventory) {

		return new GuiBreaker(inventory, this);
	}

	@Override
	public Object getGuiServer(InventoryPlayer inventory) {

		return new ContainerTEBase(inventory, this);
	}

	/* NBT METHODS */
	@Override
	public void readFromNBT(NBTTagCompound nbt) {

		super.readFromNBT(nbt);

		NBTTagList list = nbt.getTagList("StuffedInv", 10);
		stuffedItems.clear();
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound compound = list.getCompoundTagAt(i);
			stuffedItems.add(ItemStack.loadItemStackFromNBT(compound));
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {

		super.writeToNBT(nbt);

		NBTTagList list = new NBTTagList();
		list = new NBTTagList();
		for (int i = 0; i < stuffedItems.size(); i++) {
			if (stuffedItems.get(i) != null) {
				NBTTagCompound compound = new NBTTagCompound();
				stuffedItems.get(i).writeToNBT(compound);
				list.appendTag(compound);
			}
		}
		nbt.setTag("StuffedInv", list);
        return nbt;
	}

	/* IFluidHandler */
	@Override
	public int fill(EnumFacing from, FluidStack resource, boolean doFill) {

		return 0;
	}

	@Override
	public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {

		return null;
	}

	@Override
	public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {

		return null;
	}

	@Override
	public boolean canFill(EnumFacing from, Fluid fluid) {

		return false;
	}

	@Override
	public boolean canDrain(EnumFacing from, Fluid fluid) {

		return false;
	}

	@Override
	public FluidTankInfo[] getTankInfo(EnumFacing from) {

		return CoFHProps.EMPTY_TANK_INFO;
	}

	/* IInventoryConnection */
	@Override
	public ConnectionType canConnectInventory(EnumFacing from) {

		if (from != null && from.ordinal() != facing && sideCache[from.ordinal()] == 1) {
			return ConnectionType.FORCE;
		} else {
			return ConnectionType.DEFAULT;
		}
	}

}
