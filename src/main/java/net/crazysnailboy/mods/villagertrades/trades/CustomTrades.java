package net.crazysnailboy.mods.villagertrades.trades;

import java.util.Random;
import net.crazysnailboy.mods.villagertrades.loaders.TradeLoader.ItemStacksAndPrices;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityVillager.PriceInfo;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;


public class CustomTrades
{

	public static class ExtraTradeData
	{

		public double chance = 1;
		public Boolean rewardsExp;
		public Integer maxTradeUses;
	}


	public static class VTTVillagerTradeBase implements EntityVillager.ITradeList
	{

		private ItemStacksAndPrices buy1;
		private ItemStacksAndPrices buy2;
		private ItemStacksAndPrices sell;
		private ExtraTradeData extraTradeData;


		public VTTVillagerTradeBase(ItemStacksAndPrices buy1, ItemStacksAndPrices buy2, ItemStacksAndPrices sell, ExtraTradeData extraTradeData)
		{
			this.buy1 = buy1;
			this.buy2 = buy2;
			this.sell = sell;
			this.extraTradeData = extraTradeData;
		}


		@Override
		public void modifyMerchantRecipeList(MerchantRecipeList recipeList, Random random)
		{
			if (extraTradeData.chance != 1 && random.nextDouble() > extraTradeData.chance) return;

			// choose random index values to use to retrieve elements from the itemstacks and prices collections
			int buyIndex1 = random.nextInt(buy1.getItemStacks().size());
			// if the buy2 or sell collections are the same size as the buy1, use the buy1 index
			// otherwise use a random index based on that collection's size
			int buyIndex2 = (this.buy2 != null ? (buy1.getItemStacks().size() == buy2.getItemStacks().size() ? buyIndex1 : random.nextInt(buy2.getItemStacks().size())) : Short.MIN_VALUE);
			int sellIndex = (buy1.getItemStacks().size() == sell.getItemStacks().size() ? buyIndex1 : random.nextInt(sell.getItemStacks().size()));

			// choose a random number of items to apply to each itemstack within the bounds of the priceinfo values
			int buyAmount1 = this.buy1.getPrices().get(buyIndex1).getPrice(random);
			int buyAmount2 = (this.buy2 != null ? this.buy2.getPrices().get(buyIndex2).getPrice(random) : Short.MIN_VALUE);
			int sellAmount = this.sell.getPrices().get(sellIndex).getPrice(random);

			// copy itemstacks from the collections and apply the appropriate stack size to them
			ItemStack buyStack1 = this.buy1.getItemStacks().get(buyIndex1).copy();
			buyStack1.stackSize = buyAmount1;
			ItemStack buyStack2 = (this.buy2 != null ? this.buy2.getItemStacks().get(buyIndex2).copy() : null);
			if (buyStack2 != null) buyStack2.stackSize = buyAmount2;
			ItemStack sellStack = this.sell.getItemStacks().get(sellIndex).copy();
			sellStack.stackSize = sellAmount;

			// examine any nbt data present on the sell stack to see whether we should apply random effects
			NBTTagCompound tag = sellStack.getTagCompound();

			// if the nbt data specifies a random enchantment
			if (tag != null && tag.hasKey("ench") && tag.getString("ench").equals("random"))
			{
				// remove the random enchantment sub compound from the nbt tag
				tag.removeTag("ench");
				sellStack.setTagCompound(tag);

				// apply random enchantments to the item being sold
				if (sellStack.getItem() == Items.ENCHANTED_BOOK)
				{
					// *** copied from net.minecraft.entity.passive.EntityVillager.ListEnchantedBookForEmeralds ***
					Enchantment enchantment = (Enchantment)Enchantment.REGISTRY.getRandomObject(random);
					int enchLevel = MathHelper.getInt(random, enchantment.getMinLevel(), enchantment.getMaxLevel());

					sellStack.setTagCompound(Items.ENCHANTED_BOOK.getEnchantedItemStack(new EnchantmentData(enchantment, enchLevel)).getTagCompound());

					int buyAmount = 2 + random.nextInt(5 + enchLevel * 10) + 3 * enchLevel;
					if (enchantment.isTreasureEnchantment()) buyAmount *= 2;

					PriceInfo buyPrice = this.buy1.getPrices().get(buyIndex1);
					if (buyAmount < buyPrice.getFirst()) buyAmount = buyPrice.getFirst();
					if (buyAmount > buyPrice.getSecond()) buyAmount = buyPrice.getSecond();

					buyStack1.stackSize = buyAmount;
				}
				else
				{
					// *** copied from net.minecraft.entity.passive.EntityVillager.ListEnchantedItemForEmeralds ***
					sellStack = EnchantmentHelper.addRandomEnchantment(random, sellStack, 5 + random.nextInt(15), false);
				}
			}

			// if the nbt data specifies a random potion effect
			if (tag != null && tag.hasKey("Potion") && tag.getString("Potion").equals("random"))
			{
				// remove the random potion sub compound from the nbt tag
				tag.removeTag("Potion");
				sellStack.setTagCompound(tag);

				// apply random potion effects to the item being sold
				sellStack = PotionUtils.addPotionToItemStack(sellStack, PotionType.REGISTRY.getRandomObject(random));
			}


			MerchantRecipe recipe;

			boolean isOre = (buy1.isOre(buyIndex1) || (buy2 == null ? false : buy2.isOre(buyIndex2)) || sell.isOre(sellIndex));

			// create a merchant recipe to the merchant recipe list
			if (isOre)
			{
				NBTTagCompound compound = new NBTTagCompound();

				NBTTagCompound buyTagStack1 = buyStack1.writeToNBT(new NBTTagCompound());
				if (buy1.isOre(buyIndex1)) buyTagStack1.setString("id", buy1.getOreIds().get(buyIndex1));
				compound.setTag("buy", buyTagStack1);

				if (buy2 != null)
				{
					NBTTagCompound buyTagStack2 = buyStack2.writeToNBT(new NBTTagCompound());
					if (buy2.isOre(buyIndex2)) buyTagStack2.setString("id", buy2.getOreIds().get(buyIndex2));
					compound.setTag("buyB", buyTagStack2);
				}

				NBTTagCompound sellTagStack = sellStack.writeToNBT(new NBTTagCompound());
				if (sell.isOre(sellIndex)) sellTagStack.setString("id", sell.getOreIds().get(sellIndex));
				compound.setTag("sell", sellTagStack);

				recipe = new MerchantOreRecipe(compound);
			}
			else
			{
				recipe = (buy2 == null ? new MerchantRecipe(buyStack1, sellStack) : new MerchantRecipe(buyStack1, buyStack2, sellStack));
			}


			// if the extra trade data specifies a rewardsExp value
			if (extraTradeData.rewardsExp != null)
			{
				// set the private rewardsExp field on the merchant recipe
				boolean rewardsExp = extraTradeData.rewardsExp.booleanValue();
				ObfuscationReflectionHelper.setPrivateValue(MerchantRecipe.class, recipe, rewardsExp, "rewardsExp", "field_180323_f");
			}
			// if the extra trade data specifes a maxTradeUses value
			if (extraTradeData.maxTradeUses != null)
			{
				// set the private maxTradeUses field on the merchant recipe
				int maxTradeUses = extraTradeData.maxTradeUses.intValue();
				ObfuscationReflectionHelper.setPrivateValue(MerchantRecipe.class, recipe, maxTradeUses, "maxTradeUses", "field_82786_e");
			}

			recipeList.add(recipe);
		}

	}


	/**
	 * A villager buying trade where the player receives currency items (e.g. emeralds) in exchange for other items
	 *
	 */
	public static class VTTVillagerBuyingTrade extends VTTVillagerTradeBase
	{

		public VTTVillagerBuyingTrade(ItemStacksAndPrices buy1, ItemStacksAndPrices buy2, ItemStacksAndPrices sell, ExtraTradeData extraTradeData)
		{
			super(buy1, buy2, sell, extraTradeData);
		}
	}


	/**
	 * A villager selling trade where the player receives items from the villager in exchange for currency items (e.g. emeralds)
	 *
	 */
	public static class VTTVillagerSellingTrade extends VTTVillagerTradeBase
	{

		public VTTVillagerSellingTrade(ItemStacksAndPrices buy1, ItemStacksAndPrices buy2, ItemStacksAndPrices sell, ExtraTradeData extraTradeData)
		{
			super(buy1, buy2, sell, extraTradeData);
		}
	}

}
