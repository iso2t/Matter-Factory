package com.iso2t.matterfactory.common.item;

import com.iso2t.matterfactory.common.block.BaseBlock;
import lombok.Getter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class BaseBlockItem extends BlockItem {

	@Getter
	private final BaseBlock blockType;

	public BaseBlockItem (Block id, Properties properties) {
		super(id, properties);
		this.blockType = (BaseBlock) id;
	}

}
