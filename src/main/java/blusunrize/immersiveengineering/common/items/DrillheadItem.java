/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.items;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.EnumMetals;
import blusunrize.immersiveengineering.api.IETags;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.tool.IDrillHead;
import blusunrize.immersiveengineering.common.items.IEItems.Tools;
import blusunrize.immersiveengineering.common.util.Utils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.Tags.Items;
import net.minecraftforge.common.util.Constants.NBT;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DrillheadItem extends IEBaseItem implements IDrillHead
{
	//Maximal damage is slightly proportionate to pickaxes
	public static final DrillHeadPerm STEEL = new DrillHeadPerm("steel", IETags.getTagsFor(EnumMetals.STEEL).ingot, 3, 1, 3, 10, 7, 10000, new ResourceLocation(ImmersiveEngineering.MODID, "item/drill_diesel"));
	public static final DrillHeadPerm IRON = new DrillHeadPerm("iron", Items.INGOTS_IRON, 2, 1, 2, 9, 6, 6000, new ResourceLocation(ImmersiveEngineering.MODID, "item/drill_iron"));

	public static final String DAMAGE_KEY_OLD = "headDamage";
	public static final String DAMAGE_KEY = "Damage";

	public DrillHeadPerm perms;

	public DrillheadItem(DrillHeadPerm perms)
	{
		super("drillhead_"+perms.name, new Properties().stacksTo(1));
		this.perms = perms;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag)
	{
		list.add(new TranslatableComponent(Lib.DESC_FLAVOUR+"drillhead.size", perms.drillSize, perms.drillDepth));
		list.add(new TranslatableComponent(Lib.DESC_FLAVOUR+"drillhead.level", Utils.getHarvestLevelName(getMiningLevel(stack))));
		list.add(new TranslatableComponent(Lib.DESC_FLAVOUR+"drillhead.speed", Utils.formatDouble(getMiningSpeed(stack), "0.###")));
		list.add(new TranslatableComponent(Lib.DESC_FLAVOUR+"drillhead.damage", Utils.formatDouble(getAttackDamage(stack), "0.###")));

		int maxDmg = getMaximumHeadDamage(stack);
		int dmg = maxDmg-getHeadDamage(stack);
		float quote = dmg/(float)maxDmg;
		String status = ""+(quote < .1?ChatFormatting.RED: quote < .3?ChatFormatting.GOLD: quote < .6?ChatFormatting.YELLOW: ChatFormatting.GREEN);
		String s = status+(getMaximumHeadDamage(stack)-getHeadDamage(stack))+"/"+getMaximumHeadDamage(stack);
		list.add(new TranslatableComponent(Lib.DESC_INFO+"durability", s));
	}

	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack material)
	{
		return perms.repairMaterial.contains(material.getItem());
	}

	@Override
	public boolean beforeBlockbreak(ItemStack drill, ItemStack head, Player player)
	{
		return false;
	}

	@Override
	public void afterBlockbreak(ItemStack drill, ItemStack head, Player player)
	{
	}

	@Override
	public int getMiningLevel(ItemStack head)
	{
		return perms.drillLevel;
	}

	@Override
	public float getMiningSpeed(ItemStack head)
	{
		return perms.drillSpeed;
	}

	@Override
	public float getAttackDamage(ItemStack head)
	{
		return perms.drillAttack;
	}

	@Override
	public int getHeadDamage(ItemStack head)
	{
		if(head.hasTag())
		{
			CompoundTag nbt = head.getOrCreateTag();
			if(nbt.contains(DAMAGE_KEY_OLD, NBT.TAG_INT))
				return nbt.getInt(DAMAGE_KEY_OLD);
			else
				return nbt.getInt(DAMAGE_KEY);
		}
		else
			return 0;
	}

	@Override
	public int getMaximumHeadDamage(ItemStack head)
	{
		return perms.maxDamage;
	}

	@Override
	public void damageHead(ItemStack head, int dmg)
	{
		setHeadDamage(head, getHeadDamage(head)+dmg);
	}

	public static void setHeadDamage(ItemStack head, int totalDamage)
	{
		CompoundTag nbt = head.getOrCreateTag();
		nbt.remove(DAMAGE_KEY_OLD);
		nbt.putInt(DAMAGE_KEY, totalDamage);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public TextureAtlasSprite getDrillTexture(ItemStack drill, ItemStack head)
	{
		return perms.sprite;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack)
	{
		return getHeadDamage(stack)/(double)getMaximumHeadDamage(stack);
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack)
	{
		return getHeadDamage(stack) > 0;
	}

	public static class DrillHeadPerm
	{
		public static final Set<DrillHeadPerm> ALL_PERMS = new HashSet<>();

		final String name;
		final Tag<Item> repairMaterial;
		final int drillSize;
		final int drillDepth;
		final int drillLevel;
		final float drillSpeed;
		final float drillAttack;
		final int maxDamage;
		public final ResourceLocation texture;
		@OnlyIn(Dist.CLIENT)
		public TextureAtlasSprite sprite;

		public DrillHeadPerm(String name, Tag<Item> repairMaterial, int drillSize, int drillDepth, int drillLevel, float drillSpeed, int drillAttack, int maxDamage, ResourceLocation texture)
		{
			this.name = name;
			this.repairMaterial = repairMaterial;
			this.drillSize = drillSize;
			this.drillDepth = drillDepth;
			this.drillLevel = drillLevel;
			this.drillSpeed = drillSpeed;
			this.drillAttack = drillAttack;
			this.maxDamage = maxDamage;
			this.texture = texture;

			ALL_PERMS.add(this);
		}
	}

	@Override
	public ImmutableList<BlockPos> getExtraBlocksDug(ItemStack head, Level world, Player player, HitResult rtr)
	{
		if(!(rtr instanceof BlockHitResult))
			return ImmutableList.of();
		BlockHitResult brtr = (BlockHitResult)rtr;
		Direction side = brtr.getDirection();
		int diameter = perms.drillSize;
		int depth = perms.drillDepth;

		BlockPos startPos = brtr.getBlockPos();
		BlockState state = world.getBlockState(startPos);
		Block block = state.getBlock();
		float maxHardness = 1;
		if(block!=null&&!block.isAir(state, world, startPos))
			maxHardness = state.getDestroyProgress(player, world, startPos)*0.6F;
		if(maxHardness < 0)
			maxHardness = 0;

		if(diameter%2==0)//even numbers
		{
			float hx = (float)brtr.getLocation().x-brtr.getBlockPos().getX();
			float hy = (float)brtr.getLocation().y-brtr.getBlockPos().getY();
			float hz = (float)brtr.getLocation().z-brtr.getBlockPos().getZ();
			if((side.getAxis()==Axis.Y&&hx < .5)||(side.getAxis()==Axis.Z&&hx < .5))
				startPos = startPos.offset(-diameter/2, 0, 0);
			if(side.getAxis()!=Axis.Y&&hy < .5)
				startPos = startPos.offset(0, -diameter/2, 0);
			if((side.getAxis()==Axis.Y&&hz < .5)||(side.getAxis()==Axis.X&&hz < .5))
				startPos = startPos.offset(0, 0, -diameter/2);
		}
		else//odd numbers
			startPos = startPos.offset(-(side.getAxis()==Axis.X?0: diameter/2), -(side.getAxis()==Axis.Y?0: diameter/2), -(side.getAxis()==Axis.Z?0: diameter/2));
		Builder<BlockPos> b = ImmutableList.builder();
		for(int dd = 0; dd < depth; dd++)
			for(int dw = 0; dw < diameter; dw++)
				for(int dh = 0; dh < diameter; dh++)
				{
					BlockPos pos = startPos.offset((side.getAxis()==Axis.X?dd: dw), (side.getAxis()==Axis.Y?dd: dh), (side.getAxis()==Axis.Y?dh: side.getAxis()==Axis.X?dw: dd));
					if(pos.equals(brtr.getBlockPos()))
						continue;
					state = world.getBlockState(pos);
					block = state.getBlock();
					float h = state.getDestroyProgress(player, world, pos);
					boolean canHarvest = block.canHarvestBlock(world.getBlockState(pos), world, pos, player);
					boolean drillMat = ((DrillItem)Tools.drill).isEffective(ItemStack.EMPTY, state.getMaterial());
					boolean hardness = h > maxHardness;
					if(canHarvest&&drillMat&&hardness)
						b.add(pos);
				}
		return b.build();
	}
}