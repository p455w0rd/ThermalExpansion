package cofh.thermalexpansion.block;

import cofh.api.tileentity.IRedstoneControl;
import cofh.asm.relauncher.CoFHSide;
import cofh.asm.relauncher.Implementable;
import cofh.asm.relauncher.Strippable;
import cofh.core.network.PacketCoFHBase;
import cofh.lib.audio.ISoundSource;
import cofh.lib.audio.SoundLocation;
import cofh.lib.util.helpers.ServerHelper;
import cofh.lib.util.helpers.SoundHelper;
import cofh.thermalexpansion.network.PacketTEBase;

import net.minecraft.client.audio.ISound;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Implementable("buildcraft.api.tiles.IHasWork")
@Strippable(value = "cofh.core.audio.ISoundSource", side = CoFHSide.SERVER)
public abstract class TileRSControl extends TileInventorySecure implements IRedstoneControl, ISoundSource {

	public boolean isActive;

	protected int powerLevel;
	protected boolean isPowered;
	protected boolean wasPowered;

	protected ControlMode rsMode = ControlMode.DISABLED;

	@Override
	public void onNeighborBlockChange() {

		wasPowered = isPowered;
		powerLevel = worldObj.isBlockIndirectlyGettingPowered(pos);
		isPowered = powerLevel > 0;

		if (wasPowered != isPowered && sendRedstoneUpdates()) {
			PacketTEBase.sendRSPowerUpdatePacketToClients(this, worldObj, pos);
			onRedstoneUpdate();
		}
	}

	public void onRedstoneUpdate() {

	}

	protected boolean sendRedstoneUpdates() {

		return false;
	}

	public final boolean redstoneControlOrDisable() {

		return rsMode.isDisabled() || isPowered == rsMode.getState();
	}

	/* NBT METHODS */
	@Override
	public void readFromNBT(NBTTagCompound nbt) {

		super.readFromNBT(nbt);

		isActive = nbt.getBoolean("Active");
		NBTTagCompound rsTag = nbt.getCompoundTag("RS");

		isPowered = rsTag.getBoolean("Power");
		powerLevel = rsTag.getByte("Level");
		rsMode = ControlMode.values()[rsTag.getByte("Mode")];
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {

		super.writeToNBT(nbt);

		nbt.setBoolean("Active", isActive);
		NBTTagCompound rsTag = new NBTTagCompound();

		rsTag.setBoolean("Power", isPowered);
		rsTag.setByte("Level", (byte) powerLevel);
		rsTag.setByte("Mode", (byte) rsMode.ordinal());
		nbt.setTag("RS", rsTag);
	}

	/* NETWORK METHODS */
	@Override
	public PacketCoFHBase getPacket() {

		PacketCoFHBase payload = super.getPacket();

		payload.addBool(isPowered);
		payload.addByte(rsMode.ordinal());
		payload.addBool(isActive);

		return payload;
	}

	/* ITilePacketHandler */
	@Override
	public void handleTilePacket(PacketCoFHBase payload, boolean isServer) {

		super.handleTilePacket(payload, isServer);

		isPowered = payload.getBool();
		rsMode = ControlMode.values()[payload.getByte()];

		if (!isServer) {
			boolean prevActive = isActive;
			isActive = payload.getBool();

			if (isActive && !prevActive) {
				if (getSoundName() != null && !getSoundName().isEmpty()) {
					SoundHelper.playSound(getSound());
				}
			}
		} else {
			payload.getBool();
		}
	}

	/* IRedstoneControl */
	@Override
	public final void setPowered(boolean isPowered) {

		wasPowered = this.isPowered;
		this.isPowered = isPowered;
		if (ServerHelper.isClientWorld(worldObj)) {
			worldObj.markBlockForUpdate(pos);
		}
	}

	@Override
	public final boolean isPowered() {

		return isPowered;
	}

	@Override
	public final void setControl(ControlMode control) {

		rsMode = control;
		if (ServerHelper.isClientWorld(worldObj)) {
			PacketTEBase.sendRSConfigUpdatePacketToServer(this, pos);
		} else {
			sendUpdatePacket(Side.CLIENT);
		}
	}

	@Override
	public final ControlMode getControl() {

		return rsMode;
	}

	/* ISoundSource */
	@Override
	@SideOnly(Side.CLIENT)
	public ISound getSound() {

		return new SoundLocation(this, getSoundName(), 1.0F, 1.0F, true, 0, pos.getX(), pos.getY(), pos.getZ());
	}

	public String getSoundName() {

		return "";
	}

	@Override
	public boolean shouldPlaySound() {

		return !tileEntityInvalid && isActive;
	}

	/* IHasWork */
	public boolean hasWork() {

		return isActive;
	}

}
