package cofh.thermalexpansion.gui.client.dynamo;

import cofh.lib.gui.element.ElementFluidTank;
import cofh.lib.util.helpers.StringHelper;
import cofh.thermalexpansion.core.TEProps;
import cofh.thermalexpansion.gui.container.ContainerTEBase;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class GuiDynamoCompression extends GuiDynamoBase {

	static final ResourceLocation TEXTURE = new ResourceLocation(TEProps.PATH_GUI_DYNAMO + "DynamoCompression.png");

	public GuiDynamoCompression(InventoryPlayer inventory, TileEntity tile) {

		super(new ContainerTEBase(inventory, tile), tile, inventory.player, TEXTURE);

		myInfo = StringHelper.localize("tab.thermalexpansion.dynamo.compression.0");
	}

	@Override
	public void initGui() {

		super.initGui();

		addElement(new ElementFluidTank(this, 8, 9, myTile.getTank(0)));
		addElement(new ElementFluidTank(this, 152, 9, myTile.getTank(1)));
	}

}
