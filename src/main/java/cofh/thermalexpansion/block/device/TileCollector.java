package cofh.thermalexpansion.block.device;

import cofh.api.inventory.IInventoryConnection;
import cofh.core.CoFHProps;
import cofh.core.RegistrySocial;
import cofh.lib.util.helpers.BlockHelper;
import cofh.lib.util.helpers.InventoryHelper;
import cofh.lib.util.helpers.SecurityHelper;
import cofh.lib.util.helpers.ServerHelper;
import cofh.thermalexpansion.block.device.BlockDevice.Types;
import cofh.thermalexpansion.gui.client.device.GuiCollector;
import cofh.thermalexpansion.gui.container.ContainerTEBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileCollector extends TileDeviceBase implements IInventoryConnection, ITickable {

	public static void initialize() {

		int type = BlockDevice.Types.COLLECTOR.ordinal();

		defaultSideConfig[type] = new SideConfig();
		defaultSideConfig[type].numConfig = 2;
		defaultSideConfig[type].slotGroups = new int[][] { {}, {} };
		defaultSideConfig[type].allowInsertionSide = new boolean[] { false, false };
		defaultSideConfig[type].allowExtractionSide = new boolean[] { false, false };
		defaultSideConfig[type].allowInsertionSlot = new boolean[] {};
		defaultSideConfig[type].allowExtractionSlot = new boolean[] {};
		defaultSideConfig[type].sideTex = new int[] { 0, 4 };
		defaultSideConfig[type].defaultSides = new byte[] { 0, 0, 0, 0, 0, 0 };

		GameRegistry.registerTileEntity(TileCollector.class, "thermalexpansion.Collector");
	}

	int areaMajor = 2;
	int areaMinor = 1;
	LinkedList<ItemStack> stuffedItems = new LinkedList<ItemStack>();

	static float[] defaultDropChances = new float[] { 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F };

	boolean ignoreFriends = true;
	boolean ignoreOwner = true;

	public boolean augmentEntityCollection;

	public TileCollector() {

		super(Types.COLLECTOR);
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
				collectItems();
			}
		}
	}

	public boolean isEmpty() {

		return stuffedItems.size() == 0;
	}

	public boolean doNotCollectItemsFrom(EntityPlayer player) {

		String name = player.getName();

		UUID ownerID = owner.getId();
		UUID otherID = SecurityHelper.getID(player);
		if (ownerID.equals(otherID)) {
			return ignoreOwner;
		}
		return ignoreFriends && RegistrySocial.playerHasAccess(name, owner);
	}

	public void collectItems() {

		int coords[] = BlockHelper.getAdjacentCoordinatesForSide(getPos().getX(), getPos().getY(), getPos().getZ(), facing);
		stuffedItems.addAll(collectItemsInArea(worldObj, coords[0], coords[1], coords[2], facing, areaMajor, areaMinor));

		if (augmentEntityCollection) {
			stuffedItems.addAll(collectItemsFromEntities(worldObj, coords[0], coords[1], coords[2], facing, areaMajor, areaMinor));
		}
	}

	public void outputBuffer() {

		for (EnumFacing face : EnumFacing.VALUES) {
			if (face.ordinal() != facing && sideCache[face.ordinal()] == 1) {
				BlockPos offset = getPos().offset(face);
				TileEntity theTile = worldObj.getTileEntity(offset);

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

	public List<ItemStack> collectItemsInArea(World worldObj, int x, int y, int z, int side, int areaMajor, int areaMinor) {

		int areaMajor2 = 1 + areaMajor;
		List<ItemStack> stacks = new ArrayList<ItemStack>();
		List<EntityItem> result;

		switch (side) {
		case 0:
		case 1:
			result = worldObj.getEntitiesWithinAABB(EntityItem.class,
                    new AxisAlignedBB(x - areaMajor, y, z - areaMajor, x + areaMajor2, y + areaMinor, z + areaMajor2));
			break;
		case 2:
		case 3:
			result = worldObj.getEntitiesWithinAABB(EntityItem.class,
                    new AxisAlignedBB(x - areaMajor, y - areaMajor, z, x + areaMajor2, y + areaMajor2, z + areaMinor));
			break;
		default:
			result = worldObj.getEntitiesWithinAABB(EntityItem.class,
					new AxisAlignedBB(x, y - areaMajor, z - areaMajor, x + areaMinor, y + areaMajor2, z + areaMajor2));
			break;
		}
		for (int i = 0; i < result.size(); i++) {
			EntityItem entity = result.get(i);
			if (entity.isDead || entity.getEntityItem().stackSize <= 0) {
				continue;
			}
			stacks.add(entity.getEntityItem());
			entity.worldObj.removeEntity(entity);
		}
		return stacks;
	}

	public List<ItemStack> collectItemsFromEntities(World worldObj, int x, int y, int z, int side, int areaMajor, int areaMinor) {

		int areaMajor2 = 1 + areaMajor;
		List<ItemStack> stacks = new ArrayList<ItemStack>();
		List<EntityLivingBase> result;

		switch (side) {
		case 0:
		case 1:
			result = worldObj.getEntitiesWithinAABB(EntityLivingBase.class,
                    new AxisAlignedBB(x - areaMajor, y, z - areaMajor, x + areaMajor2, y + areaMinor, z + areaMajor2));
			break;
		case 2:
		case 3:
			result = worldObj.getEntitiesWithinAABB(EntityLivingBase.class,
                    new AxisAlignedBB(x - areaMajor, y - areaMajor, z, x + areaMajor2, y + areaMajor2, z + areaMinor));
			break;
		default:
			result = worldObj.getEntitiesWithinAABB(EntityLivingBase.class,
                    new AxisAlignedBB(x, y - areaMajor, z - areaMajor, x + areaMinor, y + areaMajor2, z + areaMajor2));
			break;
		}
		for (int i = 0; i < result.size(); i++) {
			EntityLivingBase entity = result.get(i);
			float[] dropChances = defaultDropChances;

			if (entity instanceof EntityLiving) {
                EntityLiving living = ((EntityLiving) entity);
                float[] handChances = living.inventoryHandsDropChances;
                float[] armorChances = living.inventoryArmorDropChances;
				dropChances = new float[]{handChances[0], handChances[1], armorChances[0], armorChances[1], armorChances[2], armorChances[3]};
			} else if (isSecured() && entity instanceof EntityPlayer) {
				if (doNotCollectItemsFrom((EntityPlayer) entity)) {
					continue;
				}
			}
			for (int j = 0; j < 6; j++) {
                EntityEquipmentSlot slot = EntityEquipmentSlot.values()[j];
				ItemStack equipmentInSlot = entity.getItemStackFromSlot(slot);
				if (equipmentInSlot != null && dropChances[j] >= 1) {
					stacks.add(equipmentInSlot);
					entity.setItemStackToSlot(slot, null);
				}
			}
		}
		return stacks;
	}

	/* GUI METHODS */
	@Override
	public Object getGuiClient(InventoryPlayer inventory) {

		return new GuiCollector(inventory, this);
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
